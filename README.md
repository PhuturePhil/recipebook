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
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

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
