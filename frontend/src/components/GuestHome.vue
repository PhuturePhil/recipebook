<template>
  <!-- Gast-Startseite (Mockup plans/rezepte-familien/06-gast-start.html): Hero, „Was ist das hier?“ mit den vier Vorteilen,
       Anmelden/Konto erstellen, die bestbewerteten öffentlichen Rezepte (nur, wenn es welche gibt), darunter die
       Konto-Karte. Keine Tab-Leiste. -->
  <div class="gast-home">
    <div class="gast-hero">
      <span class="app-chip app-chip--icon" aria-hidden="true"><img src="/icon-v2.svg" alt="" decoding="async"></span>
      <h1>Pastoors Familienrezepte</h1>
    </div>

    <section class="gast-manifest" aria-labelledby="gast-manifest-title">
      <h2 id="gast-manifest-title">Was ist das hier?</h2>
      <p class="gast-manifest__intro">{{ MANIFEST_INTRO }}</p>
      <ul class="gast-manifest__list">
        <li v-for="p in MANIFEST_POINTS" :key="p.title">
          <span class="gast-icon" aria-hidden="true"><AppIcon :name="p.icon" :size="18" /></span>
          <span><strong>{{ p.title }}</strong> {{ p.text }}</span>
        </li>
      </ul>
    </section>

    <div class="gast-cta">
      <RouterLink to="/login" class="ui-btn ui-btn--primaer">Anmelden</RouterLink>
      <RouterLink to="/registrieren" class="ui-btn">Konto erstellen</RouterLink>
    </div>

    <section v-if="recipes.length" class="gast-home__list" aria-labelledby="gast-top-title">
      <h2 id="gast-top-title" class="section-label">Beliebte Rezepte</h2>
      <div class="recipe-grid">
        <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" :show-tags="false" />
      </div>
    </section>

    <section class="gast-signup" aria-labelledby="gast-signup-title">
      <h2 id="gast-signup-title">Mit eigenem Konto kochst du mit</h2>
      <p class="gast-signup__text">Leg dein eigenes Kochbuch an und lade die Menschen ein, mit denen du kochst.</p>
      <RouterLink to="/registrieren" class="ui-btn ui-btn--primaer gast-signup__btn">Konto erstellen</RouterLink>
      <p class="gast-signup__login">Schon dabei? <RouterLink to="/login">Anmelden</RouterLink></p>
    </section>

    <LegalLinks />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { recipeService } from '@/services/recipeService'
import RecipeCard from '@/components/RecipeCard.vue'
import LegalLinks from '@/components/LegalLinks.vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import { MANIFEST_INTRO, MANIFEST_POINTS } from '@/data/guestManifest'

const recipes = ref([])

onMounted(async () => {
  try {
    recipes.value = await recipeService.getTopPublic()
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

.gast-manifest {
  margin: 22px 16px 0;
  padding: 22px 20px 20px;
  border-radius: var(--radius-karte);
  border: 1px solid var(--linie);
  background: var(--flaeche);
  box-shadow: var(--schatten-karte);
}

.gast-manifest h2 {
  margin: 0 0 10px;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text);
}

.gast-manifest__intro {
  margin: 0 0 18px;
  font-size: 0.98rem;
  line-height: 1.55;
  color: var(--text);
  hyphens: auto;
}

.gast-manifest__list {
  list-style: none;
  margin: 0;
  padding: 16px 0 0;
  border-top: 1px solid var(--linie);
  display: grid;
  gap: 14px;
}

.gast-manifest__list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 0.95rem;
  line-height: 1.45;
  color: var(--text2);
}

.gast-manifest__list strong {
  color: var(--text);
  font-weight: 650;
}

.gast-icon {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--akzent-weich);
  color: var(--akzent-text);
}

.gast-signup {
  margin: 28px 16px 8px;
  padding: 22px 20px 18px;
  border-radius: var(--radius-karte);
  border: 1px solid var(--linie);
  background: var(--flaeche);
  box-shadow: var(--schatten-karte);
}

@media (min-width: 640px) {
  .gast-manifest,
  .gast-signup {
    max-width: 560px;
    margin-left: auto;
    margin-right: auto;
  }
}

.gast-signup h2 {
  margin: 0 0 8px;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text);
}

.gast-signup__text {
  margin: 0 0 16px;
  font-size: 0.95rem;
  line-height: 1.45;
  color: var(--text2);
}

.gast-signup__btn {
  width: 100%;
  text-decoration: none;
}

.gast-signup__login {
  margin: 12px 0 0;
  text-align: center;
  font-size: 0.9rem;
  color: var(--text2);
}

.gast-signup__login a {
  color: var(--akzent-text);
  font-weight: 600;
}
</style>
