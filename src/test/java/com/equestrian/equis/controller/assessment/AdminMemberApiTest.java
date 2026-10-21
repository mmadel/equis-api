package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.support.MutableClock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /admin/members (W2's New person) over HTTP against a real database.
// Not @Transactional, so the race cases run in real, concurrent transactions. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=new-person-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
public class AdminMemberApiTest {
    private static final String PATH = "/admin/members";
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Friday 16 Oct 2026, 12:00 school time.
    private static final Instant FRIDAY_NOON = ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate FRI_16 = LocalDate.of(2026, 10, 16);
    private static final LocalDate SAT_24 = LocalDate.of(2026, 10, 24);
    private static final LocalDate TUE_20 = LocalDate.of(2026, 10, 20);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(FRIDAY_NOON);

    @TestBean
    Clock clock;

    static Clock clock() {
        return CLOCK;
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    LevelRepository levelRepository;

    private UUID ahmed;
    private int seq;

    @BeforeEach
    void setUp() {
        CLOCK.set(FRIDAY_NOON);
        // Also drops the seeded trainers and sample riders.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        seq = 0;
        ahmed = trainer("Ahmed Said", "active", "ahmed@school.eg");
    }

    // Case 1: Sat 24 Oct 10:00 with 2 of 4 booked becomes 3 of 4.
    @Test
    void addsThePersonAndBooksThemWithTheChosenAssessor() throws Exception {
        UUID saturday = slot(SAT_24, TEN, 4);
        UUID other = trainer("Mona Kamal", "active", "mona@school.eg");
        booking(member("Seat One"), saturday, other);
        booking(member("Seat Two"), saturday, other);

        ResultActions created = create("Sara Ali", "01011112222", " Sara@Mail.com ", saturday, ahmed)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.memberStatus").value("pending_assessment"))
                .andExpect(jsonPath("$.member.name").value("Sara Ali"))
                .andExpect(jsonPath("$.member.phone").value("01011112222"))
                .andExpect(jsonPath("$.member.email").value("sara@mail.com"))
                .andExpect(jsonPath("$.slot.id").value(saturday.toString()))
                .andExpect(jsonPath("$.slot.date").value("2026-10-24"))
                .andExpect(jsonPath("$.slot.start").value("10:00"))
                .andExpect(jsonPath("$.assessor.id").value(ahmed.toString()))
                .andExpect(jsonPath("$.assessor.name").value("Ahmed Said"));

        UUID bookingId = UUID.fromString(JsonPath.read(body(created.andReturn()), "$.id"));
        assertThat(jdbc.queryForObject("select assessor_id from assessment_booking where id = ?", UUID.class,
                bookingId)).isEqualTo(ahmed);
        assertThat(upcomingIn(saturday)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select email from member where phone = '01011112222'", String.class))
                .isEqualTo("sara@mail.com");

        // The new person is on W2 with the same row.
        mvc.perform(asAdmin(get("/admin/bookings")).param("q", "01011112222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings[0].id").value(bookingId.toString()))
                .andExpect(jsonPath("$.bookings[0].member.email").value("sara@mail.com"));
    }

    // Case 2
    @Test
    void invalidFieldsAreRefusedAndNothingIsSaved() throws Exception {
        UUID saturday = slot(SAT_24, TEN, 4);

        create("  ", "123", "x", saturday, ahmed)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name", "phone", "email")));
        send("""
                { "name": "Sara Ali", "phone": "01011112222", "email": "sara@mail.com" }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("slotId", "assessorId")));

        assertThat(count("member")).isZero();
        assertThat(count("assessment_booking")).isZero();
    }

    // Case 3
    @Test
    void thePhoneOfAClosedPersonNamesThem() throws Exception {
        UUID closed = member("Sara A.", "01011112222", "old.sara@mail.com");
        jdbc.update("update member set closed_at = ? where id = ?",
                Timestamp.from(FRIDAY_NOON.minusSeconds(86_400L * 30)), closed);

        create("Sara Ali", "01011112222", "sara@mail.com", slot(SAT_24, TEN, 4), ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("phone_taken"))
                .andExpect(jsonPath("$.detail").value("This phone belongs to Sara A."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("phone_taken"))
                .andExpect(jsonPath("$.existingMemberId").value(closed.toString()))
                .andExpect(jsonPath("$.existingMemberName").value("Sara A."))
                .andExpect(jsonPath("$.existingClosed").value(true));
        assertThat(count("member")).isEqualTo(1);
        assertThat(count("assessment_booking")).isZero();
    }

    // Case 4
    @Test
    void anEmailInUseIsTakenAndOnlyAPersonIsNamed() throws Exception {
        UUID saturday = slot(SAT_24, TEN, 4);
        UUID open = member("Laila Hassan", "01022223333", "laila@mail.com");

        create("Sara Ali", "01011112222", "LAILA@Mail.com", saturday, ahmed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.existingMemberId").value(open.toString()))
                .andExpect(jsonPath("$.existingMemberName").value("Laila Hassan"))
                .andExpect(jsonPath("$.existingClosed").value(false));

        for (String email : List.of("Ahmed@School.eg", "new-person-admin@example.com")) {
            create("Sara Ali", "01011112222", email, saturday, ahmed)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("email_taken"))
                    .andExpect(jsonPath("$.detail").value("This email is already registered."))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                    .andExpect(jsonPath("$.existingMemberId").doesNotExist())
                    .andExpect(jsonPath("$.existingMemberName").doesNotExist())
                    .andExpect(jsonPath("$.existingClosed").doesNotExist());
        }
        assertThat(count("member")).isEqualTo(1);
    }

    // Case 5
    @Test
    void theSlotMustBeAnOpenAssessmentSlotWithASeat() throws Exception {
        UUID full = slot(SAT_24, TEN, 1);
        booking(member("Seat Taker"), full, ahmed);

        create("Sara Ali", "01011112222", "sara@mail.com", full, ahmed)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("slot_full"));
        create("Sara Ali", "01011112222", "sara@mail.com", session(TUE_20, TEN, 6, true), ahmed)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("slot_not_found"));
        create("Sara Ali", "01011112222", "sara@mail.com", UUID.randomUUID(), ahmed)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("slot_not_found"));
        create("Sara Ali", "01011112222", "sara@mail.com", slot(FRI_16, TEN, 4), ahmed)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("slot_closed"));

        assertThat(count("member")).isEqualTo(1);
    }

    // Case 6
    @Test
    void theAssessorMustBeAnActiveTrainer() throws Exception {
        UUID saturday = slot(SAT_24, TEN, 4);

        for (String status : List.of("invited", "disabled")) {
            create("Sara Ali", "01011112222", "sara@mail.com", saturday, trainer("Not Active", status, null))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("trainer_not_active"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("assessorId"));
        }
        create("Sara Ali", "01011112222", "sara@mail.com", saturday, UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("trainer_not_found"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assessorId"));

        assertThat(count("member")).isZero();
    }

    // Case 7
    @Test
    void twoRequestsForTheLastSeatOneWins() throws Exception {
        UUID lastSeat = slot(SAT_24, TEN, 2);
        booking(member("Seat Taker"), lastSeat, ahmed);

        List<String> outcomes = race(
                () -> create("Sara Ali", "01011112222", "sara@mail.com", lastSeat, ahmed).andReturn(),
                () -> create("Omar Hassan", "01033334444", "omar@mail.com", lastSeat, ahmed).andReturn());

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "slot_full");
        assertThat(upcomingIn(lastSeat)).isEqualTo(2);
        assertThat(count("member")).isEqualTo(2);
    }

    // Case 8: different slots, so neither waits on the other's slot lock; the unique key decides.
    @Test
    void twoRequestsWithTheSameEmailOneWins() throws Exception {
        UUID saturday = slot(SAT_24, TEN, 4);
        UUID tuesday = slot(TUE_20, TEN, 4);

        List<MvcResult> results = raceResults(
                () -> create("Sara Ali", "01011112222", "sara@mail.com", saturday, ahmed).andReturn(),
                () -> create("Sara Ali", "01055556666", "Sara@Mail.com", tuesday, ahmed).andReturn());

        assertThat(results.stream().map(AdminMemberApiTest::outcome).toList())
                .containsExactlyInAnyOrder("ok", "email_taken");
        MvcResult winner = results.stream().filter(r -> r.getResponse().getStatus() == 201).findFirst().orElseThrow();
        MvcResult loser = results.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
        String winnerId = JsonPath.read(body(winner), "$.member.id");
        assertThat(JsonPath.<String>read(body(loser), "$.existingMemberId")).isEqualTo(winnerId);
        assertThat(count("member")).isEqualTo(1);
        assertThat(count("assessment_booking")).isEqualTo(1);
    }

    // Case 9
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        String body = json("Sara Ali", "01011112222", "sara@mail.com", slot(SAT_24, TEN, 4), ahmed);

        mvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(asTrainer(post(PATH), ahmed)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        assertThat(count("member")).isZero();
    }

    // ---- helpers

    private ResultActions create(String name, String phone, String email, UUID slotId, UUID assessorId)
            throws Exception {
        return send(json(name, phone, email, slotId, assessorId));
    }

    private ResultActions send(String body) throws Exception {
        return mvc.perform(asAdmin(post(PATH))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String json(String name, String phone, String email, UUID slotId, UUID assessorId) {
        return """
                { "name": "%s", "phone": "%s", "email": "%s", "slotId": "%s", "assessorId": "%s" }"""
                .formatted(name, phone, email, slotId, assessorId);
    }

    private static List<String> race(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        return raceResults(first, second).stream().map(AdminMemberApiTest::outcome).toList();
    }

    // Starts both calls at once and waits for both.
    private static List<MvcResult> raceResults(Callable<MvcResult> first, Callable<MvcResult> second)
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> a = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return first.call();
            });
            Future<MvcResult> b = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return second.call();
            });
            start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    // "ok" or the code the API answered with; never a 500.
    private static String outcome(MvcResult result) {
        int status = result.getResponse().getStatus();
        assertThat(status).as("never a 500").isLessThan(500);
        return status < 300 ? "ok" : JsonPath.read(body(result), "$.code");
    }

    private static String body(MvcResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)));
    }

    private MockHttpServletRequestBuilder asTrainer(MockHttpServletRequestBuilder request, UUID trainer) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(trainer, Role.TRAINER)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    private long upcomingIn(UUID slot) {
        return jdbc.queryForObject(
                "select count(*) from assessment_booking where session_id = ? and status = 'upcoming'",
                Long.class, slot);
    }

    // A null email gets a made-up one.
    private UUID trainer(String name, String status, String email) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, ?, ?) returning id""",
                UUID.class, name, "0199999%04d".formatted(seq),
                email != null ? email : "trainer%d@school.eg".formatted(seq), status,
                status.equals("invited") ? null : "not-a-real-hash");
    }

    private UUID member(String name) {
        seq++;
        return member(name, "0128888%04d".formatted(seq), "member%d@mail.eg".formatted(seq));
    }

    private UUID member(String name, String phone, String email) {
        return jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, name, phone, email);
    }

    // An assessment slot (no level) on that date, one hour long.
    private UUID slot(LocalDate date, LocalTime start, int capacity) {
        return session(date, start, capacity, false);
    }

    // Saved through JPA, as the app saves them, so the times are stored the way the app reads them.
    private UUID session(LocalDate date, LocalTime start, int capacity, boolean level) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(capacity);
            template.setValidFrom(date);
            if (level) {
                template.setLevel(levelRepository.getReferenceById((short) 2));
            }
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }

    private void booking(UUID member, UUID slot, UUID assessor) {
        jdbc.update("insert into assessment_booking (member_id, session_id, assessor_id, status) values (?, ?, ?, ?)",
                member, slot, assessor, "upcoming");
    }
}
