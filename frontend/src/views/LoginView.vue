<template>
  <div class="login-container">
    <div class="login-card">
      <h1>Pastoors Familienrezepte</h1>
      <h2>Anmelden</h2>
      
      <form @submit.prevent="handleLogin">
        <div v-if="error" class="error-message">{{ error }}</div>
        
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
            Ein Link wurde an Ihre E-Mail gesendet.
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { authService } from '@/services/authService'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref(null)

const showResetForm = ref(false)
const resetEmail = ref('')
const resetLoading = ref(false)
const resetSuccess = ref(false)

async function handleLogin() {
  loading.value = true
  error.value = null
  
  const success = await authStore.login(email.value, password.value)
  
  if (success) {
    router.push('/')
  } else {
    error.value = authStore.error || 'Anmeldung fehlgeschlagen'
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
  background: white;
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
  color: #2c3e50;
}

h2 {
  font-size: 1.25rem;
  text-align: center;
  margin-bottom: 30px;
  color: #666;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

input:focus {
  outline: none;
  border-color: #4a5568;
}

button {
  width: 100%;
  padding: 12px;
  background: #4a5568;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}

button:hover:not(:disabled) {
  background: #2d3748;
}

button:disabled {
  background: #a0aec0;
  cursor: not-allowed;
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.success-message {
  background: #c6f6d5;
  color: #2f855a;
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
  color: #4a5568;
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
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
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
  color: #666;
}

.modal-actions {
  display: flex;
  gap: 10px;
}

.modal-actions button {
  flex: 1;
}

.btn-secondary {
  background: #e2e8f0;
  color: #333;
}

.btn-secondary:hover:not(:disabled) {
  background: #cbd5e0;
}
</style>
