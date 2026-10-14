package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.assessment.BookingCreated;
import com.equestrian.equis.dto.assessment.NewMemberRequest;
import com.equestrian.equis.service.assessment.MemberBookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// The business rules live in AssessmentRules; this only binds, validates the shape and maps the result.
@RestController
public class MemberController {
    private final MemberBookingService booking;

    public MemberController(MemberBookingService booking) {
        this.booking = booking;
    }

    @PostMapping("/me/members")
    @ResponseStatus(HttpStatus.CREATED)
    BookingCreated register(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody NewMemberRequest req) {
        return BookingCreated.of(booking.registerAndBook(me.id(), req));
    }
}
