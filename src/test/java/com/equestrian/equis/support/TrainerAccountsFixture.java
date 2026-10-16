package com.equestrian.equis.support;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.jayway.jsonpath.JsonPath;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Plays both sides of the trainer-account journeys (flow W-E) over HTTP: the admin on W4, the trainer on the
// set-password page and in the app. Rows the API has no endpoint for (riders, sessions, bookings) go in with SQL.
// The test class gives the context a MutableClock (@TestBean) and @Import(TrainerAccountsFixture.Config.class).
public class TrainerAccountsFixture {
    public static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Every scenario starts on Friday 16 Oct 2026 at 10:00 school time.
    public static final Instant START = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, CAIRO).toInstant();
    public static final String TRAINERS = "/admin/trainers";
    public static final String ADMIN_EMAIL = "trainer-accounts-admin@example.com";
    public static final String ADMIN_PASSWORD = "Admin@2026";

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final RecordingNotificationSender recorder;
    private final JsonMapper json;
    private final MutableClock clock;
    private String adminToken;
    private Instant adminTokenExpiresAt = Instant.MIN;

    TrainerAccountsFixture(MockMvc mvc, JdbcTemplate jdbc, RecordingNotificationSender recorder, JsonMapper json,
                           MutableClock clock) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.recorder = recorder;
        this.json = json;
        this.clock = clock;
    }

    @TestConfiguration
    @Import(MailTestConfig.class)
    public static class Config {
        @Bean
        TrainerAccountsFixture trainerAccountsFixture(MockMvc mvc, JdbcTemplate jdbc,
                                                      RecordingNotificationSender recorder, JsonMapper json,
                                                      Clock clock) {
            return new TrainerAccountsFixture(mvc, jdbc, recorder, json, (MutableClock) clock);
        }

        // Hibernate fills created_at and updated_at from the same clock as the app, so the database checks
        // (accepted_at >= created_at, revoked_at >= created_at, ...) hold with a clock that is not the real time.
        @Bean
        HibernatePropertiesCustomizer hibernateClock(Clock clock) {
            return properties -> properties.put("hibernate.testing.clock", clock);
        }
    }

    // Stands in for APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH, so the admin can sign in through /auth/login.
    public static void adminAccount(DynamicPropertyRegistry registry) {
        String hash = new BCryptPasswordEncoder().encode(ADMIN_PASSWORD);
        registry.add("spring.flyway.placeholders.adminEmail", () -> ADMIN_EMAIL);
        registry.add("spring.flyway.placeholders.adminPasswordHash", () -> hash);
    }

    // For @BeforeEach: the start time, no emails, and no trainers (the seeded ones and sample riders go too).
    public void reset() {
        clock.set(START);
        recorder.clear();
        adminToken = null;
        adminTokenExpiresAt = Instant.MIN;
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
    }

    public void advance(Duration by) {
        clock.advance(by);
    }

    public RecordingNotificationSender recorder() {
        return recorder;
    }

    // Signs in again once the clock has passed the access token's expiry.
    public String adminToken() throws Exception {
        if (!clock.instant().isBefore(adminTokenExpiresAt)) {
            AuthTokens tokens = tokens(login(ADMIN_EMAIL, ADMIN_PASSWORD).andExpect(status().isOk()));
            adminToken = tokens.accessToken();
            adminTokenExpiresAt = clock.instant().plusSeconds(tokens.expiresIn());
        }
        return adminToken;
    }

    public MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken());
    }

    public MockHttpServletRequestBuilder asTrainer(MockHttpServletRequestBuilder request, AuthTokens tokens) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken());
    }

    public ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(asAdmin(request));
    }

    public ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request);
    }

    // POST /admin/trainers, whatever the answer.
    public ResultActions inviteRequest(String name, String phone, String email) throws Exception {
        return admin(post(TRAINERS).contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "%s", "phone": "%s", "email": "%s" }""".formatted(name, phone, email)));
    }

    public TrainerRow invite(String name, String phone, String email) throws Exception {
        return row(inviteRequest(name, phone, email).andExpect(status().isCreated()));
    }

    // Invited and then set their password through the emailed link. The two emails are cleared.
    public TrainerRow activeTrainer(String name, String phone, String email, String password) throws Exception {
        TrainerRow row = invite(name, phone, email);
        acceptInvite(tokenFromLastEmail(), password).andExpect(status().isNoContent());
        recorder.clear();
        return row;
    }

    public String tokenFromLastEmail() {
        return RecordingNotificationSender.tokenFrom(recorder.last());
    }

    // What the set-password page sends.
    public ResultActions acceptInvite(String token, String password) throws Exception {
        return mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"token\": \"%s\", \"password\": \"%s\" }".formatted(token, password)));
    }

    public ResultActions login(String login, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(login, password)));
    }

    public AuthTokens loginTrainer(String login, String password) throws Exception {
        return tokens(login(login, password).andExpect(status().isOk()));
    }

    public ResultActions refresh(AuthTokens tokens) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refreshToken\": \"%s\" }".formatted(tokens.refreshToken())));
    }

    // The trainer app's first screen.
    public ResultActions myLevels(AuthTokens tokens) throws Exception {
        return mvc.perform(asTrainer(get("/me/levels"), tokens));
    }

    // GET /admin/trainers, the W4 list.
    public ResultActions list() throws Exception {
        return admin(get(TRAINERS)).andExpect(status().isOk());
    }

    public static String rowPath(UUID trainerId) {
        return "$.trainers[?(@.id == '" + trainerId + "')]";
    }

    public long total(String name) throws Exception {
        Number n = JsonPath.read(list().andReturn().getResponse().getContentAsString(), "$.totals." + name);
        return n.longValue();
    }

    public List<UUID> addRiders(UUID trainerId, int levelId, int n) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            ids.add(jdbc.queryForObject("insert into rider (name, level_id, trainer_id) values (?, ?, ?) returning id",
                    UUID.class, "Rider " + i + " of " + trainerId.toString().substring(0, 8), levelId, trainerId));
        }
        return ids;
    }


    public void assign(UUID sessionId, List<UUID> riderIds, UUID assignedBy) {
        for (UUID riderId : riderIds) {
            jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                    sessionId, riderId, assignedBy);
        }
    }

    // A walk-in booked into tomorrow's assessment slot with this trainer as assessor.
    public UUID addUpcomingBooking(UUID assessorId) {
        UUID slot = addUpcomingSession(null);
        UUID member = jdbc.queryForObject("insert into member (name) values ('Walk-in') returning id", UUID.class);
        return jdbc.queryForObject(
                "insert into assessment_booking (member_id, session_id, assessor_id) values (?, ?, ?) returning id",
                UUID.class, member, slot, assessorId);
    }

    public int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    public TrainerStatus statusOf(UUID trainerId) {
        String status = jdbc.queryForObject("select status from trainer where id = ?", String.class, trainerId);
        return TrainerStatus.valueOf(status.toUpperCase());
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    // Tomorrow (Saturday) at 10:00 school time. A null level makes it an assessment slot.
    public UUID addUpcomingSession(Integer levelId) {
        LocalDate tomorrow = LocalDate.ofInstant(clock.instant(), CAIRO).plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        UUID template = jdbc.queryForObject("""
                        insert into session_template (level_id, day_of_week, start_time, end_time, capacity, valid_from)
                        values (?, ?, ?, ?, 6, ?) returning id""",
                UUID.class, levelId, tomorrow.getDayOfWeek().getValue(), start, start.plusHours(1), tomorrow);
        return jdbc.queryForObject("""
                        insert into session (template_id, session_date, start_time, end_time, capacity)
                        values (?, ?, ?, ?, 6) returning id""",
                UUID.class, template, tomorrow, start, start.plusHours(1));
    }

    private TrainerRow row(ResultActions result) throws Exception {
        return json.readValue(result.andReturn().getResponse().getContentAsString(), TrainerRow.class);
    }

    private AuthTokens tokens(ResultActions result) throws Exception {
        return json.readValue(result.andReturn().getResponse().getContentAsString(), AuthTokens.class);
    }
}
