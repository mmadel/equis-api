package com.equestrian.equis.dto.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The reason must be at least 5 characters once trimmed (SubscriptionService).
public record VoidSubscriptionRequest(@NotBlank @Size(max = 500) String reason) {
}
