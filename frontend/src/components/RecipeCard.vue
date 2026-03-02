<template>
  <div class="recipe-card" @click="navigateToDetail">
    <div v-if="recipe.imageUrl" class="recipe-card__image">
      <img :src="recipe.imageUrl" :alt="recipe.title" loading="lazy" />
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
import { useRouter } from 'vue-router'

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

const badgeKey = (badge) => badge.toLowerCase().replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue').replace(/ß/g, 'ss').replace(/\s+/g, '-')

const router = useRouter()



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
  background: var(--color-bg-card, #fff);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
  min-height: 320px;
}

.recipe-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
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
  font-size: 1.25rem;
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

.badge--schnell { background: #c6f6d5; color: #22543d; }
.badge--proteinreich { background: #bee3f8; color: #2a4365; }
.badge--kalorienarm { background: #f0fff4; color: #276749; border: 1px solid #9ae6b4; }
.badge--fettarm { background: #fefcbf; color: #744210; }
.badge--ballaststoffreich { background: #e9d8fd; color: #44337a; }
</style>
