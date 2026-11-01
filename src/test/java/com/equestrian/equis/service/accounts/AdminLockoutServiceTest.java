package com.equestrian.equis.service.accounts;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AdminLockoutService.class, PasswordService.class, SecurityBeans.class, ClockConfig.class})
public class AdminLockoutServiceTest {
    private static final String EMAIL = "lockout@example.com";
    private static final String PASSWORD = "Admin@12345";
    // app.admin.lockout.max-attempts
    private static final int MAX_ATTEMPTS = 5;

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    AdminLockoutService adminLockoutService;
    @Autowired
    PasswordService passwordService;
    @Autowired
    AdminAccountRepository adminAccountRepository;
    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void rightPasswordClearsTheCounterAndRecordsTheLogin() {
        AdminAccount admin = admin();
        admin.setFailedAttempts(3);
        Instant before = Instant.now();

        adminLockoutService.verifyPassword(admin, PASSWORD);

        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.getLockedUntil()).isNull();
        assertThat(admin.getLastLoginAt()).isBetween(before, Instant.now());
    }

    @Test
    void wrongPasswordIsCountedAndSaysHowManyAttemptsAreLeft() {
        AdminAccount admin = admin();

        assertInvalidCredentials(admin, MAX_ATTEMPTS - 1);
        assertInvalidCredentials(admin, MAX_ATTEMPTS - 2);

        assertThat(admin.getFailedAttempts()).isEqualTo(2);
        assertThat(admin.getLockedUntil()).isNull();
        assertThat(admin.getLastLoginAt()).isNull();
    }

    @Test
    void lastAllowedWrongPasswordLocksTheAccount() {
        AdminAccount admin = admin();
        admin.setFailedAttempts(MAX_ATTEMPTS - 1);
        Instant before = Instant.now();

        assertLocked(admin, "wrong-password");

        assertThat(admin.getFailedAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(admin.getLockedUntil())
                .isBetween(before.plus(Duration.ofMinutes(15)), Instant.now().plus(Duration.ofMinutes(15)));
    }

    @Test
    void lockedAccountRefusesEvenTheRightPassword() {
        AdminAccount admin = admin();
        Instant lockedUntil = Instant.now().plus(Duration.ofMinutes(10));
        admin.setFailedAttempts(MAX_ATTEMPTS);
        admin.setLockedUntil(lockedUntil);

        assertLocked(admin, PASSWORD);

        // Nothing is counted while locked, so trying again does not push the lock further out.
        assertThat(admin.getFailedAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(admin.getLockedUntil()).isEqualTo(lockedUntil);
        assertThat(admin.getLastLoginAt()).isNull();
    }

    @Test
    void expiredLockLetsTheRightPasswordIn() {
        AdminAccount admin = admin();
        admin.setFailedAttempts(MAX_ATTEMPTS);
        admin.setLockedUntil(Instant.now().minus(Duration.ofSeconds(1)));

        adminLockoutService.verifyPassword(admin, PASSWORD);

        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.getLockedUntil()).isNull();
        assertThat(admin.getLastLoginAt()).isNotNull();
    }

    @Test
    void expiredLockGivesAFullSetOfAttemptsAgain() {
        AdminAccount admin = admin();
        admin.setFailedAttempts(MAX_ATTEMPTS);
        admin.setLockedUntil(Instant.now().minus(Duration.ofSeconds(1)));

        assertInvalidCredentials(admin, MAX_ATTEMPTS - 1);

        assertThat(admin.getFailedAttempts()).isEqualTo(1);
        assertThat(admin.getLockedUntil()).isNull();
    }

    // Runs outside the usual test transaction to prove the counter is committed even though the call throws.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void wrongPasswordIsStillSavedWhenTheErrorIsThrown() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> admin());
        try {
            // The error is caught inside the transaction, as the login task must do; the commit must still go through.
            assertThatCode(() -> tx.executeWithoutResult(status -> {
                AdminAccount admin = adminAccountRepository.findByEmailForUpdate(EMAIL).orElseThrow();
                assertInvalidCredentials(admin, MAX_ATTEMPTS - 1);
            })).doesNotThrowAnyException();

            assertThat(adminAccountRepository.findByEmail(EMAIL).orElseThrow().getFailedAttempts()).isEqualTo(1);
        } finally {
            tx.executeWithoutResult(status -> adminAccountRepository.findByEmail(EMAIL)
                    .ifPresent(adminAccountRepository::delete));
        }
    }

    private AdminAccount admin() {
        AdminAccount admin = new AdminAccount();
        admin.setEmail(EMAIL);
        admin.setPasswordHash(passwordService.hash(PASSWORD));
        return adminAccountRepository.saveAndFlush(admin);
    }

    private void assertInvalidCredentials(AdminAccount admin, int attemptsLeft) {
        assertThatThrownBy(() -> adminLockoutService.verifyPassword(admin, "wrong-password"))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.INVALID_CREDENTIALS);
                    assertThat(ex.properties()).containsEntry("attemptsLeft", attemptsLeft);
                });
    }

    private void assertLocked(AdminAccount admin, String rawPassword) {
        assertThatThrownBy(() -> adminLockoutService.verifyPassword(admin, rawPassword))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.LOCKED);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.ACCOUNT_LOCKED);
                    assertThat(ex.properties()).doesNotContainKey("attemptsLeft");
                });
    }
}
