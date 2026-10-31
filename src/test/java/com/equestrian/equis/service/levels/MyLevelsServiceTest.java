package com.equestrian.equis.service.levels;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.levels.LevelRef;
import com.equestrian.equis.dto.levels.LevelSessionsResponse;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.Day;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.Seat;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.SessionSlot;
import com.equestrian.equis.dto.levels.MyLevelSummary;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(MyLevelsService.class)
public class MyLevelsServiceTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_2 = 2;
    // The clock is fixed on this Monday at 13:00 school time (Africa/Cairo); its week is Sat 3 to Fri 9 October.
    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 3);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 9);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    MyLevelsService service;
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
        sara = trainer("Sara Ali", "01999990011", "my-levels-service-test-sara@example.com");
        omar = trainer("Omar Hany", "01999990012", "my-levels-service-test-omar@example.com");
        laila = rider("Laila Nour", LEVEL_1, sara);
        nour = rider("Nour Adel", LEVEL_1, sara);
        rider("Mariam Fouad", LEVEL_2, sara);
        omarsRider = rider("Youssef Tarek", LEVEL_1, omar);
    }

    @Test
    void summariesCoverAllSixLevelsWithThisWeeksNumbers() {
        Session first = session(LEVEL_1, SATURDAY, LocalTime.of(8, 0), 6);
        Session last = session(LEVEL_1, FRIDAY, LocalTime.of(8, 0), 4);
        assign(first, laila, sara);
        assign(first, omarsRider, omar);
        assign(last, nour, sara);
        // None of these count: next week, last week, cancelled, assessment.
        assign(session(LEVEL_1, FRIDAY.plusDays(1), LocalTime.of(8, 0), 6), laila, sara);
        session(LEVEL_1, SATURDAY.minusDays(1), LocalTime.of(8, 0), 6);
        assign(cancelledSession(LEVEL_1, TODAY.plusDays(1)), laila, sara);
        session(null, TODAY, LocalTime.of(8, 0), 6);

        List<MyLevelSummary> summaries = service.summaries(sara.getId());

        assertThat(summaries).extracting(MyLevelSummary::id).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(summaries.get(0)).isEqualTo(new MyLevelSummary(1, "Level 1", "#17695B", 2, 2, 3, 10));
        assertThat(summaries.get(1)).isEqualTo(new MyLevelSummary(2, "Level 2", "#2A559A", 0, 1, 0, 0));
        assertThat(summaries.get(5)).isEqualTo(new MyLevelSummary(6, "Level 6", "#4F5B1C", 0, 0, 0, 0));
    }

    @Test
    void sessionsAreGroupedByDateInTimeOrderWithTheirSeats() {
        Session late = session(LEVEL_1, TODAY, LocalTime.of(16, 0), 6);
        Session early = session(LEVEL_1, TODAY, LocalTime.of(9, 0), 4);
        Session friday = session(LEVEL_1, FRIDAY, LocalTime.of(8, 0), 6);
        assign(late, omarsRider, omar);
        assign(late, nour, sara);
        assign(late, laila, sara);
        // Left out: cancelled, another level, outside the range.
        cancelledSession(LEVEL_1, TODAY.plusDays(1));
        session(LEVEL_2, TODAY, LocalTime.of(8, 0), 6);
        session(LEVEL_1, FRIDAY.plusDays(1), LocalTime.of(8, 0), 6);

        LevelSessionsResponse response = service.sessions(sara.getId(), LEVEL_1, SATURDAY, FRIDAY);

        assertThat(response.level()).isEqualTo(new LevelRef(1, "Level 1", "#17695B"));
        assertThat(response.from()).isEqualTo(SATURDAY);
        assertThat(response.to()).isEqualTo(FRIDAY);
        assertThat(response.days()).extracting(Day::date).containsExactly(TODAY, FRIDAY);
        assertThat(response.days().get(0).sessions()).containsExactly(
                // Started at 09:00, before the fixed 13:00.
                new SessionSlot(early.getId(), LocalTime.of(9, 0), LocalTime.of(10, 0), "scheduled", 4, 0, true,
                        List.of()),
                new SessionSlot(late.getId(), LocalTime.of(16, 0), LocalTime.of(17, 0), "scheduled", 6, 3, false,
                        List.of(
                                new Seat(true, laila.getId(), "LN"),
                                new Seat(true, nour.getId(), "NA"),
                                Seat.other())));
        assertThat(response.days().get(1).sessions()).extracting(SessionSlot::id).containsExactly(friday.getId());
    }

    @Test
    void unknownLevelIsNotFound() {
        assertRefused(() -> service.sessions(sara.getId(), (short) 7, SATURDAY, FRIDAY),
                HttpStatus.NOT_FOUND, ErrorCodes.LEVEL_NOT_FOUND);
    }

    @Test
    void rangeMustBeInOrderAndAtMost56Days() {
        assertRefused(() -> service.sessions(sara.getId(), LEVEL_1, FRIDAY, SATURDAY),
                HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE);
        assertRefused(() -> service.sessions(sara.getId(), LEVEL_1, SATURDAY, SATURDAY.plusDays(56)),
                HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE);

        assertThat(service.sessions(sara.getId(), LEVEL_1, SATURDAY, SATURDAY.plusDays(55)).days()).isEmpty();
        assertThat(service.sessions(sara.getId(), LEVEL_1, SATURDAY, SATURDAY).days()).isEmpty();
    }

    private void assertRefused(Runnable call, HttpStatus status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(status);
            assertThat(ex.code()).isEqualTo(code);
        });
    }

    private Assignment assign(Session session, Rider rider, Trainer trainer) {
        return assignmentRepository.saveAndFlush(Assignment.of(session, rider, trainer));
    }

    private Session session(Short levelId, LocalDate date, LocalTime start, int capacity) {
        return sessionRepository.saveAndFlush(Session.of(template(levelId, start, capacity), date));
    }

    private Session cancelledSession(Short levelId, LocalDate date) {
        Session session = Session.of(template(levelId, LocalTime.of(8, 0), 6), date);
        session.cancel(NOW);
        return sessionRepository.saveAndFlush(session);
    }

    private SessionTemplate template(Short levelId, LocalTime start, int capacity) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(start);
        template.setEndTime(start.plusHours(1));
        template.setCapacity(capacity);
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
