-- Nutzungsstatistik: generisches Event-Format (auch für die anderen Apps gedacht).
-- Kein Klarname, keine rohe Nutzer-ID, keine IP: user_pseudonym = HMAC-SHA256(Nutzer-ID, ANALYTICS_SALT).
-- Alles Eventspezifische (recipeId, Suchtext, Dauern …) steht in props. Doku: docs/analytics.md

CREATE TABLE usage_events (
    id BIGSERIAL PRIMARY KEY,
    app VARCHAR(40) NOT NULL,
    event VARCHAR(60) NOT NULL,
    ts TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_pseudonym VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    device VARCHAR(10),
    display VARCHAR(10),
    props JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_usage_events_app_event_ts ON usage_events(app, event, ts);
CREATE INDEX idx_usage_events_pseudonym ON usage_events(user_pseudonym);
CREATE INDEX idx_usage_events_ts ON usage_events(ts);
