-- E17 · club onboarding: a club admin can be invited like a trainer. The super admin creates the club and its first
-- admin, who gets a set-password link by email; until it is used the admin has no password and can't sign in.
-- Existing admins (the default club's seeded one) become ACTIVE with the name "Admin".

ALTER TABLE admin_account ADD COLUMN name varchar(120) NOT NULL DEFAULT 'Admin';
ALTER TABLE admin_account ALTER COLUMN name DROP DEFAULT;
ALTER TABLE admin_account ADD CONSTRAINT ck_admin_account_name CHECK (length(trim(name)) > 0);

ALTER TABLE admin_account ADD COLUMN status varchar(10) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE admin_account ADD CONSTRAINT ck_admin_account_status CHECK (status IN ('INVITED', 'ACTIVE'));

ALTER TABLE admin_account ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE admin_account ADD CONSTRAINT ck_admin_account_active_password
    CHECK (status = 'INVITED' OR password_hash IS NOT NULL);

-- Links can now belong to a trainer, a rider or a club admin: exactly one of them.
ALTER TABLE invite ADD COLUMN admin_account_id uuid;
ALTER TABLE invite ADD CONSTRAINT fk_invite_admin_account
    FOREIGN KEY (admin_account_id) REFERENCES admin_account (id) ON DELETE CASCADE;
ALTER TABLE invite DROP CONSTRAINT ck_invite_owner;
ALTER TABLE invite ADD CONSTRAINT ck_invite_owner
    CHECK (num_nonnulls(trainer_id, rider_id, admin_account_id) = 1);
-- At most one open (unaccepted) link per admin.
CREATE UNIQUE INDEX uq_invite_open_per_admin ON invite (admin_account_id)
    WHERE accepted_at IS NULL AND admin_account_id IS NOT NULL;
-- Covers the foreign key for joins and cascade deletes.
CREATE INDEX ix_invite_admin_account ON invite (admin_account_id);
