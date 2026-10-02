<template>
  <div class="search-bar">
    <div class="search-controls">
      <div class="search-input-row">
        <AppIcon class="search-icon" name="suche" :size="17" />
        <input
          ref="inputEl"
          :value="inputValue"
          type="text"
          aria-label="Rezepte durchsuchen"
          placeholder="Rezept oder Zutat suchen…"
          @input="handleInput"
          @focus="focused = true"
          @blur="focused = false"
          @keydown.enter="commitInput"
          @keydown.backspace="handleBackspace"
          @keydown.escape="inputEl?.blur()"
        />
        <button v-if="badges.length || inputValue" type="button" class="search-clear-all" aria-label="Suche leeren" @click="clearAll"><AppIcon name="schliessen" :size="16" /></button>
      </div>
      <label class="search-sort" :class="{ 'search-sort--active': store.sortMode !== 'default' }" title="Sortierung">
        <span class="search-sort__label" aria-hidden="true"><AppIcon name="sortieren" :size="18" /><span v-if="sortShort">{{ sortShort }}</span></span>
        <select :value="store.sortMode" aria-label="Sortierung" @change="store.setSortMode($event.target.value)">
          <option v-for="option in SORT_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
    </div>
    <div v-if="textBadges.length" class="search-badges">
      <span v-for="badge in textBadges" :key="badge" class="search-badge">
        {{ badge }}
        <button type="button" class="badge-remove" :aria-label="`${badge} entfernen`" @click="removeTerm(badge)"><AppIcon name="schliessen" :size="14" /></button>
      </span>
    </div>
    <div
      v-if="showTagBar"
      :class="['search-tags', { 'search-tags--expanded': tagsExpanded }]"
      aria-label="Tags"
      @mousedown.prevent
    >
      <div class="search-tags__list">
        <button
          type="button"
          :class="['search-tags__chip', { 'search-tags__chip--active': !activeTags.length }]"
          :aria-pressed="!activeTags.length"
          @click="clearTags"
        >Alle</button>
        <button
          v-for="term in activeTags"
          :key="term"
          type="button"
          class="search-tags__chip search-tags__chip--active"
          aria-pressed="true"
          :title="`${term} entfernen`"
          @click="removeTerm(term)"
        >{{ term }}</button>
        <button
          v-for="entry in tagBar.shown"
          :key="entry.tag"
          type="button"
          class="search-tags__chip"
          aria-pressed="false"
          @click="pickTag(entry.tag)"
        >#{{ entry.tag }} <span class="search-tags__count">{{ entry.count }}</span></button>
        <button v-if="tagBar.hidden" type="button" class="search-tags__more" @click="tagsExpanded = true">
          mehr…
        </button>
        <button v-else-if="tagsExpanded" type="button" class="search-tags__more" @click="tagsExpanded = false">
          weniger
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import AppIcon from '@/components/shell/AppIcon.vue'
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRecipeStore } from '@/stores/recipeStore'
import { SORT_OPTIONS } from '@/utils/recipeSearch'
import { tagBarEntries, tagCounts, tagSearchTerm } from '@/utils/recipeTags'

const store = useRecipeStore()
const badges = ref([])
const inputValue = ref('')
const inputEl = ref(null)
const focused = ref(false)
const tagsExpanded = ref(false)
const LIVE_SEARCH_DELAY_MS = 200
const sortShort = computed(() => SORT_OPTIONS.find((o) => o.value === store.sortMode)?.short ?? '')
let liveSearchTimer = null

// Leeres, fokussiertes Feld: vorhandene Tags zum Antippen, gezählt in der aktuellen Trefferliste
const tagEntries = computed(() => tagCounts(store.filteredRecipes, store.searchTerms))
const tagBar = computed(() => tagBarEntries(tagEntries.value, tagsExpanded.value))
const activeTags = computed(() => badges.value.filter((b) => b.startsWith('#')))
const textBadges = computed(() => badges.value.filter((b) => !b.startsWith('#')))
// Tag-Chips stehen immer unter der Suche (Design-System), beim Tippen weichen sie den Treffern
const showTagBar = computed(() => !inputValue.value.trim() && (tagEntries.value.length > 0 || activeTags.value.length > 0))

onMounted(() => {
  badges.value = [...store.searchTerms]
  inputValue.value = store.pendingSearchTerm
})

onUnmounted(() => clearTimeout(liveSearchTimer))

watch(() => store.searchTerms, (terms) => {
  if (terms.join('\n') !== badges.value.join('\n')) badges.value = [...terms]
})

const updateLiveSearch = (immediate = false) => {
  clearTimeout(liveSearchTimer)
  if (immediate) {
    store.setPendingSearchTerm(inputValue.value)
  } else {
    liveSearchTimer = setTimeout(() => store.setPendingSearchTerm(inputValue.value), LIVE_SEARCH_DELAY_MS)
  }
}

const addBadge = (value) => {
  const trimmed = value.trim()
  if (trimmed && !badges.value.includes(trimmed)) {
    badges.value.push(trimmed)
    store.setSearchTerms([...badges.value])
  }
}

const handleInput = (e) => {
  const val = e.target.value
  if (val.includes(',')) {
    const parts = val.split(',')
    parts.slice(0, -1).forEach((part) => addBadge(part))
    inputValue.value = parts[parts.length - 1].trimStart()
    e.target.value = inputValue.value
    updateLiveSearch(true)
  } else {
    inputValue.value = val
    updateLiveSearch()
  }
}

const commitInput = () => {
  if (inputValue.value.trim()) {
    addBadge(inputValue.value)
    inputValue.value = ''
    updateLiveSearch(true)
  }
}

const handleBackspace = (e) => {
  if (inputValue.value === '' && badges.value.length) {
    e.preventDefault()
    badges.value.pop()
    store.setSearchTerms([...badges.value])
  }
}

const pickTag = (tag) => {
  addBadge(tagSearchTerm(tag))
  inputEl.value?.blur()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const removeTerm = (term) => {
  badges.value = badges.value.filter((b) => b !== term)
  store.setSearchTerms([...badges.value])
}

const clearTags = () => {
  if (!activeTags.value.length) return
  badges.value = [...textBadges.value]
  store.setSearchTerms([...badges.value])
}

const clearAll = () => {
  badges.value = []
  inputValue.value = ''
  updateLiveSearch(true)
  store.setSearchTerms([])
}
</script>

<style scoped>
.search-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.search-controls {
  display: flex;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.search-input-row {
  position: relative;
  flex: 1;
  min-width: 0;
}

.search-icon {
  position: absolute;
  left: 13px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text2);
  pointer-events: none;
}

.search-input-row input {
  display: block;
  width: 100%;
  min-width: 0;
  height: 44px;
  padding: 0 40px 0 38px;
  border: 1px solid var(--linie);
  border-radius: var(--radius);
  background: var(--flaeche);
  color: var(--text);
  font-size: 16px;
  -webkit-appearance: none;
  appearance: none;
}

.search-input-row input:focus {
  outline: none;
  border-color: var(--akzent);
  box-shadow: 0 0 0 3px var(--akzent-weich);
}

.search-clear-all {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: none;
  color: var(--text2);
  cursor: pointer;
  border-radius: 10px;
}

.search-clear-all:hover {
  color: var(--text);
}

.search-sort {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 44px;
  padding: 0 10px;
  border: 1px solid var(--linie);
  border-radius: var(--radius);
  background: var(--flaeche);
  color: var(--text2);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  overflow: hidden;
}

.search-sort__label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.search-sort select {
  position: absolute;
  inset: 0;
  width: 100%;
  opacity: 0;
  cursor: pointer;
  font-size: 16px;
}

.search-sort:focus-within {
  border-color: var(--akzent);
}

.search-sort--active {
  color: var(--akzent-text);
  border-color: var(--akzent);
}

.search-tags {
  display: flex;
  min-width: 0;
  font-size: 13px;
}

.search-tags__list {
  display: flex;
  flex: 1;
  width: 0;
  gap: 7px;
  flex-wrap: nowrap;
  overflow-x: auto;
  scrollbar-width: none;
  margin: 0 -16px;
  padding: 0 16px 2px;
}

.search-tags__list::-webkit-scrollbar {
  display: none;
}

.search-tags--expanded .search-tags__list {
  flex-wrap: wrap;
  max-height: 40vh;
  overflow-y: auto;
}

.search-tags__chip,
.search-tags__more {
  flex: 0 0 auto;
  height: 30px;
  padding: 0 12px;
  border: 1px solid var(--linie);
  border-radius: 999px;
  background: var(--flaeche);
  color: var(--text);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}

.search-tags__chip:hover {
  border-color: var(--akzent);
}

.search-tags__chip--active {
  background: var(--akzent);
  border-color: var(--akzent);
  color: var(--akzent-kontrast);
}

.search-tags__count {
  color: var(--text3);
  font-weight: 500;
  margin-left: 2px;
}

.search-tags__more {
  border-style: dashed;
  color: var(--text2);
}

@media (min-width: 1024px) {
  .search-tags__list {
    flex-wrap: wrap;
    margin: 0;
    padding: 0;
    overflow: visible;
  }
}

.search-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.search-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px 4px 3px 12px;
  background: var(--akzent);
  color: var(--akzent-kontrast);
  border-radius: 999px;
  font-size: 13.5px;
  font-weight: 600;
}

.badge-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 50%;
  background: none;
  color: inherit;
  cursor: pointer;
  opacity: 0.85;
}

.badge-remove:hover {
  opacity: 1;
}
</style>
