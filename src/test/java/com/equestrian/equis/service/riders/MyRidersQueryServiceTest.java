package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.riders.MyRiderDto;
import com.equestrian.equis.dto.riders.UpcomingSeat;
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
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Statistics are on so the N+1 test can count SQL statements.
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(MyRidersQueryService.class)
public class MyRidersQueryServiceTest {
    private static final short LEVEL_2 = 2;
    private static final short LEVEL_3 = 3;
    private static final short LEVEL_5 = 5;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // The clock is fixed on Thursday 15 Oct 2026 at 10:00 school time.
    private static final Instant NOW = ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 17);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    MyRidersQueryService service;
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
    TestEntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    Trainer ahmed;
    Trainer mona;

    @BeforeEach
    void setUp() {
        ahmed = trainer("Ahmed Samir", "01999990021", "my-riders-test-ahmed@example.com");
        mona = trainer("Mona Fathy", "01999990022", "my-riders-test-mona@example.com");
    }

    @Test
    void listsOnlyTheCallersRidersByLevelThenNameIgnoringCase() {
        rider("Sara Mahmoud", LEVEL_3, ahmed);
        rider("omar Ali", LEVEL_3, ahmed);
        rider("Nour Saleh", LEVEL_2, ahmed);
        rider("Karim Adel", LEVEL_5, ahmed);
        rider("Hana Said", LEVEL_5, ahmed);
        rider("Laila Nabil", LEVEL_2, mona);

        List<MyRiderDto> result = service.myRiders(ahmed.getId());

        assertThat(result).extracting(MyRiderDto::name)
                .containsExactly("Nour Saleh", "omar Ali", "Sara Mahmoud", "Hana Said", "Karim Adel");
        assertThat(result).extracting(MyRiderDto::levelId).containsExactly(2, 3, 3, 5, 5);
        assertThat(result.get(1).initials()).isEqualTo("OA");
    }

    @Test
    void nextSessionIsTheEarliestThatHasNotStarted() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Session saturday1000 = session(LEVEL_3, SATURDAY, LocalTime.of(10, 0));
        Session today1800 = session(LEVEL_3, TODAY, LocalTime.of(18, 0));
        Session today0930 = session(LEVEL_3, TODAY, LocalTime.of(9, 30));
        Session yesterday = session(LEVEL_3, TODAY.minusDays(1), LocalTime.of(18, 0));
        for (Session s : List.of(saturday1000, today1800, today0930, yesterday)) {
            assign(s, sara, ahmed);
        }

        MyRiderDto row = service.myRiders(ahmed.getId()).getFirst();

        // 09:30 today has started, so it is not next.
        assertThat(row.nextSession())
                .isEqualTo(new UpcomingSeat(today1800.getId(), TODAY, LocalTime.of(18, 0), LocalTime.of(19, 0)));
    }

    @Test
    void cancelledSessionsAreNotNext() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Session cancelled = session(LEVEL_3, TODAY, LocalTime.of(12, 0));
        assign(cancelled, sara, ahmed);
        cancelled.cancel(NOW);
        sessionRepository.saveAndFlush(cancelled);
        Session saturday1000 = session(LEVEL_3, SATURDAY, LocalTime.of(10, 0));
        assign(saturday1000, sara, ahmed);

        assertThat(service.myRiders(ahmed.getId()).getFirst().nextSession().sessionId())
                .isEqualTo(saturday1000.getId());
    }

    @Test
    void aRiderWithOnlyPastSeatsHasNoNextSession() {
        Rider omar = rider("Omar Ali", LEVEL_3, ahmed);
        assign(session(LEVEL_3, TODAY.minusDays(1), LocalTime.of(18, 0)), omar, ahmed);

        assertThat(service.myRiders(ahmed.getId()).getFirst().nextSession()).isNull();
    }

    @Test
    void anotherTrainersRiderInTheSameSessionsAndUnassignedRidersAreLeftOut() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Rider monasRider = rider("Youssef Tarek", LEVEL_3, mona);
        rider("Free Rider", LEVEL_3, null);
        Session today1800 = session(LEVEL_3, TODAY, LocalTime.of(18, 0));
        Session saturday1000 = session(LEVEL_3, SATURDAY, LocalTime.of(10, 0));
        assign(saturday1000, sara, ahmed);
        // The other rider rides earlier too; that seat must not become Sara's next session.
        assign(today1800, monasRider, mona);
        assign(saturday1000, monasRider, mona);

        List<MyRiderDto> result = service.myRiders(ahmed.getId());

        assertThat(result).extracting(MyRiderDto::id).containsExactly(sara.getId());
        assertThat(result.getFirst().nextSession().sessionId()).isEqualTo(saturday1000.getId());
    }

    @Test
    void aTrainerWithNoRidersGetsAnEmptyList() {
        rider("Youssef Tarek", LEVEL_3, mona);

        assertThat(service.myRiders(ahmed.getId())).isEmpty();
    }

    @Test
    void sinceIsTheCreationDayInSchoolTime() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        // 23:30 in Cairo on 14 Oct is 20:30 UTC.
        jdbc.update("UPDATE rider SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-10-14T20:30:00Z")), sara.getId());
        em.clear();

        assertThat(service.myRiders(ahmed.getId()).getFirst().since()).isEqualTo(LocalDate.of(2026, 10, 14));
    }

    @Test
    void twentyRidersWithSeatsTakeTwoQueries() {
        // Two groups of ten, each with a seat today and one on Saturday (a session holds at most 12).
        List<Session> today = List.of(session(LEVEL_3, TODAY, LocalTime.of(17, 0)),
                session(LEVEL_3, TODAY, LocalTime.of(18, 0)));
        List<Session> saturday = List.of(session(LEVEL_3, SATURDAY, LocalTime.of(10, 0)),
                session(LEVEL_3, SATURDAY, LocalTime.of(11, 0)));
        for (int i = 0; i < 20; i++) {
            Rider rider = rider("Rider " + (char) ('A' + i), LEVEL_3, ahmed);
            assign(today.get(i / 10), rider, ahmed);
            assign(saturday.get(i / 10), rider, ahmed);
        }
        em.clear();

        Statistics statistics = em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        List<MyRiderDto> result = service.myRiders(ahmed.getId());

        assertThat(result).hasSize(20);
        for (int i = 0; i < 20; i++) {
            assertThat(result.get(i).levelId()).isEqualTo(3);
            assertThat(result.get(i).nextSession().sessionId()).isEqualTo(today.get(i / 10).getId());
        }
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    private Assignment assign(Session session, Rider rider, Trainer by) {
        return assignmentRepository.saveAndFlush(Assignment.of(session, rider, by));
    }

    private Session session(short levelId, LocalDate date, LocalTime start) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(date.getDayOfWeek());
        template.setStartTime(start);
        template.setEndTime(start.plusHours(1));
        template.setCapacity(12);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        template = sessionTemplateRepository.saveAndFlush(template);
        return sessionRepository.saveAndFlush(Session.of(template, date));
    }

    private Rider rider(String name, short levelId, Trainer trainer) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setPhone("01012345678");
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
