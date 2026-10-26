<template>
  <!-- Picker „Gericht planen“: Suche über die für die Familie planbaren Rezepte (Mini-Foto) + freier Eintrag -->
  <div class="plan-search">
    <AppIcon name="suche" :size="18" />
    <input
      v-model="query"
      class="plan-input plan-picker__search"
      type="search"
      :placeholder="t('search')"
      :aria-label="t('search')"
      autocomplete="off"
      enterkeyhint="search"
    >
  </div>
  <div class="plan-sheet__body" role="list">
    <button
      v-for="r in shown"
      :key="r.id"
      type="button"
      class="plan-pick"
      role="listitem"
      :disabled="busy"
      @click="emit('pick', { recipeId: r.id })"
    >
      <PlanPhoto :url="thumbFor(r.imageUrl)" />
      <div class="plan-mt">
        <h3>{{ r.title }}</h3>
        <p v-if="metaOf(r)">{{ metaOf(r) }}</p>
      </div>
    </button>
    <p v-if="!shown.length && !loading" class="plan-picker__none">{{ t('noResults') }}</p>
    <p v-if="!shown.length && loading" class="plan-picker__none">…</p>
  </div>
  <form class="plan-free" @submit.prevent="submitFree">
    <label :for="freeId">{{ t('freeEntry') }}</label>
    <div class="plan-free__row">
      <input
        :id="freeId"
        v-model="freeText"
        class="plan-input"
        type="text"
        maxlength="120"
        :placeholder="t('freeEntryHint')"
        enterkeyhint="done"
      >
      <button type="submit" class="plan-btn" :disabled="busy || !freeText.trim()">{{ t('add') }}</button>
    </div>
  </form>
</template>

<script setup>
import { computed, onMounted, ref, useId } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import PlanPhoto from './PlanPhoto.vue'
import { useRecipeStore } from '@/stores/recipeStore'
import { entryMeta, pickerRecipes, thumbFor } from '@/utils/mealPlan'
import { planText as t } from '@/utils/planTexts'

const props = defineProps({
  familyId: { type: Number, required: true },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['pick'])
const store = useRecipeStore()
const query = ref('')
const freeText = ref('')
const freeId = `plan-free-${useId()}`
const loading = computed(() => store.loading)
const shown = computed(() => pickerRecipes(store.recipes, props.familyId, query.value))
const metaOf = (r) => entryMeta({ recipe: r })

function submitFree() {
  const text = freeText.value.replace(/\s+/g, ' ').trim()
  if (text) emit('pick', { freeText: text })
}

onMounted(() => store.fetchRecipes({ quiet: true }))
</script>
