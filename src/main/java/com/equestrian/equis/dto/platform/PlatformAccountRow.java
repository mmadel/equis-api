package com.equestrian.equis.dto.platform;

import java.time.Instant;
import java.util.UUID;

// One account of a club on the platform's Accounts tab. email is null for a rider without one (seed riders); phone is
// always null for admins; lastSignInAt is null when the account never signed in.
public record PlatformAccountRow(UUID id, AccountType type, String name, String email, String phone,
                                 PlatformAccountStatus status, Instant lastSignInAt) {
}
