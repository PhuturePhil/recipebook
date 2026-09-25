ALTER TABLE users ADD COLUMN oidc_subject VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uk_users_oidc_subject UNIQUE (oidc_subject);
