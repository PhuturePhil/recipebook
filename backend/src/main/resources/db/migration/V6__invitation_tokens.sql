CREATE TABLE invitation_tokens (
  id          BIGSERIAL PRIMARY KEY,
  token       VARCHAR(255) NOT NULL UNIQUE,
  invited_by  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at  TIMESTAMP NOT NULL,
  used        BOOLEAN NOT NULL DEFAULT FALSE
);
