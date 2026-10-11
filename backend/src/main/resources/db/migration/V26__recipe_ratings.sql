-- Bewertungen: ganze Sterne 1–5 + optionaler Kommentar. Jede Abgabe ist eine eigene Zeile (Verlauf),
-- der Durchschnitt läuft über alle Zeilen. Gelöschte Nutzer: Bewertung bleibt anonym erhalten.

CREATE TABLE recipe_ratings (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    stars INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recipe_ratings_recipe_id ON recipe_ratings(recipe_id);
