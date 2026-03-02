<template>
  <div class="home-view">
    <div v-if="store.error" class="error">
      {{ store.error }}
    </div>

    <div v-else-if="store.filteredRecipes.length === 0 && !store.loading" class="empty">
      <p v-if="store.searchTerms.length">Keine Rezepte gefunden für "{{ store.searchTerms.join(', ') }}"</p>
      <p v-else>Noch keine Rezepte vorhanden. Erstelle dein erstes Rezept!</p>
    </div>

    <div v-if="store.recipes.length > 0" class="recipe-count">
      <span v-if="isFiltered">{{ store.filteredRecipes.length }} von {{ store.recipes.length }} Rezepten</span>
      <span v-else>{{ store.recipes.length }} Rezepte</span>
    </div>

    <div class="recipe-grid">
      <RecipeCard
        v-for="recipe in store.filteredRecipes"
        :key="recipe.id"
        :recipe="recipe"
        :badges="store.computedBadges.get(recipe.id) ?? []"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRecipeStore } from '@/stores/recipeStore'
import RecipeCard from '@/components/RecipeCard.vue'

const store = useRecipeStore()

const isFiltered = computed(() =>
  store.searchTerms.length > 0 && store.filteredRecipes.length < store.recipes.length
)

const onVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    store.fetchRecipes({ background: true })
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

.error,
.empty {
  text-align: center;
  padding: 48px 24px;
  color: var(--color-text-secondary, #666);
}

.error {
  color: var(--color-error, #e53e3e);
}

.recipe-count {
  font-size: 0.875rem;
  color: var(--color-text-muted, #a0aec0);
  margin-bottom: 16px;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}
</style>
