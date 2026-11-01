package com.equestrian.equis.controller.riderapp;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E15 · the rider app's read API (/rider/me, sessions, assessment, payments, progress) over HTTP against a real
// database. Thu 26 Nov 2026, 10:00 in Cairo. Sara (Level 2, Coach Ahmed) was placed on Wed 14 Oct by Ahmed, paid
// 1,200 EGP cash for November (8 sessions, valid to 31 Dec) and rode 6 times, 4 of them evaluated (4, 4, 3, 4).
// Omar was placed and hasn't paid. Ali is a second rider with his own data. Numbers are the ticket's test cases.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=rider-app-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Testcontainers
class RiderAppApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final Instant THU_26_NOV_10_00 = cairo(LocalDate.of(2026, 11, 26), 10, 0);
    private static final LocalDate OCT_14 = LocalDate.of(2026, 10, 14);
    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    // Sara's six November rides, oldest first; the ones at the even positions 0, 1, 3 and 5 are evaluated.
    private static final List<LocalDate> RIDES = List.of(
            LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 24));

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(THU_26_NOV_10_00);

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
    @Autowired
    EntityManagerFactory entityManagerFactory;

    private int seq;
    private UUID admin;
    private UUID ahmed;
    private UUID mona;
    private UUID sara;
    private UUID omar;
    private UUID ali;
    private UUID novPackage;
    // Sara's rides, newest first, as the API lists them.
    private final List<UUID> saraRides = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, riders, payments, generated sessions and evaluations.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(THU_26_NOV_10_00);
        seq = 0;
        saraRides.clear();
        admin = jdbc.queryForObject("select id from admin_account limit 1", UUID.class);
        ahmed = trainer("Ahmed Said");
        mona = trainer("Mona Fathy");

        sara = placedRider("Sara Ali", "sara.ali@gmail.com", 2, ahmed, OCT_14,
                "Good balance, needs work on rising trot");
        novPackage = pay(sara, NOV_1, 8, cairo(LocalDate.of(2026, 11, 3), 12, 0));
        int[] stars = {4, 4, 0, 3, 0, 4};
        for (int i = 0; i < RIDES.size(); i++) {
            UUID session = finishedSession(2, RIDES.get(i), FIVE_PM, ahmed);
            rode(sara, novPackage, session);
            if (stars[i] > 0) {
                evaluated(session, sara, ahmed, stars[i], "Ride on " + RIDES.get(i), cairo(RIDES.get(i), 18, 10));
            }
            saraRides.addFirst(session);
        }

        omar = placedRider("Omar Hassan", "omar.h@gmail.com", 1, ahmed, LocalDate.of(2026, 11, 25), null);

        ali = placedRider("Ali Kamal", "ali.kamal@gmail.com", 3, mona, OCT_14, "Confident rider");
        UUID aliPackage = pay(ali, NOV_1, 4, cairo(LocalDate.of(2026, 11, 2), 12, 0));
        UUID aliRide = finishedSession(3, LocalDate.of(2026, 11, 18), LocalTime.of(10, 0), mona);
        rode(ali, aliPackage, aliRide);
        evaluated(aliRide, ali, mona, 2, "Ali only", cairo(LocalDate.of(2026, 11, 18), 11, 10));
    }

    // 1
    @Test
    void saraSeesHerProfileAndBalance() throws Exception {
        mvc.perform(asRider(sara, get("/rider/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sara.toString()))
                .andExpect(jsonPath("$.name").value("Sara Ali"))
                .andExpect(jsonPath("$.email").value("sara.ali@gmail.com"))
                .andExpect(jsonPath("$.levelId").value(2))
                .andExpect(jsonPath("$.trainerName").value("Ahmed Said"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.sessionsLeft").value(2))
                .andExpect(jsonPath("$.nextExpiry").value("2026-12-31"))
                .andExpect(jsonPath("$.packages", hasSize(1)))
                .andExpect(jsonPath("$.packages[0].paymentId").value(novPackage.toString()))
                .andExpect(jsonPath("$.packages[0].startMonth").value("2026-11"))
                .andExpect(jsonPath("$.packages[0].validUntil").value("2026-12-31"))
                .andExpect(jsonPath("$.packages[0].sessions").value(8))
                .andExpect(jsonPath("$.packages[0].used").value(6))
                .andExpect(jsonPath("$.packages[0].left").value(2))
                .andExpect(jsonPath("$.banners.expiringSoon").value(nullValue()))
                .andExpect(jsonPath("$.banners.zeroBalance").value(false));
    }

    // 2
    @Test
    void sessionsExpiringWithinAWeekRaiseTheWarning() throws Exception {
        CLOCK.set(cairo(LocalDate.of(2026, 12, 26), 10, 0));

        mvc.perform(asRider(sara, get("/rider/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banners.expiringSoon.sessions").value(2))
                .andExpect(jsonPath("$.banners.expiringSoon.date").value("2026-12-31"))
                .andExpect(jsonPath("$.banners.zeroBalance").value(false));
    }

    // 3
    @Test
    void omarHasNotPaidYet() throws Exception {
        mvc.perform(asRider(omar, get("/rider/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("awaiting_payment"))
                .andExpect(jsonPath("$.sessionsLeft").value(0))
                .andExpect(jsonPath("$.nextExpiry").value(nullValue()))
                .andExpect(jsonPath("$.packages", empty()))
                .andExpect(jsonPath("$.banners.expiringSoon").value(nullValue()))
                .andExpect(jsonPath("$.banners.zeroBalance").value(true));
    }

    // 4
    @Test
    void aPackageForANextMonthIsNotCountedYetButIsListed() throws Exception {
        UUID december = pay(sara, DEC_1, 8, cairo(LocalDate.of(2026, 11, 25), 12, 0));

        mvc.perform(asRider(sara, get("/rider/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsLeft").value(2))
                .andExpect(jsonPath("$.packages[*].paymentId", contains(novPackage.toString())));
        mvc.perform(asRider(sara, get("/rider/payments")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[*].paymentId",
                        contains(december.toString(), novPackage.toString())));
    }

    // 5
    @Test
    void saraSeesHerRidesNewestFirst() throws Exception {
        mvc.perform(asRider(sara, get("/rider/sessions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.items[*].sessionId", contains(ids(saraRides))))
                .andExpect(jsonPath("$.items[*].date", contains("2026-11-24", "2026-11-17", "2026-11-12",
                        "2026-11-10", "2026-11-05", "2026-11-03")))
                .andExpect(jsonPath("$.items[0].start").value("17:00"))
                .andExpect(jsonPath("$.items[0].end").value("18:00"))
                .andExpect(jsonPath("$.items[0].levelId").value(2))
                .andExpect(jsonPath("$.items[0].trainerName").value("Ahmed Said"))
                .andExpect(jsonPath("$.items[0].evaluation.stars").value(4))
                .andExpect(jsonPath("$.items[0].evaluation.comment").value("Ride on 2026-11-24"))
                .andExpect(jsonPath("$.items[0].evaluation.createdAt").value(
                        cairo(LocalDate.of(2026, 11, 24), 18, 10).toString()))
                .andExpect(jsonPath("$.items[1].evaluation").value(nullValue()))
                .andExpect(jsonPath("$.items[3].evaluation").value(nullValue()))
                .andExpect(jsonPath("$.items[*].evaluation.stars", contains(4, 3, 4, 4)));
    }

    // 6
    @Test
    void ridesComeAPageAtATime() throws Exception {
        mvc.perform(asRider(sara, get("/rider/sessions").param("size", "2").param("page", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.items[*].sessionId",
                        contains(saraRides.get(2).toString(), saraRides.get(3).toString())));

        for (String[] bad : new String[][] {{"size", "0"}, {"size", "51"}, {"page", "-1"}}) {
            mvc.perform(asRider(sara, get("/rider/sessions").param(bad[0], bad[1])))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value(bad[0]));
        }
    }

    // 7
    @Test
    void theCoachIsWhoEvaluatedOrElseWhoFinished() throws Exception {
        UUID evaluatedByAhmed = finishedSession(2, LocalDate.of(2026, 11, 25), LocalTime.of(10, 0), mona);
        rode(sara, novPackage, evaluatedByAhmed);
        evaluated(evaluatedByAhmed, sara, ahmed, 5, "Lovely", cairo(LocalDate.of(2026, 11, 25), 11, 30));
        UUID notEvaluated = finishedSession(2, LocalDate.of(2026, 11, 25), LocalTime.of(16, 0), mona);
        rode(sara, novPackage, notEvaluated);

        mvc.perform(asRider(sara, get("/rider/sessions").param("size", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].sessionId",
                        contains(notEvaluated.toString(), evaluatedByAhmed.toString())))
                .andExpect(jsonPath("$.items[*].trainerName", contains("Mona Fathy", "Ahmed Said")));
    }

    // 8
    @Test
    void saraSeesHerAssessment() throws Exception {
        mvc.perform(asRider(sara, get("/rider/assessment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-14"))
                .andExpect(jsonPath("$.start").value("16:00"))
                .andExpect(jsonPath("$.end").value("17:00"))
                .andExpect(jsonPath("$.assessorName").value("Ahmed Said"))
                .andExpect(jsonPath("$.levelId").value(2))
                .andExpect(jsonPath("$.notes").value("Good balance, needs work on rising trot"));
    }

    // 9
    @Test
    void aSeedRiderHasNoAssessment() throws Exception {
        UUID seed = jdbc.queryForObject("""
                        insert into rider (name, level_id, trainer_id, email, password_hash, account_status)
                        values ('Seed Rider', 1, ?, 'seed@gmail.com', 'not-a-real-hash', 'active') returning id""",
                UUID.class, ahmed);

        mvc.perform(asRider(seed, get("/rider/assessment")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("assessment_not_found"));
    }

    // 10
    @Test
    void voidedPaymentsAreLeftOut() throws Exception {
        UUID voided = pay(sara, NOV_1, 4, cairo(LocalDate.of(2026, 11, 20), 12, 0));
        jdbc.update("update payment set voided_at = ?, voided_by = ?, void_reason = 'Typed twice' where id = ?",
                Timestamp.from(cairo(LocalDate.of(2026, 11, 20), 12, 5)), admin, voided);
        UUID december = pay(sara, DEC_1, 8, cairo(LocalDate.of(2026, 11, 25), 23, 30));

        mvc.perform(asRider(sara, get("/rider/payments")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[*].paymentId",
                        contains(december.toString(), novPackage.toString())))
                // 23:30 on 25 Nov in Cairo is still the 25th, though it is 21:30 UTC.
                .andExpect(jsonPath("$.payments[0].recordedOn").value("2026-11-25"))
                .andExpect(jsonPath("$.payments[1].recordedOn").value("2026-11-03"))
                .andExpect(jsonPath("$.payments[1].amountEgp").value(1200.00))
                .andExpect(jsonPath("$.payments[1].method").value("cash"))
                .andExpect(jsonPath("$.payments[1].startMonth").value("2026-11"))
                .andExpect(jsonPath("$.payments[1].months").value(1))
                .andExpect(jsonPath("$.payments[1].sessions").value(8))
                .andExpect(jsonPath("$.payments[1].validUntil").value("2026-12-31"));
        String body = mvc.perform(asRider(sara, get("/rider/payments"))).andReturn().getResponse()
                .getContentAsString();
        assertThat(body).contains("\"amountEgp\":1200.00");
    }

    // 11
    @Test
    void saraSeesHerProgress() throws Exception {
        mvc.perform(asRider(sara, get("/rider/progress")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starTrend", hasSize(1)))
                .andExpect(jsonPath("$.starTrend[0].month").value("2026-11"))
                .andExpect(jsonPath("$.starTrend[0].averageStars").value(3.8))
                .andExpect(jsonPath("$.starTrend[0].evaluations").value(4))
                .andExpect(jsonPath("$.levelHistory", hasSize(1)))
                .andExpect(jsonPath("$.levelHistory[0].type").value("placed"))
                .andExpect(jsonPath("$.levelHistory[0].fromLevelId").value(nullValue()))
                .andExpect(jsonPath("$.levelHistory[0].toLevelId").value(2))
                .andExpect(jsonPath("$.levelHistory[0].byName").value("Ahmed Said"));
    }

    // 12
    @Test
    void theStarTrendCoversTheLastTwelveMonthsOldestFirst() throws Exception {
        UUID nov2025 = finishedSession(2, LocalDate.of(2025, 11, 20), FIVE_PM, ahmed);
        evaluated(nov2025, sara, ahmed, 1, "Too old", cairo(LocalDate.of(2025, 11, 20), 18, 10));
        UUID oct2026 = finishedSession(2, LocalDate.of(2026, 10, 20), FIVE_PM, ahmed);
        evaluated(oct2026, sara, ahmed, 5, "October", cairo(LocalDate.of(2026, 10, 20), 18, 10));

        mvc.perform(asRider(sara, get("/rider/progress")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starTrend[*].month", contains("2026-10", "2026-11")))
                .andExpect(jsonPath("$.starTrend[*].averageStars", contains(5.0, 3.8)))
                .andExpect(jsonPath("$.starTrend[*].evaluations", contains(1, 4)));
    }

    // 13
    @Test
    void aPromotionComesFirstAndTheTrainerSeesTheSameLines() throws Exception {
        jdbc.update("""
                        insert into level_change (rider_id, from_level_id, to_level_id, reason, by_trainer_id, created_at)
                        values (?, 2, 3, 'Ready for canter work', ?, ?)""",
                sara, ahmed, Timestamp.from(cairo(LocalDate.of(2026, 11, 25), 18, 0)));
        jdbc.update("update rider set level_id = 3 where id = ?", sara);

        String riderBody = mvc.perform(asRider(sara, get("/rider/progress")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levelHistory[*].type", contains("promoted", "placed")))
                .andExpect(jsonPath("$.levelHistory[0].fromLevelId").value(2))
                .andExpect(jsonPath("$.levelHistory[0].toLevelId").value(3))
                .andExpect(jsonPath("$.levelHistory[0].reason").value("Ready for canter work"))
                .andExpect(jsonPath("$.levelHistory[0].byName").value("Ahmed Said"))
                .andReturn().getResponse().getContentAsString();
        String trainerBody = mvc.perform(asTrainer(ahmed, get("/me/riders/" + sara + "/level-history")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Object> riderLines = JsonPath.read(riderBody, "$.levelHistory");
        List<Object> trainerLines = JsonPath.read(trainerBody, "$.history");
        assertThat(trainerLines).isEqualTo(riderLines);
    }

    // 14
    @Test
    void aliOnlySeesHisOwnData() throws Exception {
        mvc.perform(asRider(ali, get("/rider/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ali Kamal"))
                .andExpect(jsonPath("$.trainerName").value("Mona Fathy"))
                .andExpect(jsonPath("$.sessionsLeft").value(3));
        mvc.perform(asRider(ali, get("/rider/sessions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[*].evaluation.comment", contains("Ali only")));
        mvc.perform(asRider(ali, get("/rider/assessment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Confident rider"));
        mvc.perform(asRider(ali, get("/rider/payments")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments", hasSize(1)))
                .andExpect(jsonPath("$.payments[0].sessions").value(4));
        mvc.perform(asRider(ali, get("/rider/progress")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starTrend[*].averageStars", contains(2.0)))
                .andExpect(jsonPath("$.levelHistory[*].toLevelId", contains(3)));
    }

    // 15
    @Test
    void onlyRiderTokensGetIn() throws Exception {
        mvc.perform(asTrainer(ahmed, get("/rider/me")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(get("/rider/me").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + jwtService.issue(new AuthUser(admin, Role.ADMIN)).value()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(get("/rider/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    // 16
    @Test
    void fortyRidesTakeAtMostThreeQueries() throws Exception {
        UUID karim = placedRider("Karim Adel", "karim@gmail.com", 2, ahmed, OCT_14, null);
        UUID big = pay(karim, LocalDate.of(2026, 9, 1), 60, cairo(LocalDate.of(2026, 9, 1), 12, 0), 3);
        LocalDate day = LocalDate.of(2026, 9, 1);
        for (int i = 0; i < 40; i++) {
            UUID s = finishedSession(2, day.plusDays(i), FIVE_PM, i % 2 == 0 ? ahmed : mona);
            rode(karim, big, s);
            if (i % 3 == 0) {
                evaluated(s, karim, ahmed, 1 + i % 5, "Ride " + i, cairo(day.plusDays(i), 18, 10));
            }
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mvc.perform(asRider(karim, get("/rider/sessions").param("size", "50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(40)))
                .andExpect(jsonPath("$.totalItems").value(40));
        // The token's rider is checked before the controller runs; then the page and the count.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    // ---- helpers ----

    private static String[] ids(List<UUID> ids) {
        return ids.stream().map(UUID::toString).toArray(String[]::new);
    }

    private MockHttpServletRequestBuilder asRider(UUID rider, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(rider, Role.RIDER)).value());
    }

    private MockHttpServletRequestBuilder asTrainer(UUID trainer, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(trainer, Role.TRAINER)).value());
    }

    private UUID trainer(String name) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, 'active', 'not-a-real-hash') returning id""",
                UUID.class, name, "0199997%04d".formatted(seq), "riderapp%d@school.eg".formatted(seq));
    }

    // A person assessed on that day at 16:00 by the assessor, placed at the level, and the active rider account
    // they became. Writing these through the API is covered by the intake and rider account tests.
    private UUID placedRider(String name, String email, int level, UUID assessor, LocalDate assessedOn,
                             String notes) {
        seq++;
        String phone = "0109997%04d".formatted(seq);
        UUID member = jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, name, phone, email);
        UUID slot = session(null, assessedOn, LocalTime.of(16, 0));
        jdbc.update("""
                        insert into assessment_booking (member_id, session_id, assessor_id, status, attended, level_id,
                                                        notes, recorded_at)
                        values (?, ?, ?, 'placed', true, ?, ?, ?)""",
                member, slot, assessor, level, notes, Timestamp.from(cairo(assessedOn, 17, 20)));
        return jdbc.queryForObject("""
                        insert into rider (name, phone, level_id, trainer_id, member_id, email, password_hash,
                                           account_status)
                        values (?, ?, ?, ?, ?, ?, 'not-a-real-hash', 'active') returning id""",
                UUID.class, name, phone, level, assessor, member, email);
    }

    private UUID pay(UUID rider, LocalDate startMonth, int sessions, Instant recordedAt) {
        return pay(rider, startMonth, sessions, recordedAt, 1);
    }

    private UUID pay(UUID rider, LocalDate startMonth, int sessions, Instant recordedAt, int months) {
        return jdbc.queryForObject("""
                        insert into payment (rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_at, recorded_by)
                        values (?, 1200, 'cash', ?, ?, ?, ?, ?, ?) returning id""",
                UUID.class, rider, startMonth, months, sessions, startMonth.plusMonths(months + 1L).minusDays(1),
                Timestamp.from(recordedAt), admin);
    }

    // The rider sat in the finished session and it was paid from this package.
    private void rode(UUID rider, UUID payment, UUID session) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                session, rider, ahmed);
        jdbc.update("insert into session_usage (rider_id, payment_id, session_id, used_at) values (?, ?, ?, ?)",
                rider, payment, session, Timestamp.from(CLOCK.instant()));
    }

    private void evaluated(UUID session, UUID rider, UUID trainer, int stars, String comment, Instant at) {
        jdbc.update("""
                        insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)
                        on conflict do nothing""", session, rider, trainer);
        jdbc.update("""
                        insert into session_evaluation (session_id, rider_id, trainer_id, stars, comment, created_at)
                        values (?, ?, ?, ?, ?, ?)""",
                session, rider, trainer, stars, comment, Timestamp.from(at));
    }

    // A one-hour session finished by that trainer at its end.
    private UUID finishedSession(int level, LocalDate date, LocalTime start, UUID finishedBy) {
        UUID id = session(level, date, start);
        Instant end = LocalDateTime.of(date, start.plusHours(1)).atZone(CAIRO).toInstant();
        jdbc.update("update session set status = 'finished', finished_at = ?, finished_by = ? where id = ?",
                Timestamp.from(end), finishedBy, id);
        return id;
    }

    // Saved through JPA, so the start time is stored the way the app reads it back. A null level is an
    // assessment slot.
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

    private static Instant cairo(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(CAIRO).toInstant();
    }
}
