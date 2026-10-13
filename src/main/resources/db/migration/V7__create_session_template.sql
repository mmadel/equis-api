-- Weekly recurring sessions defined by the admin in W1.
-- level_id NULL = assessment slot. day_of_week is ISO: 1 = Monday ... 7 = Sunday.
-- Times are school-local (Africa/Cairo). Rows are soft-deleted: past sessions reference them.

CREATE TABLE session_template (
                                  id           uuid         NOT NULL DEFAULT gen_random_uuid(),
                                  level_id     smallint,                 -- use the same type as level.id
                                  day_of_week  smallint     NOT NULL,
                                  start_time   time         NOT NULL,
                                  end_time     time         NOT NULL,
                                  capacity     smallint     NOT NULL,
                                  valid_from   date         NOT NULL,
                                  valid_to     date,
                                  created_at   timestamptz  NOT NULL DEFAULT now(),
                                  updated_at   timestamptz  NOT NULL DEFAULT now(),
                                  deleted_at   timestamptz,
                                  CONSTRAINT pk_session_template           PRIMARY KEY (id),
                                  CONSTRAINT fk_session_template_level     FOREIGN KEY (level_id) REFERENCES level (id),
                                  CONSTRAINT ck_session_template_day       CHECK (day_of_week BETWEEN 1 AND 7),
                                  CONSTRAINT ck_session_template_times     CHECK (end_time > start_time),
                                  CONSTRAINT ck_session_template_capacity  CHECK (capacity BETWEEN 1 AND 12),
                                  CONSTRAINT ck_session_template_validity  CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

-- W1 tabs and the overlap check read active templates by level and day.
CREATE INDEX ix_session_template_level_day ON session_template (level_id, day_of_week) WHERE deleted_at IS NULL;