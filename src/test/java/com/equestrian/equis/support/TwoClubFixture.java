package com.equestrian.equis.support;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.entity.club.Club;
import com.jayway.jsonpath.JsonPath;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// E17 · two complete clubs for the isolation tests: the default club "school" (A) and "cairo-eq" (B).
// Only the clubs and their admins are written with SQL (there is no endpoint for them yet); everything else goes
// through the real API with that club's tokens, so the data stays valid as the services change.
//
// Per club: an admin; two trainers; an assessment slot, a 12:00 and a 17:00 Level 1 session on Friday
// 16 Oct 2026; four applicants: two placed as riders, one still waiting (with an overdue reminder notice for its
// assessor), one closed; a paid October package for each rider; both riders seated and evaluated in the finished
// 12:00 session, the first also seated at 17:00; the second moved up to Level 2; and a rider account for the
// first rider.
// The steps run for both clubs side by side, so the clock only moves forward. It ends at 16:55.
public class TwoClubFixture {
    public static final UUID A = Club.DEFAULT_ID;
    public static final LocalDate FRIDAY = LocalDate.of(2026, 10, 16);
    public static final String PASSWORD = "Isolation@2026";

    // The school tables (V23 classification): every one has club_id and row-level security.
    public static final List<String> TENANT_TABLES = List.of(
            "admin_account", "trainer", "session_template", "session", "rider", "assignment", "member",
            "assessment_booking", "level_change", "notice", "payment", "session_usage", "session_evaluation");

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final RecordingNotificationSender recorder;
    private final MutableClock clock;
    private final PasswordEncoder passwordEncoder;

    private final Map<UUID, ClubData> clubs = new LinkedHashMap<>();
    private UUID b;

    TwoClubFixture(MockMvc mvc, JdbcTemplate jdbc, RecordingNotificationSender recorder, MutableClock clock,
                   PasswordEncoder passwordEncoder) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.recorder = recorder;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
    }

    @TestConfiguration
    @Import(TrainerAccountsFixture.Config.class)
    public static class Config {
        @Bean
        TwoClubFixture twoClubFixture(MockMvc mvc, JdbcTemplate jdbc, RecordingNotificationSender recorder,
                                      Clock clock, PasswordEncoder passwordEncoder) {
            return new TwoClubFixture(mvc, jdbc, recorder, (MutableClock) clock, passwordEncoder);
        }
    }

    // What one club holds; tokens come from the real sign-in.
    public record ClubData(UUID clubId, String code, String adminToken,
                           UUID trainer1, String trainer1Token, UUID trainer2, String trainer2Token,
                           UUID assessmentTemplate, UUID assessmentSlot, UUID noonTemplate, UUID finishedSession,
                           UUID eveningSession,
                           UUID rider1, UUID rider2, UUID payment1, String riderToken,
                           UUID pendingMember, UUID pendingBooking, UUID closedMember, UUID rider1Member) {
    }

    public ClubData a() {
        build();
        return clubs.get(A);
    }

    public ClubData b() {
        build();
        return clubs.get(b);
    }

    // Built once per Spring context (each test class has its own database). Later calls return the same clubs.
    public synchronized void build() {
        if (!clubs.isEmpty()) {
            return;
        }
        clock.set(at(LocalTime.of(10, 0)));
        recorder.clear();
        root(() -> {
            // Start from no school data at all (the seeds of V4, V5, V8 and V12 go too).
            jdbc.execute("truncate table club cascade");
            jdbc.update("INSERT INTO club (id, code, name) VALUES (?, 'school', 'Riding School')", A);
            b = jdbc.queryForObject(
                    "INSERT INTO club (code, name) VALUES ('cairo-eq', 'Cairo Equestrian') RETURNING id", UUID.class);
            for (UUID club : List.of(A, b)) {
                jdbc.update("INSERT INTO admin_account (club_id, name, email, password_hash) VALUES (?, 'Admin', ?, ?)",
                        club, adminEmail(club), passwordEncoder.encode(PASSWORD));
            }
            return null;
        });

        Map<UUID, Builder> builders = new LinkedHashMap<>();
        builders.put(A, new Builder(A, "school", "A"));
        builders.put(b, new Builder(b, "cairo-eq", "B"));
        try {
            for (Builder x : builders.values()) {
                x.adminToken = login(x.code, adminEmail(x.club));
                x.trainer1 = x.activeTrainer("Trainer One", 1);
                x.trainer2 = x.activeTrainer("Trainer Two", 2);
                x.signIn();
                x.assessmentTemplate = x.template(null, LocalTime.of(11, 0), 4);
                x.noonTemplate = x.template(1, LocalTime.of(12, 0), 6);
                x.eveningTemplate = x.template(1, LocalTime.of(17, 0), 6);
                x.assessmentSlot = sessionOf(x.assessmentTemplate);
                x.finishedSession = sessionOf(x.noonTemplate);
                x.eveningSession = sessionOf(x.eveningTemplate);
                x.booking1 = x.applicant("Rider One", 1);
                x.booking2 = x.applicant("Rider Two", 2);
                x.pendingBooking = x.applicant("Waiting Person", 3);
                UUID closedBooking = x.applicant("Closed Person", 4);
                x.pendingMember = memberOf(x.pendingBooking);
                x.closedMember = memberOf(closedBooking);
                x.rider1Member = memberOf(x.booking1);
                expect(x.admin(post("/admin/bookings/" + closedBooking + "/cancel").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"person_cancelled\" }")), 200);
                expect(x.admin(post("/admin/members/" + x.closedMember + "/close")), 200);
            }
            clock.set(at(LocalTime.of(11, 30)));
            for (Builder x : builders.values()) {
                x.signIn();
                x.rider1 = x.place(x.booking1);
                x.rider2 = x.place(x.booking2);
                x.payment1 = x.pay(x.rider1);
                x.pay(x.rider2);
            }
            clock.set(at(LocalTime.of(11, 55)));
            for (Builder x : builders.values()) {
                x.signIn();
                expect(x.assign(x.finishedSession, x.rider1, x.rider2), 200);
            }
            clock.set(at(LocalTime.of(13, 5)));
            for (Builder x : builders.values()) {
                x.signIn();
                expect(x.trainer1(post("/me/sessions/" + x.finishedSession + "/finish")), 200);
                x.evaluate(x.rider1);
                x.evaluate(x.rider2);
                // Rider 2 moves up a level (a level_change row); it has no later seat to lose.
                expect(x.trainer1(post("/me/riders/" + x.rider2 + "/level-change").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"direction\": \"up\", \"fromLevelId\": 1, \"reason\": \"Ready for Level 2\" }")),
                        200, 201);
                // The waiting person's slot ended at 12:00: the admin reminds the assessor (a notice).
                expect(x.admin(post("/admin/bookings/" + x.pendingBooking + "/remind")), 200, 201, 204);
                x.riderToken = x.riderAccount(x.rider1);
            }
            clock.set(at(LocalTime.of(16, 55)));
            for (Builder x : builders.values()) {
                x.signIn();
                expect(x.assign(x.eveningSession, x.rider1), 200);
                clubs.put(x.club, x.data());
            }
        } catch (Exception e) {
            clubs.clear();
            throw new IllegalStateException("Building the two clubs failed", e);
        }
    }

    // ---- What the isolation tests check against

    // Every id of every school row of the club, as text: a response of the other club must contain none of them.
    public Set<String> idsOf(UUID club) {
        Set<String> ids = new HashSet<>();
        root(() -> {
            for (String table : TENANT_TABLES) {
                ids.addAll(jdbc.queryForList("SELECT id::text FROM " + table + " WHERE club_id = ?", String.class, club));
            }
            return null;
        });
        return ids;
    }

    // A fingerprint of every school row of the club, to prove a refused write changed nothing there.
    public Map<String, String> snapshotOf(UUID club) {
        Map<String, String> tables = new LinkedHashMap<>();
        root(() -> {
            for (String table : TENANT_TABLES) {
                tables.put(table, jdbc.queryForObject(
                        "SELECT coalesce(md5(string_agg(t::text, '|' ORDER BY t.id)), '') FROM " + table
                                + " t WHERE t.club_id = ?", String.class, club));
            }
            return null;
        });
        return tables;
    }

    public long count(String table, UUID club) {
        return root(() -> jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE club_id = ?", Long.class, club));
    }

    public ResultActions perform(String token, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    public static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    public static ZonedDateTime cairo(LocalTime time) {
        return ZonedDateTime.of(FRIDAY, time, TrainerAccountsFixture.CAIRO);
    }

    // ---- Building one club

    private final class Builder {
        final UUID club;
        final String code;
        final String tag;
        String adminToken;
        UUID trainer1;
        UUID trainer2;
        String trainer1Token;
        String trainer2Token;
        UUID assessmentTemplate;
        UUID noonTemplate;
        UUID eveningTemplate;
        UUID assessmentSlot;
        UUID finishedSession;
        UUID eveningSession;
        UUID booking1;
        UUID booking2;
        UUID pendingBooking;
        UUID pendingMember;
        UUID closedMember;
        UUID rider1Member;
        UUID rider1;
        UUID rider2;
        UUID payment1;
        String riderToken;

        Builder(UUID club, String code, String tag) {
            this.club = club;
            this.code = code;
            this.tag = tag;
        }

        // Phones and emails differ between the clubs; PerClubSignInTest covers clubs sharing a phone.
        String phone(int n) {
            return "0105%s00000%d".formatted(tag.equals("A") ? "1" : "2", n);
        }

        UUID activeTrainer(String name, int n) throws Exception {
            recorder.clear();
            ResultActions invited = admin(post("/admin/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                    { "name": "%s %s", "phone": "%s", "email": "trainer%d@%s.eg" }"""
                    .formatted(name, tag, phone(n), n, code)));
            expect(invited, 201);
            acceptLink();
            return UUID.fromString(JsonPath.read(body(invited), "$.id"));
        }

        UUID template(Integer levelId, LocalTime start, int capacity) throws Exception {
            ResultActions saved = admin(post("/admin/session-templates").contentType(MediaType.APPLICATION_JSON).content("""
                    { "levelId": %s, "dayOfWeek": "FRIDAY", "start": "%s", "end": "%s", "capacity": %d, "validFrom": "%s" }"""
                    .formatted(levelId, start, start.plusHours(1), capacity, FRIDAY)));
            expect(saved, 201);
            return UUID.fromString(JsonPath.read(body(saved), "$.template.id"));
        }

        // A new person booked into today's assessment slot with trainer 1 as assessor. The booking id.
        UUID applicant(String name, int n) throws Exception {
            ResultActions created = admin(post("/admin/members").contentType(MediaType.APPLICATION_JSON).content("""
                    { "name": "%s %s", "phone": "0115%s00000%d", "email": "person%d@%s.eg", "slotId": "%s", "assessorId": "%s" }"""
                    .formatted(name, tag, tag.equals("A") ? "1" : "2", n, n, code, assessmentSlot, trainer1)));
            expect(created, 201);
            return UUID.fromString(JsonPath.read(body(created), "$.id"));
        }

        UUID place(UUID booking) throws Exception {
            ResultActions placed = trainer1(post("/me/bookings/" + booking + "/result")
                    .contentType(MediaType.APPLICATION_JSON).content("{ \"attended\": true, \"levelId\": 1 }"));
            expect(placed, 200);
            return UUID.fromString(JsonPath.read(body(placed), "$.riderId"));
        }

        UUID pay(UUID rider) throws Exception {
            ResultActions paid = admin(post("/admin/riders/" + rider + "/payments").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"amountEgp\": 1200, \"method\": \"cash\", \"startMonth\": \"2026-10\", \"months\": 1 }"));
            expect(paid, 201);
            return UUID.fromString(JsonPath.read(body(paid), "$.payment.id"));
        }

        ResultActions assign(UUID session, UUID... riders) throws Exception {
            StringBuilder ids = new StringBuilder();
            for (UUID rider : riders) {
                ids.append(ids.isEmpty() ? "" : ", ").append('"').append(rider).append('"');
            }
            return trainer1(put("/me/sessions/" + session + "/assignments").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"riderIds\": [" + ids + "] }"));
        }

        void evaluate(UUID rider) throws Exception {
            expect(trainer1(post("/me/sessions/" + finishedSession + "/evaluations").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"riderId\": \"%s\", \"stars\": 4, \"comment\": \"Steady seat %s\" }".formatted(rider, tag))), 200);
        }

        // The admin invites the rider; the rider sets a password through the emailed link and signs in.
        String riderAccount(UUID rider) throws Exception {
            recorder.clear();
            expect(admin(post("/admin/riders/" + rider + "/invite")), 200, 201, 204);
            acceptLink();
            return login(code, "person1@%s.eg".formatted(code));
        }

        void acceptLink() throws Exception {
            String token = RecordingNotificationSender.tokenFrom(recorder.last());
            expect(mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"token\": \"%s\", \"password\": \"%s\" }".formatted(token, PASSWORD))), 200);
            recorder.clear();
        }

        // Access tokens last minutes, so everyone signs in again after the clock moves.
        void signIn() throws Exception {
            adminToken = login(code, adminEmail(club));
            trainer1Token = login(code, phone(1));
            trainer2Token = login(code, phone(2));
            if (riderToken != null) {
                riderToken = login(code, "person1@%s.eg".formatted(code));
            }
        }

        ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
            return perform(adminToken, request);
        }

        ResultActions trainer1(MockHttpServletRequestBuilder request) throws Exception {
            return perform(trainer1Token, request);
        }

        ClubData data() {
            return new ClubData(club, code, adminToken, trainer1, trainer1Token, trainer2, trainer2Token,
                    assessmentTemplate, assessmentSlot, noonTemplate, finishedSession, eveningSession,
                    rider1, rider2, payment1, riderToken, pendingMember, pendingBooking, closedMember, rider1Member);
        }
    }

    // ---- helpers

    public ResultActions loginRequest(String clubCode, String login, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"clubCode\": \"%s\", \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": true }"
                        .formatted(clubCode, login, password)));
    }

    private String login(String clubCode, String login) throws Exception {
        ResultActions signedIn = loginRequest(clubCode, login, PASSWORD);
        expect(signedIn, 200);
        return JsonPath.read(body(signedIn), "$.accessToken");
    }

    private static String adminEmail(UUID club) {
        return club.equals(A) ? "admin@school.eg" : "admin@cairo-eq.eg";
    }

    private UUID sessionOf(UUID template) {
        return root(() -> jdbc.queryForObject(
                "SELECT id FROM session WHERE template_id = ? AND session_date = ?", UUID.class, template, FRIDAY));
    }

    private UUID memberOf(UUID booking) {
        return root(() -> jdbc.queryForObject(
                "SELECT member_id FROM assessment_booking WHERE id = ?", UUID.class, booking));
    }

    private static java.time.Instant at(LocalTime time) {
        return cairo(time).toInstant();
    }

    private static void expect(ResultActions result, int... statuses) throws Exception {
        int actual = result.andReturn().getResponse().getStatus();
        for (int s : statuses) {
            if (s == actual) {
                return;
            }
        }
        throw new AssertionError("Expected " + java.util.Arrays.toString(statuses) + " but was " + actual + ": "
                + body(result) + " for " + result.andReturn().getRequest().getRequestURI());
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
