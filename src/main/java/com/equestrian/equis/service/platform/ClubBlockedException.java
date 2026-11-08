package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.util.Map;

// 403 club_blocked: the club can't be used (trial ended, suspended or no subscription). reason says which.
public class ClubBlockedException extends ApiException {
    public ClubBlockedException(AccessReason reason) {
        super(HttpStatus.FORBIDDEN, ErrorCodes.CLUB_BLOCKED, messageFor(reason), Map.of("reason", reason.name()));
    }

    private static String messageFor(AccessReason reason) {
        return switch (reason) {
            case TRIAL_ENDED -> "Your club's trial has ended.";
            case SUSPENDED -> "Your club has been suspended.";
            case NO_SUBSCRIPTION, SUBSCRIPTION_ENDED -> "Your club has no active subscription.";
        };
    }
}
