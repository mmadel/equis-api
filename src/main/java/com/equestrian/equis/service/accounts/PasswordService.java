package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PasswordService {
    public static final int MIN_BYTES = 8;
    // BCrypt ignores everything after 72 bytes, so longer passwords are refused instead of cut short.
    public static final int MAX_BYTES = 72;

    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        // Nobody knows this password; the hash only exists to spend the same time as a real check.
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public String hash(String raw) {
        int bytes = raw == null ? 0 : byteLength(raw);
        if (bytes < MIN_BYTES || bytes > MAX_BYTES) {
            // Same body shape as a Bean Validation failure, so the page shows it under the password field.
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
                    "Some fields are not valid.",
                    Map.of("fieldErrors", List.of(new ApiFieldError("password",
                            "Password must be between " + MIN_BYTES + " and " + MAX_BYTES + " bytes."))));
        }
        return passwordEncoder.encode(raw);
    }

    public boolean matches(String raw, String hashOrNull) {
        if (raw == null || byteLength(raw) > MAX_BYTES) {
            return false;
        }
        if (hashOrNull == null) {
            // No such account: still run BCrypt so the answer takes as long as a wrong password.
            passwordEncoder.matches(raw, dummyHash);
            return false;
        }
        return passwordEncoder.matches(raw, hashOrNull);
    }

    private static int byteLength(String raw) {
        return raw.getBytes(StandardCharsets.UTF_8).length;
    }
}
