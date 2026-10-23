package com.equestrian.equis.service.riders;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.riders.Direction;
import com.equestrian.equis.dto.riders.LevelChangeRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
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
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.sessions.AssignmentService;
import com.equestrian.equis.support.MutableClock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The whole promote/demote story (flow M5) end to end against a real database: the ladder, the edges of
// "upcoming", ownership before and after a reassignment, the other screens' numbers, and the two races repeated.
// Single rules, single answer codes and single-run races are in LevelChangeServiceTest, LevelChangeApiTest,
// RiderLevelQueryServiceTest and MyRidersQueryServiceTest; they are not repeated here. That covers the task's
// G2 (stale fromLevelId), G4 (other riders' seats untouched) and G7 (placement line last, none for seed riders).
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=level-change-flow-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "app.sessions.generate-cron=-"})
@AutoConfigureMockMvc
@Testcontainers
public class LevelChangeFlowTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_3 = 3;
    private static final short LEVEL_4 = 4;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Every test starts on Thursday 15 Oct 2026 at 10:00 school time.
    private static final Instant NOW = ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    // Saturday starts the next school week.
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 17);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(NOW);

    @TestBean
    Clock clock;

    static Clock clock() {
        return CLOCK;
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    LevelChangeService levelChangeService;
    @Autowired
    AssignmentService assignmentService;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssignmentRepository assignmentRepository;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;

    // No @Transactional: every request and service call commits, so the rows each test makes are removed after it.
    private final List<UUID> trainerIds = new ArrayList<>();
    private final List<UUID> riderIds = new ArrayList<>();
    private final List<UUID> templateIds = new ArrayList<>();
    private final List<UUID> sessionIds = new ArrayList<>();

    Trainer ahmed;
    Trainer mona;
    Rider sara;

    @BeforeEach
    void setUp() {
        CLOCK.set(NOW);
        trainerIds.clear();
        riderIds.clear();
        templateIds.clear();
        sessionIds.clear();
        inTransaction(() -> {
            ahmed = trainer("Ahmed Samy", "01999993001", "level-change-flow-ahmed@example.com");
            mona = trainer("Mona Fathy", "01999993002", "level-change-flow-mona@example.com");
            sara = rider("Sara Adel", LEVEL_3, ahmed);
        });
    }

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

    // G1
    @Test
    void aRiderWalksTheWholeLadderAndStopsAtBothEnds() throws Exception {
        Rider hana = committed(() -> rider("Hana Mostafa", LEVEL_1, ahmed));

        changeLevel(hana.getId(), ahmed, "down", 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("level_at_bottom"));
        for (int from = 1; from <= 5; from++) {
            // A minute apart, so the history has a clear newest-first order; at the same instant it has none.
            CLOCK.advance(Duration.ofMinutes(1));
            changeLevel(hana.getId(), ahmed, "up", from)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromLevelId").value(from))
                    .andExpect(jsonPath("$.toLevelId").value(from + 1));
        }
        changeLevel(hana.getId(), ahmed, "up", 6)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("level_at_top"));

        ResultActions page = levelHistory(hana.getId(), ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levelId").value(6))
                .andExpect(jsonPath("$.canPromote").value(false))
                .andExpect(jsonPath("$.canDemote").value(true))
                .andExpect(jsonPath("$.history.length()").value(5));
        for (int i = 0; i < 5; i++) {
            page.andExpect(jsonPath("$.history[" + i + "].type").value("promoted"))
                    .andExpect(jsonPath("$.history[" + i + "].fromLevelId").value(5 - i))
                    .andExpect(jsonPath("$.history[" + i + "].toLevelId").value(6 - i));
        }
        assertThat(levelOf(hana)).isEqualTo((short) 6);
        assertThat(myRidersOnLevel(ahmed, 6)).isEqualTo(1);
    }

    // G3. A session that starts exactly now counts as started.
    @Test
    void onlySeatsStartingAfterNowAreFreedAndThePreviewMatches() throws Exception {
        Session today0959 = committed(() -> session(LEVEL_3, TODAY, LocalTime.of(9, 59)));
        Session today1000 = committed(() -> session(LEVEL_3, TODAY, LocalTime.of(10, 0)));
        Session today1001 = committed(() -> session(LEVEL_3, TODAY, LocalTime.of(10, 1)));
        Session tomorrow0800 = committed(() -> session(LEVEL_3, TODAY.plusDays(1), LocalTime.of(8, 0)));
        Session yesterday1800 = committed(() -> session(LEVEL_3, TODAY.minusDays(1), LocalTime.of(18, 0)));
        seat(sara, ahmed, today0959, today1000, today1001, tomorrow0800, yesterday1800);

        levelHistory(sara.getId(), ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingSeats.length()").value(2))
                .andExpect(jsonPath("$.upcomingSeats[0].sessionId").value(today1001.getId().toString()))
                .andExpect(jsonPath("$.upcomingSeats[1].sessionId").value(tomorrow0800.getId().toString()));
        changeLevel(sara.getId(), ahmed, "up", 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freedSeats.length()").value(2))
                .andExpect(jsonPath("$.freedSeats[0].sessionId").value(today1001.getId().toString()))
                .andExpect(jsonPath("$.freedSeats[1].sessionId").value(tomorrow0800.getId().toString()));

        assertThat(seatedSessions(sara))
                .containsExactlyInAnyOrder(today0959.getId(), today1000.getId(), yesterday1800.getId());
    }

    // G5
    @Test
    void anotherTrainerCannotSeeOrChangeTheRiderOnAnyEndpoint() throws Exception {
        String monasRiders = body(mvc.perform(asTrainer(get("/me/riders"), mona)).andExpect(status().isOk()));
        levelHistory(sara.getId(), mona)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
        changeLevel(sara.getId(), mona, "up", 3)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));

        assertThat(monasRiders).doesNotContain(sara.getId().toString());
        assertThat(levelOf(sara)).isEqualTo(LEVEL_3);
        assertThat(historyCount(sara)).isZero();
    }

    // G6. The W3 reassignment endpoint arrives in E7, so the move is made with SQL.
    @Test
    void afterAReassignmentOnlyTheNewTrainerCanChangeTheRider() throws Exception {
        changeLevel(sara.getId(), ahmed, "up", 3).andExpect(status().isOk());
        jdbc.update("UPDATE rider SET trainer_id = ? WHERE id = ?", mona.getId(), sara.getId());

        levelHistory(sara.getId(), ahmed)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
        changeLevel(sara.getId(), ahmed, "up", 4)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
        changeLevel(sara.getId(), mona, "up", 4).andExpect(status().isOk());

        levelHistory(sara.getId(), mona)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levelId").value(5))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[0].toLevelId").value(5))
                .andExpect(jsonPath("$.history[0].byName").value("Mona Fathy"))
                .andExpect(jsonPath("$.history[1].toLevelId").value(4))
                .andExpect(jsonPath("$.history[1].byName").value("Ahmed Samy"));
    }

    // G8. myRiders on both levels is checked by LevelChangeApiTest; this adds the seats and Assign riders (3).
    @Test
    void homeSeatCountsAndTheNewLevelsSessionsFollowThePromote() throws Exception {
        Rider omar = committed(() -> rider("Omar Khaled", LEVEL_3, ahmed));
        Session today0930 = committed(() -> session(LEVEL_3, TODAY, LocalTime.of(9, 30)));
        Session today1800 = committed(() -> session(LEVEL_3, TODAY, LocalTime.of(18, 0)));
        Session saturday1000 = committed(() -> session(LEVEL_3, SATURDAY, LocalTime.of(10, 0)));
        Session level4Today = committed(() -> session(LEVEL_4, TODAY, LocalTime.of(18, 0)));
        seat(sara, ahmed, today0930, today1800, saturday1000);
        seat(omar, ahmed, today1800);
        long level3SeatsBefore = seatsFilledOnLevel(ahmed, LEVEL_3);
        long level4SeatsBefore = seatsFilledOnLevel(ahmed, LEVEL_4);

        changeLevel(sara.getId(), ahmed, "up", 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freedSeats.length()").value(2));

        // Of the two freed seats only today 18:00 is in this week; Saturday is next week's.
        assertThat(seatsFilledOnLevel(ahmed, LEVEL_3)).isEqualTo(level3SeatsBefore - 1);
        assertThat(seatsFilledOnLevel(ahmed, LEVEL_4)).isEqualTo(level4SeatsBefore);
        assertThat(seatedSessions(omar)).containsExactly(today1800.getId());
        mvc.perform(asTrainer(get("/me/sessions/" + level4Today.getId()), ahmed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[?(@.id == '" + sara.getId() + "')].inThisSession").value(false))
                .andExpect(jsonPath("$.riders[?(@.id == '" + omar.getId() + "')]").isEmpty());
    }

    // G9. The rider row is locked FOR UPDATE, so the second change waits and then sees Level 4.
    @RepeatedTest(20)
    void doubleTapAlwaysMovesTheRiderOneLevel() throws Exception {
        List<String> outcomes = race(
                () -> levelChangeService.change(sara.getId(), ahmed.getId(), promote()),
                () -> levelChangeService.change(sara.getId(), ahmed.getId(), promote()));

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "level_changed");
        assertThat(levelOf(sara)).isEqualTo(LEVEL_4);
        assertThat(historyCount(sara)).isEqualTo(1);
    }

    // G10. The seat save reads its riders FOR SHARE, so it waits for the level change, or the change frees its seat.
    @RepeatedTest(20)
    void levelChangeAndSeatSaveNeverLeaveAnOldLevelSeat() throws Exception {
        Session today1800 = committed(() -> session(LEVEL_3, TODAY, LocalTime.of(18, 0)));

        List<String> outcomes = race(
                () -> levelChangeService.change(sara.getId(), ahmed.getId(), promote()),
                () -> assignmentService.replaceMine(ahmed.getId(), today1800.getId(), List.of(sara.getId())));

        assertThat(outcomes.get(0)).isEqualTo("ok");
        assertThat(outcomes.get(1)).isIn("ok", "level_out_of_range");
        assertThat(levelOf(sara)).isEqualTo(LEVEL_4);
        assertThat(seatedSessions(sara)).isEmpty();
    }

    // G11
    @Test
    void anotherTrainersRidersInTheSameSessionsNeverShowUp() throws Exception {
        Rider laila = committed(() -> rider("Laila Hassan", LEVEL_3, mona));
        Rider nour = committed(() -> rider("Nour Ibrahim", LEVEL_3, mona));
        Session today1800 = committed(() -> session(LEVEL_3, TODAY, LocalTime.of(18, 0)));
        Session saturday1000 = committed(() -> session(LEVEL_3, SATURDAY, LocalTime.of(10, 0)));
        seat(sara, ahmed, today1800, saturday1000);
        seat(laila, mona, today1800, saturday1000);
        seat(nour, mona, today1800);

        String riders = body(mvc.perform(asTrainer(get("/me/riders"), ahmed)).andExpect(status().isOk()));
        String page = body(levelHistory(sara.getId(), ahmed).andExpect(status().isOk()));

        for (String response : List.of(riders, page)) {
            assertThat(response).contains(sara.getId().toString());
            assertThat(response).doesNotContain(laila.getId().toString(), "Laila", "Hassan",
                    nour.getId().toString(), "Nour", "Ibrahim");
        }
    }

    private static LevelChangeRequest promote() {
        return new LevelChangeRequest(Direction.UP, 3, "Ready for canter");
    }

    // Starts both calls at once; each result is "ok" or the code of the rule that refused it.
    private List<String> race(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return outcome(first);
            });
            Future<String> b = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return outcome(second);
            });
            start.countDown();
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private static String outcome(Callable<?> call) throws Exception {
        try {
            call.call();
            return "ok";
        } catch (ApiException e) {
            return e.code();
        }
    }

    private ResultActions changeLevel(UUID riderId, Trainer by, String direction, int fromLevelId) throws Exception {
        return mvc.perform(asTrainer(post("/me/riders/" + riderId + "/level-change"), by)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "direction": "%s", "fromLevelId": %d, "reason": "Assessed in the ring" }"""
                        .formatted(direction, fromLevelId)));
    }

    private ResultActions levelHistory(UUID riderId, Trainer by) throws Exception {
        return mvc.perform(asTrainer(get("/me/riders/" + riderId + "/level-history"), by));
    }

    private long myRidersOnLevel(Trainer trainer, int levelId) throws Exception {
        return levelSummaryField(trainer, levelId, "myRiders");
    }

    private long seatsFilledOnLevel(Trainer trainer, int levelId) throws Exception {
        return levelSummaryField(trainer, levelId, "seatsFilled");
    }

    private long levelSummaryField(Trainer trainer, int levelId, String field) throws Exception {
        String body = body(mvc.perform(asTrainer(get("/me/levels"), trainer)).andExpect(status().isOk()));
        List<Number> found = JsonPath.read(body, "$[?(@.id == " + levelId + ")]." + field);
        return found.getFirst().longValue();
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private MockHttpServletRequestBuilder asTrainer(MockHttpServletRequestBuilder request, Trainer trainer) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(trainer.getId(), Role.TRAINER)).value());
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private <T> T committed(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
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

    private void seat(Rider rider, Trainer by, Session... sessions) {
        inTransaction(() -> {
            for (Session s : sessions) {
                assignmentRepository.save(Assignment.of(s, rider, by));
            }
        });
    }

    private Session session(short levelId, LocalDate date, LocalTime start) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelRepository.getReferenceById(levelId));
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
        trainer.setStatus(TrainerStatus.ACTIVE);
        trainer.setPasswordHash("not-a-real-hash");
        trainer = trainerRepository.saveAndFlush(trainer);
        trainerIds.add(trainer.getId());
        return trainer;
    }
}
