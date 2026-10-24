package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.ChangeBookingRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// W2's Reschedule, Rebook and assessor dropdown. What a slotId means follows from the booking's status:
// upcoming = move the same booking, no-show or cancelled = a new booking, the old one kept as history.
// The checks match BookingActions, so W2 only offers what this service accepts.
@Service
public class AdminBookingChangeService {
    private static final String WAITING = "waiting";

    private final AssessmentBookingRepository bookings;
    private final SessionRepository sessions;
    private final AssessmentRules rules;
    private final AdminBookingRowAssembler rows;
    private final EntityManager entityManager;
    private final Clock clock;
    private final ZoneId schoolZone;

    public AdminBookingChangeService(AssessmentBookingRepository bookings, SessionRepository sessions,
                                     AssessmentRules rules, AdminBookingRowAssembler rows, EntityManager entityManager,
                                     Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.bookings = bookings;
        this.sessions = sessions;
        this.rules = rules;
        this.rows = rows;
        this.entityManager = entityManager;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // Lock order: the booking, the target slot, then (rebook only) the member. Recording a result locks only the
    // booking, booking a new member only the slot and closing a person only the member, so no lock cycle is possible. Every refusal is thrown before anything is flushed,
    // or rolls the transaction back, so a failed change leaves the data as it was.
    @Transactional
    public AdminBookingRow change(UUID bookingId, ChangeBookingRequest req) {
        if (req.slotId() == null && req.assessorId() == null) {
            throw invalidField("slotId", ErrorCodes.NOTHING_TO_CHANGE);
        }
        Instant now = clock.instant();

        // The lock serialises this with the assessor recording a result on the same booking.
        AssessmentBooking b = bookings.findByIdForUpdate(bookingId).orElseThrow(rules::bookingNotFound);
        rules.requireNotClosed(b.getMember());
        if (b.getStatus() == BookingStatus.PLACED) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.BOOKING_CLOSED, "A placed booking can't be changed.");
        }
        // An upcoming booking is always the member's latest: they have one active booking at most.
        if (b.getStatus() != BookingStatus.UPCOMING
                && bookings.existsNewerForMember(b.getMember().getId(), b.getCreatedAt(), b.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.BOOKING_SUPERSEDED,
                    "This member already has a newer booking.");
        }

        Trainer newAssessor = req.assessorId() == null ? null : rules.requireActiveAssessor(req.assessorId());
        if (req.slotId() == null) {
            b.changeAssessor(newAssessor);
            return rows.row(b);
        }

        if (WAITING.equals(b.displayStatus(now, schoolZone))) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SLOT_STARTED,
                    "This slot has started. Record a result or no-show first.");
        }
        if (b.getStatus() == BookingStatus.UPCOMING) {
            // The same slot is no change: no capacity check, no error.
            if (!req.slotId().equals(b.getSession().getId())) {
                b.moveTo(lockBookableSlot(req.slotId(), now));
            }
            if (newAssessor != null) {
                b.changeAssessor(newAssessor);
            }
            return rows.row(b);
        }

        // Rebook a no-show or cancelled booking.
        Session slot = lockBookableSlot(req.slotId(), now);
        // Lock the member and reload it, so a close that committed meanwhile is seen. The member is already loaded
        // here, so MemberRepository.findByIdForUpdate would lock the row but hand back the old closed state.
        entityManager.refresh(b.getMember(), LockModeType.PESSIMISTIC_WRITE);
        rules.requireCanBook(b.getMember());
        Trainer assessor = newAssessor != null ? newAssessor : rules.requireActive(b.getAssessor());
        // uq_assessment_booking_active_member is only a backstop behind requireCanBook; if it fires it is the
        // same 409, never a 500.
        try {
            return rows.row(bookings.saveAndFlush(AssessmentBooking.book(b.getMember(), slot, assessor)));
        } catch (DataIntegrityViolationException e) {
            if (Constraints.nameOf(e).filter("uq_assessment_booking_active_member"::equals).isPresent()) {
                throw rules.memberAlreadyBooked(slot);
            }
            throw e;
        }
    }

    // The lock makes the seat count safe against a trainer booking, or another reschedule, into the same slot.
    // requireSeat counts the slot's upcoming bookings, so the booking's old slot frees its seat by itself.
    private Session lockBookableSlot(UUID slotId, Instant now) {
        Session slot = sessions.findByIdForUpdate(slotId).orElseThrow(rules::slotNotFound);
        rules.requireAssessmentSlot(slot);
        rules.requireBookable(slot, now);
        rules.requireSeat(slot);
        return slot;
    }

    // Answered in the same 400 shape as a failed @Valid.
    private static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }
}
