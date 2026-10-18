# Sicherheit

Stand: Sicherheitspaket vom 18.10.2026 (Version 2026-10-18.2), Grundlage war das Sicherheits-Audit vom 18.10.2026.

## Umgesetzt

- **KI-Kontingente (K1):** Pro Konto und Tag (deutsche Zeit) höchstens 10 Foto-Scans und 60 KI-Nebenaufrufe
  (Tags, Bildsuche, Übersetzung, Zuordnung neuer Zutaten); Gäste teilen sich 30. Admins und Hintergrundjobs sind
  ausgenommen. Zähler in `ai_usage` (`AiQuotaService`). Ein Scan hat höchstens 5 Fotos und 8 MB (Backend-Filter und
  nginx). Beschreibung höchstens 5.000, ein Zubereitungsschritt höchstens 2.000 Zeichen.
- **Profiländerung (M1):** Neue E-Mail-Adresse und neues Passwort nur mit dem aktuellen Passwort (5 Versuche pro
  15 Minuten und Konto). Die neue Adresse gilt erst nach dem Link an die neue Adresse (24 h, einmalig, nur als
  SHA-256-Hash gespeichert, `pending_email_changes`); danach enden alle Anmeldungen. Mit pastoors.cloud verknüpfte
  Konten ändern ihre Adresse nur dort. Konten, die nur per pastoors.cloud anmelden (`ssoOnly`), sehen im Profil
  kein Passwortfeld.
- **E-Mail-Schreibweise (M2):** Adressen werden klein geschrieben gespeichert und verglichen; Unique-Index auf
  `lower(email)`. Die Migration V31 bricht ab, wenn sich zwei Konten nur in der Schreibweise unterscheiden.
- **SSO-Verknüpfung (M3):** Ein bestehendes Konto wird nur verknüpft, wenn seine Adresse bestätigt ist
  (`users.email_verified`); beim ersten Verknüpfen enden alle bisherigen Anmeldungen.
- **Bildgrößen (M4):** Vor dem Dekodieren werden die Abmessungen aus dem Dateikopf gelesen; über 40 Megapixel wird
  abgelehnt (Speichern, Scan, Vorschaubild). JVM-Heap fest auf 640 MB.
- **Fremde Bildadressen (M5):** Rezeptbilder nur als hochgeladene Rasterbilder (Data-URL); keine Weiterleitung
  mehr auf fremde Adressen; CSP `img-src 'self' data: blob:` (blob: für die Fotovorschau beim Hochladen).
- **Nährwertkatalog (M6):** KI-Vorschläge, die Nicht-Admins auslösen, werden geprüft und warten als „REVIEW“ auf
  die Freigabe eines Admins (Tab „KI-Anfragen“); bis dahin gilt die Zutat als ausstehend.
- **Bestätigungsmails (N2):** Pro Adresse 3 je 15 Minuten, pro IP 3 je Stunde, insgesamt 30 je Stunde. Reißt der
  IP- oder Gesamtdeckel: WARN-Log und Alarm-Mail an pastoors.cloud@mail.de (höchstens eine pro Stunde und Art).
- **Quellen/Tags (N3):** Vorschläge und der Schreibweisen-Angleich nutzen nur Rezepte, die der Nutzer sehen darf.
- **Abmelden (N5):** Anmeldungen gelten 30 Tage; Abmelden beendet serverseitig alle Anmeldungen des Kontos
  (Token-Version, wie beim Passwortwechsel).
- **Passwort-Link (N6):** nur als SHA-256-Hash gespeichert.
- **Namen (N7b):** Den vollen Namen von Erstellern und Bewertenden sieht man nur bei sich selbst und innerhalb einer
  gemeinsamen Familie; sonst nur den Vornamen.
- **Bild-Uploads (N7c):** Geprüft wird die Dateisignatur (JPEG, PNG, GIF, WebP, AVIF) passend zur Typangabe; SVG
  bleibt verboten.
- **Infrastruktur (N8):** Speichergrenzen backend 1200 MB, db 512 MB, frontend 128 MB. Das Backend läuft als
  Nutzer `app`. Der nginx-Hauptprozess im Frontend bleibt root, weil er Port 443 öffnet (die Worker laufen als
  `nginx`); Postgres wechselt selbst auf den Nutzer `postgres`. `npm audit fix` hat vue (3.5.44) und source-map-js
  aktualisiert; offen bleibt nur `sharp` – ein Entwicklungswerkzeug für die App-Icons, nicht im ausgelieferten Build
  (das Update auf 0.35 wäre ein Breaking Change).

## Bewusste Entscheidungen (nicht geändert)

- **N1 Teilen-Links:** Widerrufen/Ersetzen darf weiterhin jeder, der das Rezept sehen darf – auch Fremde bei
  öffentlichen Rezepten (Entscheidung Philipp, 18.10.2026).
- **N4 Bewertungen:** Mehrere Bewertungen pro Nutzer und Rezept sind erlaubt. Wer spammt, wird gesperrt; bei starkem
  Wachstum neu bewerten (Entscheidung Philipp, 18.10.2026; Kommentar in `RatingService.addRating`).
- **N7a Login verrät SSO-Konto:** `/api/auth/login-method` und der Passwort-Login zeigen, dass eine Adresse ein
  pastoors.cloud-Konto ist. Die automatische SSO-Erkennung im Identifier-first-Login braucht genau diese Auskunft;
  das Rate-Limit (10 Anfragen pro Minute und IP) bremst das Abfragen vieler Adressen
  (Kommentar in `AuthService.loginMethod`).
