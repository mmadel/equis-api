package com.equestrian.equis.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

// Since E17 a club without a current subscription is blocked. The default club gets one when the test database is
// migrated (db/testgrants/afterMigrate__test_subscription.sql); fixtures that delete and recreate clubs call this
// so their clubs are active again. A long ANNUAL row on the test-only TEST_SEED plan, from 2000 to 2200.
// Plan and subscription have no row-level security, so any connection may run it.
public final class TestSubscriptions {
    private TestSubscriptions() {
    }

    public static void validFor(JdbcTemplate jdbc, UUID... clubs) {
        jdbc.update("""
                INSERT INTO plan (code, name, on_expiry) SELECT 'TEST_SEED', 'Test seed', 'READ_ONLY'
                WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code = 'TEST_SEED')""");
        for (UUID club : clubs) {
            jdbc.update("""
                    INSERT INTO subscription (club_id, plan_id, billing_period, starts_at, ends_at, note)
                    SELECT ?, id, 'ANNUAL', TIMESTAMPTZ '2000-01-01 00:00:00+00', TIMESTAMPTZ '2200-01-01 00:00:00+00',
                           'test seed'
                    FROM plan WHERE code = 'TEST_SEED'""", club);
        }
    }
}
