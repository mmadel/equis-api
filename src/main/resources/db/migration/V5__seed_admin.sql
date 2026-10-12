-- Admin account. Email and BCrypt hash come from APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH
-- via Flyway placeholders. No password is committed. Change the password later with the recovery CLI.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM trainer WHERE email = lower('${adminEmail}')) THEN
        RAISE EXCEPTION 'Admin email % already belongs to a trainer', lower('${adminEmail}');
END IF;
END $$;

INSERT INTO admin_account (email, password_hash)
VALUES (lower('${adminEmail}'), '${adminPasswordHash}');