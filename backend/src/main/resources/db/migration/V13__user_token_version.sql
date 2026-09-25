-- Existing JWTs have no version claim and are treated as version 0, so current sessions stay valid.
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
