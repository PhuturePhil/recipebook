-- Sicherheitspaket (18.10.2026)

-- M2: E-Mail-Adressen werden klein geschrieben gespeichert und verglichen. Unterscheiden sich zwei Konten nur in der
-- Schreibweise, bricht die Migration ab (dann muss jemand von Hand entscheiden, welches Konto bleibt).
DO $$
BEGIN
  IF EXISTS (SELECT lower(btrim(email)) FROM users GROUP BY lower(btrim(email)) HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'V31: Zwei Konten unterscheiden sich nur in der Schreibweise der E-Mail-Adresse – abgebrochen';
  END IF;
END $$;
UPDATE users SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
UPDATE pending_registrations SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));

-- M3: Nur bestätigte Adressen dürfen mit einem pastoors.cloud-Login verknüpft werden. Die bestehenden Konten hat die
-- Familie selbst angelegt, sie gelten als bestätigt.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET email_verified = TRUE;

-- N5: Anmeldungen gelten nur noch 30 statt 90 Tage; alle bisherigen Tokens enden, jeder meldet sich einmal neu an.
UPDATE users SET token_version = token_version + 1;

-- N6: Passwort-Links nur noch als SHA-256-Hash (wie beim Registrierungslink)
ALTER TABLE password_reset_tokens RENAME COLUMN token TO token_hash;
UPDATE password_reset_tokens SET token_hash = encode(sha256(convert_to(token_hash, 'UTF8')), 'hex');

-- M1: Eine neue E-Mail-Adresse gilt erst, wenn der Link an die neue Adresse geöffnet wurde
CREATE TABLE pending_email_changes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    new_email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_pending_email_changes_user ON pending_email_changes (user_id);

-- K1: KI-Tageskontingente je Konto (user_key „u:<id>“, Gäste gemeinsam „guest“), Tag nach deutscher Zeit
CREATE TABLE ai_usage (
    user_key VARCHAR(40) NOT NULL,
    day DATE NOT NULL,
    kind VARCHAR(20) NOT NULL,
    used INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_key, day, kind)
);

-- M6: KI-Vorschläge für den Nährwertkatalog, die Nicht-Admins auslösen, warten auf Freigabe (REVIEW → DONE/REJECTED)
ALTER TABLE ingredient_ai_request ADD COLUMN needs_review BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ingredient_ai_request ADD COLUMN requested_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE ingredient_ai_request ADD COLUMN reviewed_at TIMESTAMP;
ALTER TABLE ingredient_ai_request DROP CONSTRAINT chk_ingredient_ai_request_status;
ALTER TABLE ingredient_ai_request ADD CONSTRAINT chk_ingredient_ai_request_status
    CHECK (status IN ('PENDING', 'RUNNING', 'REVIEW', 'DONE', 'FAILED', 'REJECTED'));
