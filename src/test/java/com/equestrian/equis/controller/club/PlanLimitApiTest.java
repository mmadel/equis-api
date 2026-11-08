package com.equestrian.equis.controller.club;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import static com.equestrian.equis.support.TrainerAccountsFixture.TRAINERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · plan limits at intake and trainer creation, and GET /admin/club/usage. The default club ("school") is on
// Basic (2 trainers, 50 riders) from 10 days before START to 20 days after; the four plans come from
// ops/upsert-plan.sql. Ahmed is the club's one active trainer; people are added straight into the table, so only the
// request under test goes through the limit.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(TrainerAccountsFixture.Config.class)
class PlanLimitApiTest {
    private static final Path UPSERT_PLAN = Path.of("ops", "upsert-plan.sql");
    private static final String AHMED_PHONE = "01051000001";
    private static final String TRAINER_PASSWORD = "Trainer@2026";
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    // 20 days on the school calendar (Egypt moves its clocks back on 29 Oct, so not 20 x 24 hours).
    private static final Instant BASIC_ENDS = START.atZone(CAIRO).plusDays(20).toInstant();

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

    private UUID ahmed;
    private UUID slot;
    private int nextPerson;

    @BeforeEach
    void schoolOnBasic() throws Exception {
        fx.reset();
        asRoot(() -> {
            jdbc.update("delete from subscription");
            jdbc.update("delete from plan");
            jdbc.update("delete from super_admin");
            return null;
        });
        upsertPlan("TRIAL", "Trial", "NULL", "NULL", "30");
        upsertPlan("BASIC", "Basic", "2", "50", "NULL");
        upsertPlan("PRO", "Pro", "10", "300", "NULL");
        upsertPlan("MAX", "Max", "NULL", "NULL", "NULL");
        subscribe("BASIC", "MONTHLY", START.minus(Duration.ofDays(10)), BASIC_ENDS);
        ahmed = fx.activeTrainer("Ahmed", AHMED_PHONE, "ahmed@school.eg", TRAINER_PASSWORD).id();
        // Tomorrow's assessment slot: its 6 seats are more than any case books.
        slot = fx.addUpcomingSession(null);
        nextPerson = 0;
    }

    // ---- people

    // 1
    @Test
    void anIntakeUpToTheLimitIsAccepted() throws Exception {
        people(49);

        intake("Youssef").andExpect(status().isCreated());

        assertThat(notClosed()).isEqualTo(50);
    }

    // 2
    @Test
    void anIntakeOverTheLimitIsRefusedAndNothingIsSaved() throws Exception {
        people(50);

        intake("Nour")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.resource").value("RIDERS"))
                .andExpect(jsonPath("$.limit").value(50))
                .andExpect(jsonPath("$.used").value(50))
                .andExpect(jsonPath("$.detail").value("Your plan allows 50 riders and applicants."));

        assertThat(fx.count("member")).isEqualTo(50);
        assertThat(fx.count("assessment_booking")).isZero();
    }

    // 3
    @Test
    void closingAPersonMakesRoomForAnother() throws Exception {
        people(50);
        intake("Nour").andExpect(status().isConflict());

        jdbc.update("update member set closed_at = now() where id = (select min(id::text)::uuid from member)");

        intake("Nour").andExpect(status().isCreated());
    }

    // 4
    @Test
    void reopeningAPersonOverTheLimitIsRefused() throws Exception {
        people(50);
        UUID closed = closedPerson();

        fx.admin(post("/admin/members/" + closed + "/reopen"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.resource").value("RIDERS"));

        assertThat(jdbc.queryForObject("select closed_at is not null from member where id = ?", Boolean.class, closed))
                .isTrue();
    }

    // ---- trainers

    // 5
    @Test
    void inviteOverTheTrainerLimitIsRefused() throws Exception {
        fx.invite("Mona", "01051000002", "mona@school.eg");

        fx.inviteRequest("Karim", "01051000003", "karim@school.eg")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.resource").value("TRAINERS"))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.used").value(2))
                .andExpect(jsonPath("$.detail").value("Your plan allows 2 trainers."));

        assertThat(fx.count("trainer")).isEqualTo(2);
        assertThat(fx.recorder().all()).hasSize(1); // only Mona's invite
    }

    // 6
    @Test
    void aDisabledTrainerFreesAPlaceAndCantBeEnabledOnceItIsTaken() throws Exception {
        fx.invite("Mona", "01051000002", "mona@school.eg");
        fx.admin(post(TRAINERS + "/" + ahmed + "/disable")).andExpect(status().isOk());

        fx.inviteRequest("Karim", "01051000003", "karim@school.eg").andExpect(status().isCreated());
        fx.admin(post(TRAINERS + "/" + ahmed + "/enable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resource").value("TRAINERS"))
                .andExpect(jsonPath("$.used").value(2));

        assertThat(jdbc.queryForObject("select status from trainer where id = ?", String.class, ahmed))
                .isEqualTo("disabled");
    }

    // 7
    @Test
    void aTrainerRecordingAnAttendedAssessmentIsNeverStopped() throws Exception {
        people(49);
        UUID booking = fx.addUpcomingBooking(ahmed);
        // Tomorrow's slot (10:00) has started.
        fx.advance(Duration.ofHours(30));
        AuthTokens tokens = fx.loginTrainer(AHMED_PHONE, TRAINER_PASSWORD);
        intake("Nour").andExpect(jsonPath("$.code").value("plan_limit_reached")); // the club is at 50 / 50

        fx.perform(fx.asTrainer(post("/me/bookings/" + booking + "/result"), tokens)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"attended\": true, \"levelId\": 1 }"))
                .andExpect(status().is2xxSuccessful());
    }

    // ---- plan changes

    // 8
    @Test
    void anUpgradeAppliesAtOnce() throws Exception {
        people(50);
        fx.invite("Mona", "01051000002", "mona@school.eg");
        intake("Nour").andExpect(status().isConflict());

        subscribe("PRO", "MONTHLY", START, START.plus(Duration.ofDays(30)));

        intake("Nour").andExpect(status().isCreated());
        fx.inviteRequest("Karim", "01051000003", "karim@school.eg").andExpect(status().isCreated());
    }

    // 9
    @Test
    void aDowngradeKeepsEveryoneButStopsNewIntakes() throws Exception {
        subscribe("PRO", "MONTHLY", START.minus(Duration.ofDays(5)), START.plus(Duration.ofDays(25)));
        people(80);

        subscribe("BASIC", "MONTHLY", START, START.plus(Duration.ofDays(30)));

        assertThat(notClosed()).isEqualTo(80);
        intake("Nour")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.limit").value(50))
                .andExpect(jsonPath("$.used").value(80));
        assertThat(fx.count("member")).isEqualTo(80);
    }

    // 10
    @Test
    void theTrialAndMaxHaveNoLimit() throws Exception {
        subscribe("TRIAL", "TRIAL", START.minus(Duration.ofHours(1)), START.plus(Duration.ofDays(30)));
        people(500);
        intake("Nour").andExpect(status().isCreated());

        subscribe("MAX", "MONTHLY", START, START.plus(Duration.ofDays(30)));
        intake("Youssef").andExpect(status().isCreated());
        fx.invite("Mona", "01051000002", "mona@school.eg");
        fx.invite("Karim", "01051000003", "karim@school.eg");
    }

    // 11
    @Test
    void twoIntakesForTheLastPlaceAtTheSameMoment() throws Exception {
        people(49);
        MockHttpServletRequestBuilder first = fx.asAdmin(intakeRequest("Nour"));
        MockHttpServletRequestBuilder second = fx.asAdmin(intakeRequest("Youssef"));

        List<Integer> statuses = race(first, second);

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(notClosed()).isEqualTo(50);
    }

    // 12
    @Test
    void aLimitRaisedByTheOpsScriptAppliesOnTheNextRequest() throws Exception {
        people(50);
        intake("Nour").andExpect(status().isConflict());

        upsertPlan("BASIC", "Basic", "2", "51", "NULL");

        intake("Nour").andExpect(status().isCreated());
        intake("Youssef").andExpect(status().isConflict()).andExpect(jsonPath("$.limit").value(51));
    }

    // ---- usage

    // 13
    @Test
    void theAdminReadsTheClubUsage() throws Exception {
        people(42);
        closedPerson(); // not counted
        fx.invite("Mona", "01051000002", "mona@school.eg");

        fx.admin(get("/admin/club/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("BASIC"))
                .andExpect(jsonPath("$.planName").value("Basic"))
                .andExpect(jsonPath("$.billingPeriod").value("MONTHLY"))
                .andExpect(jsonPath("$.endsAt").value(BASIC_ENDS.toString()))
                .andExpect(jsonPath("$.daysLeft").value(20))
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.trainers.used").value(2))
                .andExpect(jsonPath("$.trainers.limit").value(2))
                .andExpect(jsonPath("$.riders.used").value(42))
                .andExpect(jsonPath("$.riders.limit").value(50));
    }

    // 14
    @Test
    void onlyTheClubAdminReadsTheUsage() throws Exception {
        AuthTokens trainer = fx.loginTrainer(AHMED_PHONE, TRAINER_PASSWORD);
        fx.perform(fx.asTrainer(get("/admin/club/usage"), trainer)).andExpect(status().isForbidden());

        jdbc.update("insert into super_admin (email, password_hash) values (?, ?)",
                SUPER_EMAIL, new BCryptPasswordEncoder().encode(SUPER_PASSWORD));
        String body = fx.perform(post("/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                                .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String superToken = JsonPath.read(body, "$.accessToken");
        fx.perform(get("/admin/club/usage").header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken))
                .andExpect(status().isForbidden());
    }

    // 15
    @Test
    void onMaxBothLimitsAreUnlimited() throws Exception {
        subscribe("MAX", "ANNUAL", START, START.plus(Duration.ofDays(365)));
        people(3);

        fx.admin(get("/admin/club/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("MAX"))
                .andExpect(jsonPath("$.trainers.used").value(1))
                .andExpect(jsonPath("$.trainers.limit").value(nullValue()))
                .andExpect(jsonPath("$.riders.used").value(3))
                .andExpect(jsonPath("$.riders.limit").value(nullValue()));
    }

    // ---- helpers

    private ResultActions intake(String name) throws Exception {
        return fx.admin(intakeRequest(name));
    }

    // W2's New person, booked into tomorrow's slot with Ahmed. Each call gets its own phone and email.
    private MockHttpServletRequestBuilder intakeRequest(String name) {
        int n = ++nextPerson;
        return post("/admin/members").contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "%s", "phone": "0109%07d", "email": "intake%d@mail.eg", "slotId": "%s", "assessorId": "%s" }"""
                .formatted(name, n, n, slot, ahmed));
    }

    // n people not closed, straight into the table (they need no booking to count).
    private void people(int n) {
        jdbc.update("""
                insert into member (name, phone, email)
                select 'Person ' || g, '0122' || lpad(g::text, 7, '0'), 'person' || g || '@mail.eg'
                from generate_series((select count(*) from member) + 1, (select count(*) from member) + ?) g""", n);
    }

    // A closed person with an assessment booking, as close and reopen expect.
    private UUID closedPerson() {
        UUID booking = fx.addUpcomingBooking(ahmed);
        UUID member = jdbc.queryForObject("select member_id from assessment_booking where id = ?", UUID.class, booking);
        jdbc.update("update member set closed_at = now() where id = ?", member);
        return member;
    }

    private int notClosed() {
        return jdbc.queryForObject("select count(*) from member where closed_at is null", Integer.class);
    }

    private void subscribe(String plan, String period, Instant startsAt, Instant endsAt) {
        jdbc.update("""
                insert into subscription (club_id, plan_id, billing_period, starts_at, ends_at)
                select ?, id, ?, ?, ? from plan where code = ?""",
                Club.DEFAULT_ID, period, Timestamp.from(startsAt), Timestamp.from(endsAt), plan);
    }

    // Starts both requests at once and returns their statuses.
    private List<Integer> race(MockHttpServletRequestBuilder first, MockHttpServletRequestBuilder second)
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = ClubThreads.pool(2);
        try {
            List<Future<MvcResult>> results = new ArrayList<>();
            for (MockHttpServletRequestBuilder request : List.of(first, second)) {
                results.add(pool.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return fx.perform(request).andReturn();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<MvcResult> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS).getResponse().getStatus());
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    // Runs the real ops script with its psql variables filled in, as psql -v would.
    private void upsertPlan(String code, String name, String trainers, String riders, String trialDays)
            throws Exception {
        String onExpiry = trialDays.equals("NULL") ? "READ_ONLY" : "BLOCK";
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

    // Plan and subscription have no row-level security; root mode is only needed to see every club's rows.
    private static <T> T asRoot(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
