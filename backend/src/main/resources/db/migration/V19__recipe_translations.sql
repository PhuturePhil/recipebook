-- Gespeicherte KI-Übersetzungen; das Original in recipes/ingredients bleibt unverändert.
-- ingredients: JSON-Array [{"amount","unit","name"}] in Originalreihenfolge, instructions: JSON-Array von Texten.
-- source_hash: SHA-256 über die Originalfelder; weicht er ab, wird beim nächsten Abruf neu übersetzt.
CREATE TABLE recipe_translations (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL CHECK (language IN ('de', 'en')),
    title TEXT NOT NULL,
    description TEXT,
    ingredients TEXT NOT NULL,
    instructions TEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    model VARCHAR(64),
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_recipe_translations_recipe_language UNIQUE (recipe_id, language)
);
