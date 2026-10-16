package com.equestrian.equis.repository.notices;

import java.time.Instant;
import java.util.UUID;

// The newest notice time for one ref (booking or session).
public record RefTime(UUID refId, Instant at) {
}
