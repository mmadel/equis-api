package com.equestrian.equis.controller.payments;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
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

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /admin/payments/{id}/void (W3) over HTTP against a real database, on Tue 3 Nov 2026 at 10:15 school time.
// Payments and usage go in with SQL (no endpoint writes usage yet). Not @Transactional, so the race case commits.
// The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=payments-void-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
public class AdminPaymentVoidApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final Instant TUESDAY = ZonedDateTime.of(2026, 11, 3, 10, 15, 0, 0, CAIRO).toInstant();
    private static final YearMonth NOV = YearMonth.of(2026, 11);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    static final MutableClock CLOCK = new MutableClock(TUESDAY);

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

    private UUID admin;
    private UUID ahmed;
    private UUID sara;

    @BeforeEach
    void setUp() {
        CLOCK.set(TUESDAY);
        // Also drops the seeded trainers and sample riders, and with them every payment and usage.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        // voided_by points at admin_account, so the token carries the seeded admin's id.
        admin = jdbc.queryForObject("select id from admin_account where email = 'payments-void-admin@example.com'",
                UUID.class);
        ahmed = jdbc.queryForObject("""
                insert into trainer (name, phone, email, status, password_hash)
                values ('Ahmed Said', '01999990001', 'ahmed@school.eg', 'active', 'not-a-real-hash') returning id""",
                UUID.class);
        sara = jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values ('Sara Ali', 2, ?) returning id",
                UUID.class, ahmed);
    }

    // Case 1
    @Test
    void voidingTheOnlyPackageLeavesTheRiderAwaitingPayment() throws Exception {
        UUID p = payment(NOV, 1, 8);

        voidIt(p, "  Typo in amount, should be 1,200  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.id").value(p.toString()))
                .andExpect(jsonPath("$.payment.state").value("voided"))
                .andExpect(jsonPath("$.payment.voidReason").value("Typo in amount, should be 1,200"))
                .andExpect(jsonPath("$.payment.voidedAt").value("2026-11-03T08:15:00Z"))
                .andExpect(jsonPath("$.payment.used").value(0))
                .andExpect(jsonPath("$.payment.usages", empty()))
                .andExpect(jsonPath("$.payment.left").value(0))
                .andExpect(jsonPath("$.balance.sessionsLeft").value(0))
                .andExpect(jsonPath("$.balance.status").value("awaiting_payment"))
                .andExpect(jsonPath("$.balance.nextExpiry").value(nullValue()));
        assertThat(jdbc.queryForObject("select void_reason from payment where id = ?", String.class, p))
                .isEqualTo("Typo in amount, should be 1,200");
    }

    // Case 2
    @Test
    void thenTheRightPaymentMakesTheRiderActiveAgain() throws Exception {
        UUID wrong = payment(NOV, 1, 8);
        voidIt(wrong, "Typo in amount").andExpect(status().isOk());

        admin(post("/admin/riders/" + sara + "/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 1 }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance.sessionsLeft").value(8))
                .andExpect(jsonPath("$.balance.status").value("active"));
        assertThat(jdbc.queryForObject("select count(*) from payment where rider_id = ?", Integer.class, sara))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from payment where voided_at is not null", Integer.class))
                .isEqualTo(1);
    }

    // Case 3
    @Test
    void voidingOneOfTwoLeavesTheOthersSessions() throws Exception {
        UUID first = payment(NOV, 1, 8);
        payment(NOV, 2, 16);

        voidIt(first, "Recorded twice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance.sessionsLeft").value(16))
                .andExpect(jsonPath("$.balance.status").value("active"))
                .andExpect(jsonPath("$.balance.nextExpiry").value("2027-01-31"));
    }

    // Case 4
    @Test
    void voidingTwiceIsRefused() throws Exception {
        UUID p = payment(NOV, 1, 8);
        voidIt(p, "Wrong rider").andExpect(status().isOk());

        voidIt(p, "Again")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("payment_already_voided"));
        assertThat(jdbc.queryForObject("select void_reason from payment where id = ?", String.class, p))
                .isEqualTo("Wrong rider");
    }

    // Case 5
    @Test
    void aPackageWithUsageIsRefused() throws Exception {
        UUID p = payment(NOV, 1, 8);
        use(p, LocalDate.of(2026, 11, 1));
        use(p, LocalDate.of(2026, 11, 2));

        voidIt(p, "Typo")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("payment_has_usage"))
                .andExpect(jsonPath("$.details.used").value(2))
                .andExpect(jsonPath("$.detail").value("2 sessions were already used from this payment."));
        assertThat(jdbc.queryForObject("select voided_at from payment where id = ?", Timestamp.class, p)).isNull();
    }

    // Case 6
    @Test
    void expiredAndUpcomingPackagesCanBeVoided() throws Exception {
        UUID expired = payment(YearMonth.of(2026, 8), 1, 8);
        UUID upcoming = payment(YearMonth.of(2026, 12), 1, 8);

        voidIt(expired, "Old typo").andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.state").value("voided"));
        voidIt(upcoming, "Paid by mistake").andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.state").value("voided"));
    }

    // Case 7
    @Test
    void theReasonIsRequiredAndAtMostThreeHundred() throws Exception {
        UUID p = payment(NOV, 1, 8);

        badReason(p, "{ \"reason\": \"   \" }", "reason_required");
        badReason(p, "{}", "reason_required");
        badReason(p, "{ \"reason\": \"" + "x".repeat(301) + "\" }", "reason_too_long");
        voidIt(p, "x".repeat(300)).andExpect(status().isOk());
    }

    // Case 8
    @Test
    void anUnknownPaymentIsNotFound() throws Exception {
        voidIt(UUID.randomUUID(), "Typo")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("payment_not_found"));
    }

    // Case 9
    @Test
    void theAdminIsStoredAsVoidedBy() throws Exception {
        UUID p = payment(NOV, 1, 8);

        voidIt(p, "Typo").andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select voided_by from payment where id = ?", UUID.class, p)).isEqualTo(admin);
    }

    // Case 10
    @Test
    void twoVoidsAtOnceGiveOneSuccessAndOneConflict() throws Exception {
        UUID p = payment(NOV, 1, 8);

        List<Integer> statuses = race(() -> voidIt(p, "First").andReturn(), () -> voidIt(p, "Second").andReturn());

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(jdbc.queryForObject("select count(*) from payment where voided_at is not null", Integer.class))
                .isEqualTo(1);
    }

    // Case 11
    @Test
    void onlyTheAdminCanVoid() throws Exception {
        UUID p = payment(NOV, 1, 8);
        String body = "{ \"reason\": \"Typo\" }";

        mvc.perform(post(path(p)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post(path(p)).contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(ahmed, Role.TRAINER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        assertThat(jdbc.queryForObject("select voided_at from payment where id = ?", Timestamp.class, p)).isNull();
    }

    private void badReason(UUID paymentId, String json, String message) throws Exception {
        admin(post(path(paymentId)).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(message));
        assertThat(jdbc.queryForObject("select voided_at from payment where id = ?", Timestamp.class, paymentId))
                .isNull();
    }

    private ResultActions voidIt(UUID paymentId, String reason) throws Exception {
        return admin(post(path(paymentId)).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"reason\": \"" + reason + "\" }"));
    }

    private static String path(UUID paymentId) {
        return "/admin/payments/" + paymentId + "/void";
    }

    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(admin, Role.ADMIN))));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    // valid_until is the last day of the grace month, as the CHECK wants it.
    private UUID payment(YearMonth start, int months, int sessions) {
        return jdbc.queryForObject("""
                        insert into payment (rider_id, amount_egp, method, start_month, months, sessions, valid_until,
                                             recorded_at, recorded_by)
                        values (?, 1200.00, 'cash', ?, ?, ?, ?, ?, ?) returning id""",
                UUID.class, sara, start.atDay(1), months, sessions, start.plusMonths(months).atEndOfMonth(),
                Timestamp.from(TUESDAY.minusSeconds(60)), admin);
    }

    // One session Sara rode on that date, paid from this package.
    private void use(UUID paymentId, LocalDate date) {
        LocalTime start = LocalTime.of(17, 0);
        UUID template = jdbc.queryForObject("""
                        insert into session_template (level_id, day_of_week, start_time, end_time, capacity, valid_from)
                        values (2, ?, ?, ?, 6, ?) returning id""",
                UUID.class, date.getDayOfWeek().getValue(), start, start.plusHours(1), date);
        UUID session = jdbc.queryForObject("""
                        insert into session (template_id, session_date, start_time, end_time, capacity)
                        values (?, ?, ?, ?, 6) returning id""",
                UUID.class, template, date, start, start.plusHours(1));
        jdbc.update("insert into session_usage (rider_id, payment_id, session_id) values (?, ?, ?)",
                sara, paymentId, session);
    }

    private List<Integer> race(Request a, Request b) throws Exception {
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
            return List.of(first.get(30, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(30, TimeUnit.SECONDS).getResponse().getStatus());
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface Request {
        MvcResult send() throws Exception;
    }
}
