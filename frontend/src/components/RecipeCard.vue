<template>
  <div class="recipe-card" @click="navigateToDetail">
    <div v-if="recipe.imageUrl" ref="imageBox" class="recipe-card__image">
      <img v-if="imageSrc" :src="imageSrc" :alt="recipe.title" loading="lazy" decoding="async" />
    </div>
    <div class="recipe-card__content">
      <h3 class="recipe-card__title">{{ recipe.title }}</h3>
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
      </div>
      <div v-if="recipe.tags?.length" class="recipe-card__tags">
        <button
          v-for="tag in recipe.tags"
          :key="tag"
          type="button"
          class="recipe-card__tag"
          :title="`Nach „${tag}“ suchen`"
          @click.stop="searchTag(tag)"
        >#{{ tag }}</button>
      </div>
      <div v-if="badges && badges.length" class="recipe-card__badges">
        <span v-for="badge in badges" :key="badge" :class="['badge', `badge--${badgeKey(badge)}`]">
          {{ badge }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRecipeImage } from '@/composables/useRecipeImage'
import { useRecipeStore } from '@/stores/recipeStore'
import { tagSearchTerm } from '@/utils/recipeTags'

const props = defineProps({
  recipe: {
    type: Object,
    required: true
  },
  badges: {
    type: Array,
    default: () => []
  }
})

const imageBox = ref(null)
const imageSrc = useRecipeImage(() => props.recipe.imageUrl, imageBox)

const kcalPerServing = computed(() => {
  const kcal = props.recipe.nutrition?.perServing?.kcal
  return kcal == null ? null : Math.round(kcal).toLocaleString('de-DE')
})

const badgeKey = (badge) => badge.toLowerCase().replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue').replace(/ß/g, 'ss').replace(/\s+/g, '-')

const router = useRouter()
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

const navigateToDetail = () => {
  router.push(`/recipe/${props.recipe.id}`)
}
</script>

<style scoped>
.recipe-card {
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
  gap: 16px;
  font-size: 0.875rem;
  color: var(--color-text-muted, #999);
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
