-- E13 · finished sessions
ALTER TABLE session ADD COLUMN finished_at timestamptz;
ALTER TABLE session ADD COLUMN finished_by uuid;

ALTER TABLE session ADD CONSTRAINT fk_session_finished_by FOREIGN KEY (finished_by) REFERENCES trainer (id);

ALTER TABLE session DROP CONSTRAINT ck_session_status;
ALTER TABLE session ADD CONSTRAINT ck_session_status
    CHECK (status IN ('scheduled', 'cancelled', 'finished'));

ALTER TABLE session ADD CONSTRAINT ck_session_finished CHECK (
    (status = 'finished' AND finished_at IS NOT NULL AND finished_by IS NOT NULL)
    OR (status <> 'finished' AND finished_at IS NULL AND finished_by IS NULL));

-- Release 2: no booking ahead (epic open question 1).
-- Seats booked in release 1 for later days are removed; today's and past seats stay.
DELETE FROM assignment a
 USING session s
 WHERE a.session_id = s.id
   AND s.session_date > (now() AT TIME ZONE 'Africa/Cairo')::date;
