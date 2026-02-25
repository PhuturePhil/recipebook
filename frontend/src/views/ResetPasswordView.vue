<template>
  <div class="reset-password-container">
    <div class="reset-password-card">
      <h1>Neues Passwort setzen</h1>

      <div v-if="success" class="success-message">
        Passwort erfolgreich geändert.
        <router-link to="/login">Zum Login</router-link>
      </div>

      <div v-else-if="!token" class="error-message">
        Ungültiger oder fehlender Token. Bitte fordere einen neuen Link an.
      </div>

      <form v-else @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="password">Neues Passwort</label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            minlength="6"
            placeholder="Mindestens 6 Zeichen"
          />
        </div>

        <div class="form-group">
          <label for="password-confirm">Passwort bestätigen</label>
          <input
            id="password-confirm"
            v-model="passwordConfirm"
            type="password"
            required
            placeholder="Passwort wiederholen"
          />
        </div>

        <div v-if="error" class="error-message">{{ error }}</div>

        <button type="submit" :disabled="loading" class="btn-primary">
          {{ loading ? 'Speichern...' : 'Passwort speichern' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { authService } from '@/services/authService'

const route = useRoute()

const token = ref('')
const password = ref('')
const passwordConfirm = ref('')
const loading = ref(false)
const error = ref(null)
const success = ref(false)

onMounted(() => {
  token.value = route.query.token || ''
})

async function handleSubmit() {
  error.value = null

  if (password.value !== passwordConfirm.value) {
    error.value = 'Passwörter stimmen nicht überein.'
    return
  }

  loading.value = true
  try {
    await authService.resetPassword(token.value, password.value)
    success.value = true
  } catch (err) {
    error.value = 'Fehler beim Zurücksetzen des Passworts. Der Link ist möglicherweise abgelaufen.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reset-password-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg, #f9fafb);
  padding: 20px;
}

.reset-password-card {
  background: white;
  border-radius: 8px;
  padding: 40px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

h1 {
  margin-bottom: 24px;
  font-size: 1.5rem;
  color: var(--color-text-primary, #1a202c);
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: var(--color-text-secondary, #4a5568);
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 4px;
  font-size: 1rem;
}

.btn-primary {
  width: 100%;
  padding: 12px;
  background: var(--color-primary, #4a5568);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  margin-top: 8px;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark, #2d3748);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.success-message {
  background: #c6f6d5;
  color: #276749;
  padding: 16px;
  border-radius: 4px;
}

.success-message a {
  color: #276749;
  font-weight: 600;
  margin-left: 8px;
}
</style>
