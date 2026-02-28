<template>
  <div class="home-view">
    <div v-if="store.loading" class="loading">
      Lädt...
    </div>

    <div v-else-if="store.error" class="error">
      {{ store.error }}
    </div>

    <div v-else-if="store.filteredRecipes.length === 0" class="empty">
      <p v-if="store.searchQuery">Keine Rezepte gefunden für "{{ store.searchQuery }}"</p>
      <p v-else>Noch keine Rezepte vorhanden. Erstelle dein erstes Rezept!</p>
    </div>

    <div v-else class="recipe-grid">
      <RecipeCard
        v-for="recipe in store.filteredRecipes"
        :key="recipe.id"
        :recipe="recipe"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRecipeStore } from '@/stores/recipeStore'
import RecipeCard from '@/components/RecipeCard.vue'

const store = useRecipeStore()

const onVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    store.invalidateRecipes()
    store.fetchRecipes()
  }
}

onMounted(() => {
  store.fetchRecipes()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.home-view {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.loading,
.error,
.empty {
  text-align: center;
  padding: 48px 24px;
  color: var(--color-text-secondary, #666);
}

.error {
  color: var(--color-error, #e53e3e);
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}
</style>
