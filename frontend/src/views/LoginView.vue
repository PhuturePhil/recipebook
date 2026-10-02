<template>
  <div class="login-container">
    <div class="login-card">
      <h1>Pastoors Familienrezepte</h1>
      <h2>Anmelden</h2>

      <div v-if="error" class="error-message">{{ error }}</div>

      <template v-if="oidcEnabled">
        <a :href="oidcLoginUrl" class="oidc-button">Mit pastoors.cloud anmelden</a>
        <p class="oidc-hint">Für die Familie – ein Login für alles auf pastoors.cloud.</p>
        <div class="divider"><span>Anmelden mit E-Mail</span></div>
      </template>
      
      <form @submit.prevent="handleLogin" :class="{ 'email-login-secondary': oidcEnabled }">
        
        <div class="form-group">
          <label for="email">E-Mail</label>
          <input 
            id="email" 
            v-model="email" 
            type="email" 
            required 
            placeholder="Ihre E-Mail"
          />
        </div>
        
        <div class="form-group">
          <label for="password">Passwort</label>
          <input 
            id="password" 
            v-model="password" 
            type="password" 
            required 
            placeholder="Ihr Passwort"
          />
        </div>
        
        <button type="submit" :disabled="loading">
          {{ loading ? 'Anmelden...' : 'Anmelden' }}
        </button>
      </form>
      
      <div class="links">
        <a href="#" @click.prevent="showResetForm = true">Passwort vergessen?</a>
      </div>
    </div>

    <div v-if="showResetForm" class="modal-overlay" @click.self="showResetForm = false">
      <div class="modal-content">
        <h3>Passwort zurücksetzen</h3>
        <p>Geben Sie Ihre E-Mail ein, um einen Reset-Link zu erhalten.</p>
        <p v-if="oidcEnabled" class="reset-sso-hint">
          Wer sich mit pastoors.cloud anmeldet, braucht hier kein Passwort – bitte oben „Mit pastoors.cloud anmelden“ nutzen.
        </p>
        
        <form @submit.prevent="handlePasswordReset">
          <div class="form-group">
            <label for="reset-email">E-Mail</label>
            <input 
              id="reset-email" 
              v-model="resetEmail" 
              type="email" 
              required 
              placeholder="Ihre E-Mail"
            />
          </div>
          
          <div class="modal-actions">
            <button type="button" @click="showResetForm = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="resetLoading">
              {{ resetLoading ? 'Senden...' : 'Link senden' }}
            </button>
          </div>
          
          <p v-if="resetSuccess" class="success-message">
            Falls ein Konto mit dieser E-Mail existiert, wurde ein Link verschickt.
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { authService } from '@/services/authService'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const oidcEnabled = ref(false)
const oidcLoginUrl = authService.getOidcLoginUrl()

const oidcErrors = {
  access_denied: 'Dein pastoors.cloud-Konto hat keinen Zugriff auf die Rezepte.',
  unavailable: 'Die Anmeldung über pastoors.cloud ist gerade nicht verfügbar.',
  missing_email: 'Für dein pastoors.cloud-Konto ist keine E-Mail-Adresse hinterlegt.',
  email_not_verified: 'Die E-Mail-Adresse deines pastoors.cloud-Kontos ist nicht bestätigt.',
  account_conflict: 'Dieses Rezepte-Konto ist bereits mit einem anderen pastoors.cloud-Login verknüpft.'
}

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref(null)

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
  if (authStore.sessionExpired) {
    error.value = 'Deine Anmeldung ist abgelaufen. Bitte melde dich erneut an.'
    authStore.sessionExpired = false
  }
  const oidcError = route.query.oidcError
  if (oidcError) {
    error.value = oidcErrors[oidcError] || 'Die Anmeldung über pastoors.cloud ist fehlgeschlagen.'
    router.replace({ name: 'login' })
  }
  oidcEnabled.value = await authService.isOidcEnabled()
})

async function handleLogin() {
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
  cursor: not-allowed;
}

.oidc-button {
  display: block;
  width: 100%;
  padding: 16px 12px;
  background: var(--akzent);
  color: var(--akzent-kontrast);
  border-radius: 6px;
  font-size: 1.1rem;
  font-weight: 600;
  text-align: center;
  text-decoration: none;
  transition: background 0.2s;
}

.oidc-hint {
  margin: 8px 0 0;
  text-align: center;
  font-size: 0.85rem;
  color: var(--text2);
}

.email-login-secondary .form-group {
  margin-bottom: 12px;
}

.email-login-secondary label {
  font-size: 0.85rem;
}

.email-login-secondary input {
  padding: 9px;
  font-size: 0.95rem;
}

.email-login-secondary button {
  padding: 9px;
  font-size: 0.95rem;
  background: var(--flaeche);
  color: var(--text);
  border: 1px solid var(--akzent);
}

.email-login-secondary button:hover:not(:disabled) {
  background: var(--flaeche2);
}

.reset-sso-hint {
  font-size: 0.85rem;
  background: var(--flaeche2);
  padding: 8px 10px;
  border-radius: 4px;
}

.oidc-button:hover {
  background: var(--akzent-hover);
}

.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 25px 0;
  color: var(--text3);
  font-size: 0.85rem;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-top: 1px solid var(--linie);
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
