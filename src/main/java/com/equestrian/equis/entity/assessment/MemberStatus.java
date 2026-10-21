package com.equestrian.equis.entity.assessment;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

// Where a person is in the intake (W2). Not stored: only closed_at is saved, the rest
// is worked out from the bookings by MemberStatusResolver.
public enum MemberStatus {
    PENDING_ASSESSMENT,
    NO_SHOW,
    RIDER,
    CLOSED;

    // Sent to the apps: "pending_assessment", "no_show", ...
    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
