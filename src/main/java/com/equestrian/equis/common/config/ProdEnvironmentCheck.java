package com.equestrian.equis.common.config;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

// In the prod profile, stops startup before any bean (and so before Flyway) when a required
// environment variable is missing or blank. Without it, a missing ${VAR} can reach the app as
// literal text, e.g. the admin seed would store "${APP_ADMIN_EMAIL}" as an email.
public class ProdEnvironmentCheck implements EnvironmentPostProcessor, Ordered {

    static final List<String> REQUIRED = List.of(
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_USERNAME",
            "SPRING_DATASOURCE_PASSWORD",
            "APP_JWT_SECRET",
            "APP_BASE_URL",
            "APP_MAIL_MODE",
            "APP_ADMIN_EMAIL",
            "APP_ADMIN_PASSWORD_HASH");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        List<String> missing = REQUIRED.stream()
                .filter(name -> isBlank(environment.getProperty(name)))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(missing.stream()
                    .map(name -> "Missing required environment variable: " + name)
                    .collect(Collectors.joining("\n")));
        }
    }

    // After the config-data step, so profiles from SPRING_PROFILES_ACTIVE are already known.
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
