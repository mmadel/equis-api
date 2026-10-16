-- In-app notices to trainers (spec 4.5). No email.
CREATE TABLE notice (
    id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    trainer_id  uuid         NOT NULL,
    type        varchar(32)  NOT NULL,
    ref_id      uuid,                       -- booking id for result_overdue, session id for session_cancelled
    message     varchar(300) NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    read_at     timestamptz,
    CONSTRAINT pk_notice          PRIMARY KEY (id),
    CONSTRAINT fk_notice_trainer  FOREIGN KEY (trainer_id) REFERENCES trainer (id),
    CONSTRAINT ck_notice_type     CHECK (type IN ('result_overdue', 'session_cancelled')),
    CONSTRAINT ck_notice_message  CHECK (btrim(message) <> '')
);

-- Home (1) lists a trainer's newest notices.
CREATE INDEX ix_notice_trainer ON notice (trainer_id, created_at DESC);

-- W2 reads the latest reminder per booking.
CREATE INDEX ix_notice_ref ON notice (ref_id, type);

-- At most one unread reminder per trainer and booking.
CREATE UNIQUE INDEX uq_notice_unread_reminder
    ON notice (trainer_id, ref_id)
    WHERE type = 'result_overdue' AND read_at IS NULL;
