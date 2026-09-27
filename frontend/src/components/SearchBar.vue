<template>
  <div class="search-bar">
    <div class="search-controls">
      <div class="search-input-row">
        <input
          ref="inputEl"
          :value="inputValue"
          type="text"
          aria-label="Rezepte durchsuchen"
          placeholder="Suchen… (Komma = neuer Begriff)"
          @input="handleInput"
          @focus="focused = true"
          @blur="focused = false"
          @keydown.enter="commitInput"
          @keydown.backspace="handleBackspace"
          @keydown.escape="inputEl?.blur()"
        />
        <span v-if="badges.length || inputValue" class="search-clear-all" @click="clearAll">&times;</span>
      </div>
      <label class="search-sort" :class="{ 'search-sort--active': store.sortMode !== 'default' }" title="Sortierung">
        <span aria-hidden="true">⇅{{ sortShort ? ` ${sortShort}` : '' }}</span>
        <select :value="store.sortMode" aria-label="Sortierung" @change="store.setSortMode($event.target.value)">
          <option v-for="option in SORT_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>
    </div>
    <div v-if="badges.length" class="search-badges">
      <span v-for="(badge, index) in badges" :key="index" class="search-badge">
        {{ badge }}
        <span class="badge-remove" @click="removeBadge(index)">&times;</span>
      </span>
    </div>
    <div
      v-if="showTagBar"
      :class="['search-tags', { 'search-tags--expanded': tagsExpanded }]"
      aria-label="Tags"
      @mousedown.prevent
    >
      <span class="search-tags__label">Tags:</span>
      <div class="search-tags__list">
        <button
          v-for="entry in tagBar.shown"
          :key="entry.tag"
          type="button"
          class="search-tags__chip"
          @click="pickTag(entry.tag)"
        >{{ entry.tag }} <span class="search-tags__count">({{ entry.count }})</span></button>
      </div>
      <button v-if="tagBar.hidden" type="button" class="search-tags__more" @click="tagsExpanded = true">
        mehr…
      </button>
      <button v-else-if="tagsExpanded" type="button" class="search-tags__more" @click="tagsExpanded = false">
        weniger
      </button>
    </div>
  </div>
</template>

<script setup>
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
const showTagBar = computed(() => focused.value && !inputValue.value.trim() && tagEntries.value.length > 0)

watch(focused, (isFocused) => {
  if (!isFocused) tagsExpanded.value = false
})

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

const removeBadge = (index) => {
  badges.value.splice(index, 1)
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
  max-width: 500px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-controls {
  display: flex;
  gap: 6px;
  align-items: stretch;
}

.search-input-row {
  position: relative;
  flex: 1;
  min-width: 0;
}

.search-sort {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  padding: 0 10px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 8px;
  background: var(--color-bg-card, #fff);
  color: var(--color-text-muted, #999);
  font-size: 0.9rem;
  white-space: nowrap;
  cursor: pointer;
}

.search-sort select {
  position: absolute;
  inset: 0;
  width: 100%;
  opacity: 0;
  cursor: pointer;
  font-size: 1rem;
}

.search-sort:focus-within {
  border-color: var(--color-primary, #4a5568);
}

.search-sort--active {
  color: var(--color-text-primary, #333);
  border-color: var(--color-primary, #4a5568);
}

.search-input-row input {
  width: 100%;
  padding: 12px 40px 12px 16px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-input-row input:focus {
  outline: none;
  border-color: var(--color-primary, #4a5568);
  box-shadow: 0 0 0 3px rgba(74, 85, 104, 0.1);
}

.search-input-row input::placeholder {
  color: var(--color-text-muted, #999);
}

.search-clear-all {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.5rem;
  color: var(--color-text-muted, #999);
  cursor: pointer;
  line-height: 1;
}

.search-clear-all:hover {
  color: var(--color-text-primary, #333);
}

.search-tags {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 0.8rem;
}

.search-tags__label {
  padding: 4px 0;
  color: var(--color-text-muted, #999);
}

.search-tags__list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.search-tags--expanded .search-tags__list {
  max-height: 40vh;
  overflow-y: auto;
}

.search-tags__chip,
.search-tags__more {
  flex: 0 0 auto;
  padding: 3px 9px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 999px;
  background: var(--color-bg-card, #fff);
  color: var(--color-text-secondary, #666);
  font: inherit;
  white-space: nowrap;
  cursor: pointer;
}

.search-tags__chip:hover {
  border-color: var(--color-primary, #4a5568);
  color: var(--color-text-primary, #333);
}

.search-tags__count {
  color: var(--color-text-muted, #999);
}

.search-tags__more {
  border-style: dashed;
}

/* Mobil eingeklappt nur eine wischbare Zeile, damit die fixierte Kopfzeile kaum wächst */
@media (max-width: 600px) {
  .search-tags:not(.search-tags--expanded) .search-tags__list {
    flex: 1;
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .search-tags:not(.search-tags--expanded) .search-tags__list::-webkit-scrollbar {
    display: none;
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
  gap: 4px;
  padding: 4px 10px;
  background: var(--color-primary, #4a5568);
  color: #fff;
  border-radius: 999px;
  font-size: 0.875rem;
}

.badge-remove {
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  opacity: 0.8;
}

.badge-remove:hover {
  opacity: 1;
}
</style>
