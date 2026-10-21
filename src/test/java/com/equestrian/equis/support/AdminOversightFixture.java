package com.equestrian.equis.support;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TrainerAccountsFixture.TRAINERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Plays both sides of the admin-oversight journeys (flows W-C and W-D) over HTTP: the admin on W1, W2 and W3, the
// trainers in the app (A1/A3/A5, My riders, a session's seats). Builds on TrainerAccountsFixture for the admin,
// the trainers' accounts and the clock. Only the checks the API can't answer (notices, old rows) read the database.
// The test class gives the context a MutableClock (@TestBean) and @Import(AdminOversightFixture.Config.class).
public class AdminOversightFixture {
    public static final LocalDate FRI_16 = LocalDate.of(2026, 10, 16);
    public static final LocalDate SAT_17 = LocalDate.of(2026, 10, 17);
    public static final LocalDate TUE_20 = LocalDate.of(2026, 10, 20);
    public static final LocalDate SAT_24 = LocalDate.of(2026, 10, 24);
    public static final LocalTime TEN = LocalTime.of(10, 0);
    public static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    public static final LocalTime SIX_PM = LocalTime.of(18, 0);
    public static final int LEVEL_TWO = 2;
    public static final String PASSWORD = "Coach@2026";

    private final TrainerAccountsFixture accounts;
    private final JdbcTemplate jdbc;
    private final SessionRepository sessions;
    private final MutableClock clock;
    private final Map<UUID, AuthTokens> tokens = new HashMap<>();
    private final Map<UUID, Instant> tokenExpiry = new HashMap<>();
    private int seq;

    AdminOversightFixture(TrainerAccountsFixture accounts, JdbcTemplate jdbc, SessionRepository sessions,
                          MutableClock clock) {
        this.accounts = accounts;
        this.jdbc = jdbc;
        this.sessions = sessions;
        this.clock = clock;
    }

    @TestConfiguration
    @Import(TrainerAccountsFixture.Config.class)
    public static class Config {
        @Bean
        AdminOversightFixture adminOversightFixture(TrainerAccountsFixture accounts, JdbcTemplate jdbc,
                                                    SessionRepository sessions, Clock clock) {
            return new AdminOversightFixture(accounts, jdbc, sessions, (MutableClock) clock);
        }
    }

    // A trainer as the tests know them: who they are and how they sign in.
    public record Coach(UUID id, String name, String phone) {
    }

    // The W1 templates every scenario starts with.
    public record Schedule(UUID saturdayAssessment, UUID tuesdayAssessment, UUID tuesdayLevelTwo) {
    }

    // For @BeforeEach: Friday 16 Oct 2026 10:00 Cairo, no trainers, members, riders, templates or notices.
    public void reset() {
        accounts.reset();
        tokens.clear();
        tokenExpiry.clear();
        seq = 0;
    }

    public TrainerAccountsFixture accounts() {
        return accounts;
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    // Moves the clock to that moment in school time.
    public void at(LocalDate date, LocalTime time) {
        clock.set(cairo(date, time));
    }

    public static Instant cairo(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, CAIRO).toInstant();
    }

    // Invited by the admin and signed up through the emailed link.
    public Coach coach(String name) throws Exception {
        seq++;
        String phone = "0101111%04d".formatted(seq);
        TrainerRow row = accounts.activeTrainer(name, phone, "coach%d@school.eg".formatted(seq), PASSWORD);
        return new Coach(row.id(), name, phone);
    }

    // Saturday 10:00–11:00 assessments for 2, Tuesday 17:00–18:00 assessments for 4 and Level 2 on Tuesday
    // 18:00–19:00 for 6, all from today, so the generator creates their dated sessions.
    public Schedule schedule() throws Exception {
        return new Schedule(
                template(null, "SATURDAY", TEN, 2),
                template(null, "TUESDAY", FIVE_PM, 4),
                template(LEVEL_TWO, "TUESDAY", SIX_PM, 6));
    }

    private UUID template(Integer levelId, String day, LocalTime start, int capacity) throws Exception {
        String body = """
                { "levelId": %s, "dayOfWeek": "%s", "start": "%s", "end": "%s", "capacity": %d, "validFrom": "%s" }"""
                .formatted(levelId, day, start, start.plusHours(1), capacity, LocalDate.ofInstant(clock.instant(), CAIRO));
        ResultActions saved = accounts.admin(post("/admin/session-templates")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        return UUID.fromString(read(saved, "$.template.id"));
    }

    // The session (assessment slot or level session) starting then. Read through JPA: times are stored with
    // hibernate.jdbc.time_zone, so plain SQL on start_time would not match.
    public UUID slotOn(LocalDate date, LocalTime start) {
        return sessions.findAll().stream()
                .filter(s -> s.getSessionDate().equals(date) && s.getStartTime().equals(start))
                .map(Session::getId)
                .reduce((a, b) -> {
                    throw new AssertionError("Two sessions on " + date + " at " + start);
                })
                .orElseThrow(() -> new AssertionError("No session on " + date + " at " + start));
    }

    // The trainer's access token, signing in again once the clock has passed its expiry.
    public String token(Coach coach) throws Exception {
        Instant expiry = tokenExpiry.get(coach.id());
        if (expiry == null || !clock.instant().isBefore(expiry)) {
            AuthTokens t = accounts.loginTrainer(coach.phone(), PASSWORD);
            tokens.put(coach.id(), t);
            tokenExpiry.put(coach.id(), clock.instant().plusSeconds(t.expiresIn()));
        }
        return tokens.get(coach.id()).accessToken();
    }

    public ResultActions asCoach(Coach coach, MockHttpServletRequestBuilder request) throws Exception {
        return accounts.perform(request.header("Authorization", "Bearer " + token(coach)));
    }

    // ---- The trainer app

    // W2's New person: a new member booked into the slot with this trainer as assessor. The booking id.
    public UUID bookMember(Coach coach, String name, String phone, UUID slotId) throws Exception {
        return UUID.fromString(read(newPerson(coach, name, phone, slotId).andExpect(status().isCreated()), "$.id"));
    }

    // The same, for a race: "ok" or the refusal code.
    public String bookMemberOutcome(Coach coach, String name, String phone, UUID slotId) throws Exception {
        return outcome(newPerson(coach, name, phone, slotId).andReturn());
    }

    public ResultActions newPerson(Coach coach, String name, String phone, UUID slotId) throws Exception {
        seq++;
        return accounts.admin(post("/admin/members").contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "%s", "phone": "%s", "email": "member%d@mail.eg", "slotId": "%s", "assessorId": "%s" }"""
                .formatted(name, phone, seq, slotId, coach.id())));
    }

    // A5: a null levelId with attended false is a no-show.
    public ResultActions recordResult(Coach coach, UUID bookingId, boolean attended, Integer levelId)
            throws Exception {
        return asCoach(coach, post("/me/bookings/" + bookingId + "/result").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"attended\": %s, \"levelId\": %s }".formatted(attended, levelId)));
    }

    // A3: one slot with its bookings.
    public ResultActions slotBookings(Coach coach, UUID slotId) throws Exception {
        return asCoach(coach, get("/me/assessment/slots/" + slotId)).andExpect(status().isOk());
    }

    // Screen 4.
    public ResultActions myRiders(Coach coach) throws Exception {
        return asCoach(coach, get("/me/riders")).andExpect(status().isOk());
    }

    // Screen 3: a session's seats.
    public ResultActions session(Coach coach, UUID sessionId) throws Exception {
        return asCoach(coach, get("/me/sessions/" + sessionId));
    }

    // Screen 3's save: riderIds becomes this trainer's whole list for the session.
    public ResultActions assignToSession(Coach coach, UUID sessionId, List<UUID> riderIds) throws Exception {
        return asCoach(coach, put("/me/sessions/" + sessionId + "/assignments")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"riderIds\": " + json(riderIds) + " }"));
    }

    // 4b: promote a rider from the level the sheet showed.
    public ResultActions promote(Coach coach, UUID riderId, int fromLevelId) throws Exception {
        return asCoach(coach, post("/me/riders/" + riderId + "/level-change").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"direction\": \"up\", \"fromLevelId\": %d, \"reason\": \"Ready for the next level\" }"
                        .formatted(fromLevelId)));
    }

    // Books one member per name into the slot with this assessor, then moves the clock past the slot's start and
    // places them all at the level. The rider ids, in the order of the names.
    public List<UUID> placedRiders(Coach assessor, UUID slotId, int levelId, String... names) throws Exception {
        List<UUID> bookings = new ArrayList<>();
        for (String name : names) {
            bookings.add(bookMember(assessor, name, nextPhone(), slotId));
        }
        Session slot = sessions.findById(slotId).orElseThrow();
        at(slot.getSessionDate(), slot.getStartTime().plusMinutes(30));
        List<UUID> riders = new ArrayList<>();
        for (UUID booking : bookings) {
            riders.add(UUID.fromString(read(recordResult(assessor, booking, true, levelId)
                    .andExpect(status().isOk()), "$.riderId")));
        }
        return riders;
    }

    // A member phone no other member in the scenario has.
    public String nextPhone() {
        seq++;
        return "0128888%04d".formatted(seq);
    }

    // ---- The admin console

    // W2, expected to answer.
    public ResultActions w2() throws Exception {
        return accounts.admin(get("/admin/bookings")).andExpect(status().isOk());
    }

    public ResultActions w2(String status, String q) throws Exception {
        MockHttpServletRequestBuilder request = get("/admin/bookings");
        if (status != null) {
            request.param("status", status);
        }
        if (q != null) {
            request.param("q", q);
        }
        return accounts.admin(request).andExpect(status().isOk());
    }

    public ResultActions patchBooking(UUID bookingId, String body) throws Exception {
        return accounts.admin(patch("/admin/bookings/" + bookingId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    public ResultActions remind(UUID bookingId) throws Exception {
        return accounts.admin(post("/admin/bookings/" + bookingId + "/remind"));
    }

    // W3, expected to answer.
    public ResultActions w3() throws Exception {
        return w3(Map.of());
    }

    public ResultActions w3(Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder request = get("/admin/riders");
        params.forEach(request::param);
        return accounts.admin(request).andExpect(status().isOk());
    }

    public ResultActions reassign(String body) throws Exception {
        return accounts.admin(patch("/admin/riders/trainer").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    public ResultActions assignRiders(List<UUID> riderIds, UUID trainerId) throws Exception {
        return reassign("{ \"riderIds\": %s, \"trainerId\": \"%s\" }".formatted(json(riderIds), trainerId));
    }

    public ResultActions resetToAssessor(List<UUID> riderIds) throws Exception {
        return reassign("{ \"riderIds\": %s, \"resetToAssessor\": true }".formatted(json(riderIds)));
    }

    // W4's Disable, confirmed: the trainer has riders or bookings.
    public ResultActions disable(Coach coach) throws Exception {
        return accounts.admin(post(TRAINERS + "/" + coach.id() + "/disable").param("confirm", "true"));
    }

    public ResultActions enable(Coach coach) throws Exception {
        return accounts.admin(post(TRAINERS + "/" + coach.id() + "/enable"));
    }

    // W1's Delete on a template.
    public ResultActions deleteTemplate(UUID templateId) throws Exception {
        return accounts.admin(delete("/admin/session-templates/" + templateId));
    }

    public static String rowOf(UUID id) {
        return "$.bookings[?(@.id == '" + id + "')]";
    }

    public static String riderRow(UUID id) {
        return "$.riders[?(@.id == '" + id + "')]";
    }

    // ---- What only the database shows

    public String bookingStatus(UUID bookingId) {
        return jdbc.queryForObject("select status from assessment_booking where id = ?", String.class, bookingId);
    }

    public UUID assessorOf(UUID bookingId) {
        return jdbc.queryForObject("select assessor_id from assessment_booking where id = ?", UUID.class, bookingId);
    }

    public UUID slotOf(UUID bookingId) {
        return jdbc.queryForObject("select session_id from assessment_booking where id = ?", UUID.class, bookingId);
    }

    public UUID trainerOf(UUID riderId) {
        return jdbc.queryForObject("select trainer_id from rider where id = ?", UUID.class, riderId);
    }

    // Bookings holding a seat in the slot.
    public long upcomingIn(UUID slotId) {
        return jdbc.queryForObject(
                "select count(*) from assessment_booking where session_id = ? and status = 'upcoming'",
                Long.class, slotId);
    }

    // result_overdue notices for this trainer about this booking.
    public int reminders(Coach coach, UUID bookingId) {
        return jdbc.queryForObject("""
                        select count(*) from notice
                        where trainer_id = ? and type = 'result_overdue' and ref_id = ?""",
                Integer.class, coach.id(), bookingId);
    }

    public int noticesFor(Coach coach) {
        return jdbc.queryForObject("select count(*) from notice where trainer_id = ?", Integer.class, coach.id());
    }

    // What opening the notice in the app will do; the trainer-side notice endpoints are not built yet.
    public void readNotices(Coach coach) {
        jdbc.update("update notice set read_at = created_at where trainer_id = ? and read_at is null", coach.id());
    }

    // ---- Helpers

    public static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path).toString();
    }

    public static <T> T readValue(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    public static String json(List<UUID> ids) {
        return ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    // Starts both calls at once and waits for both; each result is "ok" or the code the API answered with.
    // A deadlock would surface as a 500 or a timeout. Sign everyone in first: the calls share the cached tokens.
    public static List<String> race(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        return raceOutcomes(() -> outcome(first.call()), () -> outcome(second.call()));
    }

    // Like race, for calls that report their own outcome ("ok" or a code), e.g. bookMemberOutcome.
    public static List<String> raceOutcomes(Callable<String> first, Callable<String> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return first.call();
            });
            Future<String> b = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return second.call();
            });
            start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    public static String outcome(MvcResult result) throws Exception {
        int status = result.getResponse().getStatus();
        assertThat(status).as("never a 500").isLessThan(500);
        return status < 300 ? "ok" : JsonPath.read(result.getResponse().getContentAsString(), "$.code");
    }
}
