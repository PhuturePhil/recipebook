-- Teilen-Seiten: Aufrufe von Gästen ohne Login werden ohne Pseudonym gespeichert (Doku: docs/analytics.md)
ALTER TABLE usage_events ALTER COLUMN user_pseudonym DROP NOT NULL;
