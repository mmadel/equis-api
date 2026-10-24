package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

// W2's Close and Reopen: a person who doesn't come back is hidden from the active lists, never deleted.
// The member row is locked, so a close can't slip in between a rebook's checks (the rebook locks it too).
@Service
public class MemberCloseService {
    // A booked assessment: the seat and the trainer's list still count on it, so it is cancelled first.
    private static final Set<String> BOOKED = Set.of("upcoming", "waiting");

    private final MemberRepository members;
    private final RiderRepository riders;
    private final AssessmentBookingRepository bookings;
    private final AdminBookingRowAssembler rows;
    private final Clock clock;
    private final ZoneId schoolZone;

    public MemberCloseService(MemberRepository members, RiderRepository riders, AssessmentBookingRepository bookings,
                              AdminBookingRowAssembler rows, Clock clock,
                              @Value("${app.time-zone}") ZoneId schoolZone) {
        this.members = members;
        this.riders = riders;
        this.bookings = bookings;
        this.rows = rows;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    @Transactional
    public AdminBookingRow close(UUID id) {
        Instant now = clock.instant();
        Member member = lock(id);
        if (member.isClosed()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.MEMBER_ALREADY_CLOSED,
                    member.getName() + " is already closed.");
        }
        if (riders.existsByMemberId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.MEMBER_IS_RIDER,
                    member.getName() + " is a rider and can't be closed.");
        }
        AssessmentBooking latest = latestBooking(id);
        if (BOOKED.contains(latest.displayStatus(now, schoolZone))) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.MEMBER_HAS_ACTIVE_BOOKING,
                    "Cancel the assessment first.");
        }
        member.close(now);
        return rows.row(latest);
    }

    @Transactional
    public AdminBookingRow reopen(UUID id) {
        Member member = lock(id);
        if (!member.isClosed()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.MEMBER_NOT_CLOSED,
                    member.getName() + " is not closed.");
        }
        member.reopen(clock.instant());
        return rows.row(latestBooking(id));
    }

    // The first read of the member in this transaction, so the lock also brings back its latest state.
    private Member lock(UUID id) {
        return members.findByIdForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.MEMBER_NOT_FOUND, "Person not found."));
    }

    // Every person is added together with a booking, so there is always one. It is their W2 row.
    private AssessmentBooking latestBooking(UUID id) {
        return bookings.findLatestForMember(id)
                .orElseThrow(() -> new IllegalStateException("Member " + id + " has no booking"));
    }
}
