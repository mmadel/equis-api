package com.equestrian.equis.dto.accounts;

import java.time.Instant;

// The trainer's unused link. purpose is "invite" or "reset"; expired is worked out from the clock.
public record PendingLink(String purpose, Instant expiresAt, boolean expired) {
}
