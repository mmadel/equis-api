package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class SessionRepositoryTest {
    private static final short LEVEL_1 = 1;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Seeded by V4.
    private static final UUID TRAINER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
    @Autowired
    EntityManager entityManager;

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

    // E13 case 5
    @Test
    void finishedSessionIsSavedAndReadBack() {
        Session session = sessionRepository.saveAndFlush(Session.of(template("16:00"), MONDAY));
        Instant at = Instant.parse("2026-10-05T14:05:00Z");

        session.finish(TRAINER_1, at);
        sessionRepository.saveAndFlush(session);
        entityManager.clear();

        Session read = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(read.getStatus()).isEqualTo(SessionStatus.FINISHED);
        assertThat(read.isFinished()).isTrue();
        assertThat(read.getFinishedAt()).isEqualTo(at);
        assertThat(read.getFinishedBy()).isEqualTo(TRAINER_1);
        assertThat(statusInDb(read)).isEqualTo("finished");
    }

    // E13 case 2. One rejected insert per test: it aborts the test's transaction.
    @Test
    void finishedNeedsFinishedAt() {
        assertThatThrownBy(() -> insertSession(template("16:00"), MONDAY, "finished", null, TRAINER_1))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_session_finished");
    }

    @Test
    void onlyAFinishedSessionHasFinishedBy() {
        assertThatThrownBy(() -> insertSession(template("16:00"), MONDAY, "scheduled", null, TRAINER_1))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_session_finished");
    }

    // E13 case 3
    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> insertSession(template("16:00"), MONDAY, "done", null, null))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_session_status");
    }

    // E13 case 6
    @Test
    void finishOnlyFromScheduledAndAFinishedSessionCantBeCancelled() {
        Instant at = Instant.parse("2026-10-05T14:05:00Z");
        Session finished = Session.of(template("16:00"), MONDAY);
        finished.finish(TRAINER_1, at);

        assertThatThrownBy(() -> finished.finish(TRAINER_1, at)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> finished.cancel(at)).isInstanceOf(IllegalStateException.class);
        assertThat(finished.getStatus()).isEqualTo(SessionStatus.FINISHED);

        Session cancelled = Session.of(template("16:00"), MONDAY);
        cancelled.cancel(at);
        assertThatThrownBy(() -> cancelled.finish(TRAINER_1, at)).isInstanceOf(IllegalStateException.class);
        assertThat(cancelled.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(cancelled.getFinishedAt()).isNull();
    }

    // E13 case 7
    @Test
    void hasStartedFromTheStartTime() {
        Session session = Session.of(template("17:00"), MONDAY);
        Instant start = MONDAY.atTime(17, 0).atZone(CAIRO).toInstant();

        assertThat(session.hasStarted(start.minusSeconds(60), CAIRO)).isFalse();
        assertThat(session.hasStarted(start, CAIRO)).isTrue();
        assertThat(session.hasStarted(start.plusSeconds(65 * 60), CAIRO)).isTrue();
    }

    private void insertSession(SessionTemplate template, LocalDate date, String status, Instant finishedAt,
                               UUID finishedBy) {
        jdbcTemplate.update("""
                INSERT INTO session (template_id, session_date, start_time, end_time, capacity, status,
                                     finished_at, finished_by)
                VALUES (?, ?, '16:00', '17:00', 6, ?, ?, ?)
                """, template.getId(), date, status,
                finishedAt == null ? null : java.sql.Timestamp.from(finishedAt), finishedBy);
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
