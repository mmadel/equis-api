package com.equestrian.equis.service.assessment;

import com.equestrian.equis.dto.accounts.TrainerRef;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.MemberRef;
import com.equestrian.equis.dto.assessment.SlotRef;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.notices.NoticeType;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.notices.NoticeRepository;
import com.equestrian.equis.repository.notices.RefTime;
import com.equestrian.equis.repository.riders.MemberRider;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// The one place W2 rows are built. The bookings come with member, slot and assessor loaded;
// one more query finds the riders of the placed members and one more the latest reminder per booking.
@Component
public class AdminBookingRowAssembler {
    private final RiderRepository riders;
    private final NoticeRepository notices;
    private final Clock clock;
    private final ZoneId schoolZone;

    public AdminBookingRowAssembler(RiderRepository riders, NoticeRepository notices, Clock clock,
                                    @Value("${app.time-zone}") ZoneId schoolZone) {
        this.riders = riders;
        this.notices = notices;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    public List<AdminBookingRow> rows(List<AssessmentBooking> bookings, Instant now) {
        List<UUID> placedMembers = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PLACED)
                .map(b -> b.getMember().getId())
                .toList();
        Map<UUID, UUID> riderIds = placedMembers.isEmpty() ? Map.of()
                : riders.findMemberRiderIds(placedMembers).stream()
                        .collect(Collectors.toMap(MemberRider::memberId, MemberRider::riderId));
        List<UUID> bookingIds = bookings.stream().map(AssessmentBooking::getId).toList();
        Map<UUID, Instant> reminders = bookingIds.isEmpty() ? Map.of()
                : notices.findLatestByRefIds(bookingIds, NoticeType.RESULT_OVERDUE).stream()
                        .collect(Collectors.toMap(RefTime::refId, RefTime::at));

        return bookings.stream()
                .map(b -> row(b, riderIds.get(b.getMember().getId()), reminders.get(b.getId()), now))
                .toList();
    }

    // The updated row the PATCH and remind actions answer with.
    public AdminBookingRow row(AssessmentBooking booking) {
        return rows(List.of(booking), clock.instant()).getFirst();
    }

    private AdminBookingRow row(AssessmentBooking b, UUID riderId, Instant remindedAt, Instant now) {
        Member m = b.getMember();
        Session s = b.getSession();
        Trainer a = b.getAssessor();
        String status = b.displayStatus(now, schoolZone);
        List<String> actions = BookingActions.allowed(status, a.getStatus() == TrainerStatus.ACTIVE).stream()
                .map(BookingAction::value)
                .toList();
        return new AdminBookingRow(b.getId(),
                new MemberRef(m.getId(), m.getName(), m.getPhone()),
                new SlotRef(s.getId(), s.getSessionDate(), s.getStartTime(), s.getEndTime()),
                new TrainerRef(a.getId(), a.getName(), a.getStatus().name().toLowerCase(Locale.ROOT)),
                status,
                b.getLevel() == null ? null : b.getLevel().getId().intValue(),
                b.getNotes(),
                b.getRecordedAt(),
                b.getStatus() == BookingStatus.PLACED ? riderId : null,
                remindedAt,
                actions,
                b.getCreatedAt());
    }
}
