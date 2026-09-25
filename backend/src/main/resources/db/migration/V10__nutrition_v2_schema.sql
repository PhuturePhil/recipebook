-- Nährwerte v2: Referenzdaten (BLS), neuer Zutatenkatalog pro 100 g, Umrechnungstabelle, KI-Anfragen.
-- Die alten Strukturen (ingredient_catalog, recipes.nutrition_*) bleiben unverändert stehen,
-- damit ein Rollback auf das vorherige Image ohne Datenbank-Eingriff möglich ist.
-- Sie werden vom neuen Code nicht mehr beschrieben und in einer späteren Migration entfernt.

CREATE TABLE nutrient_dataset (
  id           BIGSERIAL PRIMARY KEY,
  source_key   VARCHAR(20)  NOT NULL UNIQUE,
  name         VARCHAR(255) NOT NULL,
  version      VARCHAR(50)  NOT NULL,
  publisher    VARCHAR(255) NOT NULL,
  license      VARCHAR(100) NOT NULL,
  license_url  VARCHAR(255),
  citation     TEXT         NOT NULL,
  source_url   VARCHAR(255),
  doi          VARCHAR(100),
  row_count    INTEGER      NOT NULL DEFAULT 0,
  imported_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reference_food (
  code           VARCHAR(20) PRIMARY KEY,
  dataset_id     BIGINT       NOT NULL REFERENCES nutrient_dataset(id),
  name_de        VARCHAR(500) NOT NULL,
  name_en        VARCHAR(500),
  kcal           DOUBLE PRECISION,
  kj             DOUBLE PRECISION,
  water          DOUBLE PRECISION,
  protein        DOUBLE PRECISION,
  fat            DOUBLE PRECISION,
  carbs          DOUBLE PRECISION,
  fiber          DOUBLE PRECISION,
  sugar          DOUBLE PRECISION,
  salt           DOUBLE PRECISION,
  alcohol        DOUBLE PRECISION,
  saturated_fat  DOUBLE PRECISION,
  micronutrients JSONB
);

CREATE INDEX idx_reference_food_dataset ON reference_food(dataset_id);

CREATE TABLE nutrition_ingredient (
  id               BIGSERIAL PRIMARY KEY,
  name             VARCHAR(255) NOT NULL,
  name_key         VARCHAR(255) NOT NULL UNIQUE,
  ingredient_class VARCHAR(30)  NOT NULL DEFAULT 'DEFAULT',
  source           VARCHAR(20)  NOT NULL,
  reference_code   VARCHAR(20) REFERENCES reference_food(code),
  negligible       BOOLEAN      NOT NULL DEFAULT FALSE,
  kcal             DOUBLE PRECISION,
  protein          DOUBLE PRECISION,
  fat              DOUBLE PRECISION,
  carbs            DOUBLE PRECISION,
  fiber            DOUBLE PRECISION,
  sugar            DOUBLE PRECISION,
  salt             DOUBLE PRECISION,
  note             TEXT,
  updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_nutrition_ingredient_source CHECK (source IN ('BLS', 'MANUAL', 'AI_ESTIMATE')),
  CONSTRAINT chk_nutrition_ingredient_bls CHECK (source <> 'BLS' OR reference_code IS NOT NULL)
);

CREATE TABLE nutrition_ingredient_alias (
  id            BIGSERIAL PRIMARY KEY,
  alias_key     VARCHAR(255) NOT NULL UNIQUE,
  alias         VARCHAR(255) NOT NULL,
  ingredient_id BIGINT       NOT NULL REFERENCES nutrition_ingredient(id) ON DELETE CASCADE,
  origin        VARCHAR(20)  NOT NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nutrition_ingredient_alias_ingredient ON nutrition_ingredient_alias(ingredient_id);

CREATE TABLE nutrition_ingredient_legacy_link (
  legacy_catalog_id BIGINT PRIMARY KEY,
  ingredient_id     BIGINT NOT NULL REFERENCES nutrition_ingredient(id) ON DELETE CASCADE,
  per100_basis      TEXT
);

CREATE TABLE unit_conversion (
  id               BIGSERIAL PRIMARY KEY,
  unit             VARCHAR(50)  NOT NULL,
  ingredient_class VARCHAR(30),
  ingredient_id    BIGINT REFERENCES nutrition_ingredient(id) ON DELETE CASCADE,
  grams            DOUBLE PRECISION NOT NULL,
  source           VARCHAR(20)  NOT NULL,
  note             VARCHAR(255),
  updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_unit_conversion UNIQUE NULLS NOT DISTINCT (unit, ingredient_class, ingredient_id),
  CONSTRAINT chk_unit_conversion_grams CHECK (grams > 0),
  CONSTRAINT chk_unit_conversion_scope CHECK (ingredient_class IS NULL OR ingredient_id IS NULL)
);

CREATE TABLE ingredient_ai_request (
  id              BIGSERIAL PRIMARY KEY,
  kind            VARCHAR(20)  NOT NULL,
  request_key     VARCHAR(400) NOT NULL,
  ingredient_name VARCHAR(255) NOT NULL,
  unit            VARCHAR(50),
  ingredient_id   BIGINT REFERENCES nutrition_ingredient(id) ON DELETE CASCADE,
  status          VARCHAR(20)  NOT NULL,
  attempts        INTEGER      NOT NULL DEFAULT 0,
  requested_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_attempt_at TIMESTAMP,
  error           TEXT,
  result_json     TEXT,
  CONSTRAINT uq_ingredient_ai_request UNIQUE (kind, request_key),
  CONSTRAINT chk_ingredient_ai_request_kind CHECK (kind IN ('NAME_MATCH', 'UNIT_WEIGHT')),
  CONSTRAINT chk_ingredient_ai_request_status CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED'))
);

COMMENT ON COLUMN recipes.nutrition_kcal IS 'DEPRECATED seit V10: Nährwerte werden zur Laufzeit berechnet';
COMMENT ON COLUMN recipes.nutrition_fat IS 'DEPRECATED seit V10';
COMMENT ON COLUMN recipes.nutrition_protein IS 'DEPRECATED seit V10';
COMMENT ON COLUMN recipes.nutrition_carbs IS 'DEPRECATED seit V10';
COMMENT ON COLUMN recipes.nutrition_fiber IS 'DEPRECATED seit V10';
COMMENT ON TABLE ingredient_catalog IS 'DEPRECATED seit V10: Altkatalog (KI-Werte pro Einheit), nur noch lesend als Archiv';
