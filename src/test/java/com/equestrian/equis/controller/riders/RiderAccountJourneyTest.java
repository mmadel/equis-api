package com.equestrian.equis.controller.riders;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.RecordingNotificationSender;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.equestrian.equis.support.TrainerAccountsFixture.ADMIN_EMAIL;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E15 rider invite, set password and sign-in: placement sends the invite, the rider sets a password and signs in
// by email with role RIDER, and the admin invites or resets from W3. Numbers are the ticket's test cases.
// Trainer email texts (case 22) are in AccountLinkEmailsTest and the trainer journeys; intake's email_in_use
// (case 17) is also in AdminMemberApiTest, AdminMemberEditApiTest and IntakeScenarioTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import({TrainerAccountsFixture.Config.class, RiderAccountJourneyTest.RiderPingController.class})
class RiderAccountJourneyTest {
    private static final String OMAR_EMAIL = "omar.h@gmail.com";
    private static final String OMAR_PHONE = "01055550001";
    private static final String PASSWORD = "Riding@2026";
    private static final String TRAINER_PASSWORD = "Coach@2026";
    private static final String RIDERS = "/admin/riders";
    // From Friday 10:00 to Saturday 16:00, well after the 10:00 slot. The slot goes in with plain SQL, and
    // hibernate.jdbc.time_zone shifts its start time by the machine's offset when read back, so leave room.
    private static final Duration AFTER_SLOT = Duration.ofHours(30);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void admin(DynamicPropertyRegistry registry) {
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
    JsonMapper json;

    private TrainerRow ahmed;

    // Only rider tokens reach it; stands in for the rider endpoints of the next ticket.
    @RestController
    static class RiderPingController {
        @GetMapping("/rider/ping")
        String ping() {
            return "pong";
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.activeTrainer("Ahmed Coach", "01012340000", "ahmed@school.eg", TRAINER_PASSWORD);
    }

    // 1
    @Test
    void placingAPersonInvitesTheNewRiderAfterCommit() throws Exception {
        Instant placedAt = START.plus(AFTER_SLOT);
        UUID omar = placeOmar();

        Map<String, Object> rider = fx.jdbc().queryForMap(
                "select email, account_status, password_hash from rider where id = ?", omar);
        assertThat(rider.get("email")).isEqualTo(OMAR_EMAIL);
        assertThat(rider.get("account_status")).isEqualTo("invited");
        assertThat(rider.get("password_hash")).isNull();

        Map<String, Object> link = fx.jdbc().queryForMap(
                "select purpose, expires_at from invite where rider_id = ? and accepted_at is null", omar);
        assertThat(link.get("purpose")).isEqualTo("invite");
        assertThat(((Timestamp) link.get("expires_at")).toInstant()).isEqualTo(placedAt.plus(Duration.ofHours(72)));

        assertThat(fx.recorder().all()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo(OMAR_EMAIL);
            assertThat(email.subject()).isEqualTo("Your rider account at Riding Schedule");
            assertThat(email.text()).contains("http://rider.test/invite/" + RecordingNotificationSender.tokenFrom(email));
        });
    }

    // 2
    @Test
    void aRolledBackPlacementLeavesNoRiderLinkOrEmail() throws Exception {
        // Fails the rider link's insert, after the rider is saved and the event is published.
        fx.jdbc().execute("""
                create or replace function fail_rider_link() returns trigger language plpgsql as
                $$ begin raise exception 'forced failure'; end $$""");
        fx.jdbc().execute("""
                create trigger fail_rider_link before insert on invite
                for each row when (new.rider_id is not null) execute function fail_rider_link()""");
        try {
            UUID booking = omarsBooking();
            fx.advance(AFTER_SLOT);
            AuthTokens coach = fx.loginTrainer("01012340000", TRAINER_PASSWORD);

            ResultActions result = fx.perform(fx.asTrainer(recordAttended(booking), coach));

            result.andExpect(status().isInternalServerError());
            assertThat(fx.count("rider")).isZero();
            assertThat(fx.jdbc().queryForObject("select count(*) from invite where rider_id is not null",
                    Integer.class)).isZero();
            assertThat(fx.recorder().all()).isEmpty();
        } finally {
            fx.jdbc().execute("drop trigger if exists fail_rider_link on invite");
            fx.jdbc().execute("drop function if exists fail_rider_link()");
        }
    }

    // 3
    @Test
    void acceptingTheLinkActivatesTheAccount() throws Exception {
        UUID omar = placeOmar();

        fx.acceptInvite(fx.tokenFromLastEmail(), PASSWORD).andExpect(status().isNoContent());

        assertThat(accountStatus(omar)).isEqualTo("active");
        assertThat(fx.jdbc().queryForObject(
                "select count(*) from invite where rider_id = ? and accepted_at is not null", Integer.class, omar))
                .isEqualTo(1);
    }

    // 4
    @Test
    void theRiderSignsInByEmailInAnyCaseWithRoleRider() throws Exception {
        UUID omar = activeOmar();

        fx.login("Omar.H@Gmail.COM", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RIDER"));

        assertThat(fx.jdbc().queryForObject("select last_active_at from rider where id = ?", Timestamp.class, omar)
                .toInstant()).isEqualTo(clockNow());
        assertThat(fx.jdbc().queryForObject(
                "select count(*) from refresh_token where subject_type = 'rider' and subject_id = ?",
                Integer.class, omar)).isEqualTo(1);
    }

    // 5
    @Test
    void beforeAcceptingWrongPasswordAndUnknownEmailAllGetTheSameAnswer() throws Exception {
        placeOmar();
        String before = detail(fx.login(OMAR_EMAIL, PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials")));

        fx.acceptInvite(fx.tokenFromLastEmail(), PASSWORD).andExpect(status().isNoContent());
        String wrong = detail(fx.login(OMAR_EMAIL, "Wrong@2026").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials")));
        String unknown = detail(fx.login("nobody@gmail.com", PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials")));

        assertThat(before).isEqualTo(wrong).isEqualTo(unknown);
    }

    // 6
    @Test
    void ridersCannotSignInWithTheirPhone() throws Exception {
        activeOmar();

        fx.login(OMAR_PHONE, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    // 7 and 8
    @Test
    void riderTokensOpenOnlyRiderEndpoints() throws Exception {
        activeOmar();
        AuthTokens omar = riderTokens();

        fx.perform(as(get("/rider/ping"), omar)).andExpect(status().isOk());
        for (String path : new String[] {"/me/riders", RIDERS, "/assessment/slots"}) {
            fx.perform(as(get(path), omar))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }

        AuthTokens coach = fx.loginTrainer("01012340000", TRAINER_PASSWORD);
        fx.perform(fx.asTrainer(get("/rider/ping"), coach)).andExpect(status().isForbidden());
        fx.admin(get("/rider/ping")).andExpect(status().isForbidden());
    }

    // 9
    @Test
    void aRiderRefreshesTheirTokens() throws Exception {
        activeOmar();
        AuthTokens omar = riderTokens();

        fx.refresh(omar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RIDER"))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    // 10
    @Test
    void aRiderWhoIsNoLongerActiveIsSignedOut() throws Exception {
        UUID omarId = activeOmar();
        AuthTokens phone = riderTokens();
        riderTokens(); // a second device

        fx.jdbc().update("update rider set account_status = 'invited' where id = ?", omarId);

        fx.perform(as(get("/rider/ping"), phone))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        fx.refresh(phone)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
        assertThat(openRiderTokens(omarId)).isZero();
    }

    // 11
    @Test
    void theAdminInvitesARiderWhoHasNoAccount() throws Exception {
        UUID sara = rider("Sara Adel", "sara.adel@gmail.com");

        fx.admin(post(RIDERS + "/" + sara + "/invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderId").value(sara.toString()))
                .andExpect(jsonPath("$.accountStatus").value("invited"))
                .andExpect(jsonPath("$.linkPurpose").value("invite"))
                .andExpect(jsonPath("$.linkExpiresAt").value(clockNow().plus(Duration.ofHours(72)).toString()));

        assertThat(accountStatus(sara)).isEqualTo("invited");
        assertThat(fx.recorder().all()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("sara.adel@gmail.com");
            assertThat(email.subject()).isEqualTo("Your rider account at Riding Schedule");
        });
    }

    // 12
    @Test
    void invitingAgainKillsTheFirstLink() throws Exception {
        UUID sara = rider("Sara Adel", "sara.adel@gmail.com");
        fx.admin(post(RIDERS + "/" + sara + "/invite")).andExpect(status().isOk());
        String first = fx.tokenFromLastEmail();

        fx.admin(post(RIDERS + "/" + sara + "/invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("invited"));
        String second = fx.tokenFromLastEmail();

        assertThat(second).isNotEqualTo(first);
        fx.acceptInvite(first, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
        fx.acceptInvite(second, PASSWORD).andExpect(status().isNoContent());
    }

    // 13
    @Test
    void inviteNeedsNoAccountAndResetNeedsAnActiveOne() throws Exception {
        UUID omar = activeOmar();
        UUID sara = rider("Sara Adel", "sara.adel@gmail.com");
        fx.admin(post(RIDERS + "/" + sara + "/invite")).andExpect(status().isOk());
        fx.recorder().clear();

        fx.admin(post(RIDERS + "/" + omar + "/invite"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_account_status"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.action").value("invite"));
        fx.admin(post(RIDERS + "/" + sara + "/reset-password"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_account_status"))
                .andExpect(jsonPath("$.status").value("invited"))
                .andExpect(jsonPath("$.action").value("reset-password"));
        assertThat(fx.recorder().all()).isEmpty();
    }

    // 14
    @Test
    void aResetKeepsTheOldPasswordUntilUsedThenSignsOutEverywhere() throws Exception {
        UUID omarId = activeOmar();
        AuthTokens before = riderTokens();

        fx.admin(post(RIDERS + "/" + omarId + "/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("active"))
                .andExpect(jsonPath("$.linkPurpose").value("reset"))
                .andExpect(jsonPath("$.linkExpiresAt").value(clockNow().plus(Duration.ofHours(24)).toString()));
        assertThat(fx.recorder().last().subject()).isEqualTo("Set a new password for Riding Schedule");
        String reset = fx.tokenFromLastEmail();

        fx.login(OMAR_EMAIL, PASSWORD).andExpect(status().isOk());

        fx.acceptInvite(reset, "Gallop@2027").andExpect(status().isNoContent());
        assertThat(openRiderTokens(omarId)).isZero();
        fx.refresh(before).andExpect(status().isUnauthorized());
        fx.login(OMAR_EMAIL, PASSWORD).andExpect(status().isUnauthorized());
        fx.login(OMAR_EMAIL, "Gallop@2027")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RIDER"));
    }

    // 15
    @Test
    void aSeedRiderWithNoEmailCannotBeInvited() throws Exception {
        UUID seed = fx.addRiders(ahmed.id(), 1, 1).getFirst();

        fx.admin(post(RIDERS + "/" + seed + "/invite"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_email"))
                .andExpect(jsonPath("$.detail").value("This rider has no email."));
        assertThat(accountStatus(seed)).isEqualTo("none");
    }

    // 16
    @Test
    void aRiderWithATrainersEmailCannotBeInvited() throws Exception {
        UUID clash = rider("Old Data", "ahmed@school.eg");

        fx.admin(post(RIDERS + "/" + clash + "/invite"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_in_use"));
        assertThat(accountStatus(clash)).isEqualTo("none");
        assertThat(fx.recorder().all()).isEmpty();
    }

    // 17
    @Test
    void intakeRefusesATrainersOrTheAdminsEmail() throws Exception {
        UUID slot = fx.addUpcomingSession(null);
        for (String email : new String[] {"Ahmed@School.eg", ADMIN_EMAIL}) {
            fx.admin(post("/admin/members").contentType(MediaType.APPLICATION_JSON).content("""
                            { "name": "Laila Hassan", "phone": "01077770000", "email": "%s",
                              "slotId": "%s", "assessorId": "%s" }""".formatted(email, slot, ahmed.id())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("email_in_use"))
                    .andExpect(jsonPath("$.detail").value("This email is already used by another account."));
        }
        assertThat(fx.count("member")).isZero();
    }

    // 18
    @Test
    void aTrainerCannotBeInvitedWithAPersonsEmail() throws Exception {
        fx.jdbc().update("insert into member (name, phone, email) values ('Laila Hassan', '01077770000', ?)",
                "laila@gmail.com");

        fx.inviteRequest("Laila Coach", "01077770001", "laila@gmail.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_in_use"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
        assertThat(fx.recorder().all()).isEmpty();
    }

    // 19
    @Test
    void w3RowsCarryTheAccountStatus() throws Exception {
        UUID omar = placeOmar();
        UUID seed = fx.addRiders(ahmed.id(), 1, 1).getFirst();

        fx.admin(get(RIDERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[?(@.id == '" + omar + "')].accountStatus", contains("invited")))
                .andExpect(jsonPath("$.riders[?(@.id == '" + seed + "')].accountStatus", contains("none")));
    }

    // 20
    @Test
    void unknownRidersAndNonAdminsAreRefused() throws Exception {
        UUID unknown = UUID.randomUUID();
        for (String action : new String[] {"/invite", "/reset-password"}) {
            fx.admin(post(RIDERS + "/" + unknown + action))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("rider_not_found"));
        }

        UUID sara = rider("Sara Adel", "sara.adel@gmail.com");
        AuthTokens coach = fx.loginTrainer("01012340000", TRAINER_PASSWORD);
        for (String action : new String[] {"/invite", "/reset-password"}) {
            fx.perform(fx.asTrainer(post(RIDERS + "/" + sara + action), coach))
                    .andExpect(status().isForbidden());
        }
        assertThat(accountStatus(sara)).isEqualTo("none");
    }

    // 21
    @Test
    void aRiderLinkExpiresAfter72Hours() throws Exception {
        UUID omar = placeOmar();
        String token = fx.tokenFromLastEmail();
        fx.advance(Duration.ofHours(73));

        fx.acceptInvite(token, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
        assertThat(accountStatus(omar)).isEqualTo("invited");
    }

    // ---- helpers ----

    // Omar booked with Ahmed tomorrow at 10:00; Ahmed records him attended at Level 1 that afternoon.
    // Returns the new rider's id; Ahmed's sign-in emails were already cleared, so only Omar's invite is recorded.
    private UUID placeOmar() throws Exception {
        UUID booking = omarsBooking();
        fx.advance(AFTER_SLOT);
        AuthTokens coach = fx.loginTrainer("01012340000", TRAINER_PASSWORD);

        String body = fx.perform(fx.asTrainer(recordAttended(booking), coach))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("placed"))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.riderId"));
    }

    private UUID activeOmar() throws Exception {
        UUID omar = placeOmar();
        fx.acceptInvite(fx.tokenFromLastEmail(), PASSWORD).andExpect(status().isNoContent());
        fx.recorder().clear();
        return omar;
    }

    private UUID omarsBooking() {
        UUID slot = fx.addUpcomingSession(null);
        UUID member = fx.jdbc().queryForObject(
                "insert into member (name, phone, email) values ('Omar Hassan', ?, ?) returning id",
                UUID.class, OMAR_PHONE, OMAR_EMAIL);
        return fx.jdbc().queryForObject(
                "insert into assessment_booking (member_id, session_id, assessor_id) values (?, ?, ?) returning id",
                UUID.class, member, slot, ahmed.id());
    }

    private static MockHttpServletRequestBuilder recordAttended(UUID booking) {
        return post("/me/bookings/" + booking + "/result").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"attended\": true, \"levelId\": 1 }");
    }

    // A rider placed before this release: an email, no account yet.
    private UUID rider(String name, String email) {
        return fx.jdbc().queryForObject(
                "insert into rider (name, level_id, trainer_id, email) values (?, 1, ?, ?) returning id",
                UUID.class, name, ahmed.id(), email);
    }

    private AuthTokens riderTokens() throws Exception {
        String body = fx.login(OMAR_EMAIL, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readValue(body, AuthTokens.class);
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, AuthTokens tokens) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken());
    }

    private String accountStatus(UUID riderId) {
        return fx.jdbc().queryForObject("select account_status from rider where id = ?", String.class, riderId);
    }

    private int openRiderTokens(UUID riderId) {
        return fx.jdbc().queryForObject("""
                select count(*) from refresh_token
                where subject_type = 'rider' and subject_id = ? and revoked_at is null""", Integer.class, riderId);
    }

    private Instant clockNow() {
        return clock.instant();
    }

    private static String detail(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.detail");
    }
}
