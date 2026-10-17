# Nutzungsstatistik (Events)

Selbst gehostet, seit Version 2026-10-17.1. Keine Statistik-Seite – ausgewertet wird per SQL direkt auf der DB
(`docker exec -it recipebook-db psql -U postgres recipebook`).

## Datenschutz

- Erfasst werden eingeloggte Nutzer in der App. Auf öffentlichen `/share/`-Seiten (seit 2026-10-17.2) nur der Aufruf
  selbst (`teilen_link_aufgerufen`), sonst nichts; Gäste ohne Login dabei **ohne** Pseudonym (`user_pseudonym` leer).
- Teilen-Token landen nie in der Tabelle: der Client schickt den Token mit, der Server ersetzt ihn durch `recipeId`,
  `shareLinkId` und `linkRef` = die ersten 12 Hex-Zeichen von SHA-256(Token). Daraus lässt sich kein gültiger Link
  zurückgewinnen, man kann aber Aufrufe desselben Links (auch ungültiger) zusammenzählen und mit `share_links` verbinden.
- `user_pseudonym` = HMAC-SHA256 der Nutzer-ID mit dem Secret `ANALYTICS_SALT` (nur in `/opt/recipebook/.env`).
  Kein Name, keine E-Mail, keine rohe Nutzer-ID, keine IP-Adresse in der Tabelle.
- Ohne `ANALYTICS_SALT` (oder kürzer als 16 Zeichen) verwirft das Backend alle Events (auch die anonymen).
- Ein neues Salt erzeugt neue Pseudonyme – alte und neue Zeilen lassen sich dann nicht mehr verbinden.
- Suchtexte werden gespeichert, Bewertungskommentare nicht (nur `mitKommentar`).

## Tabelle `usage_events` (Migration V27)

| Spalte | Typ | Inhalt |
|---|---|---|
| id | bigserial | |
| app | varchar(40) | `rezepte` (Format ist für die anderen Apps gedacht) |
| event | varchar(60) | Event-Name, siehe unten |
| ts | timestamptz | Zeitpunkt auf dem Gerät (unplausibel = mehr als 5 Min. in der Zukunft oder älter als 7 Tage → Serverzeit) |
| received_at | timestamptz | Eingang am Server |
| user_pseudonym | varchar(64) | HMAC-Hex, siehe oben; leer (NULL) bei Gästen auf der Teilen-Seite (seit V28) |
| session_id | varchar(64) | zufällig pro App-Besuch: neuer Tab/Start oder mehr als 30 Min. im Hintergrund = neue Sitzung |
| device | varchar(10) | `mobile` / `desktop` (Mobil-UA, iPad, oder Fenster < 1024 px = mobile) |
| display | varchar(10) | `pwa` (display-mode standalone) / `browser` |
| props | jsonb | eventspezifische Felder, nur flache Werte (Text ≤ 300 Zeichen, Zahl, Wahrheitswert) |

Endpoint: `POST /api/events` mit JWT, Body `{"events":[{"event","ts","sessionId","device","display","props"}]}`,
höchstens 50 Events pro Aufruf, 60 Aufrufe pro Minute und Nutzer. Ungültige Events werden still verworfen.

Ausnahme ohne Login: `POST /api/events/public` (gleiches Format) – nur für Gäste auf der Teilen-Seite. Angenommen wird
ausschließlich `teilen_link_aufgerufen` mit einem `token` im Format eines Teilen-Tokens (16–64 Zeichen `A-Za-z0-9_-`);
alle anderen Events und props fallen weg, gespeichert wird ohne Pseudonym. Höchstens 5 Events pro Aufruf, Body bis
4 KB (sonst 413), 20 Aufrufe pro Minute je Client und 300 pro Minute insgesamt (sonst 429). Die IP dient nur als
flüchtiger Schlüssel dieses Rate-Limits im Arbeitsspeicher und wird nirgends gespeichert. `teilen_login` kommt immer
nach dem Login, also mit JWT über `/api/events` – dafür braucht es keine Ausnahme.

Client: `frontend/src/utils/analytics.js` sammelt in einer Warteschlange (localStorage) und schickt gebündelt per
`fetch(keepalive)` – alle 30 s, ab 20 Events, beim Verdecken der App und beim Schließen. Fehlgeschlagene
Sendungen (offline, 5xx, 429) bleiben in der Warteschlange. Der anonyme Teilen-Aufruf geht ohne Warteschlange sofort
raus (geht er schief, fehlt er).

## Events

Dauern immer in Millisekunden. `sichtbar_ms` = Zeit, in der die Seite sichtbar war (Uhr pausiert im Hintergrund
bzw. bei Display aus); `gesamt_ms` = Spanne vom Öffnen bis zum Verlassen inkl. Unterbrechungen.

| event | props |
|---|---|
| rezept_geoeffnet | `recipeId`, `herkunft` (`liste`, `suche`, `teilen_link`, `direkt`, `formular` = nach Speichern/Abbrechen im Formular, `sonstige`), `besuch` |
| rezept_verlassen | `recipeId`, `besuch`, `sichtbar_ms` (ungekappt), `gesamt_ms` (gekappt bei 6 h), optional `abschluss` |
| suche | `query` (Suchbegriffe ohne Tag-Filter, kommagetrennt), `treffer` – nach 1,2 s Tipppause, bei Enter oder beim Leeren/Verlassen; gleiche Suche nicht doppelt |
| filter_gesetzt | `filter` (`tag`, `sortierung`), `wert` (Tag-Name bzw. `alle`; Sortierung `default`, `newest`, …), optional `quelle: 'rezept'` (Tag auf der Rezeptseite angetippt) |
| portionen_geaendert | `recipeId`, `von`, `auf` – mehrere schnelle Klicks (1,5 s Pause) ergeben ein Event |
| sprache_gewechselt | `recipeId`, `richtung` (`en→de` = Deutsch-Übersetzung gewählt, `de→en` = Original) |
| bewertung_abgegeben | `recipeId`, `sterne`, `mitKommentar` |
| teilen_link_erstellt | `recipeId`, `erneuert` (true = neuer Link ersetzt den alten) |
| rezept_angelegt / rezept_bearbeitet | `recipeId` |
| seite_geoeffnet | `seite` (`zutaten`, `naehrwerte_badges`, `changelog`, `familien`, `familie`), `sichtbar_ms` (gekappt bei 30 Min.) – gesendet beim Verlassen |
| pdf_gespeichert | `recipeId` – Klick auf „Als PDF speichern“ (ob danach wirklich gespeichert oder gedruckt wird, sieht die App nicht) |
| teilen_link_aufgerufen | `recipeId`, `shareLinkId` (beide nur bei bekanntem Link), `linkRef`, `link` (`gueltig`, `abgelaufen`, `zurueckgezogen`, `ungueltig` = unbekannter Token), `status` (`eingeloggt` = mit Pseudonym, `anonym` = Gast ohne Pseudonym) – einmal pro Sitzung und Link (Neuladen zählt nicht doppelt) |
| teilen_login | `recipeId`, `shareLinkId`, `linkRef`, `link` wie oben = der zuletzt als Gast geöffnete Teilen-Link, `seit_ms` = Zeit vom Teilen-Aufruf bis zum Login – wenn sich ein Gast im selben Tab danach anmeldet (Herkunft in sessionStorage `an:share-guest`, einmalig) |
| familie_angelegt | – (ohne Familienname/-id) |
| einladung_angenommen | – Beitritt über einen Familien-Einladungslink (ohne Code, ohne Familie) |
| popup_neuerungen | `aktion`: `angezeigt` (mit `version`, `eintraege`) beim Öffnen; beim Schließen `weggeklickt` (`wie`: `knopf`, `daneben`, `esc`) oder `alle_neuerungen`, jeweils mit `version` und `sichtbar_ms` (gekappt bei 30 Min.) |

`besuch` verbindet `rezept_geoeffnet` und `rezept_verlassen` eines Rezeptbesuchs.

`abschluss` (bei `rezept_verlassen`, `seite_geoeffnet`, `popup_neuerungen`): fehlt = normal verlassen;
`app_geschlossen` = Tab/App geschlossen bzw. neu geladen; `abgebrochen` = die App wurde im Hintergrund vom System
beendet und hat den Besuch beim nächsten Start mit dem Stand beim Verdecken nachgemeldet (Dauern sind dann eine
Untergrenze, `ts` = Zeitpunkt des Verdeckens).

`teilen_link` als Herkunft kommt nur vor, wenn jemand von einer geöffneten Share-Seite aus in der App ein Rezept
öffnet. Von der Share-Seite selbst wird nur `teilen_link_aufgerufen` erfasst (Portionen, Sprache usw. dort nicht).

Gast-Aufruf und späteres `teilen_login` tragen dieselbe `session_id` (gleicher Tab) – so lässt sich ein Login einem
Gast-Aufruf zuordnen. Das `teilen_login` selbst nennt den Link ohnehin.

## Beispiel-Queries

Top-Rezepte der letzten 30 Tage (Öffnungen, verschiedene Nutzer, Titel aus `recipes`):

```sql
SELECT (e.props->>'recipeId')::bigint AS recipe_id, r.title,
       count(*) AS oeffnungen, count(DISTINCT e.user_pseudonym) AS nutzer
FROM usage_events e
LEFT JOIN recipes r ON r.id = (e.props->>'recipeId')::bigint
WHERE e.app = 'rezepte' AND e.event = 'rezept_geoeffnet' AND e.ts > now() - interval '30 days'
GROUP BY 1, 2
ORDER BY oeffnungen DESC
LIMIT 20;
```

Events pro Pseudonym und Event-Typ:

```sql
SELECT left(user_pseudonym, 8) AS nutzer, event, count(*)
FROM usage_events
WHERE app = 'rezepte'
GROUP BY 1, 2
ORDER BY 1, 3 DESC;
```

Verweildauer pro Rezept (Median sichtbar und gesamt, in Minuten):

```sql
SELECT (props->>'recipeId')::bigint AS recipe_id, count(*) AS besuche,
       round((percentile_cont(0.5) WITHIN GROUP (ORDER BY (props->>'sichtbar_ms')::bigint) / 60000)::numeric, 1) AS median_sichtbar_min,
       round((percentile_cont(0.5) WITHIN GROUP (ORDER BY (props->>'gesamt_ms')::bigint) / 60000)::numeric, 1) AS median_gesamt_min
FROM usage_events
WHERE app = 'rezepte' AND event = 'rezept_verlassen'
GROUP BY 1
ORDER BY besuche DESC;
```

Herkunft der Rezeptaufrufe und Anteil mobil/PWA:

```sql
SELECT props->>'herkunft' AS herkunft, device, display, count(*)
FROM usage_events
WHERE app = 'rezepte' AND event = 'rezept_geoeffnet'
GROUP BY 1, 2, 3
ORDER BY 4 DESC;
```

Häufigste Suchen ohne Treffer:

```sql
SELECT lower(props->>'query') AS suche, count(*)
FROM usage_events
WHERE app = 'rezepte' AND event = 'suche' AND (props->>'treffer')::int = 0
GROUP BY 1
ORDER BY 2 DESC
LIMIT 20;
```

PDF-Klicks pro Rezept:

```sql
SELECT (e.props->>'recipeId')::bigint AS recipe_id, r.title, count(*) AS pdf_klicks
FROM usage_events e
LEFT JOIN recipes r ON r.id = (e.props->>'recipeId')::bigint
WHERE e.app = 'rezepte' AND e.event = 'pdf_gespeichert'
GROUP BY 1, 2
ORDER BY 3 DESC;
```

Aufrufe pro Teilen-Link (Gäste vs. eingeloggt, ungültige Links über `linkRef`):

```sql
SELECT e.props->>'linkRef' AS link_ref, (e.props->>'shareLinkId')::bigint AS share_link_id, r.title,
       e.props->>'link' AS zustand,
       count(*) FILTER (WHERE e.props->>'status' = 'anonym') AS gaeste,
       count(*) FILTER (WHERE e.props->>'status' = 'eingeloggt') AS eingeloggt,
       min(e.ts) AS erster_aufruf, max(e.ts) AS letzter_aufruf
FROM usage_events e
LEFT JOIN recipes r ON r.id = (e.props->>'recipeId')::bigint
WHERE e.app = 'rezepte' AND e.event = 'teilen_link_aufgerufen'
GROUP BY 1, 2, 3, 4
ORDER BY count(*) DESC;
```

Einen bekannten Link wiederfinden (Token → `linkRef`, nur zum Nachschlagen, gespeichert wird der Token nie):

```sql
SELECT id, recipe_id, left(encode(sha256(convert_to(token, 'UTF8')), 'hex'), 12) AS link_ref FROM share_links;
```

Konversion Gast → Login je Teilen-Link (Gast-Sitzungen mit Aufruf, davon mit anschließendem Login im selben Tab):

```sql
WITH gast AS (
  SELECT DISTINCT props->>'linkRef' AS link_ref, session_id
  FROM usage_events
  WHERE app = 'rezepte' AND event = 'teilen_link_aufgerufen' AND props->>'status' = 'anonym'
), login AS (
  SELECT DISTINCT props->>'linkRef' AS link_ref, session_id, user_pseudonym
  FROM usage_events
  WHERE app = 'rezepte' AND event = 'teilen_login'
)
SELECT g.link_ref, count(*) AS gast_sitzungen, count(l.session_id) AS danach_eingeloggt,
       round(100.0 * count(l.session_id) / count(*), 1) AS quote_prozent
FROM gast g
LEFT JOIN login l ON l.link_ref = g.link_ref AND l.session_id = g.session_id
GROUP BY 1
ORDER BY 2 DESC;
```

Logins über Teilen-Links insgesamt, mit Median der Zeit bis zum Login (Minuten):

```sql
SELECT count(*) AS logins, count(DISTINCT user_pseudonym) AS nutzer,
       round((percentile_cont(0.5) WITHIN GROUP (ORDER BY (props->>'seit_ms')::bigint) / 60000)::numeric, 1) AS median_min
FROM usage_events
WHERE app = 'rezepte' AND event = 'teilen_login';
```

Testdaten entfernen (z. B. nach einem Smoke-Test, Pseudonym aus einem Test-Event):

```sql
DELETE FROM usage_events WHERE user_pseudonym = '<pseudonym>' AND ts > '<start des tests>';
-- anonyme Test-Aufrufe (kein Pseudonym) über die Sitzung des Test-Browsers:
DELETE FROM usage_events WHERE session_id = '<session_id>' AND user_pseudonym IS NULL;
```
