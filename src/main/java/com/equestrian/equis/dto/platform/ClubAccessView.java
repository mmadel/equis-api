package com.equestrian.equis.dto.platform;

import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.service.platform.AccessReason;
import com.equestrian.equis.service.platform.AccessState;

import java.time.Instant;

// A club's access state after a change, so the page can show it straight away. reason is null when ACTIVE;
// planCode, billingPeriod and endsAt are null when the club has no current subscription.
public record ClubAccessView(AccessState state, AccessReason reason, String planCode, BillingPeriod billingPeriod,
                             Instant endsAt, int daysLeft) {
}
