package com.equestrian.equis.repository.sessions;

import java.util.UUID;

// One taken seat of a session and the trainer behind it: the rider's trainer in a level session, the assessor in an
// assessment slot. trainerName is null when the rider has no trainer.
public record SeatHolder(UUID sessionId, String trainerName) {
}
