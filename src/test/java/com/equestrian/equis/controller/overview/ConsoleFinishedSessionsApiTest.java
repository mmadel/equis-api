package com.equestrian.equis.controller.overview;

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

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E13 · finished sessions on the console: the real state of today's sessions on GET /admin/overview, and the sessions
// used from each package in the payment answers (W3). Tue 3 Nov 2026 in Cairo, admin token.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=console-finished-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Testcontainers
public class ConsoleFinishedSessionsApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 3);
    private static final LocalDate NOV = LocalDate.of(2026, 11, 1);
    private static final LocalDate DEC = LocalDate.of(2026, 12, 1);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);
    private static final LocalTime EIGHT_PM = LocalTime.of(20, 0);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(at(19, 30));

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

    private int seq;
    private UUID admin;
    private UUID ahmed;
    private UUID mona;
    private UUID sara;
    private UUID saraNov;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, riders, payments and generated sessions.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(at(19, 30));
        seq = 0;
        admin = jdbc.queryForObject(
                "select id from admin_account where email = 'console-finished-admin@example.com'", UUID.class);
        ahmed = trainer("Ahmed Said");
        mona = trainer("Mona Fathy");
        sara = rider("Sara Ali", 2, ahmed);
        saraNov = pay(sara, NOV, 8);
    }

    // Case 1
    @Test
    void atHalfPastSevenTodaysSessionsShowTheirRealState() throws Exception {
        UUID finished = session(2, TUESDAY, FIVE_PM);
        UUID forgotten = session(4, TUESDAY, SIX_PM);
        UUID later = session(1, TUESDAY, EIGHT_PM);
        markFinished(finished, at(18, 5));

        overview()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsToday[*].sessionId",
                        contains(finished.toString(), forgotten.toString(), later.toString())))
                .andExpect(jsonPath("$.sessionsToday[*].state", contains("finished", "not_finished", "upcoming")));
    }

    // Case 2
    @Test
    void anUnfinishedSessionBetweenStartAndEndIsInProgress() throws Exception {
        session(4, TUESDAY, SIX_PM);
        CLOCK.set(at(18, 30));

        overview().andExpect(jsonPath("$.sessionsToday[0].state").value("in_progress"));
    }

    // Case 3
    @Test
    void aSessionFinishedBeforeItsEndTimeIsFinished() throws Exception {
        UUID session = session(2, TUESDAY, FIVE_PM);
        markFinished(session, at(17, 25));
        CLOCK.set(at(17, 30));

        overview().andExpect(jsonPath("$.sessionsToday[0].state").value("finished"));
    }

    // Case 4
    @Test
    void anAssessmentSlotKeepsTheTimeRule() throws Exception {
        session(null, TUESDAY, LocalTime.of(12, 0));

        overview()
                .andExpect(jsonPath("$.sessionsToday[0].levelId").value((Object) null))
                .andExpect(jsonPath("$.sessionsToday[0].state").value("finished"));
    }

    // Case 5
    @Test
    void finishingASessionLeavesTheWeekFillAsItWas() throws Exception {
        UUID session = session(2, TUESDAY, FIVE_PM);
        assign(session, sara, ahmed);
        String before = body(overview());

        finish(session, ahmed).andExpect(status().isOk());
        String after = body(overview());

        assertThat((Map<String, Object>) JsonPath.read(after, "$.week"))
                .isEqualTo(JsonPath.read(before, "$.week"));
        assertThat((Integer) JsonPath.read(after, "$.week.booked")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(after, "$.sessionsToday[0].booked")).isEqualTo(1);
    }

    // Case 6
    @Test
    void afterOneFinishedSessionTheHistoryListsItUnderThePackage() throws Exception {
        UUID session = session(2, TUESDAY, FIVE_PM);
        assign(session, sara, ahmed);
        CLOCK.set(at(18, 5));
        finish(session, ahmed).andExpect(status().isOk());

        history(sara)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[0].id").value(saraNov.toString()))
                .andExpect(jsonPath("$.payments[0].used").value(1))
                .andExpect(jsonPath("$.payments[0].left").value(7))
                .andExpect(jsonPath("$.payments[0].usages.length()").value(1))
                .andExpect(jsonPath("$.payments[0].usages[0].sessionId").value(session.toString()))
                .andExpect(jsonPath("$.payments[0].usages[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$.payments[0].usages[0].start").value("17:00"))
                .andExpect(jsonPath("$.payments[0].usages[0].end").value("18:00"))
                .andExpect(jsonPath("$.payments[0].usages[0].levelId").value(2))
                .andExpect(jsonPath("$.payments[0].usages[0].trainerName").value("Ahmed Said"))
                .andExpect(jsonPath("$.payments[0].usages[0].usedAt").value("2026-11-03T16:05:00Z"));
    }

    // Case 7: three sessions from November, then two from December, inserted out of order.
    @Test
    void eachLineIsUnderItsOwnPackageOldestFirst() throws Exception {
        UUID saraDec = pay(sara, DEC, 8);
        UUID nov10 = rode(saraNov, LocalDate.of(2026, 11, 10));
        UUID nov3 = rode(saraNov, LocalDate.of(2026, 11, 3));
        UUID nov7 = rode(saraNov, LocalDate.of(2026, 11, 7));
        UUID dec5 = rode(saraDec, LocalDate.of(2026, 12, 5));
        UUID dec1 = rode(saraDec, LocalDate.of(2026, 12, 1));

        history(sara)
                .andExpect(jsonPath("$.payments[?(@.id == '" + saraNov + "')].used", contains(3)))
                .andExpect(jsonPath("$.payments[?(@.id == '" + saraNov + "')].usages[*].sessionId",
                        contains(nov3.toString(), nov7.toString(), nov10.toString())))
                .andExpect(jsonPath("$.payments[?(@.id == '" + saraNov + "')].usages[*].date",
                        contains("2026-11-03", "2026-11-07", "2026-11-10")))
                .andExpect(jsonPath("$.payments[?(@.id == '" + saraDec + "')].used", contains(2)))
                .andExpect(jsonPath("$.payments[?(@.id == '" + saraDec + "')].usages[*].sessionId",
                        contains(dec1.toString(), dec5.toString())));
    }

    // Case 8
    @Test
    void aRiderMovedToAnotherCoachKeepsTheCoachWhoSeatedThem() throws Exception {
        rode(saraNov, LocalDate.of(2026, 11, 2));
        jdbc.update("update rider set trainer_id = ? where id = ?", mona, sara);

        history(sara).andExpect(jsonPath("$.payments[0].usages[0].trainerName").value("Ahmed Said"));
    }

    // Case 9
    @Test
    void recordAndVoidAnswersCarryAnEmptyUsageList() throws Exception {
        mvc.perform(asAdmin(post("/admin/riders/" + sara + "/payments"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-12\", \"months\": 1 }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.usages", empty()));
        mvc.perform(asAdmin(post("/admin/payments/" + saraNov + "/void"))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"reason\": \"Entered twice\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.used").value(0))
                .andExpect(jsonPath("$.payment.usages", empty()));
    }

    // Case 10
    @Test
    void theHistoryCostsTheSameNumberOfStatementsWithOneOrTenPackages() throws Exception {
        rode(saraNov, LocalDate.of(2026, 11, 2));
        long withOne = statementsForHistory();

        for (int month = 1; month <= 9; month++) {
            UUID p = pay(sara, LocalDate.of(2026, month, 1), 8);
            rode(p, LocalDate.of(2026, month, 10));
        }
        history(sara).andExpect(jsonPath("$.payments.length()").value(10));
        long withTen = statementsForHistory();

        assertThat(withTen).isEqualTo(withOne);
    }

    private long statementsForHistory() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        history(sara).andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private ResultActions overview() throws Exception {
        return mvc.perform(asAdmin(get("/admin/overview")));
    }

    private ResultActions history(UUID rider) throws Exception {
        return mvc.perform(asAdmin(get("/admin/riders/" + rider + "/payments")));
    }

    private ResultActions finish(UUID session, UUID trainer) throws Exception {
        return mvc.perform(post("/me/sessions/" + session + "/finish").header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(trainer, Role.TRAINER)).value()));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(admin, Role.ADMIN)).value());
    }

    private UUID trainer(String name) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, 'active', 'not-a-real-hash') returning id""",
                UUID.class, name, "0199996%04d".formatted(seq), "console%d@school.eg".formatted(seq));
    }

    private UUID rider(String name, int level, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, level, trainer);
    }

    // A one-month package starting on startMonth, valid to the end of the following month.
    private UUID pay(UUID rider, LocalDate startMonth, int sessions) {
        return jdbc.queryForObject("""
                        insert into payment (rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_by)
                        values (?, 1200, 'cash', ?, 1, ?, ?, ?) returning id""",
                UUID.class, rider, startMonth, sessions, startMonth.plusMonths(2).minusDays(1), admin);
    }

    private void assign(UUID session, UUID rider, UUID assignedBy) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                session, rider, assignedBy);
    }

    // Sara rode a 17:00 Level 2 session on that date, seated by Ahmed and paid from this package.
    private UUID rode(UUID paymentId, LocalDate date) {
        UUID session = session(2, date, FIVE_PM);
        assign(session, sara, ahmed);
        Instant usedAt = date.atTime(18, 5).atZone(CAIRO).toInstant();
        markFinished(session, usedAt);
        jdbc.update("insert into session_usage (rider_id, payment_id, session_id, used_at) values (?, ?, ?, ?)",
                sara, paymentId, session, Timestamp.from(usedAt));
        return session;
    }

    private void markFinished(UUID sessionId, Instant when) {
        new TransactionTemplate(transactionManager).executeWithoutResult(s ->
                sessionRepository.findById(sessionId).orElseThrow().finish(ahmed, when));
    }

    // A one-hour session on that date, capacity 6; a null level makes it an assessment slot.
    private UUID session(Integer level, LocalDate date, LocalTime start) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setLevel(level == null ? null : levelRepository.getReferenceById(level.shortValue()));
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(6);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private static Instant at(int hour, int minute) {
        return LocalDateTime.of(TUESDAY, LocalTime.of(hour, minute)).atZone(CAIRO).toInstant();
    }
}
