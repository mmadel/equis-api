-- E17 · the person who runs equis itself. A global table, like club: no club_id and no row-level security.
-- Rows are created only by hand with ops/create-super-admin.sql, never by Flyway or at startup.
-- The app role gets its rights from the default privileges of V24.
CREATE TABLE super_admin (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           varchar(254) NOT NULL,
    password_hash   varchar(100) NOT NULL,
    failed_attempts int          NOT NULL DEFAULT 0,
    locked_until    timestamptz,
    last_login_at   timestamptz,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_super_admin_email UNIQUE (email),
    CONSTRAINT ck_super_admin_email_lower CHECK (email = lower(email))
);

-- Super admin refresh rows have club_id NULL (V23 made it nullable for this).
ALTER TABLE refresh_token DROP CONSTRAINT ck_refresh_token_subject_type;
ALTER TABLE refresh_token ADD CONSTRAINT ck_refresh_token_subject_type
    CHECK (subject_type IN ('trainer', 'admin', 'rider', 'super_admin'));
