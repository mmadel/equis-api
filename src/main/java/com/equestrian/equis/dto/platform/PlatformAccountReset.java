package com.equestrian.equis.dto.platform;

import java.time.Instant;
import java.util.UUID;

// The answer to a platform password reset: whose link it was and when it expires (24 hours). In demo mode (mail off)
// there is no link: the demo password is set at once and linkExpiresAt is null (still sent, as null).
public record PlatformAccountReset(UUID accountId, AccountType type, String email, Instant linkExpiresAt) {
}
