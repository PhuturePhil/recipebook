<template>
  <!-- Kopfzeile nach app-shell-Spezifikation: deckend (iOS-26-Unschärfe), mobil App-Chip + Titel,
       Unterseiten mit Zurück-Pfeil; am Desktop großer Seitentitel (Chip steht dann in der Seitenleiste).
       Startseite: Suche + Tag-Chips als zweite Zeile. -->
  <header :class="['shell-header', 'app-header', { 'app-header--search': search }]">
    <button v-if="back" type="button" class="shell-back" aria-label="Zurück" @click="goBack">
      <AppIcon name="zurueck" :size="22" />
    </button>
    <span v-else class="app-chip" aria-hidden="true">R</span>
    <h1><span class="shell-title-app">{{ title }}</span><span class="shell-title-page">{{ title }}</span></h1>
    <div v-if="loginLink" class="shell-header-actions">
      <RouterLink to="/login" class="app-header__login">Anmelden</RouterLink>
    </div>
    <div v-if="search" class="app-header__search">
      <SearchBar />
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import SearchBar from '@/components/SearchBar.vue'

const props = defineProps({
  title: { type: String, default: '' },
  back: { type: [String, Boolean], default: false },
  search: { type: Boolean, default: false },
  loginLink: { type: Boolean, default: false },
})
const router = useRouter()

function goBack() {
  // Innerhalb der App zurück, sonst (Direktaufruf/geteilter Link) zur Übersicht
  if (window.history.state?.back) router.back()
  else router.push(typeof props.back === 'string' ? props.back : '/')
}
</script>

<style>
.app-header { flex-wrap: wrap; row-gap: 10px; }
.app-header__search { flex: 1 0 100%; min-width: 0; }
.app-header__login {
  display: inline-flex; align-items: center; height: 36px; padding: 0 14px;
  border-radius: var(--radius); background: var(--akzent); color: var(--akzent-kontrast);
  font-size: 14px; font-weight: 650; text-decoration: none;
}
@media (min-width: 1024px) {
  .app-header__search > * { max-width: 760px; }
}
</style>
