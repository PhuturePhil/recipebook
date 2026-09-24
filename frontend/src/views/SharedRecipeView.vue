<template>
  <div class="shared-recipe">
    <div v-if="loading" class="loading">Lädt...</div>

    <div v-else-if="errorMessage" class="not-found">
      <p>{{ errorMessage }}</p>
    </div>

    <div v-else-if="recipe" class="recipe-content">
      <header class="recipe-header">
        <h1>{{ recipe.title }}</h1>
        <p v-if="recipe.attribution" class="recipe-attribution">{{ recipe.attribution }}</p>
      </header>

      <div v-if="!authStore.isAuthenticated" class="guest-hint">
        <p>
          Schön, dass du vorbeischaust! Dieses Rezept wurde mit dir aus dem Familienkochbuch
          geteilt. Beschreibung und persönliche Notizen sind registrierten Nutzern vorbehalten.
        </p>
        <p>
          Die Registrierung ist nur mit Einladung möglich — frag einfach die Person, die dir
          den Link geschickt hat.
        </p>
        <router-link to="/login" class="guest-hint__link">Registriert? Hier anmelden</router-link>
      </div>

      <div class="servings-control">
        <span class="servings-label">Personen:</span>
        <button class="servings-btn" @click="decreaseServings" :disabled="currentServings <= 1">-</button>
        <span class="servings-value">{{ currentServings }}</span>
        <button class="servings-btn" @click="increaseServings">+</button>
      </div>

      <section v-if="recipe.ingredients?.length" class="recipe-section">
        <h2>Zutaten</h2>
        <ul class="ingredients-list">
          <li v-for="(ingredient, index) in scaledIngredients" :key="index">
            <span class="ingredient-amount">{{ ingredient.amount }} {{ ingredient.unit }}</span>
            <span class="ingredient-name">{{ ingredient.name }}</span>
          </li>
        </ul>
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
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { shareService } from '@/services/shareService'
import { scaleIngredients } from '@/utils/scaleIngredients'

const route = useRoute()
const authStore = useAuthStore()

const recipe = ref(null)
const loading = ref(true)
const errorMessage = ref(null)
const currentServings = ref(1)

const originalTitle = document.title
let robotsMeta = null

onMounted(async () => {
  robotsMeta = document.createElement('meta')
  robotsMeta.name = 'robots'
  robotsMeta.content = 'noindex, nofollow'
  document.head.appendChild(robotsMeta)

  try {
    recipe.value = await shareService.getSharedRecipe(route.params.token)
    currentServings.value = recipe.value.baseServings || 1
    document.title = `${recipe.value.title} – Pastoors Familienrezepte`
  } catch (error) {
    errorMessage.value = error.status === 410
      ? 'Dieser Link ist abgelaufen oder wurde zurückgezogen.'
      : 'Dieser Link ist ungültig.'
  }
  loading.value = false
})

onUnmounted(() => {
  robotsMeta?.remove()
  document.title = originalTitle
})

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
</script>

<style scoped>
.shared-recipe {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.loading,
.not-found {
  text-align: center;
  padding: 48px 24px;
  color: var(--color-text-secondary, #666);
}

.recipe-header {
  margin-bottom: 24px;
}

.recipe-header h1 {
  margin: 0;
  font-size: 2rem;
  color: var(--color-text-primary, #333);
}

.recipe-attribution {
  margin-top: 4px;
  font-size: 0.95rem;
  color: var(--color-text-secondary, #666);
  font-style: italic;
}

.guest-hint {
  margin-bottom: 24px;
  padding: 16px;
  background: #ebf4ff;
  border: 1px solid #bee3f8;
  border-radius: 8px;
  font-size: 0.925rem;
  color: var(--color-text-primary, #333);
}

.guest-hint p + p {
  margin-top: 8px;
}

.guest-hint__link {
  display: inline-block;
  margin-top: 12px;
  color: var(--color-primary, #4a5568);
  font-weight: 600;
  text-underline-offset: 3px;
}

.guest-hint__link:hover {
  color: var(--color-primary-dark, #2d3748);
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
</style>
