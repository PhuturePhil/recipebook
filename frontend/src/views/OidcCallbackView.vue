<template>
  <div class="callback-container">
    <div class="callback-card">
      <template v-if="error">
        <div class="error-message">{{ error }}</div>
        <router-link to="/login">Zurück zur Anmeldung</router-link>
      </template>
      <p v-else>Anmeldung läuft...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const error = ref(null)

onMounted(async () => {
  const ticket = route.query.ticket
  if (!ticket) {
    error.value = 'Die Anmeldung ist fehlgeschlagen. Bitte erneut anmelden.'
    return
  }

  const success = await authStore.loginWithOidcTicket(ticket)
  if (success) {
    router.replace('/')
  } else {
    error.value = authStore.error || 'Die Anmeldung ist fehlgeschlagen. Bitte erneut anmelden.'
  }
})
</script>

<style scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  padding: 20px;
}

.callback-card {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  text-align: center;
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 20px;
}

a {
  color: #4a5568;
}
</style>
