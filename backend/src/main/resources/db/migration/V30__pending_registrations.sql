-- Selbst-Registrierung: Das Konto entsteht erst, wenn der Link aus der Bestätigungs-Mail geöffnet wird.
-- Bis dahin liegt die Anmeldung hier. Gespeichert wird nur der SHA-256-Hash des Tokens, das Passwort als BCrypt-Hash.
-- Jede Registrierung hat ihr eigenes Passwort, damit niemand ein fremdes Postfach mit seinem Passwort vorbelegen kann.

CREATE TABLE pending_registrations (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    vorname VARCHAR(100) NOT NULL,
    nachname VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_pending_registrations_email ON pending_registrations (lower(email));
CREATE INDEX idx_pending_registrations_expires_at ON pending_registrations (expires_at);
