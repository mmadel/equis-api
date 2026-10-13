package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.config.SessionProperties;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@EnableConfigurationProperties(SessionProperties.class)
@Import({SessionTemplateService.class, SessionTemplateRules.class, SessionGenerator.class, AssignmentBookedSeats.class,
        AssignmentCancellationListener.class})
@RecordApplicationEvents
public class SessionTemplateServiceTest {
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 2;
    // app.sessions.horizon-weeks
    private static final int HORIZON_WEEKS = 8;
    // The clock is fixed on this Monday at 13:00 school time (Africa/Cairo).
    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 10, 1);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    SessionTemplateService service;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    AssignmentRepository assignmentRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    ApplicationEvents events;

    // V8 seeds sample templates; clear them so each test starts from an empty table (rolled back afterwards).
    @BeforeEach
    void clearSeededTemplates() {
        jdbcTemplate.update("DELETE FROM session_template");
    }

    @Test
    void createSavesTheTemplateAndGeneratesItsSessions() {
        SaveResult result = service.create(input(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00", 6, VALID_FROM));

        SessionTemplate template = result.template();
        assertThat(template.getId()).isNotNull();
        assertThat(template.getLevel().getId()).isEqualTo((short) LEVEL_1);
        assertThat(template.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(template.getStartTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(template.getEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(template.getCapacity()).isEqualTo(6);
        assertThat(template.getValidFrom()).isEqualTo(VALID_FROM);
        assertThat(result.warnings()).isEmpty();
        assertThat(sessionsOf(template)).hasSize(HORIZON_WEEKS);
    }

    @Test
    void createSavesAnAssessmentSlotWithoutALevel() {
        SaveResult result = service.create(input(null, DayOfWeek.MONDAY, "16:00", "17:00", 6, VALID_FROM));

        assertThat(result.template().isAssessment()).isTrue();
    }

    @Test
    void createReturnsWarningsAndStillSaves() {
        SessionTemplate first = service.create(input(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00", 6, VALID_FROM))
                .template();

        SaveResult result = service.create(input(LEVEL_1, DayOfWeek.MONDAY, "16:30", "17:30", 6, VALID_FROM));

        assertThat(result.warnings()).containsExactly(Warning.overlap(List.of(
                new OverlapRef(first.getId(), LocalTime.of(16, 0), LocalTime.of(17, 0)))));
        assertThat(sessionsOf(result.template())).hasSize(HORIZON_WEEKS);
    }

    @Test
    void createSavesNothingWhenARuleFails() {
        assertThatThrownBy(() -> service.create(input(LEVEL_1, DayOfWeek.MONDAY, "17:00", "16:00", 6, VALID_FROM)))
                .isInstanceOf(ApiException.class);

        assertThat(sessionTemplateRepository.count()).isZero();
    }

    @Test
    void updateCopiesNewTimesAndCapacityToUpcomingSessions() {
        SessionTemplate template = created(DayOfWeek.MONDAY, "16:00");

        SaveResult result = service.update(template.getId(),
                input(LEVEL_2, DayOfWeek.MONDAY, "18:00", "19:30", 8, VALID_FROM));

        assertThat(result.template().getLevel().getId()).isEqualTo((short) LEVEL_2);
        List<Session> sessions = sessionsOf(template);
        assertThat(sessions).hasSize(HORIZON_WEEKS).allSatisfy(session -> {
            assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
            assertThat(session.getStartTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(session.getEndTime()).isEqualTo(LocalTime.of(19, 30));
            assertThat(session.getCapacity()).isEqualTo(8);
            assertThat(session.getVersion()).isEqualTo(1);
        });
        assertThat(events.stream(SessionsCancelledEvent.class)).isEmpty();
    }

    @Test
    void updateLeavesASessionThatAlreadyStartedAlone() {
        // 09:00 today is before the fixed 13:00, so today's session is no longer upcoming.
        SessionTemplate template = created(DayOfWeek.MONDAY, "09:00");

        service.update(template.getId(), input(LEVEL_1, DayOfWeek.MONDAY, "18:00", "19:00", 6, VALID_FROM));

        List<Session> sessions = sessionsOf(template);
        assertThat(sessions.getFirst().getSessionDate()).isEqualTo(TODAY);
        assertThat(sessions.getFirst().getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(sessions.getFirst().getVersion()).isZero();
        assertThat(sessions.subList(1, sessions.size()))
                .allSatisfy(session -> assertThat(session.getStartTime()).isEqualTo(LocalTime.of(18, 0)));
    }

    @Test
    void updateToAnotherDayCancelsUpcomingSessionsAndGeneratesTheNewDay() {
        SessionTemplate template = created(DayOfWeek.MONDAY, "16:00");
        List<UUID> mondayIds = sessionsOf(template).stream().map(Session::getId).toList();

        service.update(template.getId(), input(LEVEL_1, DayOfWeek.TUESDAY, "16:00", "17:00", 6, VALID_FROM));

        List<Session> sessions = sessionsOf(template);
        assertThat(sessions).hasSize(2 * HORIZON_WEEKS);
        assertThat(sessions).filteredOn(session -> session.getSessionDate().getDayOfWeek() == DayOfWeek.MONDAY)
                .hasSize(HORIZON_WEEKS)
                .allSatisfy(session -> {
                    assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED);
                    assertThat(session.getCancelledAt()).isEqualTo(NOW);
                });
        assertThat(sessions).filteredOn(session -> session.getSessionDate().getDayOfWeek() == DayOfWeek.TUESDAY)
                .hasSize(HORIZON_WEEKS)
                .allSatisfy(session -> assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED));
        assertThat(events.stream(SessionsCancelledEvent.class))
                .containsExactly(new SessionsCancelledEvent(template.getId(), mondayIds, NOW));
    }

    @Test
    void updateWithALaterStartsFromCancelsTheSessionsBeforeIt() {
        SessionTemplate template = created(DayOfWeek.MONDAY, "16:00");
        List<UUID> firstTwo = sessionsOf(template).stream().limit(2).map(Session::getId).toList();

        service.update(template.getId(),
                input(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00", 6, TODAY.plusWeeks(2)));

        List<Session> sessions = sessionsOf(template);
        assertThat(sessions).hasSize(HORIZON_WEEKS);
        assertThat(sessions.subList(0, 2))
                .allSatisfy(session -> assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED));
        assertThat(sessions.subList(2, sessions.size()))
                .allSatisfy(session -> assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED));
        assertThat(events.stream(SessionsCancelledEvent.class))
                .containsExactly(new SessionsCancelledEvent(template.getId(), firstTwo, NOW));
    }

    @Test
    void updateWithAnEarlierStartsFromCancelsNothing() {
        SessionTemplate template = service.create(
                input(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00", 6, TODAY.plusWeeks(2))).template();
        assertThat(sessionsOf(template)).hasSize(HORIZON_WEEKS - 2);

        service.update(template.getId(), input(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00", 6, VALID_FROM));

        assertThat(sessionsOf(template)).hasSize(HORIZON_WEEKS)
                .allSatisfy(session -> assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED));
        assertThat(events.stream(SessionsCancelledEvent.class)).isEmpty();
    }

    @Test
    void updateOfAMissingOrDeletedTemplateIsNotFound() {
        SessionTemplate deleted = created(DayOfWeek.MONDAY, "16:00");
        service.delete(deleted.getId());
        SessionTemplateInput in = input(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00", 6, VALID_FROM);

        assertNotFound(() -> service.update(UUID.randomUUID(), in));
        assertNotFound(() -> service.update(deleted.getId(), in));
    }

    @Test
    void deleteMarksTheTemplateDeletedAndCancelsUpcomingSessions() {
        SessionTemplate template = created(DayOfWeek.MONDAY, "16:00");
        List<UUID> ids = sessionsOf(template).stream().map(Session::getId).toList();

        DeleteImpact impact = service.delete(template.getId());

        assertThat(impact).isEqualTo(new DeleteImpact(HORIZON_WEEKS, 0));
        assertThat(template.getDeletedAt()).isEqualTo(NOW);
        assertThat(sessionTemplateRepository.findByIdAndDeletedAtIsNull(template.getId())).isEmpty();
        assertThat(sessionsOf(template)).hasSize(HORIZON_WEEKS).allSatisfy(session -> {
            assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED);
            assertThat(session.getCancelledAt()).isEqualTo(NOW);
            assertThat(session.getVersion()).isEqualTo(1);
        });
        assertThat(events.stream(SessionsCancelledEvent.class))
                .containsExactly(new SessionsCancelledEvent(template.getId(), ids, NOW));
    }

    @Test
    void deleteCountsAndRemovesTheSeatsOfUpcomingSessions() {
        // 09:00 today is before the fixed 13:00, so today's session has started and keeps its seat.
        SessionTemplate template = created(DayOfWeek.MONDAY, "09:00");
        List<Session> sessions = sessionsOf(template);
        Trainer sara = trainer();
        Rider laila = rider("Laila Nour", sara);
        Rider nour = rider("Nour Adel", null);
        assign(sessions.get(0), laila, sara);
        assign(sessions.get(1), laila, sara);
        assign(sessions.get(1), nour, sara);
        assign(sessions.get(2), laila, sara);

        DeleteImpact impact = service.delete(template.getId());

        assertThat(impact).isEqualTo(new DeleteImpact(HORIZON_WEEKS - 1, 3));
        assertThat(assignmentRepository.findAll()).extracting(Assignment::getSession)
                .containsExactly(sessions.get(0));
    }

    @Test
    void updateToAnotherDayRemovesTheSeatsOfTheCancelledSessions() {
        SessionTemplate template = created(DayOfWeek.MONDAY, "16:00");
        Trainer sara = trainer();
        assign(sessionsOf(template).getFirst(), rider("Laila Nour", sara), sara);

        service.update(template.getId(), input(LEVEL_1, DayOfWeek.TUESDAY, "16:00", "17:00", 6, VALID_FROM));

        assertThat(assignmentRepository.count()).isZero();
    }

    @Test
    void updateKeepsTheSeatsOfSessionsThatStay() {
        SessionTemplate template = created(DayOfWeek.MONDAY, "16:00");
        Trainer sara = trainer();
        assign(sessionsOf(template).getFirst(), rider("Laila Nour", sara), sara);

        service.update(template.getId(), input(LEVEL_1, DayOfWeek.MONDAY, "18:00", "19:00", 6, VALID_FROM));

        assertThat(assignmentRepository.count()).isEqualTo(1);
    }

    @Test
    void deleteLeavesASessionThatAlreadyStartedAlone() {
        SessionTemplate template = created(DayOfWeek.MONDAY, "09:00");

        assertThat(service.delete(template.getId())).isEqualTo(new DeleteImpact(HORIZON_WEEKS - 1, 0));

        assertThat(sessionsOf(template).getFirst().getStatus()).isEqualTo(SessionStatus.SCHEDULED);
    }

    @Test
    void deleteWithNoUpcomingSessionsPublishesNoEvent() {
        SessionTemplate template = service.create(
                input(LEVEL_1, DayOfWeek.MONDAY, "16:00", "17:00", 6, TODAY.plusWeeks(HORIZON_WEEKS))).template();

        assertThat(service.delete(template.getId())).isEqualTo(new DeleteImpact(0, 0));

        assertThat(events.stream(SessionsCancelledEvent.class)).isEmpty();
    }

    @Test
    void deleteOfAMissingOrDeletedTemplateIsNotFound() {
        SessionTemplate deleted = created(DayOfWeek.MONDAY, "16:00");
        service.delete(deleted.getId());

        assertNotFound(() -> service.delete(UUID.randomUUID()));
        assertNotFound(() -> service.delete(deleted.getId()));
    }

    private void assertNotFound(Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.code()).isEqualTo(ErrorCodes.NOT_FOUND);
        });
    }

    private SessionTemplate created(DayOfWeek day, String start) {
        LocalTime startTime = LocalTime.parse(start);
        return service.create(new SessionTemplateInput(LEVEL_1, day, startTime, startTime.plusHours(1), 6, VALID_FROM))
                .template();
    }

    private SessionTemplateInput input(Integer levelId, DayOfWeek day, String start, String end, int capacity,
                                       LocalDate validFrom) {
        return new SessionTemplateInput(levelId, day, LocalTime.parse(start), LocalTime.parse(end), capacity,
                validFrom);
    }

    private void assign(Session session, Rider rider, Trainer trainer) {
        assignmentRepository.saveAndFlush(Assignment.of(session, rider, trainer));
    }

    private Trainer trainer() {
        Trainer trainer = new Trainer();
        trainer.setName("Sara Ali");
        trainer.setPhone("01999990001");
        trainer.setEmail("template-service-test-sara@example.com");
        return trainerRepository.saveAndFlush(trainer);
    }

    private Rider rider(String name, Trainer trainer) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(levelRepository.getReferenceById((short) LEVEL_1));
        rider.setTrainer(trainer);
        return riderRepository.saveAndFlush(rider);
    }

    private List<Session> sessionsOf(SessionTemplate template) {
        return sessionRepository.findAll().stream()
                .filter(session -> session.getTemplate().equals(template))
                .sorted(Comparator.comparing(Session::getSessionDate))
                .toList();
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
