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
      <header class="recipe-header">
        <h1 ref="titleRef">{{ shown.title }}</h1>
        <TranslationBanner v-if="banner" :banner="banner" @show-original="setLanguage('original')" />
        <div class="recipe-tools">
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
          <template v-if="!isGuest">
            <button class="btn-tool" @click="showShareModal = true">Teilen</button>
            <button class="btn-tool" @click="printRecipe">Als PDF speichern</button>
            <KeepScreenOnToggle />
          </template>
        </div>
        <p v-if="translationNote" class="translation-note">{{ translationNote }}</p>
      </header>

      <figure v-if="recipe.imageUrl" class="recipe-figure">
        <div class="recipe-image">
          <img ref="heroImage" :src="recipe.imageUrl" :alt="shown.title" />
        </div>
        <ImageCredit :credit="recipe.imageCredit" />
      </figure>

      <p v-if="shown.description" class="recipe-description">
        {{ shown.description }}
      </p>

      <div class="recipe-meta">
        <span v-if="recipe.prepTimeMinutes" class="meta-item">
          {{ formatPrepTime(recipe.prepTimeMinutes) }}
        </span>
        <span v-if="recipe.baseServings" class="meta-item">
          {{ recipe.baseServings }}{{ recipe.servingsTo ? `–${recipe.servingsTo}` : '' }} Personen
        </span>
        <button
          v-if="nutrition?.hasValues"
          type="button"
          :class="['meta-item', 'meta-item--kcal', { 'meta-item--incomplete': !nutrition.coverage.complete }]"
          :title="kcalChipTitle"
          @click="activeTab = 'nutrition'"
        >
          {{ nutrition.coverage.complete ? '' : 'mind. ' }}{{ formatKcal(nutrition.perServing.kcal) }} kcal pro Portion
        </button>
        <button
          v-if="ratingStats.count"
          type="button"
          class="meta-item meta-item--rating"
          :title="`Durchschnitt aus ${ratingCountLabel(ratingStats.count)}`"
          @click="scrollToRatings()"
        >
          <RatingStars :model-value="1" :count="1" :size="15" readonly aria-hidden="true" />
          {{ ratingLabel(ratingStats.avg, ratingStats.count) }}
        </button>
        <span v-if="isGuest && recipe.createdByFirstName" class="meta-item meta-item--origin">
          <AppIcon name="globus" :size="14" />von {{ recipe.createdByFirstName }}
        </span>
      </div>

      <div v-if="!isGuest && (createdText || visibility)" class="recipe-origin">
        <p v-if="createdText" class="recipe-createdby">{{ createdText }}</p>
        <!-- Sichtbarkeit: für den Besitzer Sprung in die Bearbeitung, sonst reine Info -->
        <router-link
          v-if="visibility && isOwner"
          :to="recipeEditPath(recipe.id)"
          class="recipe-visibility"
          :title="'Sichtbarkeit ändern'"
        ><AppIcon :name="visibility.icon" :size="14" />{{ visibility.text }}</router-link>
        <p v-else-if="visibility" class="recipe-visibility"><AppIcon :name="visibility.icon" :size="14" />{{ visibility.text }}</p>
      </div>

      <div v-if="recipe.tags?.length" class="recipe-tags">
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

      <div v-if="recipe.author || recipe.source || sourceDomain(recipe.sourceUrl)" class="recipe-source">
        <span class="source-label">Quelle:</span>
        <span v-if="recipe.author" :class="['source-author', { 'source-author--sep': recipe.source }]">{{ recipe.author }}</span>
        <span v-if="recipe.source" class="source-name">{{ recipe.source }}</span>
        <span v-if="recipe.page" class="source-page">Seite {{ recipe.page }}</span>
        <SourceLink :href="recipe.sourceUrl" class="source-url" />
      </div>

      <div class="servings-control">
        <span class="servings-label">Personen:</span>
        <button class="servings-btn" @click="decreaseServings" :disabled="currentServings <= 1">-</button>
        <span class="servings-value">{{ currentServings }}</span>
        <button class="servings-btn" @click="increaseServings">+</button>
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
              <span class="ingredient-amount">{{ ingredient.amount }} {{ ingredient.unit }}</span>
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
            {{ instruction }}
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
import { visibilityLabel, VISIBILITY } from '@/utils/visibility'
import LegalLinks from '@/components/LegalLinks.vue'
import {
  formatRating,
  ratingLabel,
  ratingCountLabel,
  ratingSummary,
  markUpdated,
  formatRatingDate,
  createdLine,
  raterName,
  wantsRatings,
} from '@/utils/ratings'
import { sourceDomain } from '@/utils/sourceUrl'
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
const createdText = computed(() => createdLine(recipe.value?.createdBy, recipe.value?.createdAt))

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

const printRecipe = async () => {
  track('pdf_gespeichert', { recipeId: recipe.value?.id })
  activeTab.value = 'ingredients'
  await nextTick()
  const originalTitle = document.title
  document.title = shown.value.title
  window.addEventListener('afterprint', () => { document.title = originalTitle }, { once: true })
  window.print()
}

const formatPrepTime = (minutes) => {
  if (!minutes) return ''
  if (minutes < 60) return `${minutes} Min.`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h} Std. ${m} Min.` : `${h} Std.`
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
.recipe-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
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

.recipe-header {
  margin-bottom: 24px;
}

.language-toggle {
  display: inline-flex;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  overflow: hidden;
}

.lang-btn {
  padding: 6px 10px;
  border: none;
  background: transparent;
  font-size: 0.85rem;
  font-family: inherit;
  color: var(--color-text-secondary, #666);
  cursor: pointer;
}

.lang-btn + .lang-btn {
  border-left: 1px solid var(--color-border, #ddd);
}

.lang-btn.active {
  background: var(--color-primary, #4a5568);
  color: var(--akzent-kontrast);
}

.translation-note {
  margin: 8px 0 0;
  font-size: 0.875rem;
  color: var(--warn-text);
}

.recipe-header h1 {
  margin: 0;
  font-size: 2rem;
  color: var(--color-text-primary, #333);
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  position: sticky;
  bottom: var(--shell-bottom, 0px);
  background: var(--color-bg, #f9fafb);
  padding: 12px 0;
  border-top: 1px solid var(--color-border, #e2e8f0);
  margin-top: 32px;
}

/* Mobil übernimmt die Aktionsleiste (DetailActionBar), zurück geht es über den Pfeil in der Kopfzeile;
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

.recipe-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  font-size: 0.95rem;
  color: var(--color-text-secondary, #666);
}

.recipe-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin: -8px 0 16px;
}

.recipe-tag {
  padding: 2px 0;
  border: none;
  background: none;
  font: inherit;
  font-size: 0.85rem;
  color: var(--color-text-muted, #999);
  cursor: pointer;
}

.recipe-tag:disabled {
  cursor: default;
}

.recipe-tag:not(:disabled):hover {
  color: var(--color-primary, #4a5568);
  text-decoration: underline;
}

.meta-item {
  padding: 4px 10px;
  background: var(--color-bg-secondary, #f0f0f0);
  border-radius: 4px;
  font-weight: 500;
}

.meta-item--kcal {
  border: 1px solid transparent;
  font-family: inherit;
  font-size: inherit;
  color: var(--pos);
  background: var(--pos-weich);
  cursor: pointer;
}

.meta-item--kcal:hover {
  border-color: var(--pos-linie);
}

.meta-item--rating {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid transparent;
  font-family: inherit;
  font-size: inherit;
  color: var(--color-text-primary, #333);
  cursor: pointer;
}

.meta-item--rating:hover {
  border-color: var(--linie);
}

.recipe-origin {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 14px;
  margin: -6px 0 16px;
}

.recipe-createdby {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text3);
}

.recipe-origin .recipe-visibility {
  margin: 0;
  font-size: 0.85rem;
}

.meta-item--origin {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text2);
}

.rate-login {
  width: 100%;
  margin-top: 14px;
  text-decoration: none;
}

.meta-item--incomplete {
  color: var(--warn-text);
  background: var(--warn-bg);
}

.nutrition-state {
  color: var(--color-text-secondary, #666);
}

.nutrition-state--error {
  color: var(--color-error, #e53e3e);
}

.recipe-description {
  font-size: 1.125rem;
  color: var(--color-text-secondary, #666);
  line-height: 1.6;
  margin-bottom: 16px;
}

.recipe-source {
  margin-bottom: 24px;
  padding: 12px 16px;
  background: var(--color-bg-secondary, #f0f0f0);
  border-radius: 8px;
  font-size: 0.875rem;
  /* Lange Webadressen im Quellenfeld umbrechen statt die Seite zu verbreitern (Rezept 194) */
  overflow-wrap: anywhere;
}

.source-label {
  font-weight: 600;
  color: var(--color-text-primary, #333);
  margin-right: 8px;
}

.source-author {
  color: var(--color-text-primary, #333);
}

.source-author--sep::after {
  content: ' - ';
}

.source-name {
  color: var(--color-text-secondary, #666);
}

.source-page {
  color: var(--color-text-muted, #999);
  margin-left: 8px;
}

.source-url {
  margin-left: 8px;
}

.source-label + .source-url {
  margin-left: 0;
}

.recipe-figure {
  margin: 0 0 24px;
}

.recipe-image {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: 12px;
  background: var(--color-bg-secondary, #f0f0f0);
}

.recipe-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.servings-control {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
  padding: 12px 16px;
  background: var(--color-bg-card, #fff);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.servings-label {
  font-weight: 600;
  color: var(--color-text-primary, #333);
}

.servings-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
  font-size: 1.25rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
}

.servings-btn:hover:not(:disabled) {
  background: var(--color-border, #ddd);
}

.servings-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.servings-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-primary, #4a5568);
  min-width: 32px;
  text-align: center;
}

.recipe-section {
  margin-bottom: 32px;
}

.recipe-section h2 {
  font-size: 1.5rem;
  color: var(--color-text-primary, #333);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--color-border, #ddd);
}

.ingredients-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ingredients-list li {
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light, #eee);
  display: flex;
  gap: 8px;
}

.ingredients-list li:last-child {
  border-bottom: none;
}

.ingredients-list li.ingredient-group-title {
  display: block;
  padding: 16px 0 6px;
  font-weight: 700;
  color: var(--color-text-primary, #333);
  border-bottom: 1px solid var(--color-border, #ddd);
  break-after: avoid;
}

.ingredients-list li.ingredient-group-title:first-child {
  padding-top: 4px;
}

.ingredients-list li:has(+ .ingredient-group-title),
.ingredients-list li:has(+ .ingredient-group-gap) {
  border-bottom: none;
}

.ingredients-list li.ingredient-group-gap {
  padding: 6px 0;
  border-bottom: 1px solid var(--color-border, #ddd);
}

.ingredient-amount {
  font-weight: 600;
  color: var(--color-primary, #4a5568);
  min-width: 100px;
}

.ingredient-name {
  color: var(--color-text-primary, #333);
}

.instructions-list {
  padding-left: 24px;
}

.instructions-list li {
  padding: 12px 0;
  line-height: 1.6;
  color: var(--color-text-primary, #333);
}

.instructions-list li::marker {
  color: var(--color-primary, #4a5568);
  font-weight: 600;
}

.section-header {
  display: flex;
  align-items: flex-end;
  margin-bottom: 16px;
  border-bottom: 2px solid var(--color-border, #ddd);
}

.section-header h2 {
  font-size: 1.5rem;
  color: var(--color-text-primary, #333);
  margin: 0;
  padding-bottom: 8px;
  border-bottom: none;
}

.tab-toggle {
  display: flex;
  gap: 0;
}

.tab-btn {
  padding: 0 20px 10px 0;
  border: none;
  border-bottom: 3px solid transparent;
  background: none;
  color: var(--color-text-muted, #a0aec0);
  font-size: 1.5rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease;
  margin-bottom: -2px;
  font-family: inherit;
}

.tab-btn.active {
  color: var(--color-text-primary, #333);
  border-bottom-color: var(--color-primary, #4a5568);
}

.tab-btn:not(.active):hover {
  color: var(--color-text-secondary, #4a5568);
}

.recipe-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.btn-tool {
  padding: 6px 12px;
  background: var(--color-bg-card, #fff);
  color: var(--color-primary, #4a5568);
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-tool:hover {
  background: var(--color-bg-secondary, #f0f0f0);
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

  .recipe-ratings,
  .recipe-createdby,
  .recipe-origin,
  .meta-item--rating,
  .recipe-tools,
  .recipe-tags,
  .translation-note,
  .detail-actions,
  .servings-control,
  .tab-btn:not(.active) {
    display: none !important;
  }

  .recipe-image {
    max-height: 240px;
  }

  .recipe-section,
  .ingredients-list li,
  .instructions-list li {
    break-inside: avoid;
  }

  .tab-btn.active {
    padding-right: 0;
    border-bottom-color: transparent;
    color: var(--color-text-primary, #333);
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
