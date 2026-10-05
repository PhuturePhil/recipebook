<template>
  <form
    ref="formRef"
    class="recipe-form"
    novalidate
    @submit.prevent="submit"
    @keydown.enter="blockImplicitSubmit"
  >
    <StartChoice v-if="entry === 'choice'" @choose="chooseEntry" />

    <ImportSection
      v-else-if="entry === 'import'"
      :images="selectedImages"
      :scanning="scanning"
      :error="scanError"
      @files="addScanImages"
      @remove="removeScanImage"
      @analyze="handleScan"
      @manual="entry = 'form'"
    />

    <template v-else>
    <div v-if="draftOffer" class="draft-offer" role="status">
      <span class="draft-offer-text">Entwurf {{ formatDraftTime(draftOffer.savedAt) }} wiederherstellen?</span>
      <div class="draft-offer-actions">
        <button type="button" class="btn-draft-restore" @click="restoreDraftOffer">Wiederherstellen</button>
        <button type="button" class="btn-draft-discard" @click="discardDraftOffer">Verwerfen</button>
      </div>
    </div>

    <ImportBanner v-if="importedFromScan" :raw-text="unrecognizedText" @close="importedFromScan = false" />

    <p class="required-legend">* Pflichtfeld</p>

    <div class="form-group">
      <label for="title">Titel<span class="req" aria-hidden="true">*</span></label>
      <input
        id="title"
        v-model="formData.title"
        type="text"
        required
        aria-required="true"
        :aria-invalid="errors.title ? 'true' : undefined"
        :aria-describedby="errors.title ? 'title-error' : undefined"
        placeholder="Rezeptname eingeben"
      />
      <p v-if="errors.title" id="title-error" class="field-error" role="alert">{{ errors.title }}</p>
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
      <div class="image-upload" :class="{ 'has-image': formData.imageUrl }">
        <div class="image-picker">
          <input
            type="file"
            accept="image/*"
            class="visually-hidden"
            @change="handleImageUpload"
            id="image-upload"
          />
          <label for="image-upload" class="btn-image-pick">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
              <circle cx="12" cy="13" r="4"/>
            </svg>
            {{ imageLoading ? 'Foto wird geladen…' : (formData.imageUrl ? 'Anderes Foto wählen' : 'Foto aufnehmen oder auswählen') }}
          </label>
          <p v-if="!formData.imageUrl && !isEdit" class="image-hint">
            Ohne eigenes Foto sucht die App automatisch ein passendes Bild (Unsplash).
          </p>
        </div>
        <div v-if="formData.imageUrl" class="image-preview">
          <img :src="formData.imageUrl" alt="Vorschau des Rezeptbilds" />
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
          <label for="servings-from" class="field-sublabel">Von<span class="req" aria-hidden="true">*</span></label>
          <input
            id="servings-from"
            v-model.number="formData.baseServings"
            type="number"
            inputmode="numeric"
            min="1"
            max="100"
            required
            aria-required="true"
            :aria-invalid="errors.baseServings ? 'true' : undefined"
            :aria-describedby="errors.baseServings ? 'servings-from-error' : undefined"
          />
        </div>
        <div class="servings-field">
          <label for="servings-to" class="field-sublabel">Bis</label>
          <input
            id="servings-to"
            v-model.number="formData.servingsTo"
            type="number"
            inputmode="numeric"
            min="1"
            max="100"
            :aria-invalid="errors.servingsTo ? 'true' : undefined"
            :aria-describedby="errors.servingsTo ? 'servings-to-error' : undefined"
          />
        </div>
      </div>
      <p v-if="errors.baseServings" id="servings-from-error" class="field-error" role="alert">{{ errors.baseServings }}</p>
      <p v-if="errors.servingsTo" id="servings-to-error" class="field-error" role="alert">{{ errors.servingsTo }}</p>
    </div>

    <div class="form-group">
      <label for="prep-time">Zubereitungszeit (Minuten)</label>
      <input
        id="prep-time"
        v-model.number="formData.prepTimeMinutes"
        type="number"
        inputmode="numeric"
        min="1"
        max="10080"
        placeholder="z.B. 30"
        :aria-invalid="errors.prepTimeMinutes ? 'true' : undefined"
        :aria-describedby="errors.prepTimeMinutes ? 'prep-time-error' : undefined"
      />
      <p v-if="errors.prepTimeMinutes" id="prep-time-error" class="field-error" role="alert">{{ errors.prepTimeMinutes }}</p>
    </div>

    <div class="form-group">
      <label for="tags">Tags</label>
      <TagInput v-model="formData.tags" :known="knownTags" />
    </div>

    <div class="form-group">
      <label for="language">Sprache des Rezepts</label>
      <select id="language" v-model="languageSelection" class="language-select">
        <option value="auto">{{ autoLanguageLabel(formData.language) }}</option>
        <option value="de">Deutsch</option>
        <option value="en">Englisch</option>
      </select>
    </div>

    <div class="form-group">
      <label>Quelle</label>
      <div class="source-fields">
        <div class="source-input-wrapper">
          <input
            v-model="formData.author"
            type="text"
            placeholder="Autor"
            autocomplete="off"
            @focus="activeSourceField = 'author'"
            @blur="onSourceFieldBlur('author')"
            @keydown="onSourceFieldKeydown"
          />
          <button
            v-if="formData.author"
            type="button"
            class="source-clear-btn"
            aria-label="Autor leeren"
            @mousedown.prevent="formData.author = ''"
          ><AppIcon name="schliessen" :size="16" /></button>
          <ul
            v-if="activeSourceField === 'author' && filteredAuthors.length > 0"
            class="source-dropdown"
            tabindex="-1"
          >
            <li
              v-for="item in filteredAuthors"
              :key="item.author"
              @mousedown.prevent="selectAuthor(item)"
            >{{ item.author }}</li>
          </ul>
        </div>
        <div class="source-input-wrapper">
          <input
            v-model="formData.source"
            type="text"
            placeholder="Buch, Website oder Eigenrezept"
            autocomplete="off"
            @focus="activeSourceField = 'source'"
            @blur="onSourceFieldBlur('source')"
            @keydown="onSourceFieldKeydown"
          />
          <button
            v-if="formData.source"
            type="button"
            class="source-clear-btn"
            aria-label="Quelle leeren"
            @mousedown.prevent="formData.source = ''"
          ><AppIcon name="schliessen" :size="16" /></button>
          <ul
            v-if="activeSourceField === 'source' && filteredSources.length > 0"
            class="source-dropdown"
            tabindex="-1"
          >
            <li
              v-for="item in filteredSources"
              :key="item.source"
              @mousedown.prevent="selectSource(item)"
            >{{ item.source }}</li>
          </ul>
        </div>
        <div class="source-url-field">
          <input
            ref="sourceUrlRef"
            v-model="formData.sourceUrl"
            type="url"
            inputmode="url"
            placeholder="Link (Website)"
            aria-label="Link (Website)"
            autocomplete="off"
            autocapitalize="off"
            spellcheck="false"
            :aria-invalid="showSourceUrlError ? 'true' : 'false'"
            :aria-describedby="showSourceUrlError ? 'source-url-error' : null"
            @blur="onSourceUrlBlur"
          />
          <p v-if="showSourceUrlError" id="source-url-error" class="field-error" role="alert">{{ sourceUrlError }}</p>
        </div>
        <input
          v-model="formData.page"
          type="text"
          inputmode="numeric"
          placeholder="Seite (bei Büchern)"
        />
      </div>
    </div>

    <div class="form-group">
      <label id="ingredients-label">Zutaten<span class="req" aria-hidden="true">*</span></label>
      <div v-if="ingredientTextMode" class="ingredient-text-editor">
        <textarea
          ref="ingredientTextRef"
          v-model="ingredientText"
          rows="8"
          required
          placeholder="Eine Zutat pro Zeile, z. B.&#10;200 g Zwiebeln&#10;1/2 TL Salz&#10;Salz und Pfeffer"
          aria-label="Zutaten als Text, eine pro Zeile"
        ></textarea>
        <p class="ingredient-text-hint">Eine Zutat pro Zeile. Menge und Einheit am Zeilenanfang werden beim Übernehmen automatisch erkannt. Eine Zeile wie „Salsa:“ beginnt eine Gruppe.</p>
        <div class="ingredient-text-actions">
          <button type="button" class="btn-add" @click="closeIngredientText(false)">Abbrechen</button>
          <button type="button" class="btn-apply-text" @click="closeIngredientText(true)">Übernehmen</button>
        </div>
      </div>
      <template v-else>
        <!-- Zutaten ohne Gruppe oben ohne Karte, darunter die Gruppen-Karten; Ziehen am Griff auch zwischen den Listen -->
        <draggable
          :list="formData.sections[0].items"
          :item-key="keyOf"
          group="zutaten"
          v-bind="DRAG_OPTIONS"
          class="ingredient-list"
        >
          <template #item="{ element, index }">
            <IngredientRow
              v-bind="rowProps(element, 0, index)"
              @enter="onIngredientEnter($event, element)"
              @move="(delta, event) => moveIngredient(element, delta, event)"
              @remove="removeIngredient(element)"
              @paste="onIngredientPaste($event, element)"
            />
          </template>
        </draggable>
        <draggable
          v-model="groupSections"
          item-key="key"
          group="gruppen"
          v-bind="DRAG_OPTIONS"
          handle=".group-drag-handle"
          class="ingredient-groups"
        >
          <template #item="{ element: section, index: g }">
            <IngredientGroupCard
              v-model:name="section.name"
              :section-id="section.key"
              @move="(delta, event) => moveGroup(section, delta, event)"
              @remove="removeGroup(section)"
              @add="addToGroup(section)"
              @enter="onGroupEnter($event, section)"
            >
              <draggable
                :list="section.items"
                :item-key="keyOf"
                group="zutaten"
                v-bind="DRAG_OPTIONS"
                class="group-items"
              >
                <template #item="{ element, index }">
                  <IngredientRow
                    v-bind="rowProps(element, g + 1, index)"
                    @enter="onIngredientEnter($event, element)"
                    @move="(delta, event) => moveIngredient(element, delta, event)"
                    @remove="removeIngredient(element)"
                    @paste="onIngredientPaste($event, element)"
                  />
                </template>
              </draggable>
            </IngredientGroupCard>
          </template>
        </draggable>
        <p v-if="errors.ingredients" id="ingredients-error" class="field-error" role="alert">{{ errors.ingredients }}</p>
        <div class="ingredient-buttons">
          <button type="button" class="btn-add" @click="addIngredient">
            + Zutat hinzufügen
          </button>
          <button type="button" class="btn-add" @click="addGroup">
            + Gruppe
          </button>
          <button type="button" class="btn-add" @click="openIngredientText">
            Als Text bearbeiten
          </button>
        </div>
      </template>
    </div>

    <div class="form-group">
      <label id="instructions-label">Arbeitsanweisungen<span class="req" aria-hidden="true">*</span></label>
      <draggable
        :model-value="instructionItems"
        item-key="index"
        group="schritte"
        v-bind="DRAG_OPTIONS"
        class="instruction-list"
        @update:model-value="reorderInstructions"
      >
        <template #item="{ element: { index } }">
          <div class="instruction-row" :data-step="index">
            <span class="step-number">{{ index + 1 }}.</span>
            <textarea
              v-model="formData.instructions[index]"
              rows="1"
              placeholder="Arbeitsschritt eingeben"
              :aria-label="`Arbeitsschritt ${index + 1}`"
              :required="isInstructionRequired(formData.instructions, index)"
              :aria-required="isInstructionRequired(formData.instructions, index) ? 'true' : undefined"
              :aria-invalid="errors.instructions && index === 0 ? 'true' : undefined"
              :aria-describedby="errors.instructions && index === 0 ? 'instructions-error' : undefined"
              @input="autoResize"
              @keydown.alt.up.prevent="moveInstruction(index, -1, $event)"
              @keydown.alt.down.prevent="moveInstruction(index, 1, $event)"
            ></textarea>
            <div class="row-actions">
              <button
                type="button"
                class="drag-handle"
                aria-label="Schritt verschieben: Pfeiltasten"
                title="Ziehen zum Verschieben"
                @keydown.up.prevent="moveInstruction(index, -1, $event)"
                @keydown.down.prevent="moveInstruction(index, 1, $event)"
              >
                <GripIcon />
              </button>
              <button type="button" class="btn-remove-icon" title="Schritt entfernen" aria-label="Schritt entfernen" @click="removeInstruction(index)">
                <AppIcon name="loeschen" :size="18" />
              </button>
            </div>
          </div>
        </template>
      </draggable>
      <p v-if="errors.instructions" id="instructions-error" class="field-error" role="alert">{{ errors.instructions }}</p>
      <button type="button" class="btn-add" @click="addInstruction">
        + Schritt hinzufügen
      </button>
    </div>
    </template>

    <div v-if="saveError" class="save-error" role="alert">
      Speichern fehlgeschlagen: {{ saveError }} Deine Eingaben sind noch da, du kannst es erneut versuchen.
    </div>

    <ConfirmDialog
      v-if="confirmState"
      v-bind="confirmState.props"
      @confirm="confirmState.resolve(true)"
      @cancel="confirmState.resolve(false)"
    />
  </form>
</template>

<script setup>
import AppIcon from '@/components/shell/AppIcon.vue'
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount, toRaw } from 'vue'
import { recipeService } from '@/services/recipeService'
import { ingredientCatalogService } from '@/services/ingredientCatalogService'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import { useAuthStore } from '@/stores/authStore'
import TagInput from '@/components/TagInput.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import StartChoice from '@/components/form/StartChoice.vue'
import ImportSection from '@/components/form/ImportSection.vue'
import ImportBanner from '@/components/form/ImportBanner.vue'
import IngredientRow from '@/components/form/IngredientRow.vue'
import IngredientGroupCard from '@/components/form/IngredientGroupCard.vue'
import GripIcon from '@/components/form/GripIcon.vue'
import draggable from 'vuedraggable'
import { resizeImageFile } from '@/utils/resizeImage'
import {
  emptyIngredient,
  isBlankIngredient,
  cleanRecipeData,
  isIngredientNameRequired,
  isInstructionRequired,
  addIngredientBelow,
  addSection,
  isSectionEmpty,
  dissolveSection,
  ensureIngredientRow,
  removeIngredientAt,
  moveIngredientAcross,
  moveSection,
  moveRow,
  preventsImplicitSubmit,
  formSnapshot,
  validateRecipeForm,
  firstError,
  ingredientsWithoutName,
  languageChoice,
  applyLanguageChoice,
  autoLanguageLabel
} from '@/utils/recipeFormData'
import { ingredientsFromText, ingredientsToText, insertPastedIngredients } from '@/utils/ingredientText'
import {
  ingredientSection,
  ingredientsToRows,
  ingredientsToSections,
  rowsToIngredients,
  sectionsToIngredients
} from '@/utils/ingredientGroups'
import {
  RECOGNIZE_DELAY_MS,
  recognitionKey,
  pendingRecognition,
  recognitionHint
} from '@/utils/ingredientSuggest'
import { sourceSuggestions, authorSuggestions, sourcesOfAuthor } from '@/utils/recipeSources'
import { completeSourceUrl, sourceUrlProblem } from '@/utils/sourceUrl'
import {
  DRAFT_DELAY_MS,
  draftKey,
  saveDraft,
  loadDraft,
  clearDraft,
  draftDiffers,
  restoreDraft,
  autosaveAction,
  formatDraftTime
} from '@/utils/recipeDraft'

const props = defineProps({
  recipe: {
    type: Object,
    default: null
  },
  saving: {
    type: Boolean,
    default: false
  },
  saveError: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['submit'])

const store = useRecipeStore()
const uiStore = useUiStore()
const authStore = useAuthStore()
const isEdit = ref(!!props.recipe)
const scanning = ref(false)
const scanError = ref('')
const unrecognizedText = ref('')
const selectedImages = ref([])
const importedFromScan = ref(false)

const formData = ref({
  title: '',
  description: '',
  baseServings: 4,
  servingsTo: null,
  prepTimeMinutes: null,
  language: null,
  languageAuto: true,
  imageUrl: '',
  author: '',
  source: '',
  page: '',
  sourceUrl: '',
  tags: [],
  sections: [ingredientSection(null, [emptyIngredient()])],
  instructions: ['']
})

// All ingredients in form order
const allIngredients = () => formData.value.sections.flatMap((section) => section.items)

// Each ingredient keeps its own key while it is dragged or moved (rows have their own state, e.g. open lists)
const rowKeys = new WeakMap()
let rowCount = 0
const keyOf = (item) => {
  const raw = toRaw(item)
  if (!rowKeys.has(raw)) rowKeys.set(raw, `z${++rowCount}`)
  return rowKeys.get(raw)
}

// Where an ingredient is right now: [sectionIndex, itemIndex] or null
const locate = (item) => {
  const raw = toRaw(item)
  const sections = formData.value.sections
  for (let s = 0; s < sections.length; s++) {
    const i = sections[s].items.findIndex((other) => toRaw(other) === raw)
    if (i >= 0) return [s, i]
  }
  return null
}

const sectionIndexOf = (section) => formData.value.sections.findIndex((other) => toRaw(other) === toRaw(section))

// Handle only (scrolling stays free); on touch the finger has to rest briefly before the row lifts
const DRAG_OPTIONS = {
  handle: '.drag-handle',
  animation: 150,
  ghostClass: 'drag-ghost',
  delay: 150,
  delayOnTouchOnly: true,
  touchStartThreshold: 4
}

// The group cards as one list for dragging; "ohne Gruppe" (index 0) always stays on top
const groupSections = computed({
  get: () => formData.value.sections.slice(1),
  set: (list) => formData.value.sections.splice(1, Infinity, ...list)
})

const formRef = ref(null)

const languageSelection = computed({
  get: () => languageChoice(formData.value),
  set: (choice) => applyLanguageChoice(formData.value, choice)
})
const savedSnapshot = ref(formSnapshot(formData.value))

const isDirty = () => formSnapshot(formData.value) !== savedSnapshot.value

// Errors appear on saving only; while typing, a fixed field loses its message (a still wrong one keeps it)
const errors = ref({})

watch(formData, () => {
  const shown = Object.keys(errors.value)
  if (!shown.length) return
  const now = validateRecipeForm(formData.value)
  errors.value = Object.fromEntries(shown.filter((field) => now[field]).map((field) => [field, now[field]]))
}, { deep: true })

// Rows marked red after a failed save: started rows without a name, or the first row when there is no ingredient
const invalidNameRows = computed(() => {
  if (!errors.value.ingredients) return new Set()
  const sections = formData.value.sections
  const missing = ingredientsWithoutName(sections)
  const items = missing.length ? missing.map(([s, i]) => sections[s].items[i]) : allIngredients().slice(0, 1)
  return new Set(items.map(keyOf))
})

const rowProps = (item, s, i) => ({
  ingredient: item,
  rowId: keyOf(item),
  knownUnits: knownUnits.value,
  hint: hintFor(item),
  required: isIngredientNameRequired(formData.value.sections, s, i),
  invalid: invalidNameRows.value.has(keyOf(item)),
  describedby: 'ingredients-error'
})

const descriptionRef = ref(null)
const knownUnits = ref([])
const knownSources = ref([])
const knownTags = ref([])
const activeSourceField = ref(null)
const sourceEscPressed = ref(false)

function autoResize(event) {
  const el = event.target
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

async function resizeAllTextareas() {
  await nextTick()
  await nextTick()
  document.querySelectorAll('.recipe-form textarea:not([readonly])').forEach(el => {
    el.style.height = 'auto'
    el.style.height = el.scrollHeight + 'px'
  })
}

const filteredSources = computed(() =>
  sourceSuggestions(knownSources.value, formData.value.source, formData.value.author))

const filteredAuthors = computed(() =>
  authorSuggestions(knownSources.value, formData.value.author).map(author => ({ author })))

const selectSource = (item) => {
  formData.value.source = item.source
  if (!formData.value.author && item.author) {
    formData.value.author = item.author
  }
  activeSourceField.value = null
}

const selectAuthor = (item) => {
  formData.value.author = item.author
  const booksOfAuthor = sourcesOfAuthor(knownSources.value, item.author)
  if (booksOfAuthor.length === 1 && !formData.value.source?.trim()) {
    formData.value.source = booksOfAuthor[0].source
  }
  activeSourceField.value = null
}

const onSourceFieldBlur = (field) => {
  setTimeout(() => {
    sourceEscPressed.value = false
    if (activeSourceField.value === field) {
      activeSourceField.value = null
    }
  }, 150)
}

const onSourceFieldKeydown = (event) => {
  if (event.key === 'Escape') {
    sourceEscPressed.value = true
    activeSourceField.value = null
  }
}

// Link: https:// wird beim Verlassen ergänzt; die Meldung erscheint erst danach bzw. beim Speichern
const sourceUrlRef = ref(null)
const sourceUrlTouched = ref(false)
const sourceUrlError = computed(() => errors.value.sourceUrl || sourceUrlProblem(formData.value.sourceUrl))
const showSourceUrlError = computed(() => !!errors.value.sourceUrl || (sourceUrlTouched.value && !!sourceUrlError.value))

const onSourceUrlBlur = () => {
  formData.value.sourceUrl = completeSourceUrl(formData.value.sourceUrl)
  sourceUrlTouched.value = true
}

// ✓ / ? per row: the server tells whether the row gets nutrition values (same matching as the calculation)
const recognition = ref({})
let recognizeTimer = null

const hintFor = (ingredient) => {
  const key = recognitionKey(ingredient)
  return key ? recognitionHint(recognition.value[key]) : null
}

const recognizeIngredients = async () => {
  const pending = pendingRecognition(allIngredients(), recognition.value)
  if (!pending.length) return
  try {
    const results = await ingredientCatalogService.recognize(pending.map((p) => p.line))
    const next = { ...recognition.value }
    pending.forEach((p, i) => {
      if (results?.[i]) next[p.key] = results[i]
    })
    recognition.value = next
  } catch {
    // ohne Antwort bleibt die Zeile einfach ohne Hinweis
  }
}

watch(
  () => allIngredients().map(recognitionKey).join('\n'),
  () => {
    clearTimeout(recognizeTimer)
    recognizeTimer = setTimeout(recognizeIngredients, RECOGNIZE_DELAY_MS)
  },
  { immediate: true }
)

onMounted(async () => {
  try {
    const [units, sources] = await Promise.all([
      recipeService.getUnits(),
      recipeService.getSources(),
    ])
    knownUnits.value = units
    knownSources.value = sources
  } catch {
    // Vorschläge nicht verfügbar — kein kritischer Fehler
  }
  try {
    knownTags.value = await recipeService.getTags()
  } catch {
    // Tag-Vorschläge nicht verfügbar — Tags lassen sich trotzdem eintippen
  }
})

const loadedSections = (ingredients) => {
  const sections = ingredientsToSections(ingredients)
  ensureIngredientRow(sections)
  return sections
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
        language: newRecipe.language || null,
        languageAuto: newRecipe.languageAuto ?? true,
        imageUrl: newRecipe.imageUrl || '',
        author: newRecipe.author || '',
        source: newRecipe.source || '',
        page: newRecipe.page || '',
        sourceUrl: newRecipe.sourceUrl || '',
        tags: [...(newRecipe.tags ?? [])],
        sections: loadedSections(newRecipe.ingredients),
        instructions: newRecipe.instructions?.length
          ? [...newRecipe.instructions]
          : ['']
      }
      savedSnapshot.value = formSnapshot(formData.value)
      resizeAllTextareas()
    }
  },
  { immediate: true }
)

// Entwurf: every change is kept in localStorage per recipe (or "neu") and offered again on the next visit
const draftStorage = (() => {
  try {
    return window.localStorage
  } catch {
    return null
  }
})()
const draftStorageKey = draftKey(authStore.user?.id, props.recipe?.id)
const draftOffer = ref(null)
let draftTimer = null

const storedDraft = draftStorage && loadDraft(draftStorage, draftStorageKey)
if (draftDiffers(storedDraft, formData.value)) {
  draftOffer.value = storedDraft
} else if (storedDraft) {
  clearDraft(draftStorage, draftStorageKey)
}

function saveDraftNow() {
  clearTimeout(draftTimer)
  draftTimer = null
  if (!draftStorage) return
  const action = autosaveAction({ dirty: isDirty(), draftPending: !!draftOffer.value })
  if (action === 'save') saveDraft(draftStorage, draftStorageKey, formData.value)
  else if (action === 'clear') clearDraft(draftStorage, draftStorageKey)
}

function discardDraft() {
  clearTimeout(draftTimer)
  draftTimer = null
  draftOffer.value = null
  if (draftStorage) clearDraft(draftStorage, draftStorageKey)
}

watch(formData, () => {
  clearTimeout(draftTimer)
  draftTimer = setTimeout(saveDraftNow, DRAFT_DELAY_MS)
}, { deep: true })

// Neu: first the choice between photo and typing; an existing draft (or editing) goes straight to the form
const entry = ref(isEdit.value || draftOffer.value ? 'form' : 'choice')

const chooseEntry = async (choice) => {
  entry.value = choice
  if (choice !== 'form') return
  await nextTick()
  document.getElementById('title')?.focus()
}

const restoreDraftOffer = () => {
  formData.value = restoreDraft(draftOffer.value, formData.value)
  draftOffer.value = null
  saveDraftNow()
  resizeAllTextareas()
}

// Without own input an empty form makes no sense: back to the choice
const discardDraftOffer = () => {
  discardDraft()
  if (!isEdit.value && !isDirty()) entry.value = 'choice'
}

const onPageHide = () => saveDraftNow()
const onVisibilityChange = () => {
  if (document.visibilityState === 'hidden') saveDraftNow()
}

onMounted(() => {
  window.addEventListener('pagehide', onPageHide)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  clearTimeout(draftTimer)
  clearTimeout(recognizeTimer)
  window.removeEventListener('pagehide', onPageHide)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})


const addScanImages = async (files) => {
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
}

const removeScanImage = (index) => {
  URL.revokeObjectURL(selectedImages.value[index].previewUrl)
  selectedImages.value.splice(index, 1)
}

const hasIngredientsOrSteps = () =>
  allIngredients().some((i) => !isBlankIngredient(i)) ||
  formData.value.instructions.some((i) => i.trim())

const handleScan = async () => {
  if (hasIngredientsOrSteps() && !await askConfirm({
    title: 'Zutaten und Schritte ersetzen?',
    text: 'Das Foto-Ergebnis ersetzt die vorhandenen Zutaten und Arbeitsschritte.',
    confirmLabel: 'Ersetzen',
    cancelLabel: 'Behalten',
    destructive: true
  })) return
  scanning.value = true
  scanError.value = ''
  unrecognizedText.value = ''
  uiStore.showLoading('Rezept wird analysiert…')

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
      formData.value.sections = loadedSections(
        result.ingredients.map(({ group, ...ingredient }) => ({ ...emptyIngredient(), ...ingredient, groupName: group }))
      )
    }
    if (result.instructions?.length) {
      formData.value.instructions = result.instructions
    }
    if (result.rawText) {
      unrecognizedText.value = result.rawText
    }

    selectedImages.value.forEach(img => URL.revokeObjectURL(img.previewUrl))
    selectedImages.value = []
    importedFromScan.value = true
    entry.value = 'form'
    await resizeAllTextareas()
  } catch {
    scanError.value = 'Das Rezeptbild konnte nicht analysiert werden. Bitte versuche es erneut.'
  } finally {
    scanning.value = false
    uiStore.hideLoading()
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

// Focus a field of an ingredient row once it is rendered
const focusRow = async (item, selector = 'input') => {
  await nextTick()
  formRef.value?.querySelector(`[data-row="${keyOf(item)}"] ${selector}`)?.focus()
}

const addIngredient = () => {
  const item = emptyIngredient()
  formData.value.sections[0].items.push(item)
  focusRow(item)
}

const addToGroup = (section) => {
  const item = emptyIngredient()
  section.items.push(item)
  focusRow(item)
}

const addGroup = async () => {
  const s = addSection(formData.value.sections)
  await nextTick()
  formRef.value?.querySelector(`[data-section="${formData.value.sections[s].key}"] .ingredient-group-input`)?.focus()
}

const ingredientTextMode = ref(false)
const ingredientText = ref('')
const ingredientTextRef = ref(null)

const openIngredientText = async () => {
  ingredientText.value = ingredientsToText(ingredientsToRows(sectionsToIngredients(formData.value.sections)))
  ingredientTextMode.value = true
  await nextTick()
  const el = ingredientTextRef.value
  if (!el) return
  el.focus()
  el.setSelectionRange(el.value.length, el.value.length)
}

const closeIngredientText = (apply) => {
  if (apply) {
    const previous = ingredientsToRows(sectionsToIngredients(formData.value.sections))
    const rows = ingredientsFromText(ingredientText.value, previous, knownUnits.value)
    formData.value.sections = loadedSections(rowsToIngredients(rows))
  }
  ingredientTextMode.value = false
}

// Multi-line text pasted into a row becomes one ingredient per line, split into Menge | Einheit | Zutat
// Paste works on the flat row list (headings included): the rows go in at the pasted row, then everything turns
// back into sections (ingredients without a group gather on top again, see ingredientsToSections).
// A marker travels with the copies so the focus can follow the last pasted row.
const PASTE_MARK = Symbol('paste')

const onIngredientPaste = (event, ingredient) => {
  const text = event.clipboardData?.getData('text/plain') ?? ''
  const current = toRaw(ingredient)
  const marked = formData.value.sections.map((section) => ({
    ...section,
    items: section.items.map((item) => (toRaw(item) === current ? { ...item, [PASTE_MARK]: true } : item))
  }))
  const flat = ingredientsToRows(sectionsToIngredients(marked))
  const at = flat.findIndex((row) => row[PASTE_MARK])
  delete flat[at][PASTE_MARK]
  const last = insertPastedIngredients(flat, at, text, knownUnits.value)
  if (last < 0) return
  event.preventDefault()
  flat[last][PASTE_MARK] = true
  const sections = loadedSections(rowsToIngredients(flat))
  let target = null
  sections.forEach((section) => section.items.forEach((item) => {
    if (item[PASTE_MARK]) target = item
    delete item[PASTE_MARK]
  }))
  formData.value.sections = sections
  if (target) focusRow(target, '.ingredient-name')
}

// The moved row (ingredient, group card or step) keeps the focus on the same field or handle.
// move() does the move and returns the selector of the moved row, or null when nothing moved.
const moveKeepingFocus = async (event, move) => {
  const field = event?.target
  const focusable = (row) => (row ? [...row.querySelectorAll('input, textarea, button:not([tabindex="-1"])')] : [])
  const position = focusable(field?.closest?.('[data-row], [data-section], [data-step]')).indexOf(field)
  const selector = move()
  if (!selector) return false
  await nextTick()
  const row = formRef.value?.querySelector(selector)
  ;(focusable(row)[position] ?? row?.querySelector('.drag-handle'))?.focus()
  return true
}

// Alt+↑/↓ in a field or ↑/↓ on the handle; at the edge of its group an ingredient moves on into the neighbouring one
const moveIngredient = (item, delta, event) => moveKeepingFocus(event, () => {
  const at = locate(item)
  if (!at || !moveIngredientAcross(formData.value.sections, at[0], at[1], delta)) return null
  return `[data-row="${keyOf(item)}"]`
})

const moveGroup = (section, delta, event) => moveKeepingFocus(event, () =>
  moveSection(formData.value.sections, sectionIndexOf(section), delta) < 0 ? null : `[data-section="${section.key}"]`)

const moveInstruction = async (index, delta, event) => {
  const moved = await moveKeepingFocus(event, () => {
    const to = moveRow(formData.value.instructions, index, delta)
    return to < 0 ? null : `[data-step="${to}"]`
  })
  if (moved) resizeAllTextareas()
}

// Steps are plain texts, so the drag list works on their positions and the new order is applied afterwards
const instructionItems = computed(() => formData.value.instructions.map((_, index) => ({ index })))

const reorderInstructions = (list) => {
  const steps = formData.value.instructions
  formData.value.instructions = list.map(({ index }) => steps[index])
  resizeAllTextareas()
}

const blockImplicitSubmit = (event) => {
  if (preventsImplicitSubmit(event)) event.preventDefault()
}

// Enter opens a new row below (or takes the empty one already there)
const onIngredientEnter = (event, item) => {
  if (event.isComposing) return
  event.preventDefault()
  const at = locate(item)
  if (!at) return
  const items = formData.value.sections[at[0]].items
  focusRow(items[addIngredientBelow(items, at[1])])
}

// Enter in a group name opens a row at the top of that group
const onGroupEnter = (event, section) => {
  if (event.isComposing) return
  event.preventDefault()
  focusRow(section.items[addIngredientBelow(section.items, -1)])
}

const imageLoading = ref(false)

const handleImageUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  imageLoading.value = true
  try {
    formData.value.imageUrl = await resizeImageFile(file)
  } finally {
    imageLoading.value = false
    event.target.value = ''
  }
}

const removeImage = () => {
  formData.value.imageUrl = ''
}

// The last ingredient row always stays
const removeIngredient = (item) => {
  const at = locate(item)
  if (at) removeIngredientAt(formData.value.sections, at[0], at[1])
}

// A removed card leaves its ingredients to the section above; a card with only empty rows goes without asking
const removeGroup = async (section) => {
  if (!isSectionEmpty(section) && !await askConfirm({
    title: 'Gruppe auflösen?',
    text: 'Die Zutaten bleiben erhalten.',
    confirmLabel: 'Auflösen',
    cancelLabel: 'Abbrechen'
  })) return
  const s = sectionIndexOf(section)
  if (s > 0) dissolveSection(formData.value.sections, s)
}

const addInstruction = () => {
  formData.value.instructions.push('')
}

const removeInstruction = (index) => {
  if (formData.value.instructions.length > 1) {
    formData.value.instructions.splice(index, 1)
  }
}

// Own confirmation dialog (instead of confirm()), resolves with true/false
const confirmState = ref(null)
const askConfirm = (dialogProps) => new Promise((resolve) => {
  confirmState.value = {
    props: dialogProps,
    resolve: (answer) => {
      confirmState.value = null
      resolve(answer)
    }
  }
})

// Saving is started by the action bar (RecipeEdit) or by submitting the form itself
const errorTarget = (field) => {
  const form = formRef.value
  switch (field) {
    case 'title': return document.getElementById('title')
    case 'baseServings': return document.getElementById('servings-from')
    case 'servingsTo': return document.getElementById('servings-to')
    case 'prepTimeMinutes': return document.getElementById('prep-time')
    case 'sourceUrl': return sourceUrlRef.value
    case 'ingredients': return form?.querySelector('.ingredient-name[aria-invalid="true"]') ?? form?.querySelector('.ingredient-name')
    case 'instructions': return form?.querySelector('.instruction-row textarea')
    default: return null
  }
}

// Checks first (the form has novalidate): errors go next to the fields, the first one is scrolled to and focused
const submit = async () => {
  if (props.saving) return
  if (ingredientTextMode.value) closeIngredientText(true)
  entry.value = 'form'
  errors.value = validateRecipeForm(formData.value)
  const field = firstError(errors.value)
  if (field) {
    await nextTick()
    const el = errorTarget(field)
    el?.scrollIntoView({ block: 'center', behavior: 'smooth' })
    el?.focus({ preventScroll: true })
    return
  }
  emit('submit', cleanRecipeData(formData.value))
}

defineExpose({ isDirty, saveDraftNow, discardDraft, submit })
</script>

<style scoped>
.recipe-form {
  max-width: 600px;
  margin: 0 auto;
  padding-bottom: 8px;
}

.draft-offer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  padding: 10px 12px;
  margin-bottom: 20px;
  border: 1px solid var(--color-border, #ddd);
  border-left: 4px solid var(--color-primary, #4a5568);
  border-radius: 6px;
  background: var(--color-bg-secondary, #f8f8f8);
  font-size: 0.95rem;
  color: var(--color-text-primary, #333);
}

.draft-offer-actions {
  display: flex;
  gap: 8px;
}

.btn-draft-restore,
.btn-draft-discard {
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
}

.btn-draft-restore {
  border: 1px solid var(--linie);
  background: var(--flaeche);
  color: var(--text);
  font-weight: 600;
}

.btn-draft-restore:hover {
  background: var(--linie);
}

.btn-draft-discard {
  border: 1px solid var(--color-border, #ddd);
  background: transparent;
  color: var(--color-text-secondary, #666);
}

.btn-draft-discard:hover {
  background: var(--color-border, #ddd);
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

.language-select {
  padding: 8px 10px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
  background: var(--flaeche);
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
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 12px 16px;
}

.image-picker {
  flex: 1 1 220px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

.btn-image-pick {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 18px;
  background: var(--flaeche2);
  border: 1px solid var(--linie);
  color: var(--text);
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 600;
  transition: background-color 0.2s ease;
}

.btn-image-pick:hover {
  background: var(--linie);
}

.btn-image-pick svg {
  flex-shrink: 0;
}

.visually-hidden:focus-visible + .btn-image-pick {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}

.image-hint {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-secondary, #666);
}

.image-preview {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.image-preview img {
  width: 160px;
  height: 120px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--color-border, #ddd);
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
  background: var(--neg-weich);
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

.req {
  margin-left: 2px;
}

.required-legend {
  margin: 0 0 16px;
  font-size: 0.85rem;
  color: var(--text2);
}

.recipe-form [aria-invalid='true'] {
  border-color: var(--neg);
}

.recipe-form [aria-invalid='true']:focus-visible {
  outline-color: var(--neg);
}

.field-error {
  margin: 4px 0 0;
  font-size: 0.875rem;
  color: var(--color-error, #e53e3e);
}

.ingredient-list {
  min-height: 44px;
}

.group-items {
  margin-top: 4px;
  padding-left: 10px;
  border-left: 3px solid var(--akzent-weich);
  min-height: 44px;
}

.drag-ghost {
  opacity: 0.4;
}

.sortable-chosen {
  background: var(--akzent-weich);
  border-radius: 8px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.drag-handle,
.btn-remove-icon {
  width: 44px;
  height: 44px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.drag-handle {
  color: var(--text3);
  cursor: grab;
  touch-action: none;
}

.drag-handle:hover {
  color: var(--text2);
  background: var(--flaeche2);
}

.btn-remove-icon {
  color: var(--neg);
  cursor: pointer;
}

.btn-remove-icon:hover {
  background: var(--neg-weich);
}

.drag-handle:focus-visible,
.btn-remove-icon:focus-visible,
.btn-add:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 1px;
}

.ingredient-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.ingredient-text-editor textarea {
  resize: vertical;
  overflow: auto;
  min-height: 160px;
  line-height: 1.5;
}

.ingredient-text-hint {
  margin: 6px 0 0;
  font-size: 0.85rem;
  color: var(--color-text-secondary, #666);
}

.ingredient-text-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.btn-apply-text {
  padding: 8px 16px;
  border: 1px solid var(--linie);
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 600;
  background: var(--flaeche);
  color: var(--text);
}

.btn-apply-text:hover {
  background: var(--linie);
}

@media (max-width: 599px) {
  .instruction-row {
    flex-wrap: wrap;
  }

  .instruction-row .row-actions {
    flex-basis: 100%;
    justify-content: flex-end;
  }
}

.instruction-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.step-number {
  font-weight: 600;
  color: var(--color-text-muted, #999);
  min-width: 24px;
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
  background: var(--neg-weich);
}

.save-error {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--neg-weich);
  border: 1px solid var(--color-error, #e53e3e);
  color: var(--color-error, #e53e3e);
  font-size: 0.9rem;
}

.source-input-wrapper {
  position: relative;
}

.source-input-wrapper input {
  width: 100%;
  padding-right: 32px;
  box-sizing: border-box;
}

.source-clear-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-error, #e53e3e);
  font-size: 0.75rem;
  padding: 2px 4px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.source-clear-btn:hover {
  color: var(--neg);
}

.source-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--flaeche);
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  margin-top: 2px;
  padding: 4px 0;
  list-style: none;
  max-height: 200px;
  overflow-y: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.source-dropdown li {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 0.95rem;
  color: var(--color-text-primary, #333);
}

.source-dropdown li:hover {
  background: var(--color-bg-secondary, #f0f0f0);
}
</style>
