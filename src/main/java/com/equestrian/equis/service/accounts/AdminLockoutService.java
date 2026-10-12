package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.config.AuthProperties;
import com.equestrian.equis.common.config.AuthProperties.Lockout;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.AdminAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Service
public class AdminLockoutService {
    private final PasswordService passwordService;
    private final AuthProperties properties;
    private final Clock clock;

    public AdminLockoutService(PasswordService passwordService, AuthProperties properties, Clock clock) {
        this.passwordService = passwordService;
        this.properties = properties;
        this.clock = clock;
    }

    // The admin must come from findByEmailForUpdate in the caller's transaction, so the counter is saved.
    // noRollbackFor keeps the counter when the error is thrown; otherwise a wrong guess would never be counted.
    @Transactional(noRollbackFor = ApiException.class)
    public void verifyPassword(AdminAccount admin, String rawPassword) {
        Lockout lockout = properties.admin().lockout();
        Instant now = clock.instant();

        if (admin.getLockedUntil() != null) {
            if (admin.getLockedUntil().isAfter(now)) {
                // While locked nothing is checked or counted, so even the right password is refused.
                throw locked();
            }
            // The lock has run out: start again with a full set of attempts.
            admin.setLockedUntil(null);
            admin.setFailedAttempts(0);
        }

        if (!passwordService.matches(rawPassword, admin.getPasswordHash())) {
            int attempts = admin.getFailedAttempts() + 1;
            admin.setFailedAttempts(attempts);
            if (attempts >= lockout.maxAttempts()) {
                admin.setLockedUntil(now.plus(lockout.duration()));
                throw locked();
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS,
                    "Wrong email or password.", Map.of("attemptsLeft", lockout.maxAttempts() - attempts));
        }

        admin.setFailedAttempts(0);
        admin.setLastLoginAt(now);
    }

    private static ApiException locked() {
        return new ApiException(HttpStatus.LOCKED, ErrorCodes.ACCOUNT_LOCKED,
                "Too many wrong attempts. Try again later.");
    }
}
