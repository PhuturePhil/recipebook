-- Kategorien pro Rezept (max. 5), automatisch per KI vergeben oder von Hand gepflegt
CREATE TABLE recipe_tags (
    recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL,
    tag VARCHAR(30) NOT NULL,
    PRIMARY KEY (recipe_id, sort_order)
);
