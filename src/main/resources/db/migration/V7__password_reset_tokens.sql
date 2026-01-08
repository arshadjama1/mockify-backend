-- Password reset tokens for local-auth users
-- One-time, time-bound, hashed tokens

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT NOW() NOT NULL,

    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- Fast lookup by token
CREATE INDEX idx_password_reset_token_hash
    ON password_reset_tokens(token_hash);

-- Useful for cleanup jobs & audits
CREATE INDEX idx_password_reset_user
    ON password_reset_tokens(user_id);