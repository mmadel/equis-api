package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.support.MutableClock;
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
import org.springframework.http.MediaType;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E13 · riders are added on the session's day only (screens 2 and 3), over HTTP against a real database.
// Tue 3 Nov 2026 in Cairo. Ahmed's Level 2 riders: Sara (November package, 8), Nour (1 left), Omar (never paid),
// Laila (only a December package). Mona is another trainer with her own Level 2 rider.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=assign-on-the-day-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Testcontainers
public class AssignOnTheDayApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 3);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 7);
    private static final LocalDate MONDAY = TUESDAY.minusDays(1);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);
    private static final Instant AT_16_50 = cairo(TUESDAY, 16, 50);
    private static final Instant AT_17_10 = cairo(TUESDAY, 17, 10);
    private static final int LEVEL_2 = 2;
    private static final int LEVEL_3 = 3;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(AT_16_50);

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

    private int seq;
    private UUID admin;
    private UUID ahmed;
    private UUID mona;
    private UUID sara;
    private UUID nour;
    private UUID omar;
    private UUID laila;
    private UUID monasRider;
    private UUID saraPackage;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, riders, payments and generated sessions.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(AT_16_50);
        seq = 0;
        admin = jdbc.queryForObject("select id from admin_account limit 1", UUID.class);
        ahmed = trainer("Ahmed Said");
        mona = trainer("Mona Adel");
        sara = rider("Sara Ali", LEVEL_2, ahmed);
        saraPackage = pay(sara, LocalDate.of(2026, 11, 1), 8);
        nour = rider("Nour Adel", LEVEL_2, ahmed);
        pay(nour, LocalDate.of(2026, 11, 1), 1);
        omar = rider("Omar Hany", LEVEL_2, ahmed);
        laila = rider("Laila Nour", LEVEL_2, ahmed);
        pay(laila, LocalDate.of(2026, 12, 1), 8);
        monasRider = rider("Youssef Tarek", LEVEL_2, mona);
        pay(monasRider, LocalDate.of(2026, 11, 1), 8);
    }

    // Case 1
    @Test
    void todayBeforeTheStartListsOnlyRidersWhoCanRideToday() throws Exception {
        UUID session = session(LEVEL_2, TUESDAY, FIVE_PM);

        detail(session, ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("scheduled"))
                .andExpect(jsonPath("$.finishedAt").value(nullValue()))
                .andExpect(jsonPath("$.canAssign").value(true))
                .andExpect(jsonPath("$.canFinish").value(false))
                .andExpect(jsonPath("$.readOnly").value(false))
                .andExpect(jsonPath("$.riders[*].name", contains("Nour Adel", "Sara Ali")))
                .andExpect(jsonPath("$.riders[*].sessionsLeft", contains(1, 8)));
    }

    // Case 2
    @Test
    void afterTheStartRidersCanStillBeAddedAndTheSessionFinished() throws Exception {
        UUID session = session(LEVEL_2, TUESDAY, FIVE_PM);
        assign(session, sara, ahmed);
        CLOCK.set(AT_17_10);

        detail(session, ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canAssign").value(true))
                .andExpect(jsonPath("$.canFinish").value(true))
                .andExpect(jsonPath("$.mySeats[0].riderId").value(sara.toString()))
                .andExpect(jsonPath("$.mySeats[0].sessionsLeft").value(8));
    }

    // Case 3
    @Test
    void anotherDaysSessionIsReadOnlyWithNoPickList() throws Exception {
        UUID saturday = session(LEVEL_2, SATURDAY, FIVE_PM);

        detail(saturday, ahmed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canAssign").value(false))
                .andExpect(jsonPath("$.readOnly").value(true))
                .andExpect(jsonPath("$.riders", empty()));
    }

    // Case 4
    @Test
    void ridersAreAddedBeforeAndAfterTheStart() throws Exception {
        UUID session = session(LEVEL_2, TUESDAY, FIVE_PM);

        save(session, ahmed, sara).andExpect(status().isOk());
        CLOCK.set(AT_17_10);
        save(session, ahmed, sara, nour)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mySeats[*].name", contains("Nour Adel", "Sara Ali")));

        assertThat(seatedBy(session, ahmed)).containsExactlyInAnyOrder(sara, nour);
    }

    // Cases 5 and 6
    @Test
    void savingOnAnotherDayIsRefused() throws Exception {
        UUID saturday = session(LEVEL_2, SATURDAY, FIVE_PM);
        UUID yesterday = session(LEVEL_2, MONDAY, FIVE_PM);

        for (UUID session : List.of(saturday, yesterday)) {
            save(session, ahmed, sara)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("session_not_today"));
            assertThat(seatedBy(session, ahmed)).isEmpty();
        }
    }

    // Case 7
    @Test
    void savingOnAFinishedOrCancelledSessionIsRefused() throws Exception {
        UUID finished = finishedSession(LEVEL_2, TUESDAY, LocalTime.of(15, 0));
        UUID cancelled = saveSession(LEVEL_2, TUESDAY, FIVE_PM, 6, State.CANCELLED);

        for (UUID session : List.of(finished, cancelled)) {
            save(session, ahmed, sara)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("session_read_only"));
        }
    }

    // Case 8
    @Test
    void addingARiderWithoutASessionLeftTodayIsRefused() throws Exception {
        UUID session = session(LEVEL_2, TUESDAY, FIVE_PM);

        for (UUID rider : List.of(omar, laila)) {
            save(session, ahmed, sara, rider)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("rider_no_sessions_left"))
                    .andExpect(jsonPath("$.fieldErrors[*].field", contains(rider.toString())));
        }
        assertThat(seatedBy(session, ahmed)).isEmpty();
    }

    // Case 9
    @Test
    void aSeatedRiderWhosePackageWasVoidedIsKeptAndShowsZero() throws Exception {
        UUID session = session(LEVEL_2, TUESDAY, FIVE_PM);
        assign(session, sara, ahmed);
        jdbc.update("update payment set voided_at = now(), voided_by = ?, void_reason = 'Entered twice' where id = ?",
                admin, saraPackage);

        save(session, ahmed, sara, nour).andExpect(status().isOk());

        detail(session, ahmed)
                .andExpect(jsonPath("$.mySeats[?(@.name == 'Sara Ali')].sessionsLeft", contains(0)))
                .andExpect(jsonPath("$.riders[*].name", contains("Nour Adel")));
        assertThat(seatedBy(session, ahmed)).containsExactlyInAnyOrder(sara, nour);
    }

    // Case 10
    @Test
    void onlyATrainerWithARiderInItOrAnyoneOnAnEmptySessionCanFinish() throws Exception {
        UUID monasOnly = session(LEVEL_2, TUESDAY, FIVE_PM);
        assign(monasOnly, monasRider, mona);
        UUID empty = session(LEVEL_2, TUESDAY, FIVE_PM);
        CLOCK.set(AT_17_10);

        detail(monasOnly, ahmed).andExpect(jsonPath("$.canFinish").value(false));
        detail(monasOnly, mona).andExpect(jsonPath("$.canFinish").value(true));
        detail(empty, ahmed).andExpect(jsonPath("$.canFinish").value(true));
    }

    // Case 11
    @Test
    void levelSessionRowsCarryTheirStatus() throws Exception {
        UUID finished = finishedSession(LEVEL_2, TUESDAY, LocalTime.of(15, 0));
        UUID scheduled = session(LEVEL_2, TUESDAY, FIVE_PM);
        saveSession(LEVEL_2, TUESDAY, LocalTime.of(19, 0), 6, State.CANCELLED);

        mvc.perform(as(ahmed, get("/me/levels/" + LEVEL_2 + "/sessions")
                        .param("from", "2026-11-03").param("to", "2026-11-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].sessions[*].id", contains(finished.toString(), scheduled.toString())))
                .andExpect(jsonPath("$.days[0].sessions[*].status", contains("finished", "scheduled")));
    }

    // Case 12: the release 1 rules still hold on today's sessions.
    @Test
    void earlierRulesStillHoldOnTodaysSessions() throws Exception {
        UUID session = session(LEVEL_2, TUESDAY, FIVE_PM, 2);
        UUID levelThree = rider("Mariam Fouad", LEVEL_3, ahmed);
        pay(levelThree, LocalDate.of(2026, 11, 1), 8);
        assign(session, monasRider, mona);

        save(session, ahmed, sara, monasRider)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("not_your_rider"));
        save(session, ahmed, sara, levelThree)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("level_out_of_range"));
        save(session, ahmed, sara, nour)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("capacity_exceeded"));
        // Repeated ids are seated once, and Mona's seat is untouched.
        save(session, ahmed, sara, sara).andExpect(status().isOk());

        assertThat(seatedBy(session, ahmed)).containsExactly(sara);
        assertThat(seatedBy(session, mona)).containsExactly(monasRider);
    }

    // Case 13
    @Test
    void theRiderListCostsTheSameNumberOfStatementsWithOneOrTwelveRiders() throws Exception {
        UUID session = session(LEVEL_3, TUESDAY, FIVE_PM);
        pay(rider("Rider 1", LEVEL_3, ahmed), LocalDate.of(2026, 11, 1), 8);
        long withOne = statementsFor(session);

        for (int i = 2; i <= 12; i++) {
            pay(rider("Rider " + i, LEVEL_3, ahmed), LocalDate.of(2026, 11, 1), 8);
        }
        detail(session, ahmed).andExpect(jsonPath("$.riders.length()").value(12));
        long withTwelve = statementsFor(session);

        assertThat(withTwelve).isEqualTo(withOne);
    }

    private ResultActions detail(UUID session, UUID trainer) throws Exception {
        return mvc.perform(as(trainer, get("/me/sessions/" + session)));
    }

    private ResultActions save(UUID session, UUID trainer, UUID... riders) throws Exception {
        String ids = List.of(riders).stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
        return mvc.perform(as(trainer, put("/me/sessions/" + session + "/assignments"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"riderIds\": [" + ids + "]}"));
    }

    private long statementsFor(UUID session) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        detail(session, ahmed).andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private List<UUID> seatedBy(UUID session, UUID trainer) {
        return jdbc.queryForList("select rider_id from assignment where session_id = ? and assigned_by = ?",
                UUID.class, session, trainer);
    }

    private MockHttpServletRequestBuilder as(UUID trainer, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(trainer, Role.TRAINER)).value());
    }

    private UUID trainer(String name) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, 'active', 'not-a-real-hash') returning id""",
                UUID.class, name, "0199998%04d".formatted(seq), "on-the-day%d@school.eg".formatted(seq));
    }

    private UUID rider(String name, int level, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, level, trainer);
    }

    // A one-month package starting on startMonth, valid to the end of the following month.
    private UUID pay(UUID rider, LocalDate startMonth, int sessions) {
        return jdbc.queryForObject("""
                        insert into payment (rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_by)
                        values (?, 1200, 'cash', ?, 1, ?, ?, ?) returning id""",
                UUID.class, rider, startMonth, sessions, startMonth.plusMonths(2).minusDays(1), admin);
    }

    private void assign(UUID session, UUID rider, UUID assignedBy) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                session, rider, assignedBy);
    }

    private UUID session(int level, LocalDate date, LocalTime start) {
        return session(level, date, start, 6);
    }

    private UUID session(int level, LocalDate date, LocalTime start, int capacity) {
        return saveSession(level, date, start, capacity, State.SCHEDULED);
    }

    private UUID finishedSession(int level, LocalDate date, LocalTime start) {
        return saveSession(level, date, start, 6, State.FINISHED);
    }

    private enum State { SCHEDULED, CANCELLED, FINISHED }

    // A one-hour session of the level on that date.
    private UUID saveSession(int level, LocalDate date, LocalTime start, int capacity, State state) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setLevel(levelRepository.getReferenceById((short) level));
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(capacity);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            Session session = Session.of(template, date);
            if (state == State.CANCELLED) {
                session.cancel(CLOCK.instant());
            } else if (state == State.FINISHED) {
                session.finish(ahmed, CLOCK.instant());
            }
            return sessionRepository.save(session).getId();
        });
    }

    private static Instant cairo(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(CAIRO).toInstant();
    }
}
