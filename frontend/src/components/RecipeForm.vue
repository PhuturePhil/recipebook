<template>
  <form class="recipe-form" @submit.prevent="handleSubmit">

    <div class="scan-section">
      <p class="scan-hint">Rezept aus Foto laden</p>
      <div v-if="!scanned" class="scan-upload">
        <input
          type="file"
          accept="image/*"
          multiple
          @change="handleScanUpload"
          id="scan-upload"
        />
        <label for="scan-upload" class="btn-scan">
          {{ selectedImages.length > 0 ? 'Weitere Bilder hinzufügen' : 'Rezeptfotos auswählen' }}
        </label>
        <div v-if="selectedImages.length > 0" class="selected-images">
          <div v-for="(img, index) in selectedImages" :key="index" class="selected-image-item">
            <img :src="img.previewUrl" :alt="img.fileName" />
            <span class="image-name">{{ img.fileName }}</span>
            <button type="button" class="btn-remove-scan-image" @click="removeScanImage(index)">
              ✕
            </button>
          </div>
        </div>
        <button
          v-if="selectedImages.length > 0"
          type="button"
          class="btn-analyze"
          :class="{ loading: scanning }"
          :disabled="scanning"
          @click="handleScan"
        >
          {{ scanning ? 'Wird analysiert...' : `${selectedImages.length} ${selectedImages.length === 1 ? 'Bild' : 'Bilder'} analysieren` }}
        </button>
      </div>
      <div v-if="scanError" class="scan-error">
        {{ scanError }}
      </div>
      <div v-if="unrecognizedText" class="unrecognized-text">
        <label>Vollständiger erkannter Text (zur Kontrolle):</label>
        <textarea readonly :value="unrecognizedText" rows="4"></textarea>
        <button type="button" class="btn-copy" @click="copyUnrecognizedText">
          {{ copied ? 'Kopiert!' : 'Text kopieren' }}
        </button>
      </div>
    </div>

    <div class="form-group">
      <label for="title">Titel</label>
      <input
        id="title"
        v-model="formData.title"
        type="text"
        required
        placeholder="Rezeptname eingeben"
      />
    </div>

    <div class="form-group">
      <label for="description">Beschreibung</label>
      <textarea
        id="description"
        v-model="formData.description"
        rows="1"
        placeholder="Kurze Beschreibung eingeben"
        @input="autoResize"
        ref="descriptionRef"
      ></textarea>
    </div>

    <div class="form-group">
      <label>Bild</label>
      <div class="image-upload">
        <input
          type="file"
          accept="image/*"
          @change="handleImageUpload"
          id="image-upload"
        />
        <div v-if="formData.imageUrl" class="image-preview">
          <img :src="formData.imageUrl" alt="Rezept-Bild" />
          <button type="button" class="btn-remove-image" @click="removeImage">
            Bild entfernen
          </button>
        </div>
      </div>
    </div>

    <div class="form-group">
      <label>Personenanzahl</label>
      <div class="servings-fields">
        <div class="servings-field">
          <label for="servings-from" class="field-sublabel">Von</label>
          <input
            id="servings-from"
            v-model.number="formData.baseServings"
            type="number"
            min="1"
            max="100"
            required
          />
        </div>
        <div class="servings-field">
          <label for="servings-to" class="field-sublabel">Bis (optional)</label>
          <input
            id="servings-to"
            v-model.number="formData.servingsTo"
            type="number"
            min="1"
            max="100"
          />
        </div>
      </div>
    </div>

    <div class="form-group">
      <label for="prep-time">Zubereitungszeit (Minuten)</label>
      <input
        id="prep-time"
        v-model.number="formData.prepTimeMinutes"
        type="number"
        min="1"
        placeholder="z.B. 30"
      />
    </div>

    <div class="form-group">
      <label>Quelle</label>
      <div class="source-fields">
        <input
          v-model="formData.author"
          type="text"
          placeholder="Autor"
        />
        <input
          v-model="formData.source"
          type="text"
          placeholder="Buch oder Website"
        />
        <input
          v-model="formData.page"
          type="text"
          placeholder="Seite (bei Büchern)"
        />
      </div>
    </div>

    <div class="form-group">
      <label>Zutaten</label>
      <div v-for="(ingredient, index) in formData.ingredients" :key="index" class="ingredient-row">
        <input
          v-model="ingredient.name"
          type="text"
          placeholder="Zutat"
          class="ingredient-name"
          required
        />
        <button type="button" class="btn-remove-icon" @click="removeIngredient(index)" title="Zutat entfernen">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
            <path d="M10 11v6M14 11v6"/>
            <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
          </svg>
        </button>
        <div class="ingredient-secondary">
        <input
          v-model="ingredient.amount"
          type="text"
          placeholder="Menge"
          class="ingredient-amount-input"
        />
        <div class="unit-input-wrapper">
          <input
            v-model="ingredient.unit"
            type="text"
            placeholder="Einheit"
            autocomplete="off"
            @focus="activeUnitIndex = index"
            @blur="closeUnitDropdown"
          />
          <ul
            v-if="activeUnitIndex === index && unitDropdownItems(index).length > 0"
            class="unit-dropdown"
          >
            <li
              v-for="unit in filteredKnownUnits(index)"
              :key="unit"
              @mousedown.prevent="selectUnit(index, unit)"
            >
              {{ unit }}
            </li>
            <li
              v-if="showAddOption(index)"
              class="unit-add"
              @mousedown.prevent="selectUnit(index, ingredient.unit)"
            >
              {{ ingredient.unit }} <span class="unit-add-label">(hinzufügen)</span>
            </li>
          </ul>
        </div>
        </div>
      </div>
      <button type="button" class="btn-add" @click="addIngredient">
        + Zutat hinzufügen
      </button>
    </div>

    <div class="form-group">
      <label>Arbeitsanweisungen</label>
      <div v-for="(instruction, index) in formData.instructions" :key="index" class="instruction-row">
        <span class="step-number">{{ index + 1 }}.</span>
        <textarea
          v-model="formData.instructions[index]"
          rows="1"
          placeholder="Arbeitsschritt eingeben"
          required
          @input="autoResize"
        ></textarea>
        <button type="button" class="btn-remove-icon" @click="removeInstruction(index)" title="Schritt entfernen">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
            <path d="M10 11v6M14 11v6"/>
            <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
          </svg>
        </button>
      </div>
      <button type="button" class="btn-add" @click="addInstruction">
        + Schritt hinzufügen
      </button>
    </div>

    <div class="form-actions">
      <button type="button" class="btn-cancel" @click="handleCancel">
        Abbrechen
      </button>
      <button type="submit" class="btn-submit">
        {{ isEdit ? 'Rezept aktualisieren' : 'Rezept erstellen' }}
      </button>
    </div>
  </form>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { recipeService } from '@/services/recipeService'
import { useRecipeStore } from '@/stores/recipeStore'

const props = defineProps({
  recipe: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['submit', 'cancel'])

const router = useRouter()
const store = useRecipeStore()
const isEdit = ref(!!props.recipe)
const scanning = ref(false)
const scanned = ref(false)
const scanError = ref('')
const unrecognizedText = ref('')
const copied = ref(false)
const selectedImages = ref([])

const formData = ref({
  title: '',
  description: '',
  baseServings: 4,
  servingsTo: null,
  prepTimeMinutes: null,
  imageUrl: '',
  author: '',
  source: '',
  page: '',
  ingredients: [{ name: '', amount: '', unit: '' }],
  instructions: ['']
})

const activeUnitIndex = ref(null)
const descriptionRef = ref(null)

function autoResize(event) {
  const el = event.target
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

function resizeAllTextareas() {
  nextTick(() => {
    document.querySelectorAll('.recipe-form textarea:not([readonly])').forEach(el => {
      el.style.height = 'auto'
      el.style.height = el.scrollHeight + 'px'
    })
  })
}

const allKnownUnits = computed(() => {
  const fromStore = store.recipes
    .flatMap(r => r.ingredients ?? [])
    .map(i => i.unit?.trim())
    .filter(u => u && u.length > 0)
  return [...new Set(fromStore)]
})

const filteredKnownUnits = (index) => {
  const query = formData.value.ingredients[index]?.unit?.trim().toLowerCase() ?? ''
  if (!query) return allKnownUnits.value
  return allKnownUnits.value.filter(u => u.toLowerCase().includes(query))
}

const showAddOption = (index) => {
  const val = formData.value.ingredients[index]?.unit?.trim()
  if (!val) return false
  return !allKnownUnits.value.some(u => u.toLowerCase() === val.toLowerCase())
}

const unitDropdownItems = (index) => {
  return filteredKnownUnits(index).length > 0 || showAddOption(index)
    ? [true]
    : []
}

const selectUnit = (index, value) => {
  formData.value.ingredients[index].unit = value
  activeUnitIndex.value = null
}

const closeUnitDropdown = () => {
  setTimeout(() => { activeUnitIndex.value = null }, 150)
}

watch(
  () => props.recipe,
  (newRecipe) => {
    if (newRecipe) {
      isEdit.value = true
      formData.value = {
        title: newRecipe.title || '',
        description: newRecipe.description || '',
        baseServings: newRecipe.baseServings,
        servingsTo: newRecipe.servingsTo || null,
        prepTimeMinutes: newRecipe.prepTimeMinutes || null,
        imageUrl: newRecipe.imageUrl || '',
        author: newRecipe.author || '',
        source: newRecipe.source || '',
        page: newRecipe.page || '',
        ingredients: newRecipe.ingredients?.length
          ? [...newRecipe.ingredients]
          : [{ name: '', amount: '', unit: '' }],
        instructions: newRecipe.instructions?.length
          ? [...newRecipe.instructions]
          : ['']
      }
      resizeAllTextareas()
    }
  },
  { immediate: true }
)

const handleScanUpload = async (event) => {
  const files = Array.from(event.target.files)
  if (!files.length) return

  for (const file of files) {
    const base64 = await readFileAsBase64(file)
    const previewUrl = URL.createObjectURL(file)
    selectedImages.value.push({
      imageData: base64,
      mimeType: file.type || 'image/jpeg',
      fileName: file.name,
      previewUrl
    })
  }

  event.target.value = ''
}

const removeScanImage = (index) => {
  URL.revokeObjectURL(selectedImages.value[index].previewUrl)
  selectedImages.value.splice(index, 1)
}

const handleScan = async () => {
  scanning.value = true
  scanError.value = ''
  unrecognizedText.value = ''

  try {
    const payload = selectedImages.value.map(({ imageData, mimeType }) => ({ imageData, mimeType }))
    const result = await recipeService.scanRecipe(payload)

    formData.value.title = result.title || formData.value.title
    formData.value.description = result.description || formData.value.description
    formData.value.baseServings = result.baseServings || formData.value.baseServings
    formData.value.servingsTo = result.servingsTo || null
    formData.value.prepTimeMinutes = result.prepTimeMinutes || null
    formData.value.author = result.author || formData.value.author
    formData.value.source = result.source || formData.value.source
    formData.value.page = result.page || formData.value.page

    if (result.ingredients?.length) {
      formData.value.ingredients = result.ingredients
    }
    if (result.instructions?.length) {
      formData.value.instructions = result.instructions
    }
    if (result.rawText) {
      unrecognizedText.value = result.rawText
    }

    selectedImages.value.forEach(img => URL.revokeObjectURL(img.previewUrl))
    scanned.value = true
  } catch {
    scanError.value = 'Das Rezeptbild konnte nicht analysiert werden. Bitte versuche es erneut.'
  } finally {
    scanning.value = false
  }
}

const readFileAsBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const dataUrl = e.target.result
      const base64 = dataUrl.split(',')[1]
      resolve(base64)
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const copyUnrecognizedText = async () => {
  try {
    await navigator.clipboard.writeText(unrecognizedText.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    scanError.value = 'Text konnte nicht kopiert werden.'
  }
}

const addIngredient = () => {
  formData.value.ingredients.push({ name: '', amount: '', unit: '' })
}

const handleImageUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      formData.value.imageUrl = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

const removeImage = () => {
  formData.value.imageUrl = ''
}

const removeIngredient = (index) => {
  if (formData.value.ingredients.length > 1) {
    formData.value.ingredients.splice(index, 1)
  }
}

const addInstruction = () => {
  formData.value.instructions.push('')
}

const removeInstruction = (index) => {
  if (formData.value.instructions.length > 1) {
    formData.value.instructions.splice(index, 1)
  }
}

const handleSubmit = () => {
  const recipeData = {
    ...formData.value,
    ingredients: formData.value.ingredients.filter((i) => i.name.trim()),
    instructions: formData.value.instructions.filter((i) => i.trim())
  }
  emit('submit', recipeData)
}

const handleCancel = () => {
  router.push('/')
}
</script>

<style scoped>
.recipe-form {
  max-width: 600px;
  margin: 0 auto;
}

.scan-section {
  background: var(--color-bg-secondary, #f8f8f8);
  border: 2px dashed var(--color-border, #ddd);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 32px;
  text-align: center;
}

.scan-hint {
  font-weight: 600;
  font-size: 1rem;
  color: var(--color-text-primary, #333);
  margin: 0 0 12px 0;
}

.scan-upload input[type='file'] {
  display: none;
}

.btn-scan {
  display: inline-block;
  padding: 10px 20px;
  background: var(--color-primary, #4a5568);
  color: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
  transition: background-color 0.2s ease;
}

.btn-scan:hover {
  background: var(--color-primary-dark, #2d3748);
}

.selected-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  justify-content: center;
}

.selected-image-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100px;
}

.selected-image-item img {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--color-border, #ddd);
}

.image-name {
  font-size: 0.75rem;
  color: var(--color-text-muted, #999);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-remove-scan-image {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: var(--color-error, #e53e3e);
  color: white;
  font-size: 0.7rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.btn-remove-scan-image:hover {
  background: #c53030;
}

.btn-analyze {
  display: inline-block;
  margin-top: 14px;
  padding: 10px 24px;
  background: var(--color-success, #38a169);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
  transition: background-color 0.2s ease;
}

.btn-analyze:hover:not(:disabled) {
  background: #2f855a;
}

.btn-analyze.loading,
.btn-analyze:disabled {
  background: var(--color-text-muted, #999);
  cursor: not-allowed;
}

.scan-error {
  margin-top: 12px;
  color: var(--color-error, #e53e3e);
  font-size: 0.875rem;
}

.unrecognized-text {
  margin-top: 16px;
  text-align: left;
}

.unrecognized-text label {
  display: block;
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 6px;
  color: var(--color-text-primary, #333);
}

.unrecognized-text textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 0.875rem;
  font-family: inherit;
  box-sizing: border-box;
  background: white;
  resize: vertical;
}

.btn-copy {
  margin-top: 8px;
  padding: 6px 14px;
  border: 1px solid var(--color-primary, #4a5568);
  border-radius: 4px;
  background: transparent;
  color: var(--color-primary, #4a5568);
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s ease;
}

.btn-copy:hover {
  background: var(--color-primary, #4a5568);
  color: white;
}

.form-group {
  margin-bottom: 24px;
}

.form-group > label {
  display: block;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--color-text-primary, #333);
}

.form-group input[type='text'],
.form-group input[type='number'],
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 1rem;
  font-family: inherit;
  box-sizing: border-box;
}

.form-group textarea {
  resize: none;
  overflow: hidden;
  min-height: 44px;
}

.servings-fields {
  display: flex;
  gap: 16px;
}

.servings-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-sublabel {
  font-size: 0.875rem;
  font-weight: 400;
  color: var(--color-text-secondary, #666);
}

.servings-field input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 1rem;
  box-sizing: border-box;
}

.image-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-preview {
  position: relative;
  display: inline-block;
}

.image-preview img {
  max-width: 300px;
  max-height: 200px;
  border-radius: 8px;
  object-fit: cover;
}

.btn-remove-image {
  margin-top: 8px;
  padding: 6px 12px;
  border: 1px solid var(--color-error, #e53e3e);
  border-radius: 4px;
  background: transparent;
  color: var(--color-error, #e53e3e);
  cursor: pointer;
  font-size: 0.875rem;
}

.btn-remove-image:hover {
  background: rgba(229, 62, 62, 0.1);
}

.form-group input:focus,
.form-group textarea:focus,
.servings-field input:focus {
  outline: none;
  border-color: var(--color-primary, #4a5568);
  box-shadow: 0 0 0 3px rgba(74, 85, 104, 0.1);
}

.source-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.source-fields input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 1rem;
  box-sizing: border-box;
}

.ingredient-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.ingredient-row .ingredient-name {
  flex: 2;
  min-width: 0;
}

.ingredient-secondary {
  display: flex;
  gap: 8px;
  flex: 2;
  min-width: 0;
}

.ingredient-secondary .ingredient-amount-input {
  flex: 1;
  min-width: 0;
}

.ingredient-secondary .unit-input-wrapper {
  flex: 1;
  min-width: 0;
}

.btn-remove-icon {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-error, #e53e3e);
  padding: 6px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background-color 0.15s ease;
}

.btn-remove-icon:hover {
  background: rgba(229, 62, 62, 0.1);
}

@media (max-width: 600px) {
  .ingredient-row {
    flex-wrap: wrap;
  }

  .ingredient-row .ingredient-name {
    flex: 1 1 calc(100% - 44px);
  }

  .ingredient-secondary {
    flex: 1 1 100%;
  }
}

.instruction-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: flex-start;
}

.step-number {
  font-weight: 600;
  color: var(--color-text-muted, #999);
  min-width: 24px;
  padding-top: 10px;
}

.instruction-row textarea {
  flex: 1;
}

.btn-add,
.btn-remove {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background-color 0.2s ease;
}

.btn-add {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.btn-add:hover {
  background: var(--color-border, #ddd);
}

.btn-remove {
  background: transparent;
  color: var(--color-error, #e53e3e);
}

.btn-remove:hover {
  background: rgba(229, 62, 62, 0.1);
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 32px;
  position: sticky;
  bottom: 0;
  background: var(--color-bg, #f9fafb);
  padding: 12px 0;
  border-top: 1px solid var(--color-border, #e2e8f0);
}

.btn-cancel,
.btn-submit {
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-cancel {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.btn-cancel:hover {
  background: var(--color-border, #ddd);
}

.btn-submit {
  background: var(--color-primary, #4a5568);
  color: white;
}

.btn-submit:hover {
  background: var(--color-primary-dark, #2d3748);
}

.unit-input-wrapper {
  position: relative;
  flex: 1;
}

.unit-input-wrapper input {
  width: 100%;
  box-sizing: border-box;
}

.unit-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  background: white;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  margin-top: 2px;
  padding: 4px 0;
  list-style: none;
  max-height: 200px;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.unit-dropdown li {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 0.95rem;
  color: var(--color-text-primary, #333);
}

.unit-dropdown li:hover {
  background: var(--color-bg-secondary, #f0f0f0);
}

.unit-add {
  border-top: 1px solid var(--color-border, #ddd);
  color: var(--color-primary, #4a5568);
}

.unit-add-label {
  font-size: 0.8rem;
  color: var(--color-text-muted, #999);
  margin-left: 4px;
}
</style>
