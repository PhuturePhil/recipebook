<template>
  <!-- Kopfzeile nach app-shell-Spezifikation: deckend (iOS-26-Unschärfe), mobil App-Chip + Titel,
       Unterseiten mit Zurück-Pfeil; am Desktop großer Seitentitel (Chip steht dann in der Seitenleiste).
       Startseite: Suche + Tag-Chips als zweite Zeile. -->
  <header :class="['shell-header', 'app-header', { 'app-header--search': search }]">
    <button v-if="back" type="button" class="shell-back" aria-label="Zurück" @click="goBack">
      <AppIcon name="zurueck" :size="22" />
    </button>
    <span v-if="!back || loginLink" class="app-chip app-chip--icon" aria-hidden="true"><img src="/icon-v2.svg" alt="" decoding="async"></span>
    <h1><span class="shell-title-app">{{ title }}</span><span class="shell-title-page">{{ title }}</span></h1>
    <div v-if="loginLink" class="shell-header-actions">
      <RouterLink :to="loginTarget" class="app-header__login">Anmelden</RouterLink>
    </div>
    <div v-else-if="action" class="shell-header-actions">
      <button type="button" class="app-header__action" :aria-label="action.label" :title="action.label" @click="action.run()">
        <AppIcon :name="action.icon" :size="20" />
      </button>
    </div>
    <div v-if="search" class="app-header__search">
      <SearchBar />
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import SearchBar from '@/components/SearchBar.vue'
import { headerBack } from '@/utils/backNavigation'

const props = defineProps({
  title: { type: String, default: '' },
  back: { type: [String, Boolean], default: false },
  search: { type: Boolean, default: false },
  loginLink: { type: Boolean, default: false },
  action: { type: Object, default: null },
})
const router = useRouter()
const route = useRoute()
// Nach dem Anmelden zurück auf die Seite, von der man kam (z. B. öffentliches Rezept)
const loginTarget = computed(() =>
  route.fullPath && route.fullPath !== '/' ? { path: '/login', query: { redirect: route.fullPath } } : '/login'
)

function goBack() {
  // back als Pfad (Rezeptseite): immer dorthin, per router.back() nur wenn der vorige Eintrag schon das Ziel ist.
  // back = true: innerhalb der App zurück, sonst (Direktaufruf/geteilter Link) zur Übersicht
  headerBack(router, props.back)
}
</script>

<style>
.app-header { flex-wrap: wrap; row-gap: 10px; }
.app-header__search { flex: 1 0 100%; min-width: 0; }
.app-header__action {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border: 0; border-radius: 10px;
  background: none; color: var(--akzent-text); cursor: pointer;
}
.app-header__login {
  display: inline-flex; align-items: center; height: 36px; padding: 0 14px;
  border-radius: var(--radius); background: var(--akzent); color: var(--akzent-kontrast);
  font-size: 14px; font-weight: 650; text-decoration: none;
}
@media (min-width: 1024px) {
  .app-header__search > * { max-width: 760px; }
}
</style>
