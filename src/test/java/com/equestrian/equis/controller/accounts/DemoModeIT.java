package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.AccountLinkService;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E23 demo mode (mail off): every invite, resend and admin reset of decision 7 gives an active account that signs in
// with the demo password, with no link and no email; resets sign the person out everywhere. Numbers are the ticket's
// test cases. Mail off needs its own properties, so this class has a context of its own.
// The default club ("school") is on Basic (2 trainers) so case 6 can reach the limit.
@TestPropertySource(properties = {"app.mail.enabled=false", "app.mail.demo-password=" + DemoModeIT.DEMO})
class DemoModeIT extends IntegrationTestBase {
    static final String DEMO = "Demo-2026!";
    private static final Instant NOV_22 = ZonedDateTime.of(2026, 11, 22, 10, 0, 0, 0, CAIRO).toInstant();
    private static final Path UPSERT_PLAN = Path.of("ops", "upsert-plan.sql");
    private static final String OMAR_EMAIL = "trainer1@demo.equis";
    private static final String OMAR_PHONE = "01060000001";
    private static final String RIDERS = "/admin/riders";
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    // From 10:00 to 16:00 the next day, well after tomorrow's 10:00 assessment slot (see RiderAccountJourneyIT).
    private static final Duration AFTER_SLOT = Duration.ofHours(30);

    @Autowired
    TrainerAccountsFixture fx;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JsonMapper json;
    @Autowired
    AccountLinkService links;
    @Autowired
    TenantTransactions tenants;
    @Autowired
    TrainerRepository trainers;
    @Autowired
    RiderRepository riders;
    @Autowired
    PasswordEncoder passwordEncoder;

    // Every response body of the class, for case 15.
    private final List<String> bodies = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        CLOCK.set(NOV_22);
        asRoot(() -> {
            jdbc.update("delete from subscription");
            jdbc.update("delete from plan");
            jdbc.update("delete from super_admin");
            return null;
        });
        upsertPlan("TRIAL", "Trial", "NULL", "NULL", "30", "BLOCK");
        upsertPlan("BASIC", "Basic", "2", "50", "NULL", "READ_ONLY");
        jdbc.update("""
                        insert into subscription (club_id, plan_id, billing_period, starts_at, ends_at)
                        select ?, id, 'MONTHLY', ?, ? from plan where code = 'BASIC'""",
                Club.DEFAULT_ID, Timestamp.from(NOV_22.minus(Duration.ofDays(10))),
                Timestamp.from(NOV_22.plus(Duration.ofDays(20))));
        bodies.clear();
    }

    // 15: no answer of cases 5–13 carries the demo password.
    @AfterEach
    void noResponseContainsTheDemoPassword() {
        assertThat(bodies).isNotEmpty().allSatisfy(body -> assertThat(body).doesNotContain(DEMO));
    }

    // 5
    @Test
    void anInvitedTrainerIsActiveAtOnceAndSignsInWithTheDemoPassword() throws Exception {
        record(fx.inviteRequest("Coach Omar", OMAR_PHONE, OMAR_EMAIL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("active"));

        UUID omar = trainerId(OMAR_EMAIL);
        assertThat(invitesOfTrainer(omar)).isZero();
        assertThat(fx.recorder().all()).isEmpty();
        record(fx.login(OMAR_EMAIL, DEMO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TRAINER"));
        assertThat(fx.total("active")).isEqualTo(1);
        assertThat(fx.total("invited")).isZero();
    }

    // 6
    @Test
    void thePlanLimitAndATakenEmailStillRefuseTheInvite() throws Exception {
        record(fx.inviteRequest("Coach Omar", OMAR_PHONE, OMAR_EMAIL)).andExpect(status().isCreated());

        record(fx.inviteRequest("Coach Ali", "01060000002", OMAR_EMAIL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"));

        record(fx.inviteRequest("Coach Ali", "01060000002", "trainer2@demo.equis")).andExpect(status().isCreated());
        record(fx.inviteRequest("Coach Karim", "01060000003", "trainer3@demo.equis"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan_limit_reached"));

        assertThat(fx.count("trainer")).isEqualTo(2);
        assertThat(fx.count("invite")).isZero();
        assertThat(fx.recorder().all()).isEmpty();
    }

    // 7 (decision 8: invited before mail was turned off)
    @Test
    void resendActivatesATrainerInvitedBeforeMailWasTurnedOff() throws Exception {
        UUID mona = invitedTrainer("Mona", "01060000004", "mona@demo.equis");
        String oldToken = inSchool(() -> links.create(trainers.findById(mona).orElseThrow(), InvitePurpose.INVITE));

        record(fx.admin(post(TrainerAccountsFixture.TRAINERS + "/" + mona + "/resend-invite")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        assertThat(fx.statusOf(mona).name()).isEqualTo("ACTIVE");
        record(fx.acceptInvite(oldToken, "Another@2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
        assertThat(invitesOfTrainer(mona)).isZero();
        assertThat(fx.recorder().all()).isEmpty();
        record(fx.login("mona@demo.equis", DEMO)).andExpect(status().isOk());
    }

    // 8
    @Test
    void aTrainerResetSetsTheDemoPasswordAndSignsOutEverywhere() throws Exception {
        UUID omar = activeTrainerWithOwnPassword("Coach@2026");
        AuthTokens phone = tokens(record(fx.login(OMAR_EMAIL, "Coach@2026")).andExpect(status().isOk()));

        record(fx.admin(post(TrainerAccountsFixture.TRAINERS + "/" + omar + "/reset-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        record(fx.login(OMAR_EMAIL, "Coach@2026")).andExpect(status().isUnauthorized());
        record(fx.refresh(phone)).andExpect(status().isUnauthorized());
        assertThat(openTokens("trainer", omar)).isZero();
        record(fx.login(OMAR_EMAIL, DEMO)).andExpect(status().isOk());
        assertThat(invitesOfTrainer(omar)).isZero();
        assertThat(fx.recorder().all()).isEmpty();
    }

    // 9
    @Test
    void recordingAnAttendedAssessmentCreatesAnActiveRider() throws Exception {
        record(fx.inviteRequest("Coach Omar", OMAR_PHONE, OMAR_EMAIL)).andExpect(status().isCreated());
        UUID omar = trainerId(OMAR_EMAIL);
        UUID booking = booking("Sara Adel", "01070000001", "sara@demo.equis", omar);
        fx.advance(AFTER_SLOT);
        AuthTokens coach = tokens(record(fx.login(OMAR_EMAIL, DEMO)).andExpect(status().isOk()));

        String body = record(fx.perform(fx.asTrainer(post("/me/bookings/" + booking + "/result")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"attended\": true, \"levelId\": 2 }"),
                coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("placed"))
                .andReturn().getResponse().getContentAsString();
        UUID sara = UUID.fromString(JsonPath.read(body, "$.riderId"));

        assertThat(accountStatus(sara)).isEqualTo("active");
        assertThat(invitesOfRider(sara)).isZero();
        assertThat(fx.recorder().all()).isEmpty();
        record(fx.login("sara@demo.equis", DEMO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RIDER"));
    }

    // 10
    @Test
    void riderInvitesAndResendsActivateTheAccount() throws Exception {
        UUID omar = activeTrainer();
        UUID nour = rider("Nour", "nour@demo.equis", omar);
        UUID laila = rider("Laila", "laila@demo.equis", omar);
        String oldToken = inSchool(() -> {
            var rider = riders.findById(laila).orElseThrow();
            rider.markInvited();
            return links.issue(rider, InvitePurpose.INVITE).rawToken();
        });
        assertThat(accountStatus(laila)).isEqualTo("invited");

        for (UUID rider : List.of(nour, laila)) {
            record(fx.admin(post(RIDERS + "/" + rider + "/invite")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountStatus").value("active"))
                    .andExpect(jsonPath("$.linkPurpose").value("invite"))
                    .andExpect(jsonPath("$.linkExpiresAt").value(nullValue()));
            assertThat(accountStatus(rider)).isEqualTo("active");
        }
        assertThat(invitesOfRider(laila)).isZero();
        record(fx.acceptInvite(oldToken, "Another@2026")).andExpect(status().isBadRequest());
        assertThat(fx.recorder().all()).isEmpty();
        record(fx.login("nour@demo.equis", DEMO)).andExpect(status().isOk());
        record(fx.login("laila@demo.equis", DEMO)).andExpect(status().isOk());
    }

    // 11
    @Test
    void aRiderResetSetsTheDemoPasswordAndSignsOutEverywhere() throws Exception {
        UUID omar = activeTrainer();
        UUID laila = rider("Laila", "laila@demo.equis", omar);
        record(fx.admin(post(RIDERS + "/" + laila + "/invite"))).andExpect(status().isOk());
        jdbc.update("update rider set password_hash = ? where id = ?",
                new BCryptPasswordEncoder().encode("Riding@2026"), laila);
        AuthTokens phone = tokens(record(fx.login("laila@demo.equis", "Riding@2026")).andExpect(status().isOk()));

        record(fx.admin(post(RIDERS + "/" + laila + "/reset-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("active"))
                .andExpect(jsonPath("$.linkPurpose").value("reset"))
                .andExpect(jsonPath("$.linkExpiresAt").value(nullValue()));

        record(fx.login("laila@demo.equis", "Riding@2026")).andExpect(status().isUnauthorized());
        record(fx.refresh(phone)).andExpect(status().isUnauthorized());
        assertThat(openTokens("rider", laila)).isZero();
        record(fx.login("laila@demo.equis", DEMO)).andExpect(status().isOk());
        assertThat(invitesOfRider(laila)).isZero();
        assertThat(fx.recorder().all()).isEmpty();
    }

    // 12
    @Test
    void theUsualStatusChecksStillRefuse() throws Exception {
        UUID omar = activeTrainer();
        UUID seed = fx.addRiders(omar, 1, 1).getFirst();
        UUID mona = invitedTrainer("Mona", "01060000004", "mona@demo.equis");

        record(fx.admin(post(RIDERS + "/" + seed + "/invite")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_email"));
        record(fx.admin(post(TrainerAccountsFixture.TRAINERS + "/" + mona + "/reset-password")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_trainer_status"));

        assertThat(accountStatus(seed)).isEqualTo("none");
        assertThat(fx.statusOf(mona).name()).isEqualTo("INVITED");
        assertThat(fx.recorder().all()).isEmpty();
    }

    // 13
    @Test
    void aNewClubsFirstAdminIsActiveAndSignsInWithTheDemoPassword() throws Exception {
        jdbc.update("insert into super_admin (email, password_hash) values (?, ?)",
                SUPER_EMAIL, new BCryptPasswordEncoder().encode(SUPER_PASSWORD));
        AuthTokens platform = tokens(record(fx.perform(post("/platform/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(SUPER_EMAIL, SUPER_PASSWORD)))).andExpect(status().isOk()));

        record(fx.perform(post("/platform/clubs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + platform.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Demo Equestrian", "code": "demo-eq",
                          "admin": { "name": "Hany", "email": "hany@demo.equis" } }""")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admins[0].status").value("ACTIVE"));

        assertThat(asRoot(() -> jdbc.queryForObject(
                "select status from admin_account where email = 'hany@demo.equis'", String.class)))
                .isEqualTo("ACTIVE");
        assertThat(asRoot(() -> jdbc.queryForObject("""
                select count(*) from invite i join admin_account a on a.id = i.admin_account_id
                where a.email = 'hany@demo.equis'""", Integer.class))).isZero();
        assertThat(fx.recorder().all()).isEmpty();
        record(fx.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"clubCode\": \"demo-eq\", \"login\": \"hany@demo.equis\", \"password\": \"%s\", "
                        .formatted(DEMO) + "\"rememberMe\": false }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // 14
    @Test
    void everyAccountGetsItsOwnHash() throws Exception {
        record(fx.inviteRequest("Coach Omar", OMAR_PHONE, OMAR_EMAIL)).andExpect(status().isCreated());
        record(fx.inviteRequest("Coach Ali", "01060000002", "trainer2@demo.equis")).andExpect(status().isCreated());

        List<String> hashes = jdbc.queryForList("select password_hash from trainer", String.class);

        assertThat(hashes).hasSize(2).doesNotHaveDuplicates().doesNotContain(DEMO)
                .allSatisfy(hash -> assertThat(passwordEncoder.matches(DEMO, hash)).isTrue());
    }

    // ---- helpers

    private ResultActions record(ResultActions result) throws Exception {
        bodies.add(result.andReturn().getResponse().getContentAsString());
        return result;
    }

    private UUID activeTrainer() throws Exception {
        record(fx.inviteRequest("Coach Omar", OMAR_PHONE, OMAR_EMAIL)).andExpect(status().isCreated());
        return trainerId(OMAR_EMAIL);
    }

    // Active through demo mode, then a password of their own, as if they had changed it.
    private UUID activeTrainerWithOwnPassword(String password) throws Exception {
        UUID omar = activeTrainer();
        jdbc.update("update trainer set password_hash = ? where id = ?",
                new BCryptPasswordEncoder().encode(password), omar);
        return omar;
    }

    // An invited trainer, as left by an invite sent while mail was still on.
    private UUID invitedTrainer(String name, String phone, String email) {
        return jdbc.queryForObject(
                "insert into trainer (name, phone, email, status) values (?, ?, ?, 'invited') returning id",
                UUID.class, name, phone, email);
    }

    private UUID rider(String name, String email, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id, email) values (?, 1, ?, ?) returning id",
                UUID.class, name, trainer, email);
    }

    private UUID booking(String name, String phone, String email, UUID assessor) {
        UUID slot = fx.addUpcomingSession(null);
        UUID member = jdbc.queryForObject(
                "insert into member (name, phone, email) values (?, ?, ?) returning id", UUID.class, name, phone, email);
        return jdbc.queryForObject(
                "insert into assessment_booking (member_id, session_id, assessor_id) values (?, ?, ?) returning id",
                UUID.class, member, slot, assessor);
    }

    private UUID trainerId(String email) {
        return jdbc.queryForObject("select id from trainer where email = ?", UUID.class, email);
    }

    private int invitesOfTrainer(UUID trainer) {
        return jdbc.queryForObject("select count(*) from invite where trainer_id = ?", Integer.class, trainer);
    }

    private int invitesOfRider(UUID rider) {
        return jdbc.queryForObject("select count(*) from invite where rider_id = ?", Integer.class, rider);
    }

    private String accountStatus(UUID rider) {
        return jdbc.queryForObject("select account_status from rider where id = ?", String.class, rider);
    }

    private int openTokens(String subjectType, UUID subject) {
        return jdbc.queryForObject("""
                select count(*) from refresh_token
                where subject_type = ? and subject_id = ? and revoked_at is null""", Integer.class, subjectType, subject);
    }

    private AuthTokens tokens(ResultActions result) throws Exception {
        return json.readValue(result.andReturn().getResponse().getContentAsString(), AuthTokens.class);
    }

    // A link made the way it was before mail was turned off, in the school's own transaction.
    private <T> T inSchool(Supplier<T> work) {
        return tenants.inClub(Club.DEFAULT_ID, work);
    }

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

    private static <T> T asRoot(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
