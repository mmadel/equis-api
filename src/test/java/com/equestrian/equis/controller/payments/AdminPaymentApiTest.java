package com.equestrian.equis.controller.payments;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.support.MutableClock;
import com.jayway.jsonpath.JsonPath;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /admin/riders/{id}/payments (W3) over HTTP against a real database, on Tue 3 Nov 2026 at 10:15 school time.
// Riders go in with SQL. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=payments-api-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
public class AdminPaymentApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final Instant TUESDAY = ZonedDateTime.of(2026, 11, 3, 10, 15, 0, 0, CAIRO).toInstant();

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
        // Also drops the seeded trainers and sample riders, and with them every payment.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        // recorded_by points at admin_account, so the token carries the seeded admin's id.
        admin = jdbc.queryForObject("select id from admin_account where email = 'payments-api-admin@example.com'",
                UUID.class);
        ahmed = jdbc.queryForObject("""
                insert into trainer (name, phone, email, status, password_hash)
                values ('Ahmed Said', '01999990001', 'ahmed@school.eg', 'active', 'not-a-real-hash') returning id""",
                UUID.class);
        sara = rider("Sara Ali", ahmed);
    }

    // Case 1
    @Test
    void oneMonthWithoutSessionsGivesEightValidThroughTheGraceMonth() throws Exception {
        record(sara, "{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 1 }")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.riderId").value(sara.toString()))
                .andExpect(jsonPath("$.payment.method").value("cash"))
                .andExpect(jsonPath("$.payment.months").value(1))
                .andExpect(jsonPath("$.payment.sessions").value(8))
                .andExpect(jsonPath("$.payment.validUntil").value("2026-12-31"))
                .andExpect(jsonPath("$.payment.used").value(0))
                .andExpect(jsonPath("$.payment.usages", empty()))
                .andExpect(jsonPath("$.payment.left").value(8))
                .andExpect(jsonPath("$.payment.state").value("active"))
                .andExpect(jsonPath("$.balance.sessionsLeft").value(8))
                .andExpect(jsonPath("$.balance.status").value("active"))
                .andExpect(jsonPath("$.balance.nextExpiry").value("2026-12-31"));
        assertThat(count()).isEqualTo(1);
    }

    // Case 2
    @Test
    void sessionsSentAreUsedAsTheyAre() throws Exception {
        record(sara, body("2026-11", 2, 12))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.sessions").value(12))
                .andExpect(jsonPath("$.payment.left").value(12))
                .andExpect(jsonPath("$.payment.validUntil").value("2027-01-31"));
    }

    // Case 3
    @Test
    void theStartMonthRunsFromLastMonthToTwelveMonthsAhead() throws Exception {
        record(sara, body("2026-10", 1, null)).andExpect(status().isCreated());
        record(sara, body("2027-11", 1, null)).andExpect(status().isCreated());
        for (String month : new String[]{"2026-09", "2027-12"}) {
            record(sara, body(month, 1, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("startMonth"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("start_month_out_of_range"));
        }
        assertThat(count()).isEqualTo(2);
    }

    // Case 4
    @Test
    void aFuturePackageIsUpcomingAndLeavesTheRiderAwaitingPayment() throws Exception {
        record(sara, body("2026-12", 1, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.state").value("upcoming"))
                .andExpect(jsonPath("$.payment.left").value(8))
                .andExpect(jsonPath("$.balance.sessionsLeft").value(0))
                .andExpect(jsonPath("$.balance.status").value("awaiting_payment"))
                .andExpect(jsonPath("$.balance.nextExpiry").value(org.hamcrest.Matchers.nullValue()));
    }

    // Case 5
    @Test
    void aSecondPaymentAddsUpAndTheFirstStillExpiresNext() throws Exception {
        record(sara, body("2026-11", 1, null)).andExpect(status().isCreated());

        record(sara, body("2026-11", 2, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.validUntil").value("2027-01-31"))
                .andExpect(jsonPath("$.balance.sessionsLeft").value(24))
                .andExpect(jsonPath("$.balance.nextExpiry").value("2026-12-31"));
    }

    // Case 5, as the ticket words it: two one-month packages give 16.
    @Test
    void twoEightSessionPackagesGiveSixteen() throws Exception {
        record(sara, body("2026-11", 1, null)).andExpect(status().isCreated());

        record(sara, body("2026-12", 1, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance.sessionsLeft").value(8));
        record(sara, body("2026-11", 1, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance.sessionsLeft").value(16))
                .andExpect(jsonPath("$.balance.nextExpiry").value("2026-12-31"));
    }

    // Case 6
    @Test
    void everyBadValueGetsItsField() throws Exception {
        bad("{ \"amountEgp\": 0, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 1 }",
                "amountEgp", "invalid_amount");
        bad("{ \"amountEgp\": -5, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 1 }",
                "amountEgp", "invalid_amount");
        bad("{ \"amountEgp\": 12.345, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 1 }",
                "amountEgp", "invalid_amount");
        bad("{ \"amountEgp\": 1200, \"method\": \"paypal\", \"startMonth\": \"2026-11\", \"months\": 1 }",
                "method", "invalid_method");
        bad("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 0 }",
                "months", null);
        bad("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 13 }",
                "months", null);
        bad("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 1, \"sessions\": 0 }",
                "sessions", null);
        bad("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 1, \"sessions\": 201 }",
                "sessions", null);
        bad("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"Nov\", \"months\": 1 }",
                "startMonth", "invalid_value");
        assertThat(count()).isZero();
    }

    // Case 7
    @Test
    void anUnknownRiderIsNotFound() throws Exception {
        record(UUID.randomUUID(), body("2026-11", 1, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
    }

    // Case 8
    @Test
    void aRiderWithoutATrainerCanBePaidFor() throws Exception {
        UUID omar = rider("Omar Hassan", null);

        record(omar, body("2026-11", 1, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.riderId").value(omar.toString()));
    }

    // Case 9
    @Test
    void theAdminIsStoredAsRecordedBy() throws Exception {
        String id = JsonPath.read(record(sara, body("2026-11", 1, null))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.payment.id");

        assertThat(jdbc.queryForObject("select recorded_by from payment where id = ?::uuid", UUID.class, id))
                .isEqualTo(admin);
    }

    // Case 10
    @Test
    void theJsonShape() throws Exception {
        String json = record(sara, body("2026-11", 1, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.amountEgp").isNumber())
                .andExpect(jsonPath("$.payment.amountEgp").value(1200.0))
                .andExpect(jsonPath("$.payment.startMonth").value("2026-11"))
                .andExpect(jsonPath("$.payment.validUntil").value("2026-12-31"))
                .andExpect(jsonPath("$.payment.recordedAt").value("2026-11-03T08:15:00Z"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> payment = JsonPath.read(json, "$.payment");
        assertThat(payment).containsEntry("voidedAt", null).containsEntry("voidReason", null);
        assertThat(payment).containsOnlyKeys("id", "riderId", "amountEgp", "method", "startMonth", "months",
                "sessions", "validUntil", "used", "left", "state", "recordedAt", "voidedAt", "voidReason", "usages");
        Map<String, Object> balance = JsonPath.read(json, "$.balance");
        assertThat(balance).containsOnlyKeys("sessionsLeft", "status", "nextExpiry");
    }

    // Case 12
    @Test
    void onlyTheAdminCanRecord() throws Exception {
        mvc.perform(post(path(sara)).contentType(MediaType.APPLICATION_JSON).content(body("2026-11", 1, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post(path(sara)).contentType(MediaType.APPLICATION_JSON).content(body("2026-11", 1, null))
                        .header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(ahmed, Role.TRAINER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        assertThat(count()).isZero();
    }

    private void bad(String json, String field, String message) throws Exception {
        ResultActions result = record(sara, json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
        if (message != null) {
            result.andExpect(jsonPath("$.fieldErrors[0].message").value(message));
        }
    }

    private ResultActions record(UUID riderId, String json) throws Exception {
        return admin(post(path(riderId)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String path(UUID riderId) {
        return "/admin/riders/" + riderId + "/payments";
    }

    private static String body(String startMonth, int months, Integer sessions) {
        return "{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"%s\", \"months\": %d%s }"
                .formatted(startMonth, months, sessions == null ? "" : ", \"sessions\": " + sessions);
    }

    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(admin, Role.ADMIN))));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private int count() {
        return jdbc.queryForObject("select count(*) from payment", Integer.class);
    }

    private UUID rider(String name, UUID trainer) {
        return jdbc.queryForObject("insert into rider (name, level_id, trainer_id) values (?, 2, ?) returning id",
                UUID.class, name, trainer);
    }
}
