package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.SameDayJourneys;
import com.equestrian.equis.support.TrainerAccountsFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.race;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E13 same-day journeys 1–16 end to end, over HTTP: the admin on W1 (templates), W3 (payments, history) and the
// Overview; trainers Ahmed and Mona on Assign riders (screen 3) and My riders, with the clock moved through the day
// and across month ends. Usage only comes from finishing sessions. After every step assertSameDayInvariants() checks
// the rules that span the endpoints (scenario 17). The single rules are in each endpoint's own *ApiTest.
// The steps and the invariants are in SameDayJourneys, shared with E14's SessionEvaluationsScenarioTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(AdminOversightFixture.Config.class)
class SameDaySessionsScenarioTest {
    private static final LocalDate TUE_3_NOV = LocalDate.of(2026, 11, 3);
    private static final LocalDate SAT_7_NOV = LocalDate.of(2026, 11, 7);
    private static final LocalTime TEN = LocalTime.of(10, 0);
    private static final LocalTime FOUR_PM = LocalTime.of(16, 0);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);
    private static final LocalTime SEVEN_PM = LocalTime.of(19, 0);
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);
    private static final YearMonth JAN = YearMonth.of(2027, 1);
    private static final String NOV_EXPIRY = "2026-12-31";

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

    Coach ahmed;
    Coach mona;
    SameDayJourneys days;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        days = new SameDayJourneys(fx, clock, ahmed, mona);
        days.start();
    }

    // The races commit for real, so nothing may be left for the next class sharing this context.
    @AfterEach
    void cleanUp() {
        fx.reset();
    }

    // 1: the epic demo
    @Test
    void saraIsAddedAtFiveToFiveAndTheFinishUsesOneSession() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        UUID nov = days.pay(sara, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);

        days.at(TUE_3_NOV, LocalTime.of(16, 55));
        days.assign(ahmed, session, sara).andExpect(status().isOk());
        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.finish(ahmed, session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath("$.mySeats[0].sessionsLeft").value(7));

        days.payments().assertBalance(sara, 7, "active", NOV_EXPIRY);
        assertThat(days.payments().onMyRiders(ahmed).get(sara.toString())).containsEntry("sessionsLeft", 7);
        days.payments().history(sara)
                .andExpect(jsonPath("$.payments[0].id").value(nov.toString()))
                .andExpect(jsonPath("$.payments[0].startMonth").value("2026-11"))
                .andExpect(jsonPath("$.payments[0].usages[*].sessionId", contains(session.toString())))
                .andExpect(jsonPath("$.payments[0].usages[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$.payments[0].usages[0].trainerName").value("Ahmed Said"));
        days.overview().andExpect(jsonPath(today(session) + ".state", contains("finished")));
    }

    // 2
    @Test
    void thePickListOffersOnlyRidersWithASessionLeftToday() throws Exception {
        fourRiders();
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);

        days.session(ahmed, session)
                .andExpect(jsonPath("$.canAssign").value(true))
                .andExpect(jsonPath("$.riders[*].name", contains("Nour Adel", "Sara Ali")))
                .andExpect(jsonPath("$.riders[*].sessionsLeft", contains(1, 8)));
    }

    // 3
    @Test
    void saturdaysSessionCantBeFilledOnTuesday() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        days.pay(sara, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID saturday = days.sessionOn(SAT_7_NOV, FIVE_PM);

        days.session(ahmed, saturday)
                .andExpect(jsonPath("$.canAssign").value(false))
                .andExpect(jsonPath("$.readOnly").value(true))
                .andExpect(jsonPath("$.riders", empty()));
        days.assign(ahmed, saturday, sara)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_not_today"));
    }

    // 4
    @Test
    void aLatecomerIsAddedAfterTheStartAndUsesTheirLastSession() throws Exception {
        List<UUID> riders = days.placeRiders("Sara Ali", "Nour Adel");
        UUID sara = riders.get(0);
        UUID nour = riders.get(1);
        days.pay(sara, NOV, null);
        days.pay(nour, NOV, 1);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);
        days.at(TUE_3_NOV, LocalTime.of(16, 55));
        days.assign(ahmed, session, sara).andExpect(status().isOk());

        days.at(TUE_3_NOV, LocalTime.of(17, 10));
        days.assign(ahmed, session, sara, nour)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mySeats[*].name", contains("Nour Adel", "Sara Ali")));
        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.finish(ahmed, session).andExpect(status().isOk());

        days.payments().assertBalance(nour, 0, "awaiting_payment", null);
        UUID later = days.todaysSession(SEVEN_PM);
        days.session(ahmed, later).andExpect(jsonPath("$.riders[*].name", contains("Sara Ali")));
    }

    // 5
    @Test
    void twoSessionsTheSameDayUseTwoSessions() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        days.pay(sara, NOV, null);

        UUID first = days.ride(TUE_3_NOV, FOUR_PM, ahmed, sara);
        UUID second = days.ride(TUE_3_NOV, SIX_PM, ahmed, sara);

        days.payments().assertBalance(sara, 6, "active", NOV_EXPIRY);
        days.payments().history(sara)
                .andExpect(jsonPath("$.payments[0].used").value(2))
                .andExpect(jsonPath("$.payments[0].usages[*].sessionId",
                        contains(first.toString(), second.toString())));
    }

    // 6
    @Test
    void sessionsAreUsedFromTheOldestPackageFirst() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        UUID nov = days.pay(sara, NOV, 3);
        days.at(LocalDate.of(2026, 12, 10), TEN);
        UUID dec = days.pay(sara, DEC, null);

        UUID d10 = days.ride(LocalDate.of(2026, 12, 10), FIVE_PM, ahmed, sara);
        UUID d11 = days.ride(LocalDate.of(2026, 12, 11), FIVE_PM, ahmed, sara);
        UUID d12 = days.ride(LocalDate.of(2026, 12, 12), FOUR_PM, ahmed, sara);
        UUID d12Late = days.ride(LocalDate.of(2026, 12, 12), SIX_PM, ahmed, sara);

        days.payments().history(sara)
                .andExpect(jsonPath(pkg(nov) + ".used", contains(3)))
                .andExpect(jsonPath(pkg(nov) + ".left", contains(0)))
                .andExpect(jsonPath(pkg(nov) + ".usages[*].sessionId",
                        contains(d10.toString(), d11.toString(), d12.toString())))
                .andExpect(jsonPath(pkg(dec) + ".used", contains(1)))
                .andExpect(jsonPath(pkg(dec) + ".usages[*].sessionId", contains(d12Late.toString())));
        days.payments().assertBalance(sara, 7, "active", "2027-01-31");
    }

    // 7
    @Test
    void aNewYearsEveSessionFinishedAfterMidnightUsesThePackageValidOnItsDate() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        UUID nov = days.pay(sara, NOV, null);
        LocalDate newYearsEve = LocalDate.of(2026, 12, 31);
        days.at(newYearsEve, TEN);
        UUID jan = days.pay(sara, JAN, null);
        days.at(newYearsEve, LocalTime.of(20, 50));
        UUID session = days.todaysSession(LocalTime.of(21, 0));
        days.assign(ahmed, session, sara).andExpect(status().isOk());

        days.at(LocalDate.of(2027, 1, 1), LocalTime.of(0, 30));
        days.finish(ahmed, session).andExpect(status().isOk());

        days.payments().history(sara)
                .andExpect(jsonPath(pkg(nov) + ".usages[*].sessionId", contains(session.toString())))
                .andExpect(jsonPath(pkg(nov) + ".state", contains("expired")))
                .andExpect(jsonPath(pkg(jan) + ".used", contains(0)));
        days.payments().assertBalance(sara, 8, "active", "2027-02-28");
    }

    // 8
    @Test
    void aRiderWhoRanOutIsBackInThePickListOnceTheAdminRecordsAPayment() throws Exception {
        UUID nour = days.placeRiders("Nour Adel").getFirst();
        days.pay(nour, NOV, 1);
        days.ride(TUE_3_NOV, FIVE_PM, ahmed, nour);
        days.payments().assertBalance(nour, 0, "awaiting_payment", null);
        UUID later = days.todaysSession(SEVEN_PM);
        days.session(ahmed, later).andExpect(jsonPath("$.riders", empty()));

        days.at(TUE_3_NOV, LocalTime.of(18, 10));
        days.pay(nour, NOV, null);

        days.payments().assertBalance(nour, 8, "active", NOV_EXPIRY);
        days.session(ahmed, later)
                .andExpect(jsonPath("$.riders[*].name", contains("Nour Adel")))
                .andExpect(jsonPath("$.riders[*].sessionsLeft", contains(8)));
    }

    // 9
    @Test
    void aSeatedRiderWhosePaymentWasVoidedMustBeRemovedBeforeTheFinish() throws Exception {
        List<UUID> riders = days.placeRiders("Sara Ali", "Nour Adel");
        UUID sara = riders.get(0);
        UUID nour = riders.get(1);
        UUID saras = days.pay(sara, NOV, null);
        days.pay(nour, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);
        days.assign(ahmed, session, sara, nour).andExpect(status().isOk());

        days.at(TUE_3_NOV, LocalTime.of(17, 0));
        days.voidPayment(saras).andExpect(status().isOk());
        days.session(ahmed, session)
                .andExpect(jsonPath("$.mySeats[?(@.name == 'Sara Ali')].sessionsLeft", contains(0)));

        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.finish(ahmed, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_sessions_left"))
                .andExpect(jsonPath("$.details.riders[*].id", contains(sara.toString())));
        days.assign(ahmed, session, nour).andExpect(status().isOk());
        days.finish(ahmed, session).andExpect(status().isOk());

        days.payments().assertBalance(nour, 7, "active", NOV_EXPIRY);
        days.payments().assertBalance(sara, 0, "awaiting_payment", null);
    }

    // 10
    @Test
    void whenAhmedFinishesBothCoachesRidersUseASession() throws Exception {
        List<UUID> riders = days.placeRiders("Sara Ali", "Hala Samir");
        UUID sara = riders.get(0);
        UUID hala = riders.get(1);
        days.toMona(hala);
        days.pay(sara, NOV, null);
        days.pay(hala, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);
        days.assign(ahmed, session, sara).andExpect(status().isOk());
        days.assign(mona, session, hala).andExpect(status().isOk());

        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.finish(ahmed, session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCoachSeats").value(1));

        days.payments().assertBalance(sara, 7, "active", NOV_EXPIRY);
        days.payments().assertBalance(hala, 7, "active", NOV_EXPIRY);
        assertThat(days.payments().onMyRiders(mona).get(hala.toString())).containsEntry("sessionsLeft", 7);
        days.finish(mona, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_already_finished"));
    }

    // 11
    @Test
    void anotherCoachsRiderWithoutASessionLeftBlocksTheFinish() throws Exception {
        List<UUID> riders = days.placeRiders("Sara Ali", "Hala Samir");
        UUID sara = riders.get(0);
        UUID hala = riders.get(1);
        days.toMona(hala);
        days.pay(sara, NOV, null);
        UUID halas = days.pay(hala, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);
        days.assign(ahmed, session, sara).andExpect(status().isOk());
        days.assign(mona, session, hala).andExpect(status().isOk());
        days.voidPayment(halas).andExpect(status().isOk());

        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.finish(ahmed, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_sessions_left"))
                .andExpect(jsonPath("$.details.riders", empty()))
                .andExpect(jsonPath("$.details.otherCoachRiders").value(1));

        days.session(ahmed, session).andExpect(jsonPath("$.status").value("scheduled"));
        days.payments().assertBalance(sara, 8, "active", NOV_EXPIRY);
    }

    // 12
    @Test
    void anEmptySessionCanBeFinishedButNotSomeoneElsesSession() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        days.pay(sara, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(14, 50));
        UUID empty = days.todaysSession(LocalTime.of(15, 0));
        UUID ahmeds = days.todaysSession(FIVE_PM);

        days.at(TUE_3_NOV, LocalTime.of(16, 5));
        // Nobody to evaluate, so it is done at once (E14).
        days.finish(ahmed, empty)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.mySeats", empty()));
        days.at(TUE_3_NOV, LocalTime.of(16, 55));
        days.assign(ahmed, ahmeds, sara).andExpect(status().isOk());
        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.finish(mona, ahmeds)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("session_not_yours"));

        days.session(ahmed, ahmeds).andExpect(jsonPath("$.status").value("scheduled"));
    }

    // 13
    @Test
    void finishingBeforeTheStartAndAddingAfterTheFinishAreRefused() throws Exception {
        List<UUID> riders = days.placeRiders("Sara Ali", "Nour Adel");
        UUID sara = riders.get(0);
        UUID nour = riders.get(1);
        days.pay(sara, NOV, null);
        days.pay(nour, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);
        days.assign(ahmed, session, sara).andExpect(status().isOk());

        days.finish(ahmed, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_not_started"));
        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.finish(ahmed, session).andExpect(status().isOk());
        days.assign(ahmed, session, sara, nour)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_read_only"));

        days.payments().assertBalance(nour, 8, "active", NOV_EXPIRY);
    }

    // 14
    @Test
    void aPackageAFinishedSessionUsedCannotBeVoided() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        UUID nov = days.pay(sara, NOV, null);
        days.ride(TUE_3_NOV, FIVE_PM, ahmed, sara);

        days.voidPayment(nov)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("payment_has_usage"))
                .andExpect(jsonPath("$.details.used").value(1));

        days.payments().assertBalance(sara, 7, "active", NOV_EXPIRY);
    }

    // 15: two coaches finish at once
    @Test
    void twoFinishesAtOnceFinishTheSessionOnce() throws Exception {
        List<UUID> riders = days.placeRiders("Sara Ali", "Hala Samir");
        UUID sara = riders.get(0);
        UUID hala = riders.get(1);
        days.toMona(hala);
        days.pay(sara, NOV, null);
        days.pay(hala, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);
        days.assign(ahmed, session, sara).andExpect(status().isOk());
        days.assign(mona, session, hala).andExpect(status().isOk());
        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.signInEveryone();

        List<String> outcomes = race(
                () -> days.finishRequest(ahmed, session).andReturn(),
                () -> days.finishRequest(mona, session).andReturn());

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "session_already_finished");
        days.assertSameDayInvariants();
        days.payments().assertBalance(sara, 7, "active", NOV_EXPIRY);
        days.payments().assertBalance(hala, 7, "active", NOV_EXPIRY);
    }

    // 15: a finish and a void at once
    @Test
    void aFinishAndAVoidAtOnceNeverUseAVoidedPackage() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        UUID nov = days.pay(sara, NOV, null);
        days.at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = days.todaysSession(FIVE_PM);
        days.assign(ahmed, session, sara).andExpect(status().isOk());
        days.at(TUE_3_NOV, LocalTime.of(18, 5));
        days.signInEveryone();

        List<String> outcomes = race(
                () -> days.finishRequest(ahmed, session).andReturn(),
                () -> days.payments().voidRequest(nov, "Entered twice").andReturn());

        // Either the finish used the package first, or the void came first and left nothing to use.
        assertThat(outcomes).isIn(List.of("ok", "payment_has_usage"), List.of("rider_no_sessions_left", "ok"));
        days.assertSameDayInvariants();
    }

    // 16
    @Test
    void atHalfPastSevenTheOverviewShowsTheFinishedAndTheForgottenSession() throws Exception {
        UUID sara = days.placeRiders("Sara Ali").getFirst();
        days.pay(sara, NOV, null);
        UUID finished = days.ride(TUE_3_NOV, FIVE_PM, ahmed, sara);
        UUID forgotten = days.todaysSession(SIX_PM);
        days.assign(ahmed, forgotten, sara).andExpect(status().isOk());

        days.at(TUE_3_NOV, LocalTime.of(19, 30));

        days.overview()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsToday[*].sessionId",
                        contains(finished.toString(), forgotten.toString())))
                .andExpect(jsonPath("$.sessionsToday[*].state", contains("finished", "not_finished")));
    }

    // ---- Steps only this class uses

    // Sara (8), Nour (1 left), Omar (never paid) and Laila (December only), all Ahmed's.
    private void fourRiders() throws Exception {
        List<UUID> riders = days.placeRiders("Sara Ali", "Nour Adel", "Omar Hassan", "Laila Fathy");
        days.pay(riders.get(0), NOV, null);
        days.pay(riders.get(1), NOV, 1);
        days.pay(riders.get(3), DEC, null);
    }

    private static String pkg(UUID paymentId) {
        return "$.payments[?(@.id == '" + paymentId + "')]";
    }

    private static String today(UUID sessionId) {
        return "$.sessionsToday[?(@.sessionId == '" + sessionId + "')]";
    }
}
