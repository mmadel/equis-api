-- TEST DATA: remove before a real server (same as the test trainers).
INSERT INTO rider (name, phone, level_id, trainer_id)
SELECT r.name, r.phone, r.level_id, t.id
FROM (VALUES
          ('Mona Ali',    '01001234567', 1),
          ('Omar Hassan', NULL,          1),
          ('Laila Samir', '01112345678', 1)
         -- … levels 2–6
     ) AS r(name, phone, level_id)
         CROSS JOIN (SELECT id FROM trainer WHERE email = 'trainer1@example.com') t;