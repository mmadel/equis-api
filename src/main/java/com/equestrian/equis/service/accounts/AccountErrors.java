package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

// Errors shared by every kind of account (trainer, admin, rider).
public final class AccountErrors {
    private AccountErrors() {}

    // One email belongs to one kind of account: a person (future rider) on one side, a trainer or the admin on the
    // other. Never says who holds it.
    public static ApiException emailInUse() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.EMAIL_IN_USE,
                "This email is already used by another account.",
                Map.of("fieldErrors", List.of(new ApiFieldError("email", ErrorCodes.EMAIL_IN_USE))));
    }
}
