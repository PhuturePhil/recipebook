<template>
  <div class="login-container">
    <div class="login-card">
      <h1>Pastoors Familienrezepte</h1>
      <h2>Anmelden</h2>

      <div v-if="error" class="error-message" role="alert">{{ error }}</div>

      <!-- „E-Mail zuerst“: Der Server sagt, ob die Adresse ein pastoors.cloud-Konto ist (→ SSO) oder ein Passwort braucht -->
      <p v-if="ssoRedirect" class="sso-redirect" role="status">
        Dein Konto ist mit pastoors.cloud verknüpft – du wirst weitergeleitet …
      </p>

      <form v-else novalidate @submit.prevent="step === 'email' ? handleContinue() : handleLogin()">
        <div class="form-group">
          <label for="email">E-Mail</label>
          <div class="email-row">
            <input
              id="email"
              ref="emailInput"
              v-model="email"
              type="email"
              required
              autocomplete="username"
              placeholder="Deine E-Mail"
              :readonly="step === 'password'"
              :class="{ 'is-locked': step === 'password' }"
            />
            <button v-if="step === 'password'" type="button" class="change-email" @click="changeEmail">Ändern</button>
          </div>
        </div>

        <div v-if="step === 'password'" class="form-group">
          <label for="password">Passwort</label>
          <input
            id="password"
            ref="passwordInput"
            v-model="password"
            type="password"
            required
            autocomplete="current-password"
            placeholder="Dein Passwort"
          />
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <template v-if="step === 'email'">{{ loading ? 'Einen Moment …' : 'Weiter' }}</template>
          <template v-else>{{ loading ? 'Anmelden …' : 'Anmelden' }}</template>
        </button>
      </form>

      <div class="links">
        <a v-if="oidcEnabled && !ssoRedirect" :href="oidcLoginUrl" class="sso-link">Mit pastoors.cloud anmelden</a>
        <a href="#" @click.prevent="openReset">Passwort vergessen?</a>
        <p class="signup-link">Noch kein Konto? <RouterLink to="/registrieren">Konto erstellen</RouterLink></p>
      </div>
    </div>

    <LegalLinks />

    <div v-if="showResetForm" class="modal-overlay" @click.self="showResetForm = false">
      <div class="modal-content">
        <h3>Passwort zurücksetzen</h3>
        <p>Gib deine E-Mail ein, dann schicken wir dir einen Link zum Zurücksetzen.</p>
        <p v-if="oidcEnabled" class="reset-sso-hint">
          Wer sich mit pastoors.cloud anmeldet, braucht hier kein Passwort – einfach die E-Mail eingeben und „Weiter“ tippen.
        </p>

        <form @submit.prevent="handlePasswordReset">
          <div class="form-group">
            <label for="reset-email">E-Mail</label>
            <input
              id="reset-email"
              v-model="resetEmail"
              type="email"
              required
              placeholder="Deine E-Mail"
            />
          </div>

          <div class="modal-actions">
            <button type="button" @click="showResetForm = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="resetLoading">
              {{ resetLoading ? 'Senden...' : 'Link senden' }}
            </button>
          </div>

          <p v-if="resetSuccess" class="success-message">
            Falls ein Konto mit dieser E-Mail existiert, haben wir dir einen Link geschickt.
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { authService } from '@/services/authService'
import LegalLinks from '@/components/LegalLinks.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const oidcEnabled = ref(false)
const oidcLoginUrl = authService.getOidcLoginUrl()
let oidcStatus = Promise.resolve(false)

const oidcErrors = {
  access_denied: 'Dein pastoors.cloud-Konto hat keinen Zugriff auf die Rezepte.',
  unavailable: 'Die Anmeldung über pastoors.cloud ist gerade nicht verfügbar.',
  missing_email: 'Für dein pastoors.cloud-Konto ist keine E-Mail-Adresse hinterlegt.',
  email_not_verified: 'Die E-Mail-Adresse deines pastoors.cloud-Kontos ist nicht bestätigt.',
  account_conflict: 'Dieses Rezepte-Konto ist bereits mit einem anderen pastoors.cloud-Login verknüpft.',
  email_unconfirmed: 'Die Adresse deines Rezepte-Kontos ist noch nicht bestätigt. Setze dein Passwort einmal über „Passwort vergessen“ neu – danach klappt die Anmeldung über pastoors.cloud.'
}

const step = ref('email')
const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref(null)
const ssoRedirect = ref(false)
const emailInput = ref(null)
const passwordInput = ref(null)

const showResetForm = ref(false)
const resetEmail = ref('')
const resetLoading = ref(false)
const resetSuccess = ref(false)

function redirectTarget() {
  const target = route.query.redirect
  // Only follow app-internal paths
  return typeof target === 'string' && target.startsWith('/') && !target.startsWith('//') ? target : '/'
}

onMounted(async () => {
  // Nach der Konto-Bestätigung kommt die Adresse mit
  if (typeof route.query.email === 'string') email.value = route.query.email
  if (authStore.sessionExpired) {
    error.value = 'Deine Anmeldung ist abgelaufen. Bitte melde dich erneut an.'
    authStore.sessionExpired = false
  }
  const oidcError = route.query.oidcError
  if (oidcError) {
    error.value = oidcErrors[oidcError] || 'Die Anmeldung über pastoors.cloud ist fehlgeschlagen.'
    router.replace({ name: 'login' })
  }
  if (!email.value) emailInput.value?.focus()
  oidcStatus = authService.isOidcEnabled()
  oidcEnabled.value = await oidcStatus
})

async function showPasswordStep() {
  step.value = 'password'
  await nextTick()
  passwordInput.value?.focus()
}

function changeEmail() {
  step.value = 'email'
  password.value = ''
  error.value = null
  nextTick(() => emailInput.value?.focus())
}

async function handleContinue() {
  error.value = null
  email.value = email.value.trim()
  if (!emailInput.value?.checkValidity() || !email.value) {
    error.value = 'Bitte gib eine gültige E-Mail-Adresse ein.'
    emailInput.value?.focus()
    return
  }
  loading.value = true
  try {
    // Ohne pastoors.cloud-Anmeldung (OIDC aus) gibt es nur das Passwort
    const method = (await oidcStatus) ? await authService.loginMethod(email.value) : 'password'
    if (method === 'sso') {
      ssoRedirect.value = true
      window.location.assign(oidcLoginUrl)
      return
    }
    await showPasswordStep()
  } catch (err) {
    if (err.status === 429) {
      error.value = err.message
    } else {
      // Prüfung nicht erreichbar: normal mit Passwort weiter, der Server weist SSO-Konten beim Login selbst hin
      await showPasswordStep()
    }
  } finally {
    loading.value = false
  }
}

async function handleLogin() {
  if (!password.value) {
    error.value = 'Bitte gib dein Passwort ein.'
    passwordInput.value?.focus()
    return
  }
  loading.value = true
  error.value = null

  const success = await authStore.login(email.value, password.value)

  if (success) {
    router.push(redirectTarget())
  } else {
    error.value = authStore.error || 'Anmeldung fehlgeschlagen'
    if (authStore.error && authStore.error.includes('pastoors.cloud')) {
      password.value = ''
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  loading.value = false
}

function openReset() {
  resetEmail.value = resetEmail.value || email.value
  showResetForm.value = true
}

async function handlePasswordReset() {
  resetLoading.value = true
  resetSuccess.value = false

  try {
    await authService.requestPasswordReset(resetEmail.value)
    resetSuccess.value = true
  } catch (err) {
    error.value = 'Fehler beim Senden des Reset-Links'
  }

  resetLoading.value = false
}
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  padding: 20px;
}

.login-card {
  background: var(--flaeche);
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

h1 {
  font-size: 1.5rem;
  text-align: center;
  margin-bottom: 10px;
  color: var(--text);
}

h2 {
  font-size: 1.25rem;
  text-align: center;
  margin-bottom: 30px;
  color: var(--text2);
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: var(--text);
}

input {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--linie);
  border-radius: 4px;
  font-size: 1rem;
}

input:focus {
  outline: none;
  border-color: var(--akzent);
}

button {
  width: 100%;
  padding: 12px;
  background: var(--akzent);
  color: var(--akzent-kontrast);
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}

button:hover:not(:disabled) {
  background: var(--akzent-hover);
}

button:disabled {
  background: var(--linie);
  color: var(--text2);
  cursor: not-allowed;
}

.email-row {
  display: flex;
  gap: 8px;
  align-items: stretch;
}

.email-row input {
  flex: 1;
  min-width: 0;
}

input.is-locked {
  background: var(--flaeche2);
  color: var(--text2);
}

.change-email {
  width: auto;
  padding: 0 14px;
  background: transparent;
  color: var(--akzent-text);
  border: 1px solid var(--linie);
  font-size: 0.9rem;
  font-weight: 600;
}

.change-email:hover:not(:disabled) {
  background: var(--flaeche2);
}

.sso-redirect {
  margin: 0;
  padding: 14px 12px;
  border-radius: 6px;
  background: var(--akzent-weich);
  color: var(--text);
  text-align: center;
}

.links .sso-link {
  display: block;
  margin-bottom: 12px;
}

.reset-sso-hint {
  font-size: 0.85rem;
  background: var(--flaeche2);
  padding: 8px 10px;
  border-radius: 4px;
}

.error-message {
  background: var(--neg-weich);
  color: var(--neg);
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.success-message {
  background: var(--pos-weich);
  color: var(--pos);
  padding: 10px;
  border-radius: 4px;
  margin-top: 15px;
  text-align: center;
}

.links {
  margin-top: 20px;
  text-align: center;
}

.links a {
  color: var(--text2);
  text-decoration: none;
  font-size: 0.9rem;
}

.links a:hover {
  text-decoration: underline;
}

.signup-link {
  margin: 14px 0 0;
  font-size: 0.9rem;
  color: var(--text2);
}

.links .signup-link a {
  color: var(--akzent-text);
  font-weight: 600;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--overlay);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: var(--flaeche);
  padding: 30px;
  border-radius: 8px;
  width: 90%;
  max-width: 400px;
}

.modal-content h3 {
  margin-bottom: 15px;
}

.modal-content p {
  margin-bottom: 20px;
  color: var(--text2);
}

.modal-actions {
  display: flex;
  gap: 10px;
}

.modal-actions button {
  flex: 1;
}

.btn-secondary {
  background: var(--flaeche2);
  color: var(--text);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--linie);
}
</style>
