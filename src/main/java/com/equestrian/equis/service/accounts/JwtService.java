package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.config.AuthProperties;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.AuthUserConverter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, AuthProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public AccessToken issue(AuthUser user) {
        // JWT times are whole seconds, so the expiry we report matches the one inside the token.
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(properties.jwt().accessTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.id().toString())
                .claim(AuthUserConverter.ROLE_CLAIM, user.role().name())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new AccessToken(token, expiresAt);
    }

    public record AccessToken(String value, Instant expiresAt) {
        // Keeps the token out of logs if the record is ever printed.
        @Override
        public String toString() {
            return "AccessToken[expiresAt=" + expiresAt + "]";
        }
    }
}
