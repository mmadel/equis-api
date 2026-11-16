package com.equestrian.equis.repository.club;

import com.equestrian.equis.common.tenant.TenantContext;
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
import java.util.UUID;

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
// schema (ddl-auto=validate), as on the production server. E18 · the same upgrade creates the report views (V28; E19 · V29-V31). Only db/migration runs: no test grants, no test
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
    // The app's own connections: they work as equis_app in the club of TenantContext.
    @Autowired
    JdbcTemplate app;

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

    // E18 · the Trainers report view: created by the upgrade, run with the caller's rights (security_invoker), so the
    // trainer table's row-level security keeps each club's rows apart even for SQL that names no club.
    @Test
    void theTrainersReportViewKeepsEachClubsRowsApart() {
        assertThat(db.queryForObject("""
                select array_to_string(reloptions, ',') from pg_class
                where relname = 'report_trainer_v' and relkind = 'v'""", String.class))
                .contains("security_invoker=true");

        UUID other = db.queryForObject(
                "insert into club (code, name) values ('other-club', 'Other Club') returning id", UUID.class);
        try {
            db.update("""
                    insert into trainer (club_id, name, phone, email, status)
                    values (?, 'Other Trainer', '01099999999', 'other@other-club.eg', 'invited')""", other);
            List<String> releaseTwoTrainers = db.queryForList(
                    "select name from trainer where club_id = ?", String.class, Club.DEFAULT_ID);

            assertThat(TenantContext.runAs(Club.DEFAULT_ID,
                    () -> app.queryForList("select name from report_trainer_v", String.class)))
                    .isNotEmpty()
                    .containsExactlyInAnyOrderElementsOf(releaseTwoTrainers);
            assertThat(TenantContext.runAs(other,
                    () -> app.queryForList("select name from report_trainer_v", String.class)))
                    .containsExactly("Other Trainer");
            assertThat(TenantContext.runAs(other, () -> app.queryForObject("select current_user", String.class)))
                    .isEqualTo("equis_app");
        } finally {
            db.update("delete from trainer where club_id = ?", other);
            db.update("delete from club where id = ?", other);
        }
    }

    // E19 · the three money report views: created by the upgrade (V29-V31), run with the caller's rights
    // (security_invoker), so the row-level security of payment, session_usage, rider and trainer keeps each club's
    // rows apart even for SQL that names no club.
    @Test
    void theMoneyReportViewsKeepEachClubsRowsApart() {
        List<String> views = List.of("report_payment_v", "report_rider_v", "report_expired_package_v");
        for (String view : views) {
            assertThat(db.queryForObject("""
                    select array_to_string(reloptions, ',') from pg_class
                    where relname = ? and relkind = 'v'""", String.class, view))
                    .as(view).contains("security_invoker=true");
        }

        UUID other = db.queryForObject(
                "insert into club (code, name) values ('other-club', 'Other Club') returning id", UUID.class);
        try {
            UUID admin = db.queryForObject("""
                    insert into admin_account (club_id, name, email, password_hash)
                    values (?, 'Other Admin', 'admin@other-club.eg', 'x') returning id""", UUID.class, other);
            UUID rider = db.queryForObject("""
                    insert into rider (club_id, name, level_id) values (?, 'Other Rider', 1) returning id""",
                    UUID.class, other);
            // A package of September 2026, never used: listed by all three views.
            UUID payment = db.queryForObject("""
                    insert into payment (club_id, rider_id, amount_egp, method, start_month, months, sessions,
                                         valid_until, recorded_at, recorded_by)
                    values (?, ?, 2400, 'cash', date '2026-09-01', 1, 8, date '2026-10-31', now(), ?)
                    returning id""", UUID.class, other, rider, admin);

            assertThat(TenantContext.runAs(Club.DEFAULT_ID,
                    () -> app.queryForList("select id from report_rider_v", UUID.class)))
                    .as("the release 2 riders").isNotEmpty();
            assertThat(TenantContext.runAs(Club.DEFAULT_ID,
                    () -> app.queryForList("select id from report_payment_v", UUID.class)))
                    .as("the release 2 payments").isNotEmpty();
            for (String view : views) {
                List<UUID> releaseTwo = db.queryForList(
                        "select id from " + view + " where club_id = ?", UUID.class, Club.DEFAULT_ID);
                assertThat(TenantContext.runAs(Club.DEFAULT_ID,
                        () -> app.queryForList("select id from " + view, UUID.class)))
                        .as(view + " in the default club").containsExactlyInAnyOrderElementsOf(releaseTwo);
                assertThat(TenantContext.runAs(other, () -> app.queryForList("select id from " + view, UUID.class)))
                        .as(view + " in the other club")
                        .containsExactly(view.equals("report_rider_v") ? rider : payment);
            }
            assertThat(TenantContext.runAs(other, () -> app.queryForObject("select current_user", String.class)))
                    .isEqualTo("equis_app");
        } finally {
            db.update("delete from payment where club_id = ?", other);
            db.update("delete from rider where club_id = ?", other);
            db.update("delete from admin_account where club_id = ?", other);
            db.update("delete from club where id = ?", other);
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
