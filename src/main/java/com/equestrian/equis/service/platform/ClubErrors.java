package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

final class ClubErrors {
    private ClubErrors() {}

    static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.CLUB_NOT_FOUND, "This club doesn't exist.");
    }

    static ApiException codeTaken() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.CLUB_CODE_TAKEN, "This club code is already taken.");
    }

    static ApiException noPlans() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.NO_PLANS_CONFIGURED,
                "No plans configured. Run ops/upsert-plan.sql first.");
    }

    static ApiException invalidStatus(String message) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.INVALID_CLUB_STATUS, message);
    }

    // Same body shape as a Bean Validation failure, so the page shows it under the field.
    static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }
}
