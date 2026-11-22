package com.equestrian.equis.dto.platform;

import java.time.Instant;
import java.util.UUID;

// The answer to a platform password reset: whose link it was and when it expires (24 hours).
public record PlatformAccountReset(UUID accountId, AccountType type, String email, Instant linkExpiresAt) {
}
