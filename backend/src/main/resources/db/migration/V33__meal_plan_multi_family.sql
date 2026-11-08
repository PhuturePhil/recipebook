-- Essensplan für mehrere Familien (08.11.2026): Rezepte dürfen auch in Familien stehen, die sie nicht sehen
-- („nur über den Plan“). Einträge bleiben, bis jemand sie entfernt oder die Familie gelöscht wird – auch wenn das
-- Rezept gelöscht wird oder niemand in der Familie mehr Zugriff hat („Geist“). Dafür hält jeder Rezept-Eintrag
-- Titel, Quelle, Autor und Link fest; Bestandseinträge bekommen den aktuellen Stand ihres Rezepts.

ALTER TABLE meal_plan_entries ADD COLUMN snapshot_title VARCHAR(255);
ALTER TABLE meal_plan_entries ADD COLUMN snapshot_source VARCHAR(255);
ALTER TABLE meal_plan_entries ADD COLUMN snapshot_author VARCHAR(255);
ALTER TABLE meal_plan_entries ADD COLUMN snapshot_source_url VARCHAR(2048);

-- snapshot-backfill (MealPlanSnapshotMigrationTest führt genau diese Anweisung gegen H2 aus)
UPDATE meal_plan_entries SET
    snapshot_title = (SELECT r.title FROM recipes r WHERE r.id = meal_plan_entries.recipe_id),
    snapshot_source = (SELECT r.source FROM recipes r WHERE r.id = meal_plan_entries.recipe_id),
    snapshot_author = (SELECT r.author FROM recipes r WHERE r.id = meal_plan_entries.recipe_id),
    snapshot_source_url = (SELECT r.source_url FROM recipes r WHERE r.id = meal_plan_entries.recipe_id)
WHERE recipe_id IS NOT NULL;
-- end snapshot-backfill

-- Rezept gelöscht → Eintrag bleibt als Geist (recipe_id leer, Titel aus dem Snapshot)
ALTER TABLE meal_plan_entries DROP CONSTRAINT meal_plan_entries_recipe_id_fkey;
ALTER TABLE meal_plan_entries ADD CONSTRAINT meal_plan_entries_recipe_id_fkey
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE SET NULL;

-- Freitext ODER Rezept (das auch ein gelöschtes sein darf, dann mit Snapshot-Titel)
ALTER TABLE meal_plan_entries DROP CONSTRAINT meal_plan_entries_recipe_xor_text;
ALTER TABLE meal_plan_entries ADD CONSTRAINT meal_plan_entries_kind CHECK (
    (free_text IS NOT NULL AND recipe_id IS NULL AND snapshot_title IS NULL)
    OR (free_text IS NULL AND (recipe_id IS NOT NULL OR snapshot_title IS NOT NULL)));

-- Standard-Familie je Nutzer (nur für ihn selbst): erster Plan und Vorauswahl beim Planen
ALTER TABLE users ADD COLUMN default_family_id BIGINT REFERENCES families(id) ON DELETE SET NULL;
