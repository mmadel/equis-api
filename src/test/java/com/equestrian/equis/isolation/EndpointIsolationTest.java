package com.equestrian.equis.isolation;

import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.equestrian.equis.support.TwoClubFixture;
import com.equestrian.equis.support.TwoClubFixture.ClubData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

// E17 · cases 3–7: every club endpoint, called with club A's tokens, shows only club A's data, and answers 404 for
// club B's ids (in the path or the body) without changing anything in club B. Club B (cairo-eq) is the "other club"
// throughout; the rider endpoints are also called with B's rider (Laila) to check the other way round.
// EndpointCoverageGuardTest fails when a club endpoint has no case here (COVERED).
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(TwoClubFixture.Config.class)
class EndpointIsolationTest {
    enum Who { ADMIN, TRAINER, RIDER }

    // LIST: own data only. OTHER: an id of club B -> 404, club B unchanged. UNKNOWN: club B's id gets exactly the
    // answer an id that doesn't exist gets (for checks that refuse unknown ids with another status), B unchanged.
    // OWN: a write without ids of another club -> 2xx, club B unchanged.
    enum Kind { LIST, OTHER, UNKNOWN, OWN }

    record Case(String method, String mapping, Who who, Kind kind,
                Function<Ids, String> path, Function<Ids, String> body) {
        String name() {
            return method + " " + mapping + " (" + kind.name().toLowerCase() + ")";
        }
    }

    // The ids a case fills in: club A's own and club B's.
    record Ids(ClubData a, ClubData b) {
    }

    static final List<Case> CASES = List.of(
            // ---- admin console
            list("GET", "/admin/assessment/slots"),
            list("GET", "/admin/bookings"),
            list("GET", "/admin/club/usage"),
            list("GET", "/admin/evaluations/follow-up"),
            list("GET", "/admin/overview"),
            list("GET", "/admin/riders"),
            new Case("GET", "/admin/session-templates", Who.ADMIN, Kind.LIST, x -> "/admin/session-templates?level=1", null),
            list("GET", "/admin/trainers"),
            other(Who.ADMIN, "GET", "/admin/riders/{id}/evaluations", x -> "/admin/riders/" + x.b().rider1() + "/evaluations", null),
            other(Who.ADMIN, "GET", "/admin/riders/{id}/payments", x -> "/admin/riders/" + x.b().rider1() + "/payments", null),
            other(Who.ADMIN, "DELETE", "/admin/session-templates/{id}", x -> "/admin/session-templates/" + x.b().noonTemplate(), null),
            other(Who.ADMIN, "DELETE", "/admin/trainers/{id}/invite", x -> "/admin/trainers/" + x.b().trainer2() + "/invite", null),
            other(Who.ADMIN, "PATCH", "/admin/bookings/{id}", x -> "/admin/bookings/" + x.b().pendingBooking(),
                    x -> "{ \"assessorId\": \"" + x.a().trainer1() + "\" }"),
            other(Who.ADMIN, "PATCH", "/admin/members/{id}", x -> "/admin/members/" + x.b().pendingMember(),
                    x -> "{ \"name\": \"Renamed\", \"phone\": \"01159999999\", \"email\": \"renamed@school.eg\" }"),
            other(Who.ADMIN, "PATCH", "/admin/riders/trainer", x -> "/admin/riders/trainer",
                    x -> "{ \"riderIds\": [\"" + x.b().rider1() + "\"], \"trainerId\": \"" + x.a().trainer2() + "\" }"),
            // Own riders given to the other club's trainer.
            other(Who.ADMIN, "PATCH", "/admin/riders/trainer", x -> "/admin/riders/trainer",
                    x -> "{ \"riderIds\": [\"" + x.a().rider1() + "\"], \"trainerId\": \"" + x.b().trainer2() + "\" }"),
            other(Who.ADMIN, "POST", "/admin/bookings/{id}/cancel", x -> "/admin/bookings/" + x.b().pendingBooking() + "/cancel",
                    x -> "{ \"reason\": \"person_cancelled\" }"),
            other(Who.ADMIN, "POST", "/admin/bookings/{id}/remind", x -> "/admin/bookings/" + x.b().pendingBooking() + "/remind", null),
            // A new person booked into the other club's slot, with the other club's assessor.
            other(Who.ADMIN, "POST", "/admin/members", x -> "/admin/members",
                    x -> "{ \"name\": \"Visitor\", \"phone\": \"01158888888\", \"email\": \"visitor@school.eg\", \"slotId\": \""
                            + x.b().assessmentSlot() + "\", \"assessorId\": \"" + x.b().trainer1() + "\" }"),
            other(Who.ADMIN, "POST", "/admin/members/{id}/close", x -> "/admin/members/" + x.b().pendingMember() + "/close", null),
            other(Who.ADMIN, "POST", "/admin/members/{id}/reopen", x -> "/admin/members/" + x.b().closedMember() + "/reopen", null),
            other(Who.ADMIN, "POST", "/admin/payments/{id}/void", x -> "/admin/payments/" + x.b().payment1() + "/void",
                    x -> "{ \"reason\": \"Entered by mistake\" }"),
            other(Who.ADMIN, "POST", "/admin/riders/{id}/invite", x -> "/admin/riders/" + x.b().rider2() + "/invite", null),
            other(Who.ADMIN, "POST", "/admin/riders/{id}/payments", x -> "/admin/riders/" + x.b().rider1() + "/payments",
                    x -> "{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-11\", \"months\": 1 }"),
            other(Who.ADMIN, "POST", "/admin/riders/{id}/reset-password", x -> "/admin/riders/" + x.b().rider1() + "/reset-password", null),
            other(Who.ADMIN, "POST", "/admin/trainers/{id}/disable", x -> "/admin/trainers/" + x.b().trainer2() + "/disable", null),
            other(Who.ADMIN, "POST", "/admin/trainers/{id}/enable", x -> "/admin/trainers/" + x.b().trainer2() + "/enable", null),
            other(Who.ADMIN, "POST", "/admin/trainers/{id}/resend-invite", x -> "/admin/trainers/" + x.b().trainer2() + "/resend-invite", null),
            other(Who.ADMIN, "POST", "/admin/trainers/{id}/reset-password", x -> "/admin/trainers/" + x.b().trainer2() + "/reset-password", null),
            other(Who.ADMIN, "PUT", "/admin/session-templates/{id}", x -> "/admin/session-templates/" + x.b().noonTemplate(),
                    x -> template("SATURDAY")),
            own(Who.ADMIN, "POST", "/admin/session-templates", x -> "/admin/session-templates", x -> template("SUNDAY")),
            own(Who.ADMIN, "POST", "/admin/trainers", x -> "/admin/trainers",
                    x -> "{ \"name\": \"New Coach\", \"phone\": \"01057777777\", \"email\": \"newcoach@school.eg\" }"),

            // ---- trainer app
            list(Who.TRAINER, "GET", "/assessment/slots"),
            list(Who.TRAINER, "GET", "/me/evaluations/pending"),
            list(Who.TRAINER, "GET", "/me/levels"),
            new Case("GET", "/me/levels/{id}/sessions", Who.TRAINER, Kind.LIST,
                    x -> "/me/levels/1/sessions?from=2026-10-12&to=2026-10-18", null),
            list(Who.TRAINER, "GET", "/me/riders"),
            list(Who.TRAINER, "GET", "/me/week"),
            other(Who.TRAINER, "GET", "/me/assessment/slots/{id}", x -> "/me/assessment/slots/" + x.b().assessmentSlot(), null),
            other(Who.TRAINER, "GET", "/me/riders/{id}/evaluations", x -> "/me/riders/" + x.b().rider1() + "/evaluations", null),
            other(Who.TRAINER, "GET", "/me/riders/{id}/level-history", x -> "/me/riders/" + x.b().rider2() + "/level-history", null),
            other(Who.TRAINER, "GET", "/me/sessions/{id}", x -> "/me/sessions/" + x.b().finishedSession(), null),
            other(Who.TRAINER, "POST", "/me/bookings/{id}/result", x -> "/me/bookings/" + x.b().pendingBooking() + "/result",
                    x -> "{ \"attended\": false, \"levelId\": null }"),
            other(Who.TRAINER, "POST", "/me/riders/{id}/level-change", x -> "/me/riders/" + x.b().rider1() + "/level-change",
                    x -> "{ \"direction\": \"up\", \"fromLevelId\": 1, \"reason\": \"Ready for the next level\" }"),
            other(Who.TRAINER, "POST", "/me/sessions/{id}/evaluations", x -> "/me/sessions/" + x.b().finishedSession() + "/evaluations",
                    x -> "{ \"riderId\": \"" + x.b().rider1() + "\", \"stars\": 2 }"),
            other(Who.TRAINER, "POST", "/me/sessions/{id}/finish", x -> "/me/sessions/" + x.b().eveningSession() + "/finish", null),
            other(Who.TRAINER, "PUT", "/me/sessions/{id}/assignments", x -> "/me/sessions/" + x.b().eveningSession() + "/assignments",
                    x -> "{ \"riderIds\": [\"" + x.b().rider2() + "\"] }"),
            // The other club's rider seated in an own session: refused like a rider id that doesn't exist (403
            // "not your rider" for both), so it tells nothing about club B.
            new Case("PUT", "/me/sessions/{id}/assignments", Who.TRAINER, Kind.UNKNOWN, x -> "/me/sessions/" + x.a().eveningSession() + "/assignments",
                    x -> "{ \"riderIds\": [\"" + x.a().rider1() + "\", \"" + x.b().rider2() + "\"] }"),

            // ---- rider app (case 7)
            list(Who.RIDER, "GET", "/rider/assessment"),
            list(Who.RIDER, "GET", "/rider/me"),
            list(Who.RIDER, "GET", "/rider/payments"),
            list(Who.RIDER, "GET", "/rider/progress"),
            list(Who.RIDER, "GET", "/rider/sessions"),

            // ---- the person's own club, any role (E17 · club access state)
            list(Who.TRAINER, "GET", "/club/status"));

    // Every mapping with a case, as "METHOD /path/{id}", for the coverage guard.
    static final Set<String> COVERED = CASES.stream()
            .map(c -> c.method() + " " + c.mapping()).collect(Collectors.toUnmodifiableSet());

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
        TrainerAccountsFixture.adminAccount(registry);
    }

    @TestBean
    Clock clock;

    static Clock clock() {
        return new MutableClock(TrainerAccountsFixture.START);
    }

    @Autowired
    TwoClubFixture fx;

    // Cases 3, 4, 5 and 7, one dynamic test per case.
    @TestFactory
    Stream<DynamicTest> everyClubEndpointKeepsToItsClub() {
        Ids ids = new Ids(fx.a(), fx.b());
        return CASES.stream().map(c -> DynamicTest.dynamicTest(c.name(), () -> run(c, ids)));
    }

    // Case 7, the other way round: Laila (club B's rider) sees nothing of club A.
    @TestFactory
    Stream<DynamicTest> theOtherClubsRiderSeesOnlyHerOwnData() {
        ClubData b = fx.b();
        Set<String> aIds = fx.idsOf(fx.a().clubId());
        return CASES.stream().filter(c -> c.who() == Who.RIDER).map(c -> DynamicTest.dynamicTest(c.name(), () -> {
            String body = TwoClubFixture.body(fx.perform(b.riderToken(), request(HttpMethod.valueOf(c.method()), c.mapping()))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200)));
            assertNoneOf(body, aIds, "@school.eg");
        }));
    }

    // Case 6: the overview counts only club A (both clubs were built the same way).
    @Test
    void theOverviewCountsOnlyTheAdminsClub() throws Exception {
        ClubData a = fx.a();
        String body = TwoClubFixture.body(fx.perform(a.adminToken(), request(HttpMethod.GET, "/admin/overview")));

        // Club A alone: today's assessment slot and two Level 1 sessions; booked seats are the seats in sessions
        // plus the bookings in the slot that aren't cancelled; one result overdue.
        assertThat((List<?>) JsonPath.read(body, "$.sessionsToday")).hasSize(3);
        long liveBookings = fx.count("assessment_booking", a.clubId()) - 1;   // the closed person's was cancelled
        assertThat((Integer) JsonPath.read(body, "$.week.booked"))
                .isEqualTo((int) (fx.count("assignment", a.clubId()) + liveBookings));
        assertThat((Integer) JsonPath.read(body, "$.attention.overdueResultsCount")).isEqualTo(1);
    }

    private void run(Case c, Ids ids) throws Exception {
        String token = switch (c.who()) {
            case ADMIN -> ids.a().adminToken();
            case TRAINER -> ids.a().trainer1Token();
            case RIDER -> ids.a().riderToken();
        };
        UUID b = ids.b().clubId();
        Map<String, String> bBefore = fx.snapshotOf(b);
        String path = c.path().apply(ids);
        String sent = c.body() == null ? null : c.body().apply(ids);
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(c.method()), path);
        if (sent != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(sent);
        }
        // An answer may repeat the ids the caller sent (the 404's "instance", "riderIds"): the caller knew them.
        Set<String> otherIds = new java.util.HashSet<>(fx.idsOf(b));
        otherIds.removeIf(id -> path.contains(id) || (sent != null && sent.contains(id)));
        ResultActions result = fx.perform(token, request);
        int status = result.andReturn().getResponse().getStatus();
        String body = TwoClubFixture.body(result);

        switch (c.kind()) {
            case LIST -> {
                assertThat(status).as(body).isEqualTo(200);
                assertNoneOf(body, otherIds, "@cairo-eq.eg");
            }
            case OTHER -> {
                assertThat(status).as("%s answered %s", c.name(), body).isEqualTo(404);
                assertNoneOf(body, otherIds, "@cairo-eq.eg");
            }
            case UNKNOWN -> {
                String unknownPath = withUnknownIds(path, ids.b());
                MockHttpServletRequestBuilder unknown = request(HttpMethod.valueOf(c.method()), unknownPath);
                if (sent != null) {
                    unknown.contentType(MediaType.APPLICATION_JSON).content(withUnknownIds(sent, ids.b()));
                }
                ResultActions asUnknown = fx.perform(token, unknown);
                assertThat(status).as(body).isBetween(400, 499);
                assertThat(status).isEqualTo(asUnknown.andReturn().getResponse().getStatus());
                assertThat((String) JsonPath.read(body, "$.code"))
                        .isEqualTo(JsonPath.read(TwoClubFixture.body(asUnknown), "$.code"));
                assertNoneOf(body, otherIds, "@cairo-eq.eg");
            }
            case OWN -> {
                assertThat(status).as(body).isBetween(200, 299);
                assertNoneOf(body, otherIds, "@cairo-eq.eg");
            }
        }
        assertThat(fx.snapshotOf(b)).as("club B unchanged by %s", c.name()).isEqualTo(bBefore);
    }

    // The same text with club B's ids swapped for ids that exist nowhere.
    private static String withUnknownIds(String text, ClubData b) {
        String result = text;
        for (UUID id : List.of(b.rider1(), b.rider2(), b.eveningSession(), b.finishedSession(), b.pendingBooking())) {
            result = result.replace(id.toString(), UUID.randomUUID().toString());
        }
        return result;
    }

    // No id and no email of the other club anywhere in the answer.
    private static void assertNoneOf(String body, Set<String> otherIds, String otherEmailDomain) {
        assertThat(body).doesNotContain(otherEmailDomain);
        for (String id : otherIds) {
            assertThat(body).as("leaks the other club's row %s", id).doesNotContain(id);
        }
    }

    private static Case list(String method, String mapping) {
        return list(Who.ADMIN, method, mapping);
    }

    private static Case list(Who who, String method, String mapping) {
        return new Case(method, mapping, who, Kind.LIST, x -> mapping, null);
    }

    private static Case other(Who who, String method, String mapping, Function<Ids, String> path, Function<Ids, String> body) {
        return new Case(method, mapping, who, Kind.OTHER, path, body);
    }

    private static Case own(Who who, String method, String mapping, Function<Ids, String> path, Function<Ids, String> body) {
        return new Case(method, mapping, who, Kind.OWN, path, body);
    }

    private static String template(String day) {
        return """
                { "levelId": 2, "dayOfWeek": "%s", "start": "09:00", "end": "10:00", "capacity": 4, "validFrom": "2026-10-16" }"""
                .formatted(day);
    }
}
