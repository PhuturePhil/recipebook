-- Zutaten und Arbeitsschritte behalten die Reihenfolge, in der sie eingegeben oder umsortiert wurden.
-- Bisher kamen Zutaten nach ID zurück und Schritte in zufälliger physischer Reihenfolge.

ALTER TABLE ingredients ADD COLUMN sort_order INTEGER;

UPDATE ingredients i
SET sort_order = ordered.pos
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY recipe_id ORDER BY id) - 1 AS pos
    FROM ingredients
) ordered
WHERE i.id = ordered.id;

-- Schritte haben keine ID; die aktuelle physische Reihenfolge ist die, die bisher angezeigt wurde
ALTER TABLE recipe_instructions ADD COLUMN sort_order INTEGER;

UPDATE recipe_instructions ri
SET sort_order = ordered.pos
FROM (
    SELECT ctid AS row_ref, ROW_NUMBER() OVER (PARTITION BY recipe_id ORDER BY ctid) - 1 AS pos
    FROM recipe_instructions
) ordered
WHERE ri.ctid = ordered.row_ref;
