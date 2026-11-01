package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.Rider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class InviteRepositoryTest {
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String HASH_C = "c".repeat(64);
    private static final String HASH_D = "d".repeat(64);

    private static final String TOMORROW = "now() + interval '1 day'";
    private static final String AN_HOUR_AGO = "now() - interval '1 hour'";

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    InviteRepository inviteRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TestEntityManager entityManager;

    // V4 seeds dev trainers and an invite, and V12 sample riders of trainer 1; clear them so each test starts from
    // empty tables (rolled back afterwards). Riders go first: they point at their trainer.
    @BeforeEach
    void clearSeededTrainers() {
        jdbcTemplate.update("DELETE FROM rider");
        jdbcTemplate.update("DELETE FROM trainer");
    }

    @Test
    void savesWithGeneratedIdAndCreatedAt() {
        Invite saved = inviteRepository.saveAndFlush(invite(sara(), HASH_A, InvitePurpose.INVITE, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getAcceptedAt()).isNull();
    }

    @Test
    void storesPurposeAsLowercase() {
        Invite saved = inviteRepository.saveAndFlush(invite(sara(), HASH_A, InvitePurpose.RESET, null));

        String purpose = jdbcTemplate.queryForObject("SELECT purpose FROM invite WHERE id = ?", String.class, saved.getId());
        assertThat(purpose).isEqualTo("reset");
    }

    @Test
    void findsByTokenHash() {
        Invite saved = inviteRepository.saveAndFlush(invite(sara(), HASH_A, InvitePurpose.INVITE, null));

        assertThat(inviteRepository.findByTokenHash(HASH_A)).contains(saved);
        assertThat(inviteRepository.findByTokenHash(HASH_B)).isEmpty();
    }

    @Test
    void deletesOnlyTheOpenInviteOfThatTrainer() {
        Trainer sara = sara();
        Trainer omar = omar();
        inviteRepository.saveAndFlush(invite(sara, HASH_A, InvitePurpose.INVITE, Instant.now().plus(Duration.ofMinutes(1))));
        inviteRepository.saveAndFlush(invite(sara, HASH_B, InvitePurpose.RESET, null));
        inviteRepository.saveAndFlush(invite(omar, HASH_C, InvitePurpose.INVITE, null));

        int deleted = inviteRepository.deleteByTrainerIdAndAcceptedAtIsNull(sara.getId());

        assertThat(deleted).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM invite", String.class))
                .containsExactlyInAnyOrder(HASH_A, HASH_C);
    }

    @Test
    void openInviteCanBeReplacedInTheSameTransaction() {
        Trainer sara = sara();
        inviteRepository.saveAndFlush(invite(sara, HASH_A, InvitePurpose.INVITE, null));

        inviteRepository.deleteByTrainerIdAndAcceptedAtIsNull(sara.getId());
        inviteRepository.saveAndFlush(invite(sara, HASH_B, InvitePurpose.INVITE, null));

        assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM invite", String.class))
                .containsExactly(HASH_B);
    }

    @Test
    void deletingTheTrainerCascadesToItsInvites() {
        Trainer sara = sara();
        insert(sara.getId(), HASH_A, "invite", TOMORROW, "NULL");

        jdbcTemplate.update("DELETE FROM trainer WHERE id = ?", sara.getId());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM invite", Integer.class)).isZero();
    }

    @Test
    void rejectsUnknownTrainer() {
        assertThatThrownBy(() -> insert(UUID.randomUUID(), HASH_A, "invite", TOMORROW, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_invite_trainer");
    }

    @Test
    void rejectsTokenHashThatIsNotSha256Hex() {
        UUID trainerId = sara().getId();
        assertThatThrownBy(() -> insert(trainerId, "A".repeat(64), "invite", TOMORROW, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_invite_token_hash");
    }

    @Test
    void rejectsShortTokenHash() {
        UUID trainerId = sara().getId();
        assertThatThrownBy(() -> insert(trainerId, "a".repeat(63), "invite", TOMORROW, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_invite_token_hash");
    }

    @Test
    void rejectsUnknownPurpose() {
        UUID trainerId = sara().getId();
        assertThatThrownBy(() -> insert(trainerId, HASH_A, "INVITE", TOMORROW, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_invite_purpose");
    }

    @Test
    void rejectsExpiryNotAfterCreation() {
        UUID trainerId = sara().getId();
        assertThatThrownBy(() -> insert(trainerId, HASH_A, "invite", AN_HOUR_AGO, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_invite_expiry");
    }

    @Test
    void rejectsAcceptanceBeforeCreation() {
        UUID trainerId = sara().getId();
        assertThatThrownBy(() -> insert(trainerId, HASH_A, "invite", TOMORROW, AN_HOUR_AGO))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_invite_accepted");
    }

    @Test
    void rejectsDuplicateTokenHash() {
        UUID saraId = sara().getId();
        UUID omarId = omar().getId();
        insert(saraId, HASH_A, "invite", TOMORROW, "NULL");
        assertThatThrownBy(() -> insert(omarId, HASH_A, "invite", TOMORROW, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_invite_token_hash");
    }

    @Test
    void rejectsSecondOpenInviteForTheSameTrainer() {
        UUID trainerId = sara().getId();
        insert(trainerId, HASH_A, "invite", TOMORROW, "NULL");
        assertThatThrownBy(() -> insert(trainerId, HASH_B, "reset", TOMORROW, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_invite_open_per_trainer");
    }

    @Test
    void allowsAnOpenInviteAlongsideAcceptedOnes() {
        UUID trainerId = sara().getId();
        insert(trainerId, HASH_A, "invite", TOMORROW, "now()");
        insert(trainerId, HASH_B, "reset", TOMORROW, "now()");
        insert(trainerId, HASH_D, "reset", TOMORROW, "NULL");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM invite", Integer.class)).isEqualTo(3);
    }

    // E15 case 6
    @Test
    void rejectsALinkWithBothOwners() {
        UUID trainerId = sara().getId();
        UUID riderId = rider("Laila Nour", "laila@gmail.com");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO invite (trainer_id, rider_id, token_hash, purpose, expires_at)
                VALUES (?, ?, ?, 'invite', now() + interval '1 day')
                """, trainerId, riderId, HASH_A))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_invite_owner");
    }

    @Test
    void rejectsALinkWithNoOwner() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO invite (token_hash, purpose, expires_at)
                VALUES (?, 'invite', now() + interval '1 day')
                """, HASH_A))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_invite_owner");
    }

    // E15 case 7
    @Test
    void rejectsSecondOpenLinkForTheSameRider() {
        UUID riderId = rider("Laila Nour", "laila@gmail.com");
        insertForRider(riderId, HASH_A, "NULL");

        assertThatThrownBy(() -> insertForRider(riderId, HASH_B, "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_invite_open_per_rider");
    }

    @Test
    void allowsANewOpenLinkOnceTheRidersFirstIsAccepted() {
        UUID riderId = rider("Laila Nour", "laila@gmail.com");
        insertForRider(riderId, HASH_A, "NULL");

        jdbcTemplate.update("UPDATE invite SET accepted_at = now() WHERE token_hash = ?", HASH_A);
        insertForRider(riderId, HASH_C, "NULL");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM invite WHERE rider_id = ?", Integer.class, riderId))
                .isEqualTo(2);
    }

    // E15 case 8
    @Test
    void savesOpenTrainerAndRiderLinksTogether() {
        Trainer sara = sara();
        Rider laila = entityManager.find(Rider.class, rider("Laila Nour", "laila@gmail.com"));

        Invite trainerLink = inviteRepository.saveAndFlush(invite(sara, HASH_A, InvitePurpose.INVITE, null));
        Invite riderLink = Invite.forRider(laila);
        riderLink.setTokenHash(HASH_B);
        riderLink.setPurpose(InvitePurpose.INVITE);
        riderLink.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        inviteRepository.saveAndFlush(riderLink);
        entityManager.clear();

        Invite trainerRead = inviteRepository.findByTokenHash(HASH_A).orElseThrow();
        Invite riderRead = inviteRepository.findByTokenHash(HASH_B).orElseThrow();
        assertThat(trainerRead.getId()).isEqualTo(trainerLink.getId());
        assertThat(trainerRead.isForTrainer()).isTrue();
        assertThat(trainerRead.isForRider()).isFalse();
        assertThat(riderRead.isForRider()).isTrue();
        assertThat(riderRead.isForTrainer()).isFalse();
        assertThat(riderRead.getRider().getId()).isEqualTo(laila.getId());
    }

    // E15 case 13
    @Test
    void deletesOnlyTheOpenLinkOfThatRider() {
        UUID laila = rider("Laila Nour", "laila@gmail.com");
        UUID adam = rider("Adam Samir", "adam@gmail.com");
        Trainer sara = sara();
        insertForRider(laila, HASH_A, "now()");
        insertForRider(laila, HASH_B, "NULL");
        insertForRider(adam, HASH_C, "NULL");
        insert(sara.getId(), HASH_D, "invite", TOMORROW, "NULL");

        int deleted = inviteRepository.deleteByRiderIdAndAcceptedAtIsNull(laila);

        assertThat(deleted).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM invite", String.class))
                .containsExactlyInAnyOrder(HASH_A, HASH_C, HASH_D);
    }

    @Test
    void deletingTheRiderCascadesToItsLinks() {
        UUID riderId = rider("Laila Nour", "laila@gmail.com");
        insertForRider(riderId, HASH_A, "NULL");

        jdbcTemplate.update("DELETE FROM rider WHERE id = ?", riderId);

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM invite", Integer.class)).isZero();
    }

    @Test
    void factoriesNeedAnOwner() {
        assertThatThrownBy(() -> Invite.forTrainer(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Invite.forRider(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private UUID rider(String name, String email) {
        return jdbcTemplate.queryForObject("INSERT INTO rider (name, level_id, email) VALUES (?, 1, ?) RETURNING id",
                UUID.class, name, email);
    }

    private void insertForRider(UUID riderId, String tokenHash, String acceptedAt) {
        jdbcTemplate.update("INSERT INTO invite (rider_id, token_hash, purpose, expires_at, accepted_at) VALUES (?, ?, 'invite', "
                + TOMORROW + ", " + acceptedAt + ")", riderId, tokenHash);
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

    private Invite invite(Trainer trainer, String tokenHash, InvitePurpose purpose, Instant acceptedAt) {
        Invite invite = Invite.forTrainer(trainer);
        invite.setTokenHash(tokenHash);
        invite.setPurpose(purpose);
        invite.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        invite.setAcceptedAt(acceptedAt);
        return invite;
    }

    // expiresAt and acceptedAt are SQL expressions so they are relative to the row's created_at (now()).
    private void insert(UUID trainerId, String tokenHash, String purpose, String expiresAt, String acceptedAt) {
        jdbcTemplate.update("INSERT INTO invite (trainer_id, token_hash, purpose, expires_at, accepted_at) VALUES (?, ?, ?, "
                + expiresAt + ", " + acceptedAt + ")", trainerId, tokenHash, purpose);
    }
}
