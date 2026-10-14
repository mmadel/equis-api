-- A walk-in registered by a trainer, not placed yet.
CREATE TABLE member (
                        id          uuid         NOT NULL DEFAULT gen_random_uuid(),
                        name        varchar(100) NOT NULL,
                        phone       varchar(11),
                        joined_at   timestamptz  NOT NULL DEFAULT now(),
                        updated_at  timestamptz  NOT NULL DEFAULT now(),
                        CONSTRAINT pk_member        PRIMARY KEY (id),
                        CONSTRAINT uq_member_phone  UNIQUE (phone),                 -- several NULLs are allowed
                        CONSTRAINT ck_member_name   CHECK (btrim(name) <> ''),
                        CONSTRAINT ck_member_phone  CHECK (phone IS NULL OR phone ~ '^0[0-9]{10}$')
    );

-- One member booked into one assessment slot, plus the result.
CREATE TABLE assessment_booking (
                                    id            uuid         NOT NULL DEFAULT gen_random_uuid(),
                                    member_id     uuid         NOT NULL,
                                    session_id    uuid         NOT NULL,   -- must be an assessment slot: checked in the service
                                    assessor_id   uuid         NOT NULL,
                                    status        varchar(16)  NOT NULL DEFAULT 'upcoming',
                                    attended      boolean,
                                    level_id      smallint,                -- match level.id's type
                                    notes         varchar(500),
                                    recorded_at   timestamptz,
                                    cancelled_at  timestamptz,
                                    version       bigint       NOT NULL DEFAULT 0,
                                    created_at    timestamptz  NOT NULL DEFAULT now(),
                                    updated_at    timestamptz  NOT NULL DEFAULT now(),
                                    CONSTRAINT pk_assessment_booking           PRIMARY KEY (id),
                                    CONSTRAINT fk_assessment_booking_member    FOREIGN KEY (member_id)   REFERENCES member (id),
                                    CONSTRAINT fk_assessment_booking_session   FOREIGN KEY (session_id)  REFERENCES session (id),
                                    CONSTRAINT fk_assessment_booking_assessor  FOREIGN KEY (assessor_id) REFERENCES trainer (id),
                                    CONSTRAINT fk_assessment_booking_level     FOREIGN KEY (level_id)    REFERENCES level (id),
                                    CONSTRAINT ck_assessment_booking_status    CHECK (status IN ('upcoming', 'placed', 'noshow', 'cancelled')),
    -- The result columns must match the status (see the lifecycle table).
                                    CONSTRAINT ck_assessment_booking_result    CHECK (
                                        (status = 'upcoming'  AND attended IS NULL  AND level_id IS NULL     AND recorded_at IS NULL     AND cancelled_at IS NULL)
                                            OR (status = 'placed'    AND attended = true   AND level_id IS NOT NULL AND recorded_at IS NOT NULL AND cancelled_at IS NULL)
                                            OR (status = 'noshow'    AND attended = false  AND level_id IS NULL     AND recorded_at IS NOT NULL AND cancelled_at IS NULL)
                                            OR (status = 'cancelled' AND attended IS NULL  AND level_id IS NULL     AND recorded_at IS NULL     AND cancelled_at IS NOT NULL)
                                        )
);

-- Core rule 4: at most one active booking per member. Old no-show and cancelled rows stay as history.
CREATE UNIQUE INDEX uq_assessment_booking_active_member
    ON assessment_booking (member_id) WHERE status = 'upcoming';

-- A3/A4 list a slot's bookings; the capacity rule counts them.
CREATE INDEX ix_assessment_booking_session ON assessment_booking (session_id, status);

-- E3 left rider.member_id without a foreign key. Seeded sample riders have no member,
-- so clear any value first (the member table is empty at this point).
UPDATE rider SET member_id = NULL WHERE member_id IS NOT NULL;

ALTER TABLE rider
    ADD CONSTRAINT fk_rider_member FOREIGN KEY (member_id) REFERENCES member (id),
    ADD CONSTRAINT uq_rider_member UNIQUE (member_id);       -- one member becomes at most one rider