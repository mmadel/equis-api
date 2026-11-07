-- Creates the super admin account. Run it by hand, once per environment; Flyway never runs it.
--   psql "$DATABASE_URL" -v email='ops@equis.app' -v pwd='...' -f ops/create-super-admin.sql
-- Type the password on the command line only; never save it in a file.
-- Running it again with the same email changes nothing (it does not reset the password).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- BCrypt cost 10 ($2a$), the hash Spring's BCryptPasswordEncoder checks at sign-in. Emails are stored lowercase.
INSERT INTO super_admin (email, password_hash)
VALUES (lower(:'email'), crypt(:'pwd', gen_salt('bf', 10)))
ON CONFLICT (email) DO NOTHING;
