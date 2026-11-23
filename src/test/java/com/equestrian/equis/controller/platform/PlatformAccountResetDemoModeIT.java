package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.accounts.AccountLinkService;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.RecordingNotificationSender;
import com.equestrian.equis.support.TestSubscriptions;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E23 · Platform reset sets the demo password: with mail off, the super admin's reset sets the demo password at once,
// deletes the account's open links, signs the person out everywhere, sends no email and is recorded as RESET_PASSWORD.
// Numbers are the ticket's test cases; case 10 (mail on) is PlatformAccountResetIT, unchanged. Clock Sun 22 Nov 2026,
// 11:00 Cairo. The same properties as DemoModeIT, so both classes share one mail-off context.
// The club demo-eq holds the E22 people: admin Hany, trainers Omar (active), Mona (invited), Karim (disabled), riders
// Laila (active) and Sara (no account). Every test starts with the starting passwords and no links or rows.
@ExtendWith(OutputCaptureExtension.class)
@RecordApplicationEvents
@TestPropertySource(properties = {"app.mail.enabled=false",
        "app.mail.demo-password=" + PlatformAccountResetDemoModeIT.DEMO})
class PlatformAccountResetDemoModeIT extends IntegrationTestBase {
    static final String DEMO = "Demo-2026!";
    private static final Instant NOV_22 = ZonedDateTime.of(2026, 11, 22, 11, 0, 0, 0, CAIRO).toInstant();
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    private static final String CODE = "demo-eq";
    private static final String PASSWORD = "Riding@2026";
    private static final String HANY_EMAIL = "hany@demo-eq.com";
    private static final String OMAR_PHONE = "01053000001";
    private static final String OMAR_EMAIL = "omar@demo-eq.eg";
    private static final String LAILA_EMAIL = "laila@demo-eq.eg";

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    MockMvc mvc;
    @Autowired
    RecordingNotificationSender mail;
    @Autowired
    ApplicationEvents events;
    @Autowired
    AccountLinkService links;
    @Autowired
    TenantTransactions tenants;
    @Autowired
    TrainerRepository trainers;

    UUID club;
    UUID hany;
    UUID omar;
    UUID mona;
    UUID karim;
    UUID laila;
    UUID sara;
    String superToken;
    UUID superId;
    // Every response body of a test, for case 9.
    private final List<String> bodies = new ArrayList<>();

    @BeforeEach
    void theTicketsAccounts() throws Exception {
        CLOCK.set(NOV_22);
        root(() -> {
            if (jdbc.queryForObject("select count(*) from club where code = ?", Integer.class, CODE) == 0) {
                seedDemoEq();
            }
            club = jdbc.queryForObject("select id from club where code = ?", UUID.class, CODE);
            jdbc.update("update club set status = 'ACTIVE', suspended_at = null, suspended_reason = null "
                    + "where id = ?", club);
            jdbc.update("delete from invite where club_id = ?", club);
            jdbc.update("delete from platform_account_action");
            jdbc.update("delete from refresh_token where club_id = ?", club);
            // A demo reset replaces the password: put the starting ones back.
            String hash = passwordEncoder.encode(PASSWORD);
            jdbc.update("update admin_account set password_hash = ?, failed_attempts = 0, locked_until = null "
                    + "where club_id = ?", hash, club);
            jdbc.update("update trainer set password_hash = ? where club_id = ? and status <> 'invited'", hash, club);
            jdbc.update("update rider set password_hash = ? where club_id = ? and account_status = 'active'",
                    hash, club);
            jdbc.update("""
                    insert into super_admin (email, password_hash) select ?, ?
                    where not exists (select 1 from super_admin where email = ?)""",
                    SUPER_EMAIL, passwordEncoder.encode(SUPER_PASSWORD), SUPER_EMAIL);
            hany = id("admin_account", HANY_EMAIL);
            omar = id("trainer", OMAR_EMAIL);
            mona = id("trainer", "mona@demo-eq.eg");
            karim = id("trainer", "karim@demo-eq.eg");
            laila = id("rider", LAILA_EMAIL);
            sara = jdbc.queryForObject("select id from rider where club_id = ? and name = 'Sara'", UUID.class, club);
            superId = jdbc.queryForObject("select id from super_admin where email = ?", UUID.class, SUPER_EMAIL);
            return null;
        });
        ResultActions signedIn = mvc.perform(post("/platform/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk());
        superToken = JsonPath.read(body(signedIn), "$.accessToken");
        mail.clear();
        bodies.clear();
    }

    // 9: no answer carries the demo password.
    @AfterEach
    void noResponseContainsTheDemoPassword() {
        assertThat(bodies).isNotEmpty().allSatisfy(body -> assertThat(body).doesNotContain(DEMO));
    }

    // 1
    @Test
    void resettingOmarAnswersWithNoLinkSendsNoEmailAndRecordsOneRow() throws Exception {

        record(reset("trainer", omar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(omar.toString()))
                .andExpect(jsonPath("$.type").value("TRAINER"))
                .andExpect(jsonPath("$.email").value(OMAR_EMAIL))
                .andExpect(jsonPath("$.linkExpiresAt").value(nullValue()));

        assertThat(openLinks("trainer_id", omar)).isZero();
        assertThat(mail.all()).isEmpty();
        assertThat(events.stream(AccountLinkCreatedEvent.class)).isEmpty();
        assertThat(actionRows()).singleElement().satisfies(row -> {
            assertThat(row.get("club_id")).isEqualTo(club);
            assertThat(row.get("account_type")).isEqualTo("TRAINER");
            assertThat(row.get("account_id")).isEqualTo(omar);
            assertThat(row.get("action")).isEqualTo("RESET_PASSWORD");
            assertThat(row.get("done_by")).isEqualTo(superId);
            assertThat(((Timestamp) row.get("done_at")).toInstant()).isEqualTo(NOV_22);
        });
    }

    // 2
    @Test
    void afterTheResetOmarSignsInOnlyWithTheDemoPasswordAndIsSignedOut() throws Exception {
        String earlierRefresh = refreshTokenOf(record(login(OMAR_PHONE, PASSWORD)).andExpect(status().isOk()));

        record(reset("trainer", omar)).andExpect(status().isOk());

        record(login(OMAR_PHONE, PASSWORD)).andExpect(status().isUnauthorized());
        record(refresh(earlierRefresh)).andExpect(status().isUnauthorized());
        assertThat(openTokens("trainer", omar)).isZero();
        record(login(OMAR_PHONE, DEMO)).andExpect(status().isOk());
    }

    // 3
    @Test
    void hanyAndLailaAreResetTheSameWay() throws Exception {
        String hanysRefresh = refreshTokenOf(record(login(HANY_EMAIL, PASSWORD)).andExpect(status().isOk()));
        String lailasRefresh = refreshTokenOf(record(login(LAILA_EMAIL, PASSWORD)).andExpect(status().isOk()));

        record(reset("admin", hany))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ADMIN"))
                .andExpect(jsonPath("$.email").value(HANY_EMAIL))
                .andExpect(jsonPath("$.linkExpiresAt").value(nullValue()));
        record(reset("rider", laila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("RIDER"))
                .andExpect(jsonPath("$.email").value(LAILA_EMAIL))
                .andExpect(jsonPath("$.linkExpiresAt").value(nullValue()));

        for (String login : List.of(HANY_EMAIL, LAILA_EMAIL)) {
            record(login(login, PASSWORD)).andExpect(status().isUnauthorized());
        }
        record(refresh(hanysRefresh)).andExpect(status().isUnauthorized());
        record(refresh(lailasRefresh)).andExpect(status().isUnauthorized());
        // Hany signs in to the web console with the club code.
        record(login(HANY_EMAIL, DEMO)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        record(login(LAILA_EMAIL, DEMO)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("RIDER"));
        assertThat(openLinks("admin_account_id", hany)).isZero();
        assertThat(openLinks("rider_id", laila)).isZero();
        assertThat(mail.all()).isEmpty();
        assertThat(actionRows()).extracting(r -> r.get("account_type")).containsExactlyInAnyOrder("ADMIN", "RIDER");
    }

    // 4
    @Test
    void aResetLinkFromBeforeMailWasTurnedOffStopsWorking() throws Exception {
        String oldToken = tenants.inClub(club,
                () -> links.create(trainers.findById(omar).orElseThrow(), InvitePurpose.RESET));
        assertThat(openLinks("trainer_id", omar)).isOne();

        record(reset("trainer", omar)).andExpect(status().isOk());

        assertThat(openLinks("trainer_id", omar)).isZero();
        record(accept(oldToken, "Another@2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
        record(login(OMAR_PHONE, DEMO)).andExpect(status().isOk());
    }

    // 5
    @Test
    void anAccountAlreadyOnTheDemoPasswordIsResetLikeAnyOther() throws Exception {
        record(reset("trainer", omar)).andExpect(status().isOk());
        String demoRefresh = refreshTokenOf(record(login(OMAR_PHONE, DEMO)).andExpect(status().isOk()));

        record(reset("trainer", omar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkExpiresAt").value(nullValue()));

        record(refresh(demoRefresh)).andExpect(status().isUnauthorized());
        record(login(OMAR_PHONE, DEMO)).andExpect(status().isOk());
        assertThat(actionRows()).hasSize(2)
                .allSatisfy(row -> assertThat(row.get("account_id")).isEqualTo(omar));
    }

    // 6
    @Test
    void anInvitedDisabledOrAccountlessPersonIsRefusedAndNothingChanges() throws Exception {
        String monasHash = passwordHash("trainer", mona);
        String karimsHash = passwordHash("trainer", karim);

        refused(record(reset("trainer", mona)), "INVITED");
        refused(record(reset("trainer", karim)), "DISABLED");
        refused(record(reset("rider", sara)), "NO_ACCOUNT");

        assertThat(passwordHash("trainer", mona)).isEqualTo(monasHash);
        assertThat(passwordHash("trainer", karim)).isEqualTo(karimsHash);
        assertThat(actionRows()).isEmpty();
        assertThat(mail.all()).isEmpty();
    }

    // 7
    @Test
    void anotherClubsAccountOrAnUnknownClubIsNotFoundAndNothingIsWritten() throws Exception {
        // The seeded admin of the default club, asked for under demo-eq.
        UUID otherAdmin = root(() -> jdbc.queryForObject("select id from admin_account where club_id = ?",
                UUID.class, Club.DEFAULT_ID));
        String otherHash = passwordHash("admin_account", otherAdmin);
        String hanysHash = passwordHash("admin_account", hany);

        notFound(record(reset("admin", otherAdmin)), "account_not_found");
        notFound(record(reset(UUID.randomUUID(), "admin", hany)), "club_not_found");

        assertThat(passwordHash("admin_account", otherAdmin)).isEqualTo(otherHash);
        assertThat(passwordHash("admin_account", hany)).isEqualTo(hanysHash);
        assertThat(root(() -> jdbc.queryForObject("select count(*) from invite where purpose = 'reset'",
                Integer.class))).isZero();
        record(login(HANY_EMAIL, PASSWORD)).andExpect(status().isOk());
        assertThat(actionRows()).isEmpty();
        assertThat(mail.all()).isEmpty();
    }

    // 8
    @Test
    void aSuspendedClubCanStillBeHelped() throws Exception {
        root(() -> jdbc.update("update club set status = 'SUSPENDED', suspended_at = now(), "
                + "suspended_reason = 'unpaid' where id = ?", club));

        record(reset("admin", hany)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(HANY_EMAIL));

        assertThat(passwordEncoder.matches(DEMO, passwordHash("admin_account", hany))).isTrue();
        assertThat(actionRows()).hasSize(1);
    }

    // 9 (logs; the bodies are checked after each test)
    @Test
    void theLogsNeverContainTheDemoPassword(CapturedOutput output) throws Exception {
        record(reset("admin", hany)).andExpect(status().isOk());
        record(reset("trainer", omar)).andExpect(status().isOk());
        record(reset("rider", laila)).andExpect(status().isOk());

        assertThat(output.getAll()).doesNotContain(DEMO);
    }

    // ---- set-up: demo-eq with the E22 people (TwoClubFixture activates accounts through emailed links, which mail
    // off does not send, so the club is written directly). Active accounts start with PASSWORD.

    private void seedDemoEq() {
        UUID id = jdbc.queryForObject(
                "insert into club (code, name) values (?, 'Demo Equestrian') returning id", UUID.class, CODE);
        TestSubscriptions.validFor(jdbc, id);
        String hash = passwordEncoder.encode(PASSWORD);
        jdbc.update("insert into admin_account (club_id, name, email, password_hash) values (?, 'Hany Saeed', ?, ?)",
                id, HANY_EMAIL, hash);
        jdbc.update("""
                insert into trainer (club_id, name, phone, email, password_hash, status) values
                (?, 'Omar Khaled', ?, ?, ?, 'active'),
                (?, 'Mona Fathy', '01053000002', 'mona@demo-eq.eg', null, 'invited'),
                (?, 'Karim Adel', '01053000003', 'karim@demo-eq.eg', ?, 'disabled')""",
                id, OMAR_PHONE, OMAR_EMAIL, hash, id, id, hash);
        jdbc.update("""
                insert into rider (club_id, name, level_id, trainer_id, email, password_hash, account_status) values
                (?, 'Laila Mostafa', 1, null, ?, ?, 'active'),
                (?, 'Sara', 1, null, null, null, 'none')""",
                id, LAILA_EMAIL, hash, id);
    }
    // ---- helpers

    private ResultActions record(ResultActions result) throws Exception {
        bodies.add(body(result));
        return result;
    }

    private ResultActions reset(String type, UUID id) throws Exception {
        return reset(club, type, id);
    }

    private ResultActions reset(UUID club, String type, UUID id) throws Exception {
        return mvc.perform(post("/platform/clubs/" + club + "/accounts/" + type + "/" + id + "/reset-password")
                .header("Authorization", "Bearer " + superToken));
    }

    private ResultActions login(String login, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"clubCode\": \"%s\", \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(CODE, login, password)));
    }

    private ResultActions accept(String token, String password) throws Exception {
        return mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"token\": \"%s\", \"password\": \"%s\" }".formatted(token, password)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refreshToken\": \"%s\" }".formatted(refreshToken)));
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // An account of demo-eq by its email; runs inside root().
    private UUID id(String table, String email) {
        return jdbc.queryForObject("select id from " + table + " where club_id = ? and email = ?", UUID.class,
                club, email);
    }

    private static String refreshTokenOf(ResultActions signedIn) throws Exception {
        return JsonPath.read(body(signedIn), "$.refreshToken");
    }

    private static void refused(ResultActions result, String accountStatus) throws Exception {
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_account_status"))
                .andExpect(jsonPath("$.action").value("reset_password"));
        assertThat(body(result)).contains("\"status\":\"" + accountStatus + "\"");
    }

    private static void notFound(ResultActions result, String code) throws Exception {
        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(code));
    }

    private List<Map<String, Object>> actionRows() {
        return root(() -> jdbc.queryForList("select * from platform_account_action order by done_at, id"));
    }

    private int openLinks(String column, UUID account) {
        return root(() -> jdbc.queryForObject(
                "select count(*) from invite where " + column + " = ? and accepted_at is null", Integer.class, account));
    }

    private int openTokens(String subjectType, UUID subject) {
        return root(() -> jdbc.queryForObject("""
                select count(*) from refresh_token
                where subject_type = ? and subject_id = ? and revoked_at is null""", Integer.class, subjectType, subject));
    }

    private String passwordHash(String table, UUID id) {
        return root(() -> jdbc.queryForObject("select password_hash from " + table + " where id = ?", String.class,
                id));
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
