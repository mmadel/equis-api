package com.equestrian.equis.controller.riders;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.RecordingNotificationSender;
import com.equestrian.equis.support.SameDayJourneys;
import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.read;
import static com.equestrian.equis.support.AdminOversightFixture.readValue;
import static com.equestrian.equis.support.AdminOversightFixture.riderRow;
import static com.equestrian.equis.support.PaymentJourneys.body;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static com.equestrian.equis.support.TrainerAccountsFixture.TRAINERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E15 rider account journeys 1–18 end to end, over HTTP: Coach Ahmed places Omar, Omar sets his password from the
// email and signs in, the admin records his payment, Ahmed rides, evaluates and promotes him, and Omar reads his
// data in the rider app at each step while the clock moves to Christmas and into 2027. The admin's side (invite,
// resend, reset and the refusals) and the walls between riders, trainers and the admin follow.
// Placement is on the Monday 2 Nov 2026 10:00 slot of SameDayJourneys; the rides are from Tuesday 3 Nov.
// After every journey step assertRiderInvariants() checks that each signed-up rider's /rider/me balance is W3's.
// The single rules are in RiderAccountJourneyTest and RiderAppApiTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(AdminOversightFixture.Config.class)
class RiderAccountsScenarioTest {
    private static final int LEVEL_ONE = 1;
    private static final int LEVEL_TWO = 2;
    private static final LocalDate MON_2_NOV = LocalDate.of(2026, 11, 2);
    private static final LocalDate TUE_3_NOV = LocalDate.of(2026, 11, 3);
    private static final LocalDate THU_5_NOV = LocalDate.of(2026, 11, 5);
    private static final LocalDate FRI_25_DEC = LocalDate.of(2026, 12, 25);
    private static final LocalDate FRI_1_JAN = LocalDate.of(2027, 1, 1);
    private static final LocalTime TEN = LocalTime.of(10, 0);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final String RIDERS = "/admin/riders";
    private static final String RIDER_SUBJECT = "Your rider account at Riding Schedule";
    private static final String PASSWORD = "Riding@2026";
    private static final String NEW_PASSWORD = "Gallop@2027";
    private static final String OMAR_NOTES = "Good balance, needs work on rising trot";
    private static final String SARA_NOTES = "Confident at the walk";

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

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
    AdminOversightFixture fx;

    @Autowired
    JsonMapper json;

    Coach ahmed;
    Coach mona;
    SameDayJourneys days;
    final Person omar = new Person("Omar Hassan", "01055550001", "omar.h@gmail.com", OMAR_NOTES);
    final Person sara = new Person("Sara Adel", "01055550002", "sara.adel@gmail.com", SARA_NOTES);
    // The riders who have set a password, with how they sign in.
    final List<RiderLogin> signedUp = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        signedUp.clear();
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        days = new SameDayJourneys(fx, clock, ahmed, mona);
        days.afterEachStep(this::assertRiderInvariants);
        days.start();
    }

    @AfterEach
    void cleanUp() {
        fx.reset();
    }

    // 1–10: Omar's life in the rider app, from placement to the new year
    @Test
    void omarIsPlacedSignsInPaysRidesIsPromotedAndRunsOut() throws Exception {
        // 1
        UUID omarId = placeAttended(ahmed, LEVEL_ONE, omar).getFirst();
        assertThat(fx.accounts().recorder().all()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo(omar.email());
            assertThat(email.subject()).isEqualTo(RIDER_SUBJECT);
            assertThat(email.text()).contains("http://rider.test/invite/");
        });
        fx.w3().andExpect(jsonPath(riderRow(omarId) + ".accountStatus", contains("invited")));

        // 2
        RiderLogin login = acceptLink(omarId, omar, fx.accounts().tokenFromLastEmail());
        login(omar.email(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RIDER"));
        fx.w3().andExpect(jsonPath(riderRow(omarId) + ".accountStatus", contains("active")));

        // 3
        me(login)
                .andExpect(jsonPath("$.id").value(omarId.toString()))
                .andExpect(jsonPath("$.status").value("awaiting_payment"))
                .andExpect(jsonPath("$.sessionsLeft").value(0))
                .andExpect(jsonPath("$.packages", empty()))
                .andExpect(jsonPath("$.banners.zeroBalance").value(true));

        // 4
        UUID november = recordPayment(omarId, NOV, 8);
        me(login)
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.sessionsLeft").value(8))
                .andExpect(jsonPath("$.nextExpiry").value("2026-12-31"))
                .andExpect(jsonPath("$.packages[*].paymentId", contains(november.toString())))
                .andExpect(jsonPath("$.banners.zeroBalance").value(false));

        // 5
        UUID first = addAndFinish(ahmed, TUE_3_NOV, FIVE_PM, LEVEL_ONE, omarId);
        evaluate(ahmed, first, omarId, 4, "Good start").andExpect(status().isOk());
        sessions(login)
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].sessionId").value(first.toString()))
                .andExpect(jsonPath("$.items[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$.items[0].start").value("17:00"))
                .andExpect(jsonPath("$.items[0].levelId").value(LEVEL_ONE))
                .andExpect(jsonPath("$.items[0].trainerName").value("Ahmed Said"))
                .andExpect(jsonPath("$.items[0].evaluation.stars").value(4))
                .andExpect(jsonPath("$.items[0].evaluation.comment").value("Good start"));
        me(login).andExpect(jsonPath("$.sessionsLeft").value(7));

        // 6
        UUID second = addAndFinish(ahmed, THU_5_NOV, FIVE_PM, LEVEL_ONE, omarId);
        sessions(login)
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].sessionId", contains(second.toString(), first.toString())))
                .andExpect(jsonPath("$.items[0].evaluation").value(nullValue()))
                .andExpect(jsonPath("$.items[1].evaluation.stars").value(4));
        me(login).andExpect(jsonPath("$.sessionsLeft").value(6));

        // 7
        fx.promote(ahmed, omarId, LEVEL_ONE).andExpect(status().isOk());
        assertRiderInvariants();
        me(login).andExpect(jsonPath("$.levelId").value(LEVEL_TWO));
        progress(login)
                .andExpect(jsonPath("$.levelHistory[*].type", contains("promoted", "placed")))
                .andExpect(jsonPath("$.levelHistory[0].fromLevelId").value(LEVEL_ONE))
                .andExpect(jsonPath("$.levelHistory[0].toLevelId").value(LEVEL_TWO))
                .andExpect(jsonPath("$.levelHistory[0].byName").value("Ahmed Said"))
                .andExpect(jsonPath("$.levelHistory[1].toLevelId").value(LEVEL_ONE));

        // 8
        days.at(FRI_25_DEC, TEN);
        me(login)
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.sessionsLeft").value(6))
                .andExpect(jsonPath("$.banners.expiringSoon.sessions").value(6))
                .andExpect(jsonPath("$.banners.expiringSoon.date").value("2026-12-31"))
                .andExpect(jsonPath("$.banners.zeroBalance").value(false));

        // 9
        days.at(FRI_1_JAN, TEN);
        me(login)
                .andExpect(jsonPath("$.status").value("awaiting_payment"))
                .andExpect(jsonPath("$.sessionsLeft").value(0))
                .andExpect(jsonPath("$.nextExpiry").value(nullValue()))
                .andExpect(jsonPath("$.packages", empty()))
                .andExpect(jsonPath("$.banners.expiringSoon").value(nullValue()))
                .andExpect(jsonPath("$.banners.zeroBalance").value(true));
        sessions(login).andExpect(jsonPath("$.items[*].sessionId", contains(second.toString(), first.toString())));
        progress(login)
                .andExpect(jsonPath("$.starTrend[*].month", contains("2026-11")))
                .andExpect(jsonPath("$.starTrend[0].averageStars").value(4.0))
                .andExpect(jsonPath("$.starTrend[0].evaluations").value(1));

        // 10
        asRider(login, get("/rider/assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-02"))
                .andExpect(jsonPath("$.start").value("10:00"))
                .andExpect(jsonPath("$.assessorName").value("Ahmed Said"))
                .andExpect(jsonPath("$.levelId").value(LEVEL_ONE))
                .andExpect(jsonPath("$.notes").value(OMAR_NOTES));
    }

    // 11
    @Test
    void saraFromBeforeTheReleaseIsInvitedTwiceAndOnlyTheSecondLinkWorks() throws Exception {
        UUID saraId = placedBeforeTheRelease(sara);

        invite(saraId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("invited"))
                .andExpect(jsonPath("$.linkPurpose").value("invite"));
        String firstLink = fx.accounts().tokenFromLastEmail();
        invite(saraId).andExpect(status().isOk());
        String secondLink = fx.accounts().tokenFromLastEmail();

        assertThat(fx.accounts().recorder().all()).hasSize(2).allSatisfy(email -> {
            assertThat(email.to()).isEqualTo(sara.email());
            assertThat(email.subject()).isEqualTo(RIDER_SUBJECT);
        });
        fx.accounts().acceptInvite(firstLink, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
        acceptLink(saraId, sara, secondLink);
        login(sara.email(), PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("RIDER"));
        fx.w3().andExpect(jsonPath(riderRow(saraId) + ".accountStatus", contains("active")));
    }

    // 12
    @Test
    void aResetKeepsTheOldPasswordUntilOmarUsesTheLink() throws Exception {
        RiderLogin login = activeOmar();
        AuthTokens before = tokens(login(omar.email(), PASSWORD).andExpect(status().isOk()));

        fx.accounts().admin(post(RIDERS + "/" + login.id() + "/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("active"))
                .andExpect(jsonPath("$.linkPurpose").value("reset"));
        assertThat(fx.accounts().recorder().last().to()).isEqualTo(omar.email());
        String reset = fx.accounts().tokenFromLastEmail();
        login(omar.email(), PASSWORD).andExpect(status().isOk());
        fx.accounts().refresh(before).andExpect(status().isOk());
        before = tokens(login(omar.email(), PASSWORD).andExpect(status().isOk()));

        fx.accounts().acceptInvite(reset, NEW_PASSWORD).andExpect(status().isNoContent());
        login.changePassword(NEW_PASSWORD);

        fx.accounts().refresh(before)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
        login(omar.email(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
        login(omar.email(), NEW_PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("RIDER"));
        me(login).andExpect(jsonPath("$.id").value(login.id().toString()));
    }

    // 13
    @Test
    void theAdminCannotInviteWithoutAnEmailOrInviteOrResetTheWrongAccount() throws Exception {
        OmarAndSara both = omarAndSara();
        RiderLogin omarLogin = both.omar();
        UUID saraId = both.sara();
        invite(saraId).andExpect(status().isOk());
        // A rider from the seed data: no member, no email.
        UUID seed = fx.accounts().addRiders(ahmed.id(), LEVEL_ONE, 1).getFirst();
        fx.accounts().recorder().clear();

        invite(seed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_email"));
        invite(omarLogin.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_account_status"))
                .andExpect(jsonPath("$.status").value("active"));
        fx.accounts().admin(post(RIDERS + "/" + saraId + "/reset-password"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_account_status"))
                .andExpect(jsonPath("$.status").value("invited"));

        assertThat(fx.accounts().recorder().all()).isEmpty();
        fx.w3()
                .andExpect(jsonPath(riderRow(seed) + ".accountStatus", contains("none")))
                .andExpect(jsonPath(riderRow(omarLogin.id()) + ".accountStatus", contains("active")))
                .andExpect(jsonPath(riderRow(saraId) + ".accountStatus", contains("invited")));
    }

    // 14
    @Test
    void oneEmailIsOneAccountAcrossPeopleAndTrainers() throws Exception {
        // Booked on Sunday, while Monday's slot is still ahead.
        newPerson("Laila Hassan", "01077770000", trainerEmail(ahmed), fx.slotOn(MON_2_NOV, TEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_in_use"));

        activeOmar();
        fx.accounts().inviteRequest("Hana Coach", "01077770001", omar.email())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_in_use"));
        assertThat(fx.accounts().recorder().all()).isEmpty();
    }

    // 15
    @Test
    void ridersTrainersAndTheAdminEachStayBehindTheirOwnDoors() throws Exception {
        RiderLogin login = activeOmar();

        for (String path : List.of("/me/riders", RIDERS, "/assessment/slots")) {
            asRider(login, get(path))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
        fx.asCoach(ahmed, get("/rider/me")).andExpect(status().isForbidden());
        fx.accounts().admin(get("/rider/me")).andExpect(status().isForbidden());
    }

    // 16
    @Test
    void saraSeesOnlyHerOwnData() throws Exception {
        OmarAndSara both = omarAndSara();
        RiderLogin omarLogin = both.omar();
        UUID saraId = both.sara();
        invite(saraId).andExpect(status().isOk());
        RiderLogin saraLogin = acceptLink(saraId, sara, fx.accounts().tokenFromLastEmail());

        UUID omarsPackage = recordPayment(omarLogin.id(), NOV, 8);
        UUID sarasPackage = recordPayment(saraId, NOV, 4);
        UUID omarsRide = addAndFinish(ahmed, TUE_3_NOV, FIVE_PM, LEVEL_ONE, omarLogin.id());
        evaluate(ahmed, omarsRide, omarLogin.id(), 4, "Good start").andExpect(status().isOk());
        UUID sarasRide = addAndFinish(ahmed, THU_5_NOV, FIVE_PM, LEVEL_ONE, saraId);
        evaluate(ahmed, sarasRide, saraId, 2, "Sara only").andExpect(status().isOk());
        fx.promote(ahmed, omarLogin.id(), LEVEL_ONE).andExpect(status().isOk());

        me(saraLogin)
                .andExpect(jsonPath("$.id").value(saraId.toString()))
                .andExpect(jsonPath("$.name").value(sara.name()))
                .andExpect(jsonPath("$.email").value(sara.email()))
                .andExpect(jsonPath("$.levelId").value(LEVEL_ONE))
                .andExpect(jsonPath("$.sessionsLeft").value(3))
                .andExpect(jsonPath("$.packages[*].paymentId", contains(sarasPackage.toString())));
        sessions(saraLogin)
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[*].sessionId", contains(sarasRide.toString())))
                .andExpect(jsonPath("$.items[*].evaluation.comment", contains("Sara only")));
        asRider(saraLogin, get("/rider/assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value(SARA_NOTES));
        asRider(saraLogin, get("/rider/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[*].paymentId", contains(sarasPackage.toString())));
        progress(saraLogin)
                .andExpect(jsonPath("$.starTrend[*].averageStars", contains(2.0)))
                .andExpect(jsonPath("$.levelHistory[*].type", contains("placed")));

        String everything = String.join("\n", body(me(saraLogin)), body(sessions(saraLogin)),
                body(asRider(saraLogin, get("/rider/assessment"))), body(asRider(saraLogin, get("/rider/payments"))),
                body(progress(saraLogin)));
        assertThat(everything).doesNotContain(omarLogin.id().toString(), omarsPackage.toString(),
                omarsRide.toString(), omar.name(), omar.email(), omar.phone(), "Good start", OMAR_NOTES);
    }

    // 17
    @Test
    void omarCannotSignInWithHisPhone() throws Exception {
        activeOmar();

        login(omar.phone(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    // 18
    @Test
    void ahmedsSignInRefreshAndSetPasswordLinkAreUnchanged() throws Exception {
        activeOmar();

        AuthTokens coach = tokens(login(ahmed.phone(), AdminOversightFixture.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TRAINER")));
        fx.accounts().refresh(coach).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("TRAINER"));
        fx.accounts().myLevels(coach).andExpect(status().isOk());

        fx.accounts().admin(post(TRAINERS + "/" + ahmed.id() + "/reset-password")).andExpect(status().isOk());
        assertThat(fx.accounts().recorder().last().subject()).isNotEqualTo(RIDER_SUBJECT);
        assertThat(fx.accounts().recorder().last().to()).isEqualTo(trainerEmail(ahmed));
        // The trainer app's link, not the rider app's.
        assertThat(fx.accounts().recorder().last().text()).contains("/invite/").doesNotContain("http://rider.test/");
        fx.accounts().acceptInvite(fx.accounts().tokenFromLastEmail(), NEW_PASSWORD)
                .andExpect(status().isNoContent());
        login(ahmed.phone(), NEW_PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("TRAINER"));
    }

    // ---- The rule that spans the endpoints

    // Every rider who has set a password signs in and reads on /rider/me the balance W3 shows for them, and W3
    // shows their account as active.
    private void assertRiderInvariants() throws Exception {
        days.assertSameDayInvariants();
        String w3 = body(fx.w3());
        for (RiderLogin login : signedUp) {
            List<Map<String, Object>> rows = JsonPath.read(w3, riderRow(login.id()));
            assertThat(rows).as("%s is on W3", login.email()).singleElement()
                    .satisfies(row -> assertThat(row.get("accountStatus")).isEqualTo("active"));
            String me = body(me(login));
            Map<String, Object> onApp = new HashMap<>();
            onApp.put("sessionsLeft", JsonPath.read(me, "$.sessionsLeft"));
            onApp.put("status", JsonPath.read(me, "$.status"));
            onApp.put("nextExpiry", JsonPath.read(me, "$.nextExpiry"));
            assertThat(onApp).as("%s: rider app balance = W3 balance", login.email())
                    .isEqualTo(rows.getFirst().get("balance"));
        }
    }

    // ---- Steps

    // A person to place: who they are and the assessor's notes.
    private record Person(String name, String phone, String email, String notes) {
    }

    // The admin books each person into Monday 2 Nov's 10:00 assessment slot with this assessor (W2's New person);
    // at 10:30 the assessor records them attended at the level with their notes (A5). The rider ids, in order.
    private List<UUID> placeAttended(Coach assessor, int levelId, Person... people) throws Exception {
        UUID slot = fx.slotOn(MON_2_NOV, TEN);
        List<UUID> bookings = new ArrayList<>();
        for (Person person : people) {
            bookings.add(UUID.fromString(read(newPerson(person.name(), person.phone(), person.email(), slot)
                    .andExpect(status().isCreated()), "$.id")));
        }
        fx.at(MON_2_NOV, LocalTime.of(10, 30));
        List<UUID> riders = new ArrayList<>();
        for (int i = 0; i < people.length; i++) {
            ResultActions placed = fx.asCoach(assessor, post("/me/bookings/" + bookings.get(i) + "/result")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"attended\": true, \"levelId\": %d, \"notes\": \"%s\" }"
                            .formatted(levelId, people[i].notes())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("placed"));
            riders.add(UUID.fromString(read(placed, "$.riderId")));
        }
        return riders;
    }

    // W2's New person, whatever the answer.
    private ResultActions newPerson(String name, String phone, String email, UUID slotId) throws Exception {
        return fx.accounts().admin(post("/admin/members").contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "%s", "phone": "%s", "email": "%s", "slotId": "%s", "assessorId": "%s" }"""
                .formatted(name, phone, email, slotId, ahmed.id())));
    }

    // Omar placed at Level 1 by Ahmed, with his password set from the invite email. The emails are cleared.
    private RiderLogin activeOmar() throws Exception {
        UUID id = placeAttended(ahmed, LEVEL_ONE, omar).getFirst();
        RiderLogin login = acceptLink(id, omar, fx.accounts().tokenFromLastEmail());
        fx.accounts().recorder().clear();
        return login;
    }

    // Placed at Level 1 by Ahmed, then turned into a rider from before this release.
    private UUID placedBeforeTheRelease(Person person) throws Exception {
        UUID id = placeAttended(ahmed, LEVEL_ONE, person).getFirst();
        asBeforeTheRelease(id);
        return id;
    }

    // Omar and Sara placed in the same slot (it can't be booked once it has started): Omar signs up from his
    // invite, Sara becomes a rider from before this release. The emails are cleared.
    private OmarAndSara omarAndSara() throws Exception {
        List<UUID> ids = placeAttended(ahmed, LEVEL_ONE, omar, sara);
        RiderLogin omarLogin = acceptLink(ids.get(0), omar, tokenSentTo(omar));
        asBeforeTheRelease(ids.get(1));
        return new OmarAndSara(omarLogin, ids.get(1));
    }

    private record OmarAndSara(RiderLogin omar, UUID sara) {
    }

    // A rider as they were before this release: no app account and no link. Placement now sends the invite, so
    // the link is dropped with SQL; there is no endpoint for the old state. The emails are cleared.
    private void asBeforeTheRelease(UUID riderId) {
        fx.jdbc().update("delete from invite where rider_id = ?", riderId);
        fx.jdbc().update("update rider set account_status = 'none' where id = ?", riderId);
        fx.accounts().recorder().clear();
    }

    // The link in the last email sent to this person.
    private String tokenSentTo(Person person) {
        return fx.accounts().recorder().all().reversed().stream()
                .filter(email -> email.to().equals(person.email()))
                .findFirst()
                .map(RecordingNotificationSender::tokenFrom)
                .orElseThrow(() -> new AssertionError("No email to " + person.email()));
    }

    // The trainer's email as W4 lists it.
    private String trainerEmail(Coach coach) throws Exception {
        List<String> emails = readValue(fx.accounts().list(), TrainerAccountsFixture.rowPath(coach.id()) + ".email");
        return emails.getFirst();
    }

    // The rider app's set-password page, with the token from the email; the rider counts as signed up.
    private RiderLogin acceptLink(UUID riderId, Person person, String token) throws Exception {
        fx.accounts().acceptInvite(token, PASSWORD).andExpect(status().isNoContent());
        RiderLogin login = new RiderLogin(riderId, person.email(), PASSWORD);
        signedUp.add(login);
        return login;
    }

    private ResultActions login(String login, String password) throws Exception {
        return fx.accounts().login(login, password);
    }

    private ResultActions invite(UUID riderId) throws Exception {
        return fx.accounts().admin(post(RIDERS + "/" + riderId + "/invite"));
    }

    // W3's Record payment: one month, 1,200 EGP cash. The payment id.
    private UUID recordPayment(UUID riderId, YearMonth month, int sessions) throws Exception {
        return days.pay(riderId, month, sessions);
    }

    // The trainer opens the level's session on that day, adds the riders five minutes before the start and
    // finishes it 65 minutes after.
    private UUID addAndFinish(Coach trainer, LocalDate date, LocalTime start, int levelId, UUID... riderIds)
            throws Exception {
        return days.ride(date, start, levelId, trainer, riderIds);
    }

    // Screen 3's evaluation sheet.
    private ResultActions evaluate(Coach trainer, UUID sessionId, UUID riderId, int stars, String comment)
            throws Exception {
        ResultActions result = fx.asCoach(trainer, post("/me/sessions/" + sessionId + "/evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"riderId\": \"%s\", \"stars\": %d, \"comment\": \"%s\" }"
                        .formatted(riderId, stars, comment)));
        assertRiderInvariants();
        return result;
    }

    // ---- The rider app

    private ResultActions me(RiderLogin login) throws Exception {
        return asRider(login, get("/rider/me")).andExpect(status().isOk());
    }

    private ResultActions sessions(RiderLogin login) throws Exception {
        return asRider(login, get("/rider/sessions")).andExpect(status().isOk());
    }

    private ResultActions progress(RiderLogin login) throws Exception {
        return asRider(login, get("/rider/progress")).andExpect(status().isOk());
    }

    private ResultActions asRider(RiderLogin login, MockHttpServletRequestBuilder request) throws Exception {
        return fx.accounts().perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + riderToken(login)));
    }

    // The rider's access token, signing in again once the clock has passed its expiry.
    private String riderToken(RiderLogin login) throws Exception {
        if (login.tokens == null || !clock.instant().isBefore(login.expiresAt)) {
            login.tokens = tokens(login(login.email(), login.password).andExpect(status().isOk()));
            login.expiresAt = clock.instant().plusSeconds(login.tokens.expiresIn());
        }
        return login.tokens.accessToken();
    }

    private AuthTokens tokens(ResultActions result) throws Exception {
        return json.readValue(body(result), AuthTokens.class);
    }

    // How a signed-up rider signs in, with the tokens of their last sign-in.
    private static final class RiderLogin {
        private final UUID id;
        private final String email;
        private String password;
        private AuthTokens tokens;
        private Instant expiresAt;

        RiderLogin(UUID id, String email, String password) {
            this.id = id;
            this.email = email;
            this.password = password;
        }

        UUID id() {
            return id;
        }

        String email() {
            return email;
        }

        // A new password signs out everywhere, so the next call signs in again.
        void changePassword(String password) {
            this.password = password;
            this.tokens = null;
        }
    }
}
