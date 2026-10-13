package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class SessionRepositoryTest {
    private static final short LEVEL_1 = 1;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    // 2026-10-05 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void factoryCopiesTemplateAndSavesAsScheduled() {
        SessionTemplate template = template("16:00");

        Session saved = sessionRepository.saveAndFlush(Session.of(template, MONDAY));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTemplate()).isEqualTo(template);
        assertThat(saved.getSessionDate()).isEqualTo(MONDAY);
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(saved.getCapacity()).isEqualTo(6);
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
        assertThat(saved.getCancelledAt()).isNull();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void storesStatusLowercase() {
        Session session = sessionRepository.saveAndFlush(Session.of(template("16:00"), MONDAY));
        assertThat(statusInDb(session)).isEqualTo("scheduled");

        session.cancel(Instant.now());
        sessionRepository.saveAndFlush(session);
        assertThat(statusInDb(session)).isEqualTo("cancelled");
        assertThat(session.getVersion()).isEqualTo(1);
    }

    @Test
    void cancellingTwiceKeepsTheFirstCancelledAt() {
        Session session = Session.of(template("16:00"), MONDAY);
        Instant first = Instant.parse("2026-10-01T10:00:00Z");

        session.cancel(first);
        session.cancel(first.plusSeconds(60));

        assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(session.getCancelledAt()).isEqualTo(first);
    }

    @Test
    void applyTemplateCopiesTimesAndCapacityAgain() {
        SessionTemplate template = template("16:00");
        Session session = Session.of(template, MONDAY);

        template.setStartTime(LocalTime.of(18, 0));
        template.setEndTime(LocalTime.of(19, 30));
        template.setCapacity(4);
        session.applyTemplate(template);

        assertThat(session.getStartTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(session.getEndTime()).isEqualTo(LocalTime.of(19, 30));
        assertThat(session.getCapacity()).isEqualTo(4);
    }

    @Test
    void startsAtAndIsUpcomingUseTheGivenZone() {
        Session session = Session.of(template("16:00"), MONDAY);
        Instant start = MONDAY.atTime(16, 0).atZone(CAIRO).toInstant();

        assertThat(session.startsAt(CAIRO).toInstant()).isEqualTo(start);
        assertThat(session.isUpcoming(start.minusSeconds(1), CAIRO)).isTrue();
        assertThat(session.isUpcoming(start, CAIRO)).isFalse();
        assertThat(session.isUpcoming(start.plusSeconds(1), CAIRO)).isFalse();
    }

    @Test
    void findsUpcomingScheduledSessionsOfTheTemplate() {
        SessionTemplate template = template("16:00");
        SessionTemplate other = template("16:00");
        sessionRepository.saveAndFlush(Session.of(template, MONDAY.minusWeeks(1)));
        Session today = sessionRepository.saveAndFlush(Session.of(template, MONDAY));
        Session nextWeek = sessionRepository.saveAndFlush(Session.of(template, MONDAY.plusWeeks(1)));
        Session cancelled = Session.of(template, MONDAY.plusWeeks(2));
        cancelled.cancel(Instant.now());
        sessionRepository.saveAndFlush(cancelled);
        sessionRepository.saveAndFlush(Session.of(other, MONDAY.plusWeeks(1)));

        assertThat(sessionRepository.findUpcomingByTemplate(template.getId(), MONDAY, LocalTime.of(15, 59)))
                .containsExactly(today, nextWeek);
        // A session starting exactly now is no longer upcoming.
        assertThat(sessionRepository.findUpcomingByTemplate(template.getId(), MONDAY, LocalTime.of(16, 0)))
                .containsExactly(nextWeek);
    }

    @Test
    void existsByTemplateAndDate() {
        SessionTemplate template = template("16:00");
        sessionRepository.saveAndFlush(Session.of(template, MONDAY));

        assertThat(sessionRepository.existsByTemplateIdAndSessionDate(template.getId(), MONDAY)).isTrue();
        assertThat(sessionRepository.existsByTemplateIdAndSessionDate(template.getId(), MONDAY.plusWeeks(1))).isFalse();
    }

    private String statusInDb(Session session) {
        return jdbcTemplate.queryForObject("SELECT status FROM session WHERE id = ?", String.class, session.getId());
    }

    private SessionTemplate template(String start) {
        LocalTime startTime = LocalTime.parse(start);
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelRepository.getReferenceById(LEVEL_1));
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(startTime);
        template.setEndTime(startTime.plusHours(1));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 10, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }
}
