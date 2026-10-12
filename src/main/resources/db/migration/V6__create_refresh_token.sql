-- Refresh tokens for trainers and the admin. Only the SHA-256 hex of the token is stored.
-- subject_id points to trainer.id or admin_account.id, so it has no foreign key.

CREATE TABLE refresh_token (
                               id            uuid         NOT NULL DEFAULT gen_random_uuid(),
                               subject_type  varchar(16)  NOT NULL,
                               subject_id    uuid         NOT NULL,
                               token_hash    varchar(64)  NOT NULL,
                               remember_me   boolean      NOT NULL DEFAULT false,
                               expires_at    timestamptz  NOT NULL,
                               created_at    timestamptz  NOT NULL DEFAULT now(),
                               revoked_at    timestamptz,
                               replaced_by   uuid,
                               CONSTRAINT pk_refresh_token               PRIMARY KEY (id),
                               CONSTRAINT uq_refresh_token_token_hash    UNIQUE (token_hash),
                               CONSTRAINT ck_refresh_token_subject_type  CHECK (subject_type IN ('trainer', 'admin')),
                               CONSTRAINT ck_refresh_token_token_hash    CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_refresh_token_expiry        CHECK (expires_at > created_at),
    CONSTRAINT ck_refresh_token_revoked       CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    CONSTRAINT fk_refresh_token_replaced_by   FOREIGN KEY (replaced_by) REFERENCES refresh_token (id) ON DELETE SET NULL
);

-- Fast "revoke all active tokens of this user".
CREATE INDEX ix_refresh_token_active_subject ON refresh_token (subject_type, subject_id) WHERE revoked_at IS NULL;