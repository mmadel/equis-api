package com.equestrian.equis.controller.club;

import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.service.sessions.SessionGenerationJob;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.RecordingNotificationSender;
import com.equestrian.equis.support.TwoClubFixture;
import com.equestrian.equis.support.TwoClubFixture.ClubData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TwoClubFixture.PASSWORD;
import static com.equestrian.equis.support.TwoClubFixture.body;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · club access state, cases 1-15 (case 16 is the rest of the suite; the allow-list is ClubAccessFilterTest).
// The two clubs of TwoClubFixture. Before each case their subscriptions are reset to: "school" on Basic monthly
// to 1 Dec 2026 10:00 (read-only after), "cairo-eq" on the trial to 7 Dec 2026 15:00 (blocked after).
// Ahmed is school's trainer 1, Hany is cairo-eq's admin. Access tokens last 15 minutes, so each case signs in at
// the time it needs.
class ClubAccessStateIT extends IntegrationTestBase {
    private static final Instant NOV_1 = cairo(2026, 11, 1, 10, 0);
    private static final Instant NOV_7 = cairo(2026, 11, 7, 15, 0);
    private static final Instant NOV_20 = cairo(2026, 11, 20, 10, 0);
    private static final Instant NOV_30 = cairo(2026, 11, 30, 10, 0);
    private static final Instant DEC_1 = cairo(2026, 12, 1, 10, 0);
    private static final Instant DEC_1_11 = cairo(2026, 12, 1, 11, 0);
    private static final Instant DEC_3 = cairo(2026, 12, 3, 10, 0);
    private static final Instant DEC_7 = cairo(2026, 12, 7, 15, 0);
    private static final Instant DEC_7_1459 = cairo(2026, 12, 7, 14, 59);
    private static final Instant DEC_7_1501 = cairo(2026, 12, 7, 15, 1);
    private static final Instant DEC_8 = cairo(2026, 12, 8, 10, 0);
    private static final String AHMED = "01051000001";
    private static final String SCHOOL_ADMIN = "admin@school.eg";
    private static final String SCHOOL_RIDER = "person1@school.eg";
    private static final String HANY = "admin@cairo-eq.eg";

    @Autowired
    TwoClubFixture fx;
    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    RecordingNotificationSender recorder;
    @Autowired
    SessionGenerationJob sessionJob;

    ClubData school;
    ClubData cairoEq;

    @BeforeEach
    void schoolOnBasicAndCairoOnTheTrial() {
        school = fx.a();
        cairoEq = fx.b();
        root(() -> {
            jdbc.update("delete from subscription");
            jdbc.update("update club set status = 'ACTIVE', suspended_at = null, suspended_reason = null");
            jdbc.update("update admin_account set failed_attempts = 0, locked_until = null");
            jdbc.update("""
                    insert into plan (code, name, max_trainers, max_riders, trial_days, on_expiry)
                    select 'TRIAL', 'Trial', null, null, 30, 'BLOCK'
                    where not exists (select 1 from plan where code = 'TRIAL')""");
            jdbc.update("""
                    insert into plan (code, name, max_trainers, max_riders, trial_days, on_expiry)
                    select 'BASIC', 'Basic', 10, 100, null, 'READ_ONLY'
                    where not exists (select 1 from plan where code = 'BASIC')""");
            subscription(school.clubId(), "BASIC", "MONTHLY", NOV_1, DEC_1);
            subscription(cairoEq.clubId(), "TRIAL", "TRIAL", NOV_7, DEC_7);
            return null;
        });
    }

    // ---- active

    // 1
    @Test
    void beforeTheEndEverythingWorks() throws Exception {
        at(NOV_30);
        String ahmed = signIn("school", AHMED);
        String admin = signIn("school", SCHOOL_ADMIN);

        perform(ahmed, get("/me/riders")).andExpect(status().isOk());
        perform(admin, payment(school.rider1(), "2026-11")).andExpect(status().isCreated());
    }

    // ---- read-only

    // 2
    @Test
    void afterAPaidSubscriptionEndsEveryoneCanStillRead() throws Exception {
        at(DEC_1_11);
        perform(signIn("school", AHMED), get("/me/riders")).andExpect(status().isOk());
        perform(signIn("school", SCHOOL_ADMIN), get("/admin/riders")).andExpect(status().isOk());
        perform(signIn("school", SCHOOL_RIDER), get("/rider/me")).andExpect(status().isOk());
    }

    // 3
    @Test
    void afterAPaidSubscriptionEndsEveryWriteIsRefusedAndNothingChanges() throws Exception {
        at(DEC_1_11);
        String ahmed = signIn("school", AHMED);
        String admin = signIn("school", SCHOOL_ADMIN);
        Map<String, String> before = fx.snapshotOf(school.clubId());

        expired(perform(ahmed, post("/me/sessions/" + school.finishedSession() + "/finish")));
        expired(perform(admin, payment(school.rider1(), "2026-12")));
        expired(perform(admin, post("/admin/members").contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "New Person", "phone": "01159999999", "slotId": "%s", "assessorId": "%s" }"""
                .formatted(school.assessmentSlot(), school.trainer1()))));
        expired(perform(ahmed, post("/me/sessions/" + school.finishedSession() + "/evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"riderId\": \"%s\", \"stars\": 5 }".formatted(school.rider1()))));
        expired(perform(ahmed, put("/me/sessions/" + school.eveningSession() + "/assignments")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"riderIds\": [] }")));

        assertThat(fx.snapshotOf(school.clubId())).isEqualTo(before);
    }

    // 4 (logout and marking a notice read: ClubAccessFilterTest; there is no logout endpoint)
    @Test
    void aReadOnlyClubCanStillRefresh() throws Exception {
        at(DEC_1_11);
        String refresh = JsonPath.read(body(fx.loginRequest("school", AHMED, PASSWORD)), "$.refreshToken");

        refresh(refresh).andExpect(status().isOk());
    }

    // 5
    @Test
    void aReadOnlyClubSignsInAndIsToldItsState() throws Exception {
        at(DEC_1_11);
        fx.loginRequest("school", AHMED, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access.state").value("READ_ONLY"))
                .andExpect(jsonPath("$.access.reason").value("SUBSCRIPTION_ENDED"))
                .andExpect(jsonPath("$.access.daysLeft").value(0))
                .andExpect(r -> assertThat(instant(r, "$.access.endsAt")).isEqualTo(DEC_1));
    }

    // 6
    @Test
    void aRenewalWorksOnTheVeryNextRequest() throws Exception {
        at(DEC_3);
        String admin = signIn("school", SCHOOL_ADMIN);
        expired(perform(admin, payment(school.rider1(), "2026-12")));

        root(() -> subscription(school.clubId(), "BASIC", "MONTHLY", DEC_3, cairo(2027, 1, 3, 10, 0)));

        perform(admin, payment(school.rider1(), "2026-12")).andExpect(status().isCreated());
    }

    // ---- blocked

    // 7
    @Test
    void afterTheTrialEndsTheClubIsBlocked() throws Exception {
        at(DEC_7_1459);
        String hany = signIn("cairo-eq", HANY);
        perform(hany, get("/admin/riders")).andExpect(status().isOk());

        at(DEC_7_1501);
        blocked(perform(hany, get("/admin/riders")), "TRIAL_ENDED");
    }

    // 8
    @Test
    void aBlockedClubCantSignInButAWrongPasswordIsStillAWrongPassword() throws Exception {
        at(DEC_7_1501);
        root(() -> jdbc.update("update admin_account set failed_attempts = 2 where email = ?", HANY));

        blocked(fx.loginRequest("cairo-eq", HANY, PASSWORD), "TRIAL_ENDED");
        // The right password changed nothing: the counter is where it was.
        assertThat(failedAttempts(HANY)).isEqualTo(2);

        fx.loginRequest("cairo-eq", HANY, "Wrong@2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    // 9
    @Test
    void aBlockedClubCantRefreshAndTheTokenStillWorksLater() throws Exception {
        at(DEC_7_1459);
        String refresh = JsonPath.read(body(fx.loginRequest("cairo-eq", HANY, PASSWORD)), "$.refreshToken");

        at(DEC_7_1501);
        blocked(refresh(refresh), "TRIAL_ENDED");

        // The row was not revoked: once the club pays, the same token refreshes.
        root(() -> {
            jdbc.update("insert into plan (code, name, on_expiry) select 'PRO', 'Pro', 'READ_ONLY' "
                    + "where not exists (select 1 from plan where code = 'PRO')");
            return subscription(cairoEq.clubId(), "PRO", "MONTHLY", DEC_7_1501, cairo(2027, 1, 7, 15, 1));
        });
        refresh(refresh).andExpect(status().isOk());
    }

    // 10
    @Test
    void aSuspendedClubIsBlockedEvenWithAValidSubscription() throws Exception {
        at(NOV_20);
        String ahmed = signIn("school", AHMED);
        root(() -> jdbc.update("update club set status = 'SUSPENDED', suspended_at = now(), "
                + "suspended_reason = 'Unpaid invoice' where id = ?", school.clubId()));

        blocked(perform(ahmed, get("/me/riders")), "SUSPENDED");

        root(() -> jdbc.update("update club set status = 'ACTIVE', suspended_at = null, suspended_reason = null "
                + "where id = ?", school.clubId()));
        perform(ahmed, get("/me/riders")).andExpect(status().isOk());
    }

    // 11
    @Test
    void aClubWithoutASubscriptionIsBlocked() throws Exception {
        at(NOV_20);
        String hany = signIn("cairo-eq", HANY);
        root(() -> jdbc.update("delete from subscription where club_id = ?", cairoEq.clubId()));

        blocked(perform(hany, get("/admin/riders")), "NO_SUBSCRIPTION");
    }

    // ---- status

    // 12
    @Test
    void everyRoleGetsTheSameClubStatus() throws Exception {
        at(NOV_20);
        String expected = null;
        for (String login : new String[] {SCHOOL_ADMIN, AHMED, SCHOOL_RIDER}) {
            ResultActions result = perform(signIn("school", login), get("/club/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clubName").value("Riding School"))
                    .andExpect(jsonPath("$.clubCode").value("school"))
                    .andExpect(jsonPath("$.state").value("ACTIVE"))
                    .andExpect(jsonPath("$.reason").isEmpty())
                    .andExpect(jsonPath("$.planName").value("Basic"))
                    .andExpect(jsonPath("$.billingPeriod").value("MONTHLY"))
                    .andExpect(jsonPath("$.daysLeft").value(11))
                    .andExpect(r -> assertThat(instant(r, "$.endsAt")).isEqualTo(DEC_1));
            String json = body(result);
            if (expected != null) {
                assertThat(json).as(login).isEqualTo(expected);
            }
            expected = json;
        }

        at(DEC_1_11);
        perform(signIn("school", AHMED), get("/club/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("READ_ONLY"))
                .andExpect(jsonPath("$.reason").value("SUBSCRIPTION_ENDED"));
    }

    // ---- what the access state doesn't touch

    // 13
    @Test
    void theSuperAdminIsNeverBlocked() throws Exception {
        at(DEC_8);
        root(() -> {
            jdbc.update("delete from subscription");
            jdbc.update("delete from super_admin");
            return jdbc.update("insert into super_admin (email, password_hash) values ('ops@equis.app', ?)",
                    new BCryptPasswordEncoder().encode("Platform@2026"));
        });
        String token = JsonPath.read(body(mvc.perform(post("/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"ops@equis.app\", \"password\": \"Platform@2026\", \"rememberMe\": false }"))
                .andExpect(status().isOk())), "$.accessToken");

        perform(token, get("/platform/clubs")).andExpect(status().isOk());
    }

    // 14
    @Test
    void sessionsAreStillGeneratedForABlockedClub() throws Exception {
        at(DEC_8);
        LocalDate today = LocalDate.of(2026, 12, 8);
        root(() -> jdbc.update("delete from session where session_date > ?", today));

        sessionJob.generateOnSchedule();

        for (UUID club : new UUID[] {school.clubId(), cairoEq.clubId()}) {
            Long upcoming = root(() -> jdbc.queryForObject(
                    "select count(*) from session where club_id = ? and session_date > ?", Long.class, club, today));
            assertThat(upcoming).as("upcoming sessions of " + club).isPositive();
        }
    }

    // 15
    @Test
    void anInviteCanStillBeAcceptedInABlockedClub() throws Exception {
        // Sent just before the trial ends (links last 72h), accepted after it.
        at(DEC_7_1459);
        recorder.clear();
        perform(signIn("cairo-eq", HANY), post("/admin/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "Late Trainer", "phone": "01052000009", "email": "late@cairo-eq.eg" }"""))
                .andExpect(status().isCreated());
        String link = RecordingNotificationSender.tokenFrom(recorder.last());

        at(cairo(2026, 12, 7, 16, 0));
        mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"%s\", \"password\": \"%s\" }".formatted(link, PASSWORD)))
                .andExpect(status().isOk());
    }

    // ---- helpers

    private void at(Instant instant) {
        ((MutableClock) clock).set(instant);
    }

    private String signIn(String clubCode, String login) throws Exception {
        ResultActions result = fx.loginRequest(clubCode, login, PASSWORD).andExpect(status().isOk());
        return JsonPath.read(body(result), "$.accessToken");
    }

    private ResultActions perform(String token, MockHttpServletRequestBuilder request) throws Exception {
        return fx.perform(token, request);
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refreshToken\": \"%s\" }".formatted(refreshToken)));
    }

    private static MockHttpServletRequestBuilder payment(UUID rider, String month) {
        return post("/admin/riders/" + rider + "/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"%s\", \"months\": 1 }"
                        .formatted(month));
    }

    private static void expired(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("subscription_expired"))
                .andExpect(r -> assertThat(instant(r, "$.endsAt")).isEqualTo(DEC_1));
    }

    private static void blocked(ResultActions result, String reason) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("club_blocked"))
                .andExpect(jsonPath("$.reason").value(reason));
    }

    private static Instant instant(org.springframework.test.web.servlet.MvcResult result, String path)
            throws Exception {
        return Instant.parse(JsonPath.read(result.getResponse().getContentAsString(), path));
    }

    private int subscription(UUID club, String plan, String period, Instant starts, Instant ends) {
        return jdbc.update("""
                insert into subscription (club_id, plan_id, billing_period, starts_at, ends_at)
                select ?, id, ?, ?, ? from plan where code = ?""",
                club, period, java.sql.Timestamp.from(starts), java.sql.Timestamp.from(ends), plan);
    }

    private Integer failedAttempts(String email) {
        return root(() -> jdbc.queryForObject("select failed_attempts from admin_account where email = ?",
                Integer.class, email));
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
    }
}
