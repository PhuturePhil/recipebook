<template>
  <div class="recipe-edit">
    <header class="edit-header">
      <h1 ref="titleRef">{{ isEdit ? (recipe?.title || 'Rezept bearbeiten') : (currentTitle || 'Neues Rezept erstellen') }}</h1>
    </header>

    <div v-if="loadingRecipe" class="loading">Lädt...</div>

    <div v-else-if="loadError" class="error">{{ loadError }}</div>

    <RecipeForm
      v-else
      ref="formRef"
      :recipe="recipe"
      :saving="saveState.saving"
      :save-error="saveState.error"
      @submit="handleSubmit"
      @cancel="handleCancel"
      @title-change="onTitleChange"
    />
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import { runSave } from '@/utils/recipeFormData'
import RecipeForm from '@/components/RecipeForm.vue'

const route = useRoute()
const router = useRouter()
const store = useRecipeStore()
const uiStore = useUiStore()

const isEdit = computed(() => !!route.params.id)
const recipe = computed(() => {
  if (isEdit.value && store.currentRecipe?.id === Number(route.params.id)) {
    return store.currentRecipe
  }
  return null
})

const formRef = ref(null)
const loadingRecipe = ref(isEdit.value)
const loadError = ref('')
const saveState = reactive({ saving: false, error: '' })
let leaveConfirmed = false

const hasUnsavedChanges = () => !leaveConfirmed && !!formRef.value?.isDirty()

// Reloading or closing the tab needs no prompt: the form keeps a draft and offers it again.
// Leaving inside the app is a deliberate step, so it still asks, and confirming throws the draft away.
// An expired session is the exception: the draft stays so nothing is lost after logging in again.
onBeforeRouteLeave((to) => {
  if (!hasUnsavedChanges()) return true
  if (to.name === 'login') {
    formRef.value?.saveDraftNow()
    return true
  }
  if (!confirm('Ungespeicherte Änderungen verwerfen?')) return false
  formRef.value?.discardDraft()
  return true
})

const titleRef = ref(null)
const currentTitle = ref('')
let titleObserver = null

function getNavTitle() {
  if (currentTitle.value) return currentTitle.value
  return isEdit.value ? 'Rezept bearbeiten' : 'Neues Rezept'
}

function setupObserver() {
  if (!titleRef.value) return
  titleObserver?.disconnect()
  titleObserver = new IntersectionObserver(
    ([entry]) => {
      if (!entry.isIntersecting) {
        uiStore.setNavTitle(getNavTitle())
      } else {
        uiStore.clearNavTitle()
      }
    },
    { threshold: 0 }
  )
  titleObserver.observe(titleRef.value)
}

function onTitleChange(title) {
  currentTitle.value = title
  if (uiStore.navTitle) {
    uiStore.setNavTitle(getNavTitle())
  }
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      await store.fetchRecipeById(route.params.id)
    } catch (error) {
      loadError.value = error.message
    } finally {
      loadingRecipe.value = false
    }
  }
  setupObserver()
})

watch(recipe, (r) => {
  if (r?.title) currentTitle.value = r.title
})

onUnmounted(() => {
  titleObserver?.disconnect()
  uiStore.clearNavTitle()
})

const handleSubmit = async (recipeData) => {
  uiStore.showLoading('Rezept wird gespeichert…')
  const { ok, result } = await runSave(saveState, () => isEdit.value
    ? store.updateRecipe(route.params.id, recipeData)
    : store.createRecipe(recipeData))
  uiStore.hideLoading()
  if (!ok) return
  formRef.value?.discardDraft()
  leaveConfirmed = true
  router.push(`/recipe/${isEdit.value ? route.params.id : result.id}`)
}

const handleCancel = () => {
  router.push(isEdit.value ? `/recipe/${route.params.id}` : '/')
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
