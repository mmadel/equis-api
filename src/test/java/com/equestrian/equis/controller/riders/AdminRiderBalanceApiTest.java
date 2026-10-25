package com.equestrian.equis.controller.riders;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The balance on W3 (GET /admin/riders, PATCH /admin/riders/trainer) and a rider's payment history
// (GET /admin/riders/{id}/payments), on Tue 3 Nov 2026 at 10:15 school time. Rows go in with SQL.
// The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=riders-balance-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
public class AdminRiderBalanceApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final Instant TUESDAY = ZonedDateTime.of(2026, 11, 3, 10, 15, 0, 0, CAIRO).toInstant();
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

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
    private UUID mona;
    private UUID sara;
    private UUID omar;
    private UUID nour;
    private UUID saraNov;
    private UUID nourDec;

    // Sara (level 3) has a November package, Omar (level 3) none, Nour (level 2) only a December one.
    @BeforeEach
    void setUp() {
        CLOCK.set(TUESDAY);
        // Also drops the seeded trainers and sample riders, and with them every payment.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        // recorded_by points at admin_account, so the token carries the seeded admin's id.
        admin = jdbc.queryForObject("select id from admin_account where email = 'riders-balance-admin@example.com'",
                UUID.class);
        ahmed = trainer("Ahmed Said", "01999990001");
        mona = trainer("Mona Adel", "01999990002");
        sara = rider("Sara Ali", 3, ahmed);
        omar = rider("Omar Hassan", 3, ahmed);
        nour = rider("Nour Seed", 2, ahmed);
        saraNov = payment(sara, NOV, 1200, TUESDAY.minusSeconds(3600));
        nourDec = payment(nour, DEC, 1200, TUESDAY.minusSeconds(3600));
    }

    // Case 1
    @Test
    void eachRowCarriesTodaysBalance() throws Exception {
        list(get("/admin/riders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(row("Sara Ali", "balance.sessionsLeft"), contains(8)))
                .andExpect(jsonPath(row("Sara Ali", "balance.status"), contains("active")))
                .andExpect(jsonPath(row("Sara Ali", "balance.nextExpiry"), contains("2026-12-31")))
                .andExpect(jsonPath(row("Omar Hassan", "balance.sessionsLeft"), contains(0)))
                .andExpect(jsonPath(row("Omar Hassan", "balance.status"), contains("awaiting_payment")))
                .andExpect(jsonPath(row("Omar Hassan", "balance.nextExpiry"), contains(nullValue())))
                .andExpect(jsonPath(row("Nour Seed", "balance.sessionsLeft"), contains(0)))
                .andExpect(jsonPath(row("Nour Seed", "balance.status"), contains("awaiting_payment")))
                .andExpect(jsonPath(row("Nour Seed", "balance.nextExpiry"), contains(nullValue())));
    }

    // Case 2
    @Test
    void theAwaitingPaymentTotalCountsEveryRider() throws Exception {
        list(get("/admin/riders"))
                .andExpect(jsonPath("$.totals.awaitingPayment").value(2));
        list(get("/admin/riders").param("level", "3"))
                .andExpect(jsonPath("$.riders.length()").value(2))
                .andExpect(jsonPath("$.totals.awaitingPayment").value(2));
    }

    // Case 3
    @Test
    void thePaymentFilter() throws Exception {
        list(get("/admin/riders").param("payment", "awaiting_payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[*].name", containsInAnyOrder("Omar Hassan", "Nour Seed")));
        list(get("/admin/riders").param("payment", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[*].name", contains("Sara Ali")));
        list(get("/admin/riders").param("payment", "paid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("payment"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("invalid_payment_status"));
    }

    // Case 4
    @Test
    void thePaymentFilterAddsToTheOthers() throws Exception {
        list(get("/admin/riders").param("payment", "awaiting_payment").param("level", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[*].name", contains("Omar Hassan")));
    }

    // Case 5
    @Test
    void theReassignRowsCarryTheBalance() throws Exception {
        list(patch("/admin/riders/trainer").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"riderIds\": [\"%s\"], \"trainerId\": \"%s\" }".formatted(sara, mona)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[0].trainer.id").value(mona.toString()))
                .andExpect(jsonPath("$.riders[0].balance.sessionsLeft").value(8))
                .andExpect(jsonPath("$.riders[0].balance.status").value("active"))
                .andExpect(jsonPath("$.riders[0].balance.nextExpiry").value("2026-12-31"));
    }

    // Case 7
    @Test
    void historyIsNewestFirstWithTheVoidedOneMarked() throws Exception {
        UUID typo = payment(sara, NOV, 12000, TUESDAY.minusSeconds(7200));
        jdbc.update("""
                update payment set voided_at = ?, voided_by = ?, void_reason = 'Typo in amount, should be 1,200'
                where id = ?""", Timestamp.from(TUESDAY.minusSeconds(7000)), admin, typo);

        list(get("/admin/riders/" + sara + "/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderId").value(sara.toString()))
                .andExpect(jsonPath("$.riderName").value("Sara Ali"))
                .andExpect(jsonPath("$.balance.sessionsLeft").value(8))
                .andExpect(jsonPath("$.balance.status").value("active"))
                .andExpect(jsonPath("$.payments[*].id", contains(saraNov.toString(), typo.toString())))
                .andExpect(jsonPath("$.payments[0].state").value("active"))
                .andExpect(jsonPath("$.payments[0].used").value(0))
                .andExpect(jsonPath("$.payments[0].left").value(8))
                .andExpect(jsonPath("$.payments[1].state").value("voided"))
                .andExpect(jsonPath("$.payments[1].amountEgp").value(12000.0))
                .andExpect(jsonPath("$.payments[1].left").value(0))
                .andExpect(jsonPath("$.payments[1].voidReason").value("Typo in amount, should be 1,200"));
    }

    // Case 8
    @Test
    void aFuturePackageIsUpcomingInTheHistory() throws Exception {
        list(get("/admin/riders/" + nour + "/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[*].id", contains(nourDec.toString())))
                .andExpect(jsonPath("$.payments[0].state").value("upcoming"))
                .andExpect(jsonPath("$.payments[0].left").value(8))
                .andExpect(jsonPath("$.balance.sessionsLeft").value(0))
                .andExpect(jsonPath("$.balance.status").value("awaiting_payment"));
    }

    // Case 9
    @Test
    void aRiderWithNoPaymentsHasAnEmptyHistory() throws Exception {
        list(get("/admin/riders/" + omar + "/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderName").value("Omar Hassan"))
                .andExpect(jsonPath("$.payments").isEmpty())
                .andExpect(jsonPath("$.balance.sessionsLeft").value(0))
                .andExpect(jsonPath("$.balance.status").value("awaiting_payment"))
                .andExpect(jsonPath("$.balance.nextExpiry").value(nullValue()));
    }

    // Case 10
    @Test
    void anUnknownRidersHistoryIsNotFound() throws Exception {
        list(get("/admin/riders/" + UUID.randomUUID() + "/payments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
    }

    // Case 12
    @Test
    void aTrainerGetsNeither() throws Exception {
        for (String path : new String[]{"/admin/riders", "/admin/riders/" + sara + "/payments"}) {
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(ahmed, Role.TRAINER))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
    }

    private static String row(String riderName, String field) {
        return "$.riders[?(@.name == '" + riderName + "')]." + field;
    }

    private ResultActions list(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(admin, Role.ADMIN))));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private UUID trainer(String name, String phone) {
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, 'active', 'not-a-real-hash') returning id""",
                UUID.class, name, phone, phone + "@school.eg");
    }

    private UUID rider(String name, int level, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, level, trainer);
    }

    // One month, 8 sessions, valid to the end of the grace month as the CHECK wants it.
    private UUID payment(UUID riderId, YearMonth start, int amount, Instant recordedAt) {
        return jdbc.queryForObject("""
                        insert into payment (rider_id, amount_egp, method, start_month, months, sessions, valid_until,
                                             recorded_at, recorded_by)
                        values (?, ?, 'cash', ?, 1, 8, ?, ?, ?) returning id""",
                UUID.class, riderId, amount, start.atDay(1), start.plusMonths(1).atEndOfMonth(),
                Timestamp.from(recordedAt), admin);
    }
}
