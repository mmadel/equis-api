package com.equestrian.equis.repository.accounts;

import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.EquisApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// E10: the prod-only Flyway callback db/prod/afterMigrate__remove_demo_data.sql.
// Each test starts the real app on its own fresh database, so restarts can be checked.
// The APP_* and SPRING_DATASOURCE_* values below are test stand-ins for the prod environment check.
class ProdDemoDataCleanupTest {
    private static final String TRAINER_ONE_ID = "00000000-0000-0000-0000-000000000001";
    private static final String TRAINER_TWO_ID = "00000000-0000-0000-0000-000000000002";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD_HASH = new BCryptPasswordEncoder().encode("Admin@123");
    private static final List<String> TABLES = List.of(
            "level", "trainer", "invite", "admin_account", "refresh_token", "session_template", "session",
            "rider", "assignment", "member", "assessment_booking", "level_change", "notice");
    private static final AtomicInteger DATABASES = new AtomicInteger();

    static TestPostgres postgres = TestPostgres.newDatabase();

    // Case 1
    @Test
    void prodOnAnEmptyDatabaseKeepsLevelsAndAdminAndRemovesAllDemoData() {
        String url = newDatabase();
        start("prod", url).close();
        JdbcTemplate db = jdbc(url);

        assertThat(db.queryForList("SELECT id FROM level ORDER BY id", Integer.class)).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(db.queryForList("SELECT email FROM admin_account", String.class)).containsExactly(ADMIN_EMAIL);
        assertThat(count(db, "trainer")).isZero();
        assertThat(count(db, "invite")).isZero();
        assertThat(count(db, "rider")).isZero();
        assertThat(count(db, "session_template")).isZero();
        assertThat(count(db, "session")).isZero();
    }

    // Case 2
    @Test
    void restartingWithProdStartsCleanlyAndDeletesNothing() {
        String url = newDatabase();
        start("prod", url).close();
        Map<String, Long> before = counts(jdbc(url));

        start("prod", url).close();

        assertThat(counts(jdbc(url))).isEqualTo(before);
    }

    // Case 3
    @Test
    void realDataCreatedAfterTheFirstStartSurvivesARestart() {
        String url = newDatabase();
        start("prod", url).close();
        JdbcTemplate db = jdbc(url);
        UUID trainerId = UUID.randomUUID();
        db.update("INSERT INTO trainer (id, name, phone, email, status) VALUES (?, 'Real Trainer', '01011111111', 'real@example.com', 'invited')",
                trainerId);
        UUID riderId = UUID.randomUUID();
        db.update("INSERT INTO rider (id, name, level_id, trainer_id) VALUES (?, 'Real Rider', 1, ?)", riderId, trainerId);
        // The admin adds a template identical to a V8 sample row: it must not be taken for the sample.
        UUID templateId = UUID.randomUUID();
        db.update("INSERT INTO session_template (id, level_id, day_of_week, start_time, end_time, capacity, valid_from) "
                + "VALUES (?, 1, 6, '07:00', '08:00', 6, DATE '2026-10-01')", templateId);

        start("prod", url).close();

        assertThat(db.queryForObject("SELECT count(*) FROM trainer WHERE id = ?", Long.class, trainerId)).isOne();
        assertThat(db.queryForObject("SELECT count(*) FROM rider WHERE id = ?", Long.class, riderId)).isOne();
        assertThat(db.queryForObject("SELECT count(*) FROM session_template WHERE id = ?", Long.class, templateId)).isOne();
    }

    // Case 4
    @Test
    void devProfileKeepsTheTestData() {
        String url = newDatabase();
        start("dev", url).close();
        JdbcTemplate db = jdbc(url);

        assertThat(db.queryForList("SELECT id::text FROM trainer ORDER BY id", String.class))
                .containsExactly(TRAINER_ONE_ID, TRAINER_TWO_ID);
        assertThat(count(db, "invite")).isOne();
        assertThat(db.queryForObject("SELECT count(*) FROM rider WHERE trainer_id = ?::uuid", Long.class, TRAINER_ONE_ID))
                .isPositive();
        assertThat(count(db, "session_template")).isEqualTo(5);
    }

    private static ConfigurableApplicationContext start(String profile, String url) {
        // Command-line arguments outrank application-dev.properties, which points at the compose database.
        return new SpringApplicationBuilder(EquisApiApplication.class).run(
                "--spring.profiles.active=" + profile,
                "--spring.datasource.url=" + url,
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--SPRING_DATASOURCE_URL=" + url,
                "--SPRING_DATASOURCE_USERNAME=" + postgres.getUsername(),
                "--SPRING_DATASOURCE_PASSWORD=" + postgres.getPassword(),
                "--APP_JWT_SECRET=dGVzdC1vbmx5LWp3dC1zZWNyZXQtMzItYnl0ZXMtbG9uZyE=",
                "--APP_BASE_URL=http://test.local",
                "--APP_MAIL_MODE=log",
                "--APP_ADMIN_EMAIL=" + ADMIN_EMAIL,
                "--APP_ADMIN_PASSWORD_HASH=" + ADMIN_PASSWORD_HASH,
                "--spring.docker.compose.enabled=false",
                "--server.port=0",
                "--app.sessions.generate-cron=-");
    }

    private static String newDatabase() {
        String name = "cleanup_" + DATABASES.incrementAndGet();
        jdbc(postgres.getJdbcUrl()).execute("CREATE DATABASE " + name);
        return postgres.getJdbcUrl().replace("/" + postgres.getDatabaseName(), "/" + name);
    }

    private static JdbcTemplate jdbc(String url) {
        return new JdbcTemplate(new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
    }

    private static long count(JdbcTemplate db, String table) {
        return db.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private static Map<String, Long> counts(JdbcTemplate db) {
        Map<String, Long> counts = new LinkedHashMap<>();
        TABLES.forEach(table -> counts.put(table, count(db, table)));
        return counts;
    }
}
