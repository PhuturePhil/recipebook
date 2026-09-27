-- Optionale Zutatengruppe ("Salsa", "Für den Teig"); aufeinanderfolgende Zutaten mit gleichem Namen bilden eine Gruppe.
-- NULL = ohne Gruppe, wie bisher.

ALTER TABLE ingredients ADD COLUMN group_name VARCHAR(100);
