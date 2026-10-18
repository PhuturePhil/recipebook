<script setup>
import { ref, computed, watch, watchEffect, onMounted } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useUiStore } from '@/stores/uiStore'
import { useFamilyStore } from '@/stores/familyStore'
import AppHeader from '@/components/shell/AppHeader.vue'
import TabBar from '@/components/shell/TabBar.vue'
import MoreSheet from '@/components/shell/MoreSheet.vue'
import ProfileModal from '@/components/ProfileModal.vue'
import LoadingOverlay from '@/components/LoadingOverlay.vue'
import WhatsNewModal from '@/components/WhatsNewModal.vue'
import { useUpdateCheck } from '@/composables/useUpdateCheck'
import { replacesTabBar } from '@/utils/detailActions'
import { flushNow } from '@/utils/analytics'
import '@/composables/useTheme'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()
const familyStore = useFamilyStore()
const showProfile = ref(false)
const moreOpen = ref(false)
const { popup: whatsNew, close: closeWhatsNew } = useUpdateCheck()

// Seiten der App-Hülle: aktiver Tab, Seitenleisten-Punkt, Titel, Zurück-Pfeil
const PAGES = {
  home: { tab: 'rezepte', title: 'Rezepte', search: true },
  'recipe-new': { tab: 'neu', title: 'Neues Rezept' },
  ingredients: { tab: 'zutaten', title: 'Zutaten' },
  'recipe-detail': { tab: 'rezepte', title: 'Rezept', back: '/', navTitle: true },
  'recipe-edit': { tab: 'rezepte', title: 'Rezept bearbeiten', back: true },
  'nutrition-info': { tab: 'mehr', side: 'naehrwerte', title: 'Nährwerte & Badges', back: true },
  changelog: { tab: 'mehr', side: 'neuerungen', title: 'Neuerungen', back: true },
  'admin-users': { tab: 'mehr', side: 'admin', title: 'Benutzerverwaltung', back: true },
  families: { tab: 'mehr', side: 'familien', title: 'Familien', back: true },
  'family-detail': { tab: 'mehr', side: 'familien', title: 'Familie', back: '/familien', navTitle: true },
  impressum: { tab: 'mehr', title: 'Impressum', back: true },
  datenschutz: { tab: 'mehr', title: 'Datenschutz', back: true },
  'shared-recipe': { title: 'Pastoors Familienrezepte' },
  'not-found': { title: 'Pastoors Familienrezepte' },
}
// Anmelde-Abläufe haben ihre eigene Karte, dort keine Kopfzeile/Navigation
const AUTH_FLOW = ['login', 'oidc-callback', 'reset-password', 'invite', 'family-invite', 'signup', 'verify-email']
// Familien-Eintrag trägt beim Launch ein NEU, bis man die Seite einmal geöffnet hat
const FAMILIEN_SEEN_KEY = 'familien:gesehen'
const familienNeu = ref(localStorage.getItem(FAMILIEN_SEEN_KEY) !== '1')

const page = computed(() => PAGES[route.name] ?? { title: 'Rezepte' })
// Gast-Startseite hat statt Kopfzeile ihren eigenen Hero
const guestHome = computed(() => route.name === 'home' && !authStore.isAuthenticated)
const showHeader = computed(() => !!route.name && !AUTH_FLOW.includes(route.name) && !guestHome.value)
const showShell = computed(() => showHeader.value && authStore.isAuthenticated)
// Gast auf einem öffentlichen Rezept: Kopf nur mit Zurück, App-Chip und „Anmelden“
const headerTitle = computed(() => {
  if (route.name === 'recipe-detail' && !authStore.isAuthenticated) return ''
  return (page.value.navTitle && uiStore.navTitle) || page.value.title
})
// Im Rezeptformular (FormActionBar) und auf der Rezeptseite (DetailActionBar) ersetzt eine eigene Leiste
// mobil die Tab-Leiste; die Seitenleiste ab 1024 px bleibt
const tabBarReplaced = computed(() => replacesTabBar(route.name))

const mehr = computed(() => [
  { id: 'familien', label: 'Familien', icon: 'familie', to: '/familien', badge: familienNeu.value },
  { id: 'naehrwerte', label: 'Nährwerte & Badges', icon: 'balken', to: '/naehrwerte' },
  { id: 'neuerungen', label: 'Neuerungen', icon: 'neuerungen', to: '/changelog' },
  ...(authStore.isAdmin ? [{ id: 'admin', label: 'Benutzerverwaltung', icon: 'personen', to: '/admin/users' }] : []),
  { id: 'profil', label: 'Persönliche Daten', icon: 'benutzer', onClick: () => { showProfile.value = true } },
  { id: 'abmelden', label: 'Abmelden', icon: 'abmelden', onClick: logout },
])

function openAllNews() {
  closeWhatsNew()
  router.push('/changelog')
}

async function logout() {
  flushNow()
  await authStore.logoutEverywhere()
  router.push('/login')
}

watch(() => route.name, (name) => {
  if (name === 'families' && familienNeu.value) {
    familienNeu.value = false
    localStorage.setItem(FAMILIEN_SEEN_KEY, '1')
  }
}, { immediate: true })

// Seiten-Aktion in der Kopfzeile gilt nur für die Seite, die sie gesetzt hat
watch(() => route.fullPath, () => uiStore.setHeaderAction(null))

// Familien des angemeldeten Kontos (Chips, Formular); bei Abmeldung/Kontowechsel neu
watch(() => authStore.user?.id ?? null, (id) => {
  if (id != null && authStore.isAuthenticated) familyStore.ensureLoaded(id)
  else familyStore.reset()
}, { immediate: true })

watchEffect(() => {
  document.body.classList.toggle('has-shell', showShell.value)
  if (!showShell.value) moreOpen.value = false
})

onMounted(async () => {
  await authStore.init()
  if (authStore.isAuthenticated && authStore.needsProfileSetup) {
    showProfile.value = true
  }
})
</script>

<template>
  <AppHeader
    v-if="showHeader"
    :title="headerTitle"
    :back="page.back ?? false"
    :search="!!page.search && authStore.isAuthenticated"
    :login-link="!authStore.isAuthenticated"
    :action="uiStore.headerAction"
  />
  <RouterView />
  <TabBar
    v-if="showShell"
    :class="{ 'shell-nav--ersetzt': tabBarReplaced }"
    :active="page.tab ?? ''"
    :side-active="page.side ?? ''"
    :mehr="mehr"
    :more-open="moreOpen"
    @more="moreOpen = true"
  />
  <MoreSheet v-if="moreOpen" :mehr="mehr" @close="moreOpen = false" />
  <ProfileModal v-if="showProfile" @close="showProfile = false" />
  <LoadingOverlay />
  <div v-if="uiStore.toast" class="app-toast" role="status" aria-live="polite">{{ uiStore.toast }}</div>
  <WhatsNewModal v-if="whatsNew" :title="whatsNew.title" :entries="whatsNew.entries" @close="closeWhatsNew" @all="openAllNews" />
</template>

<style>
.app-toast {
  position: fixed; left: 50%; transform: translateX(-50%); z-index: 1200;
  bottom: calc(96px + env(safe-area-inset-bottom, 0px));
  max-width: min(92vw, 420px); padding: 11px 18px; border-radius: 14px;
  background: var(--text); color: var(--flaeche);
  font-size: 14.5px; font-weight: 600; text-align: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, .18);
}
@media (min-width: 1024px) { .app-toast { bottom: 32px; } }
</style>
