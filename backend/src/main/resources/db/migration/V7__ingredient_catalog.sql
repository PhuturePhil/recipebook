CREATE TABLE ingredient_catalog (
  id                BIGSERIAL PRIMARY KEY,
  name              VARCHAR(255) NOT NULL,
  unit              VARCHAR(50)  NOT NULL DEFAULT '',
  nutrition_kcal    DOUBLE PRECISION,
  nutrition_fat     DOUBLE PRECISION,
  nutrition_protein DOUBLE PRECISION,
  nutrition_carbs   DOUBLE PRECISION,
  nutrition_fiber   DOUBLE PRECISION,
  CONSTRAINT uq_ingredient_catalog_name_unit UNIQUE (name, unit)
);
