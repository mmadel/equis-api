package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SecurityBeans.class)
public class SeedDataTest {
    private static final UUID TRAINER_ONE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRAINER_TWO_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String TRAINER_ONE_PASSWORD = "Trainer@123";
    private static final String TRAINER_TWO_INVITE_TOKEN = "dev-invite-token-trainer2-0000000000000000000000";

    // Mixed case on purpose: V5 must store it lowercase.
    private static final String ADMIN_EMAIL = "Admin.Test@Example.com";
    private static final String ADMIN_PASSWORD = "Admin@123";

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    // Stand in for APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH so the test does not depend on the environment.
    @DynamicPropertySource
    static void adminPlaceholders(DynamicPropertyRegistry registry) {
        String adminPasswordHash = new BCryptPasswordEncoder().encode(ADMIN_PASSWORD);
        registry.add("spring.flyway.placeholders.adminEmail", () -> ADMIN_EMAIL);
        registry.add("spring.flyway.placeholders.adminPasswordHash", () -> adminPasswordHash);
    }

    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    InviteRepository inviteRepository;
    @Autowired
    AdminAccountRepository adminAccountRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void seedsTheTwoTestTrainers() {
        assertThat(trainerRepository.count()).isEqualTo(2);

        Trainer one = trainerRepository.findById(TRAINER_ONE_ID).orElseThrow();
        assertThat(one.getName()).isEqualTo("Test Trainer One");
        assertThat(one.getPhone()).isEqualTo("01000000001");
        assertThat(one.getEmail()).isEqualTo("trainer1@example.com");
        assertThat(one.getStatus()).isEqualTo(TrainerStatus.ACTIVE);

        Trainer two = trainerRepository.findById(TRAINER_TWO_ID).orElseThrow();
        assertThat(two.getName()).isEqualTo("Test Trainer Two");
        assertThat(two.getPhone()).isEqualTo("01000000002");
        assertThat(two.getEmail()).isEqualTo("trainer2@example.com");
        assertThat(two.getStatus()).isEqualTo(TrainerStatus.INVITED);
        assertThat(two.getPasswordHash()).isNull();
    }

    @Test
    void trainerOnePasswordHashMatchesTheDocumentedPassword() {
        Trainer one = trainerRepository.findById(TRAINER_ONE_ID).orElseThrow();

        assertThat(passwordEncoder.matches(TRAINER_ONE_PASSWORD, one.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", one.getPasswordHash())).isFalse();
    }

    @Test
    void seedsOneOpenInviteForTrainerTwo() {
        List<Invite> invites = inviteRepository.findAll();

        assertThat(invites).hasSize(1);
        Invite invite = invites.getFirst();
        assertThat(invite.getTrainer().getId()).isEqualTo(TRAINER_TWO_ID);
        assertThat(invite.getPurpose()).isEqualTo(InvitePurpose.INVITE);
        assertThat(invite.getAcceptedAt()).isNull();
        assertThat(invite.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void inviteTokenHashIsTheSha256OfTheDocumentedToken() {
        Invite invite = inviteRepository.findAll().getFirst();

        assertThat(invite.getTokenHash()).isEqualTo(sha256Hex(TRAINER_TWO_INVITE_TOKEN));
        assertThat(inviteRepository.findByTokenHash(sha256Hex(TRAINER_TWO_INVITE_TOKEN))).contains(invite);
    }

    @Test
    void seedsTheAdminWithLowercasedEmailAndGivenPasswordHash() {
        List<AdminAccount> admins = adminAccountRepository.findAll();

        assertThat(admins).hasSize(1);
        AdminAccount admin = admins.getFirst();
        assertThat(admin.getEmail()).isEqualTo(ADMIN_EMAIL.toLowerCase());
        assertThat(adminAccountRepository.findByEmail("admin.test@example.com")).contains(admin);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();
        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.getLockedUntil()).isNull();
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
