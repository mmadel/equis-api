-- One promotion or demotion of a rider. Placement is not stored here (it lives on assessment_booking).
CREATE TABLE level_change (
                              id             uuid         NOT NULL DEFAULT gen_random_uuid(),
                              rider_id       uuid         NOT NULL,
                              from_level_id  smallint     NOT NULL,          -- match level.id's type
                              to_level_id    smallint     NOT NULL,
                              reason         varchar(100) NOT NULL,
                              by_trainer_id  uuid         NOT NULL,
                              created_at     timestamptz  NOT NULL DEFAULT now(),
                              CONSTRAINT pk_level_change             PRIMARY KEY (id),
                              CONSTRAINT fk_level_change_rider       FOREIGN KEY (rider_id)      REFERENCES rider (id),
                              CONSTRAINT fk_level_change_from_level  FOREIGN KEY (from_level_id) REFERENCES level (id),
                              CONSTRAINT fk_level_change_to_level    FOREIGN KEY (to_level_id)   REFERENCES level (id),
                              CONSTRAINT fk_level_change_by_trainer  FOREIGN KEY (by_trainer_id) REFERENCES trainer (id),
    -- Core rule 6: one step at a time (the level FKs keep both ends inside 1–6).
                              CONSTRAINT ck_level_change_one_step    CHECK (abs(to_level_id - from_level_id) = 1),
                              CONSTRAINT ck_level_change_reason      CHECK (btrim(reason) <> '')
    );

-- 4a reads one rider's history, newest first.
CREATE INDEX ix_level_change_rider ON level_change (rider_id, created_at DESC);
