package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.Trainer;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.Map;

// The errors of the admin's trainer endpoints, shared by every E6 task.
public final class TrainerErrors {
    private TrainerErrors() {}

    public static ApiException phoneTaken() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.PHONE_TAKEN,
                "This phone number is already registered.",
                Map.of("fieldErrors", List.of(new ApiFieldError("phone", ErrorCodes.PHONE_TAKEN))));
    }

    // The same answer whoever holds the email, so the admin's address is never revealed.
    public static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.EMAIL_TAKEN,
                "This email is already registered.",
                Map.of("fieldErrors", List.of(new ApiFieldError("email", ErrorCodes.EMAIL_TAKEN))));
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.TRAINER_NOT_FOUND, "Trainer not found.");
    }

    public static ApiException invalidStatus(Trainer trainer, String action) {
        String status = trainer.getStatus().name().toLowerCase(Locale.ROOT);
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.INVALID_TRAINER_STATUS,
                "Cannot " + action + " a trainer who is " + status + ".",
                Map.of("status", status, "action", action));
    }

    public static ApiException inUse(Trainer trainer) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.TRAINER_IN_USE,
                "This trainer already has riders or assessments. Disable them instead.");
    }
}
