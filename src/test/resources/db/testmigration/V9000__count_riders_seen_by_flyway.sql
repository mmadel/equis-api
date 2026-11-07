-- Test only (TenantIsolationTest case 13): a data migration must see every club's rows, as Flyway bypasses
-- row-level security (spring.flyway.init-sqls). The count is kept so the test can read it.
CREATE TABLE testmigration_rider_count AS SELECT count(*) AS seen FROM rider;
