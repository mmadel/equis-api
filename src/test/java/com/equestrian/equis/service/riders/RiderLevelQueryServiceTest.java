package com.equestrian.equis.service.riders;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.riders.Direction;
import com.equestrian.equis.dto.riders.LevelChangeRequest;
import com.equestrian.equis.dto.riders.LevelChanged;
import com.equestrian.equis.dto.riders.LevelHistoryEntry;
import com.equestrian.equis.dto.riders.RiderLevelDto;
import com.equestrian.equis.dto.riders.UpcomingSeat;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.riders.LevelChange;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.LevelChangeRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.support.TestMembers;
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
import org.springframework.http.HttpStatus;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Statistics are on so the query-count test can count SQL statements.
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({RiderLevelQueryService.class, LevelChangeService.class, LevelChangeRules.class, RiderSeatQuery.class})
public class RiderLevelQueryServiceTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_3 = 3;
    private static final short LEVEL_4 = 4;
    private static final short LEVEL_6 = 6;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // The clock is fixed on Thursday 15 Oct 2026 at 10:00 school time.
    private static final Instant NOW = ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 17);
    private static final Instant PLACED_AT = Instant.parse("2026-10-11T08:40:00Z");
    private static final Instant PROMOTED_AT = Instant.parse("2026-10-13T09:05:00Z");
    private static final Instant DEMOTED_AT = Instant.parse("2026-10-14T15:20:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    RiderLevelQueryService service;
    @Autowired
    LevelChangeService levelChangeService;
    @Autowired
    LevelChangeRepository levelChangeRepository;
    @Autowired
    AssessmentBookingRepository bookingRepository;
    @Autowired
    MemberRepository memberRepository;
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
        ahmed = trainer("Ahmed Fathy", "01999992001", "rider-level-test-ahmed@example.com");
        mona = trainer("Mona Samir", "01999992002", "rider-level-test-mona@example.com");
    }

    @Test
    void placedPromotedAndDemotedRiderGetsTheWholePage() {
        Rider sara = placedRider("Sara Mahmoud", LEVEL_3, ahmed, PLACED_AT);
        setCreatedAt(sara, PLACED_AT);
        change(sara, LEVEL_3, LEVEL_4, "Ready for canter", ahmed, PROMOTED_AT);
        change(sara, LEVEL_4, LEVEL_3, "Lost confidence", ahmed, DEMOTED_AT);
        Session today0930 = session(LEVEL_3, TODAY, LocalTime.of(9, 30));
        Session today1800 = session(LEVEL_3, TODAY, LocalTime.of(18, 0));
        Session saturday1000 = session(LEVEL_3, SATURDAY, LocalTime.of(10, 0));
        for (Session s : List.of(today0930, today1800, saturday1000)) {
            assign(s, sara, ahmed);
        }
        em.clear();

        RiderLevelDto page = service.riderLevel(sara.getId(), ahmed.getId());

        assertThat(page.id()).isEqualTo(sara.getId());
        assertThat(page.name()).isEqualTo("Sara Mahmoud");
        assertThat(page.initials()).isEqualTo("SM");
        assertThat(page.levelId()).isEqualTo(3);
        assertThat(page.since()).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(page.canPromote()).isTrue();
        assertThat(page.canDemote()).isTrue();
        // 09:30 today has started, so a change would not free it.
        assertThat(page.upcomingSeats()).containsExactly(
                new UpcomingSeat(today1800.getId(), TODAY, LocalTime.of(18, 0), LocalTime.of(19, 0)),
                new UpcomingSeat(saturday1000.getId(), SATURDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        assertThat(page.history()).containsExactly(
                new LevelHistoryEntry("demoted", 4, 3, "Lost confidence", "Ahmed Fathy", DEMOTED_AT),
                new LevelHistoryEntry("promoted", 3, 4, "Ready for canter", "Ahmed Fathy", PROMOTED_AT),
                new LevelHistoryEntry("placed", null, 3, null, "Ahmed Fathy", PLACED_AT));
    }

    @Test
    void seedRiderWithoutMemberOrChangesHasNoHistory() {
        Rider omar = rider("Omar Ali", LEVEL_3, ahmed);
        em.clear();

        RiderLevelDto page = service.riderLevel(omar.getId(), ahmed.getId());

        assertThat(page.history()).isEmpty();
        assertThat(page.upcomingSeats()).isEmpty();
    }

    @Test
    void theEndsOfTheLadderTurnOffOneButton() {
        Rider top = rider("Karim Adel", LEVEL_6, ahmed);
        Rider bottom = rider("Nour Saleh", LEVEL_1, ahmed);
        em.clear();

        RiderLevelDto topPage = service.riderLevel(top.getId(), ahmed.getId());
        RiderLevelDto bottomPage = service.riderLevel(bottom.getId(), ahmed.getId());

        assertThat(topPage.canPromote()).isFalse();
        assertThat(topPage.canDemote()).isTrue();
        assertThat(bottomPage.canPromote()).isTrue();
        assertThat(bottomPage.canDemote()).isFalse();
    }

    @Test
    void runningAndPastSeatsAreNotUpcoming() {
        Rider omar = rider("Omar Ali", LEVEL_3, ahmed);
        assign(session(LEVEL_3, TODAY, LocalTime.of(9, 30)), omar, ahmed);
        assign(session(LEVEL_3, TODAY.minusDays(1), LocalTime.of(18, 0)), omar, ahmed);
        em.clear();

        assertThat(service.riderLevel(omar.getId(), ahmed.getId()).upcomingSeats()).isEmpty();
    }

    @Test
    void placementKeepsTheAssessorAfterAReassignment() {
        Rider laila = placedRider("Laila Nabil", LEVEL_3, mona, PLACED_AT);
        jdbc.update("UPDATE rider SET trainer_id = ? WHERE id = ?", ahmed.getId(), laila.getId());
        em.clear();

        RiderLevelDto page = service.riderLevel(laila.getId(), ahmed.getId());

        assertThat(page.history()).singleElement().satisfies(e -> {
            assertThat(e.type()).isEqualTo("placed");
            assertThat(e.byName()).isEqualTo("Mona Samir");
        });
    }

    @Test
    void anotherTrainersUnassignedAndUnknownRidersAreNotFound() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Rider free = rider("Free Rider", LEVEL_3, null);
        em.clear();

        assertNotFound(sara.getId(), mona.getId());
        assertNotFound(free.getId(), ahmed.getId());
        assertNotFound(UUID.randomUUID(), ahmed.getId());
    }

    @Test
    void upcomingSeatsAreWhatAPromoteFrees() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        assign(session(LEVEL_3, TODAY, LocalTime.of(9, 30)), sara, ahmed);
        assign(session(LEVEL_3, TODAY, LocalTime.of(18, 0)), sara, ahmed);
        assign(session(LEVEL_3, SATURDAY, LocalTime.of(10, 0)), sara, ahmed);
        // A seat in another level's session is not one this change frees.
        assign(session(LEVEL_4, SATURDAY, LocalTime.of(12, 0)), sara, ahmed);
        em.clear();

        List<UpcomingSeat> preview = service.riderLevel(sara.getId(), ahmed.getId()).upcomingSeats();
        LevelChanged changed = levelChangeService.change(sara.getId(), ahmed.getId(),
                new LevelChangeRequest(Direction.UP, 3, "Ready for canter"));

        assertThat(preview).hasSize(2);
        assertThat(changed.freedSeats()).isEqualTo(preview);
    }

    @Test
    void tenHistoryRowsTakeAtMostFourQueries() {
        Rider sara = placedRider("Sara Mahmoud", LEVEL_3, ahmed, PLACED_AT);
        for (int i = 0; i < 10; i++) {
            boolean up = i % 2 == 0;
            change(sara, up ? LEVEL_3 : LEVEL_4, up ? LEVEL_4 : LEVEL_3, "Change " + i, i % 3 == 0 ? mona : ahmed,
                    PLACED_AT.plusSeconds(3600L * (i + 1)));
        }
        assign(session(LEVEL_3, TODAY, LocalTime.of(18, 0)), sara, ahmed);
        assign(session(LEVEL_3, SATURDAY, LocalTime.of(10, 0)), sara, ahmed);
        em.clear();

        Statistics statistics = em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        RiderLevelDto page = service.riderLevel(sara.getId(), ahmed.getId());

        assertThat(page.history()).hasSize(11);
        assertThat(page.history().getFirst().reason()).isEqualTo("Change 9");
        assertThat(page.history().getLast().type()).isEqualTo("placed");
        assertThat(page.upcomingSeats()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    private void assertNotFound(UUID riderId, UUID trainerId) {
        assertThatThrownBy(() -> service.riderLevel(riderId, trainerId))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.RIDER_NOT_FOUND);
                });
    }

    // A member assessed and placed by the assessor at recordedAt, and the rider made from them.
    private Rider placedRider(String name, short levelId, Trainer assessor, Instant recordedAt) {
        Member member = memberRepository.saveAndFlush(TestMembers.member(name));
        AssessmentBooking booking = bookingRepository.saveAndFlush(
                AssessmentBooking.book(member, session(null, LocalDate.of(2026, 10, 11), LocalTime.of(10, 0)),
                        assessor));
        booking.recordAttended(levelRepository.getReferenceById(levelId), null, recordedAt);
        bookingRepository.saveAndFlush(booking);
        return riderRepository.saveAndFlush(
                Rider.fromAssessment(member, levelRepository.getReferenceById(levelId), assessor));
    }

    // The change row only; the rider's level is set by the test's rider helper.
    private void change(Rider rider, short from, short to, String reason, Trainer by, Instant at) {
        levelChangeRepository.saveAndFlush(LevelChange.of(rider,
                levelRepository.getReferenceById(from), levelRepository.getReferenceById(to), reason, by, at));
    }

    private void setCreatedAt(Rider rider, Instant at) {
        jdbc.update("UPDATE rider SET created_at = ? WHERE id = ?", Timestamp.from(at), rider.getId());
    }

    private void assign(Session session, Rider rider, Trainer by) {
        assignmentRepository.saveAndFlush(Assignment.of(session, rider, by));
    }

    // A null level makes an assessment slot.
    private Session session(Short levelId, LocalDate date, LocalTime start) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(date.getDayOfWeek());
        template.setStartTime(start);
        template.setEndTime(start.plusHours(1));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        template = sessionTemplateRepository.saveAndFlush(template);
        return sessionRepository.saveAndFlush(Session.of(template, date));
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
