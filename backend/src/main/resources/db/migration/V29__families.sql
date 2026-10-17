-- Familien: Gruppen von Nutzern, die Rezepte miteinander teilen. Alle Mitglieder sind gleichberechtigt,
-- höchstens 10 Mitglieder (prüft das Backend). Verlässt das letzte Mitglied die Familie, wird sie gelöscht.
-- Pro Familie gibt es genau einen Einladungslink (mehrfach nutzbar, 14 Tage gültig); ein neuer ersetzt den alten.

CREATE TABLE families (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invite_code VARCHAR(64) UNIQUE,
    invite_expires_at TIMESTAMP,
    invite_created_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE family_members (
    family_id BIGINT NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (family_id, user_id)
);

CREATE INDEX idx_family_members_user_id ON family_members(user_id);

-- Sichtbarkeit pro Rezept: privat (keine Zeile, nicht öffentlich), für Familien (Zeilen hier), öffentlich (is_public)
CREATE TABLE recipe_families (
    recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    family_id BIGINT NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    PRIMARY KEY (recipe_id, family_id)
);

CREATE INDEX idx_recipe_families_family_id ON recipe_families(family_id);

ALTER TABLE recipes ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;

-- Bestand: Familie Pastoors mit allen heutigen Nutzern, alle Bestandsrezepte für sie freigegeben
INSERT INTO families (name) VALUES ('Familie Pastoors');

INSERT INTO family_members (family_id, user_id)
SELECT f.id, u.id FROM families f CROSS JOIN users u WHERE f.name = 'Familie Pastoors';

INSERT INTO recipe_families (recipe_id, family_id)
SELECT r.id, f.id FROM recipes r CROSS JOIN families f WHERE f.name = 'Familie Pastoors';
