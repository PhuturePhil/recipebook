<template>
  <div class="recipe-detail">
    <div v-if="store.loading" class="loading">Lädt...</div>

    <div v-else-if="store.error" class="error">{{ store.error }}</div>

    <div v-else-if="recipe" class="recipe-content">
      <header class="recipe-header">
        <h1 ref="titleRef">{{ recipe.title }}</h1>
        <div class="recipe-tools">
          <button class="btn-tool" @click="showShareModal = true">Teilen</button>
          <button class="btn-tool" @click="printRecipe">Als PDF speichern</button>
        </div>
      </header>

      <div v-if="recipe.imageUrl" class="recipe-image">
        <img :src="recipe.imageUrl" :alt="recipe.title" />
      </div>

      <p v-if="recipe.description" class="recipe-description">
        {{ recipe.description }}
      </p>

      <div class="recipe-meta">
        <span v-if="recipe.prepTimeMinutes" class="meta-item">
          {{ formatPrepTime(recipe.prepTimeMinutes) }}
        </span>
        <span v-if="recipe.baseServings" class="meta-item">
          {{ recipe.baseServings }}{{ recipe.servingsTo ? `–${recipe.servingsTo}` : '' }} Personen
        </span>
        <button
          v-if="nutrition?.hasValues"
          type="button"
          :class="['meta-item', 'meta-item--kcal', { 'meta-item--incomplete': !nutrition.coverage.complete }]"
          :title="kcalChipTitle"
          @click="activeTab = 'nutrition'"
        >
          {{ nutrition.coverage.complete ? '' : 'mind. ' }}{{ formatKcal(nutrition.perServing.kcal) }} kcal pro Portion
        </button>
      </div>

      <div v-if="recipe.author || recipe.source" class="recipe-source">
        <span class="source-label">Quelle:</span>
        <span v-if="recipe.author" class="source-author">{{ recipe.author }}</span>
        <span v-if="recipe.source" class="source-name">{{ recipe.source }}</span>
        <span v-if="recipe.page" class="source-page">Seite {{ recipe.page }}</span>
      </div>

      <div class="servings-control">
        <span class="servings-label">Personen:</span>
        <button class="servings-btn" @click="decreaseServings" :disabled="currentServings <= 1">-</button>
        <span class="servings-value">{{ currentServings }}</span>
        <button class="servings-btn" @click="increaseServings">+</button>
      </div>

      <section v-if="recipe.ingredients?.length" class="recipe-section">
        <div class="section-header">
          <div class="tab-toggle">
            <button :class="['tab-btn', { active: activeTab === 'ingredients' }]" @click="activeTab = 'ingredients'">Zutaten</button>
            <button :class="['tab-btn', { active: activeTab === 'nutrition' }]" @click="activeTab = 'nutrition'">Nährwerte</button>
          </div>
        </div>

        <ul v-if="activeTab === 'ingredients'" class="ingredients-list">
          <li v-for="(ingredient, index) in scaledIngredients" :key="index">
            <span class="ingredient-amount">{{ ingredient.amount }} {{ ingredient.unit }}</span>
            <span class="ingredient-name">{{ ingredient.name }}</span>
          </li>
        </ul>

        <template v-if="activeTab === 'nutrition'">
          <p v-if="nutritionLoading" class="nutrition-state">Nährwerte werden berechnet…</p>
          <p v-else-if="nutritionError" class="nutrition-state nutrition-state--error">{{ nutritionError }}</p>
          <NutritionPanel
            v-else-if="nutrition"
            :nutrition="nutrition"
            :info="nutritionInfo"
          />
        </template>
      </section>

      <section v-if="recipe.instructions?.length" class="recipe-section">
        <h2>Zubereitung</h2>
        <ol class="instructions-list">
          <li v-for="(instruction, index) in recipe.instructions" :key="index">
            {{ instruction }}
          </li>
        </ol>
      </section>

      <footer class="print-footer">
        <p>Aus dem Familienkochbuch — pastoors.cloud</p>
        <p class="print-footer__small">Für den privaten Gebrauch</p>
      </footer>

      <div class="detail-actions">
        <button class="btn-cancel" @click="goBack">Abbrechen</button>
        <button class="btn-delete" @click="handleDelete">Löschen</button>
        <router-link :to="`/recipe/${recipe.id}/edit`" class="btn-edit">
          Bearbeiten
        </router-link>
      </div>
    </div>

    <div v-else class="not-found">
      <p>Rezept nicht gefunden</p>
      <router-link to="/" class="btn-cancel">Zurück zur Übersicht</router-link>
    </div>

    <ShareModal v-if="showShareModal && recipe" :recipe-id="recipe.id" @close="showShareModal = false" />
  </div>
</template>

<script setup>
import { computed, ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import { scaleIngredients } from '@/utils/scaleIngredients'
import ShareModal from '@/components/ShareModal.vue'
import NutritionPanel from '@/components/NutritionPanel.vue'
import { nutritionService, formatKcal } from '@/services/nutritionService'

const route = useRoute()
const router = useRouter()
const store = useRecipeStore()
const uiStore = useUiStore()



const recipe = computed(() => store.currentRecipe)
const currentServings = ref(1)
const activeTab = ref('ingredients')
const titleRef = ref(null)
const showShareModal = ref(false)
const nutrition = ref(null)
const nutritionInfo = ref(null)
const nutritionLoading = ref(false)
const nutritionError = ref(null)
let titleObserver = null

const loadNutrition = async (id) => {
  nutritionLoading.value = true
  nutritionError.value = null
  try {
    const [data, info] = await Promise.all([
      nutritionService.getRecipeNutrition(id),
      nutritionService.getInfo().catch(() => null),
    ])
    nutrition.value = data
    nutritionInfo.value = info
  } catch (error) {
    nutritionError.value = 'Nährwerte konnten nicht geladen werden.'
  } finally {
    nutritionLoading.value = false
  }
}

const kcalChipTitle = computed(() => {
  const n = nutrition.value
  if (!n?.hasValues) return ''
  const base = `Berechnet aus ${n.coverage.calculated} von ${n.coverage.relevant} Zutaten`
  return n.coverage.complete ? base : `${base} – unvollständig, tatsächlicher Wert liegt höher`
})

onMounted(async () => {
  activeTab.value = 'ingredients'
  await store.fetchRecipeById(route.params.id)
  if (recipe.value) {
    currentServings.value = recipe.value.baseServings
    loadNutrition(recipe.value.id)
  }

  if (titleRef.value) {
    titleObserver = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting && recipe.value?.title) {
          uiStore.setNavTitle(recipe.value.title)
        } else {
          uiStore.clearNavTitle()
        }
      },
      { threshold: 0 }
    )
    titleObserver.observe(titleRef.value)
  }
})

onUnmounted(() => {
  titleObserver?.disconnect()
  uiStore.clearNavTitle()
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const increaseServings = () => {
  currentServings.value++
}

const decreaseServings = () => {
  if (currentServings.value > 1) {
    currentServings.value--
  }
}

const scaledIngredients = computed(() =>
  scaleIngredients(recipe.value?.ingredients, recipe.value?.baseServings, currentServings.value)
)

const printRecipe = async () => {
  activeTab.value = 'ingredients'
  await nextTick()
  const originalTitle = document.title
  document.title = recipe.value.title
  window.addEventListener('afterprint', () => { document.title = originalTitle }, { once: true })
  window.print()
}

const formatPrepTime = (minutes) => {
  if (!minutes) return ''
  if (minutes < 60) return `${minutes} Min.`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h} Std. ${m} Min.` : `${h} Std.`
}

const handleDelete = async () => {
  if (confirm('Möchtest du dieses Rezept wirklich löschen?')) {
    try {
      await store.deleteRecipe(route.params.id)
      router.push('/')
    } catch (error) {
      alert('Fehler beim Löschen des Rezepts: ' + error.message)
    }
  }
}
</script>

<style scoped>
.recipe-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.loading,
.error,
.not-found {
  text-align: center;
  padding: 48px 24px;
  color: var(--color-text-secondary, #666);
}

.error {
  color: var(--color-error, #e53e3e);
}

.recipe-header {
  margin-bottom: 24px;
}

.recipe-header h1 {
  margin: 0;
  font-size: 2rem;
  color: var(--color-text-primary, #333);
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  position: sticky;
  bottom: 0;
  background: var(--color-bg, #f9fafb);
  padding: 12px 0;
  border-top: 1px solid var(--color-border, #e2e8f0);
  margin-top: 32px;
}

.detail-actions .btn-cancel {
  margin-right: auto;
}

.btn-edit,
.btn-delete,
.btn-cancel {
  padding: 8px 14px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.2s ease;
  white-space: nowrap;
}

.btn-edit {
  background: var(--color-primary, #4a5568);
  color: white;
}

.btn-edit:hover {
  background: var(--color-primary-dark, #2d3748);
}

.btn-delete {
  background: transparent;
  color: var(--color-error, #e53e3e);
  border: 1px solid var(--color-error, #e53e3e);
}

.btn-delete:hover {
  background: rgba(229, 62, 62, 0.1);
}

.btn-cancel {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.btn-cancel:hover {
  background: var(--color-border, #ddd);
}

.recipe-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  font-size: 0.95rem;
  color: var(--color-text-secondary, #666);
}

.meta-item {
  padding: 4px 10px;
  background: var(--color-bg-secondary, #f0f0f0);
  border-radius: 4px;
  font-weight: 500;
}

.meta-item--kcal {
  border: 1px solid transparent;
  font-family: inherit;
  font-size: inherit;
  color: #22543d;
  background: #f0fff4;
  cursor: pointer;
}

.meta-item--kcal:hover {
  border-color: #9ae6b4;
}

.meta-item--incomplete {
  color: #744210;
  background: #fffaf0;
}

.nutrition-state {
  color: var(--color-text-secondary, #666);
}

.nutrition-state--error {
  color: var(--color-error, #e53e3e);
}

.recipe-description {
  font-size: 1.125rem;
  color: var(--color-text-secondary, #666);
  line-height: 1.6;
  margin-bottom: 16px;
}

.recipe-source {
  margin-bottom: 24px;
  padding: 12px 16px;
  background: var(--color-bg-secondary, #f0f0f0);
  border-radius: 8px;
  font-size: 0.875rem;
}

.source-label {
  font-weight: 600;
  color: var(--color-text-primary, #333);
  margin-right: 8px;
}

.source-author {
  color: var(--color-text-primary, #333);
}

.source-author::after {
  content: ' - ';
}

.source-name {
  color: var(--color-text-secondary, #666);
}

.source-page {
  color: var(--color-text-muted, #999);
  margin-left: 8px;
}

.recipe-image {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: 12px;
  margin-bottom: 24px;
  background: var(--color-bg-secondary, #f0f0f0);
}

.recipe-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.servings-control {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
  padding: 12px 16px;
  background: var(--color-bg-card, #fff);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.servings-label {
  font-weight: 600;
  color: var(--color-text-primary, #333);
}

.servings-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
  font-size: 1.25rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
}

.servings-btn:hover:not(:disabled) {
  background: var(--color-border, #ddd);
}

.servings-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.servings-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-primary, #4a5568);
  min-width: 32px;
  text-align: center;
}

.recipe-section {
  margin-bottom: 32px;
}

.recipe-section h2 {
  font-size: 1.5rem;
  color: var(--color-text-primary, #333);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--color-border, #ddd);
}

.ingredients-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ingredients-list li {
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light, #eee);
  display: flex;
  gap: 8px;
}

.ingredients-list li:last-child {
  border-bottom: none;
}

.ingredient-amount {
  font-weight: 600;
  color: var(--color-primary, #4a5568);
  min-width: 100px;
}

.ingredient-name {
  color: var(--color-text-primary, #333);
}

.instructions-list {
  padding-left: 24px;
}

.instructions-list li {
  padding: 12px 0;
  line-height: 1.6;
  color: var(--color-text-primary, #333);
}

.instructions-list li::marker {
  color: var(--color-primary, #4a5568);
  font-weight: 600;
}

.section-header {
  display: flex;
  align-items: flex-end;
  margin-bottom: 16px;
  border-bottom: 2px solid var(--color-border, #ddd);
}

.section-header h2 {
  font-size: 1.5rem;
  color: var(--color-text-primary, #333);
  margin: 0;
  padding-bottom: 8px;
  border-bottom: none;
}

.tab-toggle {
  display: flex;
  gap: 0;
}

.tab-btn {
  padding: 0 20px 10px 0;
  border: none;
  border-bottom: 3px solid transparent;
  background: none;
  color: var(--color-text-muted, #a0aec0);
  font-size: 1.5rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease;
  margin-bottom: -2px;
  font-family: inherit;
}

.tab-btn.active {
  color: var(--color-text-primary, #333);
  border-bottom-color: var(--color-primary, #4a5568);
}

.tab-btn:not(.active):hover {
  color: var(--color-text-secondary, #4a5568);
}

.recipe-tools {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.btn-tool {
  padding: 6px 12px;
  background: var(--color-bg-card, #fff);
  color: var(--color-primary, #4a5568);
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-tool:hover {
  background: var(--color-bg-secondary, #f0f0f0);
}

.print-footer {
  display: none;
}

@media print {
  @page {
    margin: 18mm 16mm;
  }

  :global(.navbar) {
    display: none !important;
  }

  .recipe-detail {
    max-width: none;
    padding: 0;
  }

  .recipe-tools,
  .detail-actions,
  .servings-control,
  .tab-btn:not(.active) {
    display: none !important;
  }

  .recipe-image {
    max-height: 240px;
  }

  .recipe-section,
  .ingredients-list li,
  .instructions-list li {
    break-inside: avoid;
  }

  .tab-btn.active {
    padding-right: 0;
    border-bottom-color: transparent;
    color: var(--color-text-primary, #333);
  }

  .print-footer {
    display: block;
    margin-top: 32px;
    padding-top: 12px;
    border-top: 1px solid var(--color-border, #ddd);
    text-align: center;
    font-size: 0.875rem;
    color: var(--color-text-secondary, #666);
    break-inside: avoid;
  }

  .print-footer__small {
    font-size: 0.75rem;
    color: var(--color-text-muted, #999);
  }
}
</style>
