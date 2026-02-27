<template>
  <div class="recipe-detail">
    <div v-if="store.loading" class="loading">Lädt...</div>

    <div v-else-if="store.error" class="error">{{ store.error }}</div>

    <div v-else-if="recipe" class="recipe-content">
      <header class="recipe-header">
        <h1>{{ recipe.title }}</h1>
        <div class="recipe-actions">
          <router-link :to="`/recipe/${recipe.id}/edit`" class="btn-edit">
            Bearbeiten
          </router-link>
          <button class="btn-delete" @click="handleDelete">Löschen</button>
        </div>
      </header>

      <div class="recipe-image">
        <img :src="recipe.imageUrl || getFoodImage(recipe.id)" :alt="recipe.title" />
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
          <h2>Zutaten</h2>
          <div v-if="recipe.nutritionKcal != null" class="tab-toggle">
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

        <table v-if="activeTab === 'nutrition'" class="nutrition-table">
          <thead>
            <tr>
              <th></th>
              <th>pro Portion</th>
              <th>gesamt</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Energie</td>
              <td>{{ formatNutrition(recipe.nutritionKcal / currentServings) }} kcal</td>
              <td>{{ formatNutrition(recipe.nutritionKcal) }} kcal</td>
            </tr>
            <tr>
              <td>Fett</td>
              <td>{{ formatNutrition(recipe.nutritionFat / currentServings) }} g</td>
              <td>{{ formatNutrition(recipe.nutritionFat) }} g</td>
            </tr>
            <tr>
              <td>Eiweiß</td>
              <td>{{ formatNutrition(recipe.nutritionProtein / currentServings) }} g</td>
              <td>{{ formatNutrition(recipe.nutritionProtein) }} g</td>
            </tr>
            <tr>
              <td>Kohlenhydrate</td>
              <td>{{ formatNutrition(recipe.nutritionCarbs / currentServings) }} g</td>
              <td>{{ formatNutrition(recipe.nutritionCarbs) }} g</td>
            </tr>
            <tr>
              <td>Ballaststoffe</td>
              <td>{{ formatNutrition(recipe.nutritionFiber / currentServings) }} g</td>
              <td>{{ formatNutrition(recipe.nutritionFiber) }} g</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-if="recipe.instructions?.length" class="recipe-section">
        <h2>Zubereitung</h2>
        <ol class="instructions-list">
          <li v-for="(instruction, index) in recipe.instructions" :key="index">
            {{ instruction }}
          </li>
        </ol>
      </section>
    </div>

    <div v-else class="not-found">
      <p>Rezept nicht gefunden</p>
      <router-link to="/" class="btn-back">Zurück zur Übersicht</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'

const route = useRoute()
const router = useRouter()
const store = useRecipeStore()

const getFoodImage = (id) => `https://loremflickr.com/800/400/food?random=${id}`

const recipe = computed(() => store.currentRecipe)
const currentServings = ref(1)
const activeTab = ref('ingredients')

onMounted(async () => {
  await store.fetchRecipeById(route.params.id)
  if (recipe.value) {
    currentServings.value = recipe.value.baseServings || 4
  }
})

const increaseServings = () => {
  currentServings.value++
}

const decreaseServings = () => {
  if (currentServings.value > 1) {
    currentServings.value--
  }
}

const scaledIngredients = computed(() => {
  if (!recipe.value?.ingredients) return []
  
  const baseServings = recipe.value.baseServings || 4
  
  return recipe.value.ingredients.map(ingredient => {
    const amount = parseFloat(ingredient.amount)
    if (isNaN(amount)) {
      return ingredient
    }
    const scaledAmount = (amount / baseServings) * currentServings.value
    const formattedAmount = scaledAmount % 1 === 0 ? scaledAmount : scaledAmount.toFixed(1).replace('.0', '')
    return {
      ...ingredient,
      amount: formattedAmount
    }
  })
})

const formatNutrition = (value) => {
  if (value == null) return '–'
  return Math.round(value * 10) / 10
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.recipe-header h1 {
  margin: 0;
  font-size: 2rem;
  color: var(--color-text-primary, #333);
}

.recipe-actions {
  display: flex;
  gap: 12px;
}

.btn-edit,
.btn-delete,
.btn-back {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.2s ease;
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

.btn-back {
  display: inline-block;
  margin-top: 16px;
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.btn-back:hover {
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
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--color-border, #ddd);
}

.section-header h2 {
  font-size: 1.5rem;
  color: var(--color-text-primary, #333);
  margin: 0;
  border-bottom: none;
  padding-bottom: 0;
}

.tab-toggle {
  display: flex;
  gap: 4px;
}

.tab-btn {
  padding: 6px 14px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-secondary, #666);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: var(--color-primary, #4a5568);
  color: white;
  border-color: var(--color-primary, #4a5568);
}

.nutrition-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.nutrition-table th,
.nutrition-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--color-border-light, #eee);
}

.nutrition-table th {
  font-weight: 600;
  color: var(--color-text-secondary, #666);
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.nutrition-table td:first-child {
  color: var(--color-text-primary, #333);
  font-weight: 500;
}

.nutrition-table td:not(:first-child) {
  color: var(--color-primary, #4a5568);
  font-weight: 600;
}

.nutrition-table tr:last-child td {
  border-bottom: none;
}
</style>
