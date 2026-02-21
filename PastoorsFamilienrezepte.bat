@echo off
chcp 65001 >nul
title Pastoors Familienrezepte - Starter

echo ========================================
echo   Pastoors Familienrezepte Starten
echo ========================================
echo.

echo [1/4] Starte PostgreSQL mit Docker...
docker-compose up -d postgres
if %errorlevel% neq 0 (
    echo FEHLER: Docker konnte nicht gestartet werden
    echo Bitte Docker Desktop starten und erneut versuchen
    pause
    exit /b 1
)
echo OK: PostgreSQL gestartet
echo.

echo [2/4] Warte auf PostgreSQL...
timeout /t 5 /nobreak
echo.

echo [3/4] Starte Backend (SpringBoot)...
start "Backend - SpringBoot" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"
echo.

echo [4/4] Starte Frontend (Vue.js)...
start "Frontend - Vue.js" cmd /k "cd /d %~dp0frontend && npm run dev"
echo.

echo ========================================
echo   Gestartet!
echo ========================================
echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo.
echo Diese Fenster muessen geoeffnet bleiben!
echo.
pause
