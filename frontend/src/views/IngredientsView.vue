<template>
  <div class="ingredients-container">
    <div class="ingredients-header">
      <h1>Zutaten</h1>
      <button v-if="isAdmin" @click="openCreateModal" class="btn-primary">Neue Zutat</button>
    </div>

    <div v-if="loading" class="loading">Laden...</div>
    <div v-else-if="error" class="error-banner">{{ error }}</div>
    <div v-else-if="entries.length === 0" class="empty">Noch keine Zutaten im Katalog.</div>

    <div v-else>
      <div v-for="group in groupedEntries" :key="group.unit" class="unit-group">
        <h2 class="unit-heading">{{ group.unit || '—' }}</h2>
        <table class="ingredients-table">
          <thead>
            <tr>
              <th @click="setSort('nameDe')" class="sortable">
                Name <span class="sort-indicator">{{ sortIndicator('nameDe') }}</span>
              </th>
              <th @click="setSort('nameEn')" class="sortable">
                Name (EN) <span class="sort-indicator">{{ sortIndicator('nameEn') }}</span>
              </th>
              <th @click="setSort('gramsPerUnit')" class="sortable">
                g/Einheit <span class="sort-indicator">{{ sortIndicator('gramsPerUnit') }}</span>
              </th>
              <th v-if="activeFilter" class="col-conversion">Umrechnung</th>
              <th @click="setSort('calories100g')" class="sortable">
                kcal/100g <span class="sort-indicator">{{ sortIndicator('calories100g') }}</span>
              </th>
              <th @click="setSort('fat100g')" class="sortable">
                Fett/100g <span class="sort-indicator">{{ sortIndicator('fat100g') }}</span>
              </th>
              <th @click="setSort('protein100g')" class="sortable">
                Protein/100g <span class="sort-indicator">{{ sortIndicator('protein100g') }}</span>
              </th>
              <th @click="setSort('carbs100g')" class="sortable">
                KH/100g <span class="sort-indicator">{{ sortIndicator('carbs100g') }}</span>
              </th>
              <th @click="setSort('fiber100g')" class="sortable">
                Ballaststoffe/100g <span class="sort-indicator">{{ sortIndicator('fiber100g') }}</span>
              </th>
              <th>Schätzung</th>
              <th v-if="isAdmin"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in group.entries" :key="entry.unitId" @click="openDetailModal(entry)" class="clickable-row">
              <td>{{ entry.nameDe }}</td>
              <td class="col-en">{{ entry.nameEn }}</td>
              <td>{{ fmt(entry.gramsPerUnit) }} g</td>
              <td v-if="activeFilter" class="col-conversion">1 {{ entry.unitDescription }} = {{ fmt(entry.gramsPerUnit) }} g</td>
              <td>{{ fmt(entry.calories100g) }}</td>
              <td>{{ fmt(entry.fat100g) }}</td>
              <td>{{ fmt(entry.protein100g) }}</td>
              <td>{{ fmt(entry.carbs100g) }}</td>
              <td>{{ fmt(entry.fiber100g) }}</td>
              <td>
                <span v-if="entry.isEstimated" class="badge-estimated">&#9888; Geschätzt</span>
              </td>
              <td v-if="isAdmin" class="actions-cell" @click.stop>
                <button @click="openEditUnitModal(entry)" class="btn-edit">Einheit</button>
                <button @click="openEditIngredientModal(entry)" class="btn-edit">Nährwerte</button>
                <button @click="openDeleteConfirm(entry)" class="btn-delete">Löschen</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="selectedEntry" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content">
        <h3>{{ selectedEntry.nameDe }} <span class="unit-label">pro 100g</span></h3>
        <p class="name-en">{{ selectedEntry.nameEn }}</p>
        <p v-if="selectedEntry.isEstimated" class="estimated-notice">&#9888; Diese Werte sind eine KI-Schätzung und wurden noch nicht geprüft.</p>
        <table class="detail-table">
          <tbody>
            <tr><td>Energie</td><td>{{ fmt(selectedEntry.calories100g) }} kcal</td></tr>
            <tr><td>Fett</td><td>{{ fmt(selectedEntry.fat100g) }} g</td></tr>
            <tr><td>Protein</td><td>{{ fmt(selectedEntry.protein100g) }} g</td></tr>
            <tr><td>Kohlenhydrate</td><td>{{ fmt(selectedEntry.carbs100g) }} g</td></tr>
            <tr><td>Ballaststoffe</td><td>{{ fmt(selectedEntry.fiber100g) }} g</td></tr>
            <tr><td>g pro 1 {{ selectedEntry.unitDescription }}</td><td>{{ fmt(selectedEntry.gramsPerUnit) }} g</td></tr>
          </tbody>
        </table>
        <div class="modal-actions">
          <button @click="closeDetailModal" class="btn-secondary">Schließen</button>
        </div>
      </div>
    </div>

    <div v-if="showEditUnitModal" class="modal-overlay" @click.self="showEditUnitModal = false">
      <div class="modal-content">
        <h3>Einheit bearbeiten</h3>
        <form @submit.prevent="saveEditUnit">
          <div class="form-group">
            <label>Name (DE)</label>
            <input v-model="editUnitForm.nameDe" type="text" required />
          </div>
          <div class="form-group">
            <label>Einheit</label>
            <input v-model="editUnitForm.unitDescription" type="text" required />
          </div>
          <div class="form-group">
            <label>Gramm pro 1 Einheit</label>
            <input v-model.number="editUnitForm.gramsPerUnit" type="number" step="0.1" min="0" required />
          </div>
          <div v-if="modalError" class="error-message">{{ modalError }}</div>
          <div class="modal-actions">
            <button type="button" @click="showEditUnitModal = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="saving" class="btn-primary">{{ saving ? 'Speichern...' : 'Speichern' }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showEditIngredientModal" class="modal-overlay" @click.self="showEditIngredientModal = false">
      <div class="modal-content">
        <h3>Nährwerte bearbeiten</h3>
        <p class="modal-subtitle">{{ editIngredientForm.nameEn }}</p>
        <form @submit.prevent="saveEditIngredient">
          <div class="form-group">
            <label>Name (EN)</label>
            <input v-model="editIngredientForm.nameEn" type="text" required />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>kcal/100g</label>
              <input v-model.number="editIngredientForm.calories100g" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Fett/100g</label>
              <input v-model.number="editIngredientForm.fat100g" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Protein/100g</label>
              <input v-model.number="editIngredientForm.protein100g" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>KH/100g</label>
              <input v-model.number="editIngredientForm.carbs100g" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Ballaststoffe/100g</label>
              <input v-model.number="editIngredientForm.fiber100g" type="number" step="0.01" min="0" />
            </div>
          </div>
          <div class="form-group form-group-checkbox">
            <label>
              <input v-model="editIngredientForm.isEstimated" type="checkbox" />
              Schätzung (nicht verifiziert)
            </label>
          </div>
          <div v-if="modalError" class="error-message">{{ modalError }}</div>
          <div class="modal-actions">
            <button type="button" @click="showEditIngredientModal = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="saving" class="btn-primary">{{ saving ? 'Speichern...' : 'Speichern' }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal-content">
        <h3>Neue Zutat</h3>
        <form @submit.prevent="createEntry">
          <div class="form-group">
            <label>Name (DE)</label>
            <input v-model="createForm.nameDe" type="text" required />
          </div>
          <div class="form-group">
            <label>Name (EN)</label>
            <input v-model="createForm.nameEn" type="text" required />
          </div>
          <div class="form-group">
            <label>Einheit</label>
            <input v-model="createForm.unitDescription" type="text" placeholder="z.B. g, ml, Stück" required />
          </div>
          <div class="form-group">
            <label>Gramm pro 1 Einheit</label>
            <input v-model.number="createForm.gramsPerUnit" type="number" step="0.1" min="0" required />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>kcal/100g</label>
              <input v-model.number="createForm.calories100g" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Fett/100g</label>
              <input v-model.number="createForm.fat100g" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Protein/100g</label>
              <input v-model.number="createForm.protein100g" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>KH/100g</label>
              <input v-model.number="createForm.carbs100g" type="number" step="0.01" min="0" />
            </div>
            <div class="form-group">
              <label>Ballaststoffe/100g</label>
              <input v-model.number="createForm.fiber100g" type="number" step="0.01" min="0" />
            </div>
          </div>
          <div class="form-group form-group-checkbox">
            <label>
              <input v-model="createForm.isEstimated" type="checkbox" />
              Schätzung (nicht verifiziert)
            </label>
          </div>
          <div v-if="modalError" class="error-message">{{ modalError }}</div>
          <div class="modal-actions">
            <button type="button" @click="showCreateModal = false" class="btn-secondary">Abbrechen</button>
            <button type="submit" :disabled="saving" class="btn-primary">{{ saving ? 'Erstellen...' : 'Erstellen' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { ingredientCatalogService } from '@/services/ingredientCatalogService'

const route = useRoute()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

const entries = ref([])
const loading = ref(true)
const error = ref(null)
const saving = ref(false)
const modalError = ref(null)

const sortKey = ref('nameDe')
const sortDir = ref('asc')
const activeFilter = ref(null)

const selectedEntry = ref(null)
const showEditUnitModal = ref(false)
const editUnitForm = ref({})
const showEditIngredientModal = ref(false)
const editIngredientForm = ref({})
const showCreateModal = ref(false)
const createForm = ref(emptyForm())

function emptyForm() {
  return {
    nameDe: '', nameEn: '', unitDescription: '', gramsPerUnit: null,
    calories100g: null, fat100g: null, protein100g: null, carbs100g: null, fiber100g: null,
    isEstimated: false
  }
}

onMounted(async () => {
  if (route.query.sort) sortKey.value = route.query.sort
  if (route.query.dir) sortDir.value = route.query.dir
  if (route.query.filter) {
    activeFilter.value = route.query.filter.split(',').map(s => s.toLowerCase())
  }
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

const groupedEntries = computed(() => {
  let result = [...entries.value]
  if (activeFilter.value) {
    result = result.filter(e =>
      activeFilter.value.includes(`${e.nameDe}|${e.unitDescription}`.toLowerCase())
    )
  }
  result.sort((a, b) => {
    const av = a[sortKey.value]
    const bv = b[sortKey.value]
    if (av == null && bv == null) return 0
    if (av == null) return 1
    if (bv == null) return -1
    if (typeof av === 'string') {
      return sortDir.value === 'asc' ? av.localeCompare(bv, 'de') : bv.localeCompare(av, 'de')
    }
    return sortDir.value === 'asc' ? av - bv : bv - av
  })
  const map = new Map()
  for (const entry of result) {
    const unit = entry.unitDescription || ''
    if (!map.has(unit)) map.set(unit, [])
    map.get(unit).push(entry)
  }
  return [...map.entries()]
    .sort(([a], [b]) => a.localeCompare(b, 'de'))
    .map(([unit, unitEntries]) => ({ unit, entries: unitEntries }))
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
}

function openEditUnitModal(entry) {
  editUnitForm.value = { unitId: entry.unitId, nameDe: entry.nameDe, unitDescription: entry.unitDescription, gramsPerUnit: entry.gramsPerUnit }
  modalError.value = null
  showEditUnitModal.value = true
}

async function saveEditUnit() {
  saving.value = true
  modalError.value = null
  try {
    const updated = await ingredientCatalogService.updateUnit(editUnitForm.value.unitId, editUnitForm.value)
    const idx = entries.value.findIndex(e => e.unitId === updated.unitId)
    if (idx !== -1) entries.value[idx] = { ...entries.value[idx], ...updated }
    showEditUnitModal.value = false
  } catch (err) {
    modalError.value = err.message || 'Fehler beim Speichern.'
  } finally {
    saving.value = false
  }
}

function openEditIngredientModal(entry) {
  editIngredientForm.value = {
    ingredientId: entry.ingredientId,
    nameEn: entry.nameEn,
    calories100g: entry.calories100g,
    fat100g: entry.fat100g,
    protein100g: entry.protein100g,
    carbs100g: entry.carbs100g,
    fiber100g: entry.fiber100g,
    isEstimated: entry.isEstimated
  }
  modalError.value = null
  showEditIngredientModal.value = true
}

async function saveEditIngredient() {
  saving.value = true
  modalError.value = null
  try {
    await ingredientCatalogService.updateIngredient(editIngredientForm.value.ingredientId, editIngredientForm.value)
    entries.value = entries.value.map(e => {
      if (e.ingredientId === editIngredientForm.value.ingredientId) {
        return { ...e, ...editIngredientForm.value }
      }
      return e
    })
    showEditIngredientModal.value = false
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
  if (!confirm(`"${entry.nameDe} (${entry.unitDescription || '—'})" wirklich löschen?`)) return
  try {
    await ingredientCatalogService.deleteUnit(entry.unitId)
    entries.value = entries.value.filter(e => e.unitId !== entry.unitId)
  } catch (err) {
    error.value = err.message || 'Fehler beim Löschen.'
  }
}
</script>

<style scoped>
.ingredients-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.ingredients-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.ingredients-header h1 { margin: 0; }

.unit-group { margin-bottom: 32px; }

.unit-heading {
  font-size: 1rem;
  font-weight: 600;
  color: #4a5568;
  margin: 0 0 8px 0;
  padding-bottom: 6px;
  border-bottom: 2px solid #e2e8f0;
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
  margin-right: 4px;
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
  font-size: 0.875rem;
}

.ingredients-table th,
.ingredients-table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.ingredients-table th {
  background: #f7fafc;
  font-weight: 600;
  white-space: nowrap;
}

.sortable { cursor: pointer; user-select: none; }
.sortable:hover { background: #edf2f7; }

.sort-indicator {
  color: #a0aec0;
  font-size: 0.75rem;
  margin-left: 4px;
}

.clickable-row { cursor: pointer; }
.clickable-row:hover td { background: #f7fafc; }

.actions-cell { white-space: nowrap; }
.col-en { color: #718096; font-size: 0.8rem; }
.col-conversion { font-style: italic; color: #4a5568; }

.badge-estimated {
  background: #fef3c7;
  color: #92400e;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  white-space: nowrap;
}

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
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin: 0 0 4px 0;
  font-size: 1.25rem;
}

.unit-label {
  font-size: 0.875rem;
  color: #718096;
  font-weight: 400;
}

.name-en, .modal-subtitle {
  color: #718096;
  font-size: 0.875rem;
  margin: 0 0 16px 0;
}

.estimated-notice {
  background: #fef3c7;
  color: #92400e;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 0.875rem;
  margin-bottom: 16px;
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

.form-group { margin-bottom: 14px; }

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  font-size: 0.875rem;
  color: #4a5568;
}

.form-group input[type='text'],
.form-group input[type='number'] {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  box-sizing: border-box;
}

.form-group-checkbox label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 400;
  cursor: pointer;
}

.form-group-checkbox input[type='checkbox'] { width: auto; }

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 10px;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
