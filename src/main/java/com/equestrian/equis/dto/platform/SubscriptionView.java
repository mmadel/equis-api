package com.equestrian.equis.dto.platform;

import com.equestrian.equis.entity.platform.BillingPeriod;

import java.time.Instant;
import java.util.UUID;

// One row of a club's subscription history. voidedAt and voidReason are set only on VOIDED rows.
public record SubscriptionView(UUID id, String planCode, String planName, BillingPeriod billingPeriod,
                               Instant startsAt, Instant endsAt, String note, Instant createdAt,
                               SubscriptionStatus status, Instant voidedAt, String voidReason) {
}
