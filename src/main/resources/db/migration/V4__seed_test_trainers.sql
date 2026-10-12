-- DEV / TRIAL ONLY. Remove these rows with a later migration before a real server is used.
-- Trainer 1 (active):  sign in with trainer1@example.com or 01000000001, password Trainer@123
-- Trainer 2 (invited): invite link token = dev-invite-token-trainer2-0000000000000000000000
--                      (stored below as its SHA-256)

INSERT INTO trainer (id, name, phone, email, password_hash, status) VALUES
                                                                        ('00000000-0000-0000-0000-000000000001', 'Test Trainer One', '01000000001', 'trainer1@example.com',
                                                                         '$2a$10$Ik5cCu7XNL8SFoUPFT21hO5GvUhnVSPNyuKdsac0MJHveGb8ZWcC.', 'active'),
                                                                        ('00000000-0000-0000-0000-000000000002', 'Test Trainer Two', '01000000002', 'trainer2@example.com',
                                                                         NULL, 'invited');

INSERT INTO invite (trainer_id, token_hash, purpose, expires_at) VALUES
    ('00000000-0000-0000-0000-000000000002',
     '7b86a4fcceeda9da0942d6bd2c458b4d14bca92f1de0794db65b4b240d4fb1fd',
     'invite',
     TIMESTAMPTZ '2099-12-31 23:59:59+00');