-- E14 · session evaluations and the Done state
ALTER TABLE session DROP CONSTRAINT ck_session_status;
ALTER TABLE session ADD CONSTRAINT ck_session_status
    CHECK (status IN ('scheduled', 'cancelled', 'finished', 'done'));

-- A done session keeps its finished time and finishing trainer.
-- NOT IN rather than listing scheduled/cancelled, so an unknown status is reported by ck_session_status.
ALTER TABLE session DROP CONSTRAINT ck_session_finished;
ALTER TABLE session ADD CONSTRAINT ck_session_finished CHECK (
    (status IN ('finished', 'done') AND finished_at IS NOT NULL AND finished_by IS NOT NULL)
    OR (status NOT IN ('finished', 'done') AND finished_at IS NULL AND finished_by IS NULL));

-- One evaluation per seated rider per session. The seat FK also keeps an evaluated seat from being deleted.
CREATE TABLE session_evaluation (
    id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    session_id  uuid         NOT NULL,
    rider_id    uuid         NOT NULL,
    trainer_id  uuid         NOT NULL,
    stars       smallint     NOT NULL,
    comment     varchar(500),
    created_at  timestamptz  NOT NULL,
    CONSTRAINT pk_session_evaluation         PRIMARY KEY (id),
    CONSTRAINT fk_session_evaluation_trainer FOREIGN KEY (trainer_id) REFERENCES trainer (id),
    CONSTRAINT uq_session_evaluation         UNIQUE (session_id, rider_id),
    CONSTRAINT fk_session_evaluation_seat    FOREIGN KEY (session_id, rider_id)
        REFERENCES assignment (session_id, rider_id),
    CONSTRAINT ck_session_evaluation_stars   CHECK (stars BETWEEN 1 AND 5),
    CONSTRAINT ck_session_evaluation_comment CHECK (comment IS NULL OR length(btrim(comment)) > 0)
);

CREATE INDEX ix_session_evaluation_rider ON session_evaluation (rider_id, created_at DESC);
CREATE INDEX ix_session_evaluation_trainer ON session_evaluation (trainer_id);
