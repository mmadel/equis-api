package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.Trainer;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(SessionDetailService.class)
public class SessionDetailServiceTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_2 = 2;
    // The clock is fixed on this Monday at 13:00 school time (Africa/Cairo).
    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    SessionDetailService service;
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

    Trainer sara;
    Trainer omar;
    Rider laila;
    Rider nour;
    Rider omarsRider;

    @BeforeEach
    void setUp() {
        sara = trainer("Sara Ali", "01999990021", "session-detail-service-test-sara@example.com");
        omar = trainer("Omar Hany", "01999990022", "session-detail-service-test-omar@example.com");
        nour = rider("Nour Adel", LEVEL_1, sara);
        laila = rider("Laila Nour", LEVEL_1, sara);
        rider("Mariam Fouad", LEVEL_2, sara);
        omarsRider = rider("Youssef Tarek", LEVEL_1, omar);
    }

    @Test
    void returnsTheSessionWithEverySeatAndTheCallersRidersOfItsLevel() {
        Session session = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1), TODAY));
        Assignment mine = assign(session, laila, sara);
        Assignment others = assign(session, omarsRider, omar);

        SessionDetail detail = service.get(sara.getId(), session.getId());

        assertThat(detail.session()).isEqualTo(session);
        assertThat(detail.session().getTemplate().getLevel().getName()).isEqualTo("Level 1");
        assertThat(detail.seats()).containsExactlyInAnyOrder(mine, others);
        assertThat(detail.myRiders()).containsExactly(laila, nour);
        assertThat(detail.readOnly()).isFalse();
    }

    @Test
    void startedAndCancelledSessionsAreReadOnly() {
        Session past = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1), TODAY.minusWeeks(1)));
        Session cancelled = Session.of(template(LEVEL_1), TODAY.plusWeeks(1));
        cancelled.cancel(NOW);
        sessionRepository.saveAndFlush(cancelled);

        assertThat(service.get(sara.getId(), past.getId()).readOnly()).isTrue();
        assertThat(service.get(sara.getId(), cancelled.getId()).readOnly()).isTrue();
    }

    @Test
    void missingAndAssessmentSessionsAreNotFound() {
        Session assessment = sessionRepository.saveAndFlush(Session.of(template(null), TODAY));

        for (UUID id : new UUID[]{UUID.randomUUID(), assessment.getId()}) {
            assertThatThrownBy(() -> service.get(sara.getId(), id))
                    .isInstanceOfSatisfying(ApiException.class, ex -> {
                        assertThat(ex.status()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ex.code()).isEqualTo(ErrorCodes.SESSION_NOT_FOUND);
                    });
        }
    }

    private Assignment assign(Session session, Rider rider, Trainer trainer) {
        return assignmentRepository.saveAndFlush(Assignment.of(session, rider, trainer));
    }

    private SessionTemplate template(Short levelId) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(LocalTime.of(16, 0));
        template.setEndTime(LocalTime.of(17, 0));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    private Rider rider(String name, short levelId, Trainer trainer) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(levelRepository.getReferenceById(levelId));
        rider.setTrainer(trainer);
        return riderRepository.saveAndFlush(rider);
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainerRepository.saveAndFlush(trainer);
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
