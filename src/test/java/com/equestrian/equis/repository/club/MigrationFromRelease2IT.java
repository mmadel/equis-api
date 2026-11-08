package com.equestrian.equis.repository.club;

import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.service.platform.AccessReason;
import com.equestrian.equis.service.platform.AccessState;
import com.equestrian.equis.service.platform.ClubAccess;
import com.equestrian.equis.service.platform.ClubAccessService;
import com.equestrian.equis.support.TestPostgres;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.equestrian.equis.support.TrainerAccountsFixture.ADMIN_EMAIL;
import static com.equestrian.equis.support.TrainerAccountsFixture.ADMIN_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · upgrading a release 2 database to 3.0.0. db/r2-sample.sql is a pg_dump of a database migrated with the
// 2.0.2 migrations (V1-V22, with their Flyway history) plus a row or more in every school table. It is restored into
// a database of its own before the app starts; the app's own Flyway then runs V23 onwards and Hibernate checks the
// schema (ddl-auto=validate), as on the production server. Only db/migration runs: no test grants, no test
// subscription, so plan and subscription stay as a real upgrade leaves them.
@SpringBootTest(properties = {
        "app.sessions.generate-cron=-",
        "spring.flyway.locations=classpath:db/migration"})
@AutoConfigureMockMvc
class MigrationFromRelease2IT {
    private static final Path DUMP = Path.of("src/test/resources/db/r2-sample.sql");

    static final TestPostgres postgres = TestPostgres.newDatabase();
    // The ids of every row of the release 2 database, per table, before the app migrates it.
    static final Map<String, Set<String>> RELEASE_2_ROWS = restoreRelease2();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    Environment environment;
    @Autowired
    ClubAccessService clubAccess;

    // As the database owner, so row-level security never hides a row from the checks.
    private final JdbcTemplate db = owner();

    @Test
    void theAppStartsOnTheUpgradedDatabaseWithTheSchemaCheckOn() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(db.queryForObject("select max(version::int) from flyway_schema_history where success",
                Integer.class)).isGreaterThan(22);
        assertThat(db.queryForObject("select count(*) from flyway_schema_history where not success", Integer.class))
                .isZero();
    }

    @Test
    void everyReleaseTwoRowIsKeptAndBelongsToTheDefaultClub() {
        assertThat(db.queryForList("select id, code, name from club"))
                .singleElement()
                .satisfies(club -> {
                    assertThat(club.get("id")).isEqualTo(Club.DEFAULT_ID);
                    assertThat(club.get("code")).isEqualTo("school");
                    assertThat(club.get("name")).isEqualTo("Riding School");
                });
        List<String> withClub = db.queryForList("""
                select table_name from information_schema.columns
                where table_schema = 'public' and column_name = 'club_id' order by table_name""", String.class);
        assertThat(withClub).contains("admin_account", "trainer", "rider", "member", "session", "payment",
                "session_evaluation");
        for (String table : withClub) {
            assertThat(db.queryForObject("select count(*) from " + table + " where club_id is distinct from ?",
                    Long.class, Club.DEFAULT_ID)).as(table + " rows outside the default club").isZero();
        }
        // Nothing was lost (signing in adds refresh tokens, so later rows may come on top).
        for (Map.Entry<String, Set<String>> table : RELEASE_2_ROWS.entrySet()) {
            assertThat(ids(db, table.getKey())).as(table.getKey() + " rows").containsAll(table.getValue());
        }
        // The sample really has a row in every table, so the checks above cover each of them.
        assertThat(RELEASE_2_ROWS).allSatisfy((table, rows) -> assertThat(rows).as(table).isNotEmpty());
    }

    @Test
    void noPlanOrSubscriptionExistsSoTheDefaultClubIsBlockedUntilOneIsAdded() throws Exception {
        assertThat(db.queryForObject("select count(*) from plan", Long.class)).isZero();
        assertThat(db.queryForObject("select count(*) from subscription", Long.class)).isZero();
        ClubAccess access = clubAccess.forClub(Club.DEFAULT_ID);
        assertThat(access.state()).isEqualTo(AccessState.BLOCKED);
        assertThat(access.reason()).isEqualTo(AccessReason.NO_SUBSCRIPTION);
        login("school", ADMIN_EMAIL)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("club_blocked"))
                .andExpect(jsonPath("$.reason").value("NO_SUBSCRIPTION"));

        // The release checklist step: a paid subscription for the default club. Then its people sign in again and
        // see their release 2 data.
        db.update("insert into plan (code, name, on_expiry) values ('BASIC', 'Basic', 'READ_ONLY')");
        Instant now = Instant.now();
        db.update("""
                insert into subscription (club_id, plan_id, billing_period, starts_at, ends_at)
                select ?, id, 'ANNUAL', ?, ? from plan where code = 'BASIC'""",
                Club.DEFAULT_ID, Timestamp.from(now.minus(Duration.ofDays(1))),
                Timestamp.from(now.plus(Duration.ofDays(365))));
        try {
            String admin = token(login("school", ADMIN_EMAIL).andExpect(status().isOk()));
            mvc.perform(get("/admin/riders").header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.riders[?(@.name == 'Sara Ali')]").exists());
            // Mona signs in with her phone, as every trainer can since 3.0.0.
            String mona = token(login("school", "01022222223").andExpect(status().isOk()));
            mvc.perform(get("/me/riders").header("Authorization", "Bearer " + mona))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name").value(contains("Sara Ali")));
        } finally {
            db.update("delete from subscription");
            db.update("delete from plan");
        }
    }

    // ---- helpers

    private ResultActions login(String clubCode, String login) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"clubCode\": \"%s\", \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(clubCode, login, ADMIN_PASSWORD)));
    }

    private static String token(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private static JdbcTemplate owner() {
        return new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrlSeeingAllClubs(), postgres.getUsername(), postgres.getPassword()));
    }

    private static Set<String> ids(JdbcTemplate db, String table) {
        return new HashSet<>(db.queryForList("select id::text from public." + table, String.class));
    }

    // Restores the dump and reads each table's ids, before Spring (and so Flyway) starts.
    private static Map<String, Set<String>> restoreRelease2() {
        JdbcTemplate db = owner();
        try {
            db.execute(Files.readString(DUMP));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(db.queryForObject("select max(version::int) from flyway_schema_history", Integer.class))
                .as("the dump is a release 2 database").isEqualTo(22);
        Map<String, Set<String>> rows = new LinkedHashMap<>();
        for (String table : db.queryForList("""
                select tablename from pg_tables
                where schemaname = 'public' and tablename <> 'flyway_schema_history' order by tablename""",
                String.class)) {
            rows.put(table, ids(db, table));
        }
        return rows;
    }
}
