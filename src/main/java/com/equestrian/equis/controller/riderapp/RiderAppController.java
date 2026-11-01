package com.equestrian.equis.controller.riderapp;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.riderapp.RiderAssessment;
import com.equestrian.equis.dto.riderapp.RiderMe;
import com.equestrian.equis.dto.riderapp.RiderPayments;
import com.equestrian.equis.dto.riderapp.RiderProgress;
import com.equestrian.equis.dto.riderapp.RiderSessionPage;
import com.equestrian.equis.service.riderapp.RiderAppQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The rider app. Read-only, and the rider is always the one in the token: there is no rider id in any path.
@RestController
@RequestMapping("/rider")
public class RiderAppController {
    static final int MAX_PAGE_SIZE = 50;

    private final RiderAppQueryService service;

    public RiderAppController(RiderAppQueryService service) {
        this.service = service;
    }

    // Home and Profile.
    @GetMapping("/me")
    RiderMe me(@AuthenticationPrincipal AuthUser me) {
        return service.me(me.id());
    }

    // History: the rides, newest first, a page at a time.
    @GetMapping("/sessions")
    RiderSessionPage sessions(@AuthenticationPrincipal AuthUser me,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        requireValidPage(page, size);
        return service.sessions(me.id(), page, size);
    }

    // History: the assessment the rider was placed at.
    @GetMapping("/assessment")
    RiderAssessment assessment(@AuthenticationPrincipal AuthUser me) {
        return service.assessment(me.id());
    }

    @GetMapping("/payments")
    RiderPayments payments(@AuthenticationPrincipal AuthUser me) {
        return service.payments(me.id());
    }

    @GetMapping("/progress")
    RiderProgress progress(@AuthenticationPrincipal AuthUser me) {
        return service.progress(me.id());
    }

    // Checked here so a bad value answers like any other invalid field: 400 validation_failed with fieldErrors.
    private static void requireValidPage(int page, int size) {
        List<ApiFieldError> errors = new ArrayList<>();
        if (page < 0) {
            errors.add(new ApiFieldError("page", "must be 0 or more"));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            errors.add(new ApiFieldError("size", "must be between 1 and " + MAX_PAGE_SIZE));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
                    "Some fields are not valid.", Map.of("fieldErrors", List.copyOf(errors)));
        }
    }
}
