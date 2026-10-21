<template>
  <div class="recipe-detail">
    <div v-if="store.loading" class="loading">Lädt...</div>

    <KitchenMishap
      v-else-if="notFound"
      class="recipe-missing"
      title="Dieses Rezept ist uns angebrannt."
      text="Es wurde gelöscht, ist nicht für dich freigegeben, oder der Link ist unvollständig."
    >
      <router-link to="/" class="ui-btn ui-btn--primaer">Zur Startseite</router-link>
    </KitchenMishap>

    <div v-else-if="store.error" class="not-found">
      <p class="error">{{ store.error }}</p>
      <router-link to="/" class="btn-cancel">Zurück zur Übersicht</router-link>
    </div>

    <div v-else-if="waitingForTranslation" class="loading">Rezept wird ins Deutsche übersetzt…</div>

    <div v-else-if="recipe" class="recipe-content">
      <!-- Hero: Foto bis an die Ränder (ohne Foto warmer Verlauf), Titel + Fakten + Eigenschaften/Tags weiß im Bild -->
      <header :class="['recipe-hero', { 'recipe-hero--ph': !recipe.imageUrl, 'recipe-hero--immersive': !isGuest }]">
        <img
          v-if="recipe.imageUrl"
          ref="heroImage"
          class="recipe-hero__img"
          :src="recipe.imageUrl"
          :alt="shown.title"
          decoding="async"
          fetchpriority="high"
        />
        <HeroIcon v-else name="topf" :size="72" class="recipe-hero__pot" />
        <div class="recipe-hero__scrim"></div>
        <button
          v-if="!isGuest"
          type="button"
          class="recipe-hero__back"
          aria-label="Zurück zur Übersicht"
          @click="goBack"
        ><HeroIcon name="zurueck" :size="17" /></button>
        <ImageCredit :credit="recipe.imageCredit" overlay class="recipe-hero__credit" />
        <div class="recipe-hero__content">
          <h1 ref="titleRef">{{ shown.title }}</h1>
          <div class="recipe-hero__facts">
            <span v-if="recipe.prepTimeMinutes"><HeroIcon name="uhr" :size="14" />{{ formatPrepTime(recipe.prepTimeMinutes) }}</span>
            <span v-if="ingredientCount"><HeroIcon name="zutaten" :size="14" />{{ ingredientCount }} {{ ingredientCount === 1 ? 'Zutat' : 'Zutaten' }}</span>
            <button
              v-if="nutrition?.hasValues"
              type="button"
              :class="['recipe-hero__fact', 'meta-item--kcal', { 'meta-item--incomplete': !nutrition.coverage.complete }]"
              :title="kcalChipTitle"
              @click="activeTab = 'nutrition'"
            ><HeroIcon name="flamme" :size="14" />{{ nutrition.coverage.complete ? '' : 'mind. ' }}{{ formatKcal(nutrition.perServing.kcal) }} kcal</button>
            <button
              v-if="ratingStats.count"
              type="button"
              class="recipe-hero__fact meta-item--rating"
              :title="`Durchschnitt aus ${ratingCountLabel(ratingStats.count)}`"
              @click="scrollToRatings()"
            ><HeroIcon name="stern" :size="13" class="recipe-hero__star" />{{ ratingLabel(ratingStats.avg, ratingStats.count) }}</button>
            <span class="print-only">{{ currentServings }} {{ currentServings === 1 ? 'Person' : 'Personen' }}</span>
          </div>
          <div v-if="heroBadges.length || recipe.tags?.length" class="recipe-hero__tags">
            <span v-for="badge in heroBadges" :key="badge" class="recipe-hero__badge">
              <HeroIcon :name="badgeIcon(badge)" :size="11" />{{ badge }}
            </span>
            <button
              v-for="tag in recipe.tags"
              :key="tag"
              type="button"
              class="recipe-tag"
              :disabled="isGuest"
              :title="isGuest ? undefined : `Alle Rezepte mit „${tag}“`"
              @click="searchTag(tag)"
            >#{{ tag }}</button>
          </div>
        </div>
      </header>

      <div class="recipe-sheet">
        <TranslationBanner v-if="banner" :banner="banner" @show-original="setLanguage('original')" />
        <p v-if="translationNote" class="translation-note">{{ translationNote }}</p>

        <p v-if="shown.description" class="recipe-description">
          {{ shown.description }}
        </p>

        <!-- Quelle · Angelegt · Sichtbarkeit in einer gedämpften Zeile, Haarlinie darunter -->
        <p v-if="hasSourceLine" class="recipe-source">
          <span v-if="sourceText || recipe.page || sourceDomain(recipe.sourceUrl)" class="recipe-source__part">
            <span class="source-label">Quelle:</span>
            <span v-if="sourceText" class="source-name">{{ sourceText }}</span>
            <span v-if="recipe.page" class="source-page">Seite {{ recipe.page }}</span>
            <SourceLink :href="recipe.sourceUrl" class="source-url" />
          </span>
          <span v-if="createdText" class="recipe-source__part recipe-createdby">{{ createdText }}</span>
          <span v-if="isGuest && recipe.createdByFirstName" class="recipe-source__part meta-item--origin">von {{ recipe.createdByFirstName }}</span>
          <!-- Sichtbarkeit: für den Besitzer Sprung in die Bearbeitung, sonst reine Info -->
          <span v-if="visibility && !isGuest" class="recipe-source__part">
            <router-link
              v-if="isOwner"
              :to="recipeEditPath(recipe.id)"
              class="recipe-visibility recipe-visibility--link"
              :title="'Sichtbarkeit ändern'"
            >{{ visibility.text }}</router-link>
            <span v-else class="recipe-visibility">{{ visibility.text }}</span>
          </span>
        </p>

        <div v-if="translatable" class="language-toggle" role="group" aria-label="Sprache des Rezepts">
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

        <div v-if="!isGuest" class="recipe-tools">
          <button type="button" class="btn-tool" @click="showShareModal = true"><HeroIcon name="teilen" :size="15" />Teilen</button>
          <button type="button" class="btn-tool" title="Als PDF speichern" aria-label="Als PDF speichern" @click="printRecipe"><HeroIcon name="pdf" :size="15" />PDF</button>
          <KeepScreenOnToggle utility />
        </div>

        <div class="servings-control">
          <span class="servings-label">Personen<span v-if="recipe.servingsTo" class="servings-range">Rezept: {{ recipe.baseServings }}–{{ recipe.servingsTo }}</span></span>
          <div class="servings-step">
            <button class="servings-btn" aria-label="Eine Person weniger" @click="decreaseServings" :disabled="currentServings <= 1">−</button>
            <span class="servings-value" aria-live="polite">{{ currentServings }}</span>
            <button class="servings-btn" aria-label="Eine Person mehr" @click="increaseServings">+</button>
          </div>
        </div>

        <section v-if="recipe.ingredients?.length" class="recipe-section">
          <div class="section-header">
            <div class="tab-toggle">
              <button :class="['tab-btn', { active: activeTab === 'ingredients' }]" @click="activeTab = 'ingredients'">Zutaten</button>
              <button :class="['tab-btn', { active: activeTab === 'nutrition' }]" @click="activeTab = 'nutrition'">Nährwerte</button>
            </div>
          </div>

          <ul v-if="activeTab === 'ingredients'" class="ingredients-list">
            <template v-for="(section, sIndex) in ingredientSections" :key="sIndex">
              <li v-if="section.group" class="ingredient-group-title">{{ section.group }}</li>
              <li v-else-if="sIndex > 0" class="ingredient-group-gap" aria-hidden="true"></li>
              <li v-for="(ingredient, index) in section.items" :key="`${sIndex}-${index}`">
                <span :class="['ingredient-amount', { 'ingredient-amount--opt': amountText(ingredient, shownLanguage).toTaste }]">{{ amountText(ingredient, shownLanguage).text }}</span>
                <span class="ingredient-name">{{ ingredient.name }}</span>
              </li>
            </template>
          </ul>

          <template v-if="activeTab === 'nutrition'">
            <p v-if="nutritionLoading" class="nutrition-state">Nährwerte werden berechnet…</p>
            <p v-else-if="nutritionError" class="nutrition-state nutrition-state--error">{{ nutritionError }}</p>
            <NutritionPanel
              v-else-if="nutrition"
              :nutrition="nutrition"
              :info="nutritionInfo"
            />
          </template>
        </section>

        <section v-if="shown.instructions?.length" class="recipe-section">
          <h2>Zubereitung</h2>
          <ol class="instructions-list">
            <li v-for="(instruction, index) in shown.instructions" :key="index">
              <InstructionText v-if="ingredientRefs.enabled.value" :text="instruction" :step="index" :context="ingredientRefs" />
              <template v-else>{{ instruction }}</template>
            </li>
          </ol>
        </section>

        <section id="bewertungen" ref="ratingsSection" class="recipe-section recipe-ratings" aria-labelledby="ratings-title">
          <h2 id="ratings-title">Bewertungen</h2>

          <p v-if="ratingsError" class="ratings-state ratings-state--error">{{ ratingsError }}</p>
          <p v-else-if="ratingsLoading && !ratings.length" class="ratings-state">Bewertungen werden geladen…</p>

          <div v-if="ratingStats.count" class="ratings-summary">
            <RatingStars :model-value="Math.round(ratingStats.avg)" :size="20" readonly />
            <span class="ratings-summary__value">{{ formatRating(ratingStats.avg) }}</span>
            <span class="ratings-summary__count">bei {{ ratingCountLabel(ratingStats.count) }}</span>
          </div>
          <p v-else-if="!ratingsLoading && !ratingsError" class="ratings-state">{{ isGuest ? 'Noch keine Bewertungen.' : 'Noch keine Bewertungen – sei die/der Erste!' }}</p>

          <!-- Mobil wird über „Bewerten“ in der Aktionsleiste bewertet (Popup); das Inline-Formular gibt es nur ab 1024 px -->
          <RatingForm v-if="!isGuest" class="rating-form--inline" :recipe-id="recipe.id" @submitted="loadRatings(recipe.id)" />

          <ul v-if="ratings.length" class="rating-list">
            <li v-for="r in visibleRatings" :key="r.id" class="rating-entry">
              <div class="rating-entry__head">
                <RatingStars :model-value="r.stars" :size="15" readonly />
                <span class="rating-entry__name">{{ raterName(r) }}</span>
                <span v-if="r.own" class="rating-entry__badge">Du</span>
              </div>
              <div class="rating-entry__meta">
                <span class="rating-entry__date">{{ formatRatingDate(r.createdAt) }}</span>
                <span v-if="r.updated" class="rating-entry__updated">aktualisiert</span>
                <button
                  v-if="!isGuest && (r.own || authStore.isAdmin)"
                  type="button"
                  class="rating-entry__delete"
                  @click="ratingToDelete = r"
                >Löschen</button>
              </div>
              <p v-if="r.comment" class="rating-entry__comment">{{ r.comment }}</p>
            </li>
          </ul>
          <button
            v-if="ratings.length > RATINGS_PREVIEW"
            type="button"
            class="rating-list__toggle"
            :aria-expanded="showAllRatings"
            @click="showAllRatings = !showAllRatings"
          >{{ showAllRatings ? 'Weniger anzeigen' : `Alle ${ratings.length} Bewertungen anzeigen` }}</button>

          <!-- Gäste: die einzige Anmelde-Aufforderung auf einem öffentlichen Rezept -->
          <router-link
            v-if="isGuest"
            :to="{ path: '/login', query: { redirect: route.fullPath } }"
            class="ui-btn rate-login"
          ><AppIcon name="stern" :size="17" />Zum Bewerten anmelden</router-link>
        </section>

        <!-- Öffentliche Rezepte anderer: Melde-Link (Notice-and-Takedown) plus Impressum/Datenschutz -->
        <LegalLinks v-if="reportable" :report="report" />

        <footer class="print-footer">
          <p>Aus dem Familienkochbuch — pastoors.cloud</p>
          <p class="print-footer__small">Für den privaten Gebrauch</p>
        </footer>

        <div v-if="!isGuest" class="detail-actions">
          <button class="btn-cancel" @click="goBack">Zurück</button>
          <template v-if="canEdit">
            <button class="btn-delete" @click="openDeleteRecipe">Löschen</button>
            <router-link :to="recipeEditPath(recipe.id)" class="btn-edit">
              Bearbeiten
            </router-link>
          </template>
        </div>

      </div>

      <DetailActionBar
        v-if="!isGuest"
        ref="actionBar"
        :can-edit="canEdit"
        @delete="openDeleteRecipe"
        @edit="router.push(recipeEditPath(recipe.id))"
        @rate="showRatingDialog = true"
      />
    </div>

    <div v-else class="not-found">
      <p>Rezept nicht gefunden</p>
      <router-link to="/" class="btn-cancel">Zurück zur Übersicht</router-link>
    </div>

    <ConfirmDialog
      v-if="ratingToDelete"
      title="Bewertung löschen?"
      :text="ratingToDelete.own ? 'Deine Bewertung wird entfernt.' : `Die Bewertung von ${raterName(ratingToDelete)} wird entfernt.`"
      confirm-label="Löschen"
      destructive
      @confirm="confirmDeleteRating"
      @cancel="ratingToDelete = null"
    />

    <ConfirmDialog
      v-if="showDeleteRecipe"
      title="Rezept löschen?"
      :text="`„${shown?.title ?? ''}“ wird endgültig gelöscht.`"
      confirm-label="Löschen"
      destructive
      :confirm-disabled="!deleteWordOk"
      @confirm="confirmDeleteRecipe"
      @cancel="showDeleteRecipe = false"
    >
      <!-- Schutz vor Versehen: kurzes Küchenwort abtippen (für alle Konten, bewusst kein Passwort) -->
      <label class="delete-word" for="delete-word-input">
        Zum Bestätigen tippe <strong class="delete-word__word">{{ deleteWord }}</strong> ein:
      </label>
      <input
        id="delete-word-input"
        v-model="deleteWordInput"
        class="delete-word__input"
        type="text"
        autocomplete="off"
        autocapitalize="off"
        autocorrect="off"
        spellcheck="false"
        enterkeyhint="done"
        @keydown.enter.prevent="deleteWordOk && confirmDeleteRecipe()"
      />
    </ConfirmDialog>

    <RatingDialog
      v-if="showRatingDialog && recipe"
      :recipe-id="recipe.id"
      @close="closeRatingDialog"
      @submitted="onDialogRated"
    />

    <ShareModal
      v-if="showShareModal && recipe"
      :recipe-id="recipe.id"
      :decorate-url="shareLinkFor"
      @close="showShareModal = false"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import { useAuthStore } from '@/stores/authStore'
import { scaleIngredients } from '@/utils/scaleIngredients'
import { groupSections } from '@/utils/ingredientGroups'
import { tagSearchTerm } from '@/utils/recipeTags'
import ShareModal from '@/components/ShareModal.vue'
import NutritionPanel from '@/components/NutritionPanel.vue'
import ImageCredit from '@/components/ImageCredit.vue'
import KeepScreenOnToggle from '@/components/KeepScreenOnToggle.vue'
import TranslationBanner from '@/components/TranslationBanner.vue'
import SourceLink from '@/components/SourceLink.vue'
import RatingStars from '@/components/RatingStars.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import RatingForm from '@/components/RatingForm.vue'
import RatingDialog from '@/components/RatingDialog.vue'
import DetailActionBar from '@/components/shell/DetailActionBar.vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import HeroIcon from '@/components/HeroIcon.vue'
import { visibilityLabel, VISIBILITY } from '@/utils/visibility'
import LegalLinks from '@/components/LegalLinks.vue'
import {
  formatRating,
  ratingLabel,
  ratingCountLabel,
  ratingSummary,
  markUpdated,
  formatRatingDate,
  raterName,
  wantsRatings,
} from '@/utils/ratings'
import { sourceDomain } from '@/utils/sourceUrl'
import {
  amountText,
  badgeIcon,
  createdShort,
  formatPrepTime,
  recipeBadges,
  sourceLabel,
} from '@/utils/recipeHero'
import { navigateBackTo, LIST_PATH } from '@/utils/backNavigation'
import { nutritionService, formatKcal } from '@/services/nutritionService'
import { recipeService } from '@/services/recipeService'
import {
  readLanguagePreference,
  writeLanguagePreference,
  isTranslatable,
  wantsTranslation,
  applyTranslation,
  translationBanner,
  shareUrl,
} from '@/utils/recipeLanguage'
import {
  track,
  startVisit,
  previousRouteName,
  recipeOrigin,
  languageDirection,
  createChangeAggregator,
  randomId,
  RECIPE_TOTAL_CAP_MS,
  SERVINGS_DELAY_MS,
} from '@/utils/analytics'
import { recipePath, recipeEditPath } from '@/router/paths.js'
import KitchenMishap from '@/components/KitchenMishap.vue'
import { pickDeleteWord, matchesDeleteWord } from '@/utils/deleteWord.js'
import InstructionText from '@/components/InstructionText.vue'
import { useIngredientRefs } from '@/composables/useIngredientRefs'
import { ingredientRefsVisible } from '@/utils/ingredientRefs'

const route = useRoute()
const router = useRouter()
const store = useRecipeStore()
const uiStore = useUiStore()
const authStore = useAuthStore()



const recipe = computed(() => store.currentRecipe)

const searchTag = (tag) => {
  track('filter_gesetzt', { filter: 'tag', wert: tag, quelle: 'rezept' })
  store.setSearchTerms([tagSearchTerm(tag)])
  store.setPendingSearchTerm('')
  router.push('/')
}
// Gast auf einem öffentlichen Rezept: alles lesbar, nur Bewerten braucht ein Konto
const isGuest = computed(() => !authStore.isAuthenticated)
const isOwner = computed(() => recipe.value?.ownerId != null && recipe.value.ownerId === authStore.user?.id)
const visibility = computed(() => visibilityLabel(recipe.value))
const reportable = computed(() => isGuest.value || (recipe.value?.visibility === VISIBILITY.PUBLIC && !isOwner.value))
const report = computed(() => recipe.value && {
  recipeId: recipe.value.id,
  title: shown.value?.title ?? recipe.value.title,
  url: `${window.location.origin}${recipePath(recipe.value.id)}`,
})
const canEdit = computed(() =>
  authStore.isAdmin || (recipe.value?.ownerId != null && recipe.value.ownerId === authStore.user?.id)
)
const currentServings = ref(1)
const activeTab = ref('ingredients')
const titleRef = ref(null)
const showShareModal = ref(false)
const notFound = ref(false)
const nutrition = ref(null)
const nutritionInfo = ref(null)
const nutritionLoading = ref(false)
const nutritionError = ref(null)
let titleObserver = null

// Nutzungsstatistik: Öffnen mit Herkunft, beim Verlassen sichtbare Zeit + Gesamtspanne (gekappt bei 6 h)
let recipeVisit = null
let unmounted = false
const servingsChanges = createChangeAggregator(SERVINGS_DELAY_MS, (von, auf) => {
  track('portionen_geaendert', { recipeId: recipe.value?.id, von, auf })
})

// Bewertungen hängen am Rezept, nicht an der Sprache – sie bleiben beim Umschalten Deutsch/Original gleich
const RATINGS_PREVIEW = 3
const ratings = ref([])
const ratingsLoading = ref(false)
const ratingsError = ref(null)
const ratingsSection = ref(null)
const showAllRatings = ref(false)
const ratingToDelete = ref(null)
// Mobile Aktionsleiste: Bewerten als Dialog, Löschen mit Rückfrage
const actionBar = ref(null)
const showRatingDialog = ref(false)
const showDeleteRecipe = ref(false)
const deleteWord = ref(null)
const deleteWordInput = ref('')
const deleteWordOk = computed(() => matchesDeleteWord(deleteWordInput.value, deleteWord.value))

const ratingStats = computed(() => ratingSummary(ratings.value))
const visibleRatings = computed(() =>
  showAllRatings.value ? ratings.value : ratings.value.slice(0, RATINGS_PREVIEW)
)
const createdText = computed(() =>
  isGuest.value ? '' : createdShort(recipe.value?.createdBy, recipe.value?.createdAt, isOwner.value)
)
const sourceText = computed(() => sourceLabel(recipe.value))
const hasSourceLine = computed(() =>
  !!(sourceText.value || recipe.value?.page || sourceDomain(recipe.value?.sourceUrl) || createdText.value
    || (isGuest.value ? recipe.value?.createdByFirstName : visibility.value))
)
const ingredientCount = computed(() => recipe.value?.ingredients?.length ?? 0)
// Eigenschaften wie auf der Karte: Nährwert-Badges (Detail-Berechnung, sonst aus der Liste) + „Schnell“
const heroBadges = computed(() => recipeBadges(
  nutrition.value?.badges ?? store.getRecipeById(recipe.value?.id)?.nutrition?.badges,
  recipe.value?.prepTimeMinutes,
))

const loadRatings = async (id) => {
  ratingsLoading.value = true
  ratingsError.value = null
  try {
    ratings.value = markUpdated(await recipeService.getRatings(id))
    store.setRatingSummary(id, ratingStats.value.avg, ratingStats.value.count)
  } catch {
    ratingsError.value = 'Bewertungen konnten nicht geladen werden.'
  } finally {
    ratingsLoading.value = false
  }
}

const closeRatingDialog = async () => {
  showRatingDialog.value = false
  await nextTick()
  actionBar.value?.focus('rate')
}

const onDialogRated = async () => {
  await closeRatingDialog()
  await loadRatings(recipe.value.id)
}

const confirmDeleteRating = async () => {
  const target = ratingToDelete.value
  ratingToDelete.value = null
  if (!target) return
  try {
    await recipeService.deleteRating(recipe.value.id, target.id)
  } catch (error) {
    ratingsError.value = error.message || 'Die Bewertung konnte nicht gelöscht werden.'
    return
  }
  await loadRatings(recipe.value.id)
}

const scrollToRatings = (behavior = 'smooth') => {
  ratingsSection.value?.scrollIntoView({ behavior, block: 'start' })
}

const heroImage = ref(null)
const imageSettled = () => {
  const img = heroImage.value
  if (!img || img.complete) return Promise.resolve()
  return new Promise((resolve) => {
    img.addEventListener('load', resolve, { once: true })
    img.addEventListener('error', resolve, { once: true })
    setTimeout(resolve, 3000)
  })
}

// Bild, Nährwerte und Bewertungen verschieben den Abschnitt beim Laden (iOS hält die Position nicht fest):
// sofort hinspringen und nach dem Laden noch einmal
const jumpToRatings = async (loads) => {
  scrollToRatings('auto')
  await Promise.allSettled([...loads, imageSettled()])
  await nextTick()
  scrollToRatings('auto')
}

const languagePref = ref(readLanguagePreference())
const translation = ref(null)
const translationLoading = ref(false)
// Laufende Übersetzung = beschäftigt → kein automatisches Neuladen (update-check.js)
const translationBusy = {}
watch(translationLoading, (on) => {
  if (window.__appBusy) on ? window.__appBusy.add(translationBusy) : window.__appBusy.delete(translationBusy)
})
const translatable = computed(() => isTranslatable(recipe.value))
const shown = computed(() =>
  wantsTranslation(recipe.value, languagePref.value) ? applyTranslation(recipe.value, translation.value) : recipe.value
)
// Only the very first translation of a recipe takes a few seconds; until then the page waits instead of flashing English
const waitingForTranslation = computed(() =>
  wantsTranslation(recipe.value, languagePref.value) && translationLoading.value && !translation.value
)
const translationNote = computed(() =>
  wantsTranslation(recipe.value, languagePref.value) && translation.value && translation.value.status !== 'translated'
    ? 'Die Übersetzung ist gerade nicht verfügbar – hier steht das Original.'
    : ''
)
const banner = computed(() => translationBanner(recipe.value?.language, languagePref.value, translation.value?.status))

const loadTranslation = async () => {
  if (!wantsTranslation(recipe.value, languagePref.value) || translation.value?.status === 'translated') return
  if (translationLoading.value) return
  translationLoading.value = true
  translation.value = null
  try {
    translation.value = await recipeService.getTranslation(recipe.value.id, 'de')
  } catch {
    translation.value = { status: 'unavailable' }
  } finally {
    translationLoading.value = false
  }
}

const setLanguage = async (value) => {
  if (value !== languagePref.value) {
    track('sprache_gewechselt', { recipeId: recipe.value?.id, richtung: languageDirection(value) })
  }
  languagePref.value = value
  writeLanguagePreference(value)
  await loadTranslation()
}

const shareLinkFor = (url) => shareUrl(url, recipe.value, languagePref.value)

// The heading is rendered anew after the translation wait, so the observer follows the element
watch(titleRef, (el, previous) => {
  if (!titleObserver) return
  if (previous) titleObserver.unobserve(previous)
  if (el) titleObserver.observe(el)
})

const loadNutrition = async (id) => {
  nutritionLoading.value = true
  nutritionError.value = null
  try {
    const [data, info] = await Promise.all([
      nutritionService.getRecipeNutrition(id),
      nutritionService.getInfo().catch(() => null),
    ])
    nutrition.value = data
    nutritionInfo.value = info
  } catch (error) {
    nutritionError.value = 'Nährwerte konnten nicht geladen werden.'
  } finally {
    nutritionLoading.value = false
  }
}

const kcalChipTitle = computed(() => {
  const n = nutrition.value
  if (!n?.hasValues) return ''
  const base = `Berechnet aus ${n.coverage.calculated} von ${n.coverage.relevant} Zutaten`
  return n.coverage.complete ? base : `${base} – unvollständig, tatsächlicher Wert liegt höher`
})

onMounted(async () => {
  activeTab.value = 'ingredients'
  const herkunft = recipeOrigin(previousRouteName(), store.activeSearchTerms.some((t) => !t.startsWith('#')))
  try {
    await store.fetchRecipeById(route.params.id)
  } catch (error) {
    notFound.value = error.status === 404
    return
  }
  if (recipe.value && !unmounted) {
    const besuch = randomId()
    track('rezept_geoeffnet', { recipeId: recipe.value.id, herkunft, besuch })
    recipeVisit = startVisit('rezept_verlassen', { recipeId: recipe.value.id, besuch }, { capTotalMs: RECIPE_TOTAL_CAP_MS })
  }
  if (recipe.value) {
    currentServings.value = recipe.value.baseServings
    const loads = [loadNutrition(recipe.value.id), loadRatings(recipe.value.id)]
    await loadTranslation()
    await nextTick()
    // Von der Bewertung auf der Rezeptkarte: direkt zum Abschnitt „Bewertungen“
    if (wantsRatings(route.hash)) jumpToRatings(loads)
  }

  if (titleRef.value) {
    titleObserver = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting && shown.value?.title) {
          uiStore.setNavTitle(shown.value.title)
        } else {
          uiStore.clearNavTitle()
        }
      },
      { threshold: 0 }
    )
    titleObserver.observe(titleRef.value)
  }
})

onUnmounted(() => {
  unmounted = true
  servingsChanges.flush()
  recipeVisit?.end()
  window.__appBusy?.delete(translationBusy)
  // The list is served from cache, so a leftover error would otherwise show up there
  store.clearError()
  titleObserver?.disconnect()
  uiStore.clearNavTitle()
})

// Wie der Zurück-Pfeil der Kopfzeile: immer zur Liste, aus der Liste kommend per router.back()
const goBack = () => navigateBackTo(router, LIST_PATH)

const increaseServings = () => {
  servingsChanges.change(currentServings.value, currentServings.value + 1)
  currentServings.value++
}

const decreaseServings = () => {
  if (currentServings.value > 1) {
    servingsChanges.change(currentServings.value, currentServings.value - 1)
    currentServings.value--
  }
}

const scaledIngredients = computed(() =>
  scaleIngredients(shown.value?.ingredients, recipe.value?.baseServings, currentServings.value)
)

const ingredientSections = computed(() => groupSections(scaledIngredients.value))

// Zutaten in den Schritten antippen → Menge (skaliert, in der angezeigten Sprache)
const shownLanguage = computed(() =>
  shown.value !== recipe.value ? 'de' : (recipe.value?.language || 'de')
)
const ingredientRefs = useIngredientRefs({
  ingredients: scaledIngredients,
  language: shownLanguage,
  enabled: computed(() => ingredientRefsVisible(authStore.user)),
})

const printRecipe = async () => {
  track('pdf_gespeichert', { recipeId: recipe.value?.id })
  activeTab.value = 'ingredients'
  await nextTick()
  const originalTitle = document.title
  document.title = shown.value.title
  window.addEventListener('afterprint', () => { document.title = originalTitle }, { once: true })
  window.print()
}

const deleteRecipe = async () => {
  try {
    await store.deleteRecipe(route.params.id)
    // Gelöschtes Rezept nicht im Verlauf lassen
    navigateBackTo(router, LIST_PATH)
  } catch (error) {
    alert('Fehler beim Löschen des Rezepts: ' + error.message)
  }
}

// Mobil (Aktionsleiste) und Desktop (Knopfreihe) mit demselben App-Dialog samt Küchenwort
const openDeleteRecipe = () => {
  deleteWord.value = pickDeleteWord(deleteWord.value)
  deleteWordInput.value = ''
  showDeleteRecipe.value = true
}

const confirmDeleteRecipe = async () => {
  if (!deleteWordOk.value) return
  showDeleteRecipe.value = false
  await deleteRecipe()
}
</script>

<style scoped>
/* Hero-Design (abgenommen 21.10.2026, Entwurf media/out/mock_detail.html): Foto bis an die Ränder, Inhalt als
   ein Sheet, das sich mit runden Ecken über das Foto schiebt. Farben aus styles/hero.css (--h-*). */
.recipe-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

@media (max-width: 1023px) {
  .recipe-detail {
    padding: 0;
  }
}

.loading,
.error,
.not-found {
  text-align: center;
  padding: 48px 24px;
  color: var(--color-text-secondary, #666);
}

.error {
  color: var(--color-error, #e53e3e);
}

.not-found h2 {
  margin: 0 0 8px;
  color: var(--color-text-primary, #333);
}

.not-found .btn-cancel {
  display: inline-block;
  margin-top: 16px;
}

/* ── Hero ─────────────────────────────────────────────────────────────── */
.recipe-hero {
  position: relative;
  height: 400px;
  overflow: hidden;
  background: #2a2e37;
}

.recipe-hero--immersive {
  height: calc(400px + var(--shell-top, 0px));
}

.recipe-hero--ph {
  background: var(--h-placeholder);
}

.recipe-hero__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 45%;
}

.recipe-hero__pot {
  position: absolute;
  top: calc(var(--shell-top, 0px) + 60px);
  left: 50%;
  transform: translateX(-50%);
  color: #fff;
  opacity: .45;
}

.recipe-hero__scrim {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, .28) 0%, rgba(0, 0, 0, 0) 32%, rgba(0, 0, 0, 0) 42%, rgba(0, 0, 0, .72) 100%);
  pointer-events: none;
}

.recipe-hero__back {
  position: absolute;
  z-index: 2;
  top: calc(var(--shell-top, 0px) + 14px);
  left: 14px;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 999px;
  background: var(--h-glass);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* Tippfläche 46 px, sichtbar bleibt der 38-px-Kreis */
.recipe-hero__back::after {
  content: '';
  position: absolute;
  inset: -4px;
}

.recipe-hero__back:focus-visible {
  outline: 2px solid #fff;
  outline-offset: 2px;
}

.recipe-hero__credit {
  position: absolute;
  z-index: 2;
  top: 14px;
  right: 14px;
  max-width: calc(100% - 28px);
}

.recipe-hero--immersive .recipe-hero__credit {
  top: calc(var(--shell-top, 0px) + 63px);
}

.recipe-hero__content {
  position: absolute;
  z-index: 1;
  left: 18px;
  right: 18px;
  bottom: 44px;
  color: #fff;
}

.recipe-hero__content h1 {
  margin: 0 0 9px;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.1;
  color: #fff;
  text-shadow: 0 1px 10px rgba(0, 0, 0, .35);
  overflow-wrap: anywhere;
}

.recipe-hero__facts {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 14px;
  font-size: 13.5px;
  font-weight: 650;
  color: rgba(255, 255, 255, .95);
}

.recipe-hero__facts > * {
  display: inline-flex;
  align-items: center;
  gap: 5.5px;
}

.recipe-hero__facts .hero-icon {
  color: rgba(255, 255, 255, .85);
}

.recipe-hero__facts .recipe-hero__star {
  color: var(--h-star);
}

.recipe-hero__fact {
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
}

.recipe-hero__fact:hover {
  text-decoration: underline;
}

.recipe-hero__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 9px;
  margin-top: 9px;
  font-size: 12.5px;
  font-weight: 650;
  color: rgba(255, 255, 255, .92);
}

.recipe-hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 3.5px;
  font-weight: 750;
}

.recipe-tag {
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
}

.recipe-tag:disabled {
  cursor: default;
}

.recipe-tag:not(:disabled):hover {
  text-decoration: underline;
}

.print-only {
  display: none !important;
}

/* ── Sheet ────────────────────────────────────────────────────────────── */
.recipe-sheet {
  position: relative;
  z-index: 1;
  margin-top: -26px;
  min-height: calc(100vh - 374px);
  padding: 24px 18px 32px;
  background: var(--h-card);
  color: var(--h-ink);
  border-radius: 26px 26px 0 0;
  box-shadow: 0 -8px 30px rgba(0, 0, 0, .10);
}

@media (min-width: 1024px) {
  .recipe-hero {
    border-radius: 22px;
  }

  /* Am Desktop führt der Pfeil der Kopfzeile zurück */
  .recipe-hero__back {
    display: none;
  }

  .recipe-hero__credit,
  .recipe-hero--immersive .recipe-hero__credit {
    top: 14px;
  }

  .recipe-sheet {
    min-height: 0;
    padding: 28px 32px 8px;
    border-radius: 26px;
  }
}

.translation-note {
  margin: 0 0 12px;
  font-size: 0.875rem;
  color: var(--warn-text);
}

.recipe-sheet > :deep(.translation-banner:first-child) {
  margin-top: 0;
}

.recipe-description {
  margin: 0;
  font-size: 15px;
  line-height: 1.55;
  color: var(--h-ink);
}

.recipe-source {
  margin: 8px 0 0;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--h-sep);
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--h-by);
  /* Lange Webadressen im Quellenfeld umbrechen statt die Seite zu verbreitern (Rezept 194) */
  overflow-wrap: anywhere;
}

.recipe-sheet > .recipe-source:first-child {
  margin-top: 0;
}

.recipe-source__part + .recipe-source__part::before {
  content: '·';
  margin: 0 5px;
  color: var(--h-by);
}

.source-label {
  margin-right: 4px;
}

.source-page {
  margin-left: 4px;
}

.source-name + .source-page::before {
  content: ', ';
  margin-left: -4px;
}

.source-url {
  margin-left: 6px;
}

.source-label + .source-url {
  margin-left: 0;
}

.recipe-visibility {
  display: inline;
  margin: 0;
  font-size: inherit;
  color: inherit;
}

.recipe-visibility--link {
  color: inherit;
  text-decoration: underline dotted;
  text-underline-offset: 2px;
}

.language-toggle {
  display: inline-flex;
  gap: 2px;
  margin-top: 16px;
  padding: 3px;
  border-radius: 12px;
  background: var(--h-chip);
}

.lang-btn {
  min-height: 38px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  background: transparent;
  font: inherit;
  font-size: 13px;
  font-weight: 650;
  color: var(--h-mut);
  cursor: pointer;
}

.lang-btn.active {
  background: var(--h-card);
  color: var(--h-ink);
  box-shadow: 0 1px 2px rgba(0, 0, 0, .12);
}

.recipe-tools {
  display: flex;
  gap: 9px;
  margin-top: 16px;
}

.btn-tool {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 44px;
  padding: 12px 0;
  border: none;
  border-radius: 13px;
  background: var(--h-chip);
  font: inherit;
  font-size: 13px;
  font-weight: 650;
  color: var(--h-ink);
  cursor: pointer;
}

.btn-tool:hover {
  background: color-mix(in srgb, var(--h-chip) 88%, var(--h-ink));
}

.btn-tool:focus-visible,
.servings-btn:focus-visible,
.tab-btn:focus-visible,
.lang-btn:focus-visible {
  outline: 2px solid var(--h-acc);
  outline-offset: 2px;
}

.servings-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 0;
  border-top: 1px solid var(--h-sep);
  border-bottom: 1px solid var(--h-sep);
}

.servings-label {
  display: flex;
  flex-direction: column;
  font-size: 15px;
  font-weight: 700;
  color: var(--h-ink);
}

.servings-range {
  font-size: 12px;
  font-weight: 500;
  color: var(--h-by);
}

.servings-step {
  display: flex;
  align-items: center;
  gap: 12px;
}

.servings-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 12px;
  background: var(--h-chip);
  font: inherit;
  font-size: 19px;
  font-weight: 700;
  color: var(--h-ink);
  cursor: pointer;
}

.servings-btn:hover:not(:disabled) {
  background: color-mix(in srgb, var(--h-chip) 88%, var(--h-ink));
}

.servings-btn:disabled {
  opacity: .4;
  cursor: not-allowed;
}

.servings-value {
  min-width: 20px;
  text-align: center;
  font-size: 18px;
  font-weight: 800;
  color: var(--h-tag);   /* dunkel heller als --h-acc, sonst zu wenig Kontrast */
}

/* ── Zutaten / Nährwerte / Zubereitung ────────────────────────────────── */
.recipe-section + .recipe-section {
  margin-top: 30px;
}

.section-header {
  display: flex;
  margin: 10px 0 4px;
  border-bottom: 1px solid var(--h-line);
}

.tab-toggle {
  display: flex;
  gap: 18px;
}

.tab-btn {
  min-height: 44px;
  padding: 8px 0;
  margin-bottom: -1px;
  border: none;
  border-bottom: 3px solid transparent;
  background: none;
  font: inherit;
  font-size: 19px;
  font-weight: 800;
  line-height: 1.3;
  color: var(--h-mut);
  cursor: pointer;
}

.tab-btn.active {
  color: var(--h-ink);
  border-bottom-color: var(--h-acc);
}

.tab-btn:not(.active):hover {
  color: var(--h-ink);
}

.recipe-section h2 {
  margin: 0 0 4px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--h-line);
  font-size: 19px;
  font-weight: 800;
  color: var(--h-ink);
}

.ingredients-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ingredients-list li {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 11px 2px;
  border-bottom: 1px solid var(--h-sep);
}

.ingredients-list li.ingredient-group-title {
  display: block;
  padding: 18px 2px 6px;
  font-size: 15px;
  font-weight: 800;
  color: var(--h-ink);
  break-after: avoid;
}

.ingredients-list li.ingredient-group-title:first-child {
  padding-top: 8px;
}

.ingredients-list li:has(+ .ingredient-group-gap) {
  border-bottom: none;
}

.ingredients-list li.ingredient-group-gap {
  padding: 6px 0;
  border-bottom: 1px solid var(--h-sep);
}

.ingredient-amount {
  width: 86px;
  flex: none;
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--h-ink);
}

.ingredient-amount--opt {
  font-size: 13px;
  font-weight: 600;
  color: var(--h-by);
}

.ingredient-name {
  font-size: 15px;
  color: var(--h-ink);
  min-width: 0;
  overflow-wrap: anywhere;
}

.instructions-list {
  padding-left: 24px;
}

.instructions-list li {
  padding: 10px 0;
  font-size: 15px;
  line-height: 1.6;
  color: var(--h-ink);
}

.instructions-list li::marker {
  color: var(--h-acc);
  font-weight: 700;
}

.nutrition-state {
  margin-top: 12px;
  color: var(--h-by);
}

.nutrition-state--error {
  color: var(--color-error, #e53e3e);
}

.rate-login {
  width: 100%;
  margin-top: 14px;
  text-decoration: none;
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  position: sticky;
  bottom: var(--shell-bottom, 0px);
  background: var(--h-card);
  padding: 12px 0;
  border-top: 1px solid var(--h-line);
  margin-top: 32px;
}

/* Mobil übernimmt die Aktionsleiste (DetailActionBar), zurück geht es über den Kreis im Foto;
   bewertet wird dort über „Bewerten“ (Popup) statt über das Inline-Formular */
@media (max-width: 1023px) {
  .detail-actions,
  .recipe-ratings .rating-form--inline {
    display: none;
  }
}

.detail-actions .btn-cancel {
  margin-right: auto;
}

.btn-edit,
.btn-delete,
.btn-cancel {
  padding: 8px 14px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.2s ease;
  white-space: nowrap;
}

.btn-edit {
  background: var(--color-primary, #4a5568);
  color: var(--akzent-kontrast);
}

.btn-edit:hover {
  background: var(--color-primary-dark, #2d3748);
}

/* Löschen dezent: grau, rot erst bei Hover/Druck */
.btn-delete {
  background: transparent;
  color: var(--text2);
  border: 1px solid var(--linie);
}

.btn-delete:hover,
.btn-delete:active {
  background: var(--neg-weich);
  color: var(--neg);
  border-color: var(--neg);
}

.delete-word {
  display: block;
  margin: 16px 0 6px;
  font-size: 0.95rem;
  color: var(--text);
}

.delete-word__word {
  font-weight: 700;
  letter-spacing: 0.02em;
}

.delete-word__input {
  width: 100%;
  min-height: 44px;
  padding: 8px 12px;
  border: 1px solid var(--linie);
  border-radius: 10px;
  background: var(--flaeche);
  color: var(--text);
  font: inherit;
  font-size: 1rem;
}

.delete-word__input:focus {
  outline: 2px solid var(--akzent);
  outline-offset: 1px;
  border-color: var(--akzent);
}

.btn-cancel {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.btn-cancel:hover {
  background: var(--color-border, #ddd);
}

.print-footer {
  display: none;
}

.ratings-state {
  margin: 0 0 12px;
  color: var(--color-text-secondary, #666);
}

.ratings-state--error {
  color: var(--color-error, #e53e3e);
}

.ratings-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.ratings-summary__value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-primary, #333);
}

.ratings-summary__count {
  color: var(--color-text-secondary, #666);
}

.rating-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.rating-entry {
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border-light, #eee);
}

.rating-entry:last-child {
  border-bottom: none;
}

.rating-entry__head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  font-size: 0.95rem;
}

.rating-entry__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 24px;
  margin-top: 2px;
}

.rating-entry__name {
  font-weight: 600;
  color: var(--color-text-primary, #333);
}

.rating-entry__badge {
  padding: 1px 7px;
  border-radius: 10px;
  background: var(--akzent-weich, var(--color-bg-secondary, #f0f0f0));
  color: var(--akzent-text);
  font-size: 0.72rem;
  font-weight: 700;
}

.rating-entry__date,
.rating-entry__updated {
  color: var(--text3);
  font-size: 0.82rem;
}

.rating-entry__updated::before {
  content: '· ';
}

.rating-entry__delete {
  margin: -10px -6px -10px auto;
  min-height: 44px;
  padding: 0 6px;
  border: none;
  background: none;
  color: var(--text3);
  font: inherit;
  font-size: 0.82rem;
  cursor: pointer;
}

.rating-entry__delete:hover {
  color: var(--color-error, #e53e3e);
  text-decoration: underline;
}

.rating-entry__comment {
  margin: 6px 0 0;
  line-height: 1.5;
  color: var(--color-text-primary, #333);
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.rating-list__toggle {
  min-height: 44px;
  padding: 8px 0;
  border: none;
  background: none;
  color: var(--akzent-text);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

@media print {
  @page {
    margin: 18mm 16mm;
  }

  :global(.shell-header),
  :global(.shell-nav) {
    display: none !important;
  }

  .recipe-detail {
    max-width: none;
    padding: 0;
  }

  .recipe-hero {
    height: auto;
    overflow: visible;
    background: none;
    border-radius: 0;
  }

  .recipe-hero__img {
    position: static;
    display: block;
    height: 240px;
    border-radius: 12px;
  }

  .recipe-hero__scrim,
  .recipe-hero__back,
  .recipe-hero__pot,
  .recipe-hero__tags,
  .meta-item--rating,
  .recipe-ratings,
  .recipe-createdby,
  .recipe-visibility,
  .meta-item--origin,
  .language-toggle,
  .recipe-tools,
  .translation-note,
  .detail-actions,
  .servings-control,
  .tab-btn:not(.active) {
    display: none !important;
  }

  .recipe-hero__credit {
    position: static;
    max-width: none;
    margin-top: 4px;
  }

  .recipe-hero__content {
    position: static;
    margin-top: 12px;
    color: #1C1C1E;
  }

  .recipe-hero__content h1 {
    color: #1C1C1E;
    text-shadow: none;
  }

  .recipe-hero__facts,
  .recipe-hero__facts .hero-icon {
    color: #6E6E73;
  }

  .print-only {
    display: inline-flex !important;
  }

  .recipe-sheet {
    margin: 16px 0 0;
    padding: 0;
    min-height: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .recipe-source {
    padding-bottom: 12px;
  }

  .recipe-section,
  .ingredients-list li,
  .instructions-list li {
    break-inside: avoid;
  }

  .tab-btn.active {
    padding-right: 0;
    border-bottom-color: transparent;
  }

  .print-footer {
    display: block;
    margin-top: 32px;
    padding-top: 12px;
    border-top: 1px solid var(--color-border, #ddd);
    text-align: center;
    font-size: 0.875rem;
    color: var(--color-text-secondary, #666);
    break-inside: avoid;
  }

  .print-footer__small {
    font-size: 0.75rem;
    color: var(--color-text-muted, #999);
  }
}
</style>

<style>
/* Rezeptseite mobil (angemeldet): Foto bis unter die Statusleiste, Zurück sitzt im Bild – keine Kopfzeile;
   Seitengrund in Sheet-Farbe, damit unter dem Sheet kein Streifen bleibt */
@media (max-width: 1023px) {
  body:has(.recipe-hero--immersive) .shell-header {
    display: none;
  }

  body:has(.recipe-hero--immersive) {
    background: var(--h-card);
  }
}

@media print {
  body:has(.recipe-hero--immersive) {
    background: #fff;
  }
}
</style>
