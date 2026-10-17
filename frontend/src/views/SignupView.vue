<template>
  <!-- Offene Registrierung: das Konto entsteht erst mit dem Link aus der Bestätigungs-Mail -->
  <div class="konto">
    <div v-if="sentTo" class="konto__card konto__card--mitte ui-karte" role="status">
      <span class="app-chip app-chip--icon konto__chip" aria-hidden="true"><img src="/icon-v2.svg" alt="" decoding="async"></span>
      <h1>Fast geschafft</h1>
      <p class="konto__text">Wir haben dir eine E-Mail an <strong>{{ sentTo }}</strong> geschickt. Öffne den Link darin, um dein Konto zu aktivieren.</p>
      <p class="konto__text">Keine Mail da? Schau im Spam-Ordner nach.</p>
      <RouterLink to="/login" class="ui-btn">Zur Anmeldung</RouterLink>
    </div>

    <div v-else class="konto__card ui-karte">
      <span class="app-chip app-chip--icon konto__chip" aria-hidden="true"><img src="/icon-v2.svg" alt="" decoding="async"></span>
      <h1>Konto erstellen</h1>

      <form class="konto__form" novalidate @submit.prevent="submit">
        <div class="konto__zeile">
          <div class="konto__feld">
            <label for="su-vorname">Vorname</label>
            <input id="su-vorname" v-model="form.vorname" class="ui-input" type="text" autocomplete="given-name" maxlength="100" required>
          </div>
          <div class="konto__feld">
            <label for="su-nachname">Nachname</label>
            <input id="su-nachname" v-model="form.nachname" class="ui-input" type="text" autocomplete="family-name" maxlength="100" required>
          </div>
        </div>
        <div class="konto__feld">
          <label for="su-email">E-Mail</label>
          <input id="su-email" v-model="form.email" class="ui-input" type="email" autocomplete="email" inputmode="email" maxlength="254" required>
        </div>
        <div class="konto__feld">
          <label for="su-password">Passwort</label>
          <input id="su-password" v-model="form.password" class="ui-input" type="password" autocomplete="new-password" :minlength="MIN_PASSWORD" placeholder="Mindestens 8 Zeichen" required>
        </div>
        <div class="konto__feld">
          <label for="su-password2">Passwort wiederholen</label>
          <input id="su-password2" v-model="form.passwordConfirm" class="ui-input" type="password" autocomplete="new-password" required>
        </div>

        <p v-if="error" class="konto__error" role="alert">{{ error }}</p>

        <button type="submit" class="ui-btn ui-btn--primaer" :disabled="busy">{{ busy ? 'Wird gesendet…' : 'Konto erstellen' }}</button>
      </form>

      <p class="konto__alt">Schon ein Konto? <RouterLink to="/login">Anmelden</RouterLink></p>
    </div>

    <LegalLinks />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { authService } from '@/services/authService'
import { MIN_PASSWORD, signupError } from '@/utils/legal'
import LegalLinks from '@/components/LegalLinks.vue'

const form = reactive({ vorname: '', nachname: '', email: '', password: '', passwordConfirm: '' })
const busy = ref(false)
const error = ref('')
const sentTo = ref('')

async function submit() {
  error.value = signupError(form) ?? ''
  if (error.value) return
  busy.value = true
  try {
    await authService.signup({
      vorname: form.vorname.trim(),
      nachname: form.nachname.trim(),
      email: form.email.trim(),
      password: form.password,
    })
    sentTo.value = form.email.trim()
    form.password = ''
    form.passwordConfirm = ''
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>
