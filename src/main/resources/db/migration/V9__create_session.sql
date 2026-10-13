-- Dated occurrences of a session template, generated 8 weeks ahead.
-- start_time, end_time and capacity are copied from the template at generation,
-- so later template edits change upcoming sessions only.

CREATE TABLE session (
                         id            uuid         NOT NULL DEFAULT gen_random_uuid(),
                         template_id   uuid         NOT NULL,
                         session_date  date         NOT NULL,
                         start_time    time         NOT NULL,
                         end_time      time         NOT NULL,
                         capacity      smallint     NOT NULL,
                         status        varchar(16)  NOT NULL DEFAULT 'scheduled',
                         cancelled_at  timestamptz,
                         version       bigint       NOT NULL DEFAULT 0,
                         created_at    timestamptz  NOT NULL DEFAULT now(),
                         updated_at    timestamptz  NOT NULL DEFAULT now(),
                         CONSTRAINT pk_session                PRIMARY KEY (id),
                         CONSTRAINT fk_session_template       FOREIGN KEY (template_id) REFERENCES session_template (id),
                         CONSTRAINT uq_session_template_date  UNIQUE (template_id, session_date),
                         CONSTRAINT ck_session_times          CHECK (end_time > start_time),
                         CONSTRAINT ck_session_capacity       CHECK (capacity BETWEEN 1 AND 12),
                         CONSTRAINT ck_session_status         CHECK (status IN ('scheduled', 'cancelled')),
                         CONSTRAINT ck_session_cancelled      CHECK ((status = 'cancelled') = (cancelled_at IS NOT NULL))
);

-- Trainer screens read sessions by date range (E3).
CREATE INDEX ix_session_date ON session (session_date);