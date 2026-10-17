package com.equestrian.equis.controller.overview;

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
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /admin/overview over HTTP against a real database. Trainers, riders, bookings and seats go in with SQL;
// sessions through JPA, as the app saves them (their times are stored with hibernate.jdbc.time_zone).
// The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=overview-api-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Testcontainers
public class AdminOverviewApiTest {
    private static final String PATH = "/admin/overview";
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // The school week of Saturday 17 October 2026 runs to Friday 23.
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 17);
    private static final LocalDate MONDAY = SATURDAY.plusDays(2);
    private static final LocalDate FRIDAY = SATURDAY.plusDays(6);
    private static final Instant SATURDAY_1517 = cairo(SATURDAY, 15, 17);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(SATURDAY_1517);

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
    EntityManagerFactory entityManagerFactory;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    LevelRepository levelRepository;

    private UUID sara;
    private UUID ahmed;
    private int seq;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, sample riders and generated sessions.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(SATURDAY_1517);
        seq = 0;
        sara = trainer("Sara Mahmoud", "active");
        ahmed = trainer("Ahmed Said", "active");
    }

    // Case 1
    @Test
    void todaysSessionsAreSortedByStartWithTheirStateInSchoolTime() throws Exception {
        UUID late = session(4, SATURDAY, LocalTime.of(16, 0), 4);
        UUID morning = session(1, SATURDAY, LocalTime.of(9, 0), 6);
        UUID slot = session(null, SATURDAY, LocalTime.of(12, 0), 4);
        session(1, SATURDAY.plusDays(1), LocalTime.of(9, 0), 6);

        overview()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-17"))
                .andExpect(jsonPath("$.sessionsToday[*].sessionId",
                        contains(morning.toString(), slot.toString(), late.toString())))
                .andExpect(jsonPath("$.sessionsToday[*].levelId", contains(1, null, 4)))
                .andExpect(jsonPath("$.sessionsToday[*].state", contains("finished", "finished", "upcoming")))
                .andExpect(jsonPath("$.sessionsToday[2].start").value("16:00"))
                .andExpect(jsonPath("$.sessionsToday[2].end").value("17:00"));
    }

    // Case 2
    @Test
    void aSessionThatHasStartedAndNotEndedIsInProgress() throws Exception {
        session(4, SATURDAY, LocalTime.of(16, 0), 4);
        CLOCK.set(cairo(SATURDAY, 16, 30));

        overview().andExpect(jsonPath("$.sessionsToday[0].state").value("in_progress"));
        CLOCK.set(cairo(SATURDAY, 17, 0));
        overview().andExpect(jsonPath("$.sessionsToday[0].state").value("finished"));
    }

    // Case 3
    @Test
    void aLevelSessionCountsItsAssignmentsAndNamesTheRidersTrainers() throws Exception {
        UUID session = session(4, SATURDAY, LocalTime.of(16, 0), 4);
        assign(session, rider("Hala Samy", 4, sara), sara);
        assign(session, rider("Omar Adel", 4, ahmed), ahmed);

        overview()
                .andExpect(jsonPath("$.sessionsToday[0].capacity").value(4))
                .andExpect(jsonPath("$.sessionsToday[0].booked").value(2))
                .andExpect(jsonPath("$.sessionsToday[0].trainers", contains("Ahmed Said", "Sara Mahmoud")));
    }

    // Case 4
    @Test
    void anAssessmentSlotCountsBookingsThatAreNotCancelledAndNamesTheirAssessors() throws Exception {
        UUID slot = session(null, SATURDAY, LocalTime.of(16, 0), 4);
        UUID mostafa = trainer("Mostafa Kamel", "active");
        booking(member("Reem Adel"), slot, sara, "upcoming", null, null, at(-3));
        booking(member("Nour Helmy"), slot, ahmed, "placed", 2, at(-1), at(-2));
        booking(member("Laila Fouad"), slot, mostafa, "cancelled", null, null, at(-2));

        overview()
                .andExpect(jsonPath("$.sessionsToday[0].levelId", nullValue()))
                .andExpect(jsonPath("$.sessionsToday[0].booked").value(2))
                .andExpect(jsonPath("$.sessionsToday[0].trainers", contains("Ahmed Said", "Sara Mahmoud")));
    }

    // Case 5
    @Test
    void theWeekRunsSaturdayToFridayAndLeavesCancelledSessionsOut() throws Exception {
        UUID level2 = session(2, MONDAY, LocalTime.of(10, 0), 6);
        assign(level2, rider("Hala Samy", 2, sara), sara);
        assign(level2, rider("Omar Adel", 2, ahmed), ahmed);
        session(6, FRIDAY, LocalTime.of(18, 0), 6);
        UUID slot = session(null, SATURDAY, LocalTime.of(10, 0), 4);
        booking(member("Reem Adel"), slot, sara, "upcoming", null, null, at(-3));
        UUID cancelled = cancelledSession(2, MONDAY, LocalTime.of(12, 0));
        assign(cancelled, rider("Nada Ali", 2, sara), sara);
        session(1, SATURDAY.minusDays(1), LocalTime.of(10, 0), 6);
        session(1, FRIDAY.plusDays(1), LocalTime.of(10, 0), 6);

        overview()
                .andExpect(jsonPath("$.week.from").value("2026-10-17"))
                .andExpect(jsonPath("$.week.to").value("2026-10-23"))
                .andExpect(jsonPath("$.week.booked").value(3))
                .andExpect(jsonPath("$.week.capacity").value(16))
                .andExpect(jsonPath("$.week.byTrack[*].levelId", contains(1, 2, 3, 4, 5, 6, null)))
                .andExpect(jsonPath("$.week.byTrack[*].booked", contains(0, 2, 0, 0, 0, 0, 1)))
                .andExpect(jsonPath("$.week.byTrack[*].capacity", contains(0, 6, 0, 0, 0, 6, 4)));
    }

    // Case 6
    @Test
    void onAFridayTheWeekStartsTheSaturdayBefore() throws Exception {
        LocalDate friday = SATURDAY.minusDays(1);
        CLOCK.set(cairo(friday, 20, 0));
        session(3, friday.minusDays(6), LocalTime.of(10, 0), 5);
        session(3, friday, LocalTime.of(10, 0), 5);
        session(3, SATURDAY, LocalTime.of(10, 0), 5);

        overview()
                .andExpect(jsonPath("$.date").value("2026-10-16"))
                .andExpect(jsonPath("$.week.from").value("2026-10-10"))
                .andExpect(jsonPath("$.week.to").value("2026-10-16"))
                .andExpect(jsonPath("$.week.capacity").value(10));
    }

    // Case 7
    @Test
    void overdueResultsAreW2sWaitingRowsOldestSlotFirst() throws Exception {
        // Seven members waiting: six plain bookings and one rebooked after a no-show.
        for (int i = 0; i < 6; i++) {
            UUID slot = session(null, SATURDAY.minusDays(7 - i), LocalTime.of(10, 0), 4);
            booking(member("Waiting " + i), slot, i % 2 == 0 ? sara : ahmed, "upcoming", null, null, at(-200 + i));
        }
        // Only the newer, waiting booking is the rebooked member's row; the old no-show is history.
        UUID rebooked = member("Reem Adel");
        UUID oldSlot = session(null, SATURDAY.minusDays(14), LocalTime.of(10, 0), 4);
        booking(rebooked, oldSlot, sara, "noshow", null, at(-300), at(-400));
        UUID newSlot = session(null, SATURDAY.minusDays(1), LocalTime.of(12, 0), 4);
        UUID newest = booking(rebooked, newSlot, sara, "upcoming", null, null, at(-100));
        jdbc.update("insert into notice (trainer_id, type, ref_id, message) values (?, 'result_overdue', ?, 'Late')",
                sara, newest);

        String body = overview()
                .andExpect(jsonPath("$.attention.overdueResultsCount").value(7))
                .andExpect(jsonPath("$.attention.overdueResults", hasSize(5)))
                .andExpect(jsonPath("$.attention.overdueResults[*].status", everyItem(is("waiting"))))
                .andExpect(jsonPath("$.attention.overdueResults[*].slot.date", contains("2026-10-10", "2026-10-11",
                        "2026-10-12", "2026-10-13", "2026-10-14")))
                .andReturn().getResponse().getContentAsString();
        String w2 = mvc.perform(asAdmin(get("/admin/bookings").param("status", "waiting")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Object> rows = JsonPath.read(body, "$.attention.overdueResults");
        List<Object> w2Rows = JsonPath.read(w2, "$.bookings[0:5]");
        assertThat(rows).isEqualTo(w2Rows);
        assertThat(JsonPath.<List<String>>read(body, "$.attention.overdueResults[0].actions")).contains("remind");
    }

    // Case 8
    @Test
    void ridersWithoutATrainerFollowW3sRule() throws Exception {
        UUID mostafa = trainer("Mostafa Kamel", "active");
        rider("Hala Samy", 2, null);
        rider("Omar Adel", 3, mostafa);
        rider("Nada Ali", 3, mostafa);
        rider("Laila Fouad", 1, sara);
        jdbc.update("update trainer set status = 'disabled' where id = ?", mostafa);

        String w3 = mvc.perform(asAdmin(get("/admin/riders")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int w3Total = JsonPath.read(w3, "$.totals.unassigned");

        overview()
                .andExpect(jsonPath("$.attention.ridersWithoutTrainer").value(3))
                .andExpect(jsonPath("$.attention.ridersWithoutTrainer").value(w3Total))
                .andExpect(jsonPath("$.attention.disabledTrainersWithRiders", hasSize(1)))
                .andExpect(jsonPath("$.attention.disabledTrainersWithRiders[0].id").value(mostafa.toString()))
                .andExpect(jsonPath("$.attention.disabledTrainersWithRiders[0].name").value("Mostafa Kamel"))
                .andExpect(jsonPath("$.attention.disabledTrainersWithRiders[0].riderCount").value(2));
    }

    // Case 9
    @Test
    void invitesNotAcceptedYetAreListedOldestFirstWithTheirExpiry() throws Exception {
        UUID rania = trainer("Rania Fathy", "invited");
        UUID karim = trainer("Karim Nabil", "invited");
        Instant raniaExpiry = SATURDAY_1517.plus(Duration.ofDays(2));
        invite(karim, SATURDAY_1517.minus(Duration.ofDays(4)), SATURDAY_1517.minus(Duration.ofDays(1)));
        invite(rania, SATURDAY_1517.minus(Duration.ofDays(1)), raniaExpiry);
        // An active trainer's unused reset link is not an invite.
        invite(sara, SATURDAY_1517.minus(Duration.ofHours(1)), SATURDAY_1517.plus(Duration.ofDays(1)));

        String body = overview()
                .andExpect(jsonPath("$.attention.pendingInvites[*].trainer.id",
                        contains(karim.toString(), rania.toString())))
                .andExpect(jsonPath("$.attention.pendingInvites[*].trainer.status", everyItem(is("invited"))))
                .andExpect(jsonPath("$.attention.pendingInvites[*].expired", contains(true, false)))
                .andReturn().getResponse().getContentAsString();
        assertThat(Instant.parse(JsonPath.read(body, "$.attention.pendingInvites[1].expiresAt")))
                .isEqualTo(raniaExpiry);
    }

    // Case 10
    @Test
    void activityListsTheTenLatestResultsAndLevelChangesNewestFirst() throws Exception {
        UUID slot = session(null, SATURDAY.minusDays(3), LocalTime.of(10, 0), 10);
        // Results at -1h, -3h, ..., -11h; level changes at -2h, -4h, ..., -12h.
        for (int i = 0; i < 6; i++) {
            boolean placed = i % 2 == 0;
            booking(member("Member " + i), slot, i < 3 ? sara : ahmed, placed ? "placed" : "noshow",
                    placed ? 2 : null, at(-(2 * i + 1)), at(-100));
        }
        UUID rider = rider("Hala Samy", 3, sara);
        for (int i = 0; i < 6; i++) {
            boolean up = i % 2 == 0;
            levelChange(rider, up ? 2 : 3, up ? 3 : 2, ahmed, at(-(2 * i + 2)));
        }

        String body = overview()
                .andExpect(jsonPath("$.activity", hasSize(10)))
                .andExpect(jsonPath("$.activity[*].kind", contains("placed", "promoted", "noshow", "demoted",
                        "placed", "promoted", "noshow", "demoted", "placed", "promoted")))
                .andExpect(jsonPath("$.activity[0].actor.id").value(sara.toString()))
                .andExpect(jsonPath("$.activity[0].actor.name").value("Sara Mahmoud"))
                .andExpect(jsonPath("$.activity[0].actor.status").value("active"))
                .andExpect(jsonPath("$.activity[0].subject").value("Member 0"))
                .andExpect(jsonPath("$.activity[0].fromLevel", nullValue()))
                .andExpect(jsonPath("$.activity[0].toLevel").value(2))
                .andExpect(jsonPath("$.activity[2].toLevel", nullValue()))
                .andExpect(jsonPath("$.activity[1].actor.name").value("Ahmed Said"))
                .andExpect(jsonPath("$.activity[1].subject").value("Hala Samy"))
                .andExpect(jsonPath("$.activity[1].fromLevel").value(2))
                .andExpect(jsonPath("$.activity[1].toLevel").value(3))
                .andExpect(jsonPath("$.activity[3].fromLevel").value(3))
                .andExpect(jsonPath("$.activity[3].toLevel").value(2))
                .andExpect(jsonPath("$.activity[6].actor.name").value("Ahmed Said"))
                .andReturn().getResponse().getContentAsString();
        List<String> at = JsonPath.read(body, "$.activity[*].at");
        assertThat(at.stream().map(Instant::parse).toList()).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(Instant.parse(at.getFirst())).isEqualTo(at(-1));
    }

    // Case 11
    @Test
    void anEmptySchoolGivesEmptyListsAndZeros() throws Exception {
        jdbc.execute("truncate table trainer cascade");

        overview()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsToday", empty()))
                .andExpect(jsonPath("$.week.booked").value(0))
                .andExpect(jsonPath("$.week.capacity").value(0))
                .andExpect(jsonPath("$.week.byTrack", hasSize(7)))
                .andExpect(jsonPath("$.week.byTrack[*].booked", everyItem(is(0))))
                .andExpect(jsonPath("$.week.byTrack[*].capacity", everyItem(is(0))))
                .andExpect(jsonPath("$.attention.overdueResultsCount").value(0))
                .andExpect(jsonPath("$.attention.overdueResults", empty()))
                .andExpect(jsonPath("$.attention.ridersWithoutTrainer").value(0))
                .andExpect(jsonPath("$.attention.disabledTrainersWithRiders", empty()))
                .andExpect(jsonPath("$.attention.pendingInvites", empty()))
                .andExpect(jsonPath("$.activity", empty()));
    }

    // Case 12
    @Test
    void theOverviewRunsTheSameNumberOfQueriesWhateverTheDataSize() throws Exception {
        UUID disabled = trainer("Mostafa Kamel", "active");
        jdbc.update("update trainer set status = 'disabled' where id = ?", disabled);
        populate(1, disabled);
        overview().andExpect(jsonPath("$.attention.overdueResultsCount").value(1));
        long once = statementsFor(this::overview);

        populate(9, disabled);
        overview()
                .andExpect(jsonPath("$.attention.overdueResultsCount").value(10))
                .andExpect(jsonPath("$.sessionsToday", hasSize(20)));
        long tenTimes = statementsFor(this::overview);

        assertThat(tenTimes).isEqualTo(once);
    }

    // Case 13
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(sara, Role.TRAINER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    // Each round: a level session and an assessment slot today with seats, a waiting booking, a placed result,
    // a level change, an invited trainer, and riders without a trainer or with the disabled one.
    private void populate(int rounds, UUID disabled) {
        for (int r = 0; r < rounds; r++) {
            int n = seq;
            UUID session = session(1 + n % 6, SATURDAY, LocalTime.of(7, 0).plusMinutes(n), 6);
            UUID rider = rider("Rider " + n, 2, sara);
            assign(session, rider, sara);
            assign(session, rider("Other " + n, 2, ahmed), ahmed);
            UUID slot = session(null, SATURDAY, LocalTime.of(9, 0).plusMinutes(n), 4);
            UUID waiting = booking(member("Waiting " + n), slot, sara, "upcoming", null, null, at(-50));
            jdbc.update("insert into notice (trainer_id, type, ref_id, message) values (?, 'result_overdue', ?, 'Late')",
                    sara, waiting);
            booking(member("Placed " + n), slot, ahmed, "placed", 2, at(-2), at(-60));
            levelChange(rider, 1, 2, sara, at(-3));
            UUID invited = trainer("Invited " + n, "invited");
            invite(invited, at(-5), at(48));
            rider("Nobody's " + n, 1, null);
            rider("Disabled's " + n, 1, disabled);
        }
    }

    private ResultActions overview() throws Exception {
        return mvc.perform(asAdmin(get(PATH)));
    }

    private long statementsFor(Request request) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        request.run().andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    // Hours from SATURDAY_1517.
    private static Instant at(int hours) {
        return SATURDAY_1517.plus(Duration.ofHours(hours));
    }

    private UUID trainer(String name, String status) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, ?, ?) returning id""",
                UUID.class, name, "0199999%04d".formatted(seq), "overview%d@school.eg".formatted(seq), status,
                "invited".equals(status) ? null : "not-a-real-hash");
    }

    private UUID rider(String name, int level, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, level, trainer);
    }

    private UUID member(String name) {
        return jdbc.queryForObject("insert into member (name) values (?) returning id", UUID.class, name);
    }

    // status "upcoming", "placed", "noshow" or "cancelled"; level only for placed; recordedAt for placed and no-show.
    private UUID booking(UUID member, UUID slot, UUID assessor, String status, Integer level, Instant recordedAt,
                         Instant createdAt) {
        Boolean attended = switch (status) {
            case "placed" -> true;
            case "noshow" -> false;
            default -> null;
        };
        Timestamp cancelledAt = "cancelled".equals(status) ? Timestamp.from(createdAt.plusSeconds(60)) : null;
        return jdbc.queryForObject("""
                        insert into assessment_booking
                            (member_id, session_id, assessor_id, status, attended, level_id, recorded_at,
                             cancelled_at, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id""",
                UUID.class, member, slot, assessor, status, attended, level,
                recordedAt == null ? null : Timestamp.from(recordedAt), cancelledAt, Timestamp.from(createdAt));
    }

    private void levelChange(UUID rider, int from, int to, UUID by, Instant at) {
        jdbc.update("""
                        insert into level_change (rider_id, from_level_id, to_level_id, reason, by_trainer_id, created_at)
                        values (?, ?, ?, 'Ready', ?, ?)""",
                rider, from, to, by, Timestamp.from(at));
    }

    private void invite(UUID trainer, Instant createdAt, Instant expiresAt) {
        seq++;
        String hash = HexFormat.of().formatHex(new byte[32]).substring(4) + "%04d".formatted(seq);
        jdbc.update("""
                        insert into invite (trainer_id, token_hash, purpose, expires_at, created_at)
                        values (?, ?, ?, ?, ?)""",
                trainer, hash, trainer.equals(sara) ? "reset" : "invite", Timestamp.from(expiresAt),
                Timestamp.from(createdAt));
    }

    private void assign(UUID session, UUID rider, UUID assignedBy) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                session, rider, assignedBy);
    }

    // A one-hour session of the level on that date; a null level makes an assessment slot.
    private UUID session(Integer level, LocalDate date, LocalTime start, int capacity) {
        return saveSession(level, date, start, capacity, false);
    }

    private UUID cancelledSession(Integer level, LocalDate date, LocalTime start) {
        return saveSession(level, date, start, 6, true);
    }

    private UUID saveSession(Integer level, LocalDate date, LocalTime start, int capacity, boolean cancelled) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setLevel(level == null ? null : levelRepository.getReferenceById(level.shortValue()));
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(capacity);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            Session session = Session.of(template, date);
            if (cancelled) {
                session.cancel(CLOCK.instant());
            }
            return sessionRepository.save(session).getId();
        });
    }

    private static Instant cairo(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(CAIRO).toInstant();
    }

    @FunctionalInterface
    private interface Request {
        ResultActions run() throws Exception;
    }
}
