package com.equestrian.equis.service.sessions;

import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

// Booked seats in a template's upcoming sessions: assignments for a level template,
// UPCOMING bookings for an assessment template.
@Component
public class TemplateBookedSeats implements BookedSeats {
    private final AssignmentRepository assignments;
    private final SessionTemplateRepository templates;
    private final AssessmentSeatCounts assessmentSeats;
    private final Clock clock;
    private final ZoneId schoolZone;

    public TemplateBookedSeats(AssignmentRepository assignments, SessionTemplateRepository templates,
                               AssessmentSeatCounts assessmentSeats, Clock clock,
                               @Value("${app.time-zone}") ZoneId schoolZone) {
        this.assignments = assignments;
        this.templates = templates;
        this.assessmentSeats = assessmentSeats;
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
        boolean assessment = templates.findById(templateId).map(SessionTemplate::isAssessment).orElse(false);
        return assessment
                ? assessmentSeats.countPerUpcomingSession(templateId, schoolNow.toLocalDate(), schoolNow.toLocalTime())
                : assignments.countPerUpcomingSession(templateId, schoolNow.toLocalDate(), schoolNow.toLocalTime());
    }
}
