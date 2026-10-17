<template>
  <!-- Gast-Startseite (Mockup plans/rezepte-familien/06-gast-start.html): Hero, Anmelden/Konto erstellen,
       öffentliche Rezepte nur, wenn es welche gibt. Keine Tab-Leiste. -->
  <div class="gast-home">
    <div class="gast-hero">
      <span class="app-chip app-chip--icon" aria-hidden="true"><img src="/icon-v2.svg" alt="" decoding="async"></span>
      <h1>Pastoors Familienrezepte</h1>
      <p class="pitch">Rezepte sammeln und teilen — mit deiner Familie und deinen Freunden.</p>
    </div>

    <div class="gast-cta">
      <RouterLink to="/login" class="ui-btn ui-btn--primaer">Anmelden</RouterLink>
      <RouterLink to="/registrieren" class="ui-btn">Konto erstellen</RouterLink>
    </div>

    <div v-if="recipes.length" class="gast-home__list">
      <p class="section-label">Öffentliche Rezepte</p>
      <div class="recipe-grid">
        <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" :show-tags="false" />
      </div>
    </div>

    <LegalLinks />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { recipeService } from '@/services/recipeService'
import RecipeCard from '@/components/RecipeCard.vue'
import LegalLinks from '@/components/LegalLinks.vue'

const recipes = ref([])

onMounted(async () => {
  try {
    // Der Server liefert Gästen nur öffentliche Rezepte
    recipes.value = await recipeService.getAll()
  } catch {
    recipes.value = []
  }
})
</script>

<style scoped>
.recipe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.section-label { margin-top: 4px; }
</style>
