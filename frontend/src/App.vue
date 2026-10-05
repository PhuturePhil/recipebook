<script setup>
import { ref, computed, watchEffect, onMounted } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useUiStore } from '@/stores/uiStore'
import AppHeader from '@/components/shell/AppHeader.vue'
import TabBar from '@/components/shell/TabBar.vue'
import MoreSheet from '@/components/shell/MoreSheet.vue'
import ProfileModal from '@/components/ProfileModal.vue'
import LoadingOverlay from '@/components/LoadingOverlay.vue'
import WhatsNewModal from '@/components/WhatsNewModal.vue'
import { useUpdateCheck } from '@/composables/useUpdateCheck'
import '@/composables/useTheme'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()
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
  'shared-recipe': { title: 'Pastoors Familienrezepte' },
  'not-found': { title: 'Pastoors Familienrezepte' },
}
// Anmelde-Abläufe haben ihre eigene Karte, dort keine Kopfzeile/Navigation
const AUTH_FLOW = ['login', 'oidc-callback', 'reset-password', 'invite']

const page = computed(() => PAGES[route.name] ?? { title: 'Rezepte' })
const showHeader = computed(() => !!route.name && !AUTH_FLOW.includes(route.name))
const showShell = computed(() => showHeader.value && authStore.isAuthenticated)
const headerTitle = computed(() => (page.value.navTitle && uiStore.navTitle) || page.value.title)
// Im Rezeptformular ersetzt dessen Aktionsleiste (RecipeEdit) mobil die Tab-Leiste; die Seitenleiste ab 1024 px bleibt
const formMode = computed(() => ['recipe-new', 'recipe-edit'].includes(route.name))

const mehr = computed(() => [
  { id: 'naehrwerte', label: 'Nährwerte & Badges', icon: 'balken', to: '/naehrwerte' },
  { id: 'neuerungen', label: 'Neuerungen', icon: 'neuerungen', to: '/changelog' },
  ...(authStore.isAdmin ? [{ id: 'admin', label: 'Benutzerverwaltung', icon: 'personen', to: '/admin/users' }] : []),
  { id: 'profil', label: 'Persönliche Daten', icon: 'benutzer', onClick: () => { showProfile.value = true } },
  { id: 'abmelden', label: 'Abmelden', icon: 'abmelden', onClick: logout },
])

function logout() {
  authStore.logout()
  router.push('/login')
}

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
  />
  <RouterView />
  <TabBar
    v-if="showShell"
    :class="{ 'shell-nav--form': formMode }"
    :active="page.tab ?? ''"
    :side-active="page.side ?? ''"
    :mehr="mehr"
    :more-open="moreOpen"
    @more="moreOpen = true"
  />
  <MoreSheet v-if="moreOpen" :mehr="mehr" @close="moreOpen = false" />
  <ProfileModal v-if="showProfile" @close="showProfile = false" />
  <LoadingOverlay />
  <WhatsNewModal v-if="whatsNew" :title="whatsNew.title" :entries="whatsNew.entries" @close="closeWhatsNew" />
</template>
