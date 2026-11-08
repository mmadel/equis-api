package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

// 403 subscription_expired: the club is read-only, so a request that changes data is refused. endsAt is when the
// subscription ended.
public class SubscriptionExpiredException extends ApiException {
    public SubscriptionExpiredException(Instant endsAt) {
        super(HttpStatus.FORBIDDEN, ErrorCodes.SUBSCRIPTION_EXPIRED,
                "Your club's subscription has ended. You can still see everything, but not make changes.",
                endsAt == null ? Map.of() : Map.of("endsAt", endsAt));
    }
}
