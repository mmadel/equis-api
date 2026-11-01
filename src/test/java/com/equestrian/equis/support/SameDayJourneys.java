package com.equestrian.equis.support;

import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

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

import static com.equestrian.equis.support.AdminOversightFixture.read;
import static com.equestrian.equis.support.PaymentJourneys.body;
import static com.equestrian.equis.support.PaymentJourneys.paymentId;
import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The same-day session journeys of the scenario tests (E13, E14), over HTTP, for trainers Ahmed and Mona: the admin
// on W1 (templates), W3 (payments) and the Overview; the trainers on Assign riders (screen 3). Every step runs the
// after-step check, assertSameDayInvariants() unless the test sets its own (which should include it).
// Riders are placed at Level 2 from a Monday 10:00 assessment slot on Monday 2 Nov 2026 with Ahmed as assessor, so
// they start as his. Each level session gets a W1 template of its own, created a few minutes before it starts.
public class SameDayJourneys {
    public static final int LEVEL_TWO = 2;
    public static final LocalDate SUN_1_NOV = LocalDate.of(2026, 11, 1);
    public static final LocalDate MON_2_NOV = LocalDate.of(2026, 11, 2);
    public static final LocalTime TEN = LocalTime.of(10, 0);

    // A check that may call the API.
    @FunctionalInterface
    public interface Check {
        void run() throws Exception;
    }

    private final AdminOversightFixture fx;
    private final Clock clock;
    private final Coach ahmed;
    private final Coach mona;
    private final PaymentJourneys payments;
    // Every level session the scenario opened, for the pick-list invariant, with its W1 template.
    private final Map<UUID, UUID> opened = new HashMap<>();
    private final List<UUID> openedInOrder = new ArrayList<>();
    private Check afterEachStep = this::assertSameDayInvariants;
    private UUID mondaySlot;

    public SameDayJourneys(AdminOversightFixture fx, Clock clock, Coach ahmed, Coach mona) {
        this.fx = fx;
        this.clock = clock;
        this.ahmed = ahmed;
        this.mona = mona;
        this.payments = new PaymentJourneys(fx, ahmed, mona);
    }

    // For @BeforeEach, after fx.reset() and the coaches: Sunday 1 Nov 10:00 with the Monday assessment slots.
    public void start() throws Exception {
        fx.at(SUN_1_NOV, TEN);
        assessmentSlotsOnMondays();
        mondaySlot = fx.slotOn(MON_2_NOV, TEN);
    }

    public void afterEachStep(Check check) {
        this.afterEachStep = check;
    }

    public PaymentJourneys payments() {
        return payments;
    }

    public List<UUID> opened() {
        return List.copyOf(openedInOrder);
    }

    public UUID templateOf(UUID sessionId) {
        return opened.get(sessionId);
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), CAIRO);
    }

    // ---- Steps

    // Admin adds each person into the Monday slot with Ahmed as assessor; the clock moves past its start and Ahmed
    // places them all at Level 2. The rider ids, in the order of the names.
    public List<UUID> placeRiders(String... names) throws Exception {
        List<UUID> riders = fx.placedRiders(ahmed, mondaySlot, LEVEL_TWO, names);
        afterEachStep.run();
        return riders;
    }

    // The admin moves the rider to Mona on W3.
    public void toMona(UUID rider) throws Exception {
        fx.assignRiders(List.of(rider), mona.id()).andExpect(status().isOk());
        afterEachStep.run();
    }

    // That moment in school time.
    public void at(LocalDate date, LocalTime time) throws Exception {
        fx.at(date, time);
        afterEachStep.run();
    }

    // A one-month 1,200 EGP cash package; sessions null means the default 8. The payment id.
    public UUID pay(UUID riderId, YearMonth start, Integer sessions) throws Exception {
        // A minute apart, so the history's newest-first order is well defined.
        fx.accounts().advance(Duration.ofMinutes(1));
        UUID id = paymentId(payments.payRequest(riderId, "1200", start, 1, sessions).andExpect(status().isCreated()));
        afterEachStep.run();
        return id;
    }

    public ResultActions voidPayment(UUID paymentId) throws Exception {
        fx.accounts().advance(Duration.ofMinutes(1));
        ResultActions result = payments.voidRequest(paymentId, "Entered by mistake");
        afterEachStep.run();
        return result;
    }

    // Today's Level 2 session starting then, one hour, on a W1 template of its own.
    public UUID todaysSession(LocalTime start) throws Exception {
        return sessionOn(today(), start);
    }

    // The Level 2 session on that date, one hour, on a W1 template of its own valid from that date.
    public UUID sessionOn(LocalDate date, LocalTime start) throws Exception {
        ResultActions created = fx.accounts().admin(post("/admin/session-templates")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        { "levelId": %d, "dayOfWeek": "%s", "start": "%s", "end": "%s", "capacity": 6, "validFrom": "%s" }"""
                        .formatted(LEVEL_TWO, date.getDayOfWeek(), start, start.plusHours(1), date)))
                .andExpect(status().isCreated());
        UUID session = fx.slotOn(date, start);
        opened.put(session, UUID.fromString(read(created, "$.template.id")));
        openedInOrder.add(session);
        afterEachStep.run();
        return session;
    }

    // Screen 3's save: the riders become this trainer's whole list for the session.
    public ResultActions assign(Coach coach, UUID sessionId, UUID... riderIds) throws Exception {
        ResultActions result = fx.assignToSession(coach, sessionId, List.of(riderIds));
        afterEachStep.run();
        return result;
    }

    public ResultActions finish(Coach coach, UUID sessionId) throws Exception {
        ResultActions result = finishRequest(coach, sessionId);
        afterEachStep.run();
        return result;
    }

    public ResultActions finishRequest(Coach coach, UUID sessionId) throws Exception {
        return fx.asCoach(coach, post("/me/sessions/" + sessionId + "/finish"));
    }

    // A whole session on that day: opened and filled five minutes before the start, finished 65 minutes after it.
    public UUID ride(LocalDate date, LocalTime start, Coach coach, UUID... riderIds) throws Exception {
        at(date, start.minusMinutes(5));
        UUID session = todaysSession(start);
        assign(coach, session, riderIds).andExpect(status().isOk());
        at(date, start.plusMinutes(65));
        finish(coach, session).andExpect(status().isOk());
        return session;
    }

    // ---- What the screens show

    // Screen 3.
    public ResultActions session(Coach coach, UUID sessionId) throws Exception {
        return fx.session(coach, sessionId).andExpect(status().isOk());
    }

    public ResultActions overview() throws Exception {
        return fx.accounts().admin(get("/admin/overview")).andExpect(status().isOk());
    }

    // Each rider on W3 with their sessions left today.
    public Map<String, Integer> w3SessionsLeft() throws Exception {
        List<Map<String, Object>> rows = JsonPath.read(body(fx.w3()), "$.riders");
        Map<String, Integer> left = new HashMap<>();
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> balance = (Map<String, Object>) row.get("balance");
            left.put((String) row.get("id"), (Integer) balance.get("sessionsLeft"));
        }
        return left;
    }

    // ---- The rules that span the endpoints (E13 scenario 17)

    // E12's payment invariants; every finished (or done) session has exactly one usage per seated rider, from a
    // package of that rider valid on the session's date, and an unfinished one none; no usage is on a voided
    // package; every rider a trainer's pick list offers has a session left on the session's date, and on today's
    // sessions the pick list and the seats show the same sessionsLeft as W3.
    public void assertSameDayInvariants() throws Exception {
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
            if ("finished".equals(s.get("status")) || "done".equals(s.get("status"))) {
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
        String today = today().toString();
        for (UUID id : openedInOrder) {
            for (Coach coach : List.of(ahmed, mona)) {
                String detail = body(session(coach, id));
                boolean isToday = today.equals(JsonPath.read(detail, "$.date"));
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

    // ---- Helpers

    // Monday 10:00–11:00 assessments for 12, from today, so the slot on 2 Nov exists.
    private void assessmentSlotsOnMondays() throws Exception {
        fx.accounts().admin(post("/admin/session-templates").contentType(MediaType.APPLICATION_JSON).content("""
                { "levelId": null, "dayOfWeek": "MONDAY", "start": "10:00", "end": "11:00", "capacity": 12,
                  "validFrom": "%s" }""".formatted(SUN_1_NOV))).andExpect(status().isCreated());
    }

    // The race threads share the cached tokens, so everyone signs in first.
    public void signInEveryone() throws Exception {
        fx.accounts().adminToken();
        fx.token(ahmed);
        fx.token(mona);
    }
}
