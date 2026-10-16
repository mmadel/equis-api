package com.equestrian.equis.dto.accounts;

import java.time.Instant;
import java.util.UUID;

// One row of W4. status is "invited", "active" or "disabled"; pendingLink is null without an open link.
public record TrainerRow(UUID id, String name, String phone, String email, String status,
                         RiderCounts riders, long assessmentsThisMonth, Instant lastActiveAt,
                         PendingLink pendingLink, Instant createdAt) {
}
