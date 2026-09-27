-- Sprache des Originals pro Rezept; language_auto = true heißt: beim Speichern neu erkennen
ALTER TABLE recipes ADD COLUMN language VARCHAR(2) NOT NULL DEFAULT 'de';
ALTER TABLE recipes ADD CONSTRAINT recipes_language_check CHECK (language IN ('de', 'en'));
ALTER TABLE recipes ADD COLUMN language_auto BOOLEAN NOT NULL DEFAULT TRUE;
