# RecipeBook - Pastoors Familienrezepte

## Live Demo

**URL:** http://89.167.44.213

## Schnellstart

### Mit einem Klick starten

Doppelklick auf `start.bat` - das startet:
1. PostgreSQL (Docker)
2. Backend (SpringBoot)
3. Frontend (Vue.js)

**Oder manuell:**

```bash
# Docker starten
docker-compose up -d

# Backend starten
cd backend
mvn spring-boot:run

# Frontend starten (neues Terminal)
cd frontend
npm run dev
```

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
- Backend: Java SpringBoot + JPA
- Datenbank: PostgreSQL (Docker)
