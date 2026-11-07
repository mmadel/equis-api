package com.equestrian.equis.repository.platform;

import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.entity.platform.OnExpiry;
import com.equestrian.equis.entity.platform.Plan;
import com.equestrian.equis.entity.platform.Subscription;
import com.equestrian.equis.support.TestPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E17 · plan and subscription tables (V26) and ops/upsert-plan.sql, against the real schema.
// The script runs in the test's own transaction, so everything it writes is rolled back after each test.
// Case 16 is this class starting at all: the context runs Flyway and then ddl-auto=validate.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanAndSubscriptionTest {
    private static final Path SCRIPT = Path.of("ops", "upsert-plan.sql");
    private static final UUID SUPER_ADMIN = UUID.randomUUID();

    private static final Instant NOV_1 = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant DEC_1 = Instant.parse("2026-12-01T00:00:00Z");
    private static final Instant DEC_10 = Instant.parse("2026-12-10T12:00:00Z");
    private static final Instant DEC_15 = Instant.parse("2026-12-15T00:00:00Z");
    private static final Instant DEC_20 = Instant.parse("2026-12-20T12:00:00Z");
    private static final Instant JAN_1 = Instant.parse("2027-01-01T00:00:00Z");
    private static final Instant FEB_1 = Instant.parse("2027-02-01T00:00:00Z");

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    PlanRepository plans;
    @Autowired
    SubscriptionRepository subscriptions;
    @Autowired
    JdbcTemplate jdbc;

    UUID cairo;

    // Clubs are inserted by hand, so row-level security (V24) is bypassed for this test's transaction only.
    @BeforeEach
    void setUp() {
        jdbc.queryForObject("select set_config('app.bypass_rls', 'on', true)", String.class);
        cairo = newClub("cairo-eq");
    }

    // ---- plans and the ops script

    // Case 1
    @Test
    void theScriptCreatesTheFourPlansAndUpdatesOneWhenRunAgain() {
        fourPlans();
        // now() is fixed for the whole transaction, so move the stored time back to see the update move it.
        jdbc.update("UPDATE plan SET updated_at = updated_at - interval '1 hour' WHERE code = 'BASIC'");
        Instant before = updatedAt("BASIC");

        upsertPlan("basic", "Basic", "3", "50", "NULL", "read_only");

        assertThat(plans.findAllByOrderByCodeAsc()).extracting(Plan::getCode)
                .containsExactly("BASIC", "MAX", "PRO", "TRIAL");
        Plan basic = plans.findByCode("BASIC").orElseThrow();
        assertThat(basic.getMaxTrainers()).isEqualTo(3);
        assertThat(basic.trainerLimit()).hasValue(3);
        assertThat(basic.riderLimit()).hasValue(50);
        assertThat(basic.getOnExpiry()).isEqualTo(OnExpiry.READ_ONLY);
        assertThat(basic.isTrial()).isFalse();
        assertThat(updatedAt("BASIC")).isAfter(before);

        Plan trial = plans.findByTrialDaysIsNotNull().orElseThrow();
        assertThat(trial.getCode()).isEqualTo("TRIAL");
        assertThat(trial.isTrial()).isTrue();
        assertThat(trial.getTrialDays()).isEqualTo(30);
        assertThat(trial.getOnExpiry()).isEqualTo(OnExpiry.BLOCK);
    }

    // Case 2
    @Test
    void nullLimitsMeanUnlimited() {
        upsertPlan("MAX", "Max", "NULL", "NULL", "NULL", "READ_ONLY");

        Plan max = plans.findByCode("MAX").orElseThrow();
        assertThat(max.getMaxTrainers()).isNull();
        assertThat(max.trainerLimit()).isEmpty();
        assertThat(max.riderLimit()).isEmpty();
    }

    // Case 3
    @Test
    void onlyOnePlanMayHaveTrialDays() {
        upsertPlan("TRIAL", "Trial", "NULL", "NULL", "30", "BLOCK");

        assertRefusedBy("uq_plan_single_trial", () -> upsertPlan("TRIAL_TWO", "Trial 2", "NULL", "NULL", "14", "BLOCK"));
    }

    // Case 4 (a failed statement aborts the transaction, so one refusal per run)
    @ParameterizedTest
    @ValueSource(strings = {
            "ck_plan_code|basic plan|1|1|READ_ONLY",
            "ck_plan_on_expiry|BASIC|1|1|LATER",
            "ck_plan_max_riders|BASIC|1|0|READ_ONLY",
            "ck_plan_max_trainers|BASIC|0|1|READ_ONLY"})
    void badPlanValuesAreRefused(String row) {
        String[] v = row.split("\\|");

        assertRefusedBy(v[0], () -> upsertPlan(v[1], "Bad", v[2], v[3], "NULL", v[4]));
    }

    @Test
    void aTrialPlanNeedsPositiveDays() {
        assertRefusedBy("ck_plan_trial_days", () -> upsertPlan("TRIAL", "Trial", "NULL", "NULL", "0", "BLOCK"));
    }

    // Case 15
    @Test
    void aPlanInUseCannotBeDeleted() {
        fourPlans();
        subscriptions.saveAndFlush(paid(cairo, "BASIC", DEC_1, JAN_1));

        assertThatThrownBy(() -> jdbc.update("DELETE FROM plan WHERE code = 'BASIC'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("subscription_plan_id_fkey");
    }

    // ---- subscription rules in the database

    // Case 5
    @Test
    void aClubHasOnlyOneTrial() {
        fourPlans();
        insertSubscription(cairo, "TRIAL", "TRIAL", NOV_1, DEC_1);

        assertRefusedBy("uq_subscription_one_trial", () -> insertSubscription(cairo, "TRIAL", "TRIAL", DEC_1, JAN_1));
    }

    @Test
    void anotherClubMayHaveItsOwnTrial() {
        fourPlans();
        insertSubscription(cairo, "TRIAL", "TRIAL", NOV_1, DEC_1);

        insertSubscription(newClub("giza-eq"), "TRIAL", "TRIAL", NOV_1, DEC_1);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM subscription", Integer.class)).isEqualTo(2);
    }

    // Case 6
    @Test
    void theEndMustBeAfterTheStart() {
        fourPlans();

        assertRefusedBy("ck_subscription_dates", () -> insertSubscription(cairo, "BASIC", "MONTHLY", DEC_1, DEC_1));
    }

    @Test
    void anUnknownBillingPeriodIsRefused() {
        fourPlans();

        assertRefusedBy("ck_subscription_period", () -> insertSubscription(cairo, "BASIC", "WEEKLY", DEC_1, JAN_1));
    }

    // Case 7: reason, voided_by
    @ParameterizedTest
    @ValueSource(strings = {
            "ck_subscription_void|NULL|by",
            "ck_subscription_void_reason|'ok'|by",
            "ck_subscription_void_reason|'   ok   '|by",
            "ck_subscription_void_by|'Entered by mistake'|NULL"})
    void aVoidNeedsAReasonAndWhoDidIt(String row) {
        String[] v = row.split("\\|");
        fourPlans();
        UUID id = insertSubscription(cairo, "BASIC", "MONTHLY", DEC_1, JAN_1);
        String by = v[2].equals("NULL") ? "NULL" : "'" + SUPER_ADMIN + "'";

        assertRefusedBy(v[0], () -> jdbc.update(
                "UPDATE subscription SET voided_at = now(), void_reason = " + v[1] + ", voided_by = " + by
                        + " WHERE id = ?", id));
    }

    @Test
    void aVoidWithReasonAndAuthorIsAccepted() {
        fourPlans();
        UUID id = insertSubscription(cairo, "BASIC", "MONTHLY", DEC_1, JAN_1);

        assertThat(jdbc.update("UPDATE subscription SET voided_at = now(), void_reason = 'Entered by mistake',"
                + " voided_by = ? WHERE id = ?", SUPER_ADMIN, id)).isEqualTo(1);
    }

    // ---- Subscription entity rules

    // Case 8
    @Test
    void theBillingPeriodMustMatchThePlan() {
        fourPlans();
        Plan basic = plan("BASIC");
        Plan trial = plan("TRIAL");

        assertThatThrownBy(() -> Subscription.create(cairo, basic, BillingPeriod.TRIAL, DEC_1, JAN_1, null, SUPER_ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Subscription.create(cairo, trial, BillingPeriod.MONTHLY, NOV_1, DEC_1, null, SUPER_ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Subscription.create(cairo, trial, BillingPeriod.ANNUAL, NOV_1, DEC_1, null, SUPER_ADMIN))
                .isInstanceOf(IllegalArgumentException.class);

        Subscription annual = subscriptions.saveAndFlush(
                Subscription.create(cairo, basic, BillingPeriod.ANNUAL, DEC_1, DEC_1.plusSeconds(365L * 86400),
                        "  Paid by bank transfer  ", SUPER_ADMIN));
        assertThat(annual.getId()).isNotNull();
        assertThat(annual.getCreatedAt()).isNotNull();
        assertThat(annual.getNote()).isEqualTo("Paid by bank transfer");
        assertThat(annual.getCreatedBy()).isEqualTo(SUPER_ADMIN);
    }

    @Test
    void createRefusesAnEndThatIsNotAfterTheStart() {
        fourPlans();

        assertThatThrownBy(() -> Subscription.create(cairo, plan("BASIC"), BillingPeriod.MONTHLY, DEC_1, DEC_1,
                null, SUPER_ADMIN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void voidWithChecksTheReasonFirstAndStoresTheVoid() {
        fourPlans();
        Subscription basic = subscriptions.saveAndFlush(paid(cairo, "BASIC", DEC_1, JAN_1));

        assertThatThrownBy(() -> basic.voidWith(" ok ", SUPER_ADMIN, DEC_10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(basic.isVoided()).isFalse();

        basic.voidWith("  Entered twice  ", SUPER_ADMIN, DEC_10);
        subscriptions.flush();

        assertThat(basic.isVoided()).isTrue();
        assertThat(basic.getVoidReason()).isEqualTo("Entered twice");
        assertThat(basic.isValidAt(DEC_10)).isFalse();
        assertThatThrownBy(() -> basic.voidWith("Entered twice", SUPER_ADMIN, DEC_10))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- current subscription

    // Case 9
    @Test
    void theCurrentSubscriptionIsTheLatestStartedOne() {
        fourPlans();
        cairoTrialThenBasic();

        Subscription current = subscriptions.findCurrent(cairo, DEC_10).orElseThrow();

        assertThat(current.getPlan().getCode()).isEqualTo("BASIC");
        assertThat(current.getBillingPeriod()).isEqualTo(BillingPeriod.MONTHLY);
        assertThat(current.isValidAt(DEC_10)).isTrue();
    }

    // Case 10
    @Test
    void aVoidedLaterRowIsSkipped() {
        fourPlans();
        cairoTrialThenBasic();
        Subscription pro = subscriptions.saveAndFlush(paid(cairo, "PRO", DEC_15, JAN_1));
        assertThat(subscriptions.findCurrent(cairo, DEC_20)).get().isEqualTo(pro);

        pro.voidWith("Wrong plan picked", SUPER_ADMIN, DEC_20);
        subscriptions.flush();

        assertThat(subscriptions.findCurrent(cairo, DEC_20)).get()
                .extracting(s -> s.getPlan().getCode()).isEqualTo("BASIC");
    }

    // Case 11
    @Test
    void onlyAFutureRowMeansNoCurrentSubscription() {
        fourPlans();
        subscriptions.saveAndFlush(paid(cairo, "BASIC", JAN_1, FEB_1));

        assertThat(subscriptions.findCurrent(cairo, DEC_10)).isEmpty();
    }

    // Case 12
    @Test
    void anEndedRowIsStillCurrentButNotValid() {
        fourPlans();
        Instant now = DEC_10;
        subscriptions.saveAndFlush(paid(cairo, "BASIC", NOV_1, now.minusSeconds(86400)));

        Subscription current = subscriptions.findCurrent(cairo, now).orElseThrow();

        assertThat(current.isValidAt(now)).isFalse();
    }

    // Case 13
    @Test
    void theLatestEndIgnoresVoidedRows() {
        fourPlans();
        cairoTrialThenBasic();
        Subscription pro = subscriptions.saveAndFlush(paid(cairo, "PRO", DEC_15, FEB_1));
        pro.voidWith("Wrong plan picked", SUPER_ADMIN, DEC_20);
        subscriptions.flush();

        assertThat(subscriptions.findLatestEnd(cairo)).hasValue(JAN_1);
        assertThat(subscriptions.findLatestEnd(newClub("empty-eq"))).isEmpty();
        assertThat(subscriptions.findAllByClubIdOrderByStartsAtDesc(cairo))
                .extracting(s -> s.getPlan().getCode()).containsExactly("PRO", "BASIC", "TRIAL");
    }

    // Case 14
    @Test
    void currentForManyClubsGivesOnePerClubThatHasOne() {
        fourPlans();
        cairoTrialThenBasic();
        UUID giza = newClub("giza-eq");
        subscriptions.saveAndFlush(paid(giza, "BASIC", JAN_1, FEB_1));
        UUID alex = newClub("alex-eq");
        Subscription alexPro = subscriptions.saveAndFlush(paid(alex, "PRO", NOV_1, JAN_1));

        List<Subscription> current = subscriptions.findCurrentForClubs(List.of(cairo, giza, alex), DEC_10);

        assertThat(current).hasSize(2);
        assertThat(current).filteredOn(s -> s.getClubId().equals(cairo)).singleElement()
                .extracting(s -> s.getPlan().getCode()).isEqualTo("BASIC");
        assertThat(current).filteredOn(s -> s.getClubId().equals(alex)).containsExactly(alexPro);
        assertThat(subscriptions.findCurrentForClubs(List.of(), DEC_10)).isEmpty();
    }

    // ---- helpers

    // The placeholder plans from ops/README.md.
    private void fourPlans() {
        upsertPlan("TRIAL", "Trial", "NULL", "NULL", "30", "BLOCK");
        upsertPlan("BASIC", "Basic", "2", "50", "NULL", "READ_ONLY");
        upsertPlan("PRO", "Pro", "10", "300", "NULL", "READ_ONLY");
        upsertPlan("MAX", "Max", "NULL", "NULL", "NULL", "READ_ONLY");
    }

    // Runs the real ops script with its psql variables filled in, as psql -v would.
    private void upsertPlan(String code, String name, String trainers, String riders, String trialDays,
                            String onExpiry) {
        String sql = script()
                .replace(":'code'", literal(code))
                .replace(":'name'", literal(name))
                .replace(":'on_expiry'", literal(onExpiry))
                .replace(":trainers", trainers)
                .replace(":riders", riders)
                .replace(":trial_days", trialDays);
        jdbc.execute(sql);
    }

    private static String script() {
        try {
            return Files.readString(SCRIPT);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private Instant updatedAt(String code) {
        return jdbc.queryForObject("SELECT updated_at FROM plan WHERE code = ?", Timestamp.class, code).toInstant();
    }

    private Plan plan(String code) {
        return plans.findByCode(code).orElseThrow();
    }

    private Subscription paid(UUID clubId, String planCode, Instant startsAt, Instant endsAt) {
        return Subscription.create(clubId, plan(planCode), BillingPeriod.MONTHLY, startsAt, endsAt, null, SUPER_ADMIN);
    }

    // Trial 1 Nov → 1 Dec 2026, then Basic monthly 1 Dec → 1 Jan 2027.
    private void cairoTrialThenBasic() {
        subscriptions.saveAndFlush(Subscription.create(cairo, plan("TRIAL"), BillingPeriod.TRIAL, NOV_1, DEC_1,
                null, null));
        subscriptions.saveAndFlush(paid(cairo, "BASIC", DEC_1, JAN_1));
    }

    private UUID newClub(String code) {
        return jdbc.queryForObject("INSERT INTO club (code, name) VALUES (?, ?) RETURNING id", UUID.class, code, code);
    }

    // Plain SQL, to reach the database rules without the entity's checks.
    private UUID insertSubscription(UUID clubId, String planCode, String period, Instant startsAt, Instant endsAt) {
        Map<String, Object> plan = jdbc.queryForMap("SELECT id FROM plan WHERE code = ?", planCode);
        return jdbc.queryForObject("""
                INSERT INTO subscription (club_id, plan_id, billing_period, starts_at, ends_at)
                VALUES (?, ?, ?, ?, ?) RETURNING id""", UUID.class,
                clubId, plan.get("id"), period, Timestamp.from(startsAt), Timestamp.from(endsAt));
    }

    private void assertRefusedBy(String constraint, Runnable statement) {
        // Plain JDBC, so the constraint name is only in the driver's message.
        assertThatThrownBy(statement::run)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
    }
}
