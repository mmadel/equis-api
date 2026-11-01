package com.equestrian.equis.service.accounts;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.InviteRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccountLinkService.class, TokenGenerator.class, SecurityBeans.class, ClockConfig.class})
public class AccountLinkServiceTest {
    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    AccountLinkService accountLinkService;
    @Autowired
    TokenGenerator tokenGenerator;
    @Autowired
    InviteRepository inviteRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    EntityManager entityManager;

    // V4 seeds dev trainers and an invite, and V12 sample riders of trainer 1; clear them so each test starts from
    // empty tables (rolled back afterwards). Riders go first: they point at their trainer.
    @BeforeEach
    void clearSeededTrainers() {
        jdbcTemplate.update("DELETE FROM rider");
        jdbcTemplate.update("DELETE FROM trainer");
    }

    @Test
    void createStoresOnlyTheHashOfTheToken() {
        Trainer sara = sara();

        String raw = accountLinkService.create(sara, InvitePurpose.INVITE);

        Invite stored = stored(raw);
        assertThat(stored.getTokenHash()).isNotEqualTo(raw);
        assertThat(stored.getTrainer()).isEqualTo(sara);
        assertThat(stored.getPurpose()).isEqualTo(InvitePurpose.INVITE);
        assertThat(stored.getAcceptedAt()).isNull();
    }

    @Test
    void inviteLinkLastsLongerThanAResetLink() {
        Instant before = Instant.now();

        Invite invite = stored(accountLinkService.create(sara(), InvitePurpose.INVITE));
        Invite reset = stored(accountLinkService.create(omar(), InvitePurpose.RESET));

        Instant after = Instant.now();
        assertThat(invite.getExpiresAt()).isBetween(before.plus(Duration.ofHours(72)), after.plus(Duration.ofHours(72)));
        assertThat(reset.getExpiresAt()).isBetween(before.plus(Duration.ofHours(24)), after.plus(Duration.ofHours(24)));
    }

    @Test
    void createReplacesTheOpenLinkSoOnlyTheNewestWorks() {
        Trainer sara = sara();
        String first = accountLinkService.create(sara, InvitePurpose.INVITE);
        entityManager.flush();

        String second = accountLinkService.create(sara, InvitePurpose.INVITE);
        entityManager.flush();
        // The old link is removed by a bulk delete, so drop the stale entity before looking it up.
        entityManager.clear();

        assertInvalidLink(first);
        assertThat(accountLinkService.openLink(second).getTokenHash()).isEqualTo(tokenGenerator.hash(second));
        assertThat(inviteRepository.count()).isEqualTo(1);
    }

    @Test
    void createKeepsLinksThatWereAlreadyUsed() {
        Trainer sara = sara();
        String first = accountLinkService.create(sara, InvitePurpose.INVITE);
        accountLinkService.markUsed(stored(first));
        entityManager.flush();

        accountLinkService.create(sara, InvitePurpose.RESET);
        entityManager.flush();

        assertThat(inviteRepository.count()).isEqualTo(2);
    }

    @Test
    void openLinkReturnsTheInviteForAValidToken() {
        Trainer sara = sara();
        String raw = accountLinkService.create(sara, InvitePurpose.RESET);

        Invite invite = accountLinkService.openLink(raw);

        assertThat(invite.getTrainer()).isEqualTo(sara);
        assertThat(invite.getPurpose()).isEqualTo(InvitePurpose.RESET);
    }

    @Test
    void openLinkRejectsAnUnknownOrMissingToken() {
        assertInvalidLink(tokenGenerator.generate().raw());
        assertInvalidLink("");
        assertInvalidLink(null);
    }

    @Test
    void openLinkRejectsALinkThatWasAlreadyUsed() {
        String raw = accountLinkService.create(sara(), InvitePurpose.INVITE);
        Invite invite = accountLinkService.openLink(raw);
        Instant before = Instant.now();

        accountLinkService.markUsed(invite);

        assertThat(invite.getAcceptedAt()).isBetween(before, Instant.now());
        assertInvalidLink(raw);
    }

    @Test
    void openLinkRejectsAnExpiredLink() {
        String raw = accountLinkService.create(sara(), InvitePurpose.INVITE);
        entityManager.flush();
        jdbcTemplate.update("UPDATE invite SET created_at = now() - interval '2 days', "
                + "expires_at = now() - interval '1 day' WHERE token_hash = ?", tokenGenerator.hash(raw));
        entityManager.clear();

        assertInvalidLink(raw);
    }

    @Test
    void openLinkRejectsADisabledTrainer() {
        Trainer sara = sara();
        String raw = accountLinkService.create(sara, InvitePurpose.RESET);
        sara.setStatus(TrainerStatus.DISABLED);

        assertInvalidLink(raw);
    }

    private Invite stored(String raw) {
        return inviteRepository.findByTokenHash(tokenGenerator.hash(raw)).orElseThrow();
    }

    private void assertInvalidLink(String raw) {
        assertThatThrownBy(() -> accountLinkService.openLink(raw))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.INVALID_LINK);
                });
    }

    private Trainer sara() {
        return trainer("Sara Ali", "01000000001", "sara@example.com");
    }

    private Trainer omar() {
        return trainer("Omar Hassan", "01000000002", "omar@example.com");
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainerRepository.saveAndFlush(trainer);
    }
}
