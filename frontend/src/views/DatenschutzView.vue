<template>
  <main class="legal-page">
    <h1>Datenschutzerklärung</h1>
    <p class="legal-page__stand">Stand: 17. Oktober 2026</p>

    <h2>Wer ist verantwortlich?</h2>
    <p>
      {{ OPERATOR_NAME }}, E-Mail <a :href="`mailto:${CONTACT_EMAIL}`">{{ CONTACT_EMAIL }}</a>.
      Pastoors Familienrezepte ist ein privates, nicht-kommerzielles Projekt. Wir zeigen keine Werbung und geben keine Daten weiter, um damit Geld zu verdienen.
    </p>

    <h2>Welche Daten wir speichern</h2>
    <ul>
      <li>
        <strong>Konto:</strong> E-Mail-Adresse, Vor- und Nachname (Profilname) und dein Passwort – nur als
        verschlüsselter Hash, das Passwort selbst kennen wir nicht. Eine Registrierung wartet höchstens 24 Stunden auf
        deine Bestätigung und wird danach gelöscht.
      </li>
      <li>
        <strong>Inhalte:</strong> Rezepte, Fotos, Bewertungen und Kommentare, Familien-Mitgliedschaften und Teilen-Links.
        An Rezepten und Bewertungen steht dein Vorname bzw. Profilname – sichtbar für alle, die das Rezept sehen dürfen,
        bei öffentlichen Rezepten also für jeden.
      </li>
      <li>
        <strong>Nutzungsstatistik:</strong> Damit wir die App verbessern können, zählen wir, welche Funktionen genutzt
        werden (zum Beispiel Rezept geöffnet, Suchbegriff, Portionen geändert, PDF gespeichert). Statt deines Namens trägt
        jeder Eintrag ein Pseudonym, dazu Gerätetyp (Handy oder Computer) und eine zufällige Sitzungskennung.
        IP-Adressen speichert die Statistik nicht. Aufrufe von Teilen-Links durch Gäste zählen wir ganz ohne Pseudonym.
      </li>
      <li>
        <strong>Server-Protokoll:</strong> Der Webserver notiert bei jedem Aufruf technisch IP-Adresse, Zeitpunkt,
        aufgerufene Adresse und Browser-Kennung. Das Protokoll dient nur der Fehlersuche und der Abwehr von Angriffen,
        wird nicht ausgewertet und fortlaufend überschrieben. Gegen das Durchprobieren von Passwörtern merkt sich der
        Server IP-Adressen zusätzlich für eine Minute im Arbeitsspeicher.
      </li>
    </ul>

    <h2>Wofür und auf welcher Grundlage</h2>
    <ul>
      <li>Konto und Inhalte, damit du die App nutzen kannst (Art. 6 Abs. 1 lit. b DSGVO).</li>
      <li>Statistik, Server-Protokoll und Missbrauchsschutz aus unserem berechtigten Interesse an einer sicheren, gut funktionierenden App (Art. 6 Abs. 1 lit. f DSGVO).</li>
    </ul>

    <h2>Cookies und Speicher im Browser</h2>
    <p>
      Es gibt keine Werbe- oder Tracking-Cookies. Die App speichert in deinem Browser (localStorage und sessionStorage)
      nur, was sie technisch braucht: dein Anmelde-Token (JWT), die gewählte Darstellung hell oder dunkel, welche Neuerungen
      du schon gesehen hast, und Statistik-Einträge, bis sie gesendet sind. Beim Abmelden wird das Token gelöscht.
      Wer sich mit dem Familien-Login „pastoors.cloud“ anmeldet, bekommt dafür ein technisch nötiges Sitzungs-Cookie
      auf auth.pastoors.cloud.
    </p>

    <h2>Wer sonst mit den Daten zu tun hat</h2>
    <ul>
      <li>
        <strong>Hosting:</strong> Die App läuft auf einem gemieteten Server der Hetzner Online GmbH (Gunzenhausen,
        Deutschland) in deren Rechenzentrum in Helsinki (Finnland, EU). Dort liegen auch die Sicherungskopien der
        Datenbank (täglich und vor jedem größeren Update).
      </li>
      <li>
        <strong>E-Mail:</strong> Bestätigungs- und Passwort-Mails verschicken wir über das Postfach {{ CONTACT_EMAIL }}
        beim Anbieter mail.de (Deutschland).
      </li>
      <li>
        <strong>KI-Funktionen:</strong> Für Übersetzungen, das Einlesen von Rezeptfotos, Zutaten-Vorschläge und
        automatische Rezeptbilder schickt der Server Rezepttext bzw. das Foto an OpenAI (USA). Name und E-Mail-Adresse
        werden dabei nicht übermittelt. Automatische Rezeptbilder sucht der Server mit Stichworten bei Unsplash bzw.
        Pexels und speichert das Bild bei uns – dein Browser lädt nichts von dort.
      </li>
      <li>
        Darüber hinaus nutzen wir keine fremden Dienste: keine Analyse-, Werbe- oder Social-Media-Dienste, keine Schriften
        oder Skripte von fremden Servern.
      </li>
    </ul>

    <h2>Wie lange</h2>
    <p>
      Konto und Inhalte bleiben gespeichert, bis du dein Konto löschen lässt. Die pseudonyme Nutzungsstatistik bewahren
      wir auf, solange sie uns bei der Weiterentwicklung hilft.
    </p>

    <h2>Deine Rechte</h2>
    <p>
      Du kannst Auskunft über deine Daten verlangen, sie berichtigen oder löschen lassen, die Verarbeitung einschränken,
      ihr widersprechen und deine Daten in einem gängigen Format bekommen (Art. 15–21 DSGVO). Eine kurze Mail an
      <a :href="`mailto:${CONTACT_EMAIL}`">{{ CONTACT_EMAIL }}</a> genügt, zum Beispiel „Bitte lösche mein Konto“.
      Außerdem kannst du dich bei einer Datenschutz-Aufsichtsbehörde beschweren, etwa bei der Landesbeauftragten für
      Datenschutz und Informationsfreiheit Nordrhein-Westfalen.
    </p>

    <h2>Inhalte melden</h2>
    <p>
      Findest du auf einem öffentlichen Rezept oder über einen Teilen-Link etwas, das Rechte verletzt, nutze
      „Inhalt melden“ unten auf der Seite oder schreib an <a :href="`mailto:${CONTACT_EMAIL}`">{{ CONTACT_EMAIL }}</a>.
    </p>

    <LegalLinks />
  </main>
</template>

<script setup>
import { CONTACT_EMAIL, OPERATOR_NAME } from '@/utils/legal'
import LegalLinks from '@/components/LegalLinks.vue'
</script>
