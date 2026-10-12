package com.equestrian.equis.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
@Validated
public record AuthProperties(@NotNull @Valid Refresh refresh, @NotNull @Valid Jwt jwt,
                             @NotNull @Valid Admin admin, @NotNull @Valid Invite invite, @NotNull @Valid Reset reset) {

    public record Refresh(@NotNull Duration ttl, @NotNull Duration rememberTtl) {
    }

    public record Jwt(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration accessTtl) {
    }

    public record Admin(@NotNull @Valid Lockout lockout) {
    }

    public record Lockout(@Min(1) int maxAttempts, @NotNull Duration duration) {
    }

    public record Invite(@NotNull Duration ttl) {
    }

    public record Reset(@NotNull Duration ttl) {
    }
}
