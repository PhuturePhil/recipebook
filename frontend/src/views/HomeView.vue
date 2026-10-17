<template>
  <GuestHome v-if="!authStore.isAuthenticated" />
  <div v-else class="home-view">
    <div v-if="store.error" class="error">
      {{ store.error }}
    </div>

    <div v-else-if="store.filteredRecipes.length === 0 && !store.loading" class="empty">
      <p v-if="store.activeSearchTerms.length">Keine Rezepte gefunden für "{{ store.searchQuery }}"</p>
      <p v-else-if="store.recipes.length">In dieser Sammlung sind noch keine Rezepte.</p>
      <p v-else>Noch keine Rezepte vorhanden. Erstelle dein erstes Rezept!</p>
    </div>

    <div v-if="store.scopedRecipes.length > 0" class="recipe-count">
      <span v-if="isFiltered">{{ store.filteredRecipes.length }} von {{ store.scopedRecipes.length }} Rezepten</span>
      <span v-else>{{ store.scopedRecipes.length }} {{ store.scopedRecipes.length === 1 ? 'Rezept' : 'Rezepte' }}</span>
      <span v-if="scopeLabel"> · {{ scopeLabel }}</span>
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
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRecipeStore, SCOPE_ALL, SCOPE_MINE } from '@/stores/recipeStore'
import { useAuthStore } from '@/stores/authStore'
import { useFamilyStore } from '@/stores/familyStore'
import RecipeCard from '@/components/RecipeCard.vue'
import GuestHome from '@/components/GuestHome.vue'

const store = useRecipeStore()
const authStore = useAuthStore()
const familyStore = useFamilyStore()

const isFiltered = computed(() =>
  store.activeSearchTerms.length > 0 && store.filteredRecipes.length < store.scopedRecipes.length
)

// Trefferzeile nennt die gewählte Sammlung („38 Rezepte · Familie Pastoors“)
const scopeLabel = computed(() => {
  if (store.scope === SCOPE_MINE) return 'Meine'
  if (store.scope === SCOPE_ALL) return ''
  return familyStore.byId(store.scope)?.name ?? ''
})

// Familie verlassen o. Ä.: gewählte Sammlung gibt es nicht mehr → zurück auf „Alle“
watch(() => [familyStore.loaded, familyStore.families.length], () => {
  if (typeof store.scope === 'number' && familyStore.loaded && !familyStore.byId(store.scope)) store.setScope(SCOPE_ALL)
})

const onVisibilityChange = () => {
  if (document.visibilityState === 'visible' && authStore.isAuthenticated) {
    store.fetchRecipes({ background: true })
  }
}

onMounted(() => {
  if (authStore.isAuthenticated) store.fetchRecipes()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.home-view {
  max-width: var(--shell-max);
  margin: 0 auto;
  padding: 4px 16px 24px;
}

@media (min-width: 1024px) {
  .home-view {
    padding: 8px 34px 40px;
  }
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
  margin-bottom: 12px;
  text-align: center;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
</style>
