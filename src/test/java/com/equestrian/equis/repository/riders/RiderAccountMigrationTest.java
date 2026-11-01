package com.equestrian.equis.repository.riders;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// E15 case 2: V22 copies each placed rider's intake email onto the rider; every rider starts with no account.
// Runs Flyway by hand on its own database: up to V21, add a placed rider and a seed rider, then the rest.
@Testcontainers
class RiderAccountMigrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Test
    void placedRidersTakeTheirIntakeEmailAndSeedRidersKeepNone() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        JdbcTemplate db = new JdbcTemplate(dataSource);

        flyway(dataSource, "21").migrate();
        UUID member = db.queryForObject("""
                insert into member (name, phone, email) values ('Sara Ali', '01012345678', 'sara.ali@gmail.com')
                returning id
                """, UUID.class);
        UUID sara = db.queryForObject(
                "insert into rider (name, phone, level_id, member_id) values ('Sara Ali', '01012345678', 2, ?) returning id",
                UUID.class, member);
        UUID seed = db.queryForObject("insert into rider (name, level_id) values ('Mona Samir', 1) returning id",
                UUID.class);

        flyway(dataSource, "latest").migrate();

        Map<String, Object> saraRow = db.queryForMap("select email, account_status from rider where id = ?", sara);
        assertThat(saraRow).containsEntry("email", "sara.ali@gmail.com").containsEntry("account_status", "none");
        Map<String, Object> seedRow = db.queryForMap("select email, account_status from rider where id = ?", seed);
        assertThat(seedRow).containsEntry("email", null).containsEntry("account_status", "none");
        // Seed riders from earlier migrations have no member either.
        assertThat(db.queryForList("select account_status from rider", String.class)).containsOnly("none");
        assertThat(db.queryForObject("select count(*) from rider where member_id is null and email is not null",
                Integer.class)).isZero();
    }

    // The admin seed (V5) needs its placeholders; the values are test stand-ins.
    private static Flyway flyway(DriverManagerDataSource dataSource, String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .placeholders(Map.of("adminEmail", "admin@example.com", "adminPasswordHash", "not-a-real-hash"))
                .target(target)
                .load();
    }
}
