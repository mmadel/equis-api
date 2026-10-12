package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.entity.accounts.RefreshToken;
import com.equestrian.equis.entity.accounts.SubjectType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class RefreshTokenRepositoryTest {
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String HASH_C = "c".repeat(64);
    private static final String HASH_D = "d".repeat(64);
    private static final String HASH_E = "e".repeat(64);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void savesWithGeneratedIdAndCreatedAt() {
        RefreshToken saved = refreshTokenRepository.saveAndFlush(token(SubjectType.TRAINER, UUID.randomUUID(), HASH_A, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getRevokedAt()).isNull();
        assertThat(saved.getReplacedBy()).isNull();
    }

    @Test
    void storesSubjectTypeAsLowercase() {
        RefreshToken saved = refreshTokenRepository.saveAndFlush(token(SubjectType.ADMIN, UUID.randomUUID(), HASH_A, null));

        String subjectType = jdbcTemplate.queryForObject("SELECT subject_type FROM refresh_token WHERE id = ?", String.class, saved.getId());
        assertThat(subjectType).isEqualTo("admin");
    }

    @Test
    void findsByTokenHash() {
        RefreshToken saved = refreshTokenRepository.saveAndFlush(token(SubjectType.TRAINER, UUID.randomUUID(), HASH_A, null));

        assertThat(refreshTokenRepository.findByTokenHash(HASH_A)).contains(saved);
        assertThat(refreshTokenRepository.findByTokenHash(HASH_B)).isEmpty();
    }

    @Test
    void revokesOnlyTheActiveTokensOfThatSubject() {
        UUID saraId = UUID.randomUUID();
        Instant earlier = Instant.now().plus(Duration.ofSeconds(1)).truncatedTo(ChronoUnit.MICROS);
        Instant now = Instant.now().plus(Duration.ofMinutes(1)).truncatedTo(ChronoUnit.MICROS);
        refreshTokenRepository.saveAndFlush(token(SubjectType.TRAINER, saraId, HASH_A, null));
        refreshTokenRepository.saveAndFlush(token(SubjectType.TRAINER, saraId, HASH_B, null));
        refreshTokenRepository.saveAndFlush(token(SubjectType.TRAINER, saraId, HASH_C, earlier));
        // Same id under the other subject type, and a different trainer: neither may be touched.
        refreshTokenRepository.saveAndFlush(token(SubjectType.ADMIN, saraId, HASH_D, null));
        refreshTokenRepository.saveAndFlush(token(SubjectType.TRAINER, UUID.randomUUID(), HASH_E, null));

        int revoked = refreshTokenRepository.revokeAllActive(SubjectType.TRAINER, saraId, now);

        assertThat(revoked).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM refresh_token WHERE revoked_at = ?", String.class, Timestamp.from(now)))
                .containsExactlyInAnyOrder(HASH_A, HASH_B);
        assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM refresh_token WHERE revoked_at = ?", String.class, Timestamp.from(earlier)))
                .containsExactly(HASH_C);
        assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM refresh_token WHERE revoked_at IS NULL", String.class))
                .containsExactlyInAnyOrder(HASH_D, HASH_E);
    }

    @Test
    void revokingReturnsZeroWhenNothingIsActive() {
        assertThat(refreshTokenRepository.revokeAllActive(SubjectType.TRAINER, UUID.randomUUID(), Instant.now())).isZero();
    }

    @Test
    void deletesAllTokensOfThatSubject() {
        UUID saraId = UUID.randomUUID();
        RefreshToken replacement = refreshTokenRepository.saveAndFlush(token(SubjectType.TRAINER, saraId, HASH_A, null));
        RefreshToken rotated = token(SubjectType.TRAINER, saraId, HASH_B, Instant.now().plus(Duration.ofSeconds(1)));
        rotated.setReplacedBy(replacement);
        refreshTokenRepository.saveAndFlush(rotated);
        refreshTokenRepository.saveAndFlush(token(SubjectType.ADMIN, saraId, HASH_C, null));
        refreshTokenRepository.saveAndFlush(token(SubjectType.TRAINER, UUID.randomUUID(), HASH_D, null));

        int deleted = refreshTokenRepository.deleteBySubjectTypeAndSubjectId(SubjectType.TRAINER, saraId);

        assertThat(deleted).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM refresh_token", String.class))
                .containsExactlyInAnyOrder(HASH_C, HASH_D);
    }

    private RefreshToken token(SubjectType subjectType, UUID subjectId, String tokenHash, Instant revokedAt) {
        RefreshToken token = new RefreshToken();
        token.setSubjectType(subjectType);
        token.setSubjectId(subjectId);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        token.setRevokedAt(revokedAt);
        return token;
    }
}
