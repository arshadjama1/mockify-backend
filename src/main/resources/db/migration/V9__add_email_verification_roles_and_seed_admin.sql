-- Indicates whether the user's email address has been verified
-- Defaults to FALSE for existing and newly created users
ALTER TABLE users
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Index to speed up queries filtering verified vs unverified users
CREATE INDEX idx_users_email_verified ON users (email_verified);


-- Add role column and set existing users to USER
ALTER TABLE users
ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Insert default admin user only if it does not already exist
INSERT INTO users (id, name, username, email, email_verified, password, role, provider_name, created_at)
SELECT
    gen_random_uuid(),
    'Mockify Admin',
    'mockify',
    'mockify.noreply@gmail.com',
     true,
    '$2a$10$REPLACE_WITH_BCRYPT_HASH',          -- Replace with real bcrypt hash (use reset password)
    'ADMIN',
    'local',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'mockify.noreply@gmail.com'
);

