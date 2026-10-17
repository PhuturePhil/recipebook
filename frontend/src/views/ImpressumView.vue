<template>
  <!-- Impressum light (Beschluss 17.10.2026): privates, nicht-kommerzielles Projekt – Name und E-Mail, keine Anschrift -->
  <main class="legal-page">
    <h1>Impressum</h1>
    <p>Pastoors Familienrezepte ist ein privates, nicht-kommerzielles Projekt – ohne Werbung und ohne Einnahmen.</p>

    <div class="ui-karte">
      <p><strong>Verantwortlich</strong><br>{{ OPERATOR_NAME }}</p>
      <p><strong>Kontakt</strong><br><a :href="`mailto:${CONTACT_EMAIL}`">{{ CONTACT_EMAIL }}</a></p>
    </div>

    <h2>Inhalte melden</h2>
    <p>
      Rezepte, Fotos und Bewertungen stammen von den Mitgliedern. Fällt dir etwas auf, das nicht hierher gehört –
      etwa ein urheberrechtlich geschütztes Rezept oder ein beleidigender Kommentar –, nutze „Inhalt melden“ unten auf
      der Rezeptseite oder schreib an <a :href="`mailto:${CONTACT_EMAIL}`">{{ CONTACT_EMAIL }}</a>.
      Rechtswidrige Inhalte entfernen wir zügig.
    </p>

    <h2>Datenschutz</h2>
    <p>Welche Daten die App speichert und welche Rechte du hast, steht in der <RouterLink to="/datenschutz">Datenschutzerklärung</RouterLink>.</p>

    <LegalLinks />
  </main>
</template>

<script setup>
import { CONTACT_EMAIL, OPERATOR_NAME } from '@/utils/legal'
import LegalLinks from '@/components/LegalLinks.vue'
</script>
