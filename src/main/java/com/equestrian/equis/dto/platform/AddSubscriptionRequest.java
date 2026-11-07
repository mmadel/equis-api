package com.equestrian.equis.dto.platform;

import com.equestrian.equis.entity.platform.BillingPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// P3, a paid subscription: a paid plan, MONTHLY or ANNUAL, and when it starts. The end is worked out by the server.
public record AddSubscriptionRequest(@NotBlank String planCode,
                                     @NotNull BillingPeriod billingPeriod,
                                     @NotNull StartMode startMode,
                                     @Size(max = 500) String note) {
}
