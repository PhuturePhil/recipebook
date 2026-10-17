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
      <button type="button" class="ui-btn" :aria-expanded="showHint" @click="showHint = !showHint">Konto erstellen</button>
    </div>

    <!-- Bis es eine offene Registrierung gibt, entstehen Konten nur über Einladungslinks -->
    <div v-if="showHint" class="gast-hint">
      <div class="ui-karte" role="status">
        <p>Konten gibt es zurzeit nur mit Einladung – frag jemanden aus deiner Familie nach dem Einladungslink.</p>
      </div>
    </div>

    <div v-if="recipes.length" class="gast-home__list">
      <p class="section-label">Öffentliche Rezepte</p>
      <div class="recipe-grid">
        <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" :show-tags="false" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { recipeService } from '@/services/recipeService'
import RecipeCard from '@/components/RecipeCard.vue'

const recipes = ref([])
const showHint = ref(false)

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
