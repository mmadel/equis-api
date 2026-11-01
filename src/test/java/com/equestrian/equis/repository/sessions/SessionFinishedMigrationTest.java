package com.equestrian.equis.repository.sessions;

import com.equestrian.equis.support.TestPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// E13 case 4: V20 removes release 1 seats booked for later days; today's and past seats stay.
// Runs Flyway by hand on its own database: up to V19, add release 1 rows, then the rest.
class SessionFinishedMigrationTest {
    private static final UUID TRAINER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    static TestPostgres postgres = TestPostgres.newDatabase();

    @Test
    void laterDaySeatsAreRemovedTodaysAndPastOnesStay() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        JdbcTemplate db = new JdbcTemplate(dataSource);

        flyway(dataSource, "19").migrate();
        // The migration uses the database clock, so the dates follow today in the school time zone.
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Cairo"));
        UUID template = db.queryForObject("""
                insert into session_template (level_id, day_of_week, start_time, end_time, capacity, valid_from)
                values (1, 1, '17:00', '18:00', 6, '2026-01-01') returning id
                """, UUID.class);
        UUID rider = db.queryForObject("insert into rider (name, level_id) values ('Mona Samir', 1) returning id",
                UUID.class);
        UUID yesterday = seat(db, template, rider, today.minusDays(1));
        UUID todays = seat(db, template, rider, today);
        UUID nextWeek = seat(db, template, rider, today.plusWeeks(1));

        flyway(dataSource, "latest").migrate();

        assertThat(db.queryForList("select id from assignment", UUID.class))
                .containsExactlyInAnyOrder(yesterday, todays)
                .doesNotContain(nextWeek);
        // The sessions themselves stay, still scheduled.
        assertThat(db.queryForList("select status from session", String.class))
                .hasSize(3).containsOnly("scheduled");
    }

    private static UUID seat(JdbcTemplate db, UUID template, UUID rider, LocalDate date) {
        UUID session = db.queryForObject("""
                insert into session (template_id, session_date, start_time, end_time, capacity)
                values (?, ?, '17:00', '18:00', 6) returning id
                """, UUID.class, template, date);
        return db.queryForObject("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?) returning id",
                UUID.class, session, rider, TRAINER_1);
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
