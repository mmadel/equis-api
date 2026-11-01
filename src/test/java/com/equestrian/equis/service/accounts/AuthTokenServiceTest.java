package com.equestrian.equis.service.accounts;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.config.AuthProperties;
import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.entity.accounts.RefreshToken;
import com.equestrian.equis.entity.accounts.SubjectType;
import com.equestrian.equis.repository.accounts.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableConfigurationProperties(AuthProperties.class)
@Import({AuthTokenService.class, JwtService.class, TokenGenerator.class, JwtConfig.class, ClockConfig.class})
public class AuthTokenServiceTest {
    // Seeded by V4 as an active trainer.
    private static final UUID TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.randomUUID();

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    AuthTokenService authTokenService;
    @Autowired
    TokenGenerator tokenGenerator;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    JwtDecoder jwtDecoder;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    EntityManager entityManager;

    @Test
    void issueReturnsAnAccessTokenForThatPersonAndRole() {
        AuthTokens tokens = authTokenService.issue(Role.TRAINER, TRAINER_ID, false);

        Jwt jwt = jwtDecoder.decode(tokens.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(TRAINER_ID.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("TRAINER");
        assertThat(tokens.role()).isEqualTo(Role.TRAINER);
        assertThat(tokens.expiresIn()).isEqualTo(Duration.ofMinutes(15).toSeconds());
    }

    @Test
    void issueStoresOnlyTheHashOfTheRefreshToken() {
        AuthTokens tokens = authTokenService.issue(Role.ADMIN, ADMIN_ID, false);

        RefreshToken stored = stored(tokens);
        assertThat(stored.getTokenHash()).isNotEqualTo(tokens.refreshToken());
        assertThat(stored.getSubjectType()).isEqualTo(SubjectType.ADMIN);
        assertThat(stored.getSubjectId()).isEqualTo(ADMIN_ID);
        assertThat(stored.getRevokedAt()).isNull();
    }

    @Test
    void refreshTokenLastsLongerWithRememberMe() {
        Instant before = Instant.now();

        RefreshToken normal = stored(authTokenService.issue(Role.TRAINER, TRAINER_ID, false));
        RefreshToken remembered = stored(authTokenService.issue(Role.TRAINER, TRAINER_ID, true));

        Instant after = Instant.now();
        assertThat(normal.isRememberMe()).isFalse();
        assertThat(normal.getExpiresAt()).isBetween(before.plus(Duration.ofDays(7)), after.plus(Duration.ofDays(7)));
        assertThat(remembered.isRememberMe()).isTrue();
        assertThat(remembered.getExpiresAt()).isBetween(before.plus(Duration.ofDays(30)), after.plus(Duration.ofDays(30)));
    }

    @Test
    void rotateRevokesTheOldTokenAndReturnsANewPair() {
        AuthTokens first = authTokenService.issue(Role.TRAINER, TRAINER_ID, true);

        AuthTokens second = authTokenService.rotate(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(second.role()).isEqualTo(Role.TRAINER);
        assertThat(jwtDecoder.decode(second.accessToken()).getSubject()).isEqualTo(TRAINER_ID.toString());

        RefreshToken old = stored(first);
        RefreshToken replacement = stored(second);
        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(old.getReplacedBy()).isEqualTo(replacement);
        assertThat(replacement.getRevokedAt()).isNull();
        assertThat(replacement.isRememberMe()).isTrue();
    }

    @Test
    void rotateRejectsATokenThatWasAlreadyUsed() {
        AuthTokens first = authTokenService.issue(Role.TRAINER, TRAINER_ID, false);
        authTokenService.rotate(first.refreshToken());

        assertInvalidRefreshToken(first.refreshToken());
    }

    @Test
    void rotateRejectsAnUnknownOrMissingToken() {
        assertInvalidRefreshToken(tokenGenerator.generate().raw());
        assertInvalidRefreshToken("");
        assertInvalidRefreshToken(null);
    }

    @Test
    void rotateRejectsAnExpiredToken() {
        AuthTokens tokens = authTokenService.issue(Role.TRAINER, TRAINER_ID, false);
        entityManager.flush();
        jdbcTemplate.update("UPDATE refresh_token SET created_at = now() - interval '2 days', "
                + "expires_at = now() - interval '1 day' WHERE token_hash = ?", tokenGenerator.hash(tokens.refreshToken()));
        entityManager.clear();

        assertInvalidRefreshToken(tokens.refreshToken());
    }

    @Test
    void rotateRejectsADisabledTrainerAndLeavesTheTokenAlone() {
        AuthTokens tokens = authTokenService.issue(Role.TRAINER, TRAINER_ID, false);
        entityManager.flush();
        jdbcTemplate.update("UPDATE trainer SET status = 'disabled' WHERE id = ?", TRAINER_ID);

        assertThatThrownBy(() -> authTokenService.rotate(tokens.refreshToken()))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.ACCOUNT_DISABLED);
                });
        assertThat(stored(tokens).getRevokedAt()).isNull();
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void rotateWorksForTheAdmin() {
        AuthTokens first = authTokenService.issue(Role.ADMIN, ADMIN_ID, false);

        AuthTokens second = authTokenService.rotate(first.refreshToken());

        assertThat(second.role()).isEqualTo(Role.ADMIN);
        assertThat(jwtDecoder.decode(second.accessToken()).getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    void revokeAllCancelsEveryTokenOfThatPersonOnly() {
        AuthTokens phone = authTokenService.issue(Role.TRAINER, TRAINER_ID, false);
        AuthTokens browser = authTokenService.issue(Role.TRAINER, TRAINER_ID, true);
        AuthTokens admin = authTokenService.issue(Role.ADMIN, ADMIN_ID, false);
        entityManager.flush();

        authTokenService.revokeAll(Role.TRAINER, TRAINER_ID);
        // revokeAll is a bulk update, so drop the stale entities before reading them back.
        entityManager.clear();

        assertInvalidRefreshToken(phone.refreshToken());
        assertInvalidRefreshToken(browser.refreshToken());
        assertThat(authTokenService.rotate(admin.refreshToken()).role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void toStringDoesNotExposeTheTokens() {
        AuthTokens tokens = authTokenService.issue(Role.TRAINER, TRAINER_ID, false);

        assertThat(tokens.toString()).doesNotContain(tokens.accessToken(), tokens.refreshToken());
    }

    private RefreshToken stored(AuthTokens tokens) {
        return refreshTokenRepository.findByTokenHash(tokenGenerator.hash(tokens.refreshToken())).orElseThrow();
    }

    private void assertInvalidRefreshToken(String rawRefresh) {
        assertThatThrownBy(() -> authTokenService.rotate(rawRefresh))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.INVALID_REFRESH_TOKEN);
                });
    }
}
