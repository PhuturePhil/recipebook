-- Attribution for recipe photos. Stock photos (Unsplash/Pexels) carry the photographer and the photo page,
-- uploads only their source; recipes from before this migration stay NULL.
ALTER TABLE recipes
    ADD COLUMN image_source VARCHAR(20),
    ADD COLUMN image_credit_name VARCHAR(255),
    ADD COLUMN image_credit_url TEXT,
    ADD COLUMN image_photo_url TEXT;
