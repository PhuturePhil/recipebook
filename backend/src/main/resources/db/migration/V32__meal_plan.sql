-- Essensplan: gehört genau einer Familie, alle Mitglieder sehen und bearbeiten ihn. Pro Tag beliebig viele Einträge,
-- jeder ist entweder ein Rezept (muss für die Familie sichtbar sein, prüft das Backend) oder ein Freitext.
-- Rezept gelöscht oder Familie gelöscht → Einträge weg; Freigabe entzogen → räumt das Backend auf.

CREATE TABLE meal_plan_entries (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    recipe_id BIGINT REFERENCES recipes(id) ON DELETE CASCADE,
    free_text VARCHAR(120),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT meal_plan_entries_recipe_xor_text
        CHECK ((recipe_id IS NULL) <> (free_text IS NULL))
);

CREATE INDEX idx_meal_plan_entries_family_date ON meal_plan_entries(family_id, entry_date);
CREATE INDEX idx_meal_plan_entries_recipe_id ON meal_plan_entries(recipe_id);
