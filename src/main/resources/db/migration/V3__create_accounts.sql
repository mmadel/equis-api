-- Accounts: trainers, invite/set-password tokens, admin account.
-- Emails are stored lowercase (the service lowercases them); tokens are stored as SHA-256 hex.

CREATE TABLE trainer (
                         id              uuid          NOT NULL DEFAULT gen_random_uuid(),
                         name            varchar(100)  NOT NULL,
                         phone           varchar(11)   NOT NULL,
                         email           varchar(254)  NOT NULL,
                         password_hash   varchar(100),
                         status          varchar(16)   NOT NULL DEFAULT 'invited',
                         last_active_at  timestamptz,
                         created_at      timestamptz   NOT NULL DEFAULT now(),
                         updated_at      timestamptz   NOT NULL DEFAULT now(),
                         CONSTRAINT pk_trainer                PRIMARY KEY (id),
                         CONSTRAINT uq_trainer_phone          UNIQUE (phone),
                         CONSTRAINT uq_trainer_email          UNIQUE (email),
                         CONSTRAINT ck_trainer_name           CHECK (length(trim(name)) > 0),
                         CONSTRAINT ck_trainer_phone          CHECK (phone ~ '^0[0-9]{10}$'),
    CONSTRAINT ck_trainer_email          CHECK (email = lower(email) AND position('@' IN email) > 1),
    CONSTRAINT ck_trainer_status         CHECK (status IN ('invited', 'active', 'disabled')),
    CONSTRAINT ck_trainer_active_password CHECK (status <> 'active' OR password_hash IS NOT NULL)
);

CREATE TABLE invite (
                        id           uuid         NOT NULL DEFAULT gen_random_uuid(),
                        trainer_id   uuid         NOT NULL,
                        token_hash   varchar(64)  NOT NULL,
                        purpose      varchar(16)  NOT NULL,
                        expires_at   timestamptz  NOT NULL,
                        accepted_at  timestamptz,
                        created_at   timestamptz  NOT NULL DEFAULT now(),
                        CONSTRAINT pk_invite             PRIMARY KEY (id),
                        CONSTRAINT fk_invite_trainer     FOREIGN KEY (trainer_id) REFERENCES trainer (id) ON DELETE CASCADE,
                        CONSTRAINT uq_invite_token_hash  UNIQUE (token_hash),
                        CONSTRAINT ck_invite_token_hash  CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_invite_purpose     CHECK (purpose IN ('invite', 'reset')),
    CONSTRAINT ck_invite_expiry      CHECK (expires_at > created_at),
    CONSTRAINT ck_invite_accepted    CHECK (accepted_at IS NULL OR accepted_at >= created_at)
);

-- At most one open (unaccepted) link per trainer; Resend / Reset delete the open row first.
CREATE UNIQUE INDEX uq_invite_open_per_trainer ON invite (trainer_id) WHERE accepted_at IS NULL;
-- Covers the foreign key for joins and cascade deletes.
CREATE INDEX ix_invite_trainer ON invite (trainer_id);

CREATE TABLE admin_account (
                               id               uuid          NOT NULL DEFAULT gen_random_uuid(),
                               email            varchar(254)  NOT NULL,
                               password_hash    varchar(100)  NOT NULL,
                               failed_attempts  integer       NOT NULL DEFAULT 0,
                               locked_until     timestamptz,
                               last_login_at    timestamptz,
                               created_at       timestamptz   NOT NULL DEFAULT now(),
                               updated_at       timestamptz   NOT NULL DEFAULT now(),
                               CONSTRAINT pk_admin_account                 PRIMARY KEY (id),
                               CONSTRAINT uq_admin_account_email           UNIQUE (email),
                               CONSTRAINT ck_admin_account_email           CHECK (email = lower(email) AND position('@' IN email) > 1),
                               CONSTRAINT ck_admin_account_failed_attempts CHECK (failed_attempts >= 0)
);