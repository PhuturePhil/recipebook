<template>
  <!-- Ziel des Links an die neue E-Mail-Adresse. Bestätigt wird per POST, damit Link-Vorschauen im Mailprogramm nichts auslösen. -->
  <div class="konto">
    <div class="konto__card konto__card--mitte ui-karte" :role="state === 'error' ? 'alert' : 'status'">
      <span class="app-chip app-chip--icon konto__chip" aria-hidden="true"><img src="/icon-v2.svg" alt="" decoding="async"></span>
      <p v-if="state === 'loading'" class="konto__text">Neue E-Mail-Adresse wird bestätigt…</p>

      <template v-else-if="state === 'ok'">
        <h1>Neue Adresse bestätigt</h1>
        <p class="konto__text">
          Ab jetzt meldest du dich mit {{ email }} an. Zur Sicherheit wurdest du überall abgemeldet.
        </p>
        <RouterLink :to="{ path: '/login', query: { email } }" class="ui-btn ui-btn--primaer">Jetzt anmelden</RouterLink>
      </template>

      <template v-else>
        <h1>Link ungültig</h1>
        <p class="konto__text">{{ error }}</p>
        <RouterLink to="/" class="ui-btn">Zur Startseite</RouterLink>
      </template>
    </div>

    <LegalLinks />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/stores/authStore'
import LegalLinks from '@/components/LegalLinks.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const state = ref('loading')
const email = ref('')
const error = ref('')

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  // Token nicht im Verlauf stehen lassen
  router.replace({ query: {} })
  if (!token) {
    error.value = 'Der Bestätigungslink ist ungültig oder abgelaufen.'
    state.value = 'error'
    return
  }
  try {
    email.value = await authService.confirmEmail(token)
    // Der Server hat alle Anmeldungen des Kontos beendet
    authStore.logout()
    state.value = 'ok'
  } catch (e) {
    error.value = e.message
    state.value = 'error'
  }
})
</script>
