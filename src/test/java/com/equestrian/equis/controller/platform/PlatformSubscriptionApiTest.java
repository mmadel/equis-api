package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.support.ClubThreads;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · subscriptions: the super admin lists plans, adds paid subscriptions (renewal or upgrade), voids wrong ones
// and reads a club's history. Club cairo-eq is created Sat 7 Nov 2026, 15:00 Cairo, with its trial to 7 Dec 15:00;
// the four plans come from ops/upsert-plan.sql.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(TrainerAccountsFixture.Config.class)
class PlatformSubscriptionApiTest {
    private static final Path UPSERT_PLAN = Path.of("ops", "upsert-plan.sql");
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    private static final Instant NOV_7 = cairo(2026, 11, 7, 15, 0);
    private static final Instant DEC_7 = cairo(2026, 12, 7, 15, 0);
    private static final Instant JAN_7 = cairo(2027, 1, 7, 15, 0);
    private static final Instant FEB_7 = cairo(2027, 2, 7, 15, 0);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
        TrainerAccountsFixture.adminAccount(registry);
    }

    @TestBean
    Clock clock;

    static Clock clock() {
        return new MutableClock(START);
    }

    @Autowired
    TrainerAccountsFixture fx;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JsonMapper json;

    private String superToken;
    private UUID club;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        asRoot(() -> {
            jdbc.update("delete from invite where club_id <> ?", Club.DEFAULT_ID);
            jdbc.update("delete from admin_account where club_id <> ?", Club.DEFAULT_ID);
            jdbc.update("delete from subscription");
            jdbc.update("delete from club where id <> ?", Club.DEFAULT_ID);
            jdbc.update("update club set status = 'ACTIVE', suspended_at = null, suspended_reason = null");
            jdbc.update("delete from plan");
            jdbc.update("delete from super_admin");
            return null;
        });
        upsertPlan("TRIAL", "Trial", "NULL", "NULL", "30", "BLOCK");
        upsertPlan("BASIC", "Basic", "2", "50", "NULL", "READ_ONLY");
        upsertPlan("PRO", "Pro", "10", "300", "NULL", "READ_ONLY");
        upsertPlan("MAX", "Max", "NULL", "NULL", "NULL", "READ_ONLY");
        jdbc.update("insert into super_admin (email, password_hash) values (?, ?)",
                SUPER_EMAIL, new BCryptPasswordEncoder().encode(SUPER_PASSWORD));
        at(NOV_7);
        String body = fx.perform(asSuperAdmin(post("/platform/clubs")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Cairo Equestrian", "code": "cairo-eq",
                                  "admin": { "name": "Hany", "email": "hany@cairoeq.com" } }"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        club = UUID.fromString(JsonPath.read(body, "$.id"));
    }

    // ---- add

    // 1
    @Test
    void aRenewalStartsWhenTheTrialEnds() throws Exception {
        at(cairo(2026, 11, 20, 10, 0));
        add(club, "basic", "MONTHLY", "AFTER_CURRENT", "Paid cash, receipt 114")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.planCode").value("BASIC"))
                .andExpect(jsonPath("$.subscription.planName").value("Basic"))
                .andExpect(jsonPath("$.subscription.billingPeriod").value("MONTHLY"))
                .andExpect(jsonPath("$.subscription.startsAt").value(DEC_7.toString()))
                .andExpect(jsonPath("$.subscription.endsAt").value(JAN_7.toString()))
                .andExpect(jsonPath("$.subscription.note").value("Paid cash, receipt 114"))
                .andExpect(jsonPath("$.subscription.status").value("UPCOMING"))
                .andExpect(jsonPath("$.subscription.voidedAt").value(nullValue()))
                .andExpect(jsonPath("$.access.state").value("ACTIVE"))
                .andExpect(jsonPath("$.access.reason").value(nullValue()))
                .andExpect(jsonPath("$.access.planCode").value("TRIAL"))
                .andExpect(jsonPath("$.access.endsAt").value(DEC_7.toString()));
        assertThat(asRoot(() -> jdbc.queryForObject(
                "select created_by from subscription where billing_period = 'MONTHLY'", UUID.class)))
                .isEqualTo(superAdminId());
    }

    // 2
    @Test
    void renewalsQueueUpOneAfterAnother() throws Exception {
        at(cairo(2026, 11, 20, 10, 0));
        add(club, "BASIC", "MONTHLY", "AFTER_CURRENT", null).andExpect(status().isCreated());
        add(club, "BASIC", "MONTHLY", "AFTER_CURRENT", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.startsAt").value(JAN_7.toString()))
                .andExpect(jsonPath("$.subscription.endsAt").value(FEB_7.toString()));
    }

    // 3
    @Test
    void anUpgradeNowIsRefusedWhileARenewalIsQueuedThenReplacesTheCurrentOne() throws Exception {
        UUID queued = twoBasicRenewals().get(1);
        at(cairo(2026, 12, 15, 10, 0));
        add(club, "PRO", "ANNUAL", "NOW", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("upcoming_subscription_exists"));

        voidIt(queued, "Paid twice by mistake").andExpect(status().isOk());
        add(club, "PRO", "ANNUAL", "NOW", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.startsAt").value(cairo(2026, 12, 15, 10, 0).toString()))
                .andExpect(jsonPath("$.subscription.endsAt").value(cairo(2027, 12, 15, 10, 0).toString()))
                .andExpect(jsonPath("$.subscription.status").value("CURRENT"))
                .andExpect(jsonPath("$.access.state").value("ACTIVE"))
                .andExpect(jsonPath("$.access.planCode").value("PRO"))
                .andExpect(jsonPath("$.access.billingPeriod").value("ANNUAL"));
        history(club)
                .andExpect(jsonPath("$[?(@.startsAt == '%s')].status".formatted(DEC_7)).value(contains("REPLACED")));
    }

    // 4
    @Test
    void voidingAnUpgradeMakesThePreviousSubscriptionCurrentAgain() throws Exception {
        UUID pro = upgradeToPro();
        at(cairo(2026, 12, 16, 9, 0));
        voidIt(pro, "Wrong plan, should be Basic")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.status").value("VOIDED"))
                .andExpect(jsonPath("$.subscription.voidReason").value("Wrong plan, should be Basic"))
                .andExpect(jsonPath("$.subscription.voidedAt").value(cairo(2026, 12, 16, 9, 0).toString()))
                .andExpect(jsonPath("$.access.state").value("ACTIVE"))
                .andExpect(jsonPath("$.access.planCode").value("BASIC"))
                .andExpect(jsonPath("$.access.endsAt").value(JAN_7.toString()));
        history(club)
                .andExpect(jsonPath("$[?(@.startsAt == '%s')].status".formatted(DEC_7)).value(contains("CURRENT")));
    }

    // 5
    @Test
    void aSubscriptionIsVoidedOnlyOnce() throws Exception {
        UUID pro = upgradeToPro();
        voidIt(pro, "Wrong plan, should be Basic").andExpect(status().isOk());
        voidIt(pro, "Wrong plan, should be Basic")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("subscription_already_voided"));
    }

    // 6
    @Test
    void theTrialCannotBeVoided() throws Exception {
        UUID trial = asRoot(() -> jdbc.queryForObject("select id from subscription where club_id = ?", UUID.class,
                club));
        voidIt(trial, "Club never paid")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("trial_cannot_be_voided"));
        assertThat(asRoot(() -> jdbc.queryForObject("select voided_at from subscription where id = ?",
                Timestamp.class, trial))).isNull();
    }

    // 7
    @Test
    void aVoidNeedsAReasonOfFiveCharacters() throws Exception {
        UUID basic = twoBasicRenewals().get(0);
        voidIt(basic, "oops ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
        voidIt(basic, "  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
    }

    // 8
    @Test
    void theTrialPlanAndTrialPeriodAreRefused() throws Exception {
        add(club, "TRIAL", "MONTHLY", "AFTER_CURRENT", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_billing_period"));
        add(club, "BASIC", "TRIAL", "AFTER_CURRENT", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_billing_period"));
        assertThat(asRoot(() -> jdbc.queryForObject("select count(*) from subscription", Integer.class)))
                .isEqualTo(1);
    }

    // 9
    @Test
    void unknownPlanClubAndSubscriptionAreNotFound() throws Exception {
        add(club, "GOLD", "MONTHLY", "NOW", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("plan_not_found"));
        UUID unknown = UUID.randomUUID();
        add(unknown, "BASIC", "MONTHLY", "NOW", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("club_not_found"));
        fx.perform(asSuperAdmin(get("/platform/clubs/" + unknown + "/subscriptions")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("club_not_found"));
        voidIt(unknown, "Not a real row")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("subscription_not_found"));
    }

    // 10
    @Test
    void aRenewalAfterExpiryStartsNow() throws Exception {
        asRoot(() -> jdbc.update("""
                        insert into subscription (club_id, plan_id, billing_period, starts_at, ends_at, created_by)
                        select ?, id, 'MONTHLY', ?, ?, ? from plan where code = 'BASIC'""",
                club, Timestamp.from(cairo(2027, 1, 1, 15, 0)), Timestamp.from(cairo(2027, 2, 1, 15, 0)),
                superAdminId()));
        Instant feb10 = cairo(2027, 2, 10, 11, 0);
        at(feb10);
        add(club, "BASIC", "MONTHLY", "AFTER_CURRENT", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.startsAt").value(feb10.toString()))
                .andExpect(jsonPath("$.subscription.endsAt").value(cairo(2027, 3, 10, 11, 0).toString()))
                .andExpect(jsonPath("$.access.state").value("ACTIVE"));
    }

    // 11
    @Test
    void endDatesFollowTheCalendarAtMonthEnds() throws Exception {
        at(cairo(2027, 1, 31, 10, 0));
        add(club, "BASIC", "MONTHLY", "AFTER_CURRENT", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.startsAt").value(cairo(2027, 1, 31, 10, 0).toString()))
                .andExpect(jsonPath("$.subscription.endsAt").value(cairo(2027, 2, 28, 10, 0).toString()));
        at(cairo(2028, 2, 29, 10, 0));
        add(club, "PRO", "ANNUAL", "AFTER_CURRENT", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.startsAt").value(cairo(2028, 2, 29, 10, 0).toString()))
                .andExpect(jsonPath("$.subscription.endsAt").value(cairo(2029, 2, 28, 10, 0).toString()));
    }

    // 12
    @Test
    void twoRenewalsAtOnceGetDifferentStarts() throws Exception {
        at(cairo(2026, 11, 20, 10, 0));
        ExecutorService pool = ClubThreads.pool(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    go.await();
                    return add(club, "BASIC", "MONTHLY", "AFTER_CURRENT", null)
                            .andExpect(status().isCreated())
                            .andReturn().getResponse().getContentAsString();
                }));
            }
            go.countDown();
            List<String> starts = new ArrayList<>();
            for (Future<String> result : results) {
                starts.add(JsonPath.read(result.get(30, TimeUnit.SECONDS), "$.subscription.startsAt"));
            }
            assertThat(starts).containsExactlyInAnyOrder(DEC_7.toString(), JAN_7.toString());
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- read

    // 13
    @Test
    void theHistoryIsNewestFirstWithEachRowsStatus() throws Exception {
        UUID pro = upgradeToPro();
        add(club, "BASIC", "ANNUAL", "AFTER_CURRENT", "Next year").andExpect(status().isCreated());
        history(club)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].status").value(contains("UPCOMING", "VOIDED", "CURRENT", "REPLACED",
                        "ENDED")))
                .andExpect(jsonPath("$[*].planCode").value(contains("BASIC", "BASIC", "PRO", "BASIC", "TRIAL")))
                .andExpect(jsonPath("$[0].startsAt").value(cairo(2027, 12, 15, 10, 0).toString()))
                .andExpect(jsonPath("$[0].note").value("Next year"))
                .andExpect(jsonPath("$[2].id").value(pro.toString()))
                .andExpect(jsonPath("$[1].startsAt").value(JAN_7.toString()))
                .andExpect(jsonPath("$[1].voidReason").value("Paid twice by mistake"))
                .andExpect(jsonPath("$[4].billingPeriod").value("TRIAL"))
                .andExpect(jsonPath("$[4].startsAt").value(NOV_7.toString()));
    }

    // 14
    @Test
    void thePlanPickerListsTheFourPlans() throws Exception {
        fx.perform(asSuperAdmin(get("/platform/plans")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(contains("BASIC", "MAX", "PRO", "TRIAL")))
                .andExpect(jsonPath("$[0].name").value("Basic"))
                .andExpect(jsonPath("$[0].maxTrainers").value(2))
                .andExpect(jsonPath("$[0].maxRiders").value(50))
                .andExpect(jsonPath("$[0].trialDays").value(nullValue()))
                .andExpect(jsonPath("$[0].onExpiry").value("READ_ONLY"))
                .andExpect(jsonPath("$[1].maxTrainers").value(nullValue()))
                .andExpect(jsonPath("$[3].trialDays").value(30))
                .andExpect(jsonPath("$[3].onExpiry").value("BLOCK"));
    }

    // ---- access

    // 15
    @Test
    void aClubAdminCannotReachSubscriptions() throws Exception {
        // The school admin must be able to sign in (a club without a subscription is blocked).
        asRoot(() -> jdbc.update("""
                        insert into subscription (club_id, plan_id, billing_period, starts_at, ends_at, created_by)
                        select ?, id, 'MONTHLY', ?, ?, ? from plan where code = 'BASIC'""",
                Club.DEFAULT_ID, Timestamp.from(cairo(2026, 11, 6, 15, 0)), Timestamp.from(DEC_7), superAdminId()));
        UUID trial = asRoot(() -> jdbc.queryForObject("select id from subscription where club_id = ?", UUID.class,
                club));
        fx.admin(get("/platform/plans")).andExpect(status().isForbidden());
        fx.admin(get("/platform/clubs/" + club + "/subscriptions")).andExpect(status().isForbidden());
        fx.admin(post("/platform/clubs/" + club + "/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content(addBody("BASIC", "MONTHLY", "NOW", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        fx.admin(post("/platform/subscriptions/" + trial + "/void").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"Not allowed\" }"))
                .andExpect(status().isForbidden());
        // Only the trial: nothing was added (the school's own row is left out).
        assertThat(asRoot(() -> jdbc.queryForObject("select count(*) from subscription where club_id = ?",
                Integer.class, club))).isEqualTo(1);
    }

    // 16
    @Test
    void aSuspendedClubCanStillGetASubscription() throws Exception {
        fx.perform(asSuperAdmin(put("/platform/clubs/" + club + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"SUSPENDED\", \"reason\": \"Late payment\" }"))
                .andExpect(status().isOk());
        at(cairo(2026, 11, 8, 10, 0));
        add(club, "BASIC", "MONTHLY", "NOW", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.status").value("CURRENT"))
                .andExpect(jsonPath("$.access.state").value("BLOCKED"))
                .andExpect(jsonPath("$.access.reason").value("SUSPENDED"))
                .andExpect(jsonPath("$.access.planCode").value("BASIC"));
    }

    // ---- helpers

    // Two Basic monthly renewals added on 20 Nov: 7 Dec → 7 Jan, then 7 Jan → 7 Feb. Returns their ids in order.
    private List<UUID> twoBasicRenewals() throws Exception {
        at(cairo(2026, 11, 20, 10, 0));
        UUID first = idOf(add(club, "BASIC", "MONTHLY", "AFTER_CURRENT", null).andExpect(status().isCreated()));
        UUID second = idOf(add(club, "BASIC", "MONTHLY", "AFTER_CURRENT", null).andExpect(status().isCreated()));
        return List.of(first, second);
    }

    // Case 3's journey: two renewals, the second voided, then Pro annual NOW on 15 Dec 10:00. Returns Pro's id.
    private UUID upgradeToPro() throws Exception {
        UUID queued = twoBasicRenewals().get(1);
        at(cairo(2026, 12, 15, 10, 0));
        voidIt(queued, "Paid twice by mistake").andExpect(status().isOk());
        return idOf(add(club, "PRO", "ANNUAL", "NOW", null).andExpect(status().isCreated()));
    }

    private ResultActions add(UUID clubId, String plan, String period, String startMode, String note)
            throws Exception {
        return fx.perform(asSuperAdmin(post("/platform/clubs/" + clubId + "/subscriptions"))
                .contentType(MediaType.APPLICATION_JSON).content(addBody(plan, period, startMode, note)));
    }

    private static String addBody(String plan, String period, String startMode, String note) {
        return """
                { "planCode": "%s", "billingPeriod": "%s", "startMode": "%s", "note": %s }"""
                .formatted(plan, period, startMode, note == null ? "null" : "\"" + note + "\"");
    }

    private ResultActions voidIt(UUID subscription, String reason) throws Exception {
        return fx.perform(asSuperAdmin(post("/platform/subscriptions/" + subscription + "/void"))
                .contentType(MediaType.APPLICATION_JSON).content("{ \"reason\": \"%s\" }".formatted(reason)));
    }

    private ResultActions history(UUID clubId) throws Exception {
        return fx.perform(asSuperAdmin(get("/platform/clubs/" + clubId + "/subscriptions")));
    }

    private static UUID idOf(ResultActions result) throws Exception {
        return UUID.fromString(JsonPath.read(result.andReturn().getResponse().getContentAsString(),
                "$.subscription.id"));
    }

    // Moves the clock and signs the super admin in again, so the token is valid at the new time.
    private void at(Instant now) throws Exception {
        ((MutableClock) clock).set(now);
        superToken = json.readValue(fx.perform(post("/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                                .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), AuthTokens.class).accessToken();
    }

    private MockHttpServletRequestBuilder asSuperAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken);
    }

    private UUID superAdminId() {
        return jdbc.queryForObject("select id from super_admin", UUID.class);
    }

    // Runs the real ops script with its psql variables filled in, as psql -v would.
    private void upsertPlan(String code, String name, String trainers, String riders, String trialDays,
                            String onExpiry) throws Exception {
        jdbc.execute(Files.readString(UPSERT_PLAN)
                .replace(":'code'", literal(code))
                .replace(":'name'", literal(name))
                .replace(":'on_expiry'", literal(onExpiry))
                .replace(":trainers", trainers)
                .replace(":riders", riders)
                .replace(":trial_days", trialDays));
    }

    private static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
    }

    // Other clubs' school rows are hidden from the test's default club; root mode sees every club.
    private static <T> T asRoot(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
