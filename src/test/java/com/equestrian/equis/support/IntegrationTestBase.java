package com.equestrian.equis.support;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;

// Base of every @SpringBootTest database test (*IT). The set-up is the same for all of them, so Spring starts the
// app once per JVM and every class reuses that context:
// - the shared PostgreSQL database (PostgresContainer), with the migrated rows put back before each class
//   (FreshDatabase), so a class still starts from a database no other class has touched;
// - one MutableClock as the app's Clock and Hibernate's (CLOCK); it starts each class at TrainerAccountsFixture.START,
//   and a class that needs another time sets it in @BeforeEach;
// - emails recorded and sent on the calling thread (MailTestConfig), the fixtures (TrainerAccountsFixture,
//   AdminOversightFixture, TwoClubFixture) and MockMvc;
// - the seeded admin of the test properties (TrainerAccountsFixture.ADMIN_EMAIL, signs in with ADMIN_PASSWORD);
// - no session generation job, and Hibernate statistics on (for the query-count checks).
// A subclass that adds its own @Import, properties or bean overrides gets a context of its own: keep that rare.
@SpringBootTest(properties = {
        "app.sessions.generate-cron=-",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Import({AdminOversightFixture.Config.class, TwoClubFixture.Config.class})
public abstract class IntegrationTestBase implements PostgresContainer {
    protected static final MutableClock CLOCK = new MutableClock(TrainerAccountsFixture.START);

    @TestBean
    protected Clock clock;

    static Clock clock() {
        return CLOCK;
    }

    @BeforeAll
    static void startClock() {
        CLOCK.set(TrainerAccountsFixture.START);
    }
}
