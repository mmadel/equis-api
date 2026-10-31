package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.PaymentJourneys;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.race;
import static com.equestrian.equis.support.PaymentJourneys.body;
import static com.equestrian.equis.support.PaymentJourneys.paymentId;
import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E13 same-day journeys 1–16 end to end, over HTTP: the admin on W1 (templates), W3 (payments, history) and the
// Overview; trainers Ahmed and Mona on Assign riders (screen 3) and My riders, with the clock moved through the day
// and across month ends. Usage only comes from finishing sessions. After every step assertSameDayInvariants() checks
// the rules that span the endpoints (scenario 17). The single rules are in each endpoint's own *ApiTest.
// Riders are placed at Level 2 from a Monday 10:00 assessment slot on Monday 2 Nov 2026 with Ahmed as assessor, so
// they start as his; Mona's riders are moved to her on W3. Each level session gets a template of its own, created
// on W1 a few minutes before it starts.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(AdminOversightFixture.Config.class)
class SameDaySessionsScenarioTest {
    private static final int LEVEL_TWO = 2;
    private static final LocalDate SUN_1_NOV = LocalDate.of(2026, 11, 1);
    private static final LocalDate MON_2_NOV = LocalDate.of(2026, 11, 2);
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
    AdminOversightFixture fx;

    Coach ahmed;
    Coach mona;
    UUID mondaySlot;
    PaymentJourneys payments;
    // Every level session the scenario opened, for the pick-list invariant.
    final List<UUID> opened = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        opened.clear();
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        payments = new PaymentJourneys(fx, ahmed, mona);
        fx.at(SUN_1_NOV, TEN);
        assessmentSlotsOnMondays();
        mondaySlot = fx.slotOn(MON_2_NOV, TEN);
    }

    // The races commit for real, so nothing may be left for the next class sharing this context.
    @AfterEach
    void cleanUp() {
        fx.reset();
    }

    // 1: the epic demo
    @Test
    void saraIsAddedAtFiveToFiveAndTheFinishUsesOneSession() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        UUID nov = pay(sara, NOV, null);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);

        at(TUE_3_NOV, LocalTime.of(16, 55));
        assign(ahmed, session, sara).andExpect(status().isOk());
        at(TUE_3_NOV, LocalTime.of(18, 5));
        finish(ahmed, session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath("$.mySeats[0].sessionsLeft").value(7));

        payments.assertBalance(sara, 7, "active", NOV_EXPIRY);
        assertThat(payments.onMyRiders(ahmed).get(sara.toString())).containsEntry("sessionsLeft", 7);
        payments.history(sara)
                .andExpect(jsonPath("$.payments[0].id").value(nov.toString()))
                .andExpect(jsonPath("$.payments[0].startMonth").value("2026-11"))
                .andExpect(jsonPath("$.payments[0].usages[*].sessionId", contains(session.toString())))
                .andExpect(jsonPath("$.payments[0].usages[0].date").value("2026-11-03"))
                .andExpect(jsonPath("$.payments[0].usages[0].trainerName").value("Ahmed Said"));
        overview().andExpect(jsonPath(today(session) + ".state", contains("finished")));
    }

    // 2
    @Test
    void thePickListOffersOnlyRidersWithASessionLeftToday() throws Exception {
        fourRiders();
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);

        session(ahmed, session)
                .andExpect(jsonPath("$.canAssign").value(true))
                .andExpect(jsonPath("$.riders[*].name", contains("Nour Adel", "Sara Ali")))
                .andExpect(jsonPath("$.riders[*].sessionsLeft", contains(1, 8)));
    }

    // 3
    @Test
    void saturdaysSessionCantBeFilledOnTuesday() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        pay(sara, NOV, null);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID saturday = sessionOn(SAT_7_NOV, FIVE_PM);

        session(ahmed, saturday)
                .andExpect(jsonPath("$.canAssign").value(false))
                .andExpect(jsonPath("$.readOnly").value(true))
                .andExpect(jsonPath("$.riders", empty()));
        assign(ahmed, saturday, sara)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_not_today"));
    }

    // 4
    @Test
    void aLatecomerIsAddedAfterTheStartAndUsesTheirLastSession() throws Exception {
        List<UUID> riders = placeRiders("Sara Ali", "Nour Adel");
        UUID sara = riders.get(0);
        UUID nour = riders.get(1);
        pay(sara, NOV, null);
        pay(nour, NOV, 1);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);
        at(TUE_3_NOV, LocalTime.of(16, 55));
        assign(ahmed, session, sara).andExpect(status().isOk());

        at(TUE_3_NOV, LocalTime.of(17, 10));
        assign(ahmed, session, sara, nour)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mySeats[*].name", contains("Nour Adel", "Sara Ali")));
        at(TUE_3_NOV, LocalTime.of(18, 5));
        finish(ahmed, session).andExpect(status().isOk());

        payments.assertBalance(nour, 0, "awaiting_payment", null);
        UUID later = todaysSession(SEVEN_PM);
        session(ahmed, later).andExpect(jsonPath("$.riders[*].name", contains("Sara Ali")));
    }

    // 5
    @Test
    void twoSessionsTheSameDayUseTwoSessions() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        pay(sara, NOV, null);

        UUID first = ride(TUE_3_NOV, FOUR_PM, ahmed, sara);
        UUID second = ride(TUE_3_NOV, SIX_PM, ahmed, sara);

        payments.assertBalance(sara, 6, "active", NOV_EXPIRY);
        payments.history(sara)
                .andExpect(jsonPath("$.payments[0].used").value(2))
                .andExpect(jsonPath("$.payments[0].usages[*].sessionId",
                        contains(first.toString(), second.toString())));
    }

    // 6
    @Test
    void sessionsAreUsedFromTheOldestPackageFirst() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        UUID nov = pay(sara, NOV, 3);
        at(LocalDate.of(2026, 12, 10), TEN);
        UUID dec = pay(sara, DEC, null);

        UUID d10 = ride(LocalDate.of(2026, 12, 10), FIVE_PM, ahmed, sara);
        UUID d11 = ride(LocalDate.of(2026, 12, 11), FIVE_PM, ahmed, sara);
        UUID d12 = ride(LocalDate.of(2026, 12, 12), FOUR_PM, ahmed, sara);
        UUID d12Late = ride(LocalDate.of(2026, 12, 12), SIX_PM, ahmed, sara);

        payments.history(sara)
                .andExpect(jsonPath(pkg(nov) + ".used", contains(3)))
                .andExpect(jsonPath(pkg(nov) + ".left", contains(0)))
                .andExpect(jsonPath(pkg(nov) + ".usages[*].sessionId",
                        contains(d10.toString(), d11.toString(), d12.toString())))
                .andExpect(jsonPath(pkg(dec) + ".used", contains(1)))
                .andExpect(jsonPath(pkg(dec) + ".usages[*].sessionId", contains(d12Late.toString())));
        payments.assertBalance(sara, 7, "active", "2027-01-31");
    }

    // 7
    @Test
    void aNewYearsEveSessionFinishedAfterMidnightUsesThePackageValidOnItsDate() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        UUID nov = pay(sara, NOV, null);
        LocalDate newYearsEve = LocalDate.of(2026, 12, 31);
        at(newYearsEve, TEN);
        UUID jan = pay(sara, JAN, null);
        at(newYearsEve, LocalTime.of(20, 50));
        UUID session = todaysSession(LocalTime.of(21, 0));
        assign(ahmed, session, sara).andExpect(status().isOk());

        at(LocalDate.of(2027, 1, 1), LocalTime.of(0, 30));
        finish(ahmed, session).andExpect(status().isOk());

        payments.history(sara)
                .andExpect(jsonPath(pkg(nov) + ".usages[*].sessionId", contains(session.toString())))
                .andExpect(jsonPath(pkg(nov) + ".state", contains("expired")))
                .andExpect(jsonPath(pkg(jan) + ".used", contains(0)));
        payments.assertBalance(sara, 8, "active", "2027-02-28");
    }

    // 8
    @Test
    void aRiderWhoRanOutIsBackInThePickListOnceTheAdminRecordsAPayment() throws Exception {
        UUID nour = placeRiders("Nour Adel").getFirst();
        pay(nour, NOV, 1);
        ride(TUE_3_NOV, FIVE_PM, ahmed, nour);
        payments.assertBalance(nour, 0, "awaiting_payment", null);
        UUID later = todaysSession(SEVEN_PM);
        session(ahmed, later).andExpect(jsonPath("$.riders", empty()));

        at(TUE_3_NOV, LocalTime.of(18, 10));
        pay(nour, NOV, null);

        payments.assertBalance(nour, 8, "active", NOV_EXPIRY);
        session(ahmed, later)
                .andExpect(jsonPath("$.riders[*].name", contains("Nour Adel")))
                .andExpect(jsonPath("$.riders[*].sessionsLeft", contains(8)));
    }

    // 9
    @Test
    void aSeatedRiderWhosePaymentWasVoidedMustBeRemovedBeforeTheFinish() throws Exception {
        List<UUID> riders = placeRiders("Sara Ali", "Nour Adel");
        UUID sara = riders.get(0);
        UUID nour = riders.get(1);
        UUID saras = pay(sara, NOV, null);
        pay(nour, NOV, null);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);
        assign(ahmed, session, sara, nour).andExpect(status().isOk());

        at(TUE_3_NOV, LocalTime.of(17, 0));
        voidPayment(saras).andExpect(status().isOk());
        session(ahmed, session)
                .andExpect(jsonPath("$.mySeats[?(@.name == 'Sara Ali')].sessionsLeft", contains(0)));

        at(TUE_3_NOV, LocalTime.of(18, 5));
        finish(ahmed, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_sessions_left"))
                .andExpect(jsonPath("$.details.riders[*].id", contains(sara.toString())));
        assign(ahmed, session, nour).andExpect(status().isOk());
        finish(ahmed, session).andExpect(status().isOk());

        payments.assertBalance(nour, 7, "active", NOV_EXPIRY);
        payments.assertBalance(sara, 0, "awaiting_payment", null);
    }

    // 10
    @Test
    void whenAhmedFinishesBothCoachesRidersUseASession() throws Exception {
        List<UUID> riders = placeRiders("Sara Ali", "Hala Samir");
        UUID sara = riders.get(0);
        UUID hala = riders.get(1);
        toMona(hala);
        pay(sara, NOV, null);
        pay(hala, NOV, null);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);
        assign(ahmed, session, sara).andExpect(status().isOk());
        assign(mona, session, hala).andExpect(status().isOk());

        at(TUE_3_NOV, LocalTime.of(18, 5));
        finish(ahmed, session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCoachSeats").value(1));

        payments.assertBalance(sara, 7, "active", NOV_EXPIRY);
        payments.assertBalance(hala, 7, "active", NOV_EXPIRY);
        assertThat(payments.onMyRiders(mona).get(hala.toString())).containsEntry("sessionsLeft", 7);
        finish(mona, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_already_finished"));
    }

    // 11
    @Test
    void anotherCoachsRiderWithoutASessionLeftBlocksTheFinish() throws Exception {
        List<UUID> riders = placeRiders("Sara Ali", "Hala Samir");
        UUID sara = riders.get(0);
        UUID hala = riders.get(1);
        toMona(hala);
        pay(sara, NOV, null);
        UUID halas = pay(hala, NOV, null);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);
        assign(ahmed, session, sara).andExpect(status().isOk());
        assign(mona, session, hala).andExpect(status().isOk());
        voidPayment(halas).andExpect(status().isOk());

        at(TUE_3_NOV, LocalTime.of(18, 5));
        finish(ahmed, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rider_no_sessions_left"))
                .andExpect(jsonPath("$.details.riders", empty()))
                .andExpect(jsonPath("$.details.otherCoachRiders").value(1));

        session(ahmed, session).andExpect(jsonPath("$.status").value("scheduled"));
        payments.assertBalance(sara, 8, "active", NOV_EXPIRY);
    }

    // 12
    @Test
    void anEmptySessionCanBeFinishedButNotSomeoneElsesSession() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        pay(sara, NOV, null);
        at(TUE_3_NOV, LocalTime.of(14, 50));
        UUID empty = todaysSession(LocalTime.of(15, 0));
        UUID ahmeds = todaysSession(FIVE_PM);

        at(TUE_3_NOV, LocalTime.of(16, 5));
        finish(ahmed, empty)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath("$.mySeats", empty()));
        at(TUE_3_NOV, LocalTime.of(16, 55));
        assign(ahmed, ahmeds, sara).andExpect(status().isOk());
        at(TUE_3_NOV, LocalTime.of(18, 5));
        finish(mona, ahmeds)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("session_not_yours"));

        session(ahmed, ahmeds).andExpect(jsonPath("$.status").value("scheduled"));
    }

    // 13
    @Test
    void finishingBeforeTheStartAndAddingAfterTheFinishAreRefused() throws Exception {
        List<UUID> riders = placeRiders("Sara Ali", "Nour Adel");
        UUID sara = riders.get(0);
        UUID nour = riders.get(1);
        pay(sara, NOV, null);
        pay(nour, NOV, null);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);
        assign(ahmed, session, sara).andExpect(status().isOk());

        finish(ahmed, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_not_started"));
        at(TUE_3_NOV, LocalTime.of(18, 5));
        finish(ahmed, session).andExpect(status().isOk());
        assign(ahmed, session, sara, nour)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_read_only"));

        payments.assertBalance(nour, 8, "active", NOV_EXPIRY);
    }

    // 14
    @Test
    void aPackageAFinishedSessionUsedCannotBeVoided() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        UUID nov = pay(sara, NOV, null);
        ride(TUE_3_NOV, FIVE_PM, ahmed, sara);

        voidPayment(nov)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("payment_has_usage"))
                .andExpect(jsonPath("$.details.used").value(1));

        payments.assertBalance(sara, 7, "active", NOV_EXPIRY);
    }

    // 15: two coaches finish at once
    @Test
    void twoFinishesAtOnceFinishTheSessionOnce() throws Exception {
        List<UUID> riders = placeRiders("Sara Ali", "Hala Samir");
        UUID sara = riders.get(0);
        UUID hala = riders.get(1);
        toMona(hala);
        pay(sara, NOV, null);
        pay(hala, NOV, null);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);
        assign(ahmed, session, sara).andExpect(status().isOk());
        assign(mona, session, hala).andExpect(status().isOk());
        at(TUE_3_NOV, LocalTime.of(18, 5));
        signInEveryone();

        List<String> outcomes = race(
                () -> finishRequest(ahmed, session).andReturn(),
                () -> finishRequest(mona, session).andReturn());

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "session_already_finished");
        assertSameDayInvariants();
        payments.assertBalance(sara, 7, "active", NOV_EXPIRY);
        payments.assertBalance(hala, 7, "active", NOV_EXPIRY);
    }

    // 15: a finish and a void at once
    @Test
    void aFinishAndAVoidAtOnceNeverUseAVoidedPackage() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        UUID nov = pay(sara, NOV, null);
        at(TUE_3_NOV, LocalTime.of(16, 50));
        UUID session = todaysSession(FIVE_PM);
        assign(ahmed, session, sara).andExpect(status().isOk());
        at(TUE_3_NOV, LocalTime.of(18, 5));
        signInEveryone();

        List<String> outcomes = race(
                () -> finishRequest(ahmed, session).andReturn(),
                () -> payments.voidRequest(nov, "Entered twice").andReturn());

        // Either the finish used the package first, or the void came first and left nothing to use.
        assertThat(outcomes).isIn(List.of("ok", "payment_has_usage"), List.of("rider_no_sessions_left", "ok"));
        assertSameDayInvariants();
    }

    // 16
    @Test
    void atHalfPastSevenTheOverviewShowsTheFinishedAndTheForgottenSession() throws Exception {
        UUID sara = placeRiders("Sara Ali").getFirst();
        pay(sara, NOV, null);
        UUID finished = ride(TUE_3_NOV, FIVE_PM, ahmed, sara);
        UUID forgotten = todaysSession(SIX_PM);
        assign(ahmed, forgotten, sara).andExpect(status().isOk());

        at(TUE_3_NOV, LocalTime.of(19, 30));

        overview()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsToday[*].sessionId",
                        contains(finished.toString(), forgotten.toString())))
                .andExpect(jsonPath("$.sessionsToday[*].state", contains("finished", "not_finished")));
    }

    // ---- 17: the rules that span the endpoints

    // E12's payment invariants; every finished session has exactly one usage per seated rider, from a package of
    // that rider valid on the session's date, and an unfinished one none; no usage is on a voided package; every
    // rider a trainer's pick list offers has a session left on the session's date, and on today's sessions the
    // pick list and the seats show the same sessionsLeft as W3.
    private void assertSameDayInvariants() throws Exception {
        payments.assertPaymentInvariants();

        List<Map<String, Object>> sessions = fx.jdbc().queryForList("""
                select s.id, s.status,
                       (select count(*) from assignment a where a.session_id = s.id) as seated,
                       (select count(*) from session_usage u where u.session_id = s.id) as used,
                       (select count(*) from assignment a where a.session_id = s.id and not exists (
                            select 1 from session_usage u where u.session_id = s.id and u.rider_id = a.rider_id))
                           as seated_without_usage
                from session s""");
        for (Map<String, Object> s : sessions) {
            long seated = ((Number) s.get("seated")).longValue();
            long used = ((Number) s.get("used")).longValue();
            if ("finished".equals(s.get("status"))) {
                assertThat(used).as("finished session %s: one usage per seated rider", s.get("id")).isEqualTo(seated);
                assertThat(((Number) s.get("seated_without_usage")).longValue())
                        .as("finished session %s: every seated rider has a usage", s.get("id")).isZero();
            } else {
                assertThat(used).as("unfinished session %s uses nothing", s.get("id")).isZero();
            }
        }
        assertThat(fx.jdbc().queryForObject("""
                select count(*) from session_usage u join payment p on p.id = u.payment_id
                where p.voided_at is not null""", Integer.class)).as("usages on a voided package").isZero();
        assertThat(fx.jdbc().queryForObject("""
                select count(*) from session_usage u
                join payment p on p.id = u.payment_id
                join session s on s.id = u.session_id
                where s.session_date < p.start_month or s.session_date > p.valid_until""", Integer.class))
                .as("usages from a package not valid on the session's date").isZero();

        Map<String, Integer> w3 = w3SessionsLeft();
        LocalDate today = LocalDate.ofInstant(clock.instant(), CAIRO);
        for (UUID id : opened) {
            for (Coach coach : List.of(ahmed, mona)) {
                String detail = body(session(coach, id));
                boolean isToday = today.toString().equals(JsonPath.read(detail, "$.date"));
                List<Map<String, Object>> offered = JsonPath.read(detail, "$.riders");
                for (Map<String, Object> rider : offered) {
                    int left = (Integer) rider.get("sessionsLeft");
                    assertThat(left).as("%s offers %s with a session left", coach.name(), rider.get("name"))
                            .isPositive();
                    if (isToday) {
                        assertThat(left).as("pick list = W3 for %s", rider.get("name"))
                                .isEqualTo(w3.get((String) rider.get("id")));
                    }
                }
                if (isToday) {
                    List<Map<String, Object>> seats = JsonPath.read(detail, "$.mySeats");
                    for (Map<String, Object> seat : seats) {
                        assertThat(seat.get("sessionsLeft")).as("seat = W3 for %s", seat.get("name"))
                                .isEqualTo(w3.get((String) seat.get("riderId")));
                    }
                }
            }
        }
    }

    // ---- Steps

    // Admin adds each person into the Monday slot with Ahmed as assessor; the clock moves past its start and Ahmed
    // places them all at Level 2. The rider ids, in the order of the names.
    private List<UUID> placeRiders(String... names) throws Exception {
        List<UUID> riders = fx.placedRiders(ahmed, mondaySlot, LEVEL_TWO, names);
        assertSameDayInvariants();
        return riders;
    }

    // Sara (8), Nour (1 left), Omar (never paid) and Laila (December only), all Ahmed's.
    private void fourRiders() throws Exception {
        List<UUID> riders = placeRiders("Sara Ali", "Nour Adel", "Omar Hassan", "Laila Fathy");
        pay(riders.get(0), NOV, null);
        pay(riders.get(1), NOV, 1);
        pay(riders.get(3), DEC, null);
    }

    // The admin moves the rider to Mona on W3.
    private void toMona(UUID rider) throws Exception {
        fx.assignRiders(List.of(rider), mona.id()).andExpect(status().isOk());
        assertSameDayInvariants();
    }

    // That moment in school time.
    private void at(LocalDate date, LocalTime time) throws Exception {
        fx.at(date, time);
        assertSameDayInvariants();
    }

    // A one-month 1,200 EGP cash package; sessions null means the default 8. The payment id.
    private UUID pay(UUID riderId, YearMonth start, Integer sessions) throws Exception {
        // A minute apart, so the history's newest-first order is well defined.
        fx.accounts().advance(Duration.ofMinutes(1));
        UUID id = paymentId(payments.payRequest(riderId, "1200", start, 1, sessions).andExpect(status().isCreated()));
        assertSameDayInvariants();
        return id;
    }

    private ResultActions voidPayment(UUID paymentId) throws Exception {
        fx.accounts().advance(Duration.ofMinutes(1));
        ResultActions result = payments.voidRequest(paymentId, "Entered by mistake");
        assertSameDayInvariants();
        return result;
    }

    // Today's Level 2 session starting then, one hour, on a W1 template of its own.
    private UUID todaysSession(LocalTime start) throws Exception {
        return sessionOn(LocalDate.ofInstant(clock.instant(), CAIRO), start);
    }

    // The Level 2 session on that date, one hour, on a W1 template of its own valid from that date.
    private UUID sessionOn(LocalDate date, LocalTime start) throws Exception {
        fx.accounts().admin(post("/admin/session-templates").contentType(MediaType.APPLICATION_JSON).content("""
                { "levelId": %d, "dayOfWeek": "%s", "start": "%s", "end": "%s", "capacity": 6, "validFrom": "%s" }"""
                .formatted(LEVEL_TWO, date.getDayOfWeek(), start, start.plusHours(1), date)))
                .andExpect(status().isCreated());
        UUID session = fx.slotOn(date, start);
        opened.add(session);
        assertSameDayInvariants();
        return session;
    }

    // Screen 3's save: the riders become this trainer's whole list for the session.
    private ResultActions assign(Coach coach, UUID sessionId, UUID... riderIds) throws Exception {
        ResultActions result = fx.assignToSession(coach, sessionId, List.of(riderIds));
        assertSameDayInvariants();
        return result;
    }

    private ResultActions finish(Coach coach, UUID sessionId) throws Exception {
        ResultActions result = finishRequest(coach, sessionId);
        assertSameDayInvariants();
        return result;
    }

    private ResultActions finishRequest(Coach coach, UUID sessionId) throws Exception {
        return fx.asCoach(coach, post("/me/sessions/" + sessionId + "/finish"));
    }

    // A whole session on that day: opened and filled five minutes before the start, finished 65 minutes after it.
    private UUID ride(LocalDate date, LocalTime start, Coach coach, UUID... riderIds) throws Exception {
        at(date, start.minusMinutes(5));
        UUID session = todaysSession(start);
        assign(coach, session, riderIds).andExpect(status().isOk());
        at(date, start.plusMinutes(65));
        finish(coach, session).andExpect(status().isOk());
        return session;
    }

    // ---- What the screens show

    // Screen 3.
    private ResultActions session(Coach coach, UUID sessionId) throws Exception {
        return fx.session(coach, sessionId).andExpect(status().isOk());
    }

    private ResultActions overview() throws Exception {
        return fx.accounts().admin(get("/admin/overview")).andExpect(status().isOk());
    }

    // Each rider on W3 with their sessions left today.
    private Map<String, Integer> w3SessionsLeft() throws Exception {
        List<Map<String, Object>> rows = JsonPath.read(body(fx.w3()), "$.riders");
        Map<String, Integer> left = new HashMap<>();
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> balance = (Map<String, Object>) row.get("balance");
            left.put((String) row.get("id"), (Integer) balance.get("sessionsLeft"));
        }
        return left;
    }

    private static String pkg(UUID paymentId) {
        return "$.payments[?(@.id == '" + paymentId + "')]";
    }

    private static String today(UUID sessionId) {
        return "$.sessionsToday[?(@.sessionId == '" + sessionId + "')]";
    }

    // ---- Helpers

    // Monday 10:00–11:00 assessments for 12, from today, so the slot on 2 Nov exists.
    private void assessmentSlotsOnMondays() throws Exception {
        fx.accounts().admin(post("/admin/session-templates").contentType(MediaType.APPLICATION_JSON).content("""
                { "levelId": null, "dayOfWeek": "MONDAY", "start": "10:00", "end": "11:00", "capacity": 12,
                  "validFrom": "%s" }""".formatted(SUN_1_NOV))).andExpect(status().isCreated());
    }

    // The race threads share the cached tokens, so everyone signs in first.
    private void signInEveryone() throws Exception {
        fx.accounts().adminToken();
        fx.token(ahmed);
        fx.token(mona);
    }
}
