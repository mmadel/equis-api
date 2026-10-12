package com.equestrian.equis.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtConfigTest {
    private static final String ISSUER = "equis-api";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(SecurityBeans.class, ClockConfig.class, JwtConfig.class);

    @Test
    void decodesTokenMadeByTheEncoder() {
        contextRunner.run(context -> {
            String token = encode(context.getBean(JwtEncoder.class), ISSUER, Instant.now(), Duration.ofMinutes(15));

            Jwt jwt = context.getBean(JwtDecoder.class).decode(token);

            assertThat(jwt.getSubject()).isEqualTo("42");
            assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        });
    }

    @Test
    void rejectsExpiredToken() {
        contextRunner.run(context -> {
            String token = encode(context.getBean(JwtEncoder.class), ISSUER,
                    Instant.now().minus(Duration.ofMinutes(16)), Duration.ofMinutes(15));

            assertThatThrownBy(() -> context.getBean(JwtDecoder.class).decode(token))
                    .isInstanceOf(JwtValidationException.class);
        });
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        contextRunner.run(context -> {
            String token = encode(context.getBean(JwtEncoder.class), "someone-else", Instant.now(),
                    Duration.ofMinutes(15));

            assertThatThrownBy(() -> context.getBean(JwtDecoder.class).decode(token))
                    .isInstanceOf(JwtValidationException.class);
        });
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        contextRunner.run(context -> {
            JwtEncoder otherEncoder = NimbusJwtEncoder
                    .withSecretKey(new SecretKeySpec(new byte[32], "HmacSHA256"))
                    .algorithm(MacAlgorithm.HS256)
                    .build();
            String token = encode(otherEncoder, ISSUER, Instant.now(), Duration.ofMinutes(15));

            assertThatThrownBy(() -> context.getBean(JwtDecoder.class).decode(token))
                    .isInstanceOf(BadJwtException.class);
        });
    }

    @Test
    void failsToStartWhenSecretIsTooShort() {
        // "short" in base64: 5 bytes.
        contextRunner.withPropertyValues("app.jwt.secret=c2hvcnQ=").run(context ->
                assertThat(context).getFailure().rootCause().hasMessageContaining("at least 32 bytes"));
    }

    @Test
    void failsToStartWhenSecretIsNotBase64() {
        contextRunner.withPropertyValues("app.jwt.secret=not base64 !!").run(context ->
                assertThat(context).getFailure().hasStackTraceContaining("must be valid base64"));
    }

    private static String encode(JwtEncoder encoder, String issuer, Instant issuedAt, Duration ttl) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("42")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(ttl))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
