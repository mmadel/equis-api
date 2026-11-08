package com.equestrian.equis;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.PostgresContainer;
import com.equestrian.equis.support.RecordingNotificationSender;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · the epic's Demo, step by step through the API. The super admin and the four plans come from the real ops
// scripts; everything else is a request, except the 49 people already at Cairo EQ before the limit step and Nadia's
// rider, which are written with SQL so the demo stays short.
// Sat 7 Nov 2026, 15:00 Cairo: the super admin creates Cairo EQ (Hany) and Giza Riders (Sara), both on a 30-day
// trial; Hany invites the trainer Nadia, who signs in with the club code and her phone and sees only Cairo EQ riders.
// Mon 9 Nov 2026: Cairo EQ moves to Basic annual and its intake stops at 50. Wed 10 Nov 2027: Basic has ended, so
// Cairo EQ is read-only, and Giza Riders, still on its expired trial, is blocked.
class E17DemoIT extends IntegrationTestBase {
    private static final Instant NOV_7_2026 = ZonedDateTime.of(2026, 11, 7, 15, 0, 0, 0, CAIRO).toInstant();
    private static final Instant NOV_9_2026 = ZonedDateTime.of(2026, 11, 9, 15, 0, 0, 0, CAIRO).toInstant();
    private static final Instant NOV_10_2027 = ZonedDateTime.of(2027, 11, 10, 10, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 15);
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    private static final String PASSWORD = "Demo@2026";
    private static final String HANY = "hany@cairoeq.com";
    private static final String SARA = "sara@gizariders.com";
    private static final String NADIA_PHONE = "01052000001";

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    RecordingNotificationSender recorder;

    private int nextPerson;

    @BeforeEach
    void plansAndSuperAdminFromTheOpsScripts() throws Exception {
        CLOCK.set(NOV_7_2026);
        recorder.clear();
        upsertPlan("TRIAL", "Trial", "NULL", "NULL", "30", "BLOCK");
        upsertPlan("BASIC", "Basic", "2", "50", "NULL", "READ_ONLY");
        upsertPlan("PRO", "Pro", "10", "300", "NULL", "READ_ONLY");
        upsertPlan("MAX", "Max", "NULL", "NULL", "NULL", "READ_ONLY");
        createSuperAdmin();
    }

    @Test
    void theEpicDemo() throws Exception {
        // 1. The super admin signs in to /platform and creates Cairo EQ on a 30-day trial; Hany gets a link.
        String platform = signInToPlatform();
        String created = body(createClub(platform, "Cairo EQ", "cairo-eq", "Hany", HANY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planCode").value("TRIAL"))
                .andExpect(jsonPath("$.daysLeft").value(30))
                .andExpect(jsonPath("$.accessState").value("ACTIVE")));
        UUID cairoEq = UUID.fromString(JsonPath.read(created, "$.id"));
        acceptLink().andExpect(jsonPath("$.clubCode").value("cairo-eq"));
        String hany = token(login("cairo-eq", HANY).andExpect(status().isOk()));

        // A second club on its own trial, used at the end.
        createClub(platform, "Giza Riders", "giza-riders", "Sara", SARA).andExpect(status().isCreated());
        acceptLink();
        login("giza-riders", SARA).andExpect(status().isOk());

        // 2. A trainer at Cairo EQ signs in with club code + phone and sees only Cairo EQ riders.
        perform(hany, post("/admin/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "Nadia Farouk", "phone": "%s", "email": "nadia@cairoeq.com" }""".formatted(NADIA_PHONE)))
                .andExpect(status().isCreated());
        acceptLink();
        String nadia = token(login("cairo-eq", NADIA_PHONE).andExpect(status().isOk()));
        UUID nadiaId = inClub(cairoEq, () -> jdbc.queryForObject(
                "select id from trainer where phone = ?", UUID.class, NADIA_PHONE));
        inClub(cairoEq, () -> jdbc.update(
                "insert into rider (club_id, name, level_id, trainer_id) values (?, 'Yara Cairo', 1, ?)",
                cairoEq, nadiaId));
        perform(nadia, get("/me/riders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Yara Cairo")));
        // The default club's seed riders exist, but Hany's list has Cairo EQ's one rider only.
        perform(hany, get("/admin/riders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[*].name").value(contains("Yara Cairo")));

        // 3. Two days later the super admin adds a Basic annual subscription, starting now; at 50 riders and
        // applicants intake is refused. Access tokens last 15 minutes, so both sign in again.
        CLOCK.set(NOV_9_2026);
        platform = signInToPlatform();
        hany = token(login("cairo-eq", HANY).andExpect(status().isOk()));
        perform(platform, post("/platform/clubs/" + cairoEq + "/subscriptions").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "planCode": "BASIC", "billingPeriod": "ANNUAL", "startMode": "NOW", "note": "Paid by bank" }"""))
                .andExpect(status().isCreated());
        UUID slot = assessmentSlot(hany, cairoEq);
        inClub(cairoEq, () -> jdbc.update("""
                insert into member (club_id, name, phone, email)
                select ?, 'Person ' || g, '0122' || lpad(g::text, 7, '0'), 'person' || g || '@cairoeq.com'
                from generate_series(1, 49) g""", cairoEq));
        perform(hany, intake(slot, nadiaId)).andExpect(status().isCreated());
        perform(hany, get("/admin/club/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("BASIC"))
                .andExpect(jsonPath("$.riders.used").value(50))
                .andExpect(jsonPath("$.riders.limit").value(50));
        perform(hany, intake(slot, nadiaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan_limit_reached"))
                .andExpect(jsonPath("$.resource").value("RIDERS"))
                .andExpect(jsonPath("$.limit").value(50));

        // 4. Past the end date everyone can still sign in and view, but Finished and intake are refused.
        CLOCK.set(NOV_10_2027);
        hany = token(login("cairo-eq", HANY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access.state").value("READ_ONLY"))
                .andExpect(jsonPath("$.access.reason").value("SUBSCRIPTION_ENDED")));
        nadia = token(login("cairo-eq", NADIA_PHONE).andExpect(status().isOk()));
        perform(nadia, get("/me/riders")).andExpect(status().isOk());
        perform(hany, get("/admin/riders")).andExpect(status().isOk());
        perform(nadia, post("/me/sessions/" + slot + "/finish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("subscription_expired"));
        perform(hany, intake(slot, nadiaId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("subscription_expired"));

        // 5. The second club, still on its expired trial, cannot sign in at all.
        login("giza-riders", SARA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("club_blocked"))
                .andExpect(jsonPath("$.reason").value("TRIAL_ENDED"));
    }

    // ---- helpers

    private String signInToPlatform() throws Exception {
        return token(mvc.perform(post("/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk()));
    }

    private ResultActions createClub(String platform, String name, String code, String adminName, String adminEmail)
            throws Exception {
        recorder.clear();
        return perform(platform, post("/platform/clubs").contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "%s", "code": "%s", "admin": { "name": "%s", "email": "%s" } }"""
                .formatted(name, code, adminName, adminEmail)));
    }

    // The set-password link of the last email.
    private ResultActions acceptLink() throws Exception {
        String link = RecordingNotificationSender.tokenFrom(recorder.last());
        recorder.clear();
        return mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"%s\", \"password\": \"%s\" }".formatted(link, PASSWORD)))
                .andExpect(status().isOk());
    }

    private ResultActions login(String clubCode, String login) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"clubCode\": \"%s\", \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(clubCode, login, PASSWORD)));
    }

    // Hany's Sunday assessment slot: a template, and the session the app generates for it.
    private UUID assessmentSlot(String hany, UUID cairoEq) throws Exception {
        String saved = body(perform(hany, post("/admin/session-templates").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "levelId": null, "dayOfWeek": "SUNDAY", "start": "10:00", "end": "11:00", "capacity": 4, "validFrom": "%s" }"""
                        .formatted(SUNDAY)))
                .andExpect(status().isCreated()));
        UUID template = UUID.fromString(JsonPath.read(saved, "$.template.id"));
        return inClub(cairoEq, () -> jdbc.queryForObject(
                "select id from session where template_id = ? and session_date = ?", UUID.class, template, SUNDAY));
    }

    // W2's New person, booked into the slot with Nadia. Each call gets its own phone and email.
    private MockHttpServletRequestBuilder intake(UUID slot, UUID assessor) {
        int n = ++nextPerson;
        return post("/admin/members").contentType(MediaType.APPLICATION_JSON).content("""
                { "name": "New Person %d", "phone": "0109%07d", "email": "new%d@mail.eg", "slotId": "%s", "assessorId": "%s" }"""
                .formatted(n, n, n, slot, assessor));
    }

    private ResultActions perform(String token, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private static String token(ResultActions result) throws Exception {
        return JsonPath.read(body(result), "$.accessToken");
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private static <T> T inClub(UUID club, Supplier<T> work) {
        return TenantContext.runAs(club, work);
    }

    // ops/upsert-plan.sql with its psql variables filled in, as psql -v would.
    private void upsertPlan(String code, String name, String trainers, String riders, String trialDays,
                            String onExpiry) throws Exception {
        jdbc.execute(Files.readString(Path.of("ops", "upsert-plan.sql"))
                .replace(":'code'", literal(code))
                .replace(":'name'", literal(name))
                .replace(":'on_expiry'", literal(onExpiry))
                .replace(":trainers", trainers)
                .replace(":riders", riders)
                .replace(":trial_days", trialDays));
    }

    // ops/create-super-admin.sql, as the database owner (CREATE EXTENSION).
    private static void createSuperAdmin() throws Exception {
        String sql = Files.readString(Path.of("ops", "create-super-admin.sql"))
                .replace(":'email'", literal(SUPER_EMAIL))
                .replace(":'pwd'", literal(SUPER_PASSWORD));
        try (Connection connection = PostgresContainer.superuser();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
