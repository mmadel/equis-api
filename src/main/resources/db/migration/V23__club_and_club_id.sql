-- E17 · clubs: one row per riding school, and club_id on every table that holds a school's own data.
-- The school already using equis becomes the default club; its code and name come from
-- APP_DEFAULT_CLUB_CODE / APP_DEFAULT_CLUB_NAME via Flyway placeholders, like the admin seed in V5.
--
-- Table kinds:
--   global (no club_id): level, club, flyway_schema_history
--   auth lookup (club_id, found by token before the club is known, no RLS later): invite, refresh_token
--   tenant (club_id, RLS in the next ticket): every other table
-- Cancel reasons and payment methods are CHECK constraints, not tables.

CREATE TABLE club (
    id               uuid         NOT NULL DEFAULT gen_random_uuid(),
    code             varchar(30)  NOT NULL,
    name             varchar(120) NOT NULL,
    status           varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    contact_phone    varchar(20),
    contact_email    varchar(254),
    address          varchar(300),
    logo_url         varchar(500),
    notes            text,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    created_by       uuid,
    suspended_at     timestamptz,
    suspended_reason text,
    CONSTRAINT pk_club           PRIMARY KEY (id),
    CONSTRAINT uq_club_code      UNIQUE (code),
    -- lowercase letters, digits and inner hyphens, 3–30 characters; typed at sign-in, never changed
    CONSTRAINT ck_club_code      CHECK (code ~ '^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$'),
    CONSTRAINT ck_club_name      CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_club_status    CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_club_suspended CHECK ((status = 'SUSPENDED') = (suspended_at IS NOT NULL))
);

INSERT INTO club (id, code, name)
VALUES ('00000000-0000-0000-0000-000000000c01', '${defaultClubCode}', '${defaultClubName}');

-- club_id on every tenant table and on invite. The DEFAULT fills existing rows and keeps today's inserts
-- working; the next ticket removes it once @TenantId fills the column.
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'admin_account', 'trainer', 'invite',
        'session_template', 'session', 'rider', 'assignment',
        'member', 'assessment_booking', 'level_change', 'notice',
        'payment', 'session_usage', 'session_evaluation']
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ADD COLUMN club_id uuid NOT NULL DEFAULT ''00000000-0000-0000-0000-000000000c01'', '
            'ADD CONSTRAINT %I FOREIGN KEY (club_id) REFERENCES club (id)',
            t, 'fk_' || t || '_club');
        EXECUTE format('CREATE INDEX %I ON %I (club_id)', 'ix_' || t || '_club', t);
    END LOOP;
END $$;

-- Super admin tokens will have no club, so refresh_token.club_id is nullable and has no default.
ALTER TABLE refresh_token ADD COLUMN club_id uuid;
UPDATE refresh_token SET club_id = '00000000-0000-0000-0000-000000000c01';
ALTER TABLE refresh_token ADD CONSTRAINT fk_refresh_token_club FOREIGN KEY (club_id) REFERENCES club (id);
CREATE INDEX ix_refresh_token_club ON refresh_token (club_id);

-- Phone and email are unique inside one club. Two clubs may each have a trainer with the same phone.
-- Uniques already scoped by a parent row, like (session_id, rider_id), stay as they are.
ALTER TABLE trainer
    DROP CONSTRAINT uq_trainer_phone,
    DROP CONSTRAINT uq_trainer_email,
    ADD CONSTRAINT uq_trainer_club_phone UNIQUE (club_id, phone),
    ADD CONSTRAINT uq_trainer_club_email UNIQUE (club_id, email);

ALTER TABLE admin_account
    DROP CONSTRAINT uq_admin_account_email,
    ADD CONSTRAINT uq_admin_account_club_email UNIQUE (club_id, email);

ALTER TABLE member
    DROP CONSTRAINT uq_member_phone,
    DROP CONSTRAINT uq_member_email,
    ADD CONSTRAINT uq_member_club_phone UNIQUE (club_id, phone),
    ADD CONSTRAINT uq_member_club_email UNIQUE (club_id, email);

ALTER TABLE rider
    DROP CONSTRAINT uq_rider_email,
    ADD CONSTRAINT uq_rider_club_email UNIQUE (club_id, email);
