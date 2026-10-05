<template>
  <div class="ingredient-row" :data-row="rowId">
    <div class="amount-input-wrapper">
      <!-- bewusst ohne inputmode: Brüche „1/2“ und Bereiche „1–2“ brauchen die volle Tastatur, ½ ¼ ¾ kommen über die Bruch-Tasten -->
      <input
        v-model="ingredient.amount"
        type="text"
        placeholder="Menge"
        class="ingredient-amount-input"
        aria-label="Menge"
        @focus="amountFocused = true"
        @blur="amountFocused = false"
        @keydown.enter="emit('enter', $event)"
        @keydown.alt.up.prevent="move(-1, $event)"
        @keydown.alt.down.prevent="move(1, $event)"
        @paste="emit('paste', $event)"
      />
      <div v-if="amountFocused" class="fraction-keys">
        <button
          v-for="key in FRACTION_KEYS"
          :key="key.value"
          type="button"
          tabindex="-1"
          class="fraction-key"
          :aria-label="`${key.label} einfügen`"
          @pointerdown.prevent="pickFraction($event, key.value)"
          @mousedown.prevent
        >{{ key.label }}</button>
      </div>
    </div>
    <div class="unit-input-wrapper">
      <input
        v-model="ingredient.unit"
        type="text"
        placeholder="Einheit"
        aria-label="Einheit"
        autocomplete="off"
        role="combobox"
        aria-autocomplete="list"
        :aria-expanded="unitListOpen"
        :aria-controls="`unit-options-${rowId}`"
        :aria-activedescendant="unitListOpen && highlightedUnit >= 0 ? `unit-option-${rowId}-${highlightedUnit}` : undefined"
        @focus="openUnitDropdown"
        @input="openUnitDropdown"
        @blur="closeUnitDropdown"
        @keydown.enter="onUnitEnter"
        @keydown.down.exact="onUnitArrow($event, 1)"
        @keydown.up.exact="onUnitArrow($event, -1)"
        @keydown.esc="onUnitEscape"
        @keydown.alt.up.prevent="move(-1, $event)"
        @keydown.alt.down.prevent="move(1, $event)"
        @paste="emit('paste', $event)"
      />
      <ul
        v-if="unitListOpen"
        :id="`unit-options-${rowId}`"
        class="unit-dropdown"
        :class="{ 'drop-up': unitPlacement.up }"
        :style="{ maxHeight: `${unitPlacement.maxHeight}px` }"
        role="listbox"
        tabindex="-1"
      >
        <li
          v-for="(option, oIndex) in unitOptions"
          :id="`unit-option-${rowId}-${oIndex}`"
          :key="option.value + (option.custom ? '+' : '')"
          role="option"
          :aria-selected="oIndex === highlightedUnit"
          :class="{ highlighted: oIndex === highlightedUnit, 'unit-add': option.custom }"
          @mousedown.prevent="selectUnit(option.value)"
        >
          {{ option.value }}<span v-if="option.custom" class="unit-add-label">(eigene Angabe)</span>
        </li>
      </ul>
    </div>
    <div class="name-input-wrapper" :class="{ 'has-hint': shownHint }">
      <input
        v-model="ingredient.name"
        type="text"
        placeholder="Zutat"
        aria-label="Zutat"
        class="ingredient-name"
        autocomplete="off"
        role="combobox"
        aria-autocomplete="list"
        :aria-expanded="suggestions.length > 0"
        :aria-controls="`name-suggestions-${rowId}`"
        :aria-activedescendant="suggestions.length && highlightedSuggestion >= 0 ? `name-suggestion-${rowId}-${highlightedSuggestion}` : undefined"
        :required="required"
        :aria-required="required ? 'true' : undefined"
        :aria-invalid="invalid ? 'true' : undefined"
        :aria-describedby="invalid ? describedby : undefined"
        @input="onNameInput"
        @blur="closeNameSuggestions"
        @keydown.enter="onNameEnter"
        @keydown.down.exact="onNameArrow($event, 1)"
        @keydown.up.exact="onNameArrow($event, -1)"
        @keydown.esc="onNameEscape"
        @keydown.alt.up.prevent="move(-1, $event)"
        @keydown.alt.down.prevent="move(1, $event)"
        @paste="emit('paste', $event)"
      />
      <button
        v-if="shownHint"
        ref="hintButton"
        type="button"
        tabindex="-1"
        class="nutrition-hint"
        :class="`nutrition-hint-${shownHint.state}`"
        :title="shownHint.title"
        :aria-label="shownHint.title"
        @click="hintOpen = !hintOpen"
      >{{ shownHint.symbol }}</button>
      <span v-if="hintOpen && shownHint" class="nutrition-hint-bubble" role="tooltip">
        {{ shownHint.title }}
      </span>
      <ul
        v-if="suggestions.length > 0"
        :id="`name-suggestions-${rowId}`"
        class="unit-dropdown name-dropdown"
        role="listbox"
        tabindex="-1"
      >
        <li
          v-for="(suggestion, sIndex) in suggestions"
          :id="`name-suggestion-${rowId}-${sIndex}`"
          :key="suggestion.name"
          role="option"
          :aria-selected="sIndex === highlightedSuggestion"
          :class="{ highlighted: sIndex === highlightedSuggestion }"
          @mousedown.prevent="selectNameSuggestion(suggestion)"
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
    <button
      type="button"
      class="drag-handle"
      aria-label="Zutat verschieben: Pfeiltasten"
      title="Ziehen zum Verschieben"
      @keydown.up.prevent="move(-1, $event)"
      @keydown.down.prevent="move(1, $event)"
    >
      <GripIcon />
    </button>
    <button type="button" class="btn-remove-icon" title="Zutat entfernen" aria-label="Zutat entfernen" @click="emit('remove')">
      <AppIcon name="loeschen" :size="18" />
    </button>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import GripIcon from '@/components/form/GripIcon.vue'
import { ingredientCatalogService } from '@/services/ingredientCatalogService'
import { FRACTION_KEYS, applyFraction } from '@/utils/fractionKeys'
import { DROPDOWN_MAX_HEIGHT, dropdownPlacement } from '@/utils/dropdownPlacement'
import {
  SUGGEST_LIMIT,
  SUGGEST_DELAY_MS,
  UNKNOWN_HINT,
  suggestionQuery,
  visibleSuggestions,
  moveHighlight
} from '@/utils/ingredientSuggest'

// One ingredient row: Menge (with ½ ¼ ¾ keys), Einheit (with list), Zutat (with suggestions and ✓/? hint),
// drag handle and trash. The ingredient object is edited in place; everything that concerns other rows
// (Enter, paste, moving, removing) goes to the form as an event.
const props = defineProps({
  ingredient: { type: Object, required: true },
  rowId: { type: String, required: true },
  knownUnits: { type: Array, default: () => [] },
  hint: { type: Object, default: null },
  required: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false },
  describedby: { type: String, default: undefined }
})

const emit = defineEmits(['enter', 'move', 'remove', 'paste'])

const move = (delta, event) => {
  closeUnitList()
  emit('move', delta, event)
}

// Menge field: the key press never takes the focus away from the field
const amountFocused = ref(false)

const pickFraction = async (event, fraction) => {
  const field = event.currentTarget.closest('.amount-input-wrapper')?.querySelector('input')
  props.ingredient.amount = applyFraction(props.ingredient.amount, fraction)
  await nextTick()
  if (!field) return
  field.focus()
  field.setSelectionRange(field.value.length, field.value.length)
}

// Einheit field: known units filtered by the input, plus the typed text as own unit
const unitOpen = ref(false)
const highlightedUnit = ref(-1)
const unitPlacement = ref({ up: false, maxHeight: DROPDOWN_MAX_HEIGHT })
let unitField = null
let unitBlurTimer = null

const unitOptions = computed(() => {
  const typed = props.ingredient.unit?.trim() ?? ''
  const query = typed.toLowerCase()
  const options = (query ? props.knownUnits.filter((u) => u.toLowerCase().includes(query)) : props.knownUnits)
    .map((value) => ({ value, custom: false }))
  if (typed && !props.knownUnits.some((u) => u.toLowerCase() === query)) {
    options.push({ value: props.ingredient.unit, custom: true })
  }
  return options
})

const unitListOpen = computed(() => unitOpen.value && unitOptions.value.length > 0)

const closeUnitList = () => {
  unitOpen.value = false
  highlightedUnit.value = -1
}

const selectUnit = (value) => {
  props.ingredient.unit = value
  closeUnitList()
}

// The timer lets a tap on an option land before the list closes
const closeUnitDropdown = () => {
  clearTimeout(unitBlurTimer)
  unitBlurTimer = setTimeout(closeUnitList, 150)
}

// Room below the field ends at the keyboard (visual viewport) or the Abbrechen/Speichern bar, whichever is higher
const placeUnitDropdown = () => {
  const field = unitField
  if (!field?.isConnected) return
  const rect = field.getBoundingClientRect()
  const viewport = window.visualViewport
  const viewportBottom = viewport ? viewport.offsetTop + viewport.height : window.innerHeight
  const actionsTop = document.querySelector('.form-action-bar')?.getBoundingClientRect().top ?? viewportBottom
  const headerBottom = document.querySelector('.shell-header')?.getBoundingClientRect().bottom ?? 0
  unitPlacement.value = dropdownPlacement({
    fieldTop: rect.top,
    fieldBottom: rect.bottom,
    topLimit: Math.max(headerBottom, viewport?.offsetTop ?? 0) + 4,
    bottomLimit: Math.min(viewportBottom, actionsTop) - 4
  })
}

const openUnitDropdown = (event) => {
  clearTimeout(unitBlurTimer)
  unitField = event.target
  unitOpen.value = true
  highlightedUnit.value = -1
  placeUnitDropdown()
}

// While the list is open, scrolling or the keyboard can change the room around the field
const listenToViewport = (on) => {
  const method = on ? 'addEventListener' : 'removeEventListener'
  window[method]('scroll', placeUnitDropdown, { passive: true })
  window.visualViewport?.[method]('resize', placeUnitDropdown)
  window.visualViewport?.[method]('scroll', placeUnitDropdown)
}

watch(unitListOpen, listenToViewport)

const scrollHighlightedUnit = async () => {
  await nextTick()
  document.getElementById(`unit-option-${props.rowId}-${highlightedUnit.value}`)?.scrollIntoView({ block: 'nearest' })
}

// ↓ also reopens a list closed with Esc
const onUnitArrow = (event, delta) => {
  if (!unitListOpen.value) {
    if (delta < 0) return
    openUnitDropdown(event)
  }
  const options = unitOptions.value
  if (!options.length) return
  event.preventDefault()
  highlightedUnit.value = moveHighlight(highlightedUnit.value, delta, options.length)
  scrollHighlightedUnit()
}

const onUnitEscape = (event) => {
  if (!unitListOpen.value) return
  event.preventDefault()
  closeUnitList()
}

// Enter takes the highlighted unit and stays in the row; without one it adds a row below
const onUnitEnter = (event) => {
  const options = unitOptions.value
  if (unitListOpen.value && highlightedUnit.value >= 0 && highlightedUnit.value < options.length && !event.isComposing) {
    event.preventDefault()
    selectUnit(options[highlightedUnit.value].value)
    return
  }
  closeUnitList()
  emit('enter', event)
}

// Zutat field: suggestions from the ingredient catalog while typing (arrow keys + Enter, or tap)
const nameSuggestions = ref([])
const highlightedSuggestion = ref(-1)
let suggestTimer = null
let suggestRequest = 0

const suggestions = computed(() => visibleSuggestions(props.ingredient.name, nameSuggestions.value))

const resetNameSuggestions = () => {
  clearTimeout(suggestTimer)
  suggestRequest++
  nameSuggestions.value = []
  highlightedSuggestion.value = -1
}

const onNameInput = () => {
  clearTimeout(suggestTimer)
  highlightedSuggestion.value = -1
  const query = suggestionQuery(props.ingredient.name)
  if (!query) {
    resetNameSuggestions()
    return
  }
  const request = ++suggestRequest
  suggestTimer = setTimeout(async () => {
    try {
      const result = await ingredientCatalogService.suggest(query, SUGGEST_LIMIT)
      if (request !== suggestRequest) return
      nameSuggestions.value = result
      highlightedSuggestion.value = -1
    } catch {
      // Vorschläge sind nur eine Hilfe, das Feld bleibt frei beschreibbar
    }
  }, SUGGEST_DELAY_MS)
}

const selectNameSuggestion = (suggestion) => {
  props.ingredient.name = suggestion.name
  resetNameSuggestions()
}

const closeNameSuggestions = () => {
  setTimeout(resetNameSuggestions, 150)
}

const onNameArrow = (event, delta) => {
  const list = suggestions.value
  if (!list.length) return
  event.preventDefault()
  highlightedSuggestion.value = moveHighlight(highlightedSuggestion.value, delta, list.length)
}

const onNameEscape = (event) => {
  if (!suggestions.value.length) return
  event.preventDefault()
  resetNameSuggestions()
}

// Enter takes a highlighted suggestion; without one it adds a row below
const onNameEnter = (event) => {
  const list = suggestions.value
  if (list.length && highlightedSuggestion.value >= 0 && !event.isComposing) {
    event.preventDefault()
    selectNameSuggestion(list[highlightedSuggestion.value])
    return
  }
  resetNameSuggestions()
  emit('enter', event)
}

// ✓ / ? from the form; while the suggestion list is open the name is still being typed, so no verdict yet
const shownHint = computed(() => (suggestions.value.length ? null : props.hint))
const hintOpen = ref(false)
const hintButton = ref(null)

// The bubble closes as soon as the focus goes anywhere else
const closeHintOnFocus = (event) => {
  if (event.target !== hintButton.value) hintOpen.value = false
}

watch(hintOpen, (open) => {
  if (open) document.addEventListener('focusin', closeHintOnFocus)
  else document.removeEventListener('focusin', closeHintOnFocus)
})

onBeforeUnmount(() => {
  clearTimeout(unitBlurTimer)
  clearTimeout(suggestTimer)
  suggestRequest++
  listenToViewport(false)
  document.removeEventListener('focusin', closeHintOnFocus)
})
</script>

<style scoped>
/* ≥600px: Handle | Menge | Einheit | Zutat | Papierkorb (Tab-Reihenfolge bleibt Menge → Einheit → Zutat → Griff) */
.ingredient-row {
  display: grid;
  grid-template-columns: 44px 80px 120px minmax(0, 1fr) 44px;
  grid-template-areas: "handle amount unit name remove";
  gap: 8px;
  align-items: center;
  padding: 6px 0;
}

.ingredient-row > * {
  min-width: 0;
}

.amount-input-wrapper { grid-area: amount; }
.unit-input-wrapper { grid-area: unit; }
.name-input-wrapper { grid-area: name; }
.drag-handle { grid-area: handle; }
.btn-remove-icon { grid-area: remove; }

/* Zeile 1: Menge | Einheit | Handle | Papierkorb — Zeile 2: Zutat volle Breite */
@media (max-width: 599px) {
  .ingredient-row {
    grid-template-columns: minmax(0, 2fr) minmax(0, 3fr) 44px 44px;
    grid-template-areas:
      "amount unit handle remove"
      "name name name name";
    row-gap: 6px;
    padding: 8px 0;
    border-bottom: 1px solid var(--linie);
  }

  .ingredient-row:last-child {
    border-bottom: none;
  }
}

input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--linie);
  border-radius: 6px;
  font-size: 1rem;
}

input[aria-invalid='true'] {
  border-color: var(--neg);
}

input[aria-invalid='true']:focus-visible {
  outline-color: var(--neg);
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
.btn-remove-icon:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 1px;
}

.unit-input-wrapper,
.amount-input-wrapper,
.name-input-wrapper {
  position: relative;
}

.fraction-keys {
  position: absolute;
  bottom: calc(100% + 2px);
  left: 0;
  z-index: 100;
  display: flex;
  gap: 4px;
  padding: 3px;
  background: var(--flaeche);
  border: 1px solid var(--linie);
  border-radius: 6px;
  box-shadow: var(--bar-schatten);
}

.fraction-key {
  width: 30px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: var(--flaeche2);
  color: var(--text);
  font-size: 0.95rem;
  line-height: 1;
  cursor: pointer;
}

.fraction-key:hover {
  background: var(--linie);
}

@media (pointer: coarse) {
  .fraction-key {
    width: 38px;
    height: 34px;
    font-size: 1.05rem;
  }
}

.unit-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--flaeche);
  border: 1px solid var(--linie);
  border-radius: 6px;
  margin-top: 2px;
  padding: 4px 0;
  list-style: none;
  max-height: 200px;
  overflow-y: auto;
  box-shadow: var(--bar-schatten);
}

.unit-dropdown li {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 0.95rem;
  color: var(--text);
}

.unit-dropdown li:hover,
.unit-dropdown li.highlighted {
  background: var(--flaeche2);
}

.unit-dropdown.drop-up {
  top: auto;
  bottom: 100%;
  margin-top: 0;
  margin-bottom: 2px;
}

.unit-add {
  border-top: 1px solid var(--linie);
  color: var(--akzent-text);
}

.unit-add-label {
  font-size: 0.8rem;
  color: var(--text3);
  margin-left: 4px;
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
  color: var(--pos);
}

.nutrition-hint-unknown {
  color: var(--text3);
}

button.nutrition-hint-unknown {
  border: 1px solid var(--linie);
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
  background: var(--text);
  color: var(--bg);
  font-size: 0.8rem;
  line-height: 1.3;
  box-shadow: var(--bar-schatten);
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
</style>
