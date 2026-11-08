package com.equestrian.equis.repository.club;

import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.service.platform.AccessReason;
import com.equestrian.equis.service.platform.AccessState;
import com.equestrian.equis.service.platform.ClubAccess;
import com.equestrian.equis.service.platform.ClubAccessService;
import com.equestrian.equis.support.TestPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// E17 · a new 3.0.0 install: the app starts on an empty database with the schema check on (ddl-auto=validate).
// Flyway creates the default club and no plans; plans come from ops/upsert-plan.sql, so until then the default club
// is blocked. Only db/migration runs, as in production (no test grants or test subscription).
@SpringBootTest(properties = {
        "app.sessions.generate-cron=-",
        "spring.flyway.locations=classpath:db/migration"})
class EmptyDatabaseStartIT {
    static final TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    Environment environment;
    @Autowired
    ClubAccessService clubAccess;

    @Test
    void theAppStartsWithTheDefaultClubAndNoPlans() {
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrlSeeingAllClubs(), postgres.getUsername(), postgres.getPassword()));

        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(db.queryForList("select id from club")).singleElement()
                .satisfies(club -> assertThat(club.get("id")).isEqualTo(Club.DEFAULT_ID));
        assertThat(db.queryForObject("select count(*) from plan", Long.class)).isZero();
        assertThat(db.queryForObject("select count(*) from subscription", Long.class)).isZero();
        assertThat(db.queryForObject("select count(*) from super_admin", Long.class)).isZero();
        ClubAccess access = clubAccess.forClub(Club.DEFAULT_ID);
        assertThat(access.state()).isEqualTo(AccessState.BLOCKED);
        assertThat(access.reason()).isEqualTo(AccessReason.NO_SUBSCRIPTION);
    }
}
