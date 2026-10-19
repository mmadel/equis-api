-- PROD ONLY. Loaded by the prod profile (spring.flyway.locations in application-prod.properties).
-- Flyway runs it after every migrate. It removes the dev/test data seeded by these migrations:
--   V4__seed_test_trainers.sql     two test trainers (fixed ids ...0001 and ...0002) and trainer 2's open invite
--   V8__seed_sample_templates.sql  five sample session templates
--   V12__seed_sample_riders.sql    sample riders of test trainer 1
-- A future migration that seeds test data must be undone here too.
--
-- WARNING: never start the prod profile against the dev database. This script would delete
-- the dev test trainers, sample riders and sample templates there, with everything linked to them.
--
-- Safe to repeat: trainers are matched by fixed id; sample templates only when they equal a V8 row
-- AND were created before V9 was installed (Flyway stamps installed_on when a migration starts, so V8's
-- own stamp is earlier than its rows). The API serves nothing until all migrations have run, so a
-- template the admin adds is always newer and never matched.
-- Children are deleted before parents. Note: '$' + '{' starts a Flyway placeholder in this file.

-- 1. Seats: of test riders, made by test trainers, or in sessions of the sample templates
DELETE FROM assignment
WHERE rider_id IN (SELECT id FROM rider
                   WHERE trainer_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002'))
   OR assigned_by IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002')
   OR session_id IN (SELECT s.id FROM session s
                     JOIN session_template t ON t.id = s.template_id
                     JOIN (VALUES (1::smallint, 6::smallint, TIME '07:00', TIME '08:00', 6::smallint),
                                  (1::smallint, 1::smallint, TIME '16:00', TIME '17:00', 6::smallint),
                                  (2::smallint, 6::smallint, TIME '08:30', TIME '09:30', 6::smallint),
                                  (NULL::smallint, 6::smallint, TIME '12:00', TIME '12:30', 3::smallint),
                                  (NULL::smallint, 2::smallint, TIME '12:00', TIME '12:30', 3::smallint))
                          AS v (level_id, day_of_week, start_time, end_time, capacity)
                       ON t.level_id IS NOT DISTINCT FROM v.level_id
                      AND t.day_of_week = v.day_of_week AND t.start_time = v.start_time
                      AND t.end_time = v.end_time AND t.capacity = v.capacity
                     WHERE t.valid_from = DATE '2026-10-01' AND t.valid_to IS NULL
                       AND t.created_at < (SELECT installed_on FROM "${flyway:table}" WHERE version = '9' AND success));

-- 2. Level changes of test riders or made by test trainers
DELETE FROM level_change
WHERE rider_id IN (SELECT id FROM rider
                   WHERE trainer_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002'))
   OR by_trainer_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002');

-- 3. Assessment bookings: by test trainers, in sample sessions, or of members linked to test riders
DELETE FROM assessment_booking
WHERE assessor_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002')
   OR member_id IN (SELECT member_id FROM rider
                    WHERE trainer_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002')
                      AND member_id IS NOT NULL)
   OR session_id IN (SELECT s.id FROM session s
                     JOIN session_template t ON t.id = s.template_id
                     JOIN (VALUES (1::smallint, 6::smallint, TIME '07:00', TIME '08:00', 6::smallint),
                                  (1::smallint, 1::smallint, TIME '16:00', TIME '17:00', 6::smallint),
                                  (2::smallint, 6::smallint, TIME '08:30', TIME '09:30', 6::smallint),
                                  (NULL::smallint, 6::smallint, TIME '12:00', TIME '12:30', 3::smallint),
                                  (NULL::smallint, 2::smallint, TIME '12:00', TIME '12:30', 3::smallint))
                          AS v (level_id, day_of_week, start_time, end_time, capacity)
                       ON t.level_id IS NOT DISTINCT FROM v.level_id
                      AND t.day_of_week = v.day_of_week AND t.start_time = v.start_time
                      AND t.end_time = v.end_time AND t.capacity = v.capacity
                     WHERE t.valid_from = DATE '2026-10-01' AND t.valid_to IS NULL
                       AND t.created_at < (SELECT installed_on FROM "${flyway:table}" WHERE version = '9' AND success));

-- 4. Test riders, then the members linked to them (one statement: the rider -> member key is checked at its end)
WITH removed_rider AS (
    DELETE FROM rider
    WHERE trainer_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002')
    RETURNING member_id
)
DELETE FROM member
WHERE id IN (SELECT member_id FROM removed_rider WHERE member_id IS NOT NULL);

-- 5. Sessions of the sample templates, then the templates
DELETE FROM session
WHERE template_id IN (SELECT t.id FROM session_template t
                      JOIN (VALUES (1::smallint, 6::smallint, TIME '07:00', TIME '08:00', 6::smallint),
                                   (1::smallint, 1::smallint, TIME '16:00', TIME '17:00', 6::smallint),
                                   (2::smallint, 6::smallint, TIME '08:30', TIME '09:30', 6::smallint),
                                   (NULL::smallint, 6::smallint, TIME '12:00', TIME '12:30', 3::smallint),
                                   (NULL::smallint, 2::smallint, TIME '12:00', TIME '12:30', 3::smallint))
                           AS v (level_id, day_of_week, start_time, end_time, capacity)
                        ON t.level_id IS NOT DISTINCT FROM v.level_id
                       AND t.day_of_week = v.day_of_week AND t.start_time = v.start_time
                       AND t.end_time = v.end_time AND t.capacity = v.capacity
                      WHERE t.valid_from = DATE '2026-10-01' AND t.valid_to IS NULL
                        AND t.created_at < (SELECT installed_on FROM "${flyway:table}" WHERE version = '9' AND success));

DELETE FROM session_template
WHERE id IN (SELECT t.id FROM session_template t
             JOIN (VALUES (1::smallint, 6::smallint, TIME '07:00', TIME '08:00', 6::smallint),
                          (1::smallint, 1::smallint, TIME '16:00', TIME '17:00', 6::smallint),
                          (2::smallint, 6::smallint, TIME '08:30', TIME '09:30', 6::smallint),
                          (NULL::smallint, 6::smallint, TIME '12:00', TIME '12:30', 3::smallint),
                          (NULL::smallint, 2::smallint, TIME '12:00', TIME '12:30', 3::smallint))
                  AS v (level_id, day_of_week, start_time, end_time, capacity)
               ON t.level_id IS NOT DISTINCT FROM v.level_id
              AND t.day_of_week = v.day_of_week AND t.start_time = v.start_time
              AND t.end_time = v.end_time AND t.capacity = v.capacity
             WHERE t.valid_from = DATE '2026-10-01' AND t.valid_to IS NULL
               AND t.created_at < (SELECT installed_on FROM "${flyway:table}" WHERE version = '9' AND success));

-- 6. What else points to the test trainers: notices, refresh tokens (no foreign key), invites
DELETE FROM notice
WHERE trainer_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002');

DELETE FROM refresh_token
WHERE subject_type = 'trainer'
  AND subject_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002');

DELETE FROM invite
WHERE trainer_id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002');

-- 7. The test trainers
DELETE FROM trainer
WHERE id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002');
