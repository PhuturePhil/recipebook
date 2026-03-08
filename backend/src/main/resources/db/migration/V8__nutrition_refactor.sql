CREATE TABLE nutrition_ingredients (
  id            BIGSERIAL PRIMARY KEY,
  name_en       TEXT NOT NULL UNIQUE,
  calories_100g DOUBLE PRECISION,
  protein_100g  DOUBLE PRECISION,
  fat_100g      DOUBLE PRECISION,
  carbs_100g    DOUBLE PRECISION,
  fiber_100g    DOUBLE PRECISION,
  is_estimated  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE nutrition_ingredient_units (
  id               BIGSERIAL PRIMARY KEY,
  ingredient_id    BIGINT NOT NULL REFERENCES nutrition_ingredients(id),
  name_de          TEXT NOT NULL,
  unit_description TEXT NOT NULL,
  grams_per_unit   DOUBLE PRECISION NOT NULL,
  UNIQUE(ingredient_id, unit_description)
);
