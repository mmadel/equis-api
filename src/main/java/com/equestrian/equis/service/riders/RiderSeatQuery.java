package com.equestrian.equis.service.riders;

import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

// The one answer to "which of this rider's seats are upcoming in this level",
// so what the 4b preview promises is exactly what a level change frees.
@Component
public class RiderSeatQuery {
    private final AssignmentRepository assignments;
    private final ZoneId schoolZone;

    public RiderSeatQuery(AssignmentRepository assignments, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.assignments = assignments;
        this.schoolZone = schoolZone;
    }

    // Seats in sessions of this level that have not started, ordered by date and start.
    public List<Assignment> upcomingSeats(UUID riderId, Short levelId, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, schoolZone);
        return assignments.findFromDateByRiderId(riderId, today).stream()
                .filter(a -> a.getSession().isUpcoming(now, schoolZone))
                .filter(a -> levelId.equals(levelIdOf(a.getSession())))
                .toList();
    }

    // Null for an assessment slot.
    private static Short levelIdOf(Session s) {
        Level level = s.getTemplate().getLevel();
        return level == null ? null : level.getId();
    }
}
