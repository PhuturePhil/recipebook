<template>
  <div class="admin-users-container">
    <h1>Benutzerverwaltung</h1>
    
    <button @click="showCreateForm = true" class="btn-primary">
      Neuen Benutzer erstellen
    </button>
    
    <div v-if="loading" class="loading">Laden...</div>
    
    <div v-else-if="users.length === 0" class="empty">
      Noch keine Benutzer vorhanden.
    </div>
    
    <table v-else class="users-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>E-Mail</th>
          <th>Rolle</th>
          <th>Erstellt am</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.vorname }} {{ user.nachname }}</td>
          <td>{{ user.email }}</td>
          <td>
            <span :class="['role-badge', user.role.toLowerCase()]">
              {{ user.role === 'ADMIN' ? 'Admin' : 'Benutzer' }}
            </span>
          </td>
          <td>{{ formatDate(user.createdAt) }}</td>
        </tr>
      </tbody>
    </table>

    <div v-if="showCreateForm" class="modal-overlay" @click.self="showCreateForm = false">
      <div class="modal-content">
        <h3>Neuen Benutzer erstellen</h3>
        
        <form @submit.prevent="handleCreateUser">
          <div class="form-group">
            <label for="vorname">Vorname</label>
            <input id="vorname" v-model="newUser.vorname" type="text" required />
          </div>
          
          <div class="form-group">
            <label for="nachname">Nachname</label>
            <input id="nachname" v-model="newUser.nachname" type="text" required />
          </div>
          
          <div class="form-group">
            <label for="email">E-Mail</label>
            <input id="email" v-model="newUser.email" type="email" required />
          </div>
          
          <div class="form-group">
            <label for="password">Passwort</label>
            <input id="password" v-model="newUser.password" type="password" required />
          </div>
          
          <div class="form-group">
            <label for="role">Rolle</label>
            <select id="role" v-model="newUser.role" required>
              <option value="USER">Benutzer</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          
          <div v-if="error" class="error-message">{{ error }}</div>
          
          <div class="modal-actions">
            <button type="button" @click="showCreateForm = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="creating">
              {{ creating ? 'Erstellen...' : 'Erstellen' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { authService } from '@/services/authService'

const authStore = useAuthStore()

const users = ref([])
const loading = ref(true)
const error = ref(null)
const showCreateForm = ref(false)
const creating = ref(false)

const newUser = ref({
  vorname: '',
  nachname: '',
  email: '',
  password: '',
  role: 'USER'
})

onMounted(async () => {
  await loadUsers()
})

async function loadUsers() {
  loading.value = true
  error.value = null
  try {
    users.value = await authService.getAllUsers()
  } catch (err) {
    error.value = 'Fehler beim Laden der Benutzer'
  }
  loading.value = false
}

async function handleCreateUser() {
  creating.value = true
  error.value = null
  try {
    await authStore.createUser(newUser.value)
    showCreateForm.value = false
    newUser.value = { vorname: '', nachname: '', email: '', password: '', role: 'USER' }
    await loadUsers()
  } catch (err) {
    error.value = authStore.error || 'Fehler beim Erstellen des Benutzers'
  }
  creating.value = false
}

function formatDate(dateString) {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('de-DE')
}
</script>

<style scoped>
.admin-users-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  margin-bottom: 20px;
}

.btn-primary {
  padding: 10px 20px;
  background: #4a5568;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

.btn-primary:hover {
  background: #2d3748;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #666;
}

.users-table {
  width: 100%;
  margin-top: 20px;
  border-collapse: collapse;
}

.users-table th,
.users-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.users-table th {
  background: #f7fafc;
  font-weight: 600;
}

.role-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.85rem;
}

.role-badge.admin {
  background: #e9d8fd;
  color: #6b46c1;
}

.role-badge.user {
  background: #c6f6d5;
  color: #2f855a;
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
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.modal-actions button {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.modal-actions button:not(.btn-secondary) {
  background: #4a5568;
  color: white;
}

.btn-secondary {
  background: #e2e8f0;
  color: #333;
}
</style>
