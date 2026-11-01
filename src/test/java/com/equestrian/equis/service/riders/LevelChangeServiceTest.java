package com.equestrian.equis.service.riders;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.riders.Direction;
import com.equestrian.equis.dto.riders.LevelChangeRequest;
import com.equestrian.equis.dto.riders.LevelChanged;
import com.equestrian.equis.dto.riders.UpcomingSeat;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.LevelChange;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.LevelChangeRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.payments.RiderBalanceService;
import com.equestrian.equis.service.sessions.AssignmentRules;
import com.equestrian.equis.service.sessions.AssignmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({LevelChangeService.class, LevelChangeRules.class, RiderSeatQuery.class,
        AssignmentService.class, AssignmentRules.class, RiderBalanceService.class})
public class LevelChangeServiceTest {
    private static final short LEVEL_2 = 2;
    private static final short LEVEL_3 = 3;
    private static final short LEVEL_4 = 4;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // The clock is fixed on Thursday 15 Oct 2026 at 10:00 school time.
    private static final Instant NOW = ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    LevelChangeService service;
    @Autowired
    RiderSeatQuery riderSeatQuery;
    @Autowired
    AssignmentService assignmentService;
    @MockitoSpyBean
    LevelChangeRepository levelChangeRepository;
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
    @Autowired
    PlatformTransactionManager transactionManager;

    // Rows made by tests that commit; removed after each test.
    private final List<UUID> trainerIds = new ArrayList<>();
    private final List<UUID> riderIds = new ArrayList<>();
    private final List<UUID> templateIds = new ArrayList<>();
    private final List<UUID> sessionIds = new ArrayList<>();

    Trainer ahmed;
    Trainer mona;
    Rider sara;
    Session yesterday1700;
    Session today0930;
    Session today1800;
    Session saturday1000;

    @BeforeEach
    void setUp() {
        trainerIds.clear();
        riderIds.clear();
        templateIds.clear();
        sessionIds.clear();
        inTransaction(() -> {
            ahmed = trainer("Ahmed Samy", "01999991001", "level-change-test-ahmed@example.com");
            mona = trainer("Mona Fathy", "01999991002", "level-change-test-mona@example.com");
            sara = rider("Sara Adel", LEVEL_3, ahmed);
            yesterday1700 = session(LEVEL_3, TODAY.minusDays(1), LocalTime.of(17, 0));
            today0930 = session(LEVEL_3, TODAY, LocalTime.of(9, 30));
            today1800 = session(LEVEL_3, TODAY, LocalTime.of(18, 0));
            saturday1000 = session(LEVEL_3, LocalDate.of(2026, 10, 17), LocalTime.of(10, 0));
        });
    }

    // Transactional tests roll back on their own; this removes what the committing tests left.
    @AfterEach
    void cleanUp() {
        inTransaction(() -> {
            riderIds.forEach(id -> jdbc.update("delete from level_change where rider_id = ?", id));
            riderIds.forEach(id -> jdbc.update("delete from assignment where rider_id = ?", id));
            sessionIds.forEach(id -> jdbc.update("delete from assignment where session_id = ?", id));
            sessionIds.forEach(id -> jdbc.update("delete from session where id = ?", id));
            templateIds.forEach(id -> jdbc.update("delete from session_template where id = ?", id));
            riderIds.forEach(id -> jdbc.update("delete from rider where id = ?", id));
            trainerIds.forEach(id -> jdbc.update("delete from trainer where id = ?", id));
        });
    }

    @Test
    void promoteFreesOnlyUpcomingOldLevelSeatsAndRecordsTheChange() {
        seatAll(sara, ahmed);

        LevelChanged result = service.change(sara.getId(), ahmed.getId(), promote());
        em.flush();
        em.clear();

        assertThat(result.riderId()).isEqualTo(sara.getId());
        assertThat(result.fromLevelId()).isEqualTo(3);
        assertThat(result.toLevelId()).isEqualTo(4);
        assertThat(result.reason()).isEqualTo("Ready for canter");
        assertThat(result.changedAt()).isEqualTo(NOW);
        assertThat(result.freedSeats()).extracting(UpcomingSeat::sessionId)
                .containsExactly(today1800.getId(), saturday1000.getId());
        assertThat(result.freedSeats().get(0))
                .isEqualTo(new UpcomingSeat(today1800.getId(), TODAY, LocalTime.of(18, 0), LocalTime.of(19, 0)));

        Rider reloaded = riderRepository.findById(sara.getId()).orElseThrow();
        assertThat(reloaded.getLevel().getId()).isEqualTo(LEVEL_4);
        assertThat(reloaded.getTrainer().getId()).isEqualTo(ahmed.getId());
        assertThat(seatedSessions(sara)).containsExactlyInAnyOrder(yesterday1700.getId(), today0930.getId());

        List<LevelChange> history = levelChangeRepository.findHistory(sara.getId());
        assertThat(history).singleElement().satisfies(c -> {
            assertThat(c.getFromLevel().getId()).isEqualTo(LEVEL_3);
            assertThat(c.getToLevel().getId()).isEqualTo(LEVEL_4);
            assertThat(c.getReason()).isEqualTo("Ready for canter");
            assertThat(c.getByTrainer().getId()).isEqualTo(ahmed.getId());
        });
    }

    @Test
    void demoteWithoutUpcomingSeatsFreesNothing() {
        LevelChanged result = service.change(sara.getId(), ahmed.getId(),
                new LevelChangeRequest(Direction.DOWN, 3, "Needs more walk work"));
        em.flush();
        em.clear();

        assertThat(result.toLevelId()).isEqualTo(2);
        assertThat(result.freedSeats()).isEmpty();
        assertThat(riderRepository.findById(sara.getId()).orElseThrow().getLevel().getId()).isEqualTo(LEVEL_2);
        assertThat(levelChangeRepository.findHistory(sara.getId())).hasSize(1);
    }

    @Test
    void otherRidersSeatsInTheSameSessionsAreUntouched() {
        Rider omar = rider("Omar Khaled", LEVEL_3, ahmed);
        Rider laila = rider("Laila Hassan", LEVEL_3, mona);
        seatAll(sara, ahmed);
        seatAll(omar, ahmed);
        seatAll(laila, mona);

        service.change(sara.getId(), ahmed.getId(), promote());
        em.flush();
        em.clear();

        List<UUID> all = List.of(yesterday1700.getId(), today0930.getId(), today1800.getId(), saturday1000.getId());
        assertThat(seatedSessions(omar)).containsExactlyInAnyOrderElementsOf(all);
        assertThat(seatedSessions(laila)).containsExactlyInAnyOrderElementsOf(all);
    }

    @Test
    void freedSeatsAreExactlyWhatTheSeatQueryShowedBefore() {
        seatAll(sara, ahmed);
        Session assessment = session(null, TODAY.plusDays(1), LocalTime.of(11, 0));
        assignmentRepository.saveAndFlush(Assignment.of(assessment, sara, ahmed));

        List<UUID> preview = riderSeatQuery.upcomingSeats(sara.getId(), LEVEL_3, NOW).stream()
                .map(a -> a.getSession().getId())
                .toList();
        LevelChanged result = service.change(sara.getId(), ahmed.getId(), promote());

        assertThat(preview).containsExactly(today1800.getId(), saturday1000.getId());
        assertThat(result.freedSeats()).extracting(UpcomingSeat::sessionId).containsExactlyElementsOf(preview);
    }

    @Test
    void ruleFailuresChangeNothing() {
        seatAll(sara, ahmed);
        Rider top = rider("Hana Top", (short) 6, ahmed);
        seatAll(top, ahmed);

        assertRefused(() -> service.change(sara.getId(), mona.getId(), promote()),
                HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND);
        assertRefused(() -> service.change(UUID.randomUUID(), ahmed.getId(), promote()),
                HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND);
        assertRefused(() -> service.change(sara.getId(), ahmed.getId(),
                        new LevelChangeRequest(Direction.UP, 2, "Ready for canter")),
                HttpStatus.CONFLICT, ErrorCodes.LEVEL_CHANGED);
        assertRefused(() -> service.change(top.getId(), ahmed.getId(),
                        new LevelChangeRequest(Direction.UP, 6, "Ready for more")),
                HttpStatus.CONFLICT, ErrorCodes.LEVEL_AT_TOP);
        em.flush();
        em.clear();

        assertThat(riderRepository.findById(sara.getId()).orElseThrow().getLevel().getId()).isEqualTo(LEVEL_3);
        assertThat(seatedSessions(sara)).hasSize(4);
        assertThat(seatedSessions(top)).hasSize(4);
        assertThat(levelChangeRepository.findHistory(sara.getId())).isEmpty();
        assertThat(levelChangeRepository.findHistory(top.getId())).isEmpty();
    }

    @Test
    void unassignedRiderIsNotFound() {
        Rider unassigned = rider("Nour Free", LEVEL_3, null);

        assertRefused(() -> service.change(unassigned.getId(), ahmed.getId(), promote()),
                HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND);
    }

    // Commits for real, so the rollback of the deletes can be seen.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failureWhileSavingTheHistoryRestoresTheSeatsAndTheLevel() {
        inTransaction(() -> seatAll(sara, ahmed));
        doThrow(new IllegalStateException("history write failed")).when(levelChangeRepository).save(any());

        assertThatThrownBy(() -> service.change(sara.getId(), ahmed.getId(), promote()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(levelOf(sara)).isEqualTo(LEVEL_3);
        assertThat(seatedSessions(sara)).hasSize(4);
        assertThat(historyCount(sara)).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void doubleTapMovesTheRiderOneLevelOnly() throws Exception {
        inTransaction(() -> seatAll(sara, ahmed));

        List<Object> outcomes = race(
                () -> service.change(sara.getId(), ahmed.getId(), promote()),
                () -> service.change(sara.getId(), ahmed.getId(), promote()));

        assertThat(outcomes).filteredOn(LevelChanged.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(ApiException.class::isInstance).singleElement()
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo(ErrorCodes.LEVEL_CHANGED));
        assertThat(levelOf(sara)).isEqualTo(LEVEL_4);
        assertThat(historyCount(sara)).isEqualTo(1);
    }

    // The level change holds the rider lock until commit; the seat save must wait and then see Level 4.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void seatSaveDuringALevelChangeNeverLeavesAnOldLevelSeat() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch changed = new CountDownLatch(1);
        CountDownLatch saveStarted = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<LevelChanged> change = pool.submit(() -> tx.execute(status -> {
                LevelChanged result = service.change(sara.getId(), ahmed.getId(), promote());
                changed.countDown();
                await(saveStarted);
                // Gives the seat save time to reach the rider lock before this commits.
                sleep(500);
                return result;
            }));
            Future<Object> save = pool.submit(() -> {
                await(changed);
                saveStarted.countDown();
                return outcome(() -> assignmentService.replaceMine(ahmed.getId(), today1800.getId(),
                        List.of(sara.getId())));
            });

            assertThat(change.get(30, TimeUnit.SECONDS).toLevelId()).isEqualTo(4);
            assertThat(save.get(30, TimeUnit.SECONDS)).isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.code()).isEqualTo(ErrorCodes.LEVEL_OUT_OF_RANGE));
        } finally {
            pool.shutdownNow();
        }

        assertThat(levelOf(sara)).isEqualTo(LEVEL_4);
        assertThat(riderSeatQuery.upcomingSeats(sara.getId(), LEVEL_3, NOW)).isEmpty();
    }

    // Either order of a free race keeps the rule: never Level 4 with an upcoming Level 3 seat.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void levelChangeAndSeatSaveRacingKeepTheRiderOutOfTheOldLevel() throws Exception {
        List<Object> outcomes = race(
                () -> service.change(sara.getId(), ahmed.getId(), promote()),
                () -> assignmentService.replaceMine(ahmed.getId(), today1800.getId(), List.of(sara.getId())));

        assertThat(outcomes.get(0)).isInstanceOf(LevelChanged.class);
        assertThat(outcomes.get(1)).satisfiesAnyOf(
                o -> assertThat(o).isInstanceOf(Session.class),
                o -> assertThat(o).isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCodes.LEVEL_OUT_OF_RANGE)));
        assertThat(levelOf(sara)).isEqualTo(LEVEL_4);
        assertThat(riderSeatQuery.upcomingSeats(sara.getId(), LEVEL_3, NOW)).isEmpty();
    }

    private static LevelChangeRequest promote() {
        return new LevelChangeRequest(Direction.UP, 3, "Ready for canter");
    }

    // Starts both calls at once; each result is the return value or the thrown exception.
    private List<Object> race(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> a = pool.submit(() -> {
                await(start);
                return outcome(first);
            });
            Future<Object> b = pool.submit(() -> {
                await(start);
                return outcome(second);
            });
            start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private static Object outcome(Callable<?> call) {
        try {
            return call.call();
        } catch (Exception e) {
            return e;
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void assertRefused(Runnable call, HttpStatus status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(status);
            assertThat(ex.code()).isEqualTo(code);
        });
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private Short levelOf(Rider rider) {
        return jdbc.queryForObject("select level_id from rider where id = ?", Short.class, rider.getId());
    }

    private int historyCount(Rider rider) {
        return jdbc.queryForObject("select count(*) from level_change where rider_id = ?", Integer.class,
                rider.getId());
    }

    private List<UUID> seatedSessions(Rider rider) {
        return jdbc.queryForList("select session_id from assignment where rider_id = ?", UUID.class, rider.getId());
    }

    private void seatAll(Rider rider, Trainer by) {
        for (Session s : List.of(yesterday1700, today0930, today1800, saturday1000)) {
            assignmentRepository.saveAndFlush(Assignment.of(s, rider, by));
        }
    }

    private Session session(Short levelId, LocalDate date, LocalTime start) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(date.getDayOfWeek());
        template.setStartTime(start);
        template.setEndTime(start.plusHours(1));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        template = sessionTemplateRepository.saveAndFlush(template);
        templateIds.add(template.getId());
        Session session = sessionRepository.saveAndFlush(Session.of(template, date));
        sessionIds.add(session.getId());
        return session;
    }

    private Rider rider(String name, short levelId, Trainer trainer) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(levelRepository.getReferenceById(levelId));
        rider.setTrainer(trainer);
        rider = riderRepository.saveAndFlush(rider);
        riderIds.add(rider.getId());
        return rider;
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        trainer = trainerRepository.saveAndFlush(trainer);
        trainerIds.add(trainer.getId());
        return trainer;
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
