CREATE TABLE share_links (
  id          BIGSERIAL PRIMARY KEY,
  recipe_id   BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
  token       VARCHAR(64) NOT NULL UNIQUE,
  created_by  BIGINT REFERENCES users(id) ON DELETE SET NULL,
  created_at  TIMESTAMP NOT NULL,
  expires_at  TIMESTAMP NOT NULL,
  revoked     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_share_links_recipe_id ON share_links(recipe_id);
