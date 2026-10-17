<template>
  <!-- Öffentliche Einladungsseite im Stil der Login-Karte. Ungültig und abgelaufen sehen gleich aus. -->
  <div class="fam-invite">
    <div class="fam-invite__card ui-karte">
      <p v-if="state === 'loading'" class="fam-invite__text">Einladung wird geladen…</p>

      <template v-else-if="state === 'invalid'">
        <h1>Einladung</h1>
        <p class="fam-invite__text">{{ INVALID }}</p>
        <RouterLink to="/" class="ui-btn">Zur Startseite</RouterLink>
      </template>

      <template v-else-if="info">
        <span class="fam-avatar mini-avatar fam-invite__avatar" aria-hidden="true">{{ initial }}</span>
        <h1 v-if="info.invitedBy">{{ info.invitedBy }} lädt dich zu „{{ info.familyName }}“ ein</h1>
        <h1 v-else>Einladung zu „{{ info.familyName }}“</h1>

        <p v-if="state === 'full'" class="fam-invite__text">Diese Familie ist voll.</p>

        <template v-else-if="state === 'guest'">
          <RouterLink :to="{ path: '/invite', query: { family: code } }" class="ui-btn ui-btn--primaer">Konto erstellen</RouterLink>
          <button type="button" class="ui-btn" @click="toLogin">Ich habe schon ein Konto</button>
        </template>

        <template v-else-if="state === 'confirm'">
          <p class="fam-invite__text">{{ info.familyName }} beitreten?</p>
          <button type="button" class="ui-btn ui-btn--primaer" :disabled="busy" @click="join">Beitreten</button>
          <RouterLink to="/" class="ui-btn">Abbrechen</RouterLink>
        </template>

        <p v-if="error" class="fam-status fam-status--error" role="alert">{{ error }}</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useFamilyStore, shortFamilyName } from '@/stores/familyStore'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import { familyService } from '@/services/familyService'
import { rememberInvite, takeInvite, invitePath } from '@/utils/pendingInvite'
import { welcomeText } from '@/utils/familyTexts'
import { track } from '@/utils/analytics'

const INVALID = 'Dieser Einladungslink ist ungültig oder abgelaufen.'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const familyStore = useFamilyStore()
const recipeStore = useRecipeStore()
const uiStore = useUiStore()

const code = computed(() => String(route.params.code ?? ''))
const info = ref(null)
const state = ref('loading')
const busy = ref(false)
const error = ref('')
const initial = computed(() => shortFamilyName(info.value?.familyName).charAt(0).toLocaleUpperCase('de-DE'))

// Mitglied (gerade beigetreten oder schon über die Registrierung): ab in die gemeinsame Sammlung
async function arrive(familyId, familyName) {
  await familyStore.ensureLoaded(authStore.user?.id, { force: true })
  recipeStore.invalidateRecipes()
  recipeStore.setScope(familyId)
  uiStore.showToast(welcomeText(familyName))
  router.replace('/')
}

async function load() {
  try {
    info.value = await familyService.inviteInfo(code.value)
  } catch {
    // Ungültig, abgelaufen oder nicht erreichbar: eine Meldung für alles
    state.value = 'invalid'
    return
  }
  if (info.value.member) {
    await arrive(info.value.familyId, info.value.familyName)
  } else if (info.value.full) {
    state.value = 'full'
  } else {
    state.value = authStore.isAuthenticated ? 'confirm' : 'guest'
  }
}

function toLogin() {
  // SSO verlässt die App; danach führt /login/oidc hierher zurück
  rememberInvite(code.value)
  router.push({ path: '/login', query: { redirect: invitePath(code.value) } })
}

async function join() {
  busy.value = true
  error.value = ''
  try {
    const family = await familyService.join(code.value)
    track('einladung_angenommen')
    await arrive(family.id, family.name)
  } catch (e) {
    if (e.status === 404) state.value = 'invalid'
    else error.value = e.message
  } finally {
    busy.value = false
  }
}

onMounted(() => {
  // Angekommen: eine gemerkte Einladung ist damit erledigt
  if (authStore.isAuthenticated) takeInvite()
  load()
})
</script>

<style scoped>
.fam-invite { display: flex; justify-content: center; align-items: center; min-height: 80vh; padding: 20px; }
.fam-invite__card {
  width: 100%; max-width: 400px; padding: 32px 24px; text-align: center;
  display: flex; flex-direction: column; gap: 10px;
}
.fam-invite__avatar { margin: 0 auto 6px; }
.fam-invite h1 { font-size: 1.3rem; line-height: 1.35; margin: 0 0 8px; color: var(--text); overflow-wrap: anywhere; }
.fam-invite__text { margin: 0 0 8px; color: var(--text2); font-size: 15px; line-height: 1.5; }
.fam-invite .ui-btn { text-decoration: none; }
</style>
