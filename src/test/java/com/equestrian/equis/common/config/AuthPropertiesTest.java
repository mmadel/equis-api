package com.equestrian.equis.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthPropertiesTest {
    // Loads the real application.properties, so the test fails if a key is renamed or removed there.
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsRefreshTtlsFromApplicationProperties() {
        contextRunner.run(context -> {
            AuthProperties properties = context.getBean(AuthProperties.class);

            assertThat(properties.refresh().ttl()).isEqualTo(Duration.ofDays(7));
            assertThat(properties.refresh().rememberTtl()).isEqualTo(Duration.ofDays(30));
        });
    }

    @Test
    void bindsJwtSettingsFromApplicationProperties() {
        contextRunner.run(context -> {
            AuthProperties properties = context.getBean(AuthProperties.class);

            assertThat(properties.jwt().secret()).isNotBlank();
            assertThat(properties.jwt().issuer()).isEqualTo("equis-api");
            assertThat(properties.jwt().accessTtl()).isEqualTo(Duration.ofMinutes(15));
        });
    }

    @Test
    void bindsAdminLockoutFromApplicationProperties() {
        contextRunner.run(context -> {
            AuthProperties properties = context.getBean(AuthProperties.class);

            assertThat(properties.admin().lockout().maxAttempts()).isEqualTo(5);
            assertThat(properties.admin().lockout().duration()).isEqualTo(Duration.ofMinutes(15));
        });
    }

    @Test
    void bindsLinkTtlsFromApplicationProperties() {
        contextRunner.run(context -> {
            AuthProperties properties = context.getBean(AuthProperties.class);

            assertThat(properties.invite().ttl()).isEqualTo(Duration.ofHours(72));
            assertThat(properties.reset().ttl()).isEqualTo(Duration.ofHours(24));
        });
    }

    @Configuration
    @EnableConfigurationProperties(AuthProperties.class)
    static class TestConfig {
    }
}
