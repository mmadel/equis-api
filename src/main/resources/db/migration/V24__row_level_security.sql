-- E17 · row-level security: PostgreSQL shows and accepts only the current club's rows on every school table,
-- even for a query that forgot the club. The app sets app.club_id / app.bypass_rls on every connection it takes
-- from the pool (TenantAwareDataSource); Flyway sets app.bypass_rls = 'on' (spring.flyway.init-sqls).
--   club set      -> that club's rows only
--   bypass on     -> every row (sign-in lookups, Flyway, jobs listing clubs)
--   neither       -> no rows, and inserts are refused
--
-- RLS never applies to a superuser or to a role with BYPASSRLS, and the app's login user may be either (the postgres
-- image makes POSTGRES_USER a superuser). So the app works as equis_app: a role without login, superuser or bypass,
-- that every connection switches to when it leaves the pool (SET ROLE in TenantAwareDataSource). Flyway runs
-- RESET ROLE first and keeps working as the owner. Roles belong to the whole server, so the role may exist already.
DO $$
BEGIN
    CREATE ROLE equis_app NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;
GRANT equis_app TO CURRENT_USER;
GRANT USAGE ON SCHEMA public TO equis_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO equis_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO equis_app;
-- Tables and sequences of later migrations get the same rights.
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO equis_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO equis_app;

-- The fixed default club from V23 is replaced by "the current club": a native insert without club_id takes the
-- connection's club, and with no club set the NOT NULL check refuses the row. Hibernate always writes club_id itself.
-- invite gets the same default but no RLS: links are looked up by token before the club is known.

ALTER TABLE invite ALTER COLUMN club_id SET DEFAULT nullif(current_setting('app.club_id', true), '')::uuid;

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'admin_account', 'trainer',
        'session_template', 'session', 'rider', 'assignment',
        'member', 'assessment_booking', 'level_change', 'notice',
        'payment', 'session_usage', 'session_evaluation']
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN club_id SET DEFAULT nullif(current_setting(''app.club_id'', true), '''')::uuid',
            t);
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        -- FORCE as well, in case the app ever connects as the tables' owner (RLS skips the owner otherwise).
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format($policy$
            CREATE POLICY tenant_isolation ON %I
                USING (current_setting('app.bypass_rls', true) = 'on'
                       OR club_id = nullif(current_setting('app.club_id', true), '')::uuid)
                WITH CHECK (current_setting('app.bypass_rls', true) = 'on'
                       OR club_id = nullif(current_setting('app.club_id', true), '')::uuid)
            $policy$, t);
    END LOOP;
END $$;
