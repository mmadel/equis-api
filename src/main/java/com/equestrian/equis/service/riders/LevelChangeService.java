package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.riders.LevelChangeRequest;
import com.equestrian.equis.dto.riders.LevelChanged;
import com.equestrian.equis.dto.riders.UpcomingSeat;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.riders.LevelChange;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.LevelChangeRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LevelChangeService {
    private final RiderRepository riders;
    private final AssignmentRepository assignments;
    private final LevelRepository levels;
    private final TrainerRepository trainers;
    private final LevelChangeRepository changes;
    private final RiderSeatQuery seats;
    private final LevelChangeRules rules;
    private final Clock clock;

    public LevelChangeService(RiderRepository riders, AssignmentRepository assignments, LevelRepository levels,
                              TrainerRepository trainers, LevelChangeRepository changes, RiderSeatQuery seats,
                              LevelChangeRules rules, Clock clock) {
        this.riders = riders;
        this.assignments = assignments;
        this.levels = levels;
        this.trainers = trainers;
        this.changes = changes;
        this.seats = seats;
        this.rules = rules;
        this.clock = clock;
    }

    // Moves the trainer's rider one level and frees their upcoming seats in the old level, all or nothing.
    @Transactional
    public LevelChanged change(UUID riderId, UUID trainerId, LevelChangeRequest req) {
        Instant now = clock.instant();

        // The lock makes a second change, or a seat save for this rider, wait until this one commits.
        Rider rider = riders.findByIdForUpdate(riderId).orElseThrow(rules::riderNotFound);
        rules.requireOwnRider(rider, trainerId);
        rules.requireExpectedLevel(rider, req.fromLevelId());
        int to = rules.targetLevel(rider, req.direction());

        Level from = rider.getLevel();
        List<Assignment> freed = seats.upcomingSeats(riderId, from.getId(), now);
        assignments.deleteAllInBatch(freed);

        Level toLevel = levels.getReferenceById((short) to);
        rider.moveToLevel(toLevel);
        LevelChange change = changes.save(
                LevelChange.of(rider, from, toLevel, req.reason(), trainers.getReferenceById(trainerId)));

        return new LevelChanged(rider.getId(), from.getId(), to, change.getReason(), now,
                freed.stream().map(a -> UpcomingSeat.of(a.getSession())).toList());
    }
}
