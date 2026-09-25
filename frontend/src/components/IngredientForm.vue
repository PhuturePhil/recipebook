<template>
  <form class="ingredient-form" @submit.prevent="emit('submit')">
    <label>Name
      <input v-model="model.name" required />
    </label>

    <div class="row">
      <label>Zutat-Klasse
        <select v-model="model.ingredientClass">
          <option v-for="c in INGREDIENT_CLASSES" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
      </label>
      <label>Quelle der Nährwerte
        <select v-model="model.source">
          <option value="BLS">BLS-Eintrag</option>
          <option value="MANUAL">manuell pro 100 g</option>
          <option v-if="initialSource === 'AI_ESTIMATE'" value="AI_ESTIMATE">KI-Schätzung beibehalten</option>
        </select>
      </label>
    </div>

    <label class="checkbox">
      <input v-model="model.negligible" type="checkbox" />
      ohne Mengenangabe vernachlässigbar (z. B. Salz, Pfeffer)
    </label>

    <div v-if="model.source === 'BLS'" class="bls">
      <p class="current">
        <template v-if="model.referenceCode">Gewählt: {{ model.referenceCode }} – {{ model.referenceName }}</template>
        <template v-else>Noch kein BLS-Eintrag gewählt.</template>
      </p>
      <input v-model="query" type="search" placeholder="BLS durchsuchen, z. B. „Zwiebel roh“" @input="searchDebounced" />
      <ul v-if="results.length" class="results">
        <li v-for="r in results" :key="r.code">
          <button type="button" @click="choose(r)">
            <span>{{ r.name }}</span>
            <span class="meta">{{ r.code }} · {{ fmt(r.kcal) }} kcal · E {{ fmt(r.protein) }} · F {{ fmt(r.fat) }} · KH {{ fmt(r.carbs) }}</span>
          </button>
        </li>
      </ul>
      <p v-else-if="searched" class="meta">Keine Treffer.</p>
    </div>

    <div v-if="model.source === 'MANUAL'" class="grid">
      <label v-for="f in FIELDS" :key="f.key">{{ f.label }}
        <input v-model.number="model[f.key]" type="number" step="0.01" min="0" :required="f.key === 'kcal'" />
      </label>
    </div>

    <label>Notiz
      <input v-model="model.note" />
    </label>

    <div class="actions">
      <button class="btn-primary" :disabled="saving">Speichern</button>
    </div>
  </form>
</template>

<script setup>
import { ref } from 'vue'
import { INGREDIENT_CLASSES, ingredientCatalogService } from '@/services/ingredientCatalogService'

const model = defineModel({ type: Object, required: true })

defineProps({
  saving: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['submit'])

const FIELDS = [
  { key: 'kcal', label: 'kcal / 100 g' },
  { key: 'protein', label: 'Eiweiß g' },
  { key: 'fat', label: 'Fett g' },
  { key: 'carbs', label: 'Kohlenhydrate g' },
  { key: 'fiber', label: 'Ballaststoffe g' },
  { key: 'sugar', label: 'Zucker g' },
  { key: 'salt', label: 'Salz g' },
]

const initialSource = model.value.source
const query = ref('')
const results = ref([])
const searched = ref(false)
let timer = null

function searchDebounced() {
  clearTimeout(timer)
  timer = setTimeout(search, 300)
}

async function search() {
  if (query.value.trim().length < 2) {
    results.value = []
    searched.value = false
    return
  }
  try {
    results.value = await ingredientCatalogService.searchReferenceFoods(query.value.trim())
    searched.value = true
  } catch (error) {
    console.error('BLS search failed:', error)
    results.value = []
  }
}

function choose(food) {
  model.value.referenceCode = food.code
  model.value.referenceName = food.name
  results.value = []
  query.value = ''
  searched.value = false
}

const fmt = (v) => (v == null ? '—' : Number(v).toLocaleString('de-DE', { maximumFractionDigits: 1 }))
</script>

<style scoped>
.ingredient-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #4a5568;
}

.checkbox {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  font-weight: 400;
}

input,
select {
  padding: 8px;
  border: 1px solid #cbd5e0;
  border-radius: 4px;
  font-size: 0.95rem;
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 10px;
}

.bls {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.current {
  margin: 0;
  font-size: 0.85rem;
}

.results {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

.results button {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
  text-align: left;
  background: white;
  border: none;
  border-bottom: 1px solid #edf2f7;
  border-radius: 0;
  padding: 6px 10px;
  cursor: pointer;
  color: #2d3748;
}

.results button:hover {
  background: #f7fafc;
}

.meta {
  font-size: 0.75rem;
  color: #a0aec0;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.btn-primary {
  padding: 10px 20px;
  background: #4a5568;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 600px) {
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
