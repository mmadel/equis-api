package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.support.MutableClock;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /admin/assessment/slots (W2's New person panel) over HTTP against a real database.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=admin-slots@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
public class AdminAssessmentSlotApiTest {
    private static final String ADMIN_PATH = "/admin/assessment/slots";
    private static final String TRAINER_PATH = "/assessment/slots";
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Friday 16 Oct 2026, 12:00 school time.
    private static final Instant FRIDAY_NOON = ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, CAIRO).toInstant();
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

    private UUID trainer;
    private UUID saturday;
    private int seq;

    // Sat 24 Oct 10:00 with 3 of 4 seats booked.
    @BeforeEach
    void setUp() {
        CLOCK.set(FRIDAY_NOON);
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        seq = 0;
        trainer = trainer("Ahmed Said");
        saturday = slot(SAT_24, TEN, 4);
        for (int i = 0; i < 3; i++) {
            booking(member(), saturday, trainer);
        }
    }

    // Case 1
    @Test
    void adminGetsTheSameListAsTheTrainer() throws Exception {
        String admin = body(asAdmin(get(ADMIN_PATH).param("from", "2026-10-23").param("to", "2026-11-19")));
        String trainerView = body(asTrainer(get(TRAINER_PATH).param("from", "2026-10-23").param("to", "2026-11-19")));

        assertThat(admin).isEqualTo(trainerView);
        mvc.perform(asAdmin(get(ADMIN_PATH).param("from", "2026-10-23").param("to", "2026-11-19")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-23"))
                .andExpect(jsonPath("$.to").value("2026-11-19"))
                .andExpect(jsonPath("$.days.length()").value(1))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-24"))
                .andExpect(jsonPath("$.days[0].slots[0].id").value(saturday.toString()))
                .andExpect(jsonPath("$.days[0].slots[0].start").value("10:00"))
                .andExpect(jsonPath("$.days[0].slots[0].capacity").value(4))
                .andExpect(jsonPath("$.days[0].slots[0].booked").value(3))
                .andExpect(jsonPath("$.days[0].slots[0].open").value(true));
    }

    // Case 2: today is Fri 16 Oct, so 16..29 Oct.
    @Test
    void withoutDatesItIsTheComingTwoWeeks() throws Exception {
        mvc.perform(asAdmin(get(ADMIN_PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-16"))
                .andExpect(jsonPath("$.to").value("2026-10-29"))
                .andExpect(jsonPath("$.days[0].slots[0].id").value(saturday.toString()));
    }

    // Case 3
    @Test
    void datesOutOfOrderAreInvalidRange() throws Exception {
        mvc.perform(asAdmin(get(ADMIN_PATH).param("from", "2026-10-28").param("to", "2026-10-15")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("invalid_range"));
    }

    // Case 4
    @Test
    void trainerIsForbidden() throws Exception {
        mvc.perform(asTrainer(get(ADMIN_PATH)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    // Case 5
    @Test
    void noTokenIsUnauthorized() throws Exception {
        mvc.perform(get(ADMIN_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    // Case 6: the trainer endpoint is unchanged, and still closed to the admin.
    @Test
    void trainerEndpointStaysTrainerOnly() throws Exception {
        mvc.perform(asTrainer(get(TRAINER_PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].slots[0].booked").value(3));
        mvc.perform(asAdmin(get(TRAINER_PATH)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    private String body(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)));
    }

    private MockHttpServletRequestBuilder asTrainer(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(trainer, Role.TRAINER)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private UUID trainer(String name) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, 'active', 'not-a-real-hash') returning id""",
                UUID.class, name, "0199999%04d".formatted(seq), "trainer%d@school.eg".formatted(seq));
    }

    private UUID member() {
        seq++;
        return jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, "Rider " + seq, "0128888%04d".formatted(seq), "member%d@mail.eg".formatted(seq));
    }

    // An assessment slot (no level), one hour long, saved through JPA as the app saves them.
    private UUID slot(LocalDate date, LocalTime start, int capacity) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(start);
            template.setEndTime(start.plusHours(1));
            template.setCapacity(capacity);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }

    private void booking(UUID member, UUID slot, UUID assessor) {
        jdbc.update("insert into assessment_booking (member_id, session_id, assessor_id, status) values (?, ?, ?, ?)",
                member, slot, assessor, "upcoming");
    }
}
