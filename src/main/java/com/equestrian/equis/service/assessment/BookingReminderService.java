package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.notices.Notice;
import com.equestrian.equis.entity.notices.NoticeType;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.notices.NoticeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

// W2's Remind trainer: an in-app notice to the assessor of a booking waiting for its result. No email.
// The checks match BookingActions: remind is offered only on a waiting row with an active assessor.
@Service
public class BookingReminderService {
    private static final String UNREAD_REMINDER = "uq_notice_unread_reminder";

    private final AssessmentBookingRepository bookings;
    private final NoticeRepository notices;
    private final AssessmentRules rules;
    private final AdminBookingRowAssembler rows;
    private final Clock clock;
    private final ZoneId schoolZone;

    public BookingReminderService(AssessmentBookingRepository bookings, NoticeRepository notices,
                                  AssessmentRules rules, AdminBookingRowAssembler rows,
                                  Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.bookings = bookings;
        this.notices = notices;
        this.rules = rules;
        this.rows = rows;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // While the assessor hasn't read a reminder for this booking, reminding again creates nothing.
    @Transactional
    public AdminBookingRow remind(UUID bookingId) {
        Instant now = clock.instant();

        // The lock runs two clicks one after the other, and against the assessor recording the result.
        AssessmentBooking b = bookings.findByIdForUpdate(bookingId).orElseThrow(rules::bookingNotFound);
        if (!b.isWaiting(now, schoolZone)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.RESULT_NOT_OVERDUE,
                    "Only bookings waiting for a result can be reminded.");
        }
        Trainer assessor = b.getAssessor();
        // A disabled trainer can't open the app, so the reminder would never be seen.
        if (assessor.getStatus() != TrainerStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.TRAINER_NOT_ACTIVE,
                    assessor.getName() + " is not an active trainer. Change the assessor first.");
        }

        if (!notices.existsByTrainerIdAndTypeAndRefIdAndReadAtIsNull(
                assessor.getId(), NoticeType.RESULT_OVERDUE, b.getId())) {
            String message = "Result overdue: " + b.getMember().getName()
                    + " (assessment " + SlotLabels.describe(b.getSession()) + "). Record it in Assessment results.";
            // The booking lock means the unique index should never fire; it is only a backstop.
            try {
                notices.saveAndFlush(Notice.resultOverdue(assessor, b.getId(), message, now));
            } catch (DataIntegrityViolationException e) {
                if (Constraints.nameOf(e).filter(UNREAD_REMINDER::equals).isEmpty()) {
                    throw e;
                }
            }
        }
        return rows.row(b);
    }
}
