package com.equestrian.equis.controller.evaluations;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E14 · GET /admin/evaluations/follow-up over HTTP against a real database. Tue 3 Nov 2026 in Cairo.
// 17:00 Level 2 finished: Ahmed seated Sara (evaluated) and Omar (not), Mona seated Hala (not).
// 18:00 Level 4 scheduled: Mona seated Yara and Ziad. 20:00 Level 1 scheduled and empty.
// 12:00 assessment slot with a seat in it, which must never show.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=evaluation-follow-up-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Testcontainers
public class EvaluationFollowUpApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final LocalDate NOV_2 = LocalDate.of(2026, 11, 2);
    private static final LocalDate NOV_3 = LocalDate.of(2026, 11, 3);
    private static final LocalDate NOV_10 = LocalDate.of(2026, 11, 10);
    private static final Instant TUE_20_30 = cairo(NOV_3, 20, 30);
    private static final String URL = "/admin/evaluations/follow-up";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(TUE_20_30);

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
    @Autowired
    EntityManagerFactory entityManagerFactory;

    private int seq;
    private UUID admin;
    private UUID ahmed;
    private UUID mona;
    private UUID sara;
    private UUID omar;
    private UUID hala;
    private UUID yara;
    private UUID ziad;
    private UUID s17;
    private UUID s18;
    private UUID s20;
    private UUID s12;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, riders, payments, generated sessions and evaluations.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(TUE_20_30);
        seq = 0;
        admin = jdbc.queryForObject("select id from admin_account limit 1", UUID.class);
        ahmed = trainer("Ahmed Said");
        mona = trainer("Mona Fathy");
        sara = rider("Sara Ali", 2, ahmed);
        omar = rider("Omar Nabil", 2, ahmed);
        hala = rider("Hala Samir", 2, mona);
        yara = rider("Yara Adel", 4, mona);
        ziad = rider("Ziad Hany", 4, mona);

        s17 = session(NOV_3, LocalTime.of(17, 0), 2);
        seat(s17, sara, ahmed);
        seat(s17, omar, ahmed);
        seat(s17, hala, mona);
        finish(s17);
        evaluate(s17, sara, ahmed);

        s18 = session(NOV_3, LocalTime.of(18, 0), 4);
        seat(s18, yara, mona);
        seat(s18, ziad, mona);

        s20 = session(NOV_3, LocalTime.of(20, 0), 1);

        s12 = session(NOV_3, LocalTime.of(12, 0), null);
        seat(s12, rider("Nour Hany", 1, ahmed), ahmed);
    }

    // Case 1
    @Test
    void theEveningListGroupsLooseEndsByTrainer() throws Exception {
        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-03"))
                .andExpect(jsonPath("$.totals.trainers").value(2))
                .andExpect(jsonPath("$.totals.notFinished").value(1))
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(2))
                .andExpect(jsonPath("$.trainers[*].trainerName", contains("Ahmed Said", "Mona Fathy")))
                .andExpect(jsonPath("$.trainers[0].trainerId").value(ahmed.toString()))
                .andExpect(jsonPath("$.trainers[0].trainerActive").value(true))
                .andExpect(jsonPath("$.trainers[0].notFinished", empty()))
                .andExpect(jsonPath("$.trainers[0].toEvaluate", hasSize(1)))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].sessionId").value(s17.toString()))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].start").value("17:00"))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].end").value("18:00"))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].levelId").value(2))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].riderId", contains(omar.toString())))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].name", contains("Omar Nabil")))
                .andExpect(jsonPath("$.trainers[1].trainerId").value(mona.toString()))
                .andExpect(jsonPath("$.trainers[1].notFinished", hasSize(1)))
                .andExpect(jsonPath("$.trainers[1].notFinished[0].sessionId").value(s18.toString()))
                .andExpect(jsonPath("$.trainers[1].notFinished[0].start").value("18:00"))
                .andExpect(jsonPath("$.trainers[1].notFinished[0].end").value("19:00"))
                .andExpect(jsonPath("$.trainers[1].notFinished[0].levelId").value(4))
                .andExpect(jsonPath("$.trainers[1].notFinished[0].riders").value(2))
                .andExpect(jsonPath("$.trainers[1].toEvaluate[*].sessionId", contains(s17.toString())))
                .andExpect(jsonPath("$.trainers[1].toEvaluate[0].riders[*].name", contains("Hala Samir")));
    }

    // Case 2
    @Test
    void aSessionThatHasNotStartedIsNotListedYet() throws Exception {
        CLOCK.set(cairo(NOV_3, 17, 30));

        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.notFinished").value(0))
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(2))
                .andExpect(jsonPath("$.trainers[*].notFinished[*]", empty()))
                .andExpect(jsonPath("$.trainers[*].toEvaluate[*].sessionId",
                        contains(s17.toString(), s17.toString())));
    }

    // Case 3
    @Test
    void anEmptyUnfinishedSessionIsNotListed() throws Exception {
        CLOCK.set(cairo(NOV_3, 21, 30));

        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.notFinished").value(1))
                .andExpect(jsonPath("$.trainers[*].notFinished[*].sessionId", contains(s18.toString())))
                .andExpect(jsonPath("$.trainers[*].notFinished[*].sessionId", not(hasItem(s20.toString()))));
    }

    // Case 4
    @Test
    void anAssessmentSlotIsNeverListed() throws Exception {
        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainers[*].notFinished[*].sessionId", not(hasItem(s12.toString()))))
                .andExpect(jsonPath("$.trainers[*].toEvaluate[*].sessionId", not(hasItem(s12.toString()))));
    }

    // Case 5
    @Test
    void nothingMissingGivesAnEmptyList() throws Exception {
        evaluate(s17, omar, ahmed);
        evaluate(s17, hala, mona);
        finish(s18);
        evaluate(s18, yara, mona);

        // Ziad is still to evaluate in 18:00.
        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(1))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[0].riders[*].name", contains("Ziad Hany")));

        evaluate(s18, ziad, mona);
        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainers", empty()))
                .andExpect(jsonPath("$.totals.trainers").value(0))
                .andExpect(jsonPath("$.totals.notFinished").value(0))
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(0));
    }

    // Case 6
    @Test
    void aDoneSessionIsNotListed() throws Exception {
        jdbc.update("update session set status = 'done' where id = ?", s17);

        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(0))
                .andExpect(jsonPath("$.trainers[*].trainerName", contains("Mona Fathy")))
                .andExpect(jsonPath("$.trainers[0].toEvaluate", empty()));
    }

    // Case 7
    @Test
    void aCancelledSessionWithSeatsIsNotListed() throws Exception {
        jdbc.update("update session set status = 'cancelled', cancelled_at = ? where id = ?",
                Timestamp.from(cairo(NOV_3, 9, 0)), s18);

        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.notFinished").value(0))
                .andExpect(jsonPath("$.trainers[*].notFinished[*]", empty()));
    }

    // Case 8
    @Test
    void yesterdaysLooseEndsOnly() throws Exception {
        UUID mon17 = session(NOV_2, LocalTime.of(17, 0), 2);
        seat(mon17, sara, ahmed);

        mvc.perform(asAdmin(get(URL).param("date", "2026-11-02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-02"))
                .andExpect(jsonPath("$.totals.trainers").value(1))
                .andExpect(jsonPath("$.totals.notFinished").value(1))
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(0))
                .andExpect(jsonPath("$.trainers[*].trainerName", contains("Ahmed Said")))
                .andExpect(jsonPath("$.trainers[0].notFinished[*].sessionId", contains(mon17.toString())))
                .andExpect(jsonPath("$.trainers[0].toEvaluate", empty()));
    }

    // Case 9
    @Test
    void aFutureDateIsEmpty() throws Exception {
        UUID next = session(NOV_10, LocalTime.of(17, 0), 2);
        seat(next, sara, ahmed);

        mvc.perform(asAdmin(get(URL).param("date", "2026-11-10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-10"))
                .andExpect(jsonPath("$.trainers", empty()))
                .andExpect(jsonPath("$.totals.trainers").value(0));
    }

    // Case 10
    @Test
    void aMalformedDateIsRejected() throws Exception {
        mvc.perform(asAdmin(get(URL).param("date", "03-11-2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("date"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("invalid_date"));
    }

    // Case 11
    @Test
    void aDisabledTrainerIsStillListed() throws Exception {
        jdbc.update("update trainer set status = 'disabled' where id = ?", mona);

        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainers[*].trainerName", contains("Ahmed Said", "Mona Fathy")))
                .andExpect(jsonPath("$.trainers[0].trainerActive").value(true))
                .andExpect(jsonPath("$.trainers[1].trainerActive").value(false))
                .andExpect(jsonPath("$.trainers[1].notFinished", hasSize(1)));
    }

    // Case 12
    @Test
    void sortedByTrainerNameThenStartThenRiderName() throws Exception {
        UUID zeina = trainer("Zeina Kamal");
        UUID adam = rider("Adam Fouad", 2, ahmed);
        seat(s17, adam, ahmed);
        UUID s16 = session(NOV_3, LocalTime.of(16, 0), 2);
        seat(s16, sara, ahmed);
        finish(s16);
        seat(s18, rider("Laila Omar", 4, zeina), zeina);

        mvc.perform(asAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainers[*].trainerName",
                        contains("Ahmed Said", "Mona Fathy", "Zeina Kamal")))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[*].start", contains("16:00", "17:00")))
                .andExpect(jsonPath("$.trainers[0].toEvaluate[1].riders[*].name",
                        contains("Adam Fouad", "Omar Nabil")))
                .andExpect(jsonPath("$.trainers[2].notFinished[*].sessionId", contains(s18.toString())))
                .andExpect(jsonPath("$.trainers[2].notFinished[0].riders").value(1))
                // 18:00 is shared by Mona and Zeina but counted once.
                .andExpect(jsonPath("$.totals.notFinished").value(1))
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(4));
    }

    // Case 13
    @Test
    void twentySessionsTakeAsManyQueriesAsOne() throws Exception {
        LocalDate oct26 = LocalDate.of(2026, 10, 26);
        UUID one = session(oct26, LocalTime.of(17, 0), 2);
        seat(one, sara, ahmed);
        finish(one);

        LocalDate oct27 = LocalDate.of(2026, 10, 27);
        for (int i = 0; i < 20; i++) {
            UUID s = session(oct27, LocalTime.of(8, 0).plusMinutes(30L * i), 1 + i % 6);
            UUID coach = i % 2 == 0 ? ahmed : mona;
            UUID r1 = rider("Rider A" + i, 2, coach);
            UUID r2 = rider("Rider B" + i, 2, coach);
            seat(s, r1, coach);
            seat(s, r2, coach);
            if (i % 3 != 0) {
                finish(s);
                if (i % 2 == 0) {
                    evaluate(s, r1, coach);
                }
            }
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mvc.perform(asAdmin(get(URL).param("date", "2026-10-26")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.ridersToEvaluate").value(1));
        long oneSession = statistics.getPrepareStatementCount();

        statistics.clear();
        mvc.perform(asAdmin(get(URL).param("date", "2026-10-27")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.trainers").value(2))
                .andExpect(jsonPath("$.totals.notFinished").value(7));
        long twentySessions = statistics.getPrepareStatementCount();

        assertThat(twentySessions).isEqualTo(oneSession);
        assertThat(twentySessions).isLessThanOrEqualTo(3);
    }

    // Case 14
    @Test
    void adminOnly() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(as(ahmed, get(URL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    private MockHttpServletRequestBuilder as(UUID trainer, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(trainer, Role.TRAINER)).value());
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(admin, Role.ADMIN)).value());
    }

    private UUID trainer(String name) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, 'active', 'not-a-real-hash') returning id""",
                UUID.class, name, "0199996%04d".formatted(seq), "followup%d@school.eg".formatted(seq));
    }

    private UUID rider(String name, int level, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, level, trainer);
    }

    private void seat(UUID sessionId, UUID rider, UUID trainer) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                sessionId, rider, trainer);
    }

    private void evaluate(UUID sessionId, UUID rider, UUID trainer) {
        jdbc.update("""
                        insert into session_evaluation (session_id, rider_id, trainer_id, stars, comment, created_at)
                        values (?, ?, ?, 4, null, ?)""",
                sessionId, rider, trainer, Timestamp.from(CLOCK.instant()));
    }

    // Marked finished by Ahmed at the session's end.
    private void finish(UUID sessionId) {
        jdbc.update("""
                        update session set status = 'finished', finished_by = ?,
                               finished_at = (session_date + end_time) at time zone 'Africa/Cairo'
                        where id = ?""",
                ahmed, sessionId);
    }

    // A scheduled one-hour session on that date; level null makes an assessment slot.
    private UUID session(LocalDate date, LocalTime start, Integer level) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setLevel(level == null ? null : levelRepository.getReferenceById(level.shortValue()));
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(6);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }

    private static Instant cairo(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(CAIRO).toInstant();
    }
}
