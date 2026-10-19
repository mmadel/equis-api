package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SessionTemplateService {
    private final SessionTemplateRepository templates;
    private final SessionRepository sessions;
    private final LevelRepository levels;
    private final SessionTemplateRules rules;
    private final SessionGenerator generator;
    private final BookedSeats bookedSeats;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final ZoneId schoolZone;

    public SessionTemplateService(SessionTemplateRepository templates, SessionRepository sessions,
                                  LevelRepository levels, SessionTemplateRules rules, SessionGenerator generator,
                                  BookedSeats bookedSeats, ApplicationEventPublisher events, Clock clock,
                                  @Value("${app.time-zone}") ZoneId schoolZone) {
        this.templates = templates;
        this.sessions = sessions;
        this.levels = levels;
        this.rules = rules;
        this.generator = generator;
        this.bookedSeats = bookedSeats;
        this.events = events;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    @Transactional(readOnly = true)
    public List<SessionTemplate> listByLevel(short levelId) {
        return templates.findByLevelIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(levelId);
    }

    @Transactional(readOnly = true)
    public List<SessionTemplate> listAssessment() {
        return templates.findByLevelIsNullAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc();
    }

    @Transactional
    public SaveResult create(SessionTemplateInput in) {
        List<Warning> warnings = rules.check(in, null);

        SessionTemplate template = new SessionTemplate();
        copy(in, template);
        templates.saveAndFlush(template);

        generator.generateFor(template);
        return new SaveResult(template, warnings);
    }

    @Transactional
    public SaveResult update(UUID id, SessionTemplateInput in) {
        SessionTemplate template = load(id);
        List<Warning> warnings = rules.check(in, template);

        boolean dayChanged = template.getDayOfWeek() != in.dayOfWeek();
        copy(in, template);

        // A session on the old day, or before the new starts-from date, no longer belongs to the template.
        // The rest take the new times and capacity.
        Instant now = clock.instant();
        List<Session> cancelled = new ArrayList<>();
        for (Session session : upcoming(id, now)) {
            if (dayChanged || session.getSessionDate().isBefore(in.validFrom())) {
                session.cancel(now);
                cancelled.add(session);
            } else {
                session.applyTemplate(template);
            }
        }
        // Flushed here so the generator's inserts see the saved template.
        templates.saveAndFlush(template);
        publishCancelled(id, cancelled, now);

        generator.generateFor(template);
        return new SaveResult(template, warnings);
    }

    // Soft delete: past sessions keep pointing at the template.
    // An assessment slot that has started but has no bookings has no results to record, so it goes too;
    // otherwise trainers would keep seeing it. It is not counted in the impact, which is about upcoming sessions.
    @Transactional
    public DeleteImpact delete(UUID id) {
        SessionTemplate template = load(id);

        Instant now = clock.instant();
        // Counted before the cancel, while the sessions are still upcoming.
        int bookings = bookedSeats.bookingsInUpcoming(id);
        List<Session> cancelled = upcoming(id, now);
        cancelled.forEach(session -> session.cancel(now));
        if (template.isAssessment()) {
            startedWithoutBookings(id, now).forEach(session -> session.cancel(now));
        }

        template.setDeletedAt(now);
        templates.saveAndFlush(template);
        publishCancelled(id, cancelled, now);

        return new DeleteImpact(cancelled.size(), bookings);
    }

    private SessionTemplate load(UUID id) {
        return templates.findByIdAndDeletedAtIsNull(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "This session template does not exist."));
    }

    // Session dates and times are school-local, so "upcoming" is judged in the school time zone.
    private List<Session> upcoming(UUID templateId, Instant now) {
        ZonedDateTime schoolNow = now.atZone(schoolZone);
        return sessions.findUpcomingByTemplate(templateId, schoolNow.toLocalDate(), schoolNow.toLocalTime());
    }

    private List<Session> startedWithoutBookings(UUID templateId, Instant now) {
        ZonedDateTime schoolNow = now.atZone(schoolZone);
        return sessions.findStartedWithoutBookingsByTemplate(templateId, schoolNow.toLocalDate(),
                schoolNow.toLocalTime());
    }

    // The rules have already checked that the level exists.
    private void copy(SessionTemplateInput in, SessionTemplate template) {
        template.setLevel(in.levelId() == null ? null : levels.getReferenceById(in.levelId().shortValue()));
        template.setDayOfWeek(in.dayOfWeek());
        template.setStartTime(in.start());
        template.setEndTime(in.end());
        template.setCapacity(in.capacity());
        template.setValidFrom(in.validFrom());
    }

    private void publishCancelled(UUID templateId, List<Session> cancelled, Instant now) {
        if (cancelled.isEmpty()) {
            return;
        }
        events.publishEvent(new SessionsCancelledEvent(templateId,
                cancelled.stream().map(Session::getId).toList(), now));
    }
}
