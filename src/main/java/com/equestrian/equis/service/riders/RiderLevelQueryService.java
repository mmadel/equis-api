package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.riders.LevelHistoryEntry;
import com.equestrian.equis.dto.riders.RiderLevelDto;
import com.equestrian.equis.dto.riders.UpcomingSeat;
import com.equestrian.equis.entity.levels.Levels;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

// Read side of Rider level (4a) and the impact on its confirm sheet (4b).
@Service
@Transactional(readOnly = true)
public class RiderLevelQueryService {
    private final RiderRepository riders;
    private final LevelHistoryQuery levelHistory;
    private final RiderSeatQuery seats;
    private final LevelChangeRules rules;
    private final Clock clock;
    private final ZoneId schoolZone;

    public RiderLevelQueryService(RiderRepository riders, LevelHistoryQuery levelHistory, RiderSeatQuery seats,
                                  LevelChangeRules rules, Clock clock,
                                  @Value("${app.time-zone}") ZoneId schoolZone) {
        this.riders = riders;
        this.levelHistory = levelHistory;
        this.seats = seats;
        this.rules = rules;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // The trainer's own rider only. Four queries at most: rider, seats, history, placement.
    public RiderLevelDto riderLevel(UUID riderId, UUID trainerId) {
        Rider rider = riders.findById(riderId).orElseThrow(rules::riderNotFound);
        rules.requireOwnRider(rider, trainerId);

        Instant now = clock.instant();
        short levelId = rider.getLevel().getId();
        // The same query a level change frees seats with, so the 4b preview matches the confirm.
        List<UpcomingSeat> upcoming = seats.upcomingSeats(riderId, levelId, now).stream()
                .map(a -> UpcomingSeat.of(a.getSession()))
                .toList();

        List<LevelHistoryEntry> history = levelHistory.history(rider);

        return new RiderLevelDto(rider.getId(), rider.getName(), rider.initials(), levelId,
                LocalDate.ofInstant(rider.getCreatedAt(), schoolZone),
                levelId < Levels.MAX_LEVEL, levelId > Levels.MIN_LEVEL,
                upcoming, history);
    }
}
