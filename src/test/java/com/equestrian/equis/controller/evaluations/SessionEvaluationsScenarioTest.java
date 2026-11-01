package com.equestrian.equis.controller.evaluations;

import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.SameDayJourneys;
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

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.race;
import static com.equestrian.equis.support.AdminOversightFixture.raceOutcomes;
import static com.equestrian.equis.support.PaymentJourneys.body;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E14 evaluation journeys 1–14 end to end, over HTTP: trainers Ahmed and Mona finish and evaluate on Assign riders
// (screen 3), read Home's pending count and Rider level (4a); the admin reads the Follow-up screen, a rider's
// evaluations on W3 and the Overview. The clock moves through Tuesday 3 Nov 2026 and into the next morning.
// After every step assertEvaluationInvariants() checks the rules that span the endpoints (scenario 15).
// The steps and E13's invariants come from SameDayJourneys; the single rules are in each endpoint's own *ApiTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(AdminOversightFixture.Config.class)
class SessionEvaluationsScenarioTest {
    private static final int LEVEL_TWO = 2;
    private static final LocalDate TUE_3_NOV = LocalDate.of(2026, 11, 3);
    private static final LocalDate WED_4_NOV = LocalDate.of(2026, 11, 4);
    private static final LocalTime FOUR_PM = LocalTime.of(16, 0);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);
    private static final LocalTime TEN_TO_FIVE = LocalTime.of(16, 50);
    private static final LocalTime FIVE_PAST_SIX = LocalTime.of(18, 5);
    private static final LocalTime HALF_PAST_EIGHT = LocalTime.of(20, 30);
    private static final YearMonth NOV = YearMonth.of(2026, 11);

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
    SameDayJourneys days;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        days = new SameDayJourneys(fx, clock, ahmed, mona);
        days.afterEachStep(this::assertEvaluationInvariants);
        days.start();
    }

    // The races commit for real, so nothing may be left for the next class sharing this context.
    @AfterEach
    void cleanUp() {
        fx.reset();
    }

    // 1: the epic demo
    @Test
    void ahmedEvaluatesBothRidersAndTheSessionIsDone() throws Exception {
        List<UUID> riders = paidRiders("Sara Ali", "Omar Hassan");
        UUID sara = riders.get(0);
        UUID omar = riders.get(1);
        UUID session = days.ride(TUE_3_NOV, FIVE_PM, ahmed, sara, omar);

        evaluate(ahmed, session, sara, 4, "Better heels down")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath(seat(sara) + ".evaluation.stars", contains(4)))
                .andExpect(jsonPath(seat(sara) + ".canEvaluate", contains(false)))
                .andExpect(jsonPath(seat(omar) + ".canEvaluate", contains(true)));
        evaluate(ahmed, session, omar, 3, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.mySeats[*].canEvaluate", contains(false, false)));

        for (ResultActions history : List.of(history(ahmed, sara), adminHistory(sara))) {
            history.andExpect(status().isOk())
                    .andExpect(jsonPath("$.evaluations", hasSize(1)))
                    .andExpect(jsonPath("$.evaluations[0].sessionId").value(session.toString()))
                    .andExpect(jsonPath("$.evaluations[0].date").value("2026-11-03"))
                    .andExpect(jsonPath("$.evaluations[0].start").value("17:00"))
                    .andExpect(jsonPath("$.evaluations[0].levelId").value(LEVEL_TWO))
                    .andExpect(jsonPath("$.evaluations[0].trainerName").value("Ahmed Said"))
                    .andExpect(jsonPath("$.evaluations[0].stars").value(4))
                    .andExpect(jsonPath("$.evaluations[0].comment").value("Better heels down"));
        }
        pending(ahmed).andExpect(jsonPath("$.count").value(0)).andExpect(jsonPath("$.sessions", empty()));
    }

    // 2
    @Test
    void monasUnevaluatedSessionIsOnTheEveningFollowUpAndAhmedsDoneOneIsNot() throws Exception {
        List<UUID> riders = paidRiders("Sara Ali", "Hala Samir", "Karim Fouad");
        UUID sara = riders.get(0);
        UUID hala = riders.get(1);
        UUID karim = riders.get(2);
        days.toMona(hala);
        days.toMona(karim);
        days.at(TUE_3_NOV, TEN_TO_FIVE);
        UUID ahmeds = days.todaysSession(FIVE_PM);
        UUID monas = days.todaysSession(SIX_PM);
        days.assign(ahmed, ahmeds, sara).andExpect(status().isOk());
        days.assign(mona, monas, hala, karim).andExpect(status().isOk());
        days.at(TUE_3_NOV, FIVE_PAST_SIX);
        days.finish(ahmed, ahmeds).andExpect(status().isOk());
        evaluate(ahmed, ahmeds, sara, 4, null).andExpect(jsonPath("$.status").value("done"));
        days.at(TUE_3_NOV, LocalTime.of(19, 5));
        days.finish(mona, monas).andExpect(status().isOk());

        days.at(TUE_3_NOV, HALF_PAST_EIGHT);

        followUp(TUE_3_NOV)
                .andExpect(jsonPath("$.totals.trainers").value(1))
                .andExpect(jsonPath("$.totals.notFinished").value(0))
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(2))
                .andExpect(jsonPath("$.trainers[*].trainerName", contains("Mona Kamal")))
                .andExpect(jsonPath("$.trainers[0].notFinished", empty()))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[*].sessionId", contains(monas.toString())))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].start").value("18:00"))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].name",
                        contains("Hala Samir", "Karim Fouad")));
        pending(mona).andExpect(jsonPath("$.count").value(2));
    }

    // 3
    @Test
    void anUnevaluatedRiderDoesNotBlockTheNextFinish() throws Exception {
        List<UUID> riders = paidRiders("Sara Ali", "Omar Hassan");
        UUID sara = riders.get(0);
        UUID omar = riders.get(1);
        days.at(TUE_3_NOV, TEN_TO_FIVE);
        UUID five = days.todaysSession(FIVE_PM);
        UUID six = days.todaysSession(SIX_PM);
        days.assign(ahmed, five, sara, omar).andExpect(status().isOk());
        days.at(TUE_3_NOV, FIVE_PAST_SIX);
        days.finish(ahmed, five).andExpect(status().isOk());
        evaluate(ahmed, five, sara, 4, "Better heels down").andExpect(status().isOk());
        pending(ahmed).andExpect(jsonPath("$.count").value(1));

        days.at(TUE_3_NOV, LocalTime.of(18, 10));
        days.assign(ahmed, six, sara, omar).andExpect(status().isOk());
        days.at(TUE_3_NOV, LocalTime.of(19, 5));
        days.finish(ahmed, six)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"));

        pending(ahmed)
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.sessions[*].sessionId", contains(six.toString(), five.toString())))
                .andExpect(jsonPath("$.sessions[*].ridersToEvaluate", contains(2, 1)));
        followUp(TUE_3_NOV)
                .andExpect(jsonPath("$.trainers[*].trainerName", contains("Ahmed Said")))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[*].sessionId",
                        contains(five.toString(), six.toString())))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].name", contains("Omar Hassan")))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[1].riders[*].name",
                        contains("Omar Hassan", "Sara Ali")));
    }

    // 4
    @Test
    void aSharedSessionIsDoneOnlyOnceTheOtherCoachHasEvaluatedToo() throws Exception {
        SharedSession shared = sharedSession();
        days.at(TUE_3_NOV, FIVE_PAST_SIX);
        days.finish(ahmed, shared.session())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCoachSeats").value(1));

        evaluate(ahmed, shared.session(), shared.sara(), 4, null).andExpect(status().isOk());
        evaluate(ahmed, shared.session(), shared.omar(), 3, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"));

        days.session(ahmed, shared.session())
                .andExpect(jsonPath("$.status").value("finished"))
                .andExpect(jsonPath("$.mySeats[*].canEvaluate", contains(false, false)));
        pending(ahmed).andExpect(jsonPath("$.count").value(0));
        pending(mona)
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.sessions[*].sessionId", contains(shared.session().toString())));
        followUp(TUE_3_NOV)
                .andExpect(jsonPath("$.trainers[*].trainerName", contains("Mona Kamal")))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].name", contains("Hala Samir")));

        evaluate(mona, shared.session(), shared.hala(), 5, "Lovely seat")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"));
        days.session(ahmed, shared.session()).andExpect(jsonPath("$.status").value("done"));
        pending(mona).andExpect(jsonPath("$.count").value(0));
        followUp(TUE_3_NOV).andExpect(jsonPath("$.trainers", empty()));
    }

    // 5
    @Test
    void onlyTheSeatingCoachEvaluatesOnlyAfterTheFinishAndOnlyOnce() throws Exception {
        SharedSession shared = sharedSession();
        days.at(TUE_3_NOV, LocalTime.of(17, 30));
        evaluate(ahmed, shared.session(), shared.sara(), 4, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_not_finished"));

        days.at(TUE_3_NOV, FIVE_PAST_SIX);
        days.finish(ahmed, shared.session()).andExpect(status().isOk());
        evaluate(ahmed, shared.session(), shared.hala(), 4, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("rider_not_yours"));
        evaluate(ahmed, shared.session(), shared.sara(), 4, "Better heels down").andExpect(status().isOk());
        evaluate(ahmed, shared.session(), shared.sara(), 2, "Second thoughts")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_evaluated"));

        adminHistory(shared.sara())
                .andExpect(jsonPath("$.evaluations[*].stars", contains(4)))
                .andExpect(jsonPath("$.evaluations[*].comment", contains("Better heels down")));
        adminHistory(shared.hala()).andExpect(jsonPath("$.evaluations", empty()));
    }

    // 6
    @Test
    void anEmptySessionIsDoneAtTheFinishAndNeverFollowedUp() throws Exception {
        paidRiders("Sara Ali");
        days.at(TUE_3_NOV, LocalTime.of(14, 50));
        UUID empty = days.todaysSession(LocalTime.of(15, 0));

        days.at(TUE_3_NOV, LocalTime.of(16, 5));
        days.finish(ahmed, empty)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"));

        for (LocalTime time : List.of(LocalTime.of(16, 6), HALF_PAST_EIGHT)) {
            days.at(TUE_3_NOV, time);
            pending(ahmed).andExpect(jsonPath("$.count").value(0)).andExpect(jsonPath("$.sessions", empty()));
            pending(mona).andExpect(jsonPath("$.count").value(0));
            followUp(TUE_3_NOV).andExpect(jsonPath("$.trainers", empty()));
        }
    }

    // 7
    @Test
    void aSessionNobodyFinishedIsNotFinishedOnTheFollowUpUntilItIsFinished() throws Exception {
        List<UUID> riders = paidRiders("Sara Ali", "Omar Hassan");
        days.at(TUE_3_NOV, TEN_TO_FIVE);
        UUID session = days.todaysSession(FIVE_PM);
        days.assign(ahmed, session, riders.get(0), riders.get(1)).andExpect(status().isOk());
        followUp(TUE_3_NOV).andExpect(jsonPath("$.trainers", empty()));

        for (LocalTime time : List.of(LocalTime.of(17, 30), LocalTime.of(19, 30))) {
            days.at(TUE_3_NOV, time);
            followUp(TUE_3_NOV)
                    .andExpect(jsonPath("$.totals.notFinished").value(1))
                    .andExpect(jsonPath("$.totals.ridersToEvaluate").value(0))
                    .andExpect(jsonPath("$.trainers[*].trainerName", contains("Ahmed Said")))
                    .andExpect(jsonPath("$.trainers[0].notFinished[*].sessionId", contains(session.toString())))
                    .andExpect(jsonPath("$.trainers[0].notFinished[0].riders").value(2))
                    .andExpect(jsonPath("$.trainers[0].toEvaluate", empty()));
        }

        days.finish(ahmed, session).andExpect(status().isOk());
        followUp(TUE_3_NOV)
                .andExpect(jsonPath("$.totals.notFinished").value(0))
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(2))
                .andExpect(jsonPath("$.trainers[0].notFinished", empty()))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[*].sessionId", contains(session.toString())))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].name",
                        contains("Omar Hassan", "Sara Ali")));
    }

    // 8
    @Test
    void yesterdaysSessionCanBeEvaluatedTheNextMorning() throws Exception {
        List<UUID> riders = paidRiders("Sara Ali", "Omar Hassan");
        UUID sara = riders.get(0);
        UUID omar = riders.get(1);
        UUID yesterday = days.ride(TUE_3_NOV, FIVE_PM, ahmed, sara, omar);
        UUID morning = days.ride(WED_4_NOV, LocalTime.of(8, 0), ahmed, sara);
        followUp(WED_4_NOV).andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].name", contains("Sara Ali")));

        days.at(WED_4_NOV, LocalTime.of(9, 10));
        evaluate(ahmed, yesterday, sara, 4, "Calmer at the trot")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"));

        followUp(TUE_3_NOV)
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(1))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].riderId", contains(omar.toString())));
        followUp(WED_4_NOV)
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(1))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[*].sessionId", contains(morning.toString())))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].riderId", contains(sara.toString())));
        pending(ahmed)
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.sessions[*].sessionId", contains(morning.toString(), yesterday.toString())));
    }

    // 9
    @Test
    void aMovedAndPromotedRiderKeepsTheirHistoryAtTheOldLevel() throws Exception {
        UUID sara = paidRiders("Sara Ali").getFirst();
        UUID four = days.ride(TUE_3_NOV, FOUR_PM, ahmed, sara);
        evaluate(ahmed, four, sara, 4, "Better heels down").andExpect(status().isOk());
        UUID six = days.ride(TUE_3_NOV, SIX_PM, ahmed, sara);
        evaluate(ahmed, six, sara, 3, "Rushed the canter").andExpect(status().isOk());

        days.toMona(sara);
        fx.promote(mona, sara, LEVEL_TWO).andExpect(status().isOk());
        assertEvaluationInvariants();

        history(ahmed, sara)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
        for (ResultActions history : List.of(history(mona, sara), adminHistory(sara))) {
            history.andExpect(status().isOk())
                    .andExpect(jsonPath("$.evaluations[*].sessionId", contains(six.toString(), four.toString())))
                    .andExpect(jsonPath("$.evaluations[*].levelId", contains(LEVEL_TWO, LEVEL_TWO)))
                    .andExpect(jsonPath("$.evaluations[*].trainerName", contains("Ahmed Said", "Ahmed Said")))
                    .andExpect(jsonPath("$.evaluations[*].stars", contains(3, 4)));
        }
    }

    // 10
    @Test
    void aRiderAtZeroAfterTheFinishCanStillBeEvaluated() throws Exception {
        UUID nour = days.placeRiders("Nour Adel").getFirst();
        days.pay(nour, NOV, 1);
        UUID session = days.ride(TUE_3_NOV, FIVE_PM, ahmed, nour);
        days.payments().assertBalance(nour, 0, "awaiting_payment", null);
        days.session(ahmed, session).andExpect(jsonPath(seat(nour) + ".canEvaluate", contains(true)));

        evaluate(ahmed, session, nour, 2, "Needs more practice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"));
    }

    // 11
    @Test
    void aDoneSessionCantBeFilledFinishedOrCancelled() throws Exception {
        List<UUID> riders = paidRiders("Sara Ali", "Omar Hassan");
        UUID sara = riders.get(0);
        UUID omar = riders.get(1);
        UUID session = days.ride(TUE_3_NOV, FIVE_PM, ahmed, sara);
        evaluate(ahmed, session, sara, 4, null).andExpect(jsonPath("$.status").value("done"));

        days.assign(ahmed, session, sara, omar)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_read_only"));
        days.finish(ahmed, session)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_already_finished"));
        // A session is cancelled only by deleting or moving its W1 template: that cancels the template's upcoming
        // Tuesdays and leaves the done one alone.
        fx.deleteTemplate(days.templateOf(session)).andExpect(status().isOk());
        assertEvaluationInvariants();
        assertThat(fx.jdbc().queryForObject("select status from session where id = ?", String.class, session))
                .isEqualTo("done");

        days.session(ahmed, session)
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.mySeats[*].riderId", contains(sara.toString())));
        days.payments().assertBalance(omar, 8, "active", "2026-12-31");
    }

    // 12
    @Test
    void theOverviewShowsADoneSessionAsFinished() throws Exception {
        UUID sara = paidRiders("Sara Ali").getFirst();
        UUID session = days.ride(TUE_3_NOV, FIVE_PM, ahmed, sara);
        evaluate(ahmed, session, sara, 5, null).andExpect(jsonPath("$.status").value("done"));

        days.at(TUE_3_NOV, HALF_PAST_EIGHT);

        days.overview().andExpect(jsonPath(today(session) + ".state", contains("finished")));
    }

    // 13
    @Test
    void bothCoachesSavingTheirLastRiderAtOnceMakeTheSessionDoneOnce() throws Exception {
        SharedSession shared = sharedSession();
        days.at(TUE_3_NOV, FIVE_PAST_SIX);
        days.finish(ahmed, shared.session()).andExpect(status().isOk());
        evaluate(ahmed, shared.session(), shared.omar(), 3, null).andExpect(status().isOk());
        days.signInEveryone();

        List<String> statuses = raceOutcomes(
                () -> statusAfter(evaluateRequest(ahmed, shared.session(), shared.sara(), 4, null)),
                () -> statusAfter(evaluateRequest(mona, shared.session(), shared.hala(), 5, null)));

        // The session row is locked, so the second save sees the first and is the one that makes it done.
        assertThat(statuses).containsExactlyInAnyOrder("finished", "done");
        assertThat(evaluationsIn(shared.session())).isEqualTo(3);
        assertEvaluationInvariants();
        days.session(mona, shared.session()).andExpect(jsonPath("$.status").value("done"));
    }

    // 14
    @Test
    void twoSavesForTheSameRiderAtOnceSaveOne() throws Exception {
        List<UUID> riders = paidRiders("Sara Ali", "Omar Hassan");
        UUID sara = riders.get(0);
        UUID session = days.ride(TUE_3_NOV, FIVE_PM, ahmed, sara, riders.get(1));
        days.signInEveryone();

        List<String> outcomes = race(
                () -> evaluateRequest(ahmed, session, sara, 4, "First tap").andReturn(),
                () -> evaluateRequest(ahmed, session, sara, 2, "Second tap").andReturn());

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "already_evaluated");
        assertThat(evaluationsIn(session)).isEqualTo(1);
        assertEvaluationInvariants();
        days.session(ahmed, session).andExpect(jsonPath("$.status").value("finished"));
        pending(ahmed).andExpect(jsonPath("$.count").value(1));
    }

    // ---- 15: the rules that span the endpoints

    // E13's same-day invariants; every evaluation is for a rider seated in a finished or done session and was
    // written by the coach who seated them; a level session is done exactly when it is finished and every seat has
    // an evaluation, and an unfinished one has none; screen 3 offers canEvaluate exactly on the finished session's
    // seats without one; for each trainer, Home's pending list matches the follow-up's toEvaluate lists, session
    // by session and in total.
    private void assertEvaluationInvariants() throws Exception {
        days.assertSameDayInvariants();

        assertThat(fx.jdbc().queryForObject("""
                select count(*) from session_evaluation e
                join session s on s.id = e.session_id
                where s.status not in ('finished', 'done')""", Integer.class))
                .as("evaluations of a session not finished").isZero();
        assertThat(fx.jdbc().queryForObject("""
                select count(*) from session_evaluation e
                join assignment a on a.session_id = e.session_id and a.rider_id = e.rider_id
                where e.trainer_id <> a.assigned_by""", Integer.class))
                .as("evaluations written by someone other than the seating coach").isZero();

        List<Map<String, Object>> sessions = fx.jdbc().queryForList("""
                select s.id, s.status,
                       (select count(*) from assignment a where a.session_id = s.id) as seated,
                       (select count(*) from session_evaluation e where e.session_id = s.id) as evaluated
                from session s join session_template t on t.id = s.template_id
                where t.level_id is not null and s.status in ('finished', 'done')""");
        for (Map<String, Object> s : sessions) {
            long seated = ((Number) s.get("seated")).longValue();
            long evaluated = ((Number) s.get("evaluated")).longValue();
            assertThat("done".equals(s.get("status")))
                    .as("session %s (%d seats, %d evaluations) is done exactly when all are evaluated",
                            s.get("id"), seated, evaluated)
                    .isEqualTo(evaluated == seated);
        }

        for (UUID id : days.opened()) {
            for (Coach coach : List.of(ahmed, mona)) {
                String detail = body(days.session(coach, id));
                boolean finished = "finished".equals(JsonPath.read(detail, "$.status"));
                List<Map<String, Object>> seats = JsonPath.read(detail, "$.mySeats");
                for (Map<String, Object> seat : seats) {
                    assertThat(seat.get("canEvaluate"))
                            .as("%s: canEvaluate for %s", coach.name(), seat.get("name"))
                            .isEqualTo(finished && seat.get("evaluation") == null);
                }
            }
        }

        List<LocalDate> dates = fx.jdbc().queryForList(
                "select distinct session_date from session where status in ('finished', 'done')", Date.class)
                .stream().map(Date::toLocalDate).toList();
        Map<LocalDate, String> followUps = new HashMap<>();
        for (LocalDate date : dates) {
            followUps.put(date, body(followUp(date)));
        }
        for (Coach coach : List.of(ahmed, mona)) {
            String pending = body(pending(coach));
            Map<String, Integer> onHome = new HashMap<>();
            List<Map<String, Object>> pendingSessions = JsonPath.read(pending, "$.sessions");
            for (Map<String, Object> s : pendingSessions) {
                onHome.put((String) s.get("sessionId"), (Integer) s.get("ridersToEvaluate"));
            }
            Map<String, Integer> onFollowUp = new HashMap<>();
            for (String followUp : followUps.values()) {
                List<List<Map<String, Object>>> lists = JsonPath.read(followUp,
                        "$.trainers[?(@.trainerId == '" + coach.id() + "')].toEvaluate");
                for (List<Map<String, Object>> toEvaluate : lists) {
                    for (Map<String, Object> s : toEvaluate) {
                        onFollowUp.put((String) s.get("sessionId"), ((List<?>) s.get("riders")).size());
                    }
                }
            }
            assertThat(onHome).as("%s: Home's pending sessions = follow-up's to evaluate", coach.name())
                    .isEqualTo(onFollowUp);
            assertThat(((Number) JsonPath.read(pending, "$.count")).intValue())
                    .as("%s: Home's pending count", coach.name())
                    .isEqualTo(onFollowUp.values().stream().mapToInt(Integer::intValue).sum());
        }
    }

    // ---- Steps

    // Placed at Level 2 as Ahmed's and paid 8 November sessions each. The rider ids, in the order of the names.
    private List<UUID> paidRiders(String... names) throws Exception {
        List<UUID> riders = days.placeRiders(names);
        for (UUID rider : riders) {
            days.pay(rider, NOV, null);
        }
        return riders;
    }

    // Tuesday's 17:00 session with Ahmed's Sara and Omar and Mona's Hala, filled at 16:50 and not finished yet.
    private SharedSession sharedSession() throws Exception {
        List<UUID> riders = paidRiders("Sara Ali", "Omar Hassan", "Hala Samir");
        SharedSession shared = new SharedSession(null, riders.get(0), riders.get(1), riders.get(2));
        days.toMona(shared.hala());
        days.at(TUE_3_NOV, TEN_TO_FIVE);
        UUID session = days.todaysSession(FIVE_PM);
        days.assign(ahmed, session, shared.sara(), shared.omar()).andExpect(status().isOk());
        days.assign(mona, session, shared.hala()).andExpect(status().isOk());
        return new SharedSession(session, shared.sara(), shared.omar(), shared.hala());
    }

    private record SharedSession(UUID session, UUID sara, UUID omar, UUID hala) {
    }

    // Screen 3's evaluation sheet: one rider at a time; comment null leaves it out.
    private ResultActions evaluate(Coach coach, UUID sessionId, UUID riderId, int stars, String comment)
            throws Exception {
        ResultActions result = evaluateRequest(coach, sessionId, riderId, stars, comment);
        assertEvaluationInvariants();
        return result;
    }

    private ResultActions evaluateRequest(Coach coach, UUID sessionId, UUID riderId, int stars, String comment)
            throws Exception {
        String extra = comment == null ? "" : ", \"comment\": \"" + comment + "\"";
        return fx.asCoach(coach, post("/me/sessions/" + sessionId + "/evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"riderId\": \"%s\", \"stars\": %d%s }".formatted(riderId, stars, extra)));
    }

    // The session status the save answered with; the save must succeed.
    private static String statusAfter(ResultActions saved) throws Exception {
        saved.andExpect(status().isOk());
        return JsonPath.read(body(saved), "$.status");
    }

    // ---- What the screens show

    // Home.
    private ResultActions pending(Coach coach) throws Exception {
        return fx.asCoach(coach, get("/me/evaluations/pending")).andExpect(status().isOk());
    }

    // The admin's Follow-up screen for that date.
    private ResultActions followUp(LocalDate date) throws Exception {
        return fx.accounts().admin(get("/admin/evaluations/follow-up").param("date", date.toString()))
                .andExpect(status().isOk());
    }

    // Rider level (4a), whatever the answer.
    private ResultActions history(Coach coach, UUID riderId) throws Exception {
        return fx.asCoach(coach, get("/me/riders/" + riderId + "/evaluations"));
    }

    // W3's Evaluations section.
    private ResultActions adminHistory(UUID riderId) throws Exception {
        return fx.accounts().admin(get("/admin/riders/" + riderId + "/evaluations"));
    }

    private int evaluationsIn(UUID sessionId) {
        return fx.jdbc().queryForObject("select count(*) from session_evaluation where session_id = ?",
                Integer.class, sessionId);
    }

    private static String seat(UUID riderId) {
        return "$.mySeats[?(@.riderId == '" + riderId + "')]";
    }

    private static String today(UUID sessionId) {
        return "$.sessionsToday[?(@.sessionId == '" + sessionId + "')]";
    }
}
