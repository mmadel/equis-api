package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.CancelBookingRequest;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.CancelReason;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// W2's Cancel: an upcoming assessment is cancelled with a reason, which frees its seat. The person is then
// waiting for an assessment again and the row offers Rebook. The checks match BookingActions.
// No notice goes to the trainer: the booking simply leaves their slot list.
@Service
public class AdminBookingCancelService {
    private final AssessmentBookingRepository bookings;
    private final AssessmentRules rules;
    private final AdminBookingRowAssembler rows;
    private final Clock clock;
    private final ZoneId schoolZone;

    public AdminBookingCancelService(AssessmentBookingRepository bookings, AssessmentRules rules,
                                     AdminBookingRowAssembler rows, Clock clock,
                                     @Value("${app.time-zone}") ZoneId schoolZone) {
        this.bookings = bookings;
        this.rules = rules;
        this.rows = rows;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    @Transactional
    public AdminBookingRow cancel(UUID bookingId, CancelBookingRequest req) {
        // The request is checked before the booking is read, so a bad request never takes the lock.
        CancelReason reason = CancelReason.fromCode(req.reason())
                .orElseThrow(() -> invalidField("reason", ErrorCodes.INVALID_REASON));
        if (reason == CancelReason.OTHER && req.reasonText() == null) {
            throw invalidField("reasonText", ErrorCodes.REASON_TEXT_REQUIRED);
        }
        if (reason != CancelReason.OTHER && req.reasonText() != null) {
            throw invalidField("reasonText", ErrorCodes.REASON_TEXT_NOT_ALLOWED);
        }
        Instant now = clock.instant();

        // The lock serialises this with the assessor recording a result on the same booking.
        AssessmentBooking b = bookings.findByIdForUpdate(bookingId).orElseThrow(rules::bookingNotFound);
        rules.requireNotClosed(b.getMember());
        String shown = b.displayStatus(now, schoolZone);
        boolean assessorActive = b.getAssessor().getStatus() == TrainerStatus.ACTIVE;
        if (!BookingActions.allowed(shown, assessorActive, false).contains(BookingAction.CANCEL)) {
            throw notCancellable(shown);
        }

        b.cancelWithReason(reason, req.reasonText(), now);
        return rows.row(b);
    }

    private static ApiException notCancellable(String shown) {
        return switch (shown) {
            case "waiting" -> new ApiException(HttpStatus.CONFLICT, ErrorCodes.SLOT_STARTED,
                    "This assessment has started. Record a result or no-show first.");
            case "placed" -> new ApiException(HttpStatus.CONFLICT, ErrorCodes.BOOKING_CLOSED,
                    "A placed booking can't be cancelled.");
            default -> new ApiException(HttpStatus.CONFLICT, ErrorCodes.BOOKING_NOT_UPCOMING,
                    "Only an upcoming assessment can be cancelled.");
        };
    }

    // Answered in the same 400 shape as a failed @Valid.
    private static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }
}
