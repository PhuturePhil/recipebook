<template>
  <div class="ingredients-container">
    <div class="ingredients-header">
      <h1>Zutaten</h1>
      <button v-if="isAdmin && tab === 'ingredients'" @click="openCreate" class="btn-primary">Neue Zutat</button>
    </div>

    <div class="tabs">
      <button :class="['tab', { active: tab === 'ingredients' }]" @click="tab = 'ingredients'">Katalog</button>
      <button :class="['tab', { active: tab === 'conversions' }]" @click="tab = 'conversions'">Umrechnungen</button>
      <button v-if="isAdmin" :class="['tab', { active: tab === 'ai' }]" @click="openAiTab">KI-Anfragen</button>
    </div>

    <div v-if="error" class="error-banner">{{ error }}</div>

    <section v-if="tab === 'ingredients'">
      <div class="filters">
        <input v-model="search" type="search" placeholder="Zutat oder Schreibweise suchen…" class="search-input" />
        <select v-model="sourceFilter" class="select">
          <option value="">Alle Quellen</option>
          <option value="BLS">BLS</option>
          <option value="MANUAL">manuell</option>
          <option value="AI_ESTIMATE">KI-Schätzung</option>
          <option value="MISSING">ohne Werte</option>
        </select>
      </div>
      <p class="hint">Alle Werte pro 100 g. {{ filtered.length }} von {{ entries.length }} Zutaten.</p>

      <div v-if="loading" class="loading">Laden…</div>
      <div v-else class="table-scroll">
        <table class="ingredients-table">
          <thead>
            <tr>
              <th @click="setSort('name')" class="sortable">Name {{ sortIndicator('name') }}</th>
              <th>Quelle</th>
              <th @click="setSort('kcal')" class="sortable num">kcal {{ sortIndicator('kcal') }}</th>
              <th @click="setSort('protein')" class="sortable num">Eiweiß {{ sortIndicator('protein') }}</th>
              <th @click="setSort('fat')" class="sortable num">Fett {{ sortIndicator('fat') }}</th>
              <th @click="setSort('carbs')" class="sortable num">KH {{ sortIndicator('carbs') }}</th>
              <th @click="setSort('fiber')" class="sortable num">Ballastst. {{ sortIndicator('fiber') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in filtered" :key="entry.id" class="clickable-row" @click="openDetail(entry)">
              <td>
                {{ entry.name }}
                <span v-if="entry.referenceName" class="sub">{{ entry.referenceName }}</span>
              </td>
              <td><span :class="['source-chip', `source-chip--${entry.source.toLowerCase()}`]">{{ SOURCE_LABELS[entry.source] }}</span></td>
              <td class="num">{{ fmt(entry.per100g?.kcal, 0) }}</td>
              <td class="num">{{ fmt(entry.per100g?.protein) }}</td>
              <td class="num">{{ fmt(entry.per100g?.fat) }}</td>
              <td class="num">{{ fmt(entry.per100g?.carbs) }}</td>
              <td class="num">{{ fmt(entry.per100g?.fiber) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="tab === 'conversions'">
      <p class="hint">
        Gramm pro Einheit. Gesucht wird zuerst eine Umrechnung für die Zutat, dann für ihre Zutat-Klasse, dann die
        allgemeine. Die Einheit „ml“ steht für die Dichte (Gramm pro ml). Stückgewichte pflegst du direkt bei der Zutat.
      </p>
      <div class="table-scroll">
        <table class="ingredients-table">
          <thead>
            <tr>
              <th>Einheit</th>
              <th>gilt für</th>
              <th class="num">Gramm</th>
              <th>Quelle</th>
              <th>Notiz</th>
              <th v-if="isAdmin"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in globalConversions" :key="c.id">
              <td>{{ c.unit }}</td>
              <td>{{ c.ingredientClass ? classLabel(c.ingredientClass) : 'alle' }}</td>
              <td class="num">{{ fmt(c.grams) }}</td>
              <td><span :class="['source-chip', `source-chip--${c.source.toLowerCase()}`]">{{ SOURCE_LABELS[c.source] }}</span></td>
              <td class="sub">{{ c.note }}</td>
              <td v-if="isAdmin" class="actions-cell">
                <button class="btn-edit" @click="editConversion(c)">Bearbeiten</button>
                <button class="btn-delete" @click="removeConversion(c)">Löschen</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <button v-if="isAdmin" class="btn-secondary add-btn" @click="editConversion({ unit: '', ingredientClass: '', grams: null, note: '' })">
        Umrechnung hinzufügen
      </button>
    </section>

    <section v-if="tab === 'ai' && isAdmin">
      <p class="hint">
        Die KI wird pro Zutat bzw. Zutat+Einheit höchstens einmal gefragt. Fehlgeschlagene Anfragen werden nicht
        automatisch wiederholt – nur hier per Hand.
      </p>
      <button class="btn-secondary add-btn" :disabled="saving" @click="resolveUnknown">Unbekannte Zutaten jetzt zuordnen</button>
      <p v-if="aiMessage" class="hint">{{ aiMessage }}</p>
      <div class="table-scroll">
        <table class="ingredients-table">
          <thead>
            <tr>
              <th>Zutat</th>
              <th>Art</th>
              <th>Status</th>
              <th>Versuche</th>
              <th>Letzter Versuch</th>
              <th>Fehler</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in aiRequests" :key="r.id">
              <td>{{ r.ingredientName }}<span v-if="r.unit" class="sub">Einheit: {{ r.unit }}</span></td>
              <td>{{ r.kind === 'NAME_MATCH' ? 'BLS-Zuordnung' : 'Gewicht je Einheit' }}</td>
              <td><span :class="['status', `status--${r.status.toLowerCase()}`]">{{ AI_STATUS[r.status] }}</span></td>
              <td class="num">{{ r.attempts }}</td>
              <td>{{ fmtDate(r.lastAttemptAt) }}</td>
              <td class="sub">{{ r.error }}</td>
              <td><button v-if="r.status === 'FAILED'" class="btn-edit" @click="retry(r)">Erneut versuchen</button></td>
            </tr>
            <tr v-if="!aiRequests.length"><td colspan="7" class="sub">Keine Anfragen.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="selected" class="modal-overlay" @click.self="closeDetail">
      <div class="modal-content">
        <div class="modal-head">
          <h3>{{ selected.name }}</h3>
          <button class="btn-close" @click="closeDetail" aria-label="Schließen">&times;</button>
        </div>
        <div v-if="modalError" class="error-message">{{ modalError }}</div>

        <p class="detail-source">
          <span :class="['source-chip', `source-chip--${selected.source.toLowerCase()}`]">{{ SOURCE_LABELS[selected.source] }}</span>
          <template v-if="selected.referenceCode"> BLS {{ selected.referenceCode }} – {{ selected.referenceName }}</template>
          · Klasse: {{ classLabel(selected.ingredientClass) }}
          <template v-if="selected.negligible"> · ohne Menge vernachlässigbar</template>
        </p>
        <p v-if="selected.note" class="sub">{{ selected.note }}</p>

        <table class="detail-table">
          <tbody>
            <tr v-for="row in VALUE_ROWS" :key="row.label + row.unit">
              <td>{{ row.label }}</td>
              <td>{{ fmt(selected.per100g?.[row.key], row.key === 'kcal' || row.key === 'kj' ? 0 : 1) }} {{ row.unit }}</td>
            </tr>
          </tbody>
        </table>

        <h4>Schreibweisen (Aliasse)</h4>
        <div class="chips">
          <span v-for="a in selected.aliases" :key="a.id" class="alias-chip">
            {{ a.alias }}
            <button v-if="isAdmin" class="chip-remove" @click="removeAlias(a)" title="Entfernen">&times;</button>
          </span>
          <span v-if="!selected.aliases.length" class="sub">keine</span>
        </div>
        <form v-if="isAdmin" class="inline-form" @submit.prevent="addAlias">
          <input v-model="newAlias" placeholder="weitere Schreibweise" />
          <button class="btn-secondary" :disabled="!newAlias.trim() || saving">Hinzufügen</button>
        </form>

        <h4>Gewicht je Einheit</h4>
        <ul class="conversion-list">
          <li v-for="c in selected.conversions" :key="c.id">
            1 {{ c.unit }} = {{ fmt(c.grams) }} g
            <span :class="['source-chip', `source-chip--${c.source.toLowerCase()}`]">{{ SOURCE_LABELS[c.source] }}</span>
            <span v-if="c.note" class="sub">{{ c.note }}</span>
            <template v-if="isAdmin">
              <button class="btn-edit" @click="editConversion(c)">Bearbeiten</button>
              <button class="btn-delete" @click="removeConversion(c)">Löschen</button>
            </template>
          </li>
          <li v-if="!selected.conversions.length" class="sub">keine zutatenspezifischen Umrechnungen</li>
        </ul>
        <button v-if="isAdmin" class="btn-secondary" @click="editConversion({ unit: 'Stück', ingredientId: selected.id, grams: null, note: '' })">
          Umrechnung hinzufügen
        </button>

        <template v-if="selected.legacyValues.length">
          <h4>Altwerte (bis zur Umstellung, KI-Werte pro Einheit)</h4>
          <ul class="legacy-list">
            <li v-for="l in selected.legacyValues" :key="l.id">
              <strong>{{ l.name }}</strong> ({{ l.unit || 'ohne Einheit' }}): {{ fmt(l.kcal) }} kcal pro 1 {{ l.unit || 'Einheit' }}
              <span class="sub">
                <template v-if="l.per100g">≙ {{ fmt(l.per100g.kcal, 0) }} kcal / 100 g</template>
                <template v-else>nicht in Gramm umrechenbar</template>
              </span>
              <button v-if="isAdmin && l.per100g" class="btn-edit" @click="adoptLegacy(l)">
                als manuellen Wert übernehmen
              </button>
            </li>
          </ul>
        </template>

        <template v-if="isAdmin">
          <h4>Bearbeiten</h4>
          <IngredientForm v-model="form" :saving="saving" @submit="saveSelected" />
          <div class="modal-actions">
            <button class="btn-delete" @click="removeSelected">Zutat löschen</button>
          </div>
        </template>
      </div>
    </div>

    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal-content">
        <div class="modal-head">
          <h3>Neue Zutat</h3>
          <button class="btn-close" @click="showCreate = false" aria-label="Schließen">&times;</button>
        </div>
        <div v-if="modalError" class="error-message">{{ modalError }}</div>
        <IngredientForm v-model="form" :saving="saving" @submit="createEntry" />
      </div>
    </div>

    <div v-if="conversionForm" class="modal-overlay" @click.self="conversionForm = null">
      <div class="modal-content modal-content--small">
        <h3>{{ conversionForm.id ? 'Umrechnung bearbeiten' : 'Umrechnung hinzufügen' }}</h3>
        <div v-if="conversionError" class="error-message">{{ conversionError }}</div>
        <form class="stack-form" @submit.prevent="saveConversion">
          <label>Einheit
            <input v-model="conversionForm.unit" required placeholder="z. B. Stück, EL, Bund, ml" />
          </label>
          <label v-if="!conversionForm.ingredientId">Zutat-Klasse
            <select v-model="conversionForm.ingredientClass">
              <option value="">alle Zutaten</option>
              <option v-for="c in INGREDIENT_CLASSES" :key="c.value" :value="c.value">{{ c.label }}</option>
            </select>
          </label>
          <label>Gramm pro Einheit
            <input v-model.number="conversionForm.grams" type="number" step="0.01" min="0.01" required />
          </label>
          <label>Notiz
            <input v-model="conversionForm.note" />
          </label>
          <div class="modal-actions">
            <button type="button" class="btn-secondary" @click="conversionForm = null">Abbrechen</button>
            <button class="btn-primary" :disabled="saving">Speichern</button>
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
import { ingredientCatalogService, INGREDIENT_CLASSES, classLabel } from '@/services/ingredientCatalogService'
import { SOURCE_LABELS } from '@/services/nutritionService'
import IngredientForm from '@/components/IngredientForm.vue'

const route = useRoute()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

const VALUE_ROWS = [
  { key: 'kcal', label: 'Energie', unit: 'kcal' },
  { key: 'kj', label: 'Energie', unit: 'kJ' },
  { key: 'fat', label: 'Fett', unit: 'g' },
  { key: 'carbs', label: 'Kohlenhydrate', unit: 'g' },
  { key: 'sugar', label: 'davon Zucker', unit: 'g' },
  { key: 'fiber', label: 'Ballaststoffe', unit: 'g' },
  { key: 'protein', label: 'Eiweiß', unit: 'g' },
  { key: 'salt', label: 'Salz', unit: 'g' },
]

const AI_STATUS = { PENDING: 'wartet', RUNNING: 'läuft', DONE: 'erledigt', FAILED: 'fehlgeschlagen' }

const tab = ref('ingredients')
const entries = ref([])
const conversions = ref([])
const aiRequests = ref([])
const loading = ref(true)
const error = ref(null)
const saving = ref(false)
const modalError = ref(null)
const aiMessage = ref(null)

const search = ref('')
const sourceFilter = ref('')
const sortKey = ref('name')
const sortDir = ref('asc')

const selected = ref(null)
const showCreate = ref(false)
const form = ref({})
const newAlias = ref('')
const conversionForm = ref(null)
const conversionError = ref(null)

onMounted(async () => {
  if (route.query.q) search.value = route.query.q
  await Promise.all([load(), loadConversions()])
})

async function load() {
  loading.value = true
  error.value = null
  try {
    entries.value = await ingredientCatalogService.getAll()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function loadConversions() {
  try {
    conversions.value = await ingredientCatalogService.getConversions()
  } catch (err) {
    error.value = err.message
  }
}

async function openAiTab() {
  tab.value = 'ai'
  try {
    aiRequests.value = await ingredientCatalogService.getAiRequests()
  } catch (err) {
    error.value = err.message
  }
}

const normalize = (s) => (s || '').toLowerCase()

const filtered = computed(() => {
  const q = normalize(search.value.trim())
  const result = entries.value.filter(e => {
    if (sourceFilter.value === 'MISSING') {
      if (e.per100g) return false
    } else if (sourceFilter.value && e.source !== sourceFilter.value) {
      return false
    }
    if (!q) return true
    return normalize(e.name).includes(q)
      || normalize(e.referenceName).includes(q)
      || e.aliases.some(a => normalize(a.alias).includes(q))
  })
  return result.sort((a, b) => {
    const av = sortKey.value === 'name' ? a.name : a.per100g?.[sortKey.value]
    const bv = sortKey.value === 'name' ? b.name : b.per100g?.[sortKey.value]
    if (av == null && bv == null) return 0
    if (av == null) return 1
    if (bv == null) return -1
    const cmp = typeof av === 'string' ? av.localeCompare(bv, 'de') : av - bv
    return sortDir.value === 'asc' ? cmp : -cmp
  })
})

const globalConversions = computed(() => conversions.value.filter(c => c.ingredientId == null))

function setSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = key === 'name' ? 'asc' : 'desc'
  }
}

function sortIndicator(key) {
  if (sortKey.value !== key) return ''
  return sortDir.value === 'asc' ? '↑' : '↓'
}

function fmt(val, digits = 1) {
  if (val == null) return '—'
  return Number(val).toLocaleString('de-DE', { maximumFractionDigits: digits })
}

function fmtDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('de-DE', { dateStyle: 'short', timeStyle: 'short' })
}

function toForm(entry) {
  return {
    name: entry?.name ?? '',
    ingredientClass: entry?.ingredientClass ?? 'DEFAULT',
    source: entry?.source ?? 'MANUAL',
    referenceCode: entry?.referenceCode ?? '',
    referenceName: entry?.referenceName ?? '',
    negligible: entry?.negligible ?? false,
    kcal: entry?.manualValues?.kcal ?? null,
    protein: entry?.manualValues?.protein ?? null,
    fat: entry?.manualValues?.fat ?? null,
    carbs: entry?.manualValues?.carbs ?? null,
    fiber: entry?.manualValues?.fiber ?? null,
    sugar: entry?.manualValues?.sugar ?? null,
    salt: entry?.manualValues?.salt ?? null,
    note: entry?.note ?? '',
  }
}

function toRequest(f) {
  const body = {
    name: f.name,
    ingredientClass: f.ingredientClass,
    source: f.source,
    negligible: f.negligible,
    note: f.note,
    referenceCode: f.source === 'BLS' ? f.referenceCode : '',
  }
  if (f.source === 'MANUAL') {
    Object.assign(body, {
      kcal: f.kcal, protein: f.protein, fat: f.fat, carbs: f.carbs, fiber: f.fiber, sugar: f.sugar, salt: f.salt,
    })
  }
  return body
}

function replaceEntry(updated) {
  const idx = entries.value.findIndex(e => e.id === updated.id)
  if (idx !== -1) entries.value[idx] = updated
  else entries.value.push(updated)
  if (selected.value?.id === updated.id) {
    selected.value = updated
    form.value = toForm(updated)
  }
}

function openDetail(entry) {
  selected.value = entry
  form.value = toForm(entry)
  modalError.value = null
  newAlias.value = ''
}

function closeDetail() {
  selected.value = null
  modalError.value = null
}

function openCreate() {
  form.value = toForm(null)
  modalError.value = null
  showCreate.value = true
}

async function run(action) {
  saving.value = true
  modalError.value = null
  try {
    return await action()
  } catch (err) {
    modalError.value = err.message
    return null
  } finally {
    saving.value = false
  }
}

async function saveSelected() {
  const updated = await run(() => ingredientCatalogService.update(selected.value.id, toRequest(form.value)))
  if (updated) replaceEntry(updated)
}

async function createEntry() {
  const created = await run(() => ingredientCatalogService.create(toRequest(form.value)))
  if (created) {
    entries.value.push(created)
    showCreate.value = false
    openDetail(created)
  }
}

async function removeSelected() {
  if (!confirm(`„${selected.value.name}“ wirklich löschen? Rezepte mit dieser Zutat werden dann nicht mehr berechnet.`)) return
  const id = selected.value.id
  const ok = await run(async () => {
    await ingredientCatalogService.delete(id)
    return true
  })
  if (ok) {
    entries.value = entries.value.filter(e => e.id !== id)
    closeDetail()
  }
}

async function addAlias() {
  const updated = await run(() => ingredientCatalogService.addAlias(selected.value.id, newAlias.value.trim()))
  if (updated) {
    replaceEntry(updated)
    newAlias.value = ''
  }
}

async function removeAlias(alias) {
  const updated = await run(() => ingredientCatalogService.removeAlias(selected.value.id, alias.id))
  if (updated) replaceEntry(updated)
}

async function adoptLegacy(legacy) {
  const question = `Altwert „${legacy.name}“ (≙ ${fmt(legacy.per100g.kcal, 0)} kcal/100 g) als manuellen Wert übernehmen? `
    + 'Die BLS-Zuordnung wird dabei ersetzt.'
  if (!confirm(question)) return
  const updated = await run(() => ingredientCatalogService.adoptLegacy(selected.value.id, legacy.id))
  if (updated) replaceEntry(updated)
}

function editConversion(c) {
  conversionForm.value = { ...c, ingredientClass: c.ingredientClass ?? '' }
  conversionError.value = null
}

async function refreshAfterConversionChange() {
  await Promise.all([loadConversions(), load()])
  if (selected.value) {
    const fresh = entries.value.find(e => e.id === selected.value.id)
    if (fresh) selected.value = fresh
  }
}

async function saveConversion() {
  saving.value = true
  conversionError.value = null
  try {
    await ingredientCatalogService.saveConversion({
      ...conversionForm.value,
      ingredientClass: conversionForm.value.ingredientClass || null,
    })
    conversionForm.value = null
    await refreshAfterConversionChange()
  } catch (err) {
    conversionError.value = err.message
  } finally {
    saving.value = false
  }
}

async function removeConversion(c) {
  if (!confirm(`Umrechnung „1 ${c.unit} = ${fmt(c.grams)} g“ löschen?`)) return
  try {
    await ingredientCatalogService.deleteConversion(c.id)
    await refreshAfterConversionChange()
  } catch (err) {
    error.value = err.message
  }
}

async function retry(r) {
  try {
    await ingredientCatalogService.retryAiRequest(r.id)
    aiRequests.value = await ingredientCatalogService.getAiRequests()
  } catch (err) {
    error.value = err.message
  }
}

async function resolveUnknown() {
  saving.value = true
  try {
    const result = await ingredientCatalogService.resolveUnknown()
    aiMessage.value = result.created
      ? `${result.created} neue Anfrage(n) angelegt – die Zuordnung läuft im Hintergrund.`
      : 'Keine neuen Anfragen nötig (oder kein OpenAI-Schlüssel konfiguriert).'
    aiRequests.value = await ingredientCatalogService.getAiRequests()
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.ingredients-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
  text-align: left;
}

.ingredients-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.ingredients-header h1 {
  margin: 0;
  font-size: 2rem;
}

.tabs {
  display: flex;
  gap: 4px;
  border-bottom: 2px solid #e2e8f0;
  margin-bottom: 16px;
  overflow-x: auto;
}

.tab {
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  border-radius: 0;
  padding: 8px 14px;
  margin-bottom: -2px;
  font-weight: 600;
  color: #a0aec0;
  cursor: pointer;
  white-space: nowrap;
}

.tab.active {
  color: #2d3748;
  border-bottom-color: #4a5568;
}

.filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.search-input {
  flex: 1;
  min-width: 200px;
  padding: 8px 10px;
  border: 1px solid #cbd5e0;
  border-radius: 4px;
  font-size: 0.95rem;
}

.select {
  padding: 8px 10px;
  border: 1px solid #cbd5e0;
  border-radius: 4px;
}

.hint {
  font-size: 0.85rem;
  color: #718096;
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
  padding: 8px 16px;
  background: #e2e8f0;
  color: #333;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-secondary:hover { background: #cbd5e0; }
.btn-secondary:disabled { opacity: 0.6; cursor: not-allowed; }

.add-btn {
  margin: 12px 0;
}

.btn-edit {
  padding: 4px 10px;
  background: #ebf8ff;
  color: #2b6cb0;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  margin-left: 6px;
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
  margin-left: 6px;
}

.btn-delete:hover { background: #fed7d7; }

.loading {
  text-align: center;
  padding: 48px 24px;
  color: #666;
}

.error-banner,
.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 10px 14px;
  border-radius: 4px;
  margin-bottom: 12px;
}

.table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.ingredients-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.ingredients-table th,
.ingredients-table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid #edf2f7;
  vertical-align: top;
}

.ingredients-table th {
  background: #f7fafc;
  font-weight: 600;
  color: #4a5568;
  white-space: nowrap;
}

.ingredients-table .num {
  text-align: right;
  white-space: nowrap;
}

.sortable {
  cursor: pointer;
  user-select: none;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background: #f7fafc;
}

.sub {
  display: block;
  font-size: 0.75rem;
  color: #a0aec0;
}

.actions-cell {
  white-space: nowrap;
}

.source-chip {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 600;
  white-space: nowrap;
}

.source-chip--bls { background: #e6fffa; color: #234e52; }
.source-chip--manual { background: #ebf8ff; color: #2a4365; }
.source-chip--ai_estimate { background: #fffaf0; color: #7b341e; border: 1px dashed #ed8936; }

.status {
  font-size: 0.8rem;
  font-weight: 600;
}

.status--failed { color: #c53030; }
.status--done { color: #276749; }
.status--pending,
.status--running { color: #b7791f; }

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  z-index: 1000;
  overflow-y: auto;
  padding: 24px 12px;
}

.modal-content {
  background: white;
  color: #2d3748;
  padding: 20px 24px;
  border-radius: 8px;
  width: 100%;
  max-width: 640px;
}

.modal-content--small {
  max-width: 420px;
}

.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.modal-head h3 {
  margin: 0 0 8px;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  padding: 0 4px;
  cursor: pointer;
  color: #718096;
}

.modal-content h4 {
  margin: 20px 0 8px;
  font-size: 0.95rem;
  color: #4a5568;
}

.detail-source {
  font-size: 0.85rem;
  color: #4a5568;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.detail-table td {
  padding: 5px 8px;
  border-bottom: 1px solid #edf2f7;
}

.detail-table td:last-child {
  text-align: right;
  font-weight: 600;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.alias-chip {
  background: #edf2f7;
  border-radius: 12px;
  padding: 2px 10px;
  font-size: 0.8rem;
}

.chip-remove {
  background: none;
  border: none;
  padding: 0 0 0 4px;
  cursor: pointer;
  color: #c53030;
}

.inline-form {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.inline-form input {
  flex: 1;
  padding: 6px 8px;
  border: 1px solid #cbd5e0;
  border-radius: 4px;
}

.conversion-list,
.legacy-list {
  list-style: none;
  padding: 0;
  margin: 0 0 8px;
  font-size: 0.85rem;
}

.conversion-list li,
.legacy-list li {
  padding: 6px 0;
  border-bottom: 1px solid #edf2f7;
}

.stack-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stack-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #4a5568;
}

.stack-form input,
.stack-form select {
  padding: 8px;
  border: 1px solid #cbd5e0;
  border-radius: 4px;
  font-size: 0.95rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

@media (max-width: 600px) {
  .ingredients-container {
    padding: 12px;
  }

  .ingredients-table th,
  .ingredients-table td {
    padding: 6px;
    font-size: 0.8rem;
  }

  .modal-content {
    padding: 16px;
  }
}
</style>
