package com.equestrian.equis.controller.evaluations;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E14 · GET /me/riders/{id}/evaluations (screen 4a) and GET /admin/riders/{id}/evaluations (W3), over HTTP
// against a real database. Wed 4 Nov 2026 in Cairo, 10:00. Sara, now Ahmed's, rode Level 2 on Tue 27 Oct
// (seated and evaluated by Mona) and on Tue 3 Nov (seated and evaluated by Ahmed).
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=evaluation-history-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
public class RiderEvaluationHistoryApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final LocalDate OCT_27 = LocalDate.of(2026, 10, 27);
    private static final LocalDate NOV_3 = LocalDate.of(2026, 11, 3);
    private static final Instant WED_10_00 = cairo(LocalDate.of(2026, 11, 4), 10, 0);
    private static final int LEVEL_2 = 2;

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    static final MutableClock CLOCK = new MutableClock(WED_10_00);

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
    private UUID oct27;
    private UUID nov3;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers, riders, payments, generated sessions and evaluations.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        CLOCK.set(WED_10_00);
        seq = 0;
        admin = jdbc.queryForObject("select id from admin_account limit 1", UUID.class);
        ahmed = trainer("Ahmed Said");
        mona = trainer("Mona Fathy");
        sara = rider("Sara Ali", ahmed);

        oct27 = finishedSession(OCT_27, LocalTime.of(16, 0));
        evaluated(oct27, sara, mona, 3, "Nervous at the canter", cairo(OCT_27, 17, 20));
        nov3 = finishedSession(NOV_3, LocalTime.of(17, 0));
        evaluated(nov3, sara, ahmed, 4, "Better heels down", cairo(NOV_3, 18, 10));
    }

    // Case 1
    @Test
    void ahmedSeesSarasWholeHistoryNewestFirst() throws Exception {
        expectSarasTwoLines(mvc.perform(as(ahmed, get(mine(sara)))));
    }

    // Case 2
    @Test
    void adminSeesTheSameLines() throws Exception {
        expectSarasTwoLines(mvc.perform(asAdmin(get(admins(sara)))));
    }

    // Case 3
    @Test
    void monaCannotSeeARiderWhoIsNoLongerHers() throws Exception {
        mvc.perform(as(mona, get(mine(sara))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
    }

    // Case 4
    @Test
    void aRiderWithoutEvaluationsGetsAnEmptyList() throws Exception {
        UUID omar = rider("Omar Nabil", ahmed);

        mvc.perform(as(ahmed, get(mine(omar))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderId").value(omar.toString()))
                .andExpect(jsonPath("$.riderName").value("Omar Nabil"))
                .andExpect(jsonPath("$.evaluations", empty()));
        mvc.perform(asAdmin(get(admins(omar))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations", empty()));
    }

    // Case 5
    @Test
    void aPromotionKeepsTheSessionsLevelOnOldLines() throws Exception {
        jdbc.update("update rider set level_id = 3 where id = ?", sara);

        mvc.perform(as(ahmed, get(mine(sara))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations[*].levelId", contains(LEVEL_2, LEVEL_2)));
    }

    // Case 6
    @Test
    void theLaterSessionOfTheSameDayComesFirst() throws Exception {
        UUID hala = rider("Hala Samir", ahmed);
        UUID four = finishedSession(NOV_3, LocalTime.of(16, 0));
        UUID six = finishedSession(NOV_3, LocalTime.of(18, 0));
        evaluated(four, hala, ahmed, 2, "Early one", cairo(NOV_3, 19, 0));
        evaluated(six, hala, ahmed, 5, "Late one", cairo(NOV_3, 19, 5));

        mvc.perform(as(ahmed, get(mine(hala))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations[*].sessionId", contains(six.toString(), four.toString())))
                .andExpect(jsonPath("$.evaluations[*].start", contains("18:00", "16:00")));
    }

    // Case 7
    @Test
    void aMissingCommentIsNull() throws Exception {
        UUID hala = rider("Hala Samir", ahmed);
        evaluated(nov3, hala, ahmed, 3, null, cairo(NOV_3, 18, 15));

        mvc.perform(as(ahmed, get(mine(hala))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations", hasSize(1)))
                .andExpect(jsonPath("$.evaluations[0].stars").value(3))
                .andExpect(jsonPath("$.evaluations[0].comment").value(nullValue()))
                .andExpect(content().string(containsString("\"comment\":null")));
    }

    // Case 8
    @Test
    void unknownOrMalformedIds() throws Exception {
        UUID unknown = UUID.randomUUID();
        mvc.perform(as(ahmed, get(mine(unknown))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
        mvc.perform(asAdmin(get(admins(unknown))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
        mvc.perform(as(ahmed, get("/me/riders/abc/evaluations")))
                .andExpect(status().isBadRequest());
        mvc.perform(asAdmin(get("/admin/riders/abc/evaluations")))
                .andExpect(status().isBadRequest());
    }

    // Case 9
    @Test
    void adminSeesAClosedPersonsRider() throws Exception {
        UUID member = jdbc.queryForObject(
                "insert into member (name, phone, email, closed_at) values (?, ?, ?, ?) returning id",
                UUID.class, "Nour Hany", "01099990001", "nour@family.eg", Timestamp.from(cairo(NOV_3, 9, 0)));
        UUID nour = jdbc.queryForObject(
                "insert into rider (name, level_id, member_id) values (?, ?, ?) returning id",
                UUID.class, "Nour Hany", LEVEL_2, member);
        evaluated(oct27, nour, mona, 4, "Steady", cairo(OCT_27, 17, 25));

        mvc.perform(asAdmin(get(admins(nour))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderName").value("Nour Hany"))
                .andExpect(jsonPath("$.evaluations[*].trainerName", contains("Mona Fathy")));
    }

    // Case 10
    @Test
    void eachPathIsForItsOwnRole() throws Exception {
        mvc.perform(get(mine(sara)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(admins(sara)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(asAdmin(get(mine(sara))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(as(ahmed, get(admins(sara))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    // Case 11
    @Test
    void thirtyEvaluationsTakeAtMostTwoQueries() throws Exception {
        UUID karim = rider("Karim Adel", ahmed);
        LocalDate day = LocalDate.of(2026, 9, 1);
        for (int i = 0; i < 30; i++) {
            UUID s = finishedSession(day.plusDays(i), LocalTime.of(17, 0));
            evaluated(s, karim, i % 2 == 0 ? ahmed : mona, 1 + i % 5, "Ride " + i,
                    cairo(day.plusDays(i), 18, 10));
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mvc.perform(as(ahmed, get(mine(karim))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations", hasSize(30)))
                .andExpect(jsonPath("$.evaluations[0].comment").value("Ride 29"));
        long trainerQueries = statistics.getPrepareStatementCount();

        statistics.clear();
        mvc.perform(asAdmin(get(admins(karim))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations", hasSize(30)));
        long adminQueries = statistics.getPrepareStatementCount();

        assertThat(adminQueries).isLessThanOrEqualTo(2);
        // The trainer path also checks that the token's trainer is still active, before the controller runs.
        assertThat(trainerQueries).isLessThanOrEqualTo(3);
    }

    private void expectSarasTwoLines(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.riderId").value(sara.toString()))
                .andExpect(jsonPath("$.riderName").value("Sara Ali"))
                .andExpect(jsonPath("$.evaluations", hasSize(2)))
                .andExpect(jsonPath("$.evaluations[*].sessionId", contains(nov3.toString(), oct27.toString())))
                .andExpect(jsonPath("$.evaluations[*].date", contains("2026-11-03", "2026-10-27")))
                .andExpect(jsonPath("$.evaluations[*].start", contains("17:00", "16:00")))
                .andExpect(jsonPath("$.evaluations[*].end", contains("18:00", "17:00")))
                .andExpect(jsonPath("$.evaluations[*].levelId", contains(LEVEL_2, LEVEL_2)))
                .andExpect(jsonPath("$.evaluations[*].trainerName", contains("Ahmed Said", "Mona Fathy")))
                .andExpect(jsonPath("$.evaluations[*].stars", contains(4, 3)))
                .andExpect(jsonPath("$.evaluations[*].comment",
                        contains("Better heels down", "Nervous at the canter")))
                .andExpect(jsonPath("$.evaluations[*].createdAt",
                        contains("2026-11-03T16:10:00Z", "2026-10-27T14:20:00Z")));
    }

    private static String mine(UUID rider) {
        return "/me/riders/" + rider + "/evaluations";
    }

    private static String admins(UUID rider) {
        return "/admin/riders/" + rider + "/evaluations";
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
                UUID.class, name, "0199995%04d".formatted(seq), "history%d@school.eg".formatted(seq));
    }

    private UUID rider(String name, UUID trainer) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                UUID.class, name, LEVEL_2, trainer);
    }

    // The seat and the evaluation together; writing them is covered by SessionEvaluationApiTest.
    private void evaluated(UUID sessionId, UUID rider, UUID trainer, int stars, String comment, Instant at) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                sessionId, rider, trainer);
        jdbc.update("""
                        insert into session_evaluation (session_id, rider_id, trainer_id, stars, comment, created_at)
                        values (?, ?, ?, ?, ?, ?)""",
                sessionId, rider, trainer, stars, comment, Timestamp.from(at));
    }

    // A finished one-hour Level 2 session on that date.
    private UUID finishedSession(LocalDate date, LocalTime start) {
        UUID id = new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setLevel(levelRepository.getReferenceById((short) LEVEL_2));
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(6);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
        Instant end = LocalDateTime.of(date, start.plusHours(1)).atZone(CAIRO).toInstant();
        jdbc.update("update session set status = 'finished', finished_at = ?, finished_by = ? where id = ?",
                Timestamp.from(end), ahmed, id);
        return id;
    }

    private static Instant cairo(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(CAIRO).toInstant();
    }
}
