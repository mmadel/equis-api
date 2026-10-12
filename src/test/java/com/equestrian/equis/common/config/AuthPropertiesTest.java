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

    @Configuration
    @EnableConfigurationProperties(AuthProperties.class)
    static class TestConfig {
    }
}
