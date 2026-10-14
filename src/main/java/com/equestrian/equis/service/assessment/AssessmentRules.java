package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.assessment.RecordResultRequest;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.levels.Levels;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// One method per rule; each throws the ApiException the client sees.
@Component
public class AssessmentRules {
    // "Sat 18 Oct, 10:00", as A3 shows a slot.
    private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);

    private final AssessmentBookingRepository bookings;
    private final MemberRepository members;
    private final RiderRepository riders;
    private final ZoneId schoolZone;

    public AssessmentRules(AssessmentBookingRepository bookings, MemberRepository members, RiderRepository riders,
                           @Value("${app.time-zone}") ZoneId schoolZone) {
        this.bookings = bookings;
        this.members = members;
        this.riders = riders;
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

    public void requirePhoneFree(String phone) {
        if (phone != null && members.existsByPhone(phone)) {
            throw phoneTaken();
        }
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
            case PLACED, NOSHOW -> throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.RESULT_ALREADY_RECORDED,
                    "A result is already recorded.");
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

    public ApiException phoneTaken() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.PHONE_TAKEN,
                "This phone number is already registered.",
                Map.of("fieldErrors", List.of(new ApiFieldError("phone", ErrorCodes.PHONE_TAKEN))));
    }

    public ApiException memberAlreadyBooked(Session slot) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.MEMBER_ALREADY_BOOKED,
                "Booked " + slot.startsAt(schoolZone).format(SLOT_FORMAT));
    }

    private static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }
}
