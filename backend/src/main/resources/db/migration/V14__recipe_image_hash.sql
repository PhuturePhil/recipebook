-- Hash of uploaded (data-URL) images, maintained by Postgres on write. The recipe list uses it as a
-- cache-busting version for /api/recipes/{id}/image without reading the image itself.
ALTER TABLE recipes
    ADD COLUMN image_hash VARCHAR(32)
    GENERATED ALWAYS AS (CASE WHEN image_url LIKE 'data:%' THEN md5(image_url) END) STORED;
