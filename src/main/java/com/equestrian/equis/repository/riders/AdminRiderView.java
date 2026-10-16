package com.equestrian.equis.repository.riders;

import com.equestrian.equis.entity.accounts.TrainerStatus;

import java.time.Instant;
import java.util.UUID;

// A rider with their trainer and the assessor of their placed booking, flat. The trainer fields, or the assessor
// fields, are all null when there is none.
public record AdminRiderView(UUID id, String name, String phone, Short levelId, Instant createdAt,
                             UUID trainerId, String trainerName, TrainerStatus trainerStatus,
                             UUID assessorId, String assessorName, TrainerStatus assessorStatus) {
}
