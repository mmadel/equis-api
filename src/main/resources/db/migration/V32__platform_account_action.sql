-- E22 · what the super admin did to a club's accounts (today only RESET_PASSWORD). A global platform table, like
-- subscription: a plain club_id foreign key, no row-level security, no @TenantId. The app role gets its rights from
-- the default privileges of V24. Name and email are copied at the moment of the action, so the activity list stays
-- readable if the account changes later. account_id points to admin_account, trainer or rider by account_type,
-- so it has no foreign key.
CREATE TABLE platform_account_action (
    id            uuid         PRIMARY KEY,
    club_id       uuid         NOT NULL REFERENCES club (id),
    account_type  varchar(10)  NOT NULL,
    account_id    uuid         NOT NULL,
    account_name  varchar(120) NOT NULL,
    account_email varchar(254) NOT NULL,
    action        varchar(30)  NOT NULL,
    done_by       uuid         NOT NULL REFERENCES super_admin (id),
    done_at       timestamptz  NOT NULL,
    CONSTRAINT ck_platform_account_action_type CHECK (account_type IN ('ADMIN', 'TRAINER', 'RIDER')),
    CONSTRAINT ck_platform_account_action_action CHECK (action IN ('RESET_PASSWORD'))
);
-- The club's activity list: newest first.
CREATE INDEX platform_account_action_club_done_idx ON platform_account_action (club_id, done_at DESC);
