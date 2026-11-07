package com.equestrian.equis.dto.platform;

// Where a row of a club's subscription history stands now. Computed, never stored.
public enum SubscriptionStatus {
    // The club's current subscription, still running.
    CURRENT,
    // Not started yet (a queued renewal).
    UPCOMING,
    // Started before the current one and not over yet, e.g. after an upgrade. It comes back if the current one is
    // voided.
    REPLACED,
    ENDED,
    VOIDED
}
