<template>
  <div class="admin-users-container">
    <h1>Benutzerverwaltung</h1>

    <button @click="openCreateForm" class="btn-primary">
      Neuen Benutzer erstellen
    </button>

    <div v-if="formError && !showCreateForm && !showEditForm" class="error-banner">{{ formError }}</div>

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
          <th>Aktionen</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ [user.vorname, user.nachname].filter(Boolean).join(' ') || '-' }}</td>
          <td>{{ user.email }}</td>
          <td>
            <span :class="['role-badge', user.role.toLowerCase()]">
              {{ user.role === 'ADMIN' ? 'Admin' : 'Benutzer' }}
            </span>
          </td>
          <td>{{ formatDate(user.createdAt) }}</td>
          <td class="actions-cell">
            <button @click="openEditForm(user)" class="btn-action btn-edit">Bearbeiten</button>
            <button @click="handleDeleteUser(user)" class="btn-action btn-delete">Löschen</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="showCreateForm" class="modal-overlay" @click.self="showCreateForm = false">
      <div class="modal-content">
        <h3>Neuen Benutzer erstellen</h3>

        <form @submit.prevent="handleCreateUser">
          <div class="form-group">
            <label for="vorname">Vorname</label>
            <input id="vorname" v-model="newUser.vorname" type="text" />
          </div>

          <div class="form-group">
            <label for="nachname">Nachname</label>
            <input id="nachname" v-model="newUser.nachname" type="text" />
          </div>

          <div class="form-group">
            <label for="email">E-Mail</label>
            <input id="email" v-model="newUser.email" type="email" required />
          </div>

          <div class="form-group">
            <label for="role">Rolle</label>
            <select id="role" v-model="newUser.role" required>
              <option value="USER">Benutzer</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          <p class="hint">Kein Passwort nötig — der Benutzer wird beim ersten Login zur Passwortvergabe aufgefordert.</p>

          <div v-if="formError" class="error-message">{{ formError }}</div>

          <div class="modal-actions">
            <button type="button" @click="showCreateForm = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="creating">
              {{ creating ? 'Erstellen...' : 'Erstellen' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showEditForm" class="modal-overlay" @click.self="showEditForm = false">
      <div class="modal-content">
        <h3>Benutzer bearbeiten</h3>

        <form @submit.prevent="handleEditUser">
          <div class="form-group">
            <label for="edit-vorname">Vorname</label>
            <input id="edit-vorname" v-model="editUser.vorname" type="text" />
          </div>

          <div class="form-group">
            <label for="edit-nachname">Nachname</label>
            <input id="edit-nachname" v-model="editUser.nachname" type="text" />
          </div>

          <div class="form-group">
            <label for="edit-email">E-Mail</label>
            <input id="edit-email" v-model="editUser.email" type="email" required />
          </div>

          <div class="form-group">
            <label for="edit-role">Rolle</label>
            <select id="edit-role" v-model="editUser.role" required>
              <option value="USER">Benutzer</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          <div v-if="formError" class="error-message">{{ formError }}</div>

          <div class="modal-actions">
            <button type="button" @click="showEditForm = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="saving">
              {{ saving ? 'Speichern...' : 'Speichern' }}
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
const formError = ref(null)
const showCreateForm = ref(false)
const showEditForm = ref(false)
const creating = ref(false)
const saving = ref(false)

const newUser = ref({
  vorname: '',
  nachname: '',
  email: '',
  role: 'USER'
})

const editUser = ref({
  id: null,
  vorname: '',
  nachname: '',
  email: '',
  role: 'USER'
})

onMounted(async () => {
  await loadUsers()
})

async function loadUsers() {
  loading.value = true
  formError.value = null
  try {
    users.value = await authService.getAllUsers()
  } catch (err) {
    formError.value = 'Fehler beim Laden der Benutzer'
  }
  loading.value = false
}

function openCreateForm() {
  newUser.value = { vorname: '', nachname: '', email: '', role: 'USER' }
  formError.value = null
  showCreateForm.value = true
}

function openEditForm(user) {
  editUser.value = {
    id: user.id,
    vorname: user.vorname || '',
    nachname: user.nachname || '',
    email: user.email,
    role: user.role
  }
  formError.value = null
  showEditForm.value = true
}

async function handleCreateUser() {
  creating.value = true
  formError.value = null
  try {
    await authStore.createUser(newUser.value)
    showCreateForm.value = false
    await loadUsers()
  } catch (err) {
    formError.value = authStore.error || 'Fehler beim Erstellen des Benutzers'
  }
  creating.value = false
}

async function handleEditUser() {
  saving.value = true
  formError.value = null
  try {
    await authStore.updateUser(editUser.value.id, {
      vorname: editUser.value.vorname,
      nachname: editUser.value.nachname,
      email: editUser.value.email,
      role: editUser.value.role
    })
    showEditForm.value = false
    await loadUsers()
  } catch (err) {
    formError.value = authStore.error || 'Fehler beim Speichern'
  }
  saving.value = false
}

async function handleDeleteUser(user) {
  const name = [user.vorname, user.nachname].filter(Boolean).join(' ') || user.email
  if (!confirm(`Benutzer "${name}" wirklich loeschen?`)) return

  formError.value = null
  try {
    await authStore.deleteUser(user.id)
    await loadUsers()
  } catch (err) {
    formError.value = authStore.error || 'Fehler beim Loeschen des Benutzers.'
  }
}

function formatDate(dateString) {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('de-DE')
}
</script>

<style scoped>
.admin-users-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  margin-bottom: 20px;
}

.error-banner {
  background: #fed7d7;
  color: #c53030;
  padding: 12px 16px;
  border-radius: 4px;
  margin-top: 16px;
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

.actions-cell {
  white-space: nowrap;
}

.btn-action {
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  margin-right: 6px;
}

.btn-edit {
  background: #ebf8ff;
  color: #2b6cb0;
}

.btn-edit:hover {
  background: #bee3f8;
}

.btn-delete {
  background: #fff5f5;
  color: #c53030;
}

.btn-delete:hover {
  background: #fed7d7;
}

.hint {
  font-size: 0.85rem;
  color: #718096;
  margin-bottom: 12px;
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
