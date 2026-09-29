CREATE SEQUENCE IF NOT EXISTS auth_refresh_token_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('auth_refresh_token_seq'),
    token VARCHAR(128) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified_date TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_tokens(user_id);
