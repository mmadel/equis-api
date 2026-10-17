package com.equestrian.equis.dto.overview;

import com.equestrian.equis.dto.accounts.TrainerRef;

import java.time.Instant;

// An invited trainer's unused link; expired is worked out from the clock.
public record PendingInvite(TrainerRef trainer, Instant expiresAt, boolean expired) {
}
