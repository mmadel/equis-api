package com.equestrian.equis.service.platform;

// Why a club is not ACTIVE.
public enum AccessReason {
    // The super admin suspended the club.
    SUSPENDED,
    // The club has no current subscription.
    NO_SUBSCRIPTION,
    // The current subscription ended and its plan blocks on expiry.
    TRIAL_ENDED,
    // The current subscription ended and its plan turns read-only on expiry.
    SUBSCRIPTION_ENDED
}
