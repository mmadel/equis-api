package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.assessment.RecordResultRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.levels.Levels;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// One method per rule; each throws the ApiException the client sees.
@Component
public class AssessmentRules {
    private final AssessmentBookingRepository bookings;
    private final MemberRepository members;
    private final RiderRepository riders;
    private final TrainerRepository trainers;
    private final AdminAccountRepository admins;
    private final ZoneId schoolZone;

    public AssessmentRules(AssessmentBookingRepository bookings, MemberRepository members, RiderRepository riders,
                           TrainerRepository trainers, AdminAccountRepository admins,
                           @Value("${app.time-zone}") ZoneId schoolZone) {
        this.bookings = bookings;
        this.members = members;
        this.riders = riders;
        this.trainers = trainers;
        this.admins = admins;
        this.schoolZone = schoolZone;
    }

    // ---- booking ----

    // A level session is answered like a missing one.
    public void requireAssessmentSlot(Session slot) {
        if (!slot.getTemplate().isAssessment()) {
            throw slotNotFound();
        }
    }

    // Session.isUpcoming ignores the status, so a cancelled slot is checked separately.
    public void requireBookable(Session slot, Instant now) {
        if (slot.getStatus() != SessionStatus.SCHEDULED || !slot.isUpcoming(now, schoolZone)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SLOT_CLOSED,
                    "This slot has started, ended or been cancelled.");
        }
    }

    // Call only while the slot row is locked, or two trainers can take the last seat together.
    public void requireSeat(Session slot) {
        long booked = bookings.countBySessionIdAndStatus(slot.getId(), BookingStatus.UPCOMING);
        if (booked >= slot.getCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SLOT_FULL, "This slot is full.");
        }
    }

    // Among all members, closed ones included, so someone who comes back is reopened, not entered twice.
    public void requirePhoneFree(String phone) {
        members.findByPhone(phone).ifPresent(existing -> {
            throw phoneTaken(existing);
        });
    }

    // The email becomes the rider's login later, so it must not be a trainer's or the admin's either.
    // Pass it lowercase, as it is stored.
    public void requireEmailFree(String email) {
        members.findByEmail(email).ifPresent(existing -> {
            throw emailTaken(existing);
        });
        if (trainers.existsByEmail(email) || admins.existsByEmail(email)) {
            throw emailTaken(null);
        }
    }

    // ---- assessor ----

    public Trainer requireActiveAssessor(UUID trainerId) {
        Trainer trainer = trainers.findById(trainerId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.TRAINER_NOT_FOUND, "Trainer not found.",
                Map.of("fieldErrors", List.of(new ApiFieldError("assessorId", ErrorCodes.TRAINER_NOT_FOUND)))));
        return requireActive(trainer);
    }

    public Trainer requireActive(Trainer trainer) {
        if (trainer.getStatus() != TrainerStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.TRAINER_NOT_ACTIVE,
                    trainer.getName() + " is not an active trainer. Choose another assessor.",
                    Map.of("fieldErrors", List.of(new ApiFieldError("assessorId", ErrorCodes.TRAINER_NOT_ACTIVE))));
        }
        return trainer;
    }

    // For booking a member who is already registered.
    public void requireCanBook(Member member) {
        if (riders.existsByMemberId(member.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.MEMBER_PLACED, "This member is already a rider.");
        }
        bookings.findByMemberIdAndStatus(member.getId(), BookingStatus.UPCOMING)
                .ifPresent(b -> {
                    throw memberAlreadyBooked(b.getSession());
                });
    }

    // ---- result ----

    // Answered in the same 400 shape as a failed @Valid.
    public void requireConsistentResult(RecordResultRequest req) {
        Integer levelId = req.levelId();
        if (req.attended() && !Levels.isValid(levelId)) {
            throw invalidField("levelId", ErrorCodes.LEVEL_REQUIRED);
        }
        if (!req.attended() && levelId != null) {
            throw invalidField("levelId", ErrorCodes.LEVEL_NOT_ALLOWED);
        }
    }

    // The admin may record any booking; a trainer only the ones they assess.
    public void requireCanRecord(AssessmentBooking b, AuthUser caller, Instant now) {
        // getAssessor().getId() reads the id from the lazy proxy without a query.
        if (!caller.isAdmin() && !b.getAssessor().getId().equals(caller.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.NOT_ASSESSOR,
                    "Only the assessor can record this result.");
        }
        switch (b.getStatus()) {
            case CANCELLED -> throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.BOOKING_CANCELLED,
                    "This booking was cancelled.");
            case PLACED, NOSHOW -> throw resultAlreadyRecorded();
            case UPCOMING -> { }
        }
        // Open from the start time itself, matching AssessmentBooking.isWaiting.
        if (b.getSession().startsAt(schoolZone).toInstant().isAfter(now)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SLOT_NOT_STARTED,
                    "Results can be recorded once the slot starts.");
        }
    }

    // ---- shared errors, also used when a unique key fires ----

    public ApiException slotNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.SLOT_NOT_FOUND, "Assessment slot not found.");
    }

    public ApiException bookingNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.BOOKING_NOT_FOUND, "Booking not found.");
    }

    public ApiException resultAlreadyRecorded() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.RESULT_ALREADY_RECORDED, "A result is already recorded.");
    }

    // existing is the member who has the phone; null when unknown.
    public ApiException phoneTaken(Member existing) {
        return taken(ErrorCodes.PHONE_TAKEN, "phone", "This phone", existing);
    }

    // existing is the member who has the email; null when it is a trainer's or the admin's, who are never named.
    public ApiException emailTaken(Member existing) {
        return taken(ErrorCodes.EMAIL_TAKEN, "email", "This email", existing);
    }

    public ApiException memberAlreadyBooked(Session slot) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.MEMBER_ALREADY_BOOKED,
                "Booked " + SlotLabels.describe(slot));
    }

    // Names the person, so W2 can offer to open their record (and reopen them if closed).
    private static ApiException taken(String code, String field, String what, Member existing) {
        Map<String, Object> props = new HashMap<>();
        props.put("fieldErrors", List.of(new ApiFieldError(field, code)));
        if (existing == null) {
            return new ApiException(HttpStatus.CONFLICT, code, what + " is already registered.", props);
        }
        props.put("existingMemberId", existing.getId());
        props.put("existingMemberName", existing.getName());
        props.put("existingClosed", existing.isClosed());
        String name = existing.getName();
        // "Sara A." already ends the sentence.
        String detail = what + " belongs to " + name + (name.endsWith(".") ? "" : ".");
        return new ApiException(HttpStatus.CONFLICT, code, detail, props);
    }

    private static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }
}
