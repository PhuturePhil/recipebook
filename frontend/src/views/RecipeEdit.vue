<template>
  <div class="recipe-edit">
    <header class="edit-header">
      <h1>{{ isEdit ? 'Rezept bearbeiten' : 'Neues Rezept erstellen' }}</h1>
    </header>

    <div v-if="store.loading" class="loading">Lädt...</div>

    <div v-else-if="store.error" class="error">{{ store.error }}</div>

    <RecipeForm v-else :recipe="recipe" @submit="handleSubmit" />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import RecipeForm from '@/components/RecipeForm.vue'

const route = useRoute()
const router = useRouter()
const store = useRecipeStore()
const uiStore = useUiStore()

const isEdit = computed(() => !!route.params.id)
const recipe = computed(() => {
  if (isEdit.value && store.currentRecipe) {
    return store.currentRecipe
  }
  return null
})

onMounted(async () => {
  if (isEdit.value) {
    await store.fetchRecipeById(route.params.id)
  } else {
    uiStore.setNavTitle('Neues Rezept')
  }
})

watch(recipe, (r) => {
  if (r?.title) uiStore.setNavTitle(r.title)
})

onUnmounted(() => {
  uiStore.clearNavTitle()
})

const handleSubmit = async (recipeData) => {
  try {
    if (isEdit.value) {
      await store.updateRecipe(route.params.id, recipeData)
      router.push(`/recipe/${route.params.id}`)
    } else {
      const newRecipe = await store.createRecipe(recipeData)
      router.push(`/recipe/${newRecipe.id}`)
    }
  } catch (error) {
    alert('Fehler beim Speichern des Rezepts: ' + error.message)
  }
}
</script>

<style scoped>
.recipe-edit {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.edit-header {
  margin-bottom: 32px;
}

.edit-header h1 {
  margin: 0;
  font-size: 2rem;
  color: var(--color-text-primary, #333);
}

.loading,
.error {
  text-align: center;
  padding: 48px 24px;
  color: var(--color-text-secondary, #666);
}

.error {
  color: var(--color-error, #e53e3e);
}
</style>
