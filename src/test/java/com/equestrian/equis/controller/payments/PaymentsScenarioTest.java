package com.equestrian.equis.controller.payments;

import com.equestrian.equis.entity.payments.SessionUsage;
import com.equestrian.equis.repository.payments.PaymentRepository;
import com.equestrian.equis.repository.payments.SessionUsageRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.service.payments.RiderBalanceService;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.race;
import static com.equestrian.equis.support.AdminOversightFixture.read;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E12 payment journeys 1–15 end to end, over HTTP: the admin on W3 (record, void, history, the list) and the
// trainers on My riders, with the clock moved across month ends. Session usage goes in with SessionUsageRepository,
// because no endpoint writes it until the next epic. After every step assertPaymentInvariants() checks the rules
// that span the endpoints (scenario 16; in PaymentJourneys). The single rules are in each endpoint's own *ApiTest.
// Every scenario has trainers Ahmed and Mona and a Monday 10:00 assessment slot for 12; riders are placed in it on
// Monday 2 Nov 2026 with Ahmed as assessor, so they start as his.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(AdminOversightFixture.Config.class)
class PaymentsScenarioTest {
    private static final int LEVEL_ONE = 1;
    private static final LocalDate SUN_1_NOV = LocalDate.of(2026, 11, 1);
    private static final LocalDate MON_2_NOV = LocalDate.of(2026, 11, 2);
    private static final LocalDate NOV_3 = LocalDate.of(2026, 11, 3);
    private static final LocalTime TEN = LocalTime.of(10, 0);
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);
    private static final String NOV_EXPIRY = "2026-12-31";
    private static final String DEC_EXPIRY = "2027-01-31";

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
    @Autowired
    RiderBalanceService balances;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    SessionUsageRepository sessionUsageRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    TransactionTemplate tx;

    Coach ahmed;
    Coach mona;
    UUID mondaySlot;
    PaymentJourneys payments;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
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

    // 1
    @Test
    void aNewlyPlacedRiderIsAwaitingPaymentOnBothScreens() throws Exception {
        assertThat(payments.w3Total()).isZero();

        UUID omar = placeRiders("Omar Hassan").getFirst();

        payments.assertBalance(omar, 0, "awaiting_payment", null);
        assertThat(payments.onMyRiders(ahmed)).containsKey(omar.toString());
        assertThat(payments.w3Total()).isEqualTo(1);
    }

    // 2: the epic demo's first payment
    @Test
    void aNovemberPaymentMakesTheRiderActiveUntilTheEndOfTheGraceMonth() throws Exception {
        omarPaidForNovember();
    }

    // 3
    @Test
    void aDecemberPaymentAddsUpAndTheNextExpiryStaysNovembers() throws Exception {
        UUID omar = omarPaidForNovember();

        moveTo(LocalDate.of(2026, 12, 10));
        pay(omar, "1200", DEC, 1).andExpect(status().isCreated());

        payments.assertBalance(omar, 16, "active", NOV_EXPIRY);
    }

    // 4
    @Test
    void sessionsAreUsedFromTheOldestPackageFirst() throws Exception {
        omarUsedFiveInDecember();
    }

    // 5
    @Test
    void novembersUnusedSessionsExpireOnTheFirstOfJanuary() throws Exception {
        UUID omar = omarUsedFiveInDecember();

        moveTo(LocalDate.of(2027, 1, 1));

        payments.assertBalance(omar, 8, "active", DEC_EXPIRY);
        payments.history(omar)
                .andExpect(jsonPath("$.payments[1].startMonth").value("2026-11"))
                .andExpect(jsonPath("$.payments[1].state").value("expired"))
                .andExpect(jsonPath("$.payments[1].used").value(5))
                .andExpect(jsonPath("$.payments[1].left").value(0))
                .andExpect(jsonPath("$.payments[0].startMonth").value("2026-12"))
                .andExpect(jsonPath("$.payments[0].state").value("active"))
                .andExpect(jsonPath("$.payments[0].left").value(8));
    }

    // 6
    @Test
    void withEveryPackageExpiredTheRiderIsAwaitingPaymentAgain() throws Exception {
        UUID omar = omarUsedFiveInDecember();
        moveTo(LocalDate.of(2027, 1, 1));

        moveTo(LocalDate.of(2027, 2, 1));

        payments.assertBalance(omar, 0, "awaiting_payment", null);
        payments.history(omar).andExpect(jsonPath("$.payments[*].state", contains("expired", "expired")));
    }

    // 7: the epic demo's typo
    @Test
    void aWrongAmountIsVoidedAndEnteredAgain() throws Exception {
        UUID omar = placeRiders("Omar Hassan").getFirst();
        moveTo(NOV_3);

        UUID wrong = PaymentJourneys.paymentId(pay(omar, "12000", NOV, 1).andExpect(status().isCreated()));
        payments.assertBalance(omar, 8, "active", NOV_EXPIRY);

        voidPayment(wrong, "Typo in amount")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.state").value("voided"))
                .andExpect(jsonPath("$.balance.sessionsLeft").value(0));
        payments.assertBalance(omar, 0, "awaiting_payment", null);

        UUID right = PaymentJourneys.paymentId(pay(omar, "1200", NOV, 1).andExpect(status().isCreated()));
        payments.assertBalance(omar, 8, "active", NOV_EXPIRY);

        // Newest first: the right one, then the voided one with its reason.
        ResultActions history = payments.history(omar)
                .andExpect(jsonPath("$.payments.length()").value(2))
                .andExpect(jsonPath("$.payments[0].id").value(right.toString()))
                .andExpect(jsonPath("$.payments[0].state").value("active"))
                .andExpect(jsonPath("$.payments[1].id").value(wrong.toString()))
                .andExpect(jsonPath("$.payments[1].state").value("voided"))
                .andExpect(jsonPath("$.payments[1].voidReason").value("Typo in amount"));
        assertThat(amount(history, "$.payments[0].amountEgp")).isEqualTo(1200.0);
        assertThat(amount(history, "$.payments[1].amountEgp")).isEqualTo(12000.0);
    }

    // 8
    @Test
    void aPackageSessionsWereUsedFromCannotBeVoided() throws Exception {
        UUID omar = placeRiders("Omar Hassan").getFirst();
        moveTo(NOV_3);
        UUID nov = PaymentJourneys.paymentId(pay(omar, "1200", NOV, 1).andExpect(status().isCreated()));
        moveTo(LocalDate.of(2026, 11, 10));
        useSessions(nov, 2);
        payments.assertBalance(omar, 6, "active", NOV_EXPIRY);

        voidPayment(nov, "Entered by mistake")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("payment_has_usage"))
                .andExpect(jsonPath("$.details.used").value(2));

        payments.assertBalance(omar, 6, "active", NOV_EXPIRY);
        payments.history(omar).andExpect(jsonPath("$.payments[0].state").value("active"))
                .andExpect(jsonPath("$.payments[0].voidReason").isEmpty());
    }

    // 9
    @Test
    void aPackageForNextMonthCountsOnlyFromItsFirstDay() throws Exception {
        UUID omar = placeRiders("Omar Hassan").getFirst();
        moveTo(LocalDate.of(2026, 11, 25));

        pay(omar, "1200", DEC, 1).andExpect(status().isCreated());

        payments.assertBalance(omar, 0, "awaiting_payment", null);
        payments.history(omar).andExpect(jsonPath("$.payments[0].state").value("upcoming"))
                .andExpect(jsonPath("$.payments[0].left").value(8));
        moveTo(LocalDate.of(2026, 11, 30));
        payments.assertBalance(omar, 0, "awaiting_payment", null);

        moveTo(LocalDate.of(2026, 12, 1));

        payments.assertBalance(omar, 8, "active", DEC_EXPIRY);
        payments.history(omar).andExpect(jsonPath("$.payments[0].state").value("active"));
    }

    // 10
    @Test
    void twoPackagesForTheSameMonthAddUp() throws Exception {
        UUID omar = placeRiders("Omar Hassan").getFirst();
        moveTo(NOV_3);

        pay(omar, "1200", NOV, 1).andExpect(status().isCreated());
        pay(omar, "1200", NOV, 1).andExpect(status().isCreated());

        payments.assertBalance(omar, 16, "active", NOV_EXPIRY);
    }

    // 11
    @Test
    void aStartMonthTwoMonthsBackOrThirteenAheadIsRefused() throws Exception {
        UUID omar = placeRiders("Omar Hassan").getFirst();
        moveTo(NOV_3);

        for (YearMonth start : List.of(YearMonth.of(2026, 9), YearMonth.of(2027, 12))) {
            pay(omar, "1200", start, 1)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("startMonth"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("start_month_out_of_range"));
        }

        payments.assertBalance(omar, 0, "awaiting_payment", null);
        payments.history(omar).andExpect(jsonPath("$.payments.length()").value(0));
    }

    // 12
    @Test
    void theAwaitingPaymentFilterShowsExactlyTheRidersWithNothingLeft() throws Exception {
        List<UUID> riders = placeRiders("Omar Hassan", "Sara Ali", "Nour Adel", "Laila Fathy");
        UUID omar = riders.get(0);
        UUID sara = riders.get(1);
        UUID nour = riders.get(2);
        UUID laila = riders.get(3);
        moveTo(NOV_3);
        pay(omar, "1200", NOV, 1).andExpect(status().isCreated());
        // Sara has not paid; Nour's only payment was voided; Laila paid for December only.
        UUID nours = PaymentJourneys.paymentId(pay(nour, "12000", NOV, 1).andExpect(status().isCreated()));
        voidPayment(nours, "Typo in amount").andExpect(status().isOk());
        pay(laila, "1200", DEC, 1).andExpect(status().isCreated());

        fx.w3(Map.of("payment", "awaiting_payment"))
                .andExpect(jsonPath("$.riders.length()").value(3))
                .andExpect(jsonPath("$.totals.awaitingPayment").value(3));
        assertThat(payments.w3Ids(Map.of("payment", "awaiting_payment")))
                .containsExactlyInAnyOrder(sara.toString(), nour.toString(), laila.toString());
        assertThat(payments.w3Ids(Map.of("payment", "active"))).containsExactly(omar.toString());
    }

    // 13
    @Test
    void aRiderMovedToAnotherTrainerKeepsTheBalance() throws Exception {
        UUID omar = omarPaidForNovember();
        Map<String, Object> before = payments.onMyRiders(ahmed).get(omar.toString());

        fx.assignRiders(List.of(omar), mona.id()).andExpect(status().isOk());
        payments.assertPaymentInvariants();

        assertThat(payments.onMyRiders(ahmed)).doesNotContainKey(omar.toString());
        assertThat(payments.onMyRiders(mona)).containsEntry(omar.toString(), before);
        payments.assertBalance(omar, 8, "active", NOV_EXPIRY);
    }

    // 14
    @Test
    void twoVoidsOfTheSamePaymentAtOnce() throws Exception {
        UUID omar = placeRiders("Omar Hassan").getFirst();
        moveTo(NOV_3);
        UUID nov = PaymentJourneys.paymentId(pay(omar, "1200", NOV, 1).andExpect(status().isCreated()));
        signInEveryone();

        List<String> outcomes = race(
                () -> payments.voidRequest(nov, "Entered twice").andReturn(),
                () -> payments.voidRequest(nov, "Entered twice").andReturn());

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "payment_already_voided");
        payments.assertPaymentInvariants();
        payments.assertBalance(omar, 0, "awaiting_payment", null);
        payments.history(omar).andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].state").value("voided"));
    }

    // 15
    @Test
    void twoPaymentsForTheSameRiderAtOnceBothCount() throws Exception {
        UUID omar = placeRiders("Omar Hassan").getFirst();
        moveTo(NOV_3);
        signInEveryone();

        List<String> outcomes = race(
                () -> payments.payRequest(omar, "1200", NOV, 1).andReturn(),
                () -> payments.payRequest(omar, "1200", NOV, 1).andReturn());

        assertThat(outcomes).containsExactly("ok", "ok");
        payments.assertPaymentInvariants();
        payments.assertBalance(omar, 16, "active", NOV_EXPIRY);
        payments.history(omar).andExpect(jsonPath("$.payments.length()").value(2));
    }

    // ---- Journeys several scenarios build on

    // Steps 1 and 2: Omar placed on 2 Nov, paid 1,200 cash for November on 3 Nov.
    private UUID omarPaidForNovember() throws Exception {
        UUID omar = placeRiders("Omar Hassan").getFirst();
        payments.assertBalance(omar, 0, "awaiting_payment", null);
        moveTo(NOV_3);

        pay(omar, "1200", NOV, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.method").value("cash"))
                .andExpect(jsonPath("$.payment.sessions").value(8))
                .andExpect(jsonPath("$.payment.validUntil").value(NOV_EXPIRY))
                .andExpect(jsonPath("$.balance.sessionsLeft").value(8));

        payments.assertBalance(omar, 8, "active", NOV_EXPIRY);
        return omar;
    }

    // Steps 1–4: paid for November and December, then 5 sessions used on 12 Dec, each from the package
    // packageToUse picks, as the next epic's deduction will.
    private UUID omarUsedFiveInDecember() throws Exception {
        UUID omar = omarPaidForNovember();
        moveTo(LocalDate.of(2026, 12, 10));
        UUID dec = PaymentJourneys.paymentId(pay(omar, "1200", DEC, 1).andExpect(status().isCreated()));
        UUID nov = UUID.fromString(read(payments.history(omar), "$.payments[1].id"));
        moveTo(LocalDate.of(2026, 12, 12));

        for (int i = 0; i < 5; i++) {
            UUID from = balances.packageToUse(omar, LocalDate.of(2026, 12, 12)).orElseThrow();
            assertThat(from).as("session %d comes from November", i + 1).isEqualTo(nov);
            useSessions(from, 1);
        }

        payments.assertBalance(omar, 11, "active", NOV_EXPIRY);
        payments.history(omar)
                .andExpect(jsonPath("$.payments[1].id").value(nov.toString()))
                .andExpect(jsonPath("$.payments[1].used").value(5))
                .andExpect(jsonPath("$.payments[1].left").value(3))
                .andExpect(jsonPath("$.payments[0].id").value(dec.toString()))
                .andExpect(jsonPath("$.payments[0].used").value(0));
        return omar;
    }

    // ---- Steps

    // Admin adds each person into the Monday slot with Ahmed as assessor; the clock moves past its start and Ahmed
    // places them all at level 1. The rider ids, in the order of the names.
    private List<UUID> placeRiders(String... names) throws Exception {
        List<UUID> riders = fx.placedRiders(ahmed, mondaySlot, LEVEL_ONE, names);
        payments.assertPaymentInvariants();
        return riders;
    }

    // 10:00 school time on that day.
    private void moveTo(LocalDate date) throws Exception {
        fx.at(date, TEN);
        payments.assertPaymentInvariants();
    }

    private ResultActions pay(UUID riderId, String amount, YearMonth start, int months) throws Exception {
        // A minute apart, so the history's newest-first order is well defined.
        fx.accounts().advance(Duration.ofMinutes(1));
        ResultActions result = payments.payRequest(riderId, amount, start, months);
        payments.assertPaymentInvariants();
        return result;
    }

    private ResultActions voidPayment(UUID paymentId, String reason) throws Exception {
        fx.accounts().advance(Duration.ofMinutes(1));
        ResultActions result = payments.voidRequest(paymentId, reason);
        payments.assertPaymentInvariants();
        return result;
    }

    // n sessions the package's rider rode on the n days before today, paid from this package.
    private void useSessions(UUID paymentId, int n) throws Exception {
        LocalDate today = LocalDate.ofInstant(clock.instant(), TrainerAccountsFixture.CAIRO);
        for (int i = 0; i < n; i++) {
            // One day further back for every usage so far, so no two land on the same day.
            UUID session = pastSession(today.minusDays(1L + sessionsUsedSoFar()));
            tx.executeWithoutResult(status -> sessionUsageRepository.save(SessionUsage.of(
                    paymentRepository.findById(paymentId).orElseThrow(),
                    sessionRepository.getReferenceById(session), clock.instant())));
        }
        payments.assertPaymentInvariants();
    }

    // A level-1 session on that date, on a template of its own (the usage rows are what matter, not the timetable).
    private UUID pastSession(LocalDate date) {
        LocalTime start = LocalTime.of(17, 0);
        UUID template = fx.jdbc().queryForObject("""
                        insert into session_template (level_id, day_of_week, start_time, end_time, capacity, valid_from)
                        values (?, ?, ?, ?, 6, ?) returning id""",
                UUID.class, LEVEL_ONE, date.getDayOfWeek().getValue(), start, start.plusHours(1), date);
        return fx.jdbc().queryForObject("""
                        insert into session (template_id, session_date, start_time, end_time, capacity)
                        values (?, ?, ?, ?, 6) returning id""",
                UUID.class, template, date, start, start.plusHours(1));
    }

    private int sessionsUsedSoFar() {
        return fx.jdbc().queryForObject("select count(*) from session_usage", Integer.class);
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

    private static double amount(ResultActions result, String path) throws Exception {
        return ((Number) JsonPath.read(PaymentJourneys.body(result), path)).doubleValue();
    }
}
