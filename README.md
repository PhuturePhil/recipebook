# RecipeBook - Pastoors Familienrezepte

## Live Demo

**URL:** https://pastoors.cloud

## Schnellstart

**Voraussetzungen:**
- Docker (für PostgreSQL)
- Java 17+
- Node.js 18+

### Manuell starten

```bash
# 1. PostgreSQL starten
docker compose up -d

# 2. Backend starten (neues Terminal)
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run    # macOS/Linux/Git Bash
$env:SPRING_PROFILES_ACTIVE="local"; mvn spring-boot:run  # Windows PowerShell

# 3. Frontend starten (neues Terminal)
cd frontend
npm install
npm run dev
```

> Das Backend-Profil `local` lädt automatisch `application-local.properties` mit den nötigen Umgebungsvariablen (JWT_SECRET, ADMIN_PASSWORD etc.).

## URLs

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080/api/recipes
- **Datenbank:** localhost:5433 (PostgreSQL)

## Login mit pastoors.cloud (OIDC)

Neben E-Mail + Passwort gibt es den Button **„Mit pastoors.cloud anmelden"**. Anmeldung läuft über Authelia (`https://auth.pastoors.cloud`) als OIDC-Provider; danach bekommt man das ganz normale App-JWT.

- Zuordnung über die E-Mail-Adresse (Groß-/Kleinschreibung egal): vorhandenes Konto wird verknüpft (Spalte `users.oidc_subject`, Rolle und Passwort bleiben), unbekannte Adresse → neues Konto mit Rolle `USER`, Vor-/Nachname aus den Claims.
- Wer sich per OIDC anmelden darf, entscheidet Authelia (`authorization_policy` des Clients `recipebook`, aktuell Gruppen `familie` + `rezepte`).
- Passwort-Login, Einladungslinks und Share-Links funktionieren unverändert.
- Ist OIDC aus (Standard) oder Authelia nicht erreichbar, wird der Button ausgeblendet bzw. man landet mit Hinweis wieder auf der Login-Seite. Das Backend startet auch ohne Authelia.

| Variable | Prod-Wert | Bedeutung |
|---|---|---|
| `OIDC_ENABLED` | `true` | schaltet OIDC ein (Standard: `false`) |
| `OIDC_ISSUER_URI` | `https://auth.pastoors.cloud` | Issuer (Discovery unter `/.well-known/openid-configuration`) |
| `OIDC_CLIENT_ID` | `recipebook` | Client-ID in Authelia |
| `OIDC_CLIENT_SECRET` | *(geheim, nur in `/opt/recipebook/.env`)* | Client-Secret im Klartext; in Authelia liegt nur der argon2-Hash |
| `OIDC_REDIRECT_URI` | *(leer lassen)* | Standard `${APP_URL}/api/auth/oidc/callback` |

In Prod setzt `deploy.yml` die Variablen im Backend-Container; `OIDC_ENABLED` und `OIDC_CLIENT_SECRET` kommen aus `/opt/recipebook/.env`. Lokal (Backend auf `localhost:8080`) ist die Redirect-URI `http://localhost:8080/api/auth/oidc/callback` in Authelia bereits registriert.

## Funktionen

- Rezepte erstellen, bearbeiten, löschen
- Bild-Upload für Rezepte
- Zutatenmengen skalieren (Personenanzahl ändern)
- Suchfunktion
- Zufällige Food-Bilder wenn kein Bild hochgeladen

## Technologie

- Frontend: Vue 3 + Vite + Pinia + Vue Router
- Backend: Java SpringBoot + JPA + Flyway
- Datenbank: PostgreSQL (Docker)
