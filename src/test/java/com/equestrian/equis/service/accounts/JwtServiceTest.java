package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.service.accounts.JwtService.AccessToken;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtServiceTest {
    private static final AuthUser TRAINER =
            new AuthUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), Role.TRAINER);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class);

    @Test
    void issuedTokenCarriesSubjectRoleAndIssuer() {
        contextRunner.run(context -> {
            AccessToken token = context.getBean(JwtService.class).issue(TRAINER);

            Jwt jwt = context.getBean(JwtDecoder.class).decode(token.value());

            assertThat(jwt.getSubject()).isEqualTo(TRAINER.id().toString());
            assertThat(jwt.getClaimAsString("role")).isEqualTo("TRAINER");
            assertThat(jwt.getClaimAsString("iss")).isEqualTo("equis-api");
        });
    }

    @Test
    void expiryIsTheAccessTtlFromNowAndMatchesTheToken() {
        contextRunner.run(context -> {
            Instant before = Instant.now();

            AccessToken token = context.getBean(JwtService.class).issue(TRAINER);

            Jwt jwt = context.getBean(JwtDecoder.class).decode(token.value());
            assertThat(token.expiresAt()).isEqualTo(jwt.getExpiresAt());
            assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
            assertThat(token.expiresAt()).isBetween(
                    before.plus(Duration.ofMinutes(15)).minusSeconds(1), Instant.now().plus(Duration.ofMinutes(15)));
        });
    }

    @Test
    void toStringDoesNotExposeTheToken() {
        contextRunner.run(context -> {
            AccessToken token = context.getBean(JwtService.class).issue(TRAINER);

            assertThat(token.toString()).doesNotContain(token.value());
        });
    }
}
