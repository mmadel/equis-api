package com.equestrian.equis.controller.evaluations;

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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E14 · POST /me/sessions/{id}/evaluations and GET /me/evaluations/pending (screens 1 and 3), over HTTP against a
// real database. Tue 3 Nov 2026 in Cairo, 18:10. Today's 17:00 Level 2 session finished at 18:05 with Sara and
// Omar seated by Ahmed and Hala seated by Mona.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=session-evaluation-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
public class SessionEvaluationApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 3);
    private static final LocalDate LAST_THURSDAY = LocalDate.of(2026, 10, 29);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    private static final Instant AT_18_05 = cairo(TUESDAY, 18, 5);
    private static final Instant AT_18_10 = cairo(TUESDAY, 18, 10);
    private static final int LEVEL_2 = 2;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(AT_18_10);

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
    private UUID omar;
    private UUID hala;
    private UUID session;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, riders, payments, generated sessions and evaluations.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(AT_18_10);
        seq = 0;
        admin = jdbc.queryForObject("select id from admin_account limit 1", UUID.class);
        ahmed = trainer("Ahmed Said");
        mona = trainer("Mona Adel");
        sara = rider("Sara Ali", ahmed);
        omar = rider("Omar Nabil", ahmed);
        hala = rider("Hala Samir", mona);

        session = saveSession(LEVEL_2, TUESDAY, FIVE_PM, State.SCHEDULED);
        assign(session, sara, ahmed);
        assign(session, omar, ahmed);
        assign(session, hala, mona);
        markFinished(session, ahmed, AT_18_05);
    }

    // Case 1
    @Test
    void ahmedSeesHisRidersReadyToEvaluate() throws Exception {
        mvc.perform(as(ahmed, get("/me/sessions/" + session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath("$.mySeats[*].name", contains("Omar Nabil", "Sara Ali")))
                .andExpect(jsonPath("$.mySeats[*].canEvaluate", contains(true, true)))
                .andExpect(jsonPath("$.mySeats[0].evaluation").value(nullValue()))
                .andExpect(jsonPath("$.mySeats[1].evaluation").value(nullValue()));
    }

    // Case 2
    @Test
    void ahmedEvaluatesSara() throws Exception {
        evaluate(session, ahmed, sara, 4, "Better heels down")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.toString()))
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath("$.finishedAt").value("2026-11-03T16:05:00Z"))
                .andExpect(jsonPath("$.canAssign").value(false))
                .andExpect(jsonPath("$.canFinish").value(false))
                .andExpect(jsonPath("$.readOnly").value(true))
                .andExpect(jsonPath("$.otherCoachSeats").value(1))
                .andExpect(jsonPath("$.riders", empty()))
                .andExpect(seat("Sara Ali", "canEvaluate", false))
                .andExpect(seat("Sara Ali", "evaluation.stars", 4))
                .andExpect(seat("Sara Ali", "evaluation.comment", "Better heels down"))
                .andExpect(seat("Sara Ali", "evaluation.createdAt", "2026-11-03T16:10:00Z"))
                .andExpect(seat("Omar Nabil", "canEvaluate", true));

        assertThat(jdbc.queryForObject("""
                select trainer_id from session_evaluation where session_id = ? and rider_id = ?""",
                UUID.class, session, sara)).isEqualTo(ahmed);
    }

    // Case 3
    @Test
    void omarWithoutACommentLeavesTheSessionFinishedWhileHalaIsMissing() throws Exception {
        evaluate(session, ahmed, sara, 4, "Better heels down").andExpect(status().isOk());

        mvc.perform(as(ahmed, post("/me/sessions/" + session + "/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderId\": \"" + omar + "\", \"stars\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(seat("Omar Nabil", "evaluation.stars", 3))
                .andExpect(seat("Omar Nabil", "evaluation.comment", null))
                .andExpect(seat("Omar Nabil", "canEvaluate", false));
        assertThat(sessionStatus(session)).isEqualTo("finished");
    }

    // Case 4
    @Test
    void monasLastRiderMakesTheSessionDone() throws Exception {
        evaluate(session, ahmed, sara, 4, null).andExpect(status().isOk());
        evaluate(session, ahmed, omar, 3, null).andExpect(status().isOk());

        evaluate(session, mona, hala, 5, "Great canter")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(seat("Hala Samir", "canEvaluate", false));
        assertThat(sessionStatus(session)).isEqualTo("done");

        // Done: Ahmed's seats keep their evaluations and nothing is left to evaluate.
        mvc.perform(as(ahmed, get("/me/sessions/" + session)))
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.mySeats[*].canEvaluate", contains(false, false)))
                .andExpect(jsonPath("$.mySeats[*].evaluation.stars", contains(3, 4)));
    }

    // Case 5
    @Test
    void evaluatingSaraTwiceIsRefused() throws Exception {
        evaluate(session, ahmed, sara, 4, null).andExpect(status().isOk());

        evaluate(session, ahmed, sara, 2, "Changed my mind")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_evaluated"))
                .andExpect(jsonPath("$.detail").value("Sara Ali already has an evaluation for this session."));
        assertThat(evaluationsOf(sara)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select stars from session_evaluation where rider_id = ?", Integer.class,
                sara)).isEqualTo(4);
    }

    // Case 6
    @Test
    void ahmedCantEvaluateMonasRider() throws Exception {
        evaluate(session, ahmed, hala, 4, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("rider_not_yours"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("riderId"));
        assertThat(evaluationsOf(hala)).isZero();
    }

    // Case 7
    @Test
    void aRiderNotInTheSessionIsRefused() throws Exception {
        UUID nour = rider("Nour Adel", ahmed);

        evaluate(session, ahmed, nour, 4, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("rider_not_in_session"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("riderId"));
        evaluate(session, ahmed, UUID.randomUUID(), 4, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("rider_not_in_session"));
    }

    // Case 8
    @Test
    void scheduledCancelledAssessmentAndUnknownSessionsAreRefused() throws Exception {
        UUID scheduled = saveSession(LEVEL_2, TUESDAY, LocalTime.of(19, 0), State.SCHEDULED);
        assign(scheduled, sara, ahmed);
        UUID cancelled = saveSession(LEVEL_2, TUESDAY, LocalTime.of(15, 0), State.CANCELLED);
        UUID assessment = saveSession(null, TUESDAY, LocalTime.of(15, 0), State.SCHEDULED);

        mvc.perform(as(ahmed, get("/me/sessions/" + scheduled)))
                .andExpect(jsonPath("$.mySeats[0].canEvaluate").value(false))
                .andExpect(jsonPath("$.mySeats[0].evaluation").value(nullValue()));
        evaluate(scheduled, ahmed, sara, 4, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_not_finished"));
        evaluate(cancelled, ahmed, sara, 4, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_read_only"));
        evaluate(assessment, ahmed, sara, 4, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("session_not_found"));
        evaluate(UUID.randomUUID(), ahmed, sara, 4, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("session_not_found"));
        assertThat(jdbc.queryForObject("select count(*) from session_evaluation", Integer.class)).isZero();
    }

    // Case 9
    @Test
    void starsOutOfRangeOrMissingAndALongCommentAreRefused() throws Exception {
        for (String stars : List.of("0", "6", "null")) {
            send(session, ahmed, "{\"riderId\": \"" + sara + "\", \"stars\": " + stars + "}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[*].field", contains("stars")));
        }
        send(session, ahmed, "{\"riderId\": \"" + sara + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", contains("stars")));
        send(session, ahmed, "{\"stars\": 4}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", contains("riderId")));
        evaluate(session, ahmed, sara, 4, "a".repeat(501))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", contains("comment")));
        assertThat(evaluationsOf(sara)).isZero();

        // 500 characters is the limit, and is accepted.
        evaluate(session, ahmed, sara, 4, "a".repeat(500)).andExpect(status().isOk());
    }

    // Case 10
    @Test
    void aBlankCommentIsStoredAsNone() throws Exception {
        evaluate(session, ahmed, sara, 4, "   ")
                .andExpect(status().isOk())
                .andExpect(seat("Sara Ali", "evaluation.comment", null));
        assertThat(jdbc.queryForObject("select comment from session_evaluation where rider_id = ?", String.class,
                sara)).isNull();
    }

    // Case 11
    @Test
    void lastThursdaysSessionCanStillBeEvaluatedToday() throws Exception {
        UUID thursday = saveSession(LEVEL_2, LAST_THURSDAY, FIVE_PM, State.SCHEDULED);
        assign(thursday, sara, ahmed);
        markFinished(thursday, ahmed, cairo(LAST_THURSDAY, 18, 0));

        evaluate(thursday, ahmed, sara, 5, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"));
    }

    // Case 12
    @Test
    void saraCanBeEvaluatedAfterMovingToMonaOrRunningOutOfSessions() throws Exception {
        // Sara has no package at all, so she is awaiting payment; then she moves to Mona.
        mvc.perform(as(ahmed, get("/me/riders")))
                .andExpect(jsonPath("$[?(@.id == '" + sara + "')].balance.status", contains("awaiting_payment")));
        jdbc.update("update rider set trainer_id = ? where id = ?", mona, sara);

        evaluate(session, ahmed, sara, 4, null)
                .andExpect(status().isOk())
                .andExpect(seat("Sara Ali", "evaluation.stars", 4));
        // Her coach today is Mona, but Ahmed seated her in this session.
        evaluate(session, mona, sara, 4, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("rider_not_yours"));
    }

    // Case 13
    @Test
    void pendingCountsDownAsAhmedEvaluates() throws Exception {
        pending(ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.sessions[*].sessionId", contains(session.toString())))
                .andExpect(jsonPath("$.sessions[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$.sessions[0].start").value("17:00"))
                .andExpect(jsonPath("$.sessions[0].end").value("18:00"))
                .andExpect(jsonPath("$.sessions[0].levelId").value(LEVEL_2))
                .andExpect(jsonPath("$.sessions[0].ridersToEvaluate").value(2));
        pending(mona).andExpect(jsonPath("$.count").value(1));

        evaluate(session, ahmed, sara, 4, null).andExpect(status().isOk());
        pending(ahmed)
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.sessions[0].ridersToEvaluate").value(1));

        evaluate(session, ahmed, omar, 3, null).andExpect(status().isOk());
        // Fully evaluated by Ahmed, though not done yet: gone from his list, still on Mona's.
        pending(ahmed)
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.sessions", empty()));
        pending(mona).andExpect(jsonPath("$.count").value(1));

        evaluate(session, mona, hala, 5, null).andExpect(status().isOk());
        pending(mona)
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.sessions", empty()));
    }

    // Case 14: also a scheduled session today and an earlier finished one today, to check the order and filter.
    @Test
    void pendingListsFinishedSessionsOfSeveralDaysNewestFirst() throws Exception {
        UUID thursday = saveSession(LEVEL_2, LAST_THURSDAY, FIVE_PM, State.SCHEDULED);
        assign(thursday, sara, ahmed);
        markFinished(thursday, ahmed, cairo(LAST_THURSDAY, 18, 0));
        UUID morning = saveSession(LEVEL_2, TUESDAY, LocalTime.of(9, 0), State.SCHEDULED);
        assign(morning, omar, ahmed);
        markFinished(morning, ahmed, cairo(TUESDAY, 10, 0));
        UUID evening = saveSession(LEVEL_2, TUESDAY, LocalTime.of(20, 0), State.SCHEDULED);
        assign(evening, sara, ahmed);

        pending(ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4))
                .andExpect(jsonPath("$.sessions[*].sessionId",
                        contains(session.toString(), morning.toString(), thursday.toString())))
                .andExpect(jsonPath("$.sessions[*].ridersToEvaluate", contains(2, 1, 1)))
                .andExpect(jsonPath("$.sessions[*].date", contains("2026-11-03", "2026-11-03", "2026-10-29")));
    }

    // Case 15
    @Test
    void ahmedsAndMonasLastRidersAtOnceMakeItDoneOnce() throws Exception {
        evaluate(session, ahmed, sara, 4, null).andExpect(status().isOk());

        List<Integer> statuses = race(() -> evaluate(session, ahmed, omar, 3, null).andReturn(),
                () -> evaluate(session, mona, hala, 5, null).andReturn());

        assertThat(statuses).containsExactly(200, 200);
        assertThat(sessionStatus(session)).isEqualTo("done");
        assertThat(evaluationsOf(omar)).isEqualTo(1);
        assertThat(evaluationsOf(hala)).isEqualTo(1);
    }

    // Case 16
    @Test
    void twoSavesForSaraAtOnceKeepOne() throws Exception {
        List<MvcResult> results = raceResults(() -> evaluate(session, ahmed, sara, 4, null).andReturn(),
                () -> evaluate(session, ahmed, sara, 4, null).andReturn());

        List<Integer> statuses = results.stream().map(r -> r.getResponse().getStatus()).toList();
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        MvcResult refused = results.get(statuses.indexOf(409));
        assertThat(refused.getResponse().getContentAsString()).contains("already_evaluated");
        assertThat(evaluationsOf(sara)).isEqualTo(1);
    }

    // Case 17
    @Test
    void onlyTrainersGetIn() throws Exception {
        mvc.perform(post("/me/sessions/" + session + "/evaluations").contentType(MediaType.APPLICATION_JSON)
                        .content(body(sara, 4, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get("/me/evaluations/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(asAdmin(post("/me/sessions/" + session + "/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(sara, 4, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(asAdmin(get("/me/evaluations/pending")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        assertThat(jdbc.queryForObject("select count(*) from session_evaluation", Integer.class)).isZero();
    }

    private ResultActions evaluate(UUID sessionId, UUID trainer, UUID rider, int stars, String comment)
            throws Exception {
        return send(sessionId, trainer, body(rider, stars, comment));
    }

    private ResultActions send(UUID sessionId, UUID trainer, String json) throws Exception {
        return mvc.perform(as(trainer, post("/me/sessions/" + sessionId + "/evaluations"))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions pending(UUID trainer) throws Exception {
        return mvc.perform(as(trainer, get("/me/evaluations/pending")));
    }

    private static String body(UUID rider, int stars, String comment) {
        String c = comment == null ? "null" : "\"" + comment + "\"";
        return "{\"riderId\": \"" + rider + "\", \"stars\": " + stars + ", \"comment\": " + c + "}";
    }

    // One field of the caller's seat for that rider.
    private static ResultMatcher seat(String name, String field, Object value) {
        String path = "$.mySeats[?(@.name == '" + name + "')]." + field;
        return value == null ? jsonPath(path, contains(nullValue())) : jsonPath(path, contains(value));
    }

    private int evaluationsOf(UUID rider) {
        return jdbc.queryForObject("select count(*) from session_evaluation where session_id = ? and rider_id = ?",
                Integer.class, session, rider);
    }

    private String sessionStatus(UUID sessionId) {
        return jdbc.queryForObject("select status from session where id = ?", String.class, sessionId);
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
                UUID.class, name, "0199996%04d".formatted(seq), "evaluate%d@school.eg".formatted(seq));
    }

    private UUID rider(String name, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, LEVEL_2, trainer);
    }

    private void assign(UUID sessionId, UUID rider, UUID assignedBy) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                sessionId, rider, assignedBy);
    }

    // The finish itself (and its session usage) is covered by FinishSessionApiTest.
    private void markFinished(UUID sessionId, UUID by, Instant at) {
        jdbc.update("update session set status = 'finished', finished_at = ?, finished_by = ? where id = ?",
                Timestamp.from(at), by, sessionId);
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
