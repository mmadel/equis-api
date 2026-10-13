package com.equestrian.equis.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.sessions")
@Validated
public record SessionProperties(@Min(1) int horizonWeeks, @NotBlank String generateCron) {
}
