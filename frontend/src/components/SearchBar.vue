<template>
  <div class="search-bar">
    <div class="search-input-row">
      <input
        :value="inputValue"
        type="text"
        placeholder="Suchen… (Komma = neuer Begriff)"
        @input="handleInput"
        @keydown.enter="commitInput"
        @keydown.backspace="handleBackspace"
      />
      <span v-if="badges.length" class="search-clear-all" @click="clearAll">&times;</span>
    </div>
    <div v-if="badges.length" class="search-badges">
      <span v-for="(badge, index) in badges" :key="index" class="search-badge">
        {{ badge }}
        <span class="badge-remove" @click="removeBadge(index)">&times;</span>
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRecipeStore } from '@/stores/recipeStore'

const store = useRecipeStore()
const badges = ref([])
const inputValue = ref('')

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
  } else {
    inputValue.value = val
  }
}

const commitInput = () => {
  if (inputValue.value.trim()) {
    addBadge(inputValue.value)
    inputValue.value = ''
  }
}

const handleBackspace = (e) => {
  if (inputValue.value === '' && badges.value.length) {
    e.preventDefault()
    badges.value.pop()
    store.setSearchTerms([...badges.value])
  }
}

const removeBadge = (index) => {
  badges.value.splice(index, 1)
  store.setSearchTerms([...badges.value])
}

const clearAll = () => {
  badges.value = []
  inputValue.value = ''
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

.search-input-row {
  position: relative;
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
