-- Test databases only (spring.flyway.locations in the test properties). Since E17 a club without a current
-- subscription is blocked (ClubAccessFilter), so every club that exists after migrating (the default club of V23)
-- gets a long valid one on a test-only plan. Tests that need another state change it themselves; fixtures that
-- recreate clubs call support/TestSubscriptions. Runs after every migrate, so it only adds what is missing.
INSERT INTO plan (code, name, on_expiry)
SELECT 'TEST_SEED', 'Test seed', 'READ_ONLY'
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code = 'TEST_SEED');

INSERT INTO subscription (club_id, plan_id, billing_period, starts_at, ends_at, note)
SELECT c.id, p.id, 'ANNUAL', TIMESTAMPTZ '2000-01-01 00:00:00+00', TIMESTAMPTZ '2200-01-01 00:00:00+00', 'test seed'
FROM club c
JOIN plan p ON p.code = 'TEST_SEED'
WHERE NOT EXISTS (SELECT 1 FROM subscription s WHERE s.club_id = c.id);
