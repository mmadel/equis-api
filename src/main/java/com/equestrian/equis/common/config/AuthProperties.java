package com.equestrian.equis.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
@Validated
public record AuthProperties(@NotNull @Valid Refresh refresh) {

    public record Refresh(@NotNull Duration ttl, @NotNull Duration rememberTtl) {
    }
}
