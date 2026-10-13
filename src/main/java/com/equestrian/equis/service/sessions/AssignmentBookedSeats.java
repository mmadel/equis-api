package com.equestrian.equis.service.sessions;

import com.equestrian.equis.repository.sessions.AssignmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

// Booked seats are the assignments in the template's upcoming sessions.
@Component
public class AssignmentBookedSeats implements BookedSeats {
    private final AssignmentRepository assignments;
    private final Clock clock;
    private final ZoneId schoolZone;

    public AssignmentBookedSeats(AssignmentRepository assignments, Clock clock,
                                 @Value("${app.time-zone}") ZoneId schoolZone) {
        this.assignments = assignments;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    @Override
    public int maxBookedInUpcoming(UUID templateId) {
        return Math.toIntExact(perUpcomingSession(templateId).stream().mapToLong(Long::longValue).max().orElse(0));
    }

    @Override
    public int bookingsInUpcoming(UUID templateId) {
        return Math.toIntExact(perUpcomingSession(templateId).stream().mapToLong(Long::longValue).sum());
    }

    // Session dates and times are school-local, so "upcoming" is judged in the school time zone.
    private List<Long> perUpcomingSession(UUID templateId) {
        ZonedDateTime schoolNow = clock.instant().atZone(schoolZone);
        return assignments.countPerUpcomingSession(templateId, schoolNow.toLocalDate(), schoolNow.toLocalTime());
    }
}
