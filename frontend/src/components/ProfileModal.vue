<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <h3 v-if="forcedSetup">Profil vervollständigen</h3>
      <h3 v-else>Mein Profil</h3>

      <p v-if="user?.mustChangePassword" class="info-message">
        Bitte setze ein neues Passwort.
      </p>
      <p v-else-if="forcedSetup" class="info-message">
        Bitte vervollständige dein Profil.
      </p>

      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="profile-vorname">Vorname</label>
          <input
            id="profile-vorname"
            v-model="form.vorname"
            type="text"
            :required="forcedSetup"
            placeholder="Vorname"
          />
        </div>

        <div class="form-group">
          <label for="profile-nachname">Nachname</label>
          <input
            id="profile-nachname"
            v-model="form.nachname"
            type="text"
            :required="forcedSetup"
            placeholder="Nachname"
          />
        </div>

        <div class="form-group">
          <label for="profile-email">E-Mail</label>
          <input
            id="profile-email"
            v-model="form.email"
            type="email"
            required
            placeholder="E-Mail"
          />
        </div>

        <div class="form-group">
          <label for="profile-password">
            {{ user?.mustChangePassword ? 'Neues Passwort (Pflicht)' : 'Neues Passwort (optional)' }}
          </label>
          <input
            id="profile-password"
            v-model="form.password"
            type="password"
            :required="user?.mustChangePassword"
            minlength="6"
            placeholder="Leer lassen zum Beibehalten"
          />
        </div>

        <div v-if="error" class="error-message">{{ error }}</div>

        <div class="modal-actions">
          <button v-if="!forcedSetup" type="button" @click="handleClose" class="btn-secondary">
            Abbrechen
          </button>
          <button type="submit" :disabled="loading" class="btn-primary">
            {{ loading ? 'Speichern...' : 'Speichern' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'

const emit = defineEmits(['close'])

const authStore = useAuthStore()
const user = computed(() => authStore.user)

const forcedSetup = computed(() =>
  user.value && (!user.value.vorname || !user.value.nachname || user.value.mustChangePassword)
)

const form = ref({
  vorname: '',
  nachname: '',
  email: '',
  password: ''
})

const loading = ref(false)
const error = ref(null)

onMounted(() => {
  form.value.vorname = user.value?.vorname || ''
  form.value.nachname = user.value?.nachname || ''
  form.value.email = user.value?.email || ''
})

function handleClose() {
  if (!forcedSetup.value) {
    emit('close')
  }
}

async function handleSubmit() {
  error.value = null
  loading.value = true

  try {
    const payload = {
      vorname: form.value.vorname || undefined,
      nachname: form.value.nachname || undefined,
      email: form.value.email || undefined
    }

    if (form.value.password) {
      payload.password = form.value.password
    }

    await authStore.updateProfile(payload)
    emit('close')
  } catch (err) {
    error.value = err.message || 'Fehler beim Speichern. Bitte versuche es erneut.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
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
  max-width: 420px;
}

.modal-content h3 {
  margin-bottom: 16px;
  font-size: 1.25rem;
  color: var(--color-text-primary, #1a202c);
}

.info-message {
  background: #ebf8ff;
  color: #2c5282;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 16px;
  font-size: 0.9rem;
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  font-size: 0.9rem;
  color: var(--color-text-secondary, #4a5568);
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 4px;
  font-size: 1rem;
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 14px;
  font-size: 0.9rem;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary {
  flex: 1;
  padding: 10px;
  background: var(--color-primary, #4a5568);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark, #2d3748);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  flex: 1;
  padding: 10px;
  background: var(--color-bg-secondary, #e2e8f0);
  color: var(--color-text-primary, #333);
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
}
</style>
