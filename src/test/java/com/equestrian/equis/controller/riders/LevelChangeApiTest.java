package com.equestrian.equis.controller.riders;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
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
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /me/riders/{id}/level-change over HTTP against a real database: one case per answer code, and what the
// other screens' endpoints show afterwards. The rules, the transaction and the races are in LevelChangeServiceTest.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=level-change-api-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
public class LevelChangeApiTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_3 = 3;
    private static final short LEVEL_6 = 6;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // The clock is fixed on Thursday 15 Oct 2026 at 10:00 school time.
    private static final Instant NOW = ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final String PROMOTE = """
            { "direction": "up", "fromLevelId": 3, "reason": "Ready for canter" }""";

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @TestBean
    Clock clock;

    static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
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

    // Every request commits, so the rows each test makes are removed after it.
    private final List<UUID> trainerIds = new ArrayList<>();
    private final List<UUID> riderIds = new ArrayList<>();
    private final List<UUID> templateIds = new ArrayList<>();
    private final List<UUID> sessionIds = new ArrayList<>();

    Trainer ahmed;
    Trainer mona;
    Rider sara;
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
            ahmed = trainer("Ahmed Samy", "01999992001", "level-change-api-ahmed@example.com");
            mona = trainer("Mona Fathy", "01999992002", "level-change-api-mona@example.com");
            sara = rider("Sara Adel", LEVEL_3, ahmed);
            today0930 = session(LEVEL_3, TODAY, LocalTime.of(9, 30));
            today1800 = session(LEVEL_3, TODAY, LocalTime.of(18, 0));
            saturday1000 = session(LEVEL_3, LocalDate.of(2026, 10, 17), LocalTime.of(10, 0));
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

    // Case 1
    @Test
    void promoteAnswersTheNewLevelAndTheFreedSeatsInDateOrder() throws Exception {
        seatAll(sara);

        changeLevel(sara.getId(), ahmed, PROMOTE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderId").value(sara.getId().toString()))
                .andExpect(jsonPath("$.fromLevelId").value(3))
                .andExpect(jsonPath("$.toLevelId").value(4))
                .andExpect(jsonPath("$.reason").value("Ready for canter"))
                .andExpect(jsonPath("$.changedAt").value("2026-10-15T07:00:00Z"))
                .andExpect(jsonPath("$.freedSeats.length()").value(2))
                .andExpect(jsonPath("$.freedSeats[0].sessionId").value(today1800.getId().toString()))
                .andExpect(jsonPath("$.freedSeats[0].date").value("2026-10-15"))
                .andExpect(jsonPath("$.freedSeats[0].start").value("18:00"))
                .andExpect(jsonPath("$.freedSeats[0].end").value("19:00"))
                .andExpect(jsonPath("$.freedSeats[1].sessionId").value(saturday1000.getId().toString()))
                .andExpect(jsonPath("$.freedSeats[1].date").value("2026-10-17"))
                .andExpect(jsonPath("$.freedSeats[1].start").value("10:00"))
                .andExpect(jsonPath("$.freedSeats[1].end").value("11:00"));

        assertThat(levelOf(sara)).isEqualTo((short) 4);
        // The 09:30 seat is already running, so it stays.
        assertThat(seatedSessions(sara)).containsExactly(today0930.getId());
    }

    // Case 2
    @Test
    void demoteWithoutUpcomingSeatsAnswersAnEmptyFreedSeatsArray() throws Exception {
        changeLevel(sara.getId(), ahmed, """
                { "direction": "down", "fromLevelId": 3, "reason": "Needs more walk work" }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toLevelId").value(2))
                .andExpect(jsonPath("$.freedSeats").isArray())
                .andExpect(jsonPath("$.freedSeats").isEmpty());
    }

    // Case 3
    @Test
    void badFieldsAreAValidationFailure() throws Exception {
        changeLevel(sara.getId(), ahmed, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("direction", "fromLevelId", "reason")));
        changeLevel(sara.getId(), ahmed, """
                { "direction": "up", "fromLevelId": 3, "reason": "   " }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("reason")));
        changeLevel(sara.getId(), ahmed, """
                { "direction": "up", "fromLevelId": 3, "reason": "%s" }""".formatted("a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("reason")));
        changeLevel(sara.getId(), ahmed, """
                { "direction": "up", "fromLevelId": 7, "reason": "Ready for canter" }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("fromLevelId")));

        assertThat(levelOf(sara)).isEqualTo(LEVEL_3);
    }

    // Case 4
    @Test
    void unknownDirectionOrAnIdThatIsNotAUuidIsABadRequest() throws Exception {
        changeLevel(sara.getId(), ahmed, """
                { "direction": "sideways", "fromLevelId": 3, "reason": "Ready for canter" }""")
                .andExpect(status().isBadRequest());
        mvc.perform(asTrainer(post("/me/riders/abc/level-change"), ahmed)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROMOTE))
                .andExpect(status().isBadRequest());

        assertThat(levelOf(sara)).isEqualTo(LEVEL_3);
    }

    // Case 5
    @Test
    void anotherTrainersOrAnUnknownRiderIsNotFound() throws Exception {
        seatAll(sara);

        changeLevel(sara.getId(), mona, PROMOTE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
        changeLevel(UUID.randomUUID(), ahmed, PROMOTE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));

        assertThat(levelOf(sara)).isEqualTo(LEVEL_3);
        assertThat(seatedSessions(sara)).hasSize(3);
        assertThat(historyCount(sara)).isZero();
    }

    // Case 6
    @Test
    void theSameRequestTwiceMovesTheRiderOnce() throws Exception {
        changeLevel(sara.getId(), ahmed, PROMOTE).andExpect(status().isOk());
        changeLevel(sara.getId(), ahmed, PROMOTE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("level_changed"));

        assertThat(levelOf(sara)).isEqualTo((short) 4);
        assertThat(historyCount(sara)).isEqualTo(1);
    }

    // Case 7
    @Test
    void cannotLeaveTheLadder() throws Exception {
        Rider top = committed(() -> rider("Hana Top", LEVEL_6, ahmed));
        Rider bottom = committed(() -> rider("Ali First", LEVEL_1, ahmed));

        changeLevel(top.getId(), ahmed, """
                { "direction": "up", "fromLevelId": 6, "reason": "Ready for more" }""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("level_at_top"));
        changeLevel(bottom.getId(), ahmed, """
                { "direction": "down", "fromLevelId": 1, "reason": "Needs more walk work" }""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("level_at_bottom"));

        assertThat(levelOf(top)).isEqualTo(LEVEL_6);
        assertThat(levelOf(bottom)).isEqualTo(LEVEL_1);
    }

    // Case 8
    @Test
    void onlyTrainersGetIn() throws Exception {
        String path = "/me/riders/" + sara.getId() + "/level-change";

        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(PROMOTE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROMOTE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        assertThat(levelOf(sara)).isEqualTo(LEVEL_3);
    }

    // Case 9
    @Test
    void myRidersRiderLevelAndHomeShowTheNewLevel() throws Exception {
        long level3Before = myRidersOnLevel(3);
        long level4Before = myRidersOnLevel(4);

        changeLevel(sara.getId(), ahmed, PROMOTE).andExpect(status().isOk());

        mvc.perform(asTrainer(get("/me/riders"), ahmed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + sara.getId() + "')].levelId").value(4));
        mvc.perform(asTrainer(get("/me/riders/" + sara.getId() + "/level-history"), ahmed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levelId").value(4))
                .andExpect(jsonPath("$.history[0].type").value("promoted"))
                .andExpect(jsonPath("$.history[0].fromLevelId").value(3))
                .andExpect(jsonPath("$.history[0].toLevelId").value(4))
                .andExpect(jsonPath("$.history[0].reason").value("Ready for canter"));
        assertThat(myRidersOnLevel(3)).isEqualTo(level3Before - 1);
        assertThat(myRidersOnLevel(4)).isEqualTo(level4Before + 1);
    }

    // Case 10. The Notion task expects rider_wrong_level; the assignment rules answer level_out_of_range.
    @Test
    void theRiderCannotBeSeatedBackInTheOldLevel() throws Exception {
        changeLevel(sara.getId(), ahmed, PROMOTE).andExpect(status().isOk());

        mvc.perform(asTrainer(put("/me/sessions/" + today1800.getId() + "/assignments"), ahmed)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"riderIds\": [\"" + sara.getId() + "\"] }"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("level_out_of_range"));

        assertThat(seatedSessions(sara)).isEmpty();
    }

    private ResultActions changeLevel(UUID riderId, Trainer by, String body) throws Exception {
        return mvc.perform(asTrainer(post("/me/riders/" + riderId + "/level-change"), by)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long myRidersOnLevel(int levelId) throws Exception {
        String body = mvc.perform(asTrainer(get("/me/levels"), ahmed))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> found = JsonPath.read(body, "$[?(@.id == " + levelId + ")].myRiders");
        return found.getFirst().longValue();
    }

    private MockHttpServletRequestBuilder asTrainer(MockHttpServletRequestBuilder request, Trainer trainer) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(trainer.getId(), Role.TRAINER)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
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

    private void seatAll(Rider rider) {
        inTransaction(() -> {
            for (Session s : List.of(today0930, today1800, saturday1000)) {
                assignmentRepository.save(Assignment.of(s, rider, ahmed));
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
