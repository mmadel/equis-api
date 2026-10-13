-- A placed member. Belongs to one level and (normally) one trainer.
CREATE TABLE rider (
                       id          uuid         NOT NULL DEFAULT gen_random_uuid(),
                       name        varchar(100) NOT NULL,
                       phone       varchar(11),
                       level_id    smallint     NOT NULL,          -- match level.id's type
                       trainer_id  uuid,                           -- null = unassigned (W3)
                       member_id   uuid,                           -- FK added in E4
                       version     bigint       NOT NULL DEFAULT 0,
                       created_at  timestamptz  NOT NULL DEFAULT now(),
                       updated_at  timestamptz  NOT NULL DEFAULT now(),
                       CONSTRAINT pk_rider          PRIMARY KEY (id),
                       CONSTRAINT fk_rider_level    FOREIGN KEY (level_id)   REFERENCES level (id),
                       CONSTRAINT fk_rider_trainer  FOREIGN KEY (trainer_id) REFERENCES trainer (id),
                       CONSTRAINT ck_rider_phone    CHECK (phone IS NULL OR phone ~ '^0[0-9]{10}$')
    );

-- Screens 1–3 read a trainer's riders per level.
CREATE INDEX ix_rider_trainer_level ON rider (trainer_id, level_id);