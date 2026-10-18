<template>
  <!-- Ganze Karte klickbar über den Titel-Link (::after deckt die Karte ab) – echtes <a href>, damit Mittelklick und
       „In neuem Tab öffnen“ gehen; Bewertung und Tags liegen als eigene Ziele darüber -->
  <div class="recipe-card">
    <div v-if="recipe.imageUrl" ref="imageBox" class="recipe-card__image">
      <img v-if="imageSrc" :src="imageSrc" :alt="recipe.title" loading="lazy" decoding="async" />
    </div>
    <div class="recipe-card__content">
      <h3 class="recipe-card__title">
        <RouterLink :to="recipePath(recipe.id)" class="recipe-card__link">{{ recipe.title }}</RouterLink>
      </h3>
      <p class="recipe-card__description">{{ recipe.description }}</p>
      <div class="recipe-card__meta">
        <span v-if="recipe.prepTimeMinutes" class="recipe-card__count">
          {{ formatPrepTime(recipe.prepTimeMinutes) }}
        </span>
        <span v-if="recipe.ingredientCount" class="recipe-card__count">
          {{ recipe.ingredientCount }} Zutaten
        </span>
        <span
          v-if="kcalPerServing != null"
          class="recipe-card__count"
          :title="recipe.nutrition.complete ? 'pro Portion' : 'pro Portion, unvollständig berechnet'"
        >
          {{ recipe.nutrition.complete ? '' : 'mind. ' }}{{ kcalPerServing }} kcal
        </span>
        <!-- Tipp auf die Bewertung: Rezept öffnen und direkt zum Abschnitt „Bewertungen“ -->
        <RouterLink
          v-if="recipe.ratingCount"
          :to="ratingsRoute(recipe.id)"
          class="recipe-card__count recipe-card__rating"
          :title="`Durchschnitt aus ${ratingCountLabel(recipe.ratingCount)} – zu den Bewertungen`"
          :aria-label="`${ratingLabel(recipe.ratingAvg, recipe.ratingCount)}, zu den Bewertungen`"
        >
          <RatingStars :model-value="1" :count="1" :size="13" readonly aria-hidden="true" />
          {{ ratingLabel(recipe.ratingAvg, recipe.ratingCount) }}
        </RouterLink>
      </div>
      <div v-if="showTags && recipe.tags?.length" class="recipe-card__tags">
        <button
          v-for="tag in recipe.tags"
          :key="tag"
          type="button"
          class="recipe-card__tag"
          :title="`Nach „${tag}“ suchen`"
          @click="searchTag(tag)"
        >#{{ tag }}</button>
      </div>
      <div v-if="badges && badges.length" class="recipe-card__badges">
        <span v-for="badge in badges" :key="badge" :class="['badge', `badge--${badgeKey(badge)}`]">
          {{ badge }}
        </span>
      </div>
      <p v-if="showOrigin || visHint" class="recipe-card__created">
        <span v-if="showOrigin" class="origin-chip">
          <span :class="['mini-avatar', avatarTone(recipe.ownerId)]">{{ ownerInitials }}</span>von {{ recipe.createdByFirstName }}
        </span>
        <span v-if="visHint" class="vis-hint"><AppIcon :name="visHint.icon" :size="13" />{{ visHint.text }}</span>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRecipeImage } from '@/composables/useRecipeImage'
import { useRecipeStore } from '@/stores/recipeStore'
import { tagSearchTerm } from '@/utils/recipeTags'
import { ratingLabel, ratingCountLabel, ratingsRoute } from '@/utils/ratings'
import { recipePath } from '@/router/paths.js'
import { useAuthStore } from '@/stores/authStore'
import { avatarTone } from '@/stores/familyStore'
import { visibilityLabel, initials } from '@/utils/visibility'
import RatingStars from '@/components/RatingStars.vue'
import AppIcon from '@/components/shell/AppIcon.vue'

const props = defineProps({
  recipe: {
    type: Object,
    required: true
  },
  badges: {
    type: Array,
    default: () => []
  },
  // Gast-Startseite: Tags führen dort in keine Suche
  showTags: {
    type: Boolean,
    default: true
  }
})

const imageBox = ref(null)
const imageSrc = useRecipeImage(() => props.recipe.imageUrl, imageBox)

const kcalPerServing = computed(() => {
  const kcal = props.recipe.nutrition?.perServing?.kcal
  return kcal == null ? null : Math.round(kcal).toLocaleString('de-DE')
})

// Herkunft nur bei fremden Rezepten; Sichtbarkeit nur für Angemeldete (Gäste sehen ohnehin nur Öffentliches)
const authStore = useAuthStore()
const showOrigin = computed(() =>
  !!props.recipe.createdByFirstName && props.recipe.ownerId !== authStore.user?.id
)
const ownerInitials = computed(() => initials(props.recipe.createdBy || props.recipe.createdByFirstName))
const visHint = computed(() => (authStore.isAuthenticated ? visibilityLabel(props.recipe) : null))

const badgeKey = (badge) => badge.toLowerCase().replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue').replace(/ß/g, 'ss').replace(/\s+/g, '-')

const store = useRecipeStore()

const searchTag = (tag) => {
  store.addSearchTerm(tagSearchTerm(tag))
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const formatPrepTime = (minutes) => {
  if (!minutes) return ''
  if (minutes < 60) return `${minutes} Min.`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h} Std. ${m} Min.` : `${h} Std.`
}
</script>

<style scoped>
.recipe-card {
  position: relative;
  background: var(--flaeche);
  border: 1px solid var(--linie);
  border-radius: var(--radius-karte);
  overflow: hidden;
  box-shadow: var(--schatten-karte);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
  min-height: 320px;
}

.recipe-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--bar-schatten);
}

.recipe-card:has(.recipe-card__link:focus-visible) {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}

.recipe-card__link {
  color: inherit;
  text-decoration: none;
}

.recipe-card__link:focus-visible {
  outline: none;
}

.recipe-card__link::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
}

.recipe-card__rating,
.recipe-card__tag {
  position: relative;
  z-index: 2;
}

.recipe-card__image {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: var(--color-bg-secondary, #f0f0f0);
}

.recipe-card__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recipe-card__content {
  padding: var(--spacing-md, 16px);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.recipe-card__title {
  font-size: 1.12rem;
  font-weight: 650;
  line-height: 1.3;
  color: var(--color-text-primary, #333);
  margin: 0 0 8px 0;
}

.recipe-card__description {
  color: var(--color-text-secondary, #666);
  margin: 0 0 12px 0;
  line-height: 1.5;
  flex: 1;
}

.recipe-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  font-size: 0.875rem;
  color: var(--color-text-muted, #999);
}

.recipe-card__rating {
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  color: var(--color-text-secondary, #666);
  cursor: pointer;
}

.recipe-card__rating:hover {
  color: var(--color-text-primary, #333);
  text-decoration: underline;
}

.recipe-card__created {
  margin: 10px 0 0;
  font-size: 0.78rem;
  color: var(--text3);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
}

.recipe-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  margin-top: 10px;
}

.recipe-card__tag {
  padding: 2px 0;
  border: none;
  background: none;
  font: inherit;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--akzent-text);
  cursor: pointer;
}

.recipe-card__tag:hover {
  color: var(--color-primary, #4a5568);
  text-decoration: underline;
}

.recipe-card__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.badge {
  font-size: 0.7rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
  letter-spacing: 0.02em;
}

.badge--schnell { background: var(--pos-weich); color: var(--pos); }
.badge--proteinreich { background: var(--info-weich); color: var(--info-text); }
.badge--energiearm { background: var(--pos-weich); color: var(--pos); border: 1px solid var(--pos-linie); }
.badge--fettarm { background: var(--warn-bg); color: var(--warn-text); }
.badge--ballaststoffreich { background: var(--lila-weich); color: var(--lila-text); }
</style>
