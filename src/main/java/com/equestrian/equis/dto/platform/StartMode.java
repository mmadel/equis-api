package com.equestrian.equis.dto.platform;

// When a new subscription starts.
public enum StartMode {
    // Now, replacing the current one from this moment (an upgrade). The older row is not changed.
    NOW,
    // When the club's last covered day ends, or now if it has already expired (a renewal).
    AFTER_CURRENT
}
