package com.equestrian.equis.dto.riders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Everything Rider level (4a) and its confirm sheet (4b) show. upcomingSeats are the seats a level change
// would free, in date order; history is newest first with the placement, if any, last.
public record RiderLevelDto(UUID id, String name, String initials, int levelId, LocalDate since,
                            boolean canPromote, boolean canDemote,
                            List<UpcomingSeat> upcomingSeats, List<LevelHistoryEntry> history) {
}
