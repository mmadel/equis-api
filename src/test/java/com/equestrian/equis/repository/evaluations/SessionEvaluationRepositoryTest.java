package com.equestrian.equis.repository.evaluations;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.evaluations.SessionEvaluation;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E14 cases 1–7: the session_evaluation table and the done status.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SessionEvaluationRepositoryTest {
    private static final short LEVEL_2 = 2;
    private static final UUID TRAINER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 3);
    private static final Instant AT_18_05 = Instant.parse("2026-11-03T16:05:00Z");
    private static final Instant AT_18_10 = Instant.parse("2026-11-03T16:10:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    SessionEvaluationRepository evaluations;
    @Autowired
    AssignmentRepository assignments;
    @Autowired
    SessionRepository sessions;
    @Autowired
    SessionTemplateRepository templates;
    @Autowired
    RiderRepository riders;
    @Autowired
    LevelRepository levels;
    @Autowired
    TrainerRepository trainers;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager entityManager;

    Trainer ahmed;
    Session session;
    Rider sara;
    Rider omar;
    Rider hala;

    @BeforeEach
    void setUp() {
        ahmed = trainers.findById(TRAINER_1).orElseThrow();
        session = sessions.saveAndFlush(Session.of(template(), TUESDAY));
        sara = riders.saveAndFlush(rider("Sara Adel"));
        omar = riders.saveAndFlush(rider("Omar Khaled"));
        hala = riders.saveAndFlush(rider("Hala Nabil"));
        assignments.saveAndFlush(Assignment.of(session, sara, ahmed));
        assignments.saveAndFlush(Assignment.of(session, omar, ahmed));
        session.finish(ahmed.getId(), AT_18_05);
        sessions.saveAndFlush(session);
    }

    // Case 1: the context started, so Flyway ran V21 and Hibernate validated the table.
    @Test
    void theTableExistsAndAnEvaluationIsReadBack() {
        SessionEvaluation saved = evaluations.saveAndFlush(
                SessionEvaluation.of(session, sara, ahmed, 4, " Better heels down ", AT_18_10));
        entityManager.clear();

        SessionEvaluation read = evaluations.findById(saved.getId()).orElseThrow();
        assertThat(read.getStars()).isEqualTo((short) 4);
        assertThat(read.getComment()).isEqualTo("Better heels down");
        assertThat(read.getCreatedAt()).isEqualTo(AT_18_10);
        assertThat(jdbc.queryForObject("select trainer_id from session_evaluation where id = ?", UUID.class,
                saved.getId())).isEqualTo(TRAINER_1);
    }

    @Test
    void countsChecksAndListsBySession() {
        evaluations.saveAndFlush(SessionEvaluation.of(session, sara, ahmed, 4, null, AT_18_10));
        entityManager.clear();

        assertThat(evaluations.countBySessionId(session.getId())).isEqualTo(1);
        assertThat(assignments.countBySessionId(session.getId())).isEqualTo(2);
        assertThat(evaluations.existsBySessionIdAndRiderId(session.getId(), sara.getId())).isTrue();
        assertThat(evaluations.existsBySessionIdAndRiderId(session.getId(), omar.getId())).isFalse();

        List<SessionEvaluation> found = evaluations.findBySessionIdIn(List.of(session.getId()));
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getRider().getName()).isEqualTo("Sara Adel");
        assertThat(found.getFirst().getTrainer().getName()).isEqualTo("Test Trainer One");
    }

    @Test
    void aDoneSessionIsSaved() {
        assertThat(session.markDoneIfAllEvaluated(2, 2)).isTrue();
        sessions.saveAndFlush(session);

        assertThat(jdbc.queryForObject("select status from session where id = ?", String.class, session.getId()))
                .isEqualTo("done");
    }

    // Case 2
    @Test
    void aRiderNotSeatedInTheSessionCantBeEvaluated() {
        assertThatThrownBy(() -> insert(hala.getId(), 4, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_session_evaluation_seat");
    }

    // Case 3
    @Test
    void aRiderIsEvaluatedOncePerSession() {
        insert(sara.getId(), 4, null);

        assertThatThrownBy(() -> insert(sara.getId(), 5, "Again"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_session_evaluation");
    }

    // Case 4. One failing statement per test: Postgres aborts the test's transaction after it.
    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void starsMustBe1To5(int stars) {
        assertThatThrownBy(() -> insert(sara.getId(), stars, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_session_evaluation_stars");
    }

    // Case 5
    @Test
    void anEmptyCommentIsRejected() {
        assertThatThrownBy(() -> insert(sara.getId(), 4, ""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_session_evaluation_comment");
    }

    // Case 5
    @Test
    void aCommentOver500CharactersIsRejected() {
        assertThatThrownBy(() -> insert(sara.getId(), 4, "a".repeat(501)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void a500CharacterCommentIsAccepted() {
        insert(sara.getId(), 4, "a".repeat(500));
        assertThat(evaluations.countBySessionId(session.getId())).isEqualTo(1);
    }

    // Case 6
    @Test
    void aSeatWithAnEvaluationCantBeDeleted() {
        insert(sara.getId(), 4, null);

        assertThatThrownBy(() -> jdbc.update("delete from assignment where session_id = ? and rider_id = ?",
                session.getId(), sara.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_session_evaluation_seat");
    }

    // Case 7
    @Test
    void aDoneSessionNeedsItsFinishedColumns() {
        UUID other = sessions.saveAndFlush(Session.of(template(), TUESDAY.plusDays(1))).getId();

        assertThatThrownBy(() -> jdbc.update(
                "update session set status = 'done', finished_at = null, finished_by = null where id = ?", other))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_session_finished");
    }

    // Case 7
    @Test
    void aScheduledSessionCantHaveFinishedColumns() {
        UUID other = sessions.saveAndFlush(Session.of(template(), TUESDAY.plusDays(1))).getId();

        assertThatThrownBy(() -> jdbc.update(
                "update session set finished_at = now(), finished_by = ? where id = ?", TRAINER_1, other))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_session_finished");
    }

    private void insert(UUID riderId, int stars, String comment) {
        jdbc.update("""
                insert into session_evaluation (session_id, rider_id, trainer_id, stars, comment, created_at)
                values (?, ?, ?, ?, ?, ?)
                """, session.getId(), riderId, TRAINER_1, stars, comment, Timestamp.from(AT_18_10));
    }

    private SessionTemplate template() {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(level());
        template.setDayOfWeek(DayOfWeek.TUESDAY);
        template.setStartTime(LocalTime.of(17, 0));
        template.setEndTime(LocalTime.of(18, 0));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        return templates.saveAndFlush(template);
    }

    private Level level() {
        return levels.getReferenceById(LEVEL_2);
    }

    private Rider rider(String name) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(level());
        return rider;
    }
}
