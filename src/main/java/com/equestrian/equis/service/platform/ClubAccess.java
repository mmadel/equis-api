package com.equestrian.equis.service.platform;

import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.entity.platform.Plan;

import java.time.Instant;

// A club's access state and the subscription it comes from. reason is null when ACTIVE. plan, period and endsAt are
// null when the club has no current subscription; daysLeft is then 0.
public record ClubAccess(AccessState state, AccessReason reason, Plan plan, BillingPeriod period, Instant endsAt,
                         int daysLeft) {
}
