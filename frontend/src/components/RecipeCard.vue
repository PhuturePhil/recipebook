<template>
  <div class="recipe-card" @click="navigateToDetail">
    <div class="recipe-card__image">
      <img :src="recipe.imageUrl || getFoodImage(recipe.id)" :alt="recipe.title" />
    </div>
    <div class="recipe-card__content">
      <h3 class="recipe-card__title">{{ recipe.title }}</h3>
      <p class="recipe-card__description">{{ recipe.description }}</p>
      <div class="recipe-card__meta">
        <span v-if="recipe.ingredients" class="recipe-card__count">
          {{ recipe.ingredients.length }} Zutaten
        </span>
        <span v-if="recipe.instructions" class="recipe-card__count">
          {{ recipe.instructions.length }} Schritte
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
  }
})

const router = useRouter()

const getFoodImage = (id) => `https://loremflickr.com/400/300/food?random=${id}`

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
</style>
