package com.equestrian.equis.dto.club;

import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.service.platform.AccessState;

import java.time.Instant;

// GET /admin/club/usage: the club's plan and how much of it is used, for the admin's Overview card.
// riders counts people not closed (riders and applicants). A null limit means unlimited. planCode, planName,
// billingPeriod and endsAt are null when the club has no current subscription.
public record ClubUsageView(String planCode, String planName, BillingPeriod billingPeriod, Instant endsAt,
                            int daysLeft, AccessState state, Count trainers, Count riders) {

    public record Count(long used, Integer limit) {
    }
}
