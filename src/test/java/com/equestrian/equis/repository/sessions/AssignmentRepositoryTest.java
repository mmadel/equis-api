package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class AssignmentRepositoryTest {
    private static final short LEVEL_1 = 1;

    // 2026-10-05 is a Monday.
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    AssignmentRepository assignmentRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    EntityManager entityManager;

    Trainer sara;
    Rider laila;
    Rider nour;
    SessionTemplate template;

    @BeforeEach
    void setUp() {
        sara = trainerRepository.saveAndFlush(trainer("Sara Ali", "01999990001", "assignment-test-sara@example.com"));
        laila = riderRepository.saveAndFlush(rider("Laila Nour"));
        nour = riderRepository.saveAndFlush(rider("Nour Adel"));
        template = template();
    }

    @Test
    void factorySavesWithGeneratedIdAndTimestamp() {
        Session session = session(MONDAY);

        Assignment saved = assignmentRepository.saveAndFlush(Assignment.of(session, laila, sara));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSession()).isEqualTo(session);
        assertThat(saved.getRider()).isEqualTo(laila);
        assertThat(saved.getAssignedBy()).isEqualTo(sara);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void aRiderHasOneSeatPerSession() {
        Session session = session(MONDAY);
        assignmentRepository.saveAndFlush(Assignment.of(session, laila, sara));

        assertThatThrownBy(() -> assignmentRepository.saveAndFlush(Assignment.of(session, laila, sara)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsAssignmentsOfTheGivenSessionsWithTheirRiders() {
        Session first = session(MONDAY);
        Session second = session(MONDAY.plusWeeks(1));
        Session other = session(MONDAY.plusWeeks(2));
        Assignment a = assignmentRepository.saveAndFlush(Assignment.of(first, laila, sara));
        Assignment b = assignmentRepository.saveAndFlush(Assignment.of(first, nour, sara));
        Assignment c = assignmentRepository.saveAndFlush(Assignment.of(second, laila, sara));
        assignmentRepository.saveAndFlush(Assignment.of(other, laila, sara));
        entityManager.clear();

        List<Assignment> found = assignmentRepository.findBySessionIdIn(List.of(first.getId(), second.getId()));

        assertThat(found).containsExactlyInAnyOrder(a, b, c);
        assertThat(found).allSatisfy(assignment ->
                assertThat(Hibernate.isInitialized(assignment.getRider())).isTrue());
        assertThat(assignmentRepository.findBySessionIdIn(List.of())).isEmpty();
    }

    @Test
    void countsTheSeatsOfOneSession() {
        Session first = session(MONDAY);
        Session second = session(MONDAY.plusWeeks(1));
        assignmentRepository.saveAndFlush(Assignment.of(first, laila, sara));
        assignmentRepository.saveAndFlush(Assignment.of(first, nour, sara));

        assertThat(assignmentRepository.countBySessionId(first.getId())).isEqualTo(2);
        assertThat(assignmentRepository.countBySessionId(second.getId())).isZero();
    }

    @Test
    void deletesTheAssignmentsOfTheGivenSessionsOnly() {
        Session first = session(MONDAY);
        Session second = session(MONDAY.plusWeeks(1));
        Session kept = session(MONDAY.plusWeeks(2));
        assignmentRepository.saveAndFlush(Assignment.of(first, laila, sara));
        assignmentRepository.saveAndFlush(Assignment.of(second, laila, sara));
        assignmentRepository.saveAndFlush(Assignment.of(kept, laila, sara));

        assertThat(assignmentRepository.deleteBySessionIdIn(List.of(first.getId(), second.getId()))).isEqualTo(2);

        assertThat(assignmentRepository.countBySessionId(first.getId())).isZero();
        assertThat(assignmentRepository.countBySessionId(second.getId())).isZero();
        assertThat(assignmentRepository.countBySessionId(kept.getId())).isEqualTo(1);
    }

    @Test
    void countsSeatsPerUpcomingScheduledSessionOfTheTemplate() {
        Session past = session(MONDAY.minusWeeks(1));
        Session today = session(MONDAY);
        Session nextWeek = session(MONDAY.plusWeeks(1));
        Session cancelled = Session.of(template, MONDAY.plusWeeks(2));
        cancelled.cancel(Instant.now());
        sessionRepository.saveAndFlush(cancelled);
        session(MONDAY.plusWeeks(3));
        Session otherTemplate = sessionRepository.saveAndFlush(Session.of(template(), MONDAY.plusWeeks(1)));
        assignmentRepository.saveAndFlush(Assignment.of(past, laila, sara));
        assignmentRepository.saveAndFlush(Assignment.of(today, laila, sara));
        assignmentRepository.saveAndFlush(Assignment.of(nextWeek, laila, sara));
        assignmentRepository.saveAndFlush(Assignment.of(nextWeek, nour, sara));
        assignmentRepository.saveAndFlush(Assignment.of(cancelled, laila, sara));
        assignmentRepository.saveAndFlush(Assignment.of(otherTemplate, laila, sara));

        assertThat(assignmentRepository.countPerUpcomingSession(template.getId(), MONDAY, LocalTime.of(15, 59)))
                .containsExactlyInAnyOrder(1L, 2L);
        // A session starting exactly now is no longer upcoming.
        assertThat(assignmentRepository.countPerUpcomingSession(template.getId(), MONDAY, LocalTime.of(16, 0)))
                .containsExactly(2L);
    }

    private Session session(LocalDate date) {
        return sessionRepository.saveAndFlush(Session.of(template, date));
    }

    private SessionTemplate template() {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelRepository.getReferenceById(LEVEL_1));
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(LocalTime.of(16, 0));
        template.setEndTime(LocalTime.of(17, 0));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 10, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    private Rider rider(String name) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(levelRepository.getReferenceById(LEVEL_1));
        rider.setTrainer(sara);
        return rider;
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainer;
    }
}
