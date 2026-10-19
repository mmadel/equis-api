package com.equestrian.equis.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The prod startup check on its own: no Spring context, no database.
// MockEnvironment has no system sources, so APP_* variables set on the machine do not leak in.
public class ProdEnvironmentCheckTest {
    private final ProdEnvironmentCheck check = new ProdEnvironmentCheck();

    @Test
    void prodWithEveryVariableStarts() {
        assertThatCode(() -> check.postProcessEnvironment(environment("prod", allVariables()), new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void prodWithoutJwtSecretStopsWithItsName() {
        Map<String, Object> variables = allVariables();
        variables.remove("APP_JWT_SECRET");
        assertThatThrownBy(() -> check.postProcessEnvironment(environment("prod", variables), new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing required environment variable: APP_JWT_SECRET");
    }

    @Test
    void prodWithBlankValuesListsEachOne() {
        Map<String, Object> variables = allVariables();
        variables.put("APP_ADMIN_EMAIL", " ");
        variables.put("APP_ADMIN_PASSWORD_HASH", "");
        assertThatThrownBy(() -> check.postProcessEnvironment(environment("prod", variables), new SpringApplication()))
                .hasMessage("Missing required environment variable: APP_ADMIN_EMAIL\n"
                        + "Missing required environment variable: APP_ADMIN_PASSWORD_HASH");
    }

    @Test
    void devDoesNotCheck() {
        assertThatCode(() -> check.postProcessEnvironment(environment("dev", Map.of()), new SpringApplication()))
                .doesNotThrowAnyException();
    }

    private static MockEnvironment environment(String profile, Map<String, Object> variables) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        variables.forEach((name, value) -> environment.setProperty(name, value.toString()));
        return environment;
    }

    private static Map<String, Object> allVariables() {
        Map<String, Object> variables = new LinkedHashMap<>();
        ProdEnvironmentCheck.REQUIRED.forEach(name -> variables.put(name, "value"));
        return variables;
    }
}
