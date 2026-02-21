<template>
  <div class="search-bar">
    <input
      v-model="searchQuery"
      type="text"
      placeholder="Rezepte suchen..."
      @input="handleSearch"
    />
    <span v-if="searchQuery" class="search-clear" @click="clearSearch">
      &times;
    </span>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRecipeStore } from '@/stores/recipeStore'

const store = useRecipeStore()
const searchQuery = ref('')

const handleSearch = () => {
  store.setSearchQuery(searchQuery.value)
}

const clearSearch = () => {
  searchQuery.value = ''
  store.setSearchQuery('')
}
</script>

<style scoped>
.search-bar {
  position: relative;
  max-width: 400px;
}

.search-bar input {
  width: 100%;
  padding: 12px 40px 12px 16px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-bar input:focus {
  outline: none;
  border-color: var(--color-primary, #4a5568);
  box-shadow: 0 0 0 3px rgba(74, 85, 104, 0.1);
}

.search-bar input::placeholder {
  color: var(--color-text-muted, #999);
}

.search-clear {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.5rem;
  color: var(--color-text-muted, #999);
  cursor: pointer;
  line-height: 1;
}

.search-clear:hover {
  color: var(--color-text-primary, #333);
}
</style>
