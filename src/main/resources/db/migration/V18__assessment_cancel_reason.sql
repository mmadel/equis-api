-- E11 · allow cancelling an assessment (with reason)
-- The admin's row cancel always stores a reason; a null reason means the slot was deleted (release 1).
ALTER TABLE assessment_booking
    ADD COLUMN cancel_reason      varchar(32),
    ADD COLUMN cancel_reason_text varchar(200),
    ADD CONSTRAINT ck_assessment_booking_cancel_reason CHECK (
        cancel_reason IS NULL OR cancel_reason IN (
            'person_cancelled', 'person_unwell', 'trainer_unavailable',
            'weather', 'horse_unavailable', 'facility_closed', 'other')),
    -- a reason only on a cancelled booking
    ADD CONSTRAINT ck_assessment_booking_cancel_status CHECK (
        cancel_reason IS NULL OR status = 'cancelled'),
    -- text required for "other", not allowed otherwise
    ADD CONSTRAINT ck_assessment_booking_cancel_text CHECK (
           (cancel_reason = 'other' AND cancel_reason_text IS NOT NULL AND btrim(cancel_reason_text) <> '')
        OR (cancel_reason IS DISTINCT FROM 'other' AND cancel_reason_text IS NULL));
