package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.entity.accounts.AdminAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AdminAccountRepositoryTest {
    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    AdminAccountRepository adminAccountRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void savesWithGeneratedIdZeroFailedAttemptsAndTimestamps() {
        AdminAccount saved = adminAccountRepository.saveAndFlush(admin("admin@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFailedAttempts()).isZero();
        assertThat(saved.getLockedUntil()).isNull();
        assertThat(saved.getLastLoginAt()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findsByEmail() {
        AdminAccount saved = adminAccountRepository.saveAndFlush(admin("admin@example.com"));

        assertThat(adminAccountRepository.findByEmail("admin@example.com")).contains(saved);
        assertThat(adminAccountRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void countsAccounts() {
        long before = adminAccountRepository.count();

        adminAccountRepository.saveAndFlush(admin("admin@example.com"));

        assertThat(adminAccountRepository.count()).isEqualTo(before + 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Admin@example.com", "@example.com", "admin.example.com"})
    void rejectsMixedCaseOrMalformedEmail(String email) {
        assertThatThrownBy(() -> insert(email, "hash", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_admin_account_email");
    }

    @Test
    void rejectsNegativeFailedAttempts() {
        assertThatThrownBy(() -> insert("admin@example.com", "hash", -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_admin_account_failed_attempts");
    }

    @Test
    void rejectsMissingPassword() {
        assertThatThrownBy(() -> insert("admin@example.com", null, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("password_hash");
    }

    @Test
    void rejectsDuplicateEmail() {
        insert("admin@example.com", "hash", 0);
        assertThatThrownBy(() -> insert("admin@example.com", "other-hash", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_admin_account_email");
    }

    private AdminAccount admin(String email) {
        AdminAccount admin = new AdminAccount();
        admin.setEmail(email);
        admin.setPasswordHash("hash");
        return admin;
    }

    private void insert(String email, String passwordHash, int failedAttempts) {
        jdbcTemplate.update("INSERT INTO admin_account (email, password_hash, failed_attempts) VALUES (?, ?, ?)",
                email, passwordHash, failedAttempts);
    }
}
