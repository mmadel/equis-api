package com.equestrian.equis.entity.payments;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

// Worked out from the rider's balance each time, never stored: ACTIVE above 0 sessions, AWAITING_PAYMENT at 0.
public enum RiderPaymentStatus {
    ACTIVE,
    AWAITING_PAYMENT;

    public static RiderPaymentStatus fromBalance(int sessionsLeft) {
        return sessionsLeft > 0 ? ACTIVE : AWAITING_PAYMENT;
    }

    // Sent to the apps: "active", "awaiting_payment".
    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
