-- In Postgres eine generierte Spalte (V14); H2-Tests mit create-drop kennen sie sonst nicht
ALTER TABLE recipes ADD COLUMN image_hash VARCHAR(32);
