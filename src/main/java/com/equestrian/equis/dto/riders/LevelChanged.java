package com.equestrian.equis.dto.riders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// The answer to a level change. freedSeats are the removed upcoming seats of the old level, in date order.
public record LevelChanged(UUID riderId, int fromLevelId, int toLevelId,
                           String reason, Instant changedAt,
                           List<UpcomingSeat> freedSeats) {
}
