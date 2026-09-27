<template>
  <form ref="formRef" class="recipe-form" @submit.prevent="handleSubmit" @keydown.enter="blockImplicitSubmit" @focusin="openHintIndex = null">

    <div v-if="draftOffer" class="draft-offer" role="status">
      <span class="draft-offer-text">Entwurf {{ formatDraftTime(draftOffer.savedAt) }} wiederherstellen?</span>
      <div class="draft-offer-actions">
        <button type="button" class="btn-draft-restore" @click="restoreDraftOffer">Wiederherstellen</button>
        <button type="button" class="btn-draft-discard" @click="discardDraftOffer">Verwerfen</button>
      </div>
    </div>

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
        max="10080"
        placeholder="z.B. 30"
      />
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
            @mousedown.prevent="formData.author = ''"
          >✕</button>
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
            @mousedown.prevent="formData.source = ''"
          >✕</button>
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
        <input
          v-model="formData.page"
          type="text"
          placeholder="Seite (bei Büchern)"
        />
      </div>
    </div>

    <div class="form-group">
      <label>Zutaten</label>
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
        <template v-for="(ingredient, index) in formData.ingredients" :key="index">
        <div v-if="isGroupRow(ingredient)" class="ingredient-row ingredient-group-row">
          <input
            v-model="ingredient.group"
            type="text"
            class="ingredient-group-input"
            placeholder="Gruppe, z. B. Salsa (leer = ohne Gruppe)"
            :maxlength="MAX_GROUP_NAME"
            aria-label="Name der Zutatengruppe"
            @keydown.enter="onIngredientEnter($event, index)"
            @keydown.alt.up.prevent="moveIngredient(index, -1, $event)"
            @keydown.alt.down.prevent="moveIngredient(index, 1, $event)"
          />
          <div class="row-actions">
            <button type="button" class="btn-move-icon" data-move="up" tabindex="-1" :disabled="index === 0" @click="moveIngredient(index, -1, $event)" title="Gruppe nach oben" aria-label="Gruppe nach oben">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>
            </button>
            <button type="button" class="btn-move-icon" data-move="down" tabindex="-1" :disabled="index === formData.ingredients.length - 1" @click="moveIngredient(index, 1, $event)" title="Gruppe nach unten" aria-label="Gruppe nach unten">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
            <button type="button" class="btn-remove-icon" tabindex="-1" @click="removeIngredient(index)" title="Gruppe entfernen (Zutaten bleiben)" aria-label="Gruppe entfernen, Zutaten bleiben">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                <path d="M10 11v6M14 11v6"/>
                <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
              </svg>
            </button>
          </div>
        </div>
        <div v-else class="ingredient-row">
          <div class="amount-input-wrapper">
            <input
              v-model="ingredient.amount"
              type="text"
              placeholder="Menge"
              class="ingredient-amount-input"
              @focus="activeAmountIndex = index"
              @blur="activeAmountIndex = null"
              @keydown.enter="onIngredientEnter($event, index)"
              @keydown.alt.up.prevent="moveIngredient(index, -1, $event)"
              @keydown.alt.down.prevent="moveIngredient(index, 1, $event)"
              @paste="onIngredientPaste($event, index)"
            />
            <div v-if="activeAmountIndex === index" class="fraction-keys">
              <button
                v-for="key in FRACTION_KEYS"
                :key="key.value"
                type="button"
                tabindex="-1"
                class="fraction-key"
                :aria-label="`${key.label} einfügen`"
                @pointerdown.prevent="pickFraction($event, index, key.value)"
                @mousedown.prevent
              >{{ key.label }}</button>
            </div>
          </div>
          <div class="unit-input-wrapper">
            <input
              v-model="ingredient.unit"
              type="text"
              placeholder="Einheit"
              autocomplete="off"
              role="combobox"
              aria-autocomplete="list"
              :aria-expanded="unitListOpen(index)"
              :aria-controls="`unit-options-${index}`"
              :aria-activedescendant="unitListOpen(index) && highlightedUnit >= 0 ? `unit-option-${index}-${highlightedUnit}` : undefined"
              @focus="openUnitDropdown($event, index)"
              @input="openUnitDropdown($event, index)"
              @blur="closeUnitDropdown"
              @keydown.enter="onUnitEnter($event, index)"
              @keydown.down.exact="onUnitArrow($event, index, 1)"
              @keydown.up.exact="onUnitArrow($event, index, -1)"
              @keydown.esc="onUnitEscape($event, index)"
              @keydown.alt.up.prevent="moveIngredient(index, -1, $event)"
              @keydown.alt.down.prevent="moveIngredient(index, 1, $event)"
              @paste="onIngredientPaste($event, index)"
            />
            <ul
              v-if="unitListOpen(index)"
              :id="`unit-options-${index}`"
              class="unit-dropdown"
              :class="{ 'drop-up': unitPlacement.up }"
              :style="{ maxHeight: `${unitPlacement.maxHeight}px` }"
              role="listbox"
              tabindex="-1"
            >
              <li
                v-for="(option, oIndex) in unitOptions(index)"
                :id="`unit-option-${index}-${oIndex}`"
                :key="option.value + (option.custom ? '+' : '')"
                role="option"
                :aria-selected="oIndex === highlightedUnit"
                :class="{ highlighted: oIndex === highlightedUnit, 'unit-add': option.custom }"
                @mousedown.prevent="selectUnit(index, option.value)"
              >
                {{ option.value }}<span v-if="option.custom" class="unit-add-label">(eigene Angabe)</span>
              </li>
            </ul>
          </div>
          <div class="name-input-wrapper" :class="{ 'has-hint': nutritionHint(ingredient, index) }">
            <input
              v-model="ingredient.name"
              type="text"
              placeholder="Zutat"
              class="ingredient-name"
              autocomplete="off"
              role="combobox"
              aria-autocomplete="list"
              :aria-expanded="nameSuggestionsFor(index).length > 0"
              :aria-controls="`name-suggestions-${index}`"
              :aria-activedescendant="nameSuggestionsFor(index).length && highlightedSuggestion >= 0 ? `name-suggestion-${index}-${highlightedSuggestion}` : undefined"
              :required="isIngredientNameRequired(formData.ingredients, index)"
              @input="onNameInput(index)"
              @blur="closeNameSuggestions"
              @keydown.enter="onNameEnter($event, index)"
              @keydown.down.exact="onNameArrow($event, index, 1)"
              @keydown.up.exact="onNameArrow($event, index, -1)"
              @keydown.esc="onNameEscape($event, index)"
              @keydown.alt.up.prevent="moveIngredient(index, -1, $event)"
              @keydown.alt.down.prevent="moveIngredient(index, 1, $event)"
              @paste="onIngredientPaste($event, index)"
            />
            <button
              v-if="nutritionHint(ingredient, index)"
              type="button"
              tabindex="-1"
              class="nutrition-hint"
              :class="`nutrition-hint-${nutritionHint(ingredient, index).state}`"
              :title="nutritionHint(ingredient, index).title"
              :aria-label="nutritionHint(ingredient, index).title"
              @click="toggleHintBubble(index)"
            >{{ nutritionHint(ingredient, index).symbol }}</button>
            <span v-if="openHintIndex === index && nutritionHint(ingredient, index)" class="nutrition-hint-bubble" role="tooltip">
              {{ nutritionHint(ingredient, index).title }}
            </span>
            <ul
              v-if="nameSuggestionsFor(index).length > 0"
              :id="`name-suggestions-${index}`"
              class="unit-dropdown name-dropdown"
              role="listbox"
              tabindex="-1"
            >
              <li
                v-for="(suggestion, sIndex) in nameSuggestionsFor(index)"
                :id="`name-suggestion-${index}-${sIndex}`"
                :key="suggestion.name"
                role="option"
                :aria-selected="sIndex === highlightedSuggestion"
                :class="{ highlighted: sIndex === highlightedSuggestion }"
                @mousedown.prevent="selectNameSuggestion(index, suggestion)"
              >
                <span>{{ suggestion.name }}</span>
                <span
                  class="suggestion-mark"
                  :class="suggestion.recognized ? 'nutrition-hint-ok' : 'nutrition-hint-unknown'"
                  :title="suggestion.recognized ? 'Nährwerte vorhanden' : UNKNOWN_HINT"
                >{{ suggestion.recognized ? '✓' : '?' }}</span>
              </li>
            </ul>
          </div>
          <div class="row-actions">
            <button type="button" class="btn-move-icon" data-move="up" tabindex="-1" :disabled="index === 0" @click="moveIngredient(index, -1, $event)" title="Zutat nach oben" aria-label="Zutat nach oben">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>
            </button>
            <button type="button" class="btn-move-icon" data-move="down" tabindex="-1" :disabled="index === formData.ingredients.length - 1" @click="moveIngredient(index, 1, $event)" title="Zutat nach unten" aria-label="Zutat nach unten">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
            <button type="button" class="btn-remove-icon" tabindex="-1" @click="removeIngredient(index)" title="Zutat entfernen" aria-label="Zutat entfernen">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                <path d="M10 11v6M14 11v6"/>
                <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
              </svg>
            </button>
          </div>
        </div>
        </template>
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
      <label>Arbeitsanweisungen</label>
      <div v-for="(instruction, index) in formData.instructions" :key="index" class="instruction-row">
        <span class="step-number">{{ index + 1 }}.</span>
        <textarea
          v-model="formData.instructions[index]"
          rows="1"
          placeholder="Arbeitsschritt eingeben"
          :required="isInstructionRequired(formData.instructions, index)"
          @input="autoResize"
          @keydown.alt.up.prevent="moveInstruction(index, -1, $event)"
          @keydown.alt.down.prevent="moveInstruction(index, 1, $event)"
        ></textarea>
        <div class="row-actions">
          <button type="button" class="btn-move-icon" data-move="up" tabindex="-1" :disabled="index === 0" @click="moveInstruction(index, -1, $event)" title="Schritt nach oben" aria-label="Schritt nach oben">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>
          </button>
          <button type="button" class="btn-move-icon" data-move="down" tabindex="-1" :disabled="index === formData.instructions.length - 1" @click="moveInstruction(index, 1, $event)" title="Schritt nach unten" aria-label="Schritt nach unten">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          <button type="button" class="btn-remove-icon" tabindex="-1" @click="removeInstruction(index)" title="Schritt entfernen" aria-label="Schritt entfernen">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
              <path d="M10 11v6M14 11v6"/>
              <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
            </svg>
          </button>
        </div>
      </div>
      <button type="button" class="btn-add" @click="addInstruction">
        + Schritt hinzufügen
      </button>
    </div>

    <div class="form-actions">
      <div v-if="saveError" class="save-error" role="alert">
        Speichern fehlgeschlagen: {{ saveError }} Deine Eingaben sind noch da, du kannst es erneut versuchen.
      </div>
      <button type="button" class="btn-cancel" @click="emit('cancel')">
        Abbrechen
      </button>
      <button type="submit" class="btn-submit" :disabled="saving">
        {{ saving ? 'Wird gespeichert…' : (isEdit ? 'Rezept aktualisieren' : 'Rezept erstellen') }}
      </button>
    </div>
  </form>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { recipeService } from '@/services/recipeService'
import { ingredientCatalogService } from '@/services/ingredientCatalogService'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import { useAuthStore } from '@/stores/authStore'
import TagInput from '@/components/TagInput.vue'
import { resizeImageFile } from '@/utils/resizeImage'
import { FRACTION_KEYS, applyFraction } from '@/utils/fractionKeys'
import { DROPDOWN_MAX_HEIGHT, dropdownPlacement } from '@/utils/dropdownPlacement'
import {
  emptyIngredient,
  isBlankIngredient,
  cleanRecipeData,
  isIngredientNameRequired,
  isInstructionRequired,
  addIngredientBelow,
  addGroupRow,
  moveRow,
  preventsImplicitSubmit,
  formSnapshot,
  languageChoice,
  applyLanguageChoice,
  autoLanguageLabel
} from '@/utils/recipeFormData'
import { ingredientsFromText, ingredientsToText, insertPastedIngredients } from '@/utils/ingredientText'
import { MAX_GROUP_NAME, ingredientsToRows, isGroupRow } from '@/utils/ingredientGroups'
import {
  SUGGEST_LIMIT,
  SUGGEST_DELAY_MS,
  RECOGNIZE_DELAY_MS,
  UNKNOWN_HINT,
  suggestionQuery,
  visibleSuggestions,
  moveHighlight,
  recognitionKey,
  pendingRecognition,
  recognitionHint
} from '@/utils/ingredientSuggest'
import { sourceSuggestions, authorSuggestions, sourcesOfAuthor } from '@/utils/recipeSources'
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

const emit = defineEmits(['submit', 'cancel', 'titleChange'])

const store = useRecipeStore()
const uiStore = useUiStore()
const authStore = useAuthStore()
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
  language: null,
  languageAuto: true,
  imageUrl: '',
  author: '',
  source: '',
  page: '',
  tags: [],
  ingredients: [emptyIngredient()],
  instructions: ['']
})

const formRef = ref(null)

const languageSelection = computed({
  get: () => languageChoice(formData.value),
  set: (choice) => applyLanguageChoice(formData.value, choice)
})
const savedSnapshot = ref(formSnapshot(formData.value))

const isDirty = () => formSnapshot(formData.value) !== savedSnapshot.value

const activeUnitIndex = ref(null)
const highlightedUnit = ref(-1)
const unitPlacement = ref({ up: false, maxHeight: DROPDOWN_MAX_HEIGHT })
let unitField = null
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

const filteredKnownUnits = (index) => {
  const query = formData.value.ingredients[index]?.unit?.trim().toLowerCase() ?? ''
  if (!query) return knownUnits.value
  return knownUnits.value.filter(u => u.toLowerCase().includes(query))
}

const showAddOption = (index) => {
  const val = formData.value.ingredients[index]?.unit?.trim()
  if (!val) return false
  return !knownUnits.value.some(u => u.toLowerCase() === val.toLowerCase())
}

const unitOptions = (index) => {
  const options = filteredKnownUnits(index).map((value) => ({ value, custom: false }))
  if (showAddOption(index)) options.push({ value: formData.value.ingredients[index].unit, custom: true })
  return options
}

const unitListOpen = (index) => activeUnitIndex.value === index && unitOptions(index).length > 0

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

const selectUnit = (index, value) => {
  formData.value.ingredients[index].unit = value
  activeUnitIndex.value = null
  highlightedUnit.value = -1
}

let unitBlurTimer = null

const closeUnitDropdown = () => {
  clearTimeout(unitBlurTimer)
  unitBlurTimer = setTimeout(() => {
    activeUnitIndex.value = null
    highlightedUnit.value = -1
  }, 150)
}

// Room below the field ends at the keyboard (visual viewport) or the sticky Abbrechen/Speichern bar, whichever is higher
const placeUnitDropdown = () => {
  const field = unitField
  if (!field?.isConnected) return
  const rect = field.getBoundingClientRect()
  const viewport = window.visualViewport
  const viewportBottom = viewport ? viewport.offsetTop + viewport.height : window.innerHeight
  const actionsTop = formRef.value?.querySelector('.form-actions')?.getBoundingClientRect().top ?? viewportBottom
  const headerBottom = document.querySelector('.navbar')?.getBoundingClientRect().bottom ?? 0
  unitPlacement.value = dropdownPlacement({
    fieldTop: rect.top,
    fieldBottom: rect.bottom,
    topLimit: Math.max(headerBottom, viewport?.offsetTop ?? 0) + 4,
    bottomLimit: Math.min(viewportBottom, actionsTop) - 4
  })
}

const openUnitDropdown = (event, index) => {
  clearTimeout(unitBlurTimer)
  unitField = event.target
  activeUnitIndex.value = index
  highlightedUnit.value = -1
  placeUnitDropdown()
}

const scrollHighlightedUnit = async (index) => {
  await nextTick()
  document.getElementById(`unit-option-${index}-${highlightedUnit.value}`)?.scrollIntoView({ block: 'nearest' })
}

// ↓ also reopens a list closed with Esc
const onUnitArrow = (event, index, delta) => {
  if (!unitListOpen(index)) {
    if (delta < 0) return
    openUnitDropdown(event, index)
  }
  const options = unitOptions(index)
  if (!options.length) return
  event.preventDefault()
  highlightedUnit.value = moveHighlight(highlightedUnit.value, delta, options.length)
  scrollHighlightedUnit(index)
}

const onUnitEscape = (event, index) => {
  if (!unitListOpen(index)) return
  event.preventDefault()
  activeUnitIndex.value = null
  highlightedUnit.value = -1
}

// Enter takes the highlighted unit and stays in the row; without one it adds a row below as before
const onUnitEnter = (event, index) => {
  const options = unitOptions(index)
  if (unitListOpen(index) && highlightedUnit.value >= 0 && highlightedUnit.value < options.length && !event.isComposing) {
    event.preventDefault()
    selectUnit(index, options[highlightedUnit.value].value)
    return
  }
  onIngredientEnter(event, index)
}

// Menge field: ½ ¼ ¾ keys while it has the focus; the key press never takes the focus away from the field
const activeAmountIndex = ref(null)

const pickFraction = async (event, index, fraction) => {
  const field = event.currentTarget.closest('.amount-input-wrapper')?.querySelector('input')
  formData.value.ingredients[index].amount = applyFraction(formData.value.ingredients[index].amount, fraction)
  await nextTick()
  if (!field) return
  field.focus()
  field.setSelectionRange(field.value.length, field.value.length)
}

// Zutat field: suggestions from the ingredient catalog while typing (arrow keys + Enter, or tap)
const activeNameIndex = ref(null)
const nameSuggestions = ref([])
const highlightedSuggestion = ref(-1)
let suggestTimer = null
let suggestRequest = 0

const nameSuggestionsFor = (index) =>
  activeNameIndex.value === index
    ? visibleSuggestions(formData.value.ingredients[index]?.name, nameSuggestions.value)
    : []

const resetNameSuggestions = () => {
  clearTimeout(suggestTimer)
  suggestRequest++
  activeNameIndex.value = null
  nameSuggestions.value = []
  highlightedSuggestion.value = -1
}

const onNameInput = (index) => {
  clearTimeout(suggestTimer)
  highlightedSuggestion.value = -1
  const query = suggestionQuery(formData.value.ingredients[index]?.name)
  if (!query) {
    resetNameSuggestions()
    return
  }
  const request = ++suggestRequest
  suggestTimer = setTimeout(async () => {
    try {
      const result = await ingredientCatalogService.suggest(query, SUGGEST_LIMIT)
      if (request !== suggestRequest) return
      activeNameIndex.value = index
      nameSuggestions.value = result
      highlightedSuggestion.value = -1
    } catch {
      // Vorschläge sind nur eine Hilfe, das Feld bleibt frei beschreibbar
    }
  }, SUGGEST_DELAY_MS)
}

const selectNameSuggestion = (index, suggestion) => {
  formData.value.ingredients[index].name = suggestion.name
  resetNameSuggestions()
}

const closeNameSuggestions = () => {
  setTimeout(resetNameSuggestions, 150)
}

const onNameArrow = (event, index, delta) => {
  const list = nameSuggestionsFor(index)
  if (!list.length) return
  event.preventDefault()
  highlightedSuggestion.value = moveHighlight(highlightedSuggestion.value, delta, list.length)
}

const onNameEscape = (event, index) => {
  if (!nameSuggestionsFor(index).length) return
  event.preventDefault()
  resetNameSuggestions()
}

// Enter takes a highlighted suggestion; without one it keeps adding a row below
const onNameEnter = (event, index) => {
  const list = nameSuggestionsFor(index)
  if (list.length && highlightedSuggestion.value >= 0 && !event.isComposing) {
    event.preventDefault()
    selectNameSuggestion(index, list[highlightedSuggestion.value])
    return
  }
  resetNameSuggestions()
  onIngredientEnter(event, index)
}

// ✓ / ? per row: the server tells whether the row gets nutrition values (same matching as the calculation)
const recognition = ref({})
const openHintIndex = ref(null)
let recognizeTimer = null

// While the suggestion list is open the name is still being typed, so the row shows no verdict yet
const nutritionHint = (ingredient, index) => {
  if (nameSuggestionsFor(index).length) return null
  const key = recognitionKey(ingredient)
  return key ? recognitionHint(recognition.value[key]) : null
}

const toggleHintBubble = (index) => {
  openHintIndex.value = openHintIndex.value === index ? null : index
}

const recognizeIngredients = async () => {
  const pending = pendingRecognition(formData.value.ingredients, recognition.value)
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
  () => formData.value.ingredients.map(recognitionKey).join('\n'),
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

watch(() => formData.value.title, (title) => {
  emit('titleChange', title)
})

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
        tags: [...(newRecipe.tags ?? [])],
        ingredients: newRecipe.ingredients?.length
          ? ingredientsToRows(newRecipe.ingredients)
          : [emptyIngredient()],
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

const restoreDraftOffer = () => {
  formData.value = restoreDraft(draftOffer.value, formData.value)
  draftOffer.value = null
  saveDraftNow()
  resizeAllTextareas()
}

const discardDraftOffer = () => {
  discardDraft()
}

const onViewportChange = () => {
  if (activeUnitIndex.value !== null) placeUnitDropdown()
}

const onPageHide = () => saveDraftNow()
const onVisibilityChange = () => {
  if (document.visibilityState === 'hidden') saveDraftNow()
}

onMounted(() => {
  window.addEventListener('pagehide', onPageHide)
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('scroll', onViewportChange, { passive: true })
  window.visualViewport?.addEventListener('resize', onViewportChange)
  window.visualViewport?.addEventListener('scroll', onViewportChange)
})

onBeforeUnmount(() => {
  clearTimeout(draftTimer)
  clearTimeout(suggestTimer)
  clearTimeout(recognizeTimer)
  clearTimeout(unitBlurTimer)
  window.removeEventListener('pagehide', onPageHide)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('scroll', onViewportChange)
  window.visualViewport?.removeEventListener('resize', onViewportChange)
  window.visualViewport?.removeEventListener('scroll', onViewportChange)
})

defineExpose({ isDirty, saveDraftNow, discardDraft })

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

const hasIngredientsOrSteps = () =>
  formData.value.ingredients.some((i) => !isBlankIngredient(i)) ||
  formData.value.instructions.some((i) => i.trim())

const handleScan = async () => {
  if (hasIngredientsOrSteps() && !confirm('Ersetzt vorhandene Zutaten und Schritte — fortfahren?')) return
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
      formData.value.ingredients = ingredientsToRows(
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
    scanned.value = true
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
  formData.value.ingredients.push(emptyIngredient())
}

const addGroup = () => {
  const index = addGroupRow(formData.value.ingredients)
  focusInRow('.ingredient-row', index, '.ingredient-group-input')
}

const ingredientTextMode = ref(false)
const ingredientText = ref('')
const ingredientTextRef = ref(null)

const openIngredientText = async () => {
  ingredientText.value = ingredientsToText(formData.value.ingredients)
  ingredientTextMode.value = true
  await nextTick()
  const el = ingredientTextRef.value
  if (!el) return
  el.focus()
  el.setSelectionRange(el.value.length, el.value.length)
}

const closeIngredientText = (apply) => {
  if (apply) {
    formData.value.ingredients = ingredientsFromText(ingredientText.value, formData.value.ingredients, knownUnits.value)
  }
  ingredientTextMode.value = false
}

const focusInRow = async (rowSelector, index, selector) => {
  await nextTick()
  formRef.value?.querySelectorAll(rowSelector)[index]?.querySelector(selector)?.focus()
}

// Multi-line text pasted into a row becomes one ingredient per line, split into Menge | Einheit | Zutat
const onIngredientPaste = (event, index) => {
  const text = event.clipboardData?.getData('text/plain') ?? ''
  const last = insertPastedIngredients(formData.value.ingredients, index, text, knownUnits.value)
  if (last < 0) return
  event.preventDefault()
  activeUnitIndex.value = null
  focusInRow('.ingredient-row', last, '.ingredient-name')
}

// The moved row keeps the focus: the same field for Alt+↑/↓, the same arrow button for a click
const moveAndKeepFocus = async (rows, rowSelector, index, delta, event) => {
  const target = event?.currentTarget
  const focusable = (row) => (row ? [...row.querySelectorAll('input, textarea, button')] : [])
  const position = focusable(target?.closest?.(rowSelector)).indexOf(target)
  const to = moveRow(rows, index, delta)
  if (to < 0) return -1
  activeUnitIndex.value = null
  await nextTick()
  const row = formRef.value?.querySelectorAll(rowSelector)[to]
  let el = focusable(row)[position]
  if (!el || el.disabled) el = row?.querySelector('[data-move]:not(:disabled)')
  el?.focus()
  return to
}

const moveIngredient = (index, delta, event) =>
  moveAndKeepFocus(formData.value.ingredients, '.ingredient-row', index, delta, event)

const moveInstruction = async (index, delta, event) => {
  if (await moveAndKeepFocus(formData.value.instructions, '.instruction-row', index, delta, event) >= 0) {
    resizeAllTextareas()
  }
}

const blockImplicitSubmit = (event) => {
  if (preventsImplicitSubmit(event)) event.preventDefault()
}

const onIngredientEnter = async (event, index) => {
  if (event.isComposing) return
  event.preventDefault()
  activeUnitIndex.value = null
  const target = addIngredientBelow(formData.value.ingredients, index)
  await nextTick()
  formRef.value?.querySelectorAll('.ingredient-row')[target]?.querySelector('input')?.focus()
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

// A removed heading leaves its ingredients to the group above; the last ingredient row always stays
const removeIngredient = (index) => {
  const rows = formData.value.ingredients
  if (isGroupRow(rows[index]) || rows.filter((row) => !isGroupRow(row)).length > 1) {
    rows.splice(index, 1)
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
  if (props.saving) return
  if (ingredientTextMode.value) closeIngredientText(true)
  emit('submit', cleanRecipeData(formData.value))
}
</script>

<style scoped>
.recipe-form {
  max-width: 600px;
  margin: 0 auto;
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
  border: none;
  background: var(--color-primary, #4a5568);
  color: white;
}

.btn-draft-restore:hover {
  background: var(--color-primary-dark, #2d3748);
}

.btn-draft-discard {
  border: 1px solid var(--color-border, #ddd);
  background: transparent;
  color: var(--color-text-secondary, #666);
}

.btn-draft-discard:hover {
  background: var(--color-border, #ddd);
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

.language-select {
  padding: 8px 10px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
  background: white;
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
  padding: 10px 18px;
  background: var(--color-primary, #4a5568);
  color: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 400;
  transition: background-color 0.2s ease;
}

.btn-image-pick:hover {
  background: var(--color-primary-dark, #2d3748);
}

.image-upload.has-image .btn-image-pick {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.image-upload.has-image .btn-image-pick:hover {
  background: var(--color-border, #ddd);
}

.visually-hidden:focus-visible + .btn-image-pick {
  outline: 2px solid var(--color-primary, #4a5568);
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
  display: grid;
  grid-template-columns: 80px 120px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light, #edf2f7);
}

.ingredient-row:last-of-type {
  border-bottom: none;
}

.ingredient-row.ingredient-group-row {
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-areas: none;
  margin-top: 8px;
  border-bottom: 2px solid var(--color-border, #cbd5e0);
}

.ingredient-group-input {
  font-weight: 600;
}

.ingredient-row > * {
  min-width: 0;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.btn-remove-icon,
.btn-move-icon {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-error, #e53e3e);
  width: 34px;
  height: 34px;
  padding: 0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background-color 0.15s ease;
}

.btn-move-icon {
  color: var(--color-text-secondary, #666);
}

.btn-remove-icon:hover {
  background: rgba(229, 62, 62, 0.1);
}

.btn-move-icon:hover:not(:disabled) {
  background: var(--color-bg-secondary, #f0f0f0);
}

.btn-move-icon:disabled {
  opacity: 0.25;
  cursor: default;
}

.btn-move-icon:focus-visible,
.btn-remove-icon:focus-visible {
  outline: 2px solid var(--color-primary, #4a5568);
  outline-offset: 1px;
}

@media (pointer: coarse) {
  .btn-remove-icon,
  .btn-move-icon {
    width: 40px;
    height: 40px;
  }
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
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  background: var(--color-primary, #4a5568);
  color: white;
}

.btn-apply-text:hover {
  background: var(--color-primary-dark, #2d3748);
}

@media (max-width: 600px) {
  .ingredient-row {
    grid-template-columns: minmax(0, 2fr) minmax(0, 3fr) auto;
    grid-template-areas:
      "amount unit unit"
      "name name actions";
  }

  .ingredient-row .amount-input-wrapper {
    grid-area: amount;
  }

  .ingredient-row .unit-input-wrapper {
    grid-area: unit;
  }

  .ingredient-row .name-input-wrapper {
    grid-area: name;
  }

  .ingredient-row .row-actions {
    grid-area: actions;
  }

  .ingredient-row.ingredient-group-row .row-actions {
    grid-area: auto;
  }

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
  background: rgba(229, 62, 62, 0.1);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
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

.btn-submit:hover:not(:disabled) {
  background: var(--color-primary-dark, #2d3748);
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: wait;
}

.save-error {
  flex-basis: 100%;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(229, 62, 62, 0.08);
  border: 1px solid var(--color-error, #e53e3e);
  color: var(--color-error, #e53e3e);
  font-size: 0.9rem;
}

.unit-input-wrapper,
.amount-input-wrapper {
  position: relative;
  flex: 1;
}

.amount-input-wrapper input {
  width: 100%;
  box-sizing: border-box;
}

.fraction-keys {
  position: absolute;
  bottom: calc(100% + 2px);
  left: 0;
  z-index: 100;
  display: flex;
  gap: 4px;
  padding: 3px;
  background: white;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.fraction-key {
  width: 30px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
  font-size: 0.95rem;
  line-height: 1;
  cursor: pointer;
}

.fraction-key:hover {
  background: var(--color-border, #ddd);
}

@media (pointer: coarse) {
  .fraction-key {
    width: 38px;
    height: 34px;
    font-size: 1.05rem;
  }
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

.unit-dropdown li.highlighted {
  background: var(--color-bg-secondary, #f0f0f0);
}

.unit-dropdown.drop-up {
  top: auto;
  bottom: 100%;
  margin-top: 0;
  margin-bottom: 2px;
}

.name-input-wrapper {
  position: relative;
}

.name-input-wrapper.has-hint input {
  padding-right: 34px;
}

.nutrition-hint {
  position: absolute;
  top: 50%;
  right: 6px;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  font-size: 0.8rem;
  line-height: 22px;
  cursor: help;
}

.nutrition-hint-ok {
  color: var(--color-success, #38a169);
}

.nutrition-hint-unknown {
  color: var(--color-text-muted, #999);
}

button.nutrition-hint-unknown {
  border: 1px solid var(--color-border, #ddd);
  line-height: 20px;
}

.nutrition-hint-bubble {
  position: absolute;
  right: 0;
  bottom: calc(100% + 4px);
  z-index: 101;
  max-width: 260px;
  padding: 6px 10px;
  border-radius: 6px;
  background: var(--color-text-primary, #333);
  color: white;
  font-size: 0.8rem;
  line-height: 1.3;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.name-dropdown li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.suggestion-mark {
  font-size: 0.75rem;
  flex-shrink: 0;
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
  color: #c53030;
}

.source-dropdown {
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
