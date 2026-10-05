<template>
  <div class="recipe-edit">
    <div v-if="loadingRecipe" class="loading">Lädt...</div>

    <div v-else-if="loadError" class="error">{{ loadError }}</div>

    <template v-else>
      <RecipeForm
        ref="formRef"
        :recipe="recipe"
        :saving="saveState.saving"
        :save-error="saveState.error"
        @submit="handleSubmit"
      />
      <FormActionBar
        :submit-label="isEdit ? 'Speichern' : 'Rezept erstellen'"
        :saving="saveState.saving"
        @cancel="handleCancel"
        @submit="formRef?.submit()"
      />
    </template>

    <ConfirmDialog
      v-if="leaveDialog"
      title="Änderungen verwerfen?"
      text="Deine Eingaben in diesem Rezept gehen verloren."
      confirm-label="Verwerfen"
      cancel-label="Weiterbearbeiten"
      destructive
      @confirm="leaveDialog.resolve(true)"
      @cancel="leaveDialog.resolve(false)"
    />
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import { runSave } from '@/utils/recipeFormData'
import RecipeForm from '@/components/RecipeForm.vue'
import FormActionBar from '@/components/shell/FormActionBar.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

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

// Own dialog instead of confirm(): resolves with true for "Verwerfen", false for "Weiterbearbeiten"
const leaveDialog = ref(null)
const askToLeave = () => new Promise((resolve) => {
  leaveDialog.value = {
    resolve: (leave) => {
      leaveDialog.value = null
      resolve(leave)
    }
  }
})

// Every way out inside the app ends here: Abbrechen, the back gesture/button (vue-router restores the URL when
// the answer is "Weiterbearbeiten") and every link, including the sidebar on large screens.
// "Verwerfen" throws the draft away. An expired session is the exception: the draft stays so nothing is lost
// after logging in again.
onBeforeRouteLeave(async (to) => {
  if (!hasUnsavedChanges()) return true
  if (to.name === 'login') {
    formRef.value?.saveDraftNow()
    return true
  }
  if (leaveDialog.value) return false
  if (!await askToLeave()) return false
  formRef.value?.discardDraft()
  return true
})

// Reload, closing the tab or leaving the app: the browser shows its own question (a custom text is not possible).
// The form saves its draft on pagehide/visibilitychange anyway, so even "Verlassen" loses nothing — the next
// visit offers the draft again.
const onBeforeUnload = (event) => {
  if (!hasUnsavedChanges()) return
  event.preventDefault()
  event.returnValue = ''
}

onMounted(async () => {
  window.addEventListener('beforeunload', onBeforeUnload)
  if (isEdit.value) {
    try {
      await store.fetchRecipeById(route.params.id)
    } catch (error) {
      loadError.value = error.message
    } finally {
      loadingRecipe.value = false
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
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

@media (min-width: 1024px) {
  .recipe-edit > .form-action-bar {
    max-width: 600px;
    margin: 0 auto;
  }
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
