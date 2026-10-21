<template>
  <!-- Hero-Karte: volles Foto (ohne Foto warmer Verlauf mit Topf), Text weiß im Bild. Ganze Karte klickbar über den
       Titel-Link (::after deckt die Karte ab) – echtes <a href>, damit Mittelklick und „In neuem Tab öffnen“ gehen;
       Bewertung und Tags liegen als eigene Ziele darüber -->
  <div :class="['recipe-card', { 'recipe-card--ph': !recipe.imageUrl }]">
    <div ref="imageBox" class="recipe-card__media">
      <img v-if="imageSrc" :src="imageSrc" alt="" loading="lazy" decoding="async" />
      <HeroIcon v-else-if="!recipe.imageUrl" name="topf" :size="64" class="recipe-card__pot" />
    </div>
    <div class="recipe-card__scrim"></div>
    <p v-if="showOrigin || visHint" class="recipe-card__origin">
      <span v-if="showOrigin" class="recipe-card__owner">
        <span :class="['mini-avatar', avatarTone(recipe.ownerId)]">{{ ownerInitials }}</span>von {{ recipe.createdByFirstName }}
      </span>
      <span v-if="visHint" class="recipe-card__vis"><AppIcon :name="visHint.icon" :size="12" />{{ visHint.text }}</span>
    </p>
    <div class="recipe-card__content">
      <h3 class="recipe-card__title">
        <RouterLink :to="recipePath(recipe.id)" class="recipe-card__link">{{ recipe.title }}</RouterLink>
        <span v-if="source" class="recipe-card__by">{{ ` (${source})` }}</span>
      </h3>
      <div v-if="meta || recipe.ratingCount" class="recipe-card__meta">
        <span v-if="meta">{{ meta }}</span>
        <template v-if="recipe.ratingCount">
          <span v-if="meta" aria-hidden="true">·</span>
          <!-- Tipp auf die Bewertung: Rezept öffnen und direkt zum Abschnitt „Bewertungen“ -->
          <RouterLink
            :to="ratingsRoute(recipe.id)"
            class="recipe-card__rating"
            :title="`Durchschnitt aus ${ratingCountLabel(recipe.ratingCount)} – zu den Bewertungen`"
            :aria-label="`${ratingLabel(recipe.ratingAvg, recipe.ratingCount)}, zu den Bewertungen`"
          ><HeroIcon name="stern" :size="12" />{{ formatRating(recipe.ratingAvg) }}&nbsp;<i>({{ recipe.ratingCount }})</i></RouterLink>
        </template>
      </div>
      <div v-if="tagRow.items.length" class="recipe-card__tags">
        <template v-for="item in tagRow.items" :key="`${item.kind}-${item.name}`">
          <span v-if="item.kind === 'badge'" class="recipe-card__badge">
            <HeroIcon :name="badgeIcon(item.name)" :size="11" />{{ item.name }}
          </span>
          <button
            v-else
            type="button"
            class="recipe-card__tag"
            :title="`Nach „${item.name}“ suchen`"
            @click="searchTag(item.name)"
          >#{{ item.name }}</button>
        </template>
        <span v-if="tagRow.more" class="recipe-card__more">+{{ tagRow.more }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRecipeImage } from '@/composables/useRecipeImage'
import { useRecipeStore } from '@/stores/recipeStore'
import { tagSearchTerm } from '@/utils/recipeTags'
import { formatRating, ratingLabel, ratingCountLabel, ratingsRoute } from '@/utils/ratings'
import { badgeIcon, cardMeta, cardTagRow, sourceLabel } from '@/utils/recipeHero'
import { recipePath } from '@/router/paths.js'
import { useAuthStore } from '@/stores/authStore'
import { avatarTone } from '@/stores/familyStore'
import { visibilityLabel, initials } from '@/utils/visibility'
import AppIcon from '@/components/shell/AppIcon.vue'
import HeroIcon from '@/components/HeroIcon.vue'

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

const source = computed(() => sourceLabel(props.recipe))
const meta = computed(() => cardMeta(props.recipe))
const tagRow = computed(() => cardTagRow(props.badges, props.showTags ? props.recipe.tags : []))

// Herkunft nur bei fremden Rezepten; Sichtbarkeit nur für Angemeldete (Gäste sehen ohnehin nur Öffentliches)
const authStore = useAuthStore()
const showOrigin = computed(() =>
  !!props.recipe.createdByFirstName && props.recipe.ownerId !== authStore.user?.id
)
const ownerInitials = computed(() => initials(props.recipe.createdBy || props.recipe.createdByFirstName))
const visHint = computed(() => (authStore.isAuthenticated ? visibilityLabel(props.recipe) : null))

const store = useRecipeStore()

const searchTag = (tag) => {
  store.addSearchTerm(tagSearchTerm(tag))
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
.recipe-card {
  position: relative;
  height: 232px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  isolation: isolate;
  border-radius: 20px;
  overflow: hidden;
  background: var(--flaeche2);
  box-shadow: 0 1px 2px rgba(0, 0, 0, .06);
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

[data-theme="dark"] .recipe-card {
  box-shadow: 0 1px 2px rgba(0, 0, 0, .4);
}

.recipe-card--ph {
  background: var(--h-placeholder);
}

@media (hover: hover) {
  .recipe-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--bar-schatten);
  }
}

.recipe-card:has(.recipe-card__link:focus-visible) {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}

/* Foto und Verlauf hinter dem Text (z-index -1 in der isolierten Karte); der Text bleibt im Fluss und damit ohne
   eigenen Positionsrahmen, sodass das ::after des Titel-Links die ganze Karte abdeckt */
.recipe-card__media {
  position: absolute;
  inset: 0;
  z-index: -1;
}

.recipe-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.recipe-card__pot {
  position: absolute;
  top: 36px;
  left: 50%;
  transform: translateX(-50%);
  color: #fff;
  opacity: .45;
}

.recipe-card__scrim {
  position: absolute;
  inset: 0;
  z-index: -1;
  background: linear-gradient(180deg, rgba(0, 0, 0, .05) 30%, rgba(0, 0, 0, .78) 100%);
  pointer-events: none;
}

.recipe-card__origin {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  pointer-events: none;
}

.recipe-card__owner,
.recipe-card__vis {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 100%;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--h-glass);
  color: rgba(255, 255, 255, .9);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recipe-card__owner {
  padding-left: 3px;
}

.recipe-card__owner .mini-avatar {
  width: 17px;
  height: 17px;
  font-size: 8px;
}

.recipe-card__content {
  padding: 0 14px 12px;
  color: #fff;
}

.recipe-card__title {
  margin: 0;
  font-size: 18px;
  font-weight: 750;
  line-height: 1.25;
  text-shadow: 0 1px 8px rgba(0, 0, 0, .35);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
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

.recipe-card__by {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, .78);
}

.recipe-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0 6px;
  margin-top: 4px;
  font-size: 12.5px;
  font-weight: 600;
  color: rgba(255, 255, 255, .92);
}

.recipe-card__rating {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-weight: 750;
  color: var(--h-star);
  text-decoration: none;
}

.recipe-card__rating i {
  font-style: normal;
  font-weight: 500;
  color: rgba(255, 255, 255, .8);
}

.recipe-card__rating:hover {
  text-decoration: underline;
}

.recipe-card__tags {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 3px;
  font-size: 12px;
  font-weight: 650;
  color: rgba(255, 255, 255, .88);
  white-space: nowrap;
  overflow: hidden;
}

.recipe-card__badge,
.recipe-card__more {
  flex: none;
}

.recipe-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 3.5px;
  font-weight: 750;
}

.recipe-card__tag {
  position: relative;
  z-index: 2;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
}

.recipe-card__tag:hover {
  text-decoration: underline;
}

.recipe-card__more {
  color: rgba(255, 255, 255, .6);
}
</style>
