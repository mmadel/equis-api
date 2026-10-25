package com.equestrian.equis.service.payments;

import com.equestrian.equis.entity.payments.Payment;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.LocalDate;
import java.util.Locale;

// Where a package stands on a day. Worked out each time, never stored.
public enum PaymentState {
    UPCOMING,
    ACTIVE,
    USED_UP,
    EXPIRED,
    VOIDED;

    // Checked in this order: voided, not started yet, past its grace month, nothing left, otherwise active.
    public static PaymentState of(Payment p, int used, LocalDate today) {
        if (p.isVoided()) {
            return VOIDED;
        }
        if (today.isBefore(p.getStartMonth())) {
            return UPCOMING;
        }
        if (today.isAfter(p.getValidUntil())) {
            return EXPIRED;
        }
        if (used >= p.getSessions()) {
            return USED_UP;
        }
        return ACTIVE;
    }

    // Sessions still to use: only an upcoming or active package has any.
    public int left(Payment p, int used) {
        return this == UPCOMING || this == ACTIVE ? Math.max(0, p.getSessions() - used) : 0;
    }

    // Sent to the apps: "upcoming", "used_up", ...
    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
