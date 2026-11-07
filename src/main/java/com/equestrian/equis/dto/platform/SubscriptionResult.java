package com.equestrian.equis.dto.platform;

// The subscription that was added or voided, and the club's access state after it.
public record SubscriptionResult(SubscriptionView subscription, ClubAccessView access) {
}
