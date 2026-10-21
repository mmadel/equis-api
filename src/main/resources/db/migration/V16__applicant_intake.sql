-- E11 · applicant intake
-- The admin records every new person; the phone and email are both required and unique.

-- Part 1: phone + email
ALTER TABLE member ADD COLUMN email varchar(254);

-- Trial data only (no go-live): fill the now-required fields with clear placeholders.
-- '09' + 9 digits has the right shape but can't clash with a real 010/011/012/015 number.
WITH numbered AS (
    SELECT id, row_number() OVER (ORDER BY joined_at, id) AS n
    FROM member WHERE phone IS NULL
)
UPDATE member m SET phone = '09' || lpad(numbered.n::text, 9, '0')
FROM numbered WHERE m.id = numbered.id;

UPDATE member SET email = 'member-' || id || '@placeholder.invalid' WHERE email IS NULL;

-- Uniqueness covers closed people too, so someone who comes back is reopened, not entered twice.
-- An email clash with a trainer or the admin is checked in the API (different tables).
ALTER TABLE member
    ALTER COLUMN phone SET NOT NULL,
    ALTER COLUMN email SET NOT NULL,
    ADD CONSTRAINT uq_member_email       UNIQUE (email),
    ADD CONSTRAINT ck_member_email_lower CHECK (email = lower(email)),
    ADD CONSTRAINT ck_member_email_shape CHECK (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$');
-- uq_member_phone and ck_member_phone from V13 stay as they are.
