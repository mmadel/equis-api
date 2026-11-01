package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.config.AuthProperties;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.entity.accounts.RefreshToken;
import com.equestrian.equis.entity.accounts.SubjectType;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.riders.RiderAccountStatus;
import com.equestrian.equis.repository.accounts.RefreshTokenRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.JwtService.AccessToken;
import com.equestrian.equis.service.accounts.TokenGenerator.GeneratedToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final TrainerRepository trainerRepository;
    private final RiderRepository riderRepository;
    private final JwtService jwtService;
    private final TokenGenerator tokenGenerator;
    private final AuthProperties properties;
    private final Clock clock;

    public AuthTokenService(RefreshTokenRepository refreshTokenRepository, TrainerRepository trainerRepository,
                            RiderRepository riderRepository, JwtService jwtService, TokenGenerator tokenGenerator,
                            AuthProperties properties, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.trainerRepository = trainerRepository;
        this.riderRepository = riderRepository;
        this.jwtService = jwtService;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AuthTokens issue(Role role, UUID subjectId, boolean rememberMe) {
        return create(role, subjectId, rememberMe).tokens();
    }

    // Every failure before the disabled check gives the same answer, so a caller cannot learn why a token was refused.
    // Every refusal happens before anything is written, except the inactive rider's revoke, which must stay.
    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokens rotate(String rawRefresh) {
        if (rawRefresh == null || rawRefresh.isBlank()) {
            throw invalidRefreshToken();
        }
        RefreshToken old = refreshTokenRepository.findByTokenHashForUpdate(tokenGenerator.hash(rawRefresh))
                .orElseThrow(AuthTokenService::invalidRefreshToken);
        // Read the time after taking the lock, so it is never earlier than the token's created_at.
        Instant now = clock.instant();
        if (old.getRevokedAt() != null || !old.getExpiresAt().isAfter(now)) {
            throw invalidRefreshToken();
        }

        Role role = Role.from(old.getSubjectType());
        if (role == Role.TRAINER && trainerRepository.existsByIdAndStatus(old.getSubjectId(), TrainerStatus.DISABLED)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_DISABLED,
                    "This account has been disabled.");
        }
        // A rider who is no longer active is signed out everywhere (noRollbackFor keeps the revoke).
        if (role == Role.RIDER
                && !riderRepository.existsByIdAndAccountStatus(old.getSubjectId(), RiderAccountStatus.ACTIVE)) {
            refreshTokenRepository.revokeAllActive(SubjectType.RIDER, old.getSubjectId(), now);
            throw invalidRefreshToken();
        }
        // With "keep me signed in" a trainer may not sign in for weeks, so each refresh counts as being active.
        if (role == Role.TRAINER) {
            trainerRepository.findById(old.getSubjectId()).ifPresent(t -> t.setLastActiveAt(now));
        }

        // The new token starts a fresh lifetime of the same kind the person chose at sign-in.
        Issued issued = create(role, old.getSubjectId(), old.isRememberMe());
        old.setRevokedAt(now);
        old.setReplacedBy(issued.refreshToken());
        return issued.tokens();
    }

    @Transactional
    public void revokeAll(Role role, UUID subjectId) {
        refreshTokenRepository.revokeAllActive(role.toSubjectType(), subjectId, clock.instant());
    }

    private Issued create(Role role, UUID subjectId, boolean rememberMe) {
        GeneratedToken generated = tokenGenerator.generate();
        Duration ttl = rememberMe ? properties.refresh().rememberTtl() : properties.refresh().ttl();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSubjectType(role.toSubjectType());
        refreshToken.setSubjectId(subjectId);
        refreshToken.setTokenHash(generated.hash());
        refreshToken.setRememberMe(rememberMe);
        refreshToken.setExpiresAt(clock.instant().plus(ttl));
        refreshToken = refreshTokenRepository.save(refreshToken);

        AccessToken accessToken = jwtService.issue(new AuthUser(subjectId, role));
        long expiresIn = properties.jwt().accessTtl().toSeconds();
        return new Issued(refreshToken, new AuthTokens(accessToken.value(), expiresIn, generated.raw(), role));
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_REFRESH_TOKEN, "Please sign in again.");
    }

    private record Issued(RefreshToken refreshToken, AuthTokens tokens) {
    }
}
