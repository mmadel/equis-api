package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PasswordServiceTest {
    private final CountingEncoder encoder = new CountingEncoder();
    private final PasswordService passwordService = new PasswordService(encoder);

    @Test
    void hashIsAPlainBcryptHashThatMatchesTheRawPassword() {
        String hash = passwordService.hash("Trainer@123");

        assertThat(hash).startsWith("$2a$").doesNotContain("{bcrypt}");
        assertThat(passwordService.matches("Trainer@123", hash)).isTrue();
    }

    @Test
    void hashAcceptsTheShortestAndLongestAllowedPasswords() {
        String shortest = "a".repeat(PasswordService.MIN_BYTES);
        String longest = "a".repeat(PasswordService.MAX_BYTES);

        assertThat(passwordService.matches(shortest, passwordService.hash(shortest))).isTrue();
        assertThat(passwordService.matches(longest, passwordService.hash(longest))).isTrue();
    }

    @Test
    void hashRejectsPasswordsOutsideTheByteRange() {
        assertValidationFailed(null);
        assertValidationFailed("a".repeat(PasswordService.MIN_BYTES - 1));
        assertValidationFailed("a".repeat(PasswordService.MAX_BYTES + 1));
    }

    @Test
    void lengthIsCountedInUtf8BytesNotCharacters() {
        // "é" is two bytes: 4 characters reach the minimum, 37 characters go over the maximum.
        assertThat(passwordService.hash("é".repeat(4))).isNotBlank();
        assertValidationFailed("é".repeat(37));
    }

    @Test
    void matchesIsFalseForAWrongPassword() {
        String hash = passwordService.hash("Trainer@123");

        assertThat(passwordService.matches("Trainer@124", hash)).isFalse();
    }

    @Test
    void matchesIsFalseForAnOverlongPasswordWithoutCallingBcrypt() {
        String hash = passwordService.hash("a".repeat(PasswordService.MAX_BYTES));
        encoder.matchCalls = 0;

        assertThat(passwordService.matches("a".repeat(PasswordService.MAX_BYTES + 1), hash)).isFalse();
        assertThat(passwordService.matches(null, hash)).isFalse();
        assertThat(encoder.matchCalls).isZero();
    }

    @Test
    void matchesIsFalseForAMissingAccountButStillRunsBcrypt() {
        assertThat(passwordService.matches("Trainer@123", null)).isFalse();
        assertThat(encoder.matchCalls).isEqualTo(1);
    }

    private void assertValidationFailed(String raw) {
        assertThatThrownBy(() -> passwordService.hash(raw))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.VALIDATION_FAILED);
                });
    }

    // Real BCrypt, with a count of the checks so the tests can tell whether one was made.
    private static class CountingEncoder implements PasswordEncoder {
        private final PasswordEncoder delegate = new BCryptPasswordEncoder();
        int matchCalls;

        @Override
        public String encode(CharSequence rawPassword) {
            return delegate.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            matchCalls++;
            return delegate.matches(rawPassword, encodedPassword);
        }
    }
}
