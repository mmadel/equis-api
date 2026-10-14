package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.assessment.RecordResultRequest;
import com.equestrian.equis.dto.assessment.ResultRecorded;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RecordResultService {
    private final AssessmentBookingRepository bookings;
    private final RiderRepository riders;
    private final LevelRepository levels;
    private final AssessmentRules rules;
    private final Clock clock;

    public RecordResultService(AssessmentBookingRepository bookings, RiderRepository riders, LevelRepository levels,
                               AssessmentRules rules, Clock clock) {
        this.bookings = bookings;
        this.riders = riders;
        this.levels = levels;
        this.rules = rules;
        this.clock = clock;
    }

    // Records whether the member came and, if they did, creates their rider with the assessor as trainer.
    // The booking change is saved at commit by dirty checking, which bumps its version.
    @Transactional
    public ResultRecorded record(UUID bookingId, AuthUser caller, RecordResultRequest req) {
        // Needs no database, so a bad request never takes a lock.
        rules.requireConsistentResult(req);
        Instant now = clock.instant();

        // The lock serialises two Saves at once (a double tap, or the admin and the assessor):
        // the second sees the first's result and is refused by requireCanRecord.
        AssessmentBooking booking = bookings.findByIdForUpdate(bookingId).orElseThrow(rules::bookingNotFound);
        rules.requireCanRecord(booking, caller, now);

        if (!req.attended()) {
            booking.recordNoShow(req.notes(), now);
            return new ResultRecorded(booking.getId(), BookingStatus.NOSHOW.value(), null);
        }

        // levelId is already checked as 1-6, so a reference is enough.
        Level level = levels.getReferenceById(req.levelId().shortValue());
        booking.recordAttended(level, req.notes(), now);
        // uq_rider_member is only a backstop behind the lock; if it fires it is the same 409, never a 500.
        try {
            Rider rider = riders.saveAndFlush(Rider.fromAssessment(booking.getMember(), level, booking.getAssessor()));
            return new ResultRecorded(booking.getId(), BookingStatus.PLACED.value(), rider.getId());
        } catch (DataIntegrityViolationException e) {
            if (Constraints.nameOf(e).filter("uq_rider_member"::equals).isPresent()) {
                throw rules.resultAlreadyRecorded();
            }
            throw e;
        }
    }
}
