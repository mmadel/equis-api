package com.equestrian.equis.repository.assessment;

import com.equestrian.equis.support.TestPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// E11 case 2: V16 fills the trial members that have no phone or email with placeholders.
// Runs Flyway by hand on its own database: up to V15, add release 1 rows, then the rest.
class ApplicantIntakeMigrationTest {
    static TestPostgres postgres = TestPostgres.newDatabase();

    @Test
    void membersWithoutPhoneOrEmailGetPlaceholders() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        JdbcTemplate db = new JdbcTemplate(dataSource);

        flyway(dataSource, "15").migrate();
        UUID first = db.queryForObject("insert into member (name, joined_at) values ('First', '2026-10-01T10:00Z') returning id",
                UUID.class);
        UUID second = db.queryForObject("insert into member (name, joined_at) values ('Second', '2026-10-02T10:00Z') returning id",
                UUID.class);
        UUID withPhone = db.queryForObject("insert into member (name, phone) values ('Has Phone', '01011112222') returning id",
                UUID.class);

        flyway(dataSource, "latest").migrate();

        assertThat(row(db, first)).containsEntry("phone", "09000000001")
                .containsEntry("email", "member-" + first + "@placeholder.invalid");
        assertThat(row(db, second)).containsEntry("phone", "09000000002")
                .containsEntry("email", "member-" + second + "@placeholder.invalid");
        assertThat(row(db, withPhone)).containsEntry("phone", "01011112222")
                .containsEntry("email", "member-" + withPhone + "@placeholder.invalid");
        assertThat(db.queryForList("select phone from member where phone is null or email is null", String.class))
                .isEqualTo(List.of());
    }

    private static Map<String, Object> row(JdbcTemplate db, UUID id) {
        return db.queryForMap("select phone, email from member where id = ?", id);
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
