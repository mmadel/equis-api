-- Test databases only (spring.flyway.locations in the test properties). The fixtures clear tables with TRUNCATE
-- through the app's connection, which works as equis_app (V24). Production never grants this: TRUNCATE skips RLS.
GRANT TRUNCATE ON ALL TABLES IN SCHEMA public TO equis_app;
