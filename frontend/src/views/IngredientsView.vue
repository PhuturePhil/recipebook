<template>
  <div class="ingredients-container">
    <div class="ingredients-header">
      <h1>Zutaten</h1>
      <button v-if="isAdmin" @click="openCreateModal" class="btn-primary">Neue Zutat</button>
    </div>

    <div v-if="loading" class="loading">Laden...</div>
    <div v-else-if="error" class="error-banner">{{ error }}</div>
    <div v-else-if="entries.length === 0" class="empty">Noch keine Zutaten im Katalog.</div>

    <table v-else class="ingredients-table">
      <thead>
        <tr>
          <th @click="setSort('name')" class="sortable">
            Name <span class="sort-indicator">{{ sortIndicator('name') }}</span>
          </th>
          <th @click="setSort('unit')" class="sortable">
            Einheit <span class="sort-indicator">{{ sortIndicator('unit') }}</span>
          </th>
          <th @click="setSort('nutritionKcal')" class="sortable">
            kcal <span class="sort-indicator">{{ sortIndicator('nutritionKcal') }}</span>
          </th>
          <th @click="setSort('nutritionFat')" class="sortable">
            Fett (g) <span class="sort-indicator">{{ sortIndicator('nutritionFat') }}</span>
          </th>
          <th @click="setSort('nutritionProtein')" class="sortable">
            Protein (g) <span class="sort-indicator">{{ sortIndicator('nutritionProtein') }}</span>
          </th>
          <th @click="setSort('nutritionCarbs')" class="sortable">
            KH (g) <span class="sort-indicator">{{ sortIndicator('nutritionCarbs') }}</span>
          </th>
          <th @click="setSort('nutritionFiber')" class="sortable">
            Ballaststoffe (g) <span class="sort-indicator">{{ sortIndicator('nutritionFiber') }}</span>
          </th>
          <th v-if="isAdmin"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="entry in sortedEntries" :key="entry.id" @click="openDetailModal(entry)" class="clickable-row">
          <td>{{ entry.name }}</td>
          <td>{{ entry.unit || '—' }}</td>
          <td>{{ fmt(entry.nutritionKcal) }}</td>
          <td>{{ fmt(entry.nutritionFat) }}</td>
          <td>{{ fmt(entry.nutritionProtein) }}</td>
          <td>{{ fmt(entry.nutritionCarbs) }}</td>
          <td>{{ fmt(entry.nutritionFiber) }}</td>
          <td v-if="isAdmin" class="actions-cell" @click.stop>
            <button @click="openEditModal(entry)" class="btn-edit">Bearbeiten</button>
            <button @click="openDeleteConfirm(entry)" class="btn-delete">Löschen</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="selectedEntry" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content">
        <h3>{{ selectedEntry.name }} <span class="unit-label">pro 1 {{ selectedEntry.unit || 'Einheit' }}</span></h3>

        <table class="detail-table">
          <tbody>
            <tr>
              <td>Energie</td>
              <td>{{ fmt(selectedEntry.nutritionKcal) }} kcal</td>
            </tr>
            <tr>
              <td>Fett</td>
              <td>{{ fmt(selectedEntry.nutritionFat) }} g</td>
            </tr>
            <tr>
              <td>Protein</td>
              <td>{{ fmt(selectedEntry.nutritionProtein) }} g</td>
            </tr>
            <tr>
              <td>Kohlenhydrate</td>
              <td>{{ fmt(selectedEntry.nutritionCarbs) }} g</td>
            </tr>
            <tr>
              <td>Ballaststoffe</td>
              <td>{{ fmt(selectedEntry.nutritionFiber) }} g</td>
            </tr>
          </tbody>
        </table>

        <div class="modal-actions">
          <button @click="closeDetailModal" class="btn-secondary">Schließen</button>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-content">
        <h3>Zutat bearbeiten</h3>
        <form @submit.prevent="saveEdit">
          <div class="form-group">
            <label>Name</label>
            <input v-model="editForm.name" type="text" required />
          </div>
          <div class="form-group">
            <label>Einheit</label>
            <input v-model="editForm.unit" type="text" placeholder="z.B. g, ml, Stück" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>kcal</label>
              <input v-model.number="editForm.nutritionKcal" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Fett (g)</label>
              <input v-model.number="editForm.nutritionFat" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Protein (g)</label>
              <input v-model.number="editForm.nutritionProtein" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>KH (g)</label>
              <input v-model.number="editForm.nutritionCarbs" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Ballaststoffe (g)</label>
              <input v-model.number="editForm.nutritionFiber" type="number" step="0.01" min="0" />
            </div>
          </div>
          <div v-if="modalError" class="error-message">{{ modalError }}</div>
          <div class="modal-actions">
            <button type="button" @click="showEditModal = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="saving" class="btn-primary">
              {{ saving ? 'Speichern...' : 'Speichern' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal-content">
        <h3>Neue Zutat</h3>
        <form @submit.prevent="createEntry">
          <div class="form-group">
            <label>Name</label>
            <input v-model="createForm.name" type="text" required />
          </div>
          <div class="form-group">
            <label>Einheit <span class="hint-inline">pro 1 Einheit angeben</span></label>
            <input v-model="createForm.unit" type="text" placeholder="z.B. g, ml, Stück" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>kcal</label>
              <input v-model.number="createForm.nutritionKcal" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Fett (g)</label>
              <input v-model.number="createForm.nutritionFat" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Protein (g)</label>
              <input v-model.number="createForm.nutritionProtein" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>KH (g)</label>
              <input v-model.number="createForm.nutritionCarbs" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Ballaststoffe (g)</label>
              <input v-model.number="createForm.nutritionFiber" type="number" step="0.01" min="0" />
            </div>
          </div>
          <div v-if="modalError" class="error-message">{{ modalError }}</div>
          <div class="modal-actions">
            <button type="button" @click="showCreateModal = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="saving" class="btn-primary">
              {{ saving ? 'Erstellen...' : 'Erstellen' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { ingredientCatalogService } from '@/services/ingredientCatalogService'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

const entries = ref([])
const loading = ref(true)
const error = ref(null)
const saving = ref(false)
const modalError = ref(null)

const sortKey = ref('name')
const sortDir = ref('asc')

const selectedEntry = ref(null)

const showEditModal = ref(false)
const editForm = ref({})

const showCreateModal = ref(false)
const createForm = ref(emptyForm())

function emptyForm() {
  return { name: '', unit: '', nutritionKcal: null, nutritionFat: null, nutritionProtein: null, nutritionCarbs: null, nutritionFiber: null }
}

onMounted(async () => {
  await load()
})

async function load() {
  loading.value = true
  error.value = null
  try {
    entries.value = await ingredientCatalogService.getAll()
  } catch (err) {
    error.value = err.message || 'Fehler beim Laden.'
  } finally {
    loading.value = false
  }
}

const sortedEntries = computed(() => {
  return [...entries.value].sort((a, b) => {
    const av = a[sortKey.value]
    const bv = b[sortKey.value]
    if (av == null && bv == null) return 0
    if (av == null) return 1
    if (bv == null) return -1
    if (typeof av === 'string') {
      return sortDir.value === 'asc'
        ? av.localeCompare(bv, 'de')
        : bv.localeCompare(av, 'de')
    }
    return sortDir.value === 'asc' ? av - bv : bv - av
  })
})

function setSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = 'asc'
  }
}

function sortIndicator(key) {
  if (sortKey.value !== key) return '↕'
  return sortDir.value === 'asc' ? '↑' : '↓'
}

function fmt(val) {
  if (val == null) return '—'
  return Number(val).toLocaleString('de-DE', { maximumFractionDigits: 2 })
}

function openDetailModal(entry) {
  selectedEntry.value = entry
  modalError.value = null
}

function closeDetailModal() {
  selectedEntry.value = null
  modalError.value = null
}

function openEditModal(entry) {
  editForm.value = { ...entry }
  modalError.value = null
  showEditModal.value = true
}

async function saveEdit() {
  saving.value = true
  modalError.value = null
  try {
    const updated = await ingredientCatalogService.update(editForm.value.id, editForm.value)
    const idx = entries.value.findIndex(e => e.id === updated.id)
    if (idx !== -1) entries.value[idx] = updated
    showEditModal.value = false
  } catch (err) {
    modalError.value = err.message || 'Fehler beim Speichern.'
  } finally {
    saving.value = false
  }
}

function openCreateModal() {
  createForm.value = emptyForm()
  modalError.value = null
  showCreateModal.value = true
}

async function createEntry() {
  saving.value = true
  modalError.value = null
  try {
    const created = await ingredientCatalogService.create(createForm.value)
    entries.value.push(created)
    showCreateModal.value = false
  } catch (err) {
    modalError.value = err.message || 'Fehler beim Erstellen.'
  } finally {
    saving.value = false
  }
}

async function openDeleteConfirm(entry) {
  if (!confirm(`"${entry.name} (${entry.unit || '—'})" wirklich löschen?`)) return
  try {
    await ingredientCatalogService.delete(entry.id)
    entries.value = entries.value.filter(e => e.id !== entry.id)
  } catch (err) {
    error.value = err.message || 'Fehler beim Löschen.'
  }
}
</script>

<style scoped>
.ingredients-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.ingredients-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.ingredients-header h1 {
  margin: 0;
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

.btn-primary:hover:not(:disabled) { background: #2d3748; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-secondary {
  padding: 10px 20px;
  background: #e2e8f0;
  color: #333;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

.btn-secondary:hover { background: #cbd5e0; }

.btn-edit {
  padding: 4px 10px;
  background: #ebf8ff;
  color: #2b6cb0;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  margin-right: 6px;
}

.btn-edit:hover { background: #bee3f8; }

.btn-delete {
  padding: 4px 10px;
  background: #fff5f5;
  color: #c53030;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.btn-delete:hover { background: #fed7d7; }

.loading, .empty {
  text-align: center;
  padding: 48px 24px;
  color: #666;
}

.error-banner {
  background: #fed7d7;
  color: #c53030;
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 12px;
}

.ingredients-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.ingredients-table th,
.ingredients-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.ingredients-table th {
  background: #f7fafc;
  font-weight: 600;
  white-space: nowrap;
}

.sortable {
  cursor: pointer;
  user-select: none;
}

.sortable:hover { background: #edf2f7; }

.sort-indicator {
  color: #a0aec0;
  font-size: 0.75rem;
  margin-left: 4px;
}

.clickable-row { cursor: pointer; }
.clickable-row:hover td { background: #f7fafc; }

.actions-cell { white-space: nowrap; }

.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  padding: 30px;
  width: 90%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin: 0 0 20px 0;
  font-size: 1.25rem;
}

.unit-label {
  font-size: 0.875rem;
  color: #718096;
  font-weight: 400;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}

.detail-table td {
  padding: 10px 0;
  border-bottom: 1px solid #e2e8f0;
}

.detail-table td:first-child {
  color: #4a5568;
  font-weight: 500;
  width: 50%;
}

.detail-table tr:last-child td { border-bottom: none; }

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  font-size: 0.875rem;
  color: #4a5568;
}

.form-group input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  box-sizing: border-box;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 10px;
}

.hint-inline {
  font-size: 0.75rem;
  color: #718096;
  font-weight: 400;
  margin-left: 4px;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
