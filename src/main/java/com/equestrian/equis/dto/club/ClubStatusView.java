package com.equestrian.equis.dto.club;

import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.service.platform.AccessReason;
import com.equestrian.equis.service.platform.AccessState;

import java.time.Instant;

// GET /club/status: what the apps need for their subscription banners. reason is null when ACTIVE; planName,
// billingPeriod and endsAt are null when the club has no current subscription, and daysLeft is then 0.
public record ClubStatusView(String clubName, String clubCode, AccessState state, AccessReason reason,
                             String planName, BillingPeriod billingPeriod, Instant endsAt, int daysLeft) {
}
