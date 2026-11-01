package com.equestrian.equis.service.accounts;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.config.AuthProperties;
import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.InviteAcceptRequest;
import com.equestrian.equis.dto.accounts.LoginRequest;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableConfigurationProperties(AuthProperties.class)
@Import({AuthService.class, AdminLockoutService.class, PasswordService.class, AuthTokenService.class,
        AccountLinkService.class, JwtService.class, TokenGenerator.class, JwtConfig.class, SecurityBeans.class,
        ClockConfig.class})
public class AuthServiceTest {
    private static final String ADMIN_EMAIL = "auth-admin@example.com";
    private static final String PASSWORD = "Trainer@12345";
    // app.admin.lockout.max-attempts
    private static final int MAX_ATTEMPTS = 5;

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    AuthService authService;
    @Autowired
    PasswordService passwordService;
    @Autowired
    AccountLinkService accountLinkService;
    @Autowired
    AuthTokenService authTokenService;
    @Autowired
    AdminAccountRepository adminAccountRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    EntityManager entityManager;
    @Autowired
    PlatformTransactionManager transactionManager;

    // V4 seeds dev trainers and an invite, and V12 sample riders of trainer 1; clear them so each test starts from
    // empty tables (rolled back afterwards). Riders go first: they point at their trainer.
    @BeforeEach
    void clearSeededTrainers() {
        jdbcTemplate.update("DELETE FROM rider");
        jdbcTemplate.update("DELETE FROM trainer");
    }

    @Test
    void adminSignsInWithEmailInAnyCaseAndSpacesAround() {
        AdminAccount admin = admin();

        AuthTokens tokens = authService.login(new LoginRequest("  Auth-Admin@Example.com ", PASSWORD, false));

        assertThat(tokens.role()).isEqualTo(Role.ADMIN);
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(admin.getLastLoginAt()).isNotNull();
    }

    @Test
    void adminWrongPasswordIsCountedAndSaysHowManyAttemptsAreLeft() {
        AdminAccount admin = admin();

        assertThatThrownBy(() -> authService.login(new LoginRequest(ADMIN_EMAIL, "wrong-password", false)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.INVALID_CREDENTIALS);
                    assertThat(ex.properties()).containsEntry("attemptsLeft", MAX_ATTEMPTS - 1);
                });

        assertThat(admin.getFailedAttempts()).isEqualTo(1);
    }

    // Runs outside the usual test transaction to prove the counter is committed even though login throws.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void adminWrongPasswordIsStillSavedWhenTheErrorIsThrown() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> admin());
        try {
            assertThatThrownBy(() -> authService.login(new LoginRequest(ADMIN_EMAIL, "wrong-password", false)))
                    .isInstanceOf(ApiException.class);

            assertThat(adminAccountRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getFailedAttempts())
                    .isEqualTo(1);
        } finally {
            tx.executeWithoutResult(status -> adminAccountRepository.findByEmail(ADMIN_EMAIL)
                    .ifPresent(adminAccountRepository::delete));
        }
    }

    @Test
    void trainerSignsInWithEmail() {
        Trainer sara = activeSara();
        Instant before = Instant.now();

        AuthTokens tokens = authService.login(new LoginRequest(" Sara@Example.com", PASSWORD, true));

        assertThat(tokens.role()).isEqualTo(Role.TRAINER);
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(sara.getLastActiveAt()).isBetween(before, Instant.now());
    }

    @Test
    void trainerSignsInWithPhoneWrittenWithSpaces() {
        Trainer sara = activeSara();

        AuthTokens tokens = authService.login(new LoginRequest(" 010 0000 0001 ", PASSWORD, false));

        assertThat(tokens.role()).isEqualTo(Role.TRAINER);
        assertThat(sara.getLastActiveAt()).isNotNull();
    }

    @Test
    void unknownLoginWrongPasswordAndUnsetPasswordGiveTheSameAnswer() {
        Trainer sara = activeSara();
        // Invited, no password yet.
        trainer("Omar Hassan", "01000000002", "omar@example.com");

        assertInvalidCredentials("nobody@example.com", PASSWORD);
        assertInvalidCredentials("01099999999", PASSWORD);
        assertInvalidCredentials("sara@example.com", "wrong-password");
        assertInvalidCredentials("01000000001", "wrong-password");
        assertInvalidCredentials("omar@example.com", PASSWORD);

        assertThat(sara.getLastActiveAt()).isNull();
    }

    @Test
    void disabledTrainerLearnsItOnlyWithTheRightPassword() {
        Trainer sara = activeSara();
        sara.setStatus(TrainerStatus.DISABLED);

        assertInvalidCredentials("sara@example.com", "wrong-password");
        assertThatThrownBy(() -> authService.login(new LoginRequest("sara@example.com", PASSWORD, false)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.ACCOUNT_DISABLED);
                });

        assertThat(sara.getLastActiveAt()).isNull();
    }

    @Test
    void acceptInviteSetsThePasswordActivatesTheTrainerAndUsesUpTheLink() {
        Trainer sara = trainer("Sara Ali", "01000000001", "sara@example.com");
        String raw = accountLinkService.create(sara, InvitePurpose.INVITE);

        authService.acceptInvite(new InviteAcceptRequest(raw, PASSWORD));

        assertThat(sara.getStatus()).isEqualTo(TrainerStatus.ACTIVE);
        assertThat(passwordService.matches(PASSWORD, sara.getPasswordHash())).isTrue();
        assertInvalidLink(raw, PASSWORD);
        assertThat(authService.login(new LoginRequest("sara@example.com", PASSWORD, false)).role())
                .isEqualTo(Role.TRAINER);
    }

    @Test
    void acceptInviteWithAShortPasswordSavesNothingAndKeepsTheLinkUsable() {
        Trainer sara = trainer("Sara Ali", "01000000001", "sara@example.com");
        String raw = accountLinkService.create(sara, InvitePurpose.INVITE);

        assertThatThrownBy(() -> authService.acceptInvite(new InviteAcceptRequest(raw, "short")))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.VALIDATION_FAILED);
                    assertThat(ex.properties().get("fieldErrors"))
                            .asInstanceOf(InstanceOfAssertFactories.list(ApiFieldError.class))
                            .extracting(ApiFieldError::field)
                            .containsExactly("password");
                });

        assertThat(sara.getStatus()).isEqualTo(TrainerStatus.INVITED);
        assertThat(sara.getPasswordHash()).isNull();
        assertThat(accountLinkService.openLink(raw).getAcceptedAt()).isNull();
    }

    @Test
    void badLinkIsReportedBeforeAShortPassword() {
        assertInvalidLink("no-such-token", "short");
    }

    @Test
    void acceptInviteRefusesADisabledTrainer() {
        Trainer sara = activeSara();
        String oldHash = sara.getPasswordHash();
        String raw = accountLinkService.create(sara, InvitePurpose.RESET);
        sara.setStatus(TrainerStatus.DISABLED);

        assertInvalidLink(raw, "New@Password1");

        assertThat(sara.getPasswordHash()).isEqualTo(oldHash);
    }

    @Test
    void resetLinkChangesThePasswordAndSignsOutEverywhere() {
        Trainer sara = activeSara();
        authTokenService.issue(Role.TRAINER, sara.getId(), true);
        authTokenService.issue(Role.TRAINER, sara.getId(), false);
        entityManager.flush();
        assertThat(activeRefreshTokens(sara.getId())).isEqualTo(2);
        String raw = accountLinkService.create(sara, InvitePurpose.RESET);

        authService.acceptInvite(new InviteAcceptRequest(raw, "New@Password1"));
        entityManager.flush();

        assertThat(sara.getStatus()).isEqualTo(TrainerStatus.ACTIVE);
        assertThat(passwordService.matches("New@Password1", sara.getPasswordHash())).isTrue();
        assertThat(activeRefreshTokens(sara.getId())).isZero();
    }

    @Test
    void inviteLinkDoesNotRevokeSessions() {
        Trainer sara = activeSara();
        authTokenService.issue(Role.TRAINER, sara.getId(), false);
        String raw = accountLinkService.create(sara, InvitePurpose.INVITE);

        authService.acceptInvite(new InviteAcceptRequest(raw, "New@Password1"));
        entityManager.flush();

        assertThat(activeRefreshTokens(sara.getId())).isEqualTo(1);
    }

    private int activeRefreshTokens(UUID trainerId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_token WHERE subject_id = ? AND revoked_at IS NULL",
                Integer.class, trainerId);
    }

    private void assertInvalidCredentials(String login, String password) {
        assertThatThrownBy(() -> authService.login(new LoginRequest(login, password, false)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.INVALID_CREDENTIALS);
                    assertThat(ex.properties()).isEmpty();
                });
    }

    private void assertInvalidLink(String raw, String password) {
        assertThatThrownBy(() -> authService.acceptInvite(new InviteAcceptRequest(raw, password)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.INVALID_LINK);
                });
    }

    private AdminAccount admin() {
        AdminAccount admin = new AdminAccount();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordService.hash(PASSWORD));
        return adminAccountRepository.saveAndFlush(admin);
    }

    private Trainer activeSara() {
        Trainer sara = trainer("Sara Ali", "01000000001", "sara@example.com");
        sara.setPasswordHash(passwordService.hash(PASSWORD));
        sara.setStatus(TrainerStatus.ACTIVE);
        return trainerRepository.saveAndFlush(sara);
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainerRepository.saveAndFlush(trainer);
    }
}
