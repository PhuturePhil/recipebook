<template>
  <!-- Seite „Neuerungen“ im Versionskarten-Muster (app-shell.css: .cl-karte), komplette Historie -->
  <div class="changelog shell-view-body">
    <div>
      <article v-for="entry in changelog" :key="entry.version" class="cl-karte">
        <div class="cl-head">
          <h2>{{ entry.titel }}</h2>
          <span>{{ entry.datum }}</span>
        </div>
        <ul>
          <li v-for="(change, i) in entry.punkte" :key="i">{{ change }}</li>
        </ul>
      </article>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import changelog from '@/data/changelog.json'

// Wer alle Neuerungen gelesen hat, bekommt das „Was ist neu“-Fenster dafür nicht nochmal (und der NEU-Badge geht weg)
onMounted(() => {
  if (window.__ucMarkSeen) window.__ucMarkSeen(changelog[0].version)
  else {
    try { localStorage.setItem('uc:lastSeen', changelog[0].version) } catch { /* privater Modus */ }
  }
})
</script>

<style scoped>
.changelog > div { max-width: 760px; margin: 0 auto; }
.cl-karte li::marker { color: var(--akzent-text); }
@media (min-width: 1024px) { .changelog > div { margin: 0; } }
</style>
