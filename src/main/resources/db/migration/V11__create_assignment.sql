-- A rider's seat in one dated session.
CREATE TABLE assignment (
                            id           uuid        NOT NULL DEFAULT gen_random_uuid(),
                            session_id   uuid        NOT NULL,
                            rider_id     uuid        NOT NULL,
                            assigned_by  uuid        NOT NULL,
                            created_at   timestamptz NOT NULL DEFAULT now(),
                            CONSTRAINT pk_assignment                PRIMARY KEY (id),
                            CONSTRAINT fk_assignment_session        FOREIGN KEY (session_id)  REFERENCES session (id),
                            CONSTRAINT fk_assignment_rider          FOREIGN KEY (rider_id)    REFERENCES rider (id),
                            CONSTRAINT fk_assignment_assigned_by    FOREIGN KEY (assigned_by) REFERENCES trainer (id),
                            CONSTRAINT uq_assignment_session_rider  UNIQUE (session_id, rider_id)
);

-- Level changes (E5) remove a rider's future seats.
CREATE INDEX ix_assignment_rider ON assignment (rider_id);