package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.support.MutableClock;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /me/week (screen 5) over HTTP against a real database. Trainers, riders and seats go in with SQL; sessions
// through JPA, as the app saves them. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=week-api-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Testcontainers
public class MyWeekApiTest {
    private static final String PATH = "/me/week";
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // The school week of Sunday 18 October 2026 runs Saturday 17 to Friday 23.
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 17);
    private static final LocalDate SUNDAY = SATURDAY.plusDays(1);
    private static final LocalDate MONDAY = SATURDAY.plusDays(2);
    private static final LocalDate WEDNESDAY = SATURDAY.plusDays(4);
    private static final LocalDate FRIDAY = SATURDAY.plusDays(6);
    private static final Instant SUNDAY_NOON = cairo(SUNDAY, 12, 0);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(SUNDAY_NOON);

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
    JdbcTemplate jdbc;
    @Autowired
    EntityManagerFactory entityManagerFactory;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    LevelRepository levelRepository;

    private UUID ahmed;
    private UUID mona;
    private int seq;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, sample riders and generated sessions.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(SUNDAY_NOON);
        seq = 0;
        ahmed = trainer("Ahmed Said");
        mona = trainer("Mona Adel");
    }

    // Case 1
    @Test
    void withoutADateItIsThisSaturdayToFridayWeek() throws Exception {
        week()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-17"))
                .andExpect(jsonPath("$.to").value("2026-10-23"))
                .andExpect(jsonPath("$.days[*].date", contains("2026-10-17", "2026-10-18", "2026-10-19",
                        "2026-10-20", "2026-10-21", "2026-10-22", "2026-10-23")));
    }

    // Case 2
    @Test
    void anyDayOfTheWeekGivesThatWeek() throws Exception {
        for (String date : new String[]{"2026-10-17", "2026-10-23"}) {
            week(date)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.from").value("2026-10-17"))
                    .andExpect(jsonPath("$.to").value("2026-10-23"))
                    .andExpect(jsonPath("$.days", hasSize(7)));
        }
    }

    // Case 3
    @Test
    void aDayListsItsSessionsByStartThenLevelAndAnEmptyDayIsStillListed() throws Exception {
        UUID level5 = session(5, MONDAY, LocalTime.of(17, 0), 6);
        UUID level2Late = session(2, MONDAY, LocalTime.of(10, 0), 6);
        UUID level2 = session(2, MONDAY, LocalTime.of(8, 30), 6);
        UUID level1 = session(1, MONDAY, LocalTime.of(10, 0), 6);

        week()
                .andExpect(jsonPath("$.days[2].date").value("2026-10-19"))
                .andExpect(jsonPath("$.days[2].sessions[*].id", contains(level2.toString(), level1.toString(),
                        level2Late.toString(), level5.toString())))
                .andExpect(jsonPath("$.days[2].sessions[*].levelId", contains(2, 1, 2, 5)))
                .andExpect(jsonPath("$.days[2].sessions[0].start").value("08:30"))
                .andExpect(jsonPath("$.days[2].sessions[0].end").value("09:30"))
                // Thursday
                .andExpect(jsonPath("$.days[5].date").value("2026-10-22"))
                .andExpect(jsonPath("$.days[5].sessions", empty()));
    }

    // Case 4
    @Test
    void assessmentSlotsAndCancelledSessionsAreLeftOut() throws Exception {
        UUID kept = session(1, WEDNESDAY, LocalTime.of(8, 0), 6);
        session(null, WEDNESDAY, LocalTime.of(10, 0), 6);
        cancelledSession(1, WEDNESDAY, LocalTime.of(12, 0));

        week().andExpect(jsonPath("$.days[4].sessions[*].id", contains(kept.toString())));
    }

    // Case 5
    @Test
    void theCallersSeatsNameTheRiderAndOtherCoachesSeatsDoNot() throws Exception {
        UUID session = session(2, MONDAY, LocalTime.of(8, 30), 6);
        UUID sara = rider("Sara Ali", 2, ahmed);
        UUID omar = rider("Omar Mostafa", 2, ahmed);
        assign(session, rider("Hana Karim", 2, mona), mona);
        assign(session, sara, ahmed);
        assign(session, rider("Nour Seed", 2, mona), mona);
        assign(session, omar, ahmed);

        String path = "$.days[2].sessions[0].";
        String body = week()
                .andExpect(jsonPath(path + "capacity").value(6))
                .andExpect(jsonPath(path + "booked").value(4))
                .andExpect(jsonPath(path + "mine").value(2))
                .andExpect(jsonPath(path + "seats[*].mine", contains(true, true, false, false)))
                .andExpect(jsonPath(path + "seats[0].riderId").value(omar.toString()))
                .andExpect(jsonPath(path + "seats[0].initials").value("OM"))
                .andExpect(jsonPath(path + "seats[1].riderId").value(sara.toString()))
                .andExpect(jsonPath(path + "seats[1].initials").value("SA"))
                .andReturn().getResponse().getContentAsString();
        // Exactly { "mine": false }: nothing about another coach's rider.
        List<Map<String, Object>> others = JsonPath.read(body, path + "seats[2:]");
        assertThat(others).containsExactly(Map.of("mine", false), Map.of("mine", false));
    }

    // Case 6
    @Test
    void aSessionIsPastOnceItHasStarted() throws Exception {
        CLOCK.set(cairo(MONDAY, 9, 0));
        session(1, MONDAY, LocalTime.of(8, 30), 6);
        session(1, MONDAY, LocalTime.of(17, 0), 6);

        week().andExpect(jsonPath("$.days[2].sessions[*].past", contains(true, false)));
    }

    // Case 7
    @Test
    void aPastWeekIsAllPastAndAWeekBeyondTheHorizonIsEmpty() throws Exception {
        session(1, SATURDAY.minusDays(7), LocalTime.of(8, 0), 6);
        session(3, FRIDAY.minusDays(7), LocalTime.of(18, 0), 6);

        week("2026-10-12")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-10"))
                .andExpect(jsonPath("$.days[*].sessions[*]", hasSize(2)))
                .andExpect(jsonPath("$.days[*].sessions[*].past", everyItem(is(true))));
        week(SUNDAY.plusWeeks(12).toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[*].sessions[*]", empty()));
    }

    // Case 8
    @Test
    void aMalformedDateIsAValidationFailure() throws Exception {
        for (String date : new String[]{"2026-13-40", "abc"}) {
            week(date)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("date"));
        }
    }

    // Case 9
    @Test
    void theLevelLegendListsAllSixLevelsInOrder() throws Exception {
        week()
                .andExpect(jsonPath("$.levels[*].id", contains(1, 2, 3, 4, 5, 6)))
                .andExpect(jsonPath("$.levels[0].name").value("Level 1"))
                .andExpect(jsonPath("$.levels[0].colour").value("#17695B"))
                .andExpect(jsonPath("$.levels[*].colour", everyItem(matchesPattern("#[0-9A-F]{6}"))));
    }

    // Case 10
    @Test
    void theWeekRunsTheSameNumberOfQueriesWhateverTheNumberOfSessions() throws Exception {
        UUID rider = rider("Sara Ali", 1, ahmed);
        UUID other = rider("Hana Karim", 1, mona);
        assign(session(1, MONDAY, LocalTime.of(8, 0), 6), rider, ahmed);
        long withOne = statementsFor(this::week);

        for (int i = 1; i < 40; i++) {
            UUID session = session(1 + i % 6, SATURDAY.plusDays(i % 7), LocalTime.of(7, 0).plusMinutes(15L * i), 6);
            assign(session, rider, ahmed);
            assign(session, other, mona);
        }
        week().andExpect(jsonPath("$.days[*].sessions[*]", hasSize(40)));
        long withForty = statementsFor(this::week);

        assertThat(withForty).isEqualTo(withOne);
    }

    // Case 11
    @Test
    void aSessionLooksTheSameHereAndOnItsLevelPage() throws Exception {
        UUID session = session(2, MONDAY, LocalTime.of(8, 30), 6);
        assign(session, rider("Sara Ali", 2, ahmed), ahmed);
        assign(session, rider("Hana Karim", 2, mona), mona);
        session(2, SUNDAY, LocalTime.of(8, 0), 6);

        String levelBody = mvc.perform(asTrainer(get("/me/levels/2/sessions")
                        .param("from", SATURDAY.toString()).param("to", FRIDAY.toString())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String weekBody = week().andReturn().getResponse().getContentAsString();

        for (String field : new String[]{"capacity", "booked", "past", "seats"}) {
            List<Object> fromLevel = JsonPath.read(levelBody, "$.days[*].sessions[*]." + field);
            List<Object> fromWeek = JsonPath.read(weekBody, "$.days[*].sessions[*]." + field);
            assertThat(fromWeek).as(field).hasSize(2).isEqualTo(fromLevel);
        }
        List<Object> mine = JsonPath.read(weekBody,
                "$.days[*].sessions[?(@.id == '" + session + "')].mine");
        assertThat(mine).containsExactly(1);
    }

    // Case 12
    @Test
    void onlyTrainersGetIn() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    private ResultActions week() throws Exception {
        return mvc.perform(asTrainer(get(PATH)));
    }

    private ResultActions week(String date) throws Exception {
        return mvc.perform(asTrainer(get(PATH).param("date", date)));
    }

    private long statementsFor(Request request) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        request.run().andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private MockHttpServletRequestBuilder asTrainer(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(ahmed, Role.TRAINER)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private UUID trainer(String name) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, 'active', 'not-a-real-hash') returning id""",
                UUID.class, name, "0199999%04d".formatted(seq), "week%d@school.eg".formatted(seq));
    }

    private UUID rider(String name, int level, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, level, trainer);
    }

    private void assign(UUID session, UUID rider, UUID assignedBy) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                session, rider, assignedBy);
    }

    // A one-hour session of the level on that date; a null level makes an assessment slot.
    private UUID session(Integer level, LocalDate date, LocalTime start, int capacity) {
        return saveSession(level, date, start, capacity, false);
    }

    private UUID cancelledSession(Integer level, LocalDate date, LocalTime start) {
        return saveSession(level, date, start, 6, true);
    }

    private UUID saveSession(Integer level, LocalDate date, LocalTime start, int capacity, boolean cancelled) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setLevel(level == null ? null : levelRepository.getReferenceById(level.shortValue()));
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(capacity);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            Session session = Session.of(template, date);
            if (cancelled) {
                session.cancel(CLOCK.instant());
            }
            return sessionRepository.save(session).getId();
        });
    }

    private static Instant cairo(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(CAIRO).toInstant();
    }

    @FunctionalInterface
    private interface Request {
        ResultActions run() throws Exception;
    }
}
