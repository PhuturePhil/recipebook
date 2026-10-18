<template>
  <!-- Gast-Startseite (Mockup plans/rezepte-familien/06-gast-start.html): Hero, Anmelden/Konto erstellen,
       die bestbewerteten öffentlichen Rezepte (nur, wenn es welche gibt), darunter die Vorteile eines Kontos.
       Keine Tab-Leiste. -->
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

    <section v-if="recipes.length" class="gast-home__list" aria-labelledby="gast-top-title">
      <h2 id="gast-top-title" class="section-label">Beliebte Rezepte</h2>
      <div class="recipe-grid">
        <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" :show-tags="false" />
      </div>
    </section>

    <section class="gast-signup" aria-labelledby="gast-signup-title">
      <h2 id="gast-signup-title">Mit eigenem Konto kochst du mit</h2>
      <ul class="gast-signup__list">
        <li v-for="b in BENEFITS" :key="b.text">
          <span class="gast-signup__icon" aria-hidden="true"><AppIcon :name="b.icon" :size="18" /></span>
          <span><strong>{{ b.title }}</strong> {{ b.text }}</span>
        </li>
      </ul>
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

const BENEFITS = [
  { icon: 'buch', title: 'Eigene Rezepte sammeln', text: '– alle Lieblingsgerichte an einem Ort.' },
  { icon: 'familie', title: 'Mit der Familie teilen', text: '– gemeinsam ein Familienkochbuch führen.' },
  { icon: 'beleg', title: 'Foto-Import mit KI', text: '– Rezept aus dem Kochbuch abfotografieren, die KI tippt es ab.' },
  { icon: 'karotte', title: 'Nährwerte', text: '– Kalorien und Nährstoffe pro Portion, automatisch berechnet.' },
]

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

.gast-signup {
  margin: 28px 16px 8px;
  padding: 22px 20px 18px;
  border-radius: var(--radius-karte);
  border: 1px solid var(--linie);
  background: var(--flaeche);
  box-shadow: var(--schatten-karte);
}

@media (min-width: 640px) {
  .gast-signup {
    max-width: 560px;
    margin-left: auto;
    margin-right: auto;
  }
}

.gast-signup h2 {
  margin: 0 0 14px;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text);
}

.gast-signup__list {
  list-style: none;
  margin: 0 0 18px;
  padding: 0;
  display: grid;
  gap: 12px;
}

.gast-signup__list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 0.95rem;
  line-height: 1.45;
  color: var(--text2);
}

.gast-signup__list strong {
  color: var(--text);
  font-weight: 650;
}

.gast-signup__icon {
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
