package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class InviteRepositoryTest {
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String HASH_C = "c".repeat(64);
    private static final String HASH_D = "d".repeat(64);

    private static final String TOMORROW = "now() + interval '1 day'";
    private static final String AN_HOUR_AGO = "now() - interval '1 hour'";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    InviteRepository inviteRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

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
        Invite invite = new Invite();
        invite.setTrainer(trainer);
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
