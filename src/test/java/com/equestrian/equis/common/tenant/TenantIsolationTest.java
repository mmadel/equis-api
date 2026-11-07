package com.equestrian.equis.common.tenant;

import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.service.accounts.TokenGenerator;
import com.equestrian.equis.service.sessions.SessionGenerationJob;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.jayway.jsonpath.JsonPath;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · tenant context, @TenantId and row-level security, cases 3–13, over the real app and database.
// Two clubs: the default club "school" (coach Ahmed, rider Sara) and "cairo-eq" (coach Mona, rider Laila), both
// written with SQL in root mode. The clock is Friday 16 Oct 2026, 10:00 in Cairo; each club has a Level 1 session
// at 17:00 that day. The full isolation suite over every endpoint is the next ticket.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import(TrainerAccountsFixture.Config.class)
class TenantIsolationTest {
    private static final UUID SCHOOL = Club.DEFAULT_ID;
    private static final String PASSWORD = "Secret@2026";
    private static final String LAILA_LINK = "laila-invite-link-for-the-isolation-test-0000000000000";

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
        TrainerAccountsFixture.adminAccount(registry);
    }

    @TestBean
    Clock clock;

    static Clock clock() {
        return new MutableClock(TrainerAccountsFixture.START);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    TokenGenerator tokenGenerator;
    @Autowired
    MemberRepository members;
    @Autowired
    SessionGenerationJob generationJob;
    @Autowired
    FlywayProperties flywayProperties;

    UUID cairo;
    UUID ahmed;
    UUID mona;
    UUID sara;
    UUID laila;
    UUID cairoSession;

    @BeforeEach
    void twoClubs() {
        TenantContext.runAsRoot(() -> {
            jdbc.execute("truncate table club, trainer, admin_account, refresh_token, member, session_template cascade");
            jdbc.update("INSERT INTO club (id, code, name) VALUES (?, 'school', 'Riding School')", SCHOOL);
            cairo = jdbc.queryForObject(
                    "INSERT INTO club (code, name) VALUES ('cairo-eq', 'Cairo Equestrian') RETURNING id", UUID.class);
            ahmed = trainer(SCHOOL, "Ahmed Said", "01055550001", "ahmed@school.eg");
            mona = trainer(cairo, "Mona Kamal", "01055550002", "mona@cairo.eg");
            sara = rider(SCHOOL, "Sara Ali", ahmed, "sara@school.eg", "none");
            laila = rider(cairo, "Laila Fathy", mona, "laila@cairo.eg", "invited");
            jdbc.update("""
                    INSERT INTO invite (club_id, rider_id, token_hash, purpose, expires_at, created_at)
                    VALUES (?, ?, ?, 'invite', TIMESTAMPTZ '2099-12-31 00:00:00+00', TIMESTAMPTZ '2026-10-16 06:00:00+00')""",
                    cairo, laila, tokenGenerator.hash(LAILA_LINK));
            cairoSession = levelOneSessionToday(cairo);
            levelOneSessionToday(SCHOOL);
            // Laila needs sessions left to be seated: a cairo-eq admin records October.
            UUID cairoAdmin = jdbc.queryForObject("""
                    INSERT INTO admin_account (club_id, email, password_hash) VALUES (?, 'boss@cairo.eg', 'x')
                    RETURNING id""", UUID.class, cairo);
            jdbc.update("""
                    INSERT INTO payment (club_id, rider_id, amount_egp, method, start_month, months, sessions,
                                         valid_until, recorded_by)
                    VALUES (?, ?, 1200, 'cash', DATE '2026-10-01', 1, 8, DATE '2026-11-30', ?)""",
                    cairo, laila, cairoAdmin);
            return null;
        });
    }

    // Case 3
    @Test
    void ahmedSeesOnlyHisClubsRider() throws Exception {
        String body = myRiders(login("ahmed@school.eg"));

        assertThat(body).contains(sara.toString()).doesNotContain(laila.toString());
    }

    // Case 4
    @Test
    void monaSeesOnlyHerClubsRider() throws Exception {
        String body = myRiders(login("mona@cairo.eg"));

        assertThat(body).contains(laila.toString()).doesNotContain(sara.toString());
    }

    // Case 5: no code passes the club; Hibernate takes it from the request.
    @Test
    void monasAssignmentIsSavedInHerClub() throws Exception {
        String token = login("mona@cairo.eg");

        mvc.perform(put("/me/sessions/" + cairoSession + "/assignments").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"riderIds\": [\"" + laila + "\"] }"))
                .andExpect(status().isOk());

        assertThat(asRoot(() -> jdbc.queryForObject(
                "SELECT club_id FROM assignment WHERE rider_id = ?", UUID.class, laila))).isEqualTo(cairo);
    }

    // Case 5: and another club's session is not found at all.
    @Test
    void ahmedCannotReachTheOtherClubsSession() throws Exception {
        String token = login("ahmed@school.eg");

        mvc.perform(get("/me/sessions/" + cairoSession).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // Case 6: row-level security on a native query.
    @Test
    void aNativeCountSeesTheClubsRowsNoneWithoutAClubAndAllInRoot() {
        String count = "SELECT count(*) FROM rider";
        int inSchool = TenantContext.runAs(SCHOOL, () -> jdbc.queryForObject(count, Integer.class));
        int inCairo = TenantContext.runAs(cairo, () -> jdbc.queryForObject(count, Integer.class));
        int noClub = TenantContext.runAs(null, () -> jdbc.queryForObject(count, Integer.class));
        int root = asRoot(() -> jdbc.queryForObject(count, Integer.class));

        assertThat(inSchool).isEqualTo(1);
        assertThat(inCairo).isEqualTo(1);
        assertThat(noClub).isZero();
        assertThat(root).isEqualTo(2);
    }

    // Case 7
    @Test
    void aNativeInsertIntoAnotherClubIsRefused() {
        assertThatThrownBy(() -> TenantContext.runAs(SCHOOL, () -> jdbc.update(
                "INSERT INTO rider (club_id, name, level_id) VALUES (?, 'Intruder', 1)", cairo)))
                .isInstanceOf(DataAccessException.class)
                .rootCause().hasMessageContaining("new row violates row-level security policy for table \"rider\"");
    }

    // Case 7: and a native insert without club_id lands in the current club (the V24 default);
    // with no club the row is refused.
    @Test
    void aNativeInsertWithoutAClubTakesTheCurrentClubAndFailsWithNone() {
        UUID id = TenantContext.runAs(cairo, () -> jdbc.queryForObject(
                "INSERT INTO rider (name, level_id) VALUES ('Nour', 1) RETURNING id", UUID.class));
        assertThat(asRoot(() -> jdbc.queryForObject("SELECT club_id FROM rider WHERE id = ?", UUID.class, id)))
                .isEqualTo(cairo);

        assertThatThrownBy(() -> TenantContext.runAs(null, () -> jdbc.update(
                "INSERT INTO rider (name, level_id) VALUES ('Nobody', 1)")))
                .isInstanceOf(DataAccessException.class)
                .rootCause().hasMessageContaining("row-level security");
    }

    // Case 8
    @Test
    void savingASchoolRowInRootModeIsRefused() {
        assertThatThrownBy(() -> asRoot(() ->
                members.saveAndFlush(Member.register("Root Member", "01077770000", "root@nowhere.eg"))))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("No club for tenant write: Member");
    }

    // Case 9
    @Test
    void ahmedsTokensCarryHisClubThroughARefresh() throws Exception {
        ResultActions signIn = loginRequest("ahmed@school.eg").andExpect(status().isOk());
        String access = JsonPath.read(body(signIn), "$.accessToken");
        String refresh = JsonPath.read(body(signIn), "$.refreshToken");

        assertThat(cidOf(access)).isEqualTo(SCHOOL.toString());
        assertThat(asRoot(() -> jdbc.queryForObject(
                "SELECT club_id FROM refresh_token WHERE subject_id = ? AND revoked_at IS NULL", UUID.class, ahmed)))
                .isEqualTo(SCHOOL);

        ResultActions refreshed = mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refreshToken\": \"" + refresh + "\" }")).andExpect(status().isOk());

        assertThat(cidOf(JsonPath.read(body(refreshed), "$.accessToken"))).isEqualTo(SCHOOL.toString());
    }

    // Case 10
    @Test
    void lailaAcceptsHerInviteAndSignsInToHerClub() throws Exception {
        mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"" + LAILA_LINK + "\", \"password\": \"" + PASSWORD + "\" }"))
                .andExpect(status().isNoContent());

        String access = login("laila@cairo.eg");

        assertThat(cidOf(access)).isEqualTo(cairo.toString());
        assertThat(asRoot(() -> jdbc.queryForObject(
                "SELECT account_status FROM rider WHERE id = ?", String.class, laila))).isEqualTo("active");
    }

    // Case 11
    @Test
    void theSessionGeneratorFillsEveryClubWithItsOwnClub() {
        asRoot(() -> jdbc.update("DELETE FROM session"));

        generationJob.generateOnSchedule();

        List<Map<String, Object>> perClub = asRoot(() -> jdbc.queryForList("""
                SELECT s.club_id, t.club_id AS template_club, count(*) AS n
                FROM session s JOIN session_template t ON t.id = s.template_id
                GROUP BY s.club_id, t.club_id"""));
        assertThat(perClub).hasSize(2).allSatisfy(row -> {
            assertThat(row.get("club_id")).isEqualTo(row.get("template_club"));
            assertThat((Long) row.get("n")).isPositive();
        });
        assertThat(perClub).extracting(row -> row.get("club_id")).containsExactlyInAnyOrder(SCHOOL, cairo);
    }

    // Case 12: the pool hands the same connections out again; each checkout sets the club afresh.
    @Test
    void aPooledConnectionDoesNotKeepThePreviousClub() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(TenantContext.runAs(cairo, () -> jdbc.queryForObject("SELECT count(*) FROM rider", Integer.class)))
                    .isEqualTo(1);
            assertThat(asRoot(() -> jdbc.queryForObject("SELECT count(*) FROM rider", Integer.class))).isEqualTo(2);
            assertThat(TenantContext.runAs(null, () -> jdbc.queryForObject("SELECT count(*) FROM rider", Integer.class)))
                    .isZero();
        }
        // And a request with no user after Mona's request: the rider tables stay hidden.
        myRiders(login("mona@cairo.eg"));
        mvc.perform(get("/me/riders")).andExpect(status().isUnauthorized());
        assertThat(TenantContext.runAs(null, () -> jdbc.queryForObject("SELECT count(*) FROM rider", Integer.class)))
                .isZero();
    }

    // Case 13: a data migration sees every club's rows, with the app's own Flyway setting.
    @Test
    void aDataMigrationSeesEveryClubsRows() {
        TestPostgres database = TestPostgres.newDatabase();
        DriverManagerDataSource plain = new DriverManagerDataSource(
                database.getJdbcUrl(), database.getUsername(), database.getPassword());
        flyway(plain, "24", "classpath:db/migration").migrate();
        JdbcTemplate seeingAll = new JdbcTemplate(new DriverManagerDataSource(
                database.getJdbcUrlSeeingAllClubs(), database.getUsername(), database.getPassword()));
        UUID other = seeingAll.queryForObject(
                "INSERT INTO club (code, name) VALUES ('other', 'Other') RETURNING id", UUID.class);
        seeingAll.update("INSERT INTO rider (club_id, name, level_id) VALUES (?, 'Other Rider', 1)", other);
        int riders = seeingAll.queryForObject("SELECT count(*) FROM rider", Integer.class);

        flyway(plain, "latest", "classpath:db/migration", "classpath:db/testmigration").migrate();

        assertThat(seeingAll.queryForObject("SELECT seen FROM testmigration_rider_count", Integer.class))
                .isEqualTo(riders);
        // As the app's role and without the bypass, the same count sees nothing: the setting is what makes it work.
        SingleConnectionDataSource asApp = new SingleConnectionDataSource(
                database.getJdbcUrl(), database.getUsername(), database.getPassword(), true);
        try {
            JdbcTemplate app = new JdbcTemplate(asApp);
            app.execute("SET ROLE equis_app");
            assertThat(app.queryForObject("SELECT count(*) FROM rider", Integer.class)).isZero();
        } finally {
            asApp.destroy();
        }
    }

    // ---- helpers

    private Flyway flyway(DriverManagerDataSource dataSource, String target, String... locations) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .target(target)
                // The app's own setting, joined the way Spring Boot passes it to Flyway.
                .initSql(String.join("\n", flywayProperties.getInitSqls()))
                .placeholders(Map.of("adminEmail", "admin@example.com", "adminPasswordHash", "not-a-real-hash",
                        "defaultClubCode", "school", "defaultClubName", "Riding School"))
                .load();
    }

    private UUID trainer(UUID club, String name, String phone, String email) {
        return jdbc.queryForObject("""
                INSERT INTO trainer (club_id, name, phone, email, password_hash, status)
                VALUES (?, ?, ?, ?, ?, 'active') RETURNING id""",
                UUID.class, club, name, phone, email, passwordEncoder.encode(PASSWORD));
    }

    private UUID rider(UUID club, String name, UUID trainer, String email, String accountStatus) {
        return jdbc.queryForObject("""
                INSERT INTO rider (club_id, name, level_id, trainer_id, email, account_status)
                VALUES (?, ?, 1, ?, ?, ?) RETURNING id""",
                UUID.class, club, name, trainer, email, accountStatus);
    }

    // Friday 16 Oct 2026, 17:00–18:00, Level 1.
    private UUID levelOneSessionToday(UUID club) {
        UUID template = jdbc.queryForObject("""
                INSERT INTO session_template (club_id, level_id, day_of_week, start_time, end_time, capacity, valid_from)
                VALUES (?, 1, 5, '17:00', '18:00', 6, DATE '2026-10-01') RETURNING id""", UUID.class, club);
        return jdbc.queryForObject("""
                INSERT INTO session (club_id, template_id, session_date, start_time, end_time, capacity)
                VALUES (?, ?, DATE '2026-10-16', '17:00', '18:00', 6) RETURNING id""", UUID.class, club, template);
    }

    private ResultActions loginRequest(String email) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"login\": \"" + email + "\", \"password\": \"" + PASSWORD + "\", \"rememberMe\": false }"));
    }

    private String login(String email) throws Exception {
        return JsonPath.read(body(loginRequest(email).andExpect(status().isOk())), "$.accessToken");
    }

    private String myRiders(String token) throws Exception {
        return body(mvc.perform(get("/me/riders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()));
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String cidOf(String jwt) {
        String payload = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        return JsonPath.read(payload, "$.cid");
    }

    private static <T> T asRoot(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
