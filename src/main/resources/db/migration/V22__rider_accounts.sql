-- E15 · rider accounts: a rider can hold a login email, a password and an account status.
-- Emails are stored lowercase, like trainer and member emails.
ALTER TABLE rider ADD COLUMN email          varchar(254);
ALTER TABLE rider ADD COLUMN password_hash  varchar(100);
ALTER TABLE rider ADD COLUMN account_status varchar(16) NOT NULL DEFAULT 'none';
ALTER TABLE rider ADD COLUMN last_active_at timestamptz;

-- Existing riders take the email the admin typed at intake. Seed riders have no member and keep none.
UPDATE rider r SET email = m.email FROM member m WHERE r.member_id = m.id;

ALTER TABLE rider ADD CONSTRAINT uq_rider_email UNIQUE (email);
ALTER TABLE rider ADD CONSTRAINT ck_rider_email
    CHECK (email IS NULL OR (email = lower(email) AND position('@' IN email) > 1));
ALTER TABLE rider ADD CONSTRAINT ck_rider_account_status
    CHECK (account_status IN ('none', 'invited', 'active'));
ALTER TABLE rider ADD CONSTRAINT ck_rider_account_email
    CHECK (account_status = 'none' OR email IS NOT NULL);
ALTER TABLE rider ADD CONSTRAINT ck_rider_active_password
    CHECK (account_status <> 'active' OR password_hash IS NOT NULL);

-- Links can belong to a trainer or a rider, never both. uq_invite_open_per_trainer ignores rider links
-- (a unique index skips rows where trainer_id is null).
ALTER TABLE invite ALTER COLUMN trainer_id DROP NOT NULL;
ALTER TABLE invite ADD COLUMN rider_id uuid;
ALTER TABLE invite ADD CONSTRAINT fk_invite_rider
    FOREIGN KEY (rider_id) REFERENCES rider (id) ON DELETE CASCADE;
ALTER TABLE invite ADD CONSTRAINT ck_invite_owner
    CHECK ((trainer_id IS NULL) <> (rider_id IS NULL));
-- At most one open (unaccepted) link per rider.
CREATE UNIQUE INDEX uq_invite_open_per_rider ON invite (rider_id)
    WHERE accepted_at IS NULL AND rider_id IS NOT NULL;
-- Covers the foreign key for joins and cascade deletes.
CREATE INDEX ix_invite_rider ON invite (rider_id);

-- Refresh tokens for riders.
ALTER TABLE refresh_token DROP CONSTRAINT ck_refresh_token_subject_type;
ALTER TABLE refresh_token ADD CONSTRAINT ck_refresh_token_subject_type
    CHECK (subject_type IN ('trainer', 'admin', 'rider'));
