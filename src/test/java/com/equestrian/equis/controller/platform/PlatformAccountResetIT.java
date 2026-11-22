package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.mail.OutgoingEmail;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.RecordingNotificationSender;
import com.equestrian.equis.support.TwoClubFixture;
import com.equestrian.equis.support.TwoClubFixture.ClubData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TwoClubFixture.PASSWORD;
import static com.equestrian.equis.support.TwoClubFixture.body;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E22 · Reset password from the platform, cases 1-13 of the ticket with the approved contract. Clock Sat 21 Nov 2026,
// 17:40 Cairo. TwoClubFixture's cairo-eq is reshaped once per class into the list ticket's people: admin Hany, trainers
// Omar (active), Mona (invited) and Karim (disabled), riders Laila (active) and Sara (no account). Every test starts
// with no open reset links, no action rows, no emails, the fixture passwords and an active, paid club.
// Case 4: /auth/invite/accept answers 200 with the club code (E1/E17), not the ticket's 204.
@ExtendWith(OutputCaptureExtension.class)
@RecordApplicationEvents
class PlatformAccountResetIT extends IntegrationTestBase {
    private static final Instant NOV_21 = ZonedDateTime.of(2026, 11, 21, 17, 40, 0, 0, CAIRO).toInstant();
    private static final Instant NOV_22 = NOV_21.plus(Duration.ofHours(24));
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    private static final String HANY_EMAIL = "hany@cairo-eq.com";
    private static final String OMAR_EMAIL = "trainer1@cairo-eq.eg";
    private static final String OMAR_PHONE = "01052000001";
    private static final String LAILA_EMAIL = "person1@cairo-eq.eg";
    private static final String NEW_PASSWORD = "Reset@2026";
    private static final String SUBJECT = "Set a new password for Riding Schedule";
    private static final String NOTE = "Your current password works until you set a new one.";
    private static final String PLATFORM_INTRO =
            "The equis team sent you a link to set a new password for your %s account.";

    @Autowired
    TwoClubFixture fx;
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
    // The trainer app's address; a local .env may set it, so it is read rather than written here.
    @Value("${app.base-url}")
    String trainerBaseUrl;

    ClubData school;
    ClubData cairoEq;
    String superToken;
    UUID superId;
    UUID hany;
    UUID karim;

    @BeforeEach
    void theTicketsAccounts() throws Exception {
        school = fx.a();
        cairoEq = fx.b();
        CLOCK.set(NOV_21);
        root(() -> {
            jdbc.update("update subscription set ends_at = TIMESTAMPTZ '2200-01-01 00:00:00+00'");
            jdbc.update("update club set status = 'ACTIVE', suspended_at = null, suspended_reason = null");
            if (jdbc.queryForObject("select count(*) from trainer where name = 'Karim Adel'", Integer.class) == 0) {
                reshapeCairoEq();
            }
            // A test may set a new password or leave links and rows behind.
            jdbc.update("delete from invite where purpose = 'reset'");
            jdbc.update("delete from platform_account_action");
            // A test may sign in later in the day; an earlier clock must not revoke those tokens.
            jdbc.update("delete from refresh_token where club_id = ?", cairoEq.clubId());
            jdbc.update("update admin_account set password_hash = ?, failed_attempts = 0, locked_until = null "
                    + "where club_id = ?", passwordEncoder.encode(PASSWORD), cairoEq.clubId());
            jdbc.update("""
                    insert into super_admin (email, password_hash) select ?, ?
                    where not exists (select 1 from super_admin where email = ?)""",
                    SUPER_EMAIL, passwordEncoder.encode(SUPER_PASSWORD), SUPER_EMAIL);
            return null;
        });
        hany = root(() -> jdbc.queryForObject("select id from admin_account where club_id = ?", UUID.class,
                cairoEq.clubId()));
        karim = root(() -> jdbc.queryForObject("select id from trainer where name = 'Karim Adel'", UUID.class));
        superId = root(() -> jdbc.queryForObject("select id from super_admin where email = ?", UUID.class,
                SUPER_EMAIL));
        ResultActions signedIn = mvc.perform(post("/platform/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk());
        superToken = JsonPath.read(body(signedIn), "$.accessToken");
        mail.clear();
    }

    // 1
    @Test
    void resettingHanyAnswersTheResetAndSavesOneLinkAndOneActionRow() throws Exception {
        reset("admin", hany)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(hany.toString()))
                .andExpect(jsonPath("$.type").value("ADMIN"))
                .andExpect(jsonPath("$.email").value(HANY_EMAIL))
                .andExpect(jsonPath("$.linkExpiresAt").value(NOV_22.toString()));

        List<Map<String, Object>> links = root(() -> jdbc.queryForList(
                "select club_id, purpose, expires_at, accepted_at from invite where admin_account_id = ? "
                        + "and accepted_at is null", hany));
        assertThat(links).singleElement().satisfies(link -> {
            assertThat(link.get("club_id")).isEqualTo(cairoEq.clubId());
            assertThat(link.get("purpose")).isEqualTo("reset");
            assertThat(((Timestamp) link.get("expires_at")).toInstant()).isEqualTo(NOV_22);
        });
        assertThat(actionRows()).singleElement().satisfies(row -> {
            assertThat(row.get("club_id")).isEqualTo(cairoEq.clubId());
            assertThat(row.get("account_type")).isEqualTo("ADMIN");
            assertThat(row.get("account_id")).isEqualTo(hany);
            assertThat(row.get("account_name")).isEqualTo("Hany Saeed");
            assertThat(row.get("account_email")).isEqualTo(HANY_EMAIL);
            assertThat(row.get("action")).isEqualTo("RESET_PASSWORD");
            assertThat(row.get("done_by")).isEqualTo(superId);
            assertThat(((Timestamp) row.get("done_at")).toInstant()).isEqualTo(NOV_21);
        });
    }

    // 2
    @Test
    void hanysEmailNamesTheEquisTeamAndGivesTheConsoleLinkAndClubCode() throws Exception {
        reset("admin", hany).andExpect(status().isOk());

        assertThat(mail.all()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo(HANY_EMAIL);
            assertThat(email.subject()).isEqualTo(SUBJECT);
            assertThat(email.text())
                    .contains("http://admin.test/invite/" + RecordingNotificationSender.tokenFrom(email))
                    .contains(PLATFORM_INTRO.formatted("admin"))
                    .contains("Your club code: cairo-eq")
                    .contains("After that, sign in on the web console with this email and your club code.")
                    .contains(NOTE)
                    .doesNotContain("You asked for a link");
            assertThat(email.html()).contains(PLATFORM_INTRO.formatted("admin")).contains("Your club code: cairo-eq");
        });
    }

    // 3
    @Test
    void hanysOldPasswordStillSignsInBeforeTheLinkIsUsed() throws Exception {
        reset("admin", hany).andExpect(status().isOk());

        fx.loginRequest("cairo-eq", HANY_EMAIL, PASSWORD).andExpect(status().isOk());
    }

    // 4 (200 with the club code: see the class comment)
    @Test
    void acceptingTheLinkSetsTheNewPasswordAndSignsHanyOutEverywhere() throws Exception {
        String earlierRefresh = JsonPath.read(body(
                fx.loginRequest("cairo-eq", HANY_EMAIL, PASSWORD).andExpect(status().isOk())), "$.refreshToken");
        reset("admin", hany).andExpect(status().isOk());
        String token = RecordingNotificationSender.tokenFrom(mail.last());

        CLOCK.set(ZonedDateTime.of(2026, 11, 21, 18, 5, 0, 0, CAIRO).toInstant());
        accept(token, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubCode").value("cairo-eq"));

        refresh(earlierRefresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
        fx.loginRequest("cairo-eq", HANY_EMAIL, NEW_PASSWORD).andExpect(status().isOk());
        fx.loginRequest("cairo-eq", HANY_EMAIL, PASSWORD).andExpect(status().isUnauthorized());
        // The link works once.
        accept(token, "Other@2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
    }

    // 5
    @Test
    void omarAndLailaGetTheirAppsLinkWithThePlatformIntro() throws Exception {
        reset("trainer", cairoEq.trainer1())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(cairoEq.trainer1().toString()))
                .andExpect(jsonPath("$.type").value("TRAINER"))
                .andExpect(jsonPath("$.email").value(OMAR_EMAIL))
                .andExpect(jsonPath("$.linkExpiresAt").value(NOV_22.toString()));
        OutgoingEmail omars = mail.last();
        assertThat(omars.to()).isEqualTo(OMAR_EMAIL);
        assertThat(omars.subject()).isEqualTo(SUBJECT);
        assertThat(omars.text())
                .contains(trainerBaseUrl + "/invite/" + RecordingNotificationSender.tokenFrom(omars))
                .doesNotContain("http://admin.test/")
                .doesNotContain("http://rider.test/")
                .contains(PLATFORM_INTRO.formatted("trainer"))
                .contains(NOTE)
                .doesNotContain("Your admin sent you")
                .doesNotContain("Your club code");

        reset("rider", cairoEq.rider1())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(cairoEq.rider1().toString()))
                .andExpect(jsonPath("$.type").value("RIDER"))
                .andExpect(jsonPath("$.email").value(LAILA_EMAIL))
                .andExpect(jsonPath("$.linkExpiresAt").value(NOV_22.toString()));
        OutgoingEmail lailas = mail.last();
        assertThat(lailas.to()).isEqualTo(LAILA_EMAIL);
        assertThat(lailas.subject()).isEqualTo(SUBJECT);
        assertThat(lailas.text())
                .contains("http://rider.test/invite/" + RecordingNotificationSender.tokenFrom(lailas))
                .contains(PLATFORM_INTRO.formatted("rider"))
                .contains(NOTE)
                .doesNotContain("Your admin sent you");

        assertThat(mail.all()).hasSize(2);
        assertThat(actionRows()).extracting(r -> r.get("account_type"), r -> r.get("account_name"))
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("TRAINER", "Omar Khaled"),
                        org.assertj.core.groups.Tuple.tuple("RIDER", "Laila Mostafa"));
        // Both links work.
        accept(RecordingNotificationSender.tokenFrom(omars), NEW_PASSWORD).andExpect(status().isOk());
        accept(RecordingNotificationSender.tokenFrom(lailas), NEW_PASSWORD).andExpect(status().isOk());
        fx.loginRequest("cairo-eq", OMAR_PHONE, NEW_PASSWORD).andExpect(status().isOk());
        fx.loginRequest("cairo-eq", LAILA_EMAIL, NEW_PASSWORD).andExpect(status().isOk());
        // Put the fixture passwords back for the other tests.
        root(() -> jdbc.update("update trainer set password_hash = ? where id = ?",
                passwordEncoder.encode(PASSWORD), cairoEq.trainer1()));
        root(() -> jdbc.update("update rider set password_hash = ? where id = ?",
                passwordEncoder.encode(PASSWORD), cairoEq.rider1()));
    }

    // 6
    @Test
    void aSecondResetStopsTheFirstLinkAndAddsASecondRow() throws Exception {
        reset("admin", hany).andExpect(status().isOk());
        String first = RecordingNotificationSender.tokenFrom(mail.last());
        reset("admin", hany).andExpect(status().isOk());
        String second = RecordingNotificationSender.tokenFrom(mail.last());
        assertThat(second).isNotEqualTo(first);

        assertThat(openResetLinks()).isOne();
        assertThat(actionRows()).hasSize(2)
                .allSatisfy(row -> assertThat(row.get("account_id")).isEqualTo(hany));
        accept(first, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
        accept(second, NEW_PASSWORD).andExpect(status().isOk());
    }

    // 7
    @Test
    void anInvitedDisabledOrAccountlessPersonIsRefusedAndNothingIsWritten() throws Exception {
        refused(reset("trainer", cairoEq.trainer2()), "INVITED");
        refused(reset("trainer", karim), "DISABLED");
        refused(reset("rider", cairoEq.rider2()), "NO_ACCOUNT");
        // An invited admin too.
        root(() -> jdbc.update("update admin_account set status = 'INVITED' where id = ?", hany));
        try {
            refused(reset("admin", hany), "INVITED");
        } finally {
            root(() -> jdbc.update("update admin_account set status = 'ACTIVE' where id = ?", hany));
        }

        assertThat(openResetLinks()).isZero();
        assertThat(actionRows()).isEmpty();
        assertThat(mail.all()).isEmpty();
        assertThat(events.stream(AccountLinkCreatedEvent.class)).isEmpty();
    }

    // 8
    @Test
    void anotherClubsAccountUnderThisClubIsNotFoundAndNothingIsWritten() throws Exception {
        Map<String, String> schoolBefore = fx.snapshotOf(school.clubId());

        notFound(reset("trainer", school.trainer1()), "account_not_found");
        notFound(reset("rider", school.rider1()), "account_not_found");
        UUID schoolAdmin = root(() -> jdbc.queryForObject("select id from admin_account where club_id = ?",
                UUID.class, school.clubId()));
        notFound(reset("admin", schoolAdmin), "account_not_found");

        assertThat(root(() -> jdbc.queryForObject("select count(*) from invite where purpose = 'reset'",
                Integer.class))).isZero();
        assertThat(actionRows()).isEmpty();
        assertThat(mail.all()).isEmpty();
        assertThat(fx.snapshotOf(school.clubId())).isEqualTo(schoolBefore);
    }

    // 9
    @Test
    void anUnknownTypeAccountOrClubIsRefused() throws Exception {
        for (String type : List.of("owner", "ADMIN", "Trainer", "admins")) {
            reset(cairoEq.clubId(), type, hany)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("type"));
        }
        notFound(reset("admin", UUID.randomUUID()), "account_not_found");
        notFound(reset("trainer", UUID.randomUUID()), "account_not_found");
        notFound(reset("rider", UUID.randomUUID()), "account_not_found");
        notFound(reset(UUID.randomUUID(), "admin", hany), "club_not_found");
        fx.perform(superToken, post("/platform/clubs/" + cairoEq.clubId() + "/accounts/admin/not-a-uuid/reset-password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        assertThat(actionRows()).isEmpty();
        assertThat(mail.all()).isEmpty();
    }

    // 10 (and a blocked club: its trial or plan ended)
    @Test
    void aSuspendedOrBlockedClubCanStillBeHelped() throws Exception {
        root(() -> jdbc.update("update club set status = 'SUSPENDED', suspended_at = now(), "
                + "suspended_reason = 'unpaid' where id = ?", cairoEq.clubId()));
        reset("admin", hany).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(HANY_EMAIL));

        root(() -> jdbc.update("update club set status = 'ACTIVE', suspended_at = null, suspended_reason = null "
                + "where id = ?", cairoEq.clubId()));
        root(() -> jdbc.update("update subscription set ends_at = ? where club_id = ?",
                Timestamp.from(NOV_21.minus(Duration.ofDays(1))), cairoEq.clubId()));
        reset("trainer", cairoEq.trainer1()).andExpect(status().isOk());

        assertThat(actionRows()).hasSize(2);
        assertThat(mail.all()).hasSize(2);
    }

    // 11 (and 401 without a token)
    @Test
    void onlyTheSuperAdminMayReset() throws Exception {
        String path = path(cairoEq.clubId(), "trainer", cairoEq.trainer1());
        for (String token : List.of(clubLogin(cairoEq, HANY_EMAIL), clubLogin(cairoEq, OMAR_PHONE),
                clubLogin(cairoEq, LAILA_EMAIL), clubLogin(school, "admin@school.eg"))) {
            fx.perform(token, post(path))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
        mvc.perform(post(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));

        assertThat(openResetLinks()).isZero();
        assertThat(actionRows()).isEmpty();
        assertThat(mail.all()).isEmpty();
    }

    // 12
    @Test
    void aFailedEmailKeepsTheLinkAndTheRowAndLogsNoToken(CapturedOutput output) throws Exception {
        mail.failWith(new MailSendException("SMTP down"));

        reset("admin", hany).andExpect(status().isOk());

        String token = events.stream(AccountLinkCreatedEvent.class).findFirst().orElseThrow().rawToken();
        assertThat(openResetLinks()).isOne();
        assertThat(actionRows()).hasSize(1);
        assertThat(output.getAll()).doesNotContain(token);
        assertThat(output.getAll().lines().filter(line -> line.contains("Account link email failed")).toList())
                .singleElement().asString()
                .contains("ADMIN=" + hany + " purpose=RESET")
                .contains("SMTP down")
                .doesNotContain("/invite/");
        mail.clear();
        // The saved link still works.
        accept(token, NEW_PASSWORD).andExpect(status().isOk());
    }

    // 13
    @Test
    void aClubAdminsResetKeepsTheAdminIntro() throws Exception {
        String hanysToken = clubLogin(cairoEq, HANY_EMAIL);
        fx.perform(hanysToken, post("/admin/trainers/" + cairoEq.trainer1() + "/reset-password"))
                .andExpect(status().isOk());
        OutgoingEmail trainers = mail.last();
        assertThat(trainers.subject()).isEqualTo(SUBJECT);
        assertThat(trainers.text())
                .contains("Your admin sent you a link to set a new password for the Riding Schedule app.")
                .doesNotContain("The equis team");

        fx.perform(hanysToken, post("/admin/riders/" + cairoEq.rider1() + "/reset-password"))
                .andExpect(status().isOk());
        assertThat(mail.last().text())
                .contains("Your admin sent you a link to set a new password for your rider account.")
                .doesNotContain("The equis team");

        // A club admin's own reset is not a platform action.
        assertThat(actionRows()).isEmpty();
    }

    // ---- set-up (the list ticket's people, as in PlatformAccountApiIT)

    private void reshapeCairoEq() {
        UUID club = cairoEq.clubId();
        jdbc.update("update admin_account set name = 'Hany Saeed', email = ? where club_id = ?", HANY_EMAIL, club);
        jdbc.update("update trainer set name = 'Omar Khaled', status = 'active' where id = ?", cairoEq.trainer1());
        jdbc.update("update trainer set name = 'Mona Fathy', status = 'invited', last_active_at = null where id = ?",
                cairoEq.trainer2());
        jdbc.update("""
                insert into trainer (club_id, name, phone, email, password_hash, status, last_active_at)
                values (?, 'Karim Adel', '01052000003', 'karim@cairo-eq.eg', ?, 'disabled', ?)""",
                club, passwordEncoder.encode(PASSWORD), Timestamp.from(NOV_21.minusSeconds(86_400 * 10)));
        jdbc.update("update rider set name = 'Laila Mostafa' where id = ?", cairoEq.rider1());
        jdbc.update("update rider set name = 'Sara', email = null, account_status = 'none', last_active_at = null "
                + "where id = ?", cairoEq.rider2());
    }

    // ---- helpers

    private ResultActions reset(String type, UUID id) throws Exception {
        return reset(cairoEq.clubId(), type, id);
    }

    private ResultActions reset(UUID club, String type, UUID id) throws Exception {
        return fx.perform(superToken, post(path(club, type, id)));
    }

    private static String path(UUID club, String type, UUID id) {
        return "/platform/clubs/" + club + "/accounts/" + type + "/" + id + "/reset-password";
    }

    private String clubLogin(ClubData club, String login) throws Exception {
        return JsonPath.read(body(fx.loginRequest(club.code(), login, PASSWORD).andExpect(status().isOk())),
                "$.accessToken");
    }

    private ResultActions accept(String token, String password) throws Exception {
        return mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"token\": \"%s\", \"password\": \"%s\" }".formatted(token, password)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refreshToken\": \"%s\" }".formatted(refreshToken)));
    }

    private static void refused(ResultActions result, String accountStatus) throws Exception {
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_account_status"))
                .andExpect(jsonPath("$.action").value("reset_password"));
        // The body holds "status" twice (409, then the account status, as RiderAccountService does): read it raw.
        assertThat(body(result)).contains("\"status\":\"" + accountStatus + "\"");
    }

    private static void notFound(ResultActions result, String code) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(code));
    }

    private List<Map<String, Object>> actionRows() {
        return root(() -> jdbc.queryForList("select * from platform_account_action order by done_at, id"));
    }

    private int openResetLinks() {
        return root(() -> jdbc.queryForObject(
                "select count(*) from invite where club_id = ? and purpose = 'reset' and accepted_at is null",
                Integer.class, cairoEq.clubId()));
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
