package com.equestrian.equis.service.club;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;

import java.util.Map;

// 409 plan_limit_reached: the club already has as many trainers, or people, as its plan allows. Nothing was saved.
public class PlanLimitReachedException extends ApiException {
    public PlanLimitReachedException(LimitedResource resource, int limit, long used) {
        super(HttpStatus.CONFLICT, ErrorCodes.PLAN_LIMIT_REACHED, detail(resource, limit),
                Map.of("resource", resource.name(), "limit", limit, "used", used));
    }

    private static String detail(LimitedResource resource, int limit) {
        return switch (resource) {
            case TRAINERS -> "Your plan allows " + limit + " trainers.";
            case RIDERS -> "Your plan allows " + limit + " riders and applicants.";
        };
    }
}
