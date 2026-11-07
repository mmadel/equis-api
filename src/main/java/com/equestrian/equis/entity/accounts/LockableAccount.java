package com.equestrian.equis.entity.accounts;

import java.time.Instant;

// An account with a password and the 5-wrong-tries lock (AdminLockoutService): the club admin and the super admin.
public interface LockableAccount {
    String getPasswordHash();

    int getFailedAttempts();

    void setFailedAttempts(int failedAttempts);

    Instant getLockedUntil();

    void setLockedUntil(Instant lockedUntil);

    void setLastLoginAt(Instant lastLoginAt);
}
