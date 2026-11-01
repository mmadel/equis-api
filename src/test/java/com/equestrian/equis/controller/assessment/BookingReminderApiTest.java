package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.assessment.AssessmentRules;
import com.equestrian.equis.support.MutableClock;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /admin/bookings/{id}/remind (W2: Remind trainer) over HTTP against a real database.
// Not @Transactional, so the race case runs in real, concurrent transactions. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=booking-remind-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
public class BookingReminderApiTest {
    private static final String PATH = "/admin/bookings/";
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Sunday 18 Oct 2026, 12:00 school time: Saturday's 10:00 slot is waiting for its result.
    private static final Instant SUNDAY_NOON = cairo(2026, 10, 18, 12, 0);
    private static final LocalDate SAT_17 = LocalDate.of(2026, 10, 17);
    private static final LocalDate TUE_20 = LocalDate.of(2026, 10, 20);
    private static final LocalTime TEN = LocalTime.of(10, 0);
    private static final String SARA_MESSAGE =
            "Result overdue: Sara Ali (assessment Sat 17 Oct, 10:00). Record it in Assessment results.";

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    static final MutableClock CLOCK = new MutableClock(SUNDAY_NOON);

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
    EntityManagerFactory entityManagerFactory;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssessmentRules rules;

    private UUID ahmed;
    private UUID saturday;
    private int seq;

    @BeforeEach
    void setUp() {
        CLOCK.set(SUNDAY_NOON);
        // Also drops the seeded trainers and sample riders, and the notices through their trainer.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        seq = 0;
        ahmed = trainer("Ahmed Said", "active");
        saturday = slot(SAT_17, TEN);
    }

    // Case 4
    @Test
    void remindingAWaitingBookingNoticesTheAssessor() throws Exception {
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");

        remind(sara)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sara.toString()))
                .andExpect(jsonPath("$.status").value("waiting"))
                .andExpect(jsonPath("$.remindedAt").value(SUNDAY_NOON.toString()));

        List<NoticeRow> notices = noticesFor(sara);
        assertThat(notices).hasSize(1);
        NoticeRow notice = notices.getFirst();
        assertThat(notice.trainerId()).isEqualTo(ahmed);
        assertThat(notice.type()).isEqualTo("result_overdue");
        assertThat(notice.message()).isEqualTo(SARA_MESSAGE);
        assertThat(notice.createdAt()).isEqualTo(SUNDAY_NOON);
        assertThat(notice.readAt()).isNull();
    }

    // Case 5
    @Test
    void remindingAgainWhileUnreadCreatesNothing() throws Exception {
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");
        remind(sara).andExpect(status().isOk());

        CLOCK.advance(Duration.ofMinutes(5));
        remind(sara)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remindedAt").value(SUNDAY_NOON.toString()));

        assertThat(noticesFor(sara)).hasSize(1);
    }

    // Case 6
    @Test
    void onceReadANewReminderCreatesANewNotice() throws Exception {
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");
        remind(sara).andExpect(status().isOk());
        jdbc.update("update notice set read_at = ? where ref_id = ?", Timestamp.from(SUNDAY_NOON.plusSeconds(60)), sara);

        CLOCK.advance(Duration.ofDays(1));
        Instant tuesday = CLOCK.instant();
        remind(sara)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remindedAt").value(tuesday.toString()));

        assertThat(noticesFor(sara)).extracting(NoticeRow::createdAt).containsExactlyInAnyOrder(SUNDAY_NOON, tuesday);
    }

    // Case 7
    @Test
    void onlyAWaitingBookingCanBeReminded() throws Exception {
        UUID notStarted = booking(member("Not Started"), slot(TUE_20, TEN), ahmed, "upcoming");
        UUID placed = booking(member("Placed One"), saturday, ahmed, "placed");
        UUID noShow = booking(member("No Show"), saturday, ahmed, "noshow");
        UUID cancelled = booking(member("Cancelled One"), saturday, ahmed, "cancelled");

        for (UUID booking : List.of(notStarted, placed, noShow, cancelled)) {
            remind(booking)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("result_not_overdue"));
        }
        assertThat(noticeCount()).isZero();
    }

    // Case 8
    @Test
    void aDisabledAssessorCannotBeReminded() throws Exception {
        UUID disabled = trainer("Old Coach", "disabled");
        UUID sara = booking(member("Sara Ali"), saturday, disabled, "upcoming");

        remind(sara)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("trainer_not_active"))
                .andExpect(jsonPath("$.detail").value("Old Coach is not an active trainer. Change the assessor first."));
        assertThat(noticeCount()).isZero();
    }

    // Case 9
    @Test
    void anUnknownBookingIsNotFound() throws Exception {
        remind(UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("booking_not_found"));
    }

    // Case 10
    @Test
    void twoRemindersAtOnceMakeOneNotice() throws Exception {
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");

        List<String> outcomes = race(() -> remind(sara).andReturn(), () -> remind(sara).andReturn());

        assertThat(outcomes).containsExactly("ok", "ok");
        assertThat(noticesFor(sara)).hasSize(1);
    }

    // Case 11
    @Test
    void theListShowsTheReminderInTheSameNumberOfQueries() throws Exception {
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");
        remind(sara).andExpect(status().isOk());
        long withOne = statementsFor(this::list);

        // No placed bookings, so the riders query stays out of both counts.
        String[] statuses = {"upcoming", "noshow", "cancelled"};
        for (int i = 1; i < 20; i++) {
            UUID assessor = trainer("Trainer %02d".formatted(i), i % 3 == 0 ? "disabled" : "active");
            UUID slot = i % 2 == 0 ? saturday : slot(TUE_20.plusDays(i), TEN);
            booking(member("Member %02d".formatted(i)), slot, assessor, statuses[i % statuses.length]);
        }
        MvcResult result = list().andExpect(jsonPath("$.bookings.length()").value(20)).andReturn();
        long withTwenty = statementsFor(this::list);

        assertThat(withTwenty).isEqualTo(withOne);
        List<String> reminded = JsonPath.read(body(result), "$.bookings[?(@.remindedAt != null)].id");
        assertThat(reminded).containsExactly(sara.toString());
        assertThat(JsonPath.<List<String>>read(body(result), "$.bookings[?(@.id == '%s')].remindedAt".formatted(sara)))
                .containsExactly(SUNDAY_NOON.toString());
    }

    // Case 12
    @Test
    void theNoticeNamesTheSlotAsA3Does() throws Exception {
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");
        remind(sara).andExpect(status().isOk());

        ApiException booked = new TransactionTemplate(transactionManager)
                .execute(s -> rules.memberAlreadyBooked(sessionRepository.findById(saturday).orElseThrow()));

        assertThat(booked.getMessage()).isEqualTo("Booked Sat 17 Oct, 10:00");
        assertThat(noticesFor(sara).getFirst().message()).contains("(assessment Sat 17 Oct, 10:00)");
    }

    // Case 13
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        UUID sara = booking(member("Sara Ali"), saturday, ahmed, "upcoming");

        mvc.perform(post(PATH + sara + "/remind"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(asTrainer(post(PATH + sara + "/remind"), ahmed))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        assertThat(noticeCount()).isZero();
    }

    private ResultActions remind(UUID bookingId) throws Exception {
        return mvc.perform(asAdmin(post(PATH + bookingId + "/remind")));
    }

    private ResultActions list() throws Exception {
        return mvc.perform(asAdmin(get("/admin/bookings")));
    }

    private long statementsFor(Request request) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        request.run().andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
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
        return status < 300 ? "ok" : JsonPath.read(body(result), "$.code");
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
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

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
    }

    private record NoticeRow(UUID trainerId, String type, String message, Instant createdAt, Instant readAt) {
    }

    private List<NoticeRow> noticesFor(UUID booking) {
        return jdbc.query("select trainer_id, type, message, created_at, read_at from notice where ref_id = ?",
                (rs, i) -> new NoticeRow(rs.getObject("trainer_id", UUID.class), rs.getString("type"),
                        rs.getString("message"), rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant()),
                booking);
    }

    private long noticeCount() {
        return jdbc.queryForObject("select count(*) from notice", Long.class);
    }

    private UUID trainer(String name, String status) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, ?, ?) returning id""",
                UUID.class, name, "0199999%04d".formatted(seq), "trainer%d@school.eg".formatted(seq), status,
                "not-a-real-hash");
    }

    private UUID member(String name) {
        seq++;
        return jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, name, "0128888%04d".formatted(seq), "member%d@mail.eg".formatted(seq));
    }

    // An assessment slot on that date, one hour long. Saved through JPA, as the app saves them,
    // so the start and end times are stored the way the app reads them.
    private UUID slot(LocalDate date, LocalTime start) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(6);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }

    // The result columns follow the status as the table's check requires; placed members go to level 2.
    // Each booking is a second newer than the last, so "latest per member" is unambiguous.
    private UUID booking(UUID member, UUID slot, UUID assessor, String status) {
        Boolean attended = switch (status) {
            case "placed" -> true;
            case "noshow" -> false;
            default -> null;
        };
        boolean recorded = attended != null;
        Timestamp cancelledAt = status.equals("cancelled") ? Timestamp.from(SUNDAY_NOON) : null;
        Timestamp created = Timestamp.from(SUNDAY_NOON.minusSeconds(86_400L * 7).plusSeconds(seq++));
        return jdbc.queryForObject("""
                        insert into assessment_booking
                            (member_id, session_id, assessor_id, status, attended, level_id, recorded_at,
                             cancelled_at, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id""",
                UUID.class, member, slot, assessor, status, attended, status.equals("placed") ? 2 : null,
                recorded ? Timestamp.from(SUNDAY_NOON.minusSeconds(3600)) : null, cancelledAt, created);
    }

    private interface Request {
        ResultActions run() throws Exception;
    }
}
