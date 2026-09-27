<template>
  <div class="shared-recipe">
    <div v-if="loading" class="loading">Lädt...</div>

    <div v-else-if="errorMessage" class="not-found">
      <p>{{ errorMessage }}</p>
    </div>

    <div v-else-if="recipe" class="recipe-content">
      <header class="recipe-header">
        <h1>{{ recipe.title }}</h1>
        <TranslationBanner v-if="banner" :banner="banner" @show-original="setLanguage('original')" />
        <p v-if="recipe.attribution || sourceDomain(recipe.sourceUrl)" class="recipe-attribution">
          <span v-if="recipe.attribution">{{ recipe.attribution }}</span>
          <SourceLink :href="recipe.sourceUrl" :class="{ 'source-url': recipe.attribution }" />
        </p>
        <p v-if="recipe.tags?.length" class="recipe-tags">
          <span v-for="tag in recipe.tags" :key="tag" class="recipe-tag">#{{ tag }}</span>
        </p>
        <div v-if="recipe.sourceLanguage === 'en'" class="language-toggle" role="group" aria-label="Sprache des Rezepts">
          <button
            type="button"
            :class="['lang-btn', { active: languagePref === 'de' }]"
            :aria-pressed="languagePref === 'de'"
            @click="setLanguage('de')"
          >Deutsch</button>
          <button
            type="button"
            :class="['lang-btn', { active: languagePref === 'original' }]"
            :aria-pressed="languagePref === 'original'"
            @click="setLanguage('original')"
          >Original</button>
        </div>
        <p v-if="translationNote" class="translation-note">{{ translationNote }}</p>
        <KeepScreenOnToggle class="screen-toggle-spacing" />
      </header>

      <figure v-if="imageSrc" class="recipe-figure">
        <div class="recipe-image">
          <img :src="imageSrc" :alt="recipe.title" />
        </div>
        <ImageCredit :credit="recipe.imageCredit" />
      </figure>

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
          <template v-for="(section, sIndex) in ingredientSections" :key="sIndex">
            <li v-if="section.group" class="ingredient-group-title">{{ section.group }}</li>
            <li v-else-if="sIndex > 0" class="ingredient-group-gap" aria-hidden="true"></li>
            <li v-for="(ingredient, index) in section.items" :key="`${sIndex}-${index}`">
              <span class="ingredient-amount">{{ ingredient.amount }} {{ ingredient.unit }}</span>
              <span class="ingredient-name">{{ ingredient.name }}</span>
            </li>
          </template>
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
import { groupSections } from '@/utils/ingredientGroups'
import { GERMAN, sharedLanguage, writeLanguagePreference, translationBanner } from '@/utils/recipeLanguage'
import ImageCredit from '@/components/ImageCredit.vue'
import KeepScreenOnToggle from '@/components/KeepScreenOnToggle.vue'
import TranslationBanner from '@/components/TranslationBanner.vue'
import SourceLink from '@/components/SourceLink.vue'
import { sourceDomain } from '@/utils/sourceUrl'

const route = useRoute()
const authStore = useAuthStore()

const recipe = ref(null)
const loading = ref(true)
const errorMessage = ref(null)
const currentServings = ref(1)
const languagePref = ref(sharedLanguage(route.query.lang))

const translationNote = computed(() =>
  languagePref.value === GERMAN && recipe.value?.translationStatus === 'unavailable'
    ? 'Die Übersetzung ist gerade nicht verfügbar – hier steht das Original.'
    : ''
)
const banner = computed(() =>
  translationBanner(recipe.value?.sourceLanguage, languagePref.value, recipe.value?.translationStatus)
)

const fetchRecipe = () =>
  shareService.getSharedRecipe(route.params.token, languagePref.value === GERMAN ? GERMAN : null)

const setLanguage = async (value) => {
  if (value === languagePref.value) return
  languagePref.value = value
  writeLanguagePreference(value)
  loading.value = true
  try {
    const servings = currentServings.value
    recipe.value = await fetchRecipe()
    currentServings.value = servings
    document.title = `${recipe.value.title} – Pastoors Familienrezepte`
  } catch {
    // keep what is shown; the link itself still works
  }
  loading.value = false
}

const originalTitle = document.title
let robotsMeta = null

onMounted(async () => {
  robotsMeta = document.createElement('meta')
  robotsMeta.name = 'robots'
  robotsMeta.content = 'noindex, nofollow'
  document.head.appendChild(robotsMeta)

  try {
    recipe.value = await fetchRecipe()
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

const imageSrc = computed(() =>
  recipe.value?.hasImage ? shareService.getSharedImageUrl(route.params.token) : null
)

const scaledIngredients = computed(() =>
  scaleIngredients(recipe.value?.ingredients, recipe.value?.baseServings, currentServings.value)
)

const ingredientSections = computed(() => groupSections(scaledIngredients.value))
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

.language-toggle {
  display: inline-flex;
  margin-top: 8px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  overflow: hidden;
}

.lang-btn {
  padding: 6px 10px;
  border: none;
  background: transparent;
  font-size: 0.85rem;
  font-family: inherit;
  color: var(--color-text-secondary, #666);
  cursor: pointer;
}

.lang-btn + .lang-btn {
  border-left: 1px solid var(--color-border, #ddd);
}

.lang-btn.active {
  background: var(--color-primary, #4a5568);
  color: white;
}

.translation-note {
  margin: 8px 0 0;
  font-size: 0.875rem;
  color: #744210;
}

.recipe-header {
  margin-bottom: 24px;
}

.recipe-header h1 {
  margin: 0;
  font-size: 2rem;
  color: var(--color-text-primary, #333);
}

.recipe-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin: 6px 0 0;
  font-size: 0.85rem;
  color: var(--color-text-muted, #999);
}

.recipe-attribution {
  margin-top: 4px;
  font-size: 0.95rem;
  color: var(--color-text-secondary, #666);
  font-style: italic;
}

.source-url {
  margin-left: 8px;
}

.screen-toggle-spacing {
  margin-top: 12px;
}

.recipe-figure {
  margin: 0 0 24px;
}

.recipe-image {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: 12px;
  background: var(--color-bg-secondary, #f0f0f0);
}

.recipe-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.ingredients-list li.ingredient-group-title {
  display: block;
  padding: 16px 0 6px;
  font-weight: 700;
  color: var(--color-text-primary, #333);
  border-bottom: 1px solid var(--color-border, #ddd);
  break-after: avoid;
}

.ingredients-list li.ingredient-group-title:first-child {
  padding-top: 4px;
}

.ingredients-list li:has(+ .ingredient-group-title),
.ingredients-list li:has(+ .ingredient-group-gap) {
  border-bottom: none;
}

.ingredients-list li.ingredient-group-gap {
  padding: 6px 0;
  border-bottom: 1px solid var(--color-border, #ddd);
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
