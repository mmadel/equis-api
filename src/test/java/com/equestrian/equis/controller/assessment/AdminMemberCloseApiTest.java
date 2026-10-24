package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /admin/members/{id}/close and /reopen (W2's Close and Reopen) over HTTP against a real database.
// Not @Transactional, so the race tests see each other's commits. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=close-person-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
public class AdminMemberCloseApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Friday 16 Oct 2026, 12:00 school time.
    private static final Instant FRIDAY_NOON = ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate TUE_13 = LocalDate.of(2026, 10, 13);
    private static final LocalDate FRI_16 = LocalDate.of(2026, 10, 16);
    private static final LocalDate SAT_24 = LocalDate.of(2026, 10, 24);
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

    private UUID ahmed;
    private UUID tuesday;
    private UUID saturday;
    private int seq;

    @BeforeEach
    void setUp() {
        CLOCK.set(FRIDAY_NOON);
        // Also drops the seeded trainers and sample riders.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        ahmed = jdbc.queryForObject("""
                insert into trainer (name, phone, email, status, password_hash)
                values ('Ahmed Said', '01999990001', 'ahmed@school.eg', 'active', 'not-a-real-hash') returning id""",
                UUID.class);
        tuesday = slot(TUE_13);
        saturday = slot(SAT_24);
    }

    // Case 1: Omar missed his assessment and doesn't answer the phone.
    @Test
    void closesANoShowPerson() throws Exception {
        UUID omar = booked("Omar Hassan", "01033334444", tuesday, "noshow");

        close(omar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(omar.toString()))
                .andExpect(jsonPath("$.status").value("noshow"))
                .andExpect(jsonPath("$.memberStatus").value("closed"))
                .andExpect(jsonPath("$.actions", contains("reopen", "edit")));

        assertThat(closedAt(omar)).isEqualTo(FRIDAY_NOON);
        // Nothing is deleted.
        assertThat(jdbc.queryForObject("select count(*) from assessment_booking where member_id = ?", Integer.class,
                omar)).isEqualTo(1);
        list(null)
                .andExpect(jsonPath("$.bookings.length()").value(0))
                .andExpect(jsonPath("$.totals.closed").value(1));
        list("closed")
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.bookings[0].member.id").value(omar.toString()));
        for (String tab : List.of("upcoming", "noshow", "cancelled")) {
            list(tab).andExpect(jsonPath("$.bookings.length()").value(0));
        }
    }

    // Case 2
    @Test
    void closesAPersonWhoseLatestBookingIsCancelled() throws Exception {
        UUID laila = booked("Laila Fathy", "01055556666", saturday, "cancelled");

        close(laila)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"))
                .andExpect(jsonPath("$.memberStatus").value("closed"));
        assertThat(closedAt(laila)).isNotNull();
    }

    // Case 3: Sara is booked for Saturday; a booking whose slot has started (waiting) counts too.
    @Test
    void aPersonWithABookedAssessmentCannotBeClosed() throws Exception {
        UUID sara = booked("Sara Ali", "01011112222", saturday, "upcoming");
        UUID waleed = booked("Waleed Waiting", "01022223333", slot(FRI_16), "upcoming");

        for (UUID person : List.of(sara, waleed)) {
            close(person)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("member_has_active_booking"))
                    .andExpect(jsonPath("$.detail").value("Cancel the assessment first."));
            assertThat(closedAt(person)).isNull();
        }
    }

    // A no-show who was already rebooked is judged by the new booking.
    @Test
    void aRebookedNoShowCannotBeClosed() throws Exception {
        UUID omar = booked("Omar Hassan", "01033334444", tuesday, "noshow");
        booking(omar, saturday, "upcoming");

        close(omar)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_has_active_booking"));
    }

    // Case 4
    @Test
    void ridersTwiceClosedAndOpenPeopleAreRefused() throws Exception {
        UUID pakinam = booked("Pakinam Placed", "01077778888", tuesday, "placed");
        jdbc.update("insert into rider (name, phone, level_id, trainer_id, member_id) values (?, ?, 2, ?, ?)",
                "Pakinam Placed", "01077778888", ahmed, pakinam);
        close(pakinam)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_is_rider"));
        assertThat(closedAt(pakinam)).isNull();

        UUID omar = booked("Omar Hassan", "01033334444", tuesday, "noshow");
        close(omar).andExpect(status().isOk());
        Instant firstClose = closedAt(omar);
        CLOCK.set(FRIDAY_NOON.plusSeconds(3600));
        close(omar)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_already_closed"));
        assertThat(closedAt(omar)).isEqualTo(firstClose);

        UUID nader = booked("Nader Noshow", "01088889999", tuesday, "noshow");
        reopen(nader)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_not_closed"));
    }

    // Case 5: Omar comes back in December, as a no-show who can be rebooked.
    @Test
    void reopenBringsBackTheLastAssessmentsStatus() throws Exception {
        UUID omar = booked("Omar Hassan", "01033334444", tuesday, "noshow");
        UUID laila = booked("Laila Fathy", "01055556666", saturday, "cancelled");
        close(omar).andExpect(status().isOk());
        close(laila).andExpect(status().isOk());

        reopen(omar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("noshow"))
                .andExpect(jsonPath("$.memberStatus").value("no_show"))
                .andExpect(jsonPath("$.actions", hasItem("rebook")))
                .andExpect(jsonPath("$.actions", hasItem("close")));
        reopen(laila)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberStatus").value("pending_assessment"))
                .andExpect(jsonPath("$.actions", hasItem("rebook")));

        assertThat(closedAt(omar)).isNull();
        list("closed").andExpect(jsonPath("$.bookings.length()").value(0));
        list("noshow").andExpect(jsonPath("$.bookings[0].member.id").value(omar.toString()));
    }

    // Case 5, then the rebook W2 offers goes through.
    @Test
    void aReopenedPersonCanBeRebooked() throws Exception {
        UUID omar = booked("Omar Hassan", "01033334444", tuesday, "noshow");
        UUID noShow = latestBooking(omar);
        close(omar).andExpect(status().isOk());
        reopen(omar).andExpect(status().isOk());

        rebook(noShow, saturday)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.memberStatus").value("pending_assessment"));
    }

    // Case 6: W2 doesn't offer rebook on a closed row, and the API refuses it too.
    @Test
    void aClosedPersonCannotBeRebooked() throws Exception {
        UUID omar = booked("Omar Hassan", "01033334444", tuesday, "noshow");
        UUID noShow = latestBooking(omar);
        close(omar).andExpect(status().isOk());

        rebook(noShow, saturday)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_closed"));
        assertThat(jdbc.queryForObject("select count(*) from assessment_booking where member_id = ?", Integer.class,
                omar)).isEqualTo(1);
    }

    // Case 7: close and rebook at the same moment. One wins; never a closed person with an upcoming booking.
    @Test
    void closeAndRebookTogetherNeverLeaveAClosedPersonBooked() throws Exception {
        for (int round = 0; round < 8; round++) {
            UUID person = booked("Racer " + round, "0104444%04d".formatted(round), tuesday, "noshow");
            UUID noShow = latestBooking(person);

            List<String> outcomes = race(() -> close(person).andReturn(),
                    () -> rebook(noShow, saturday).andReturn());

            assertThat(outcomes).as("round %d", round).isIn(
                    List.of("ok", "member_closed"),
                    List.of("member_has_active_booking", "ok"));
            assertThat(closedAt(person) != null && upcomingOf(person) > 0).as("round %d", round).isFalse();
        }
    }

    // Case 7, made deterministic: the close holds the lock while the rebook (which has already loaded the person
    // as open) waits for it. Once the close commits, the rebook must see it.
    @Test
    void aRebookWaitingOnACloseSeesTheClose() throws Exception {
        UUID omar = booked("Omar Hassan", "01033334444", tuesday, "noshow");
        UUID noShow = latestBooking(omar);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> closer = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
                jdbc.queryForObject("select id from member where id = ? for update", UUID.class, omar);
                jdbc.update("update member set closed_at = ? where id = ?", Timestamp.from(FRIDAY_NOON), omar);
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<String> rebooker = pool.submit(() -> outcome(rebook(noShow, saturday).andReturn()));
            waitForALockWaiter();
            release.countDown();
            closer.get(10, TimeUnit.SECONDS);

            assertThat(rebooker.get(20, TimeUnit.SECONDS)).isEqualTo("member_closed");
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
        assertThat(upcomingOf(omar)).isZero();
    }

    // Case 8
    @Test
    void anUnknownPersonIsNotFound() throws Exception {
        for (ResultActions call : List.of(close(UUID.randomUUID()), reopen(UUID.randomUUID()))) {
            call.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("member_not_found"));
        }
    }

    // Case 8
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        UUID omar = booked("Omar Hassan", "01033334444", tuesday, "noshow");

        for (String action : List.of("close", "reopen")) {
            mvc.perform(post("/admin/members/{id}/" + action, omar))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/admin/members/{id}/" + action, omar)
                            .header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(ahmed, Role.TRAINER))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
        assertThat(closedAt(omar)).isNull();
    }

    // ---- helpers

    private ResultActions close(UUID id) throws Exception {
        return mvc.perform(asAdmin(post("/admin/members/{id}/close", id)));
    }

    private ResultActions reopen(UUID id) throws Exception {
        return mvc.perform(asAdmin(post("/admin/members/{id}/reopen", id)));
    }

    private ResultActions rebook(UUID bookingId, UUID slot) throws Exception {
        return mvc.perform(asAdmin(patch("/admin/bookings/{id}", bookingId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"slotId\": \"%s\" }".formatted(slot)));
    }

    // tab null = the default list of open people.
    private ResultActions list(String tab) throws Exception {
        MockHttpServletRequestBuilder request = asAdmin(get("/admin/bookings"));
        return mvc.perform(tab == null ? request : request.param("status", tab)).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    // Starts both calls at once; each result is "ok" or the code the API answered with.
    private static List<String> race(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return outcome(first.call());
            });
            Future<String> b = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return outcome(second.call());
            });
            start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private static String outcome(MvcResult result) throws Exception {
        int status = result.getResponse().getStatus();
        assertThat(status).as("never a 500").isLessThan(500);
        return status < 300 ? "ok" : JsonPath.read(result.getResponse().getContentAsString(), "$.code");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Until some session of this database waits for a row lock (the rebook, on the member row).
    private void waitForALockWaiter() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("""
                    select count(*) from pg_stat_activity
                    where datname = current_database() and wait_event_type = 'Lock'""", Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("The rebook never waited for the member lock");
    }

    private Instant closedAt(UUID member) {
        Timestamp at = jdbc.queryForObject("select closed_at from member where id = ?", Timestamp.class, member);
        return at == null ? null : at.toInstant();
    }

    private int upcomingOf(UUID member) {
        return jdbc.queryForObject("select count(*) from assessment_booking where member_id = ? and status = 'upcoming'",
                Integer.class, member);
    }

    private UUID latestBooking(UUID member) {
        return jdbc.queryForObject(
                "select id from assessment_booking where member_id = ? order by created_at desc, id desc limit 1",
                UUID.class, member);
    }

    // A person with one booking in that slot.
    private UUID booked(String name, String phone, UUID slot, String status) {
        UUID member = jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, name, phone, phone + "@mail.com");
        booking(member, slot, status);
        return member;
    }

    // upcoming, placed (Level 2), noshow or cancelled; a result is recorded three days ago. Each booking is a
    // second newer than the last, so "latest per member" is unambiguous.
    private UUID booking(UUID member, UUID slot, String status) {
        Boolean attended = switch (status) {
            case "placed" -> true;
            case "noshow" -> false;
            default -> null;
        };
        Instant created = FRIDAY_NOON.minusSeconds(86_400L * 7).plusSeconds(seq++);
        return jdbc.queryForObject("""
                        insert into assessment_booking
                            (member_id, session_id, assessor_id, status, attended, level_id, recorded_at,
                             cancelled_at, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id""",
                UUID.class, member, slot, ahmed, status, attended,
                Boolean.TRUE.equals(attended) ? (Object) 2 : null,
                attended != null ? Timestamp.from(FRIDAY_NOON.minusSeconds(86_400L * 3)) : null,
                status.equals("cancelled") ? Timestamp.from(FRIDAY_NOON) : null,
                Timestamp.from(created));
    }

    // An assessment slot (no level) on that date, 10:00 to 11:00. Saved through JPA, as the app saves them.
    private UUID slot(LocalDate date) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(TEN);
            template.setEndTime(TEN.plusHours(1));
            template.setCapacity(4);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }
}
