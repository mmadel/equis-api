package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.support.MutableClock;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E13 · POST /me/sessions/{id}/finish (screen 3), over HTTP against a real database.
// Tue 3 Nov 2026 in Cairo. Today's 17:00 Level 2 session: Sara (8 left) and Nour (1 left) seated by Ahmed,
// Hala (4 left) seated by Mona.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=finish-session-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
public class FinishSessionApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 3);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    private static final Instant AT_16_55 = cairo(TUESDAY, 16, 55);
    private static final Instant AT_18_05 = cairo(TUESDAY, 18, 5);
    private static final LocalDate NOV = LocalDate.of(2026, 11, 1);
    private static final int LEVEL_2 = 2;

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    static final MutableClock CLOCK = new MutableClock(AT_18_05);

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
    private UUID nour;
    private UUID hala;
    private UUID saraPackage;
    private UUID nourPackage;
    private UUID halaPackage;
    private UUID session;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, riders, payments and generated sessions.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(AT_18_05);
        seq = 0;
        admin = jdbc.queryForObject("select id from admin_account limit 1", UUID.class);
        ahmed = trainer("Ahmed Said");
        mona = trainer("Mona Adel");
        sara = rider("Sara Ali", ahmed);
        saraPackage = pay(sara, NOV, 8);
        nour = rider("Nour Adel", ahmed);
        nourPackage = pay(nour, NOV, 1);
        hala = rider("Hala Samir", mona);
        halaPackage = pay(hala, NOV, 4);

        session = saveSession(LEVEL_2, TUESDAY, FIVE_PM, State.SCHEDULED);
        assign(session, sara, ahmed);
        assign(session, nour, ahmed);
        assign(session, hala, mona);
    }

    // Case 1
    @Test
    void ahmedFinishesAndEveryRiderUsesOneSession() throws Exception {
        finish(session, ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.toString()))
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath("$.finishedAt").value("2026-11-03T16:05:00Z"))
                .andExpect(jsonPath("$.canAssign").value(false))
                .andExpect(jsonPath("$.canFinish").value(false))
                .andExpect(jsonPath("$.readOnly").value(true))
                .andExpect(jsonPath("$.otherCoachSeats").value(1))
                .andExpect(jsonPath("$.mySeats[*].name", contains("Nour Adel", "Sara Ali")))
                .andExpect(jsonPath("$.mySeats[*].sessionsLeft", contains(0, 7)))
                .andExpect(jsonPath("$.mySeats[*].canEvaluate", contains(true, true)))
                .andExpect(jsonPath("$.mySeats[0].evaluation").value(nullValue()))
                .andExpect(jsonPath("$.mySeats[1].evaluation").value(nullValue()))
                .andExpect(jsonPath("$.riders", empty()));

        assertThat(usages()).isEqualTo(3);
        assertThat(usagesOf(saraPackage)).isEqualTo(1);
        assertThat(usagesOf(nourPackage)).isEqualTo(1);
        assertThat(usagesOf(halaPackage)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select finished_by from session where id = ?", UUID.class, session))
                .isEqualTo(ahmed);
        assertThat(sessionStatus()).isEqualTo("finished");
    }

    // Case 2
    @Test
    void afterTheFinishNourIsAwaitingPaymentOnMyRidersAndW3() throws Exception {
        finish(session, ahmed).andExpect(status().isOk());

        mvc.perform(as(ahmed, get("/me/riders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + nour + "')].balance.status", contains("awaiting_payment")))
                .andExpect(jsonPath("$[?(@.id == '" + sara + "')].balance.sessionsLeft", contains(7)));
        mvc.perform(asAdmin(get("/admin/riders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[?(@.id == '" + nour + "')].balance.status",
                        contains("awaiting_payment")))
                .andExpect(jsonPath("$.riders[?(@.id == '" + sara + "')].balance.sessionsLeft", contains(7)))
                .andExpect(jsonPath("$.riders[?(@.id == '" + hala + "')].balance.sessionsLeft", contains(3)));
    }

    // Case 3
    @Test
    void beforeTheStartIsRefused() throws Exception {
        CLOCK.set(AT_16_55);

        finish(session, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_not_started"));
        assertThat(usages()).isZero();
        assertThat(sessionStatus()).isEqualTo("scheduled");
    }

    // Case 4
    @Test
    void finishingTwiceIsRefused() throws Exception {
        finish(session, ahmed).andExpect(status().isOk());

        finish(session, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_already_finished"));
        assertThat(usages()).isEqualTo(3);
    }

    // Case 5
    @Test
    void myRiderWithoutASessionLeftBlocksTheFinish() throws Exception {
        voidPackage(nourPackage);

        finish(session, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_sessions_left"))
                .andExpect(jsonPath("$.detail")
                        .value("Nour Adel has no sessions left. Remove them from the session, then finish it."))
                .andExpect(jsonPath("$.details.riders[*].id", contains(nour.toString())))
                .andExpect(jsonPath("$.details.riders[*].name", contains("Nour Adel")))
                .andExpect(jsonPath("$.details.otherCoachRiders").value(0));
        assertThat(usages()).isZero();
        assertThat(sessionStatus()).isEqualTo("scheduled");
    }

    // Case 6
    @Test
    void anotherCoachsRiderWithoutASessionLeftIsOnlyCounted() throws Exception {
        voidPackage(halaPackage);

        finish(session, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_sessions_left"))
                .andExpect(jsonPath("$.detail")
                        .value("1 rider of another coach has no sessions left. Ask their coach to remove them."))
                .andExpect(jsonPath("$.details.riders", empty()))
                .andExpect(jsonPath("$.details.otherCoachRiders").value(1));
        assertThat(usages()).isZero();
        assertThat(sessionStatus()).isEqualTo("scheduled");
    }

    // Case 7. E14 case 13: with nobody to evaluate, an empty session goes straight to done.
    @Test
    void anEmptySessionIsFinishedWithoutUsingAnything() throws Exception {
        UUID empty = saveSession(LEVEL_2, TUESDAY, LocalTime.of(15, 0), State.SCHEDULED);

        finish(empty, ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.finishedAt").value("2026-11-03T16:05:00Z"))
                .andExpect(jsonPath("$.mySeats", empty()));
        assertThat(usages()).isZero();
    }

    // Case 8
    @Test
    void aTrainerWithoutARiderInTheSessionCantFinishIt() throws Exception {
        UUID karim = trainer("Karim Fathy");

        finish(session, karim)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("session_not_yours"));
        assertThat(usages()).isZero();
        assertThat(sessionStatus()).isEqualTo("scheduled");
    }

    // Case 9
    @Test
    void cancelledAssessmentAndUnknownSessionsAreRefused() throws Exception {
        UUID cancelled = saveSession(LEVEL_2, TUESDAY, LocalTime.of(15, 0), State.CANCELLED);
        UUID assessment = saveSession(null, TUESDAY, LocalTime.of(15, 0), State.SCHEDULED);

        finish(cancelled, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_read_only"));
        finish(assessment, ahmed)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("session_not_found"));
        finish(UUID.randomUUID(), ahmed)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("session_not_found"));
    }

    // Case 10: Saturday 31 October, finished on Sunday 1 November. Omar's September package ends on 31 October,
    // so it still pays for that session.
    @Test
    void yesterdaysSessionUsesPackagesValidOnItsDate() throws Exception {
        LocalDate saturday = LocalDate.of(2026, 10, 31);
        CLOCK.set(cairo(LocalDate.of(2026, 11, 1), 9, 0));
        UUID omar = rider("Omar Hany", ahmed);
        UUID september = pay(omar, LocalDate.of(2026, 9, 1), 4);
        UUID yesterday = saveSession(LEVEL_2, saturday, FIVE_PM, State.SCHEDULED);
        assign(yesterday, omar, ahmed);

        finish(yesterday, ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath("$.mySeats[0].sessionsLeft").value(3));
        assertThat(usagesOf(september)).isEqualTo(1);
    }

    // Case 11
    @Test
    void twoCoachesFinishingAtOnceFinishItOnce() throws Exception {
        List<Integer> statuses = race(() -> finish(session, ahmed).andReturn(),
                () -> finish(session, mona).andReturn());

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(usages()).isEqualTo(3);
    }

    // Case 12
    @Test
    void aFinishAndAVoidAtOnceNeverUseAVoidedPackage() throws Exception {
        UUID onlySara = saveSession(LEVEL_2, TUESDAY, LocalTime.of(15, 0), State.SCHEDULED);
        assign(onlySara, sara, ahmed);

        List<MvcResult> results = raceResults(() -> finish(onlySara, ahmed).andReturn(),
                () -> mvc.perform(asAdmin(post("/admin/payments/" + saraPackage + "/void"))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"reason\": \"Entered twice\" }"))
                        .andReturn());
        MvcResult finish = results.get(0);
        MvcResult voidIt = results.get(1);

        if (finish.getResponse().getStatus() == 200) {
            assertThat(voidIt.getResponse().getStatus()).isEqualTo(409);
            assertThat(voidIt.getResponse().getContentAsString()).contains("payment_has_usage");
        } else {
            assertThat(finish.getResponse().getStatus()).isEqualTo(409);
            assertThat(finish.getResponse().getContentAsString()).contains("rider_no_sessions_left");
            assertThat(voidIt.getResponse().getStatus()).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject("""
                select count(*) from session_usage u join payment p on p.id = u.payment_id
                where p.voided_at is not null""", Integer.class)).isZero();
    }

    // Case 13
    @Test
    void afterTheFinishNobodyCanBeAddedOrRemoved() throws Exception {
        finish(session, ahmed).andExpect(status().isOk());

        mvc.perform(as(ahmed, put("/me/sessions/" + session + "/assignments"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderIds\": [\"" + sara + "\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_read_only"));
    }

    // Case 14
    @Test
    void onlyTrainersCanFinish() throws Exception {
        mvc.perform(post("/me/sessions/" + session + "/finish"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(asAdmin(post("/me/sessions/" + session + "/finish")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        assertThat(sessionStatus()).isEqualTo("scheduled");
    }

    private ResultActions finish(UUID sessionId, UUID trainer) throws Exception {
        return mvc.perform(as(trainer, post("/me/sessions/" + sessionId + "/finish")));
    }

    private int usages() {
        return jdbc.queryForObject("select count(*) from session_usage", Integer.class);
    }

    private int usagesOf(UUID paymentId) {
        return jdbc.queryForObject("select count(*) from session_usage where payment_id = ?", Integer.class,
                paymentId);
    }

    private String sessionStatus() {
        return jdbc.queryForObject("select status from session where id = ?", String.class, session);
    }

    private void voidPackage(UUID paymentId) {
        jdbc.update("update payment set voided_at = now(), voided_by = ?, void_reason = 'Entered twice' where id = ?",
                admin, paymentId);
    }

    private MockHttpServletRequestBuilder as(UUID trainer, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(trainer, Role.TRAINER)).value());
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
                UUID.class, name, "0199997%04d".formatted(seq), "finish%d@school.eg".formatted(seq));
    }

    private UUID rider(String name, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, LEVEL_2, trainer);
    }

    // A one-month package starting on startMonth, valid to the end of the following month.
    private UUID pay(UUID rider, LocalDate startMonth, int sessions) {
        return jdbc.queryForObject("""
                        insert into payment (rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_by)
                        values (?, 1200, 'cash', ?, 1, ?, ?, ?) returning id""",
                UUID.class, rider, startMonth, sessions, startMonth.plusMonths(2).minusDays(1), admin);
    }

    private void assign(UUID sessionId, UUID rider, UUID assignedBy) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                sessionId, rider, assignedBy);
    }

    private enum State { SCHEDULED, CANCELLED }

    // A one-hour session on that date; a null level makes it an assessment session.
    private UUID saveSession(Integer level, LocalDate date, LocalTime start, State state) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setLevel(level == null ? null : levelRepository.getReferenceById(level.shortValue()));
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(6);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            Session created = Session.of(template, date);
            if (state == State.CANCELLED) {
                created.cancel(CLOCK.instant());
            }
            return sessionRepository.save(created).getId();
        });
    }

    private List<Integer> race(Request a, Request b) throws Exception {
        List<Integer> statuses = new ArrayList<>();
        for (MvcResult result : raceResults(a, b)) {
            statuses.add(result.getResponse().getStatus());
        }
        return statuses;
    }

    // Both requests start together, without a test transaction, so each runs in its own.
    private List<MvcResult> raceResults(Request a, Request b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<MvcResult> first = pool.submit(() -> {
                start.await();
                return a.send();
            });
            Future<MvcResult> second = pool.submit(() -> {
                start.await();
                return b.send();
            });
            start.countDown();
            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface Request {
        MvcResult send() throws Exception;
    }

    private static Instant cairo(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(CAIRO).toInstant();
    }
}
