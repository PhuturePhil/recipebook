import { computed, ref, watch, onScopeDispose } from 'vue'
import { createIngredientMatcher, createPopoverState, refLines } from '@/utils/ingredientRefs'

// Zutaten-Verweise in den Schritten: Matching gegen die angezeigte (ggf. übersetzte, skalierte) Zutatenliste,
// ein gemeinsames Popover für alle Schritte. enabled = zentrale Freigabe (ingredientRefsVisible)
export function useIngredientRefs({ ingredients, language, enabled }) {
  const lang = computed(() => (language.value === 'en' ? 'en' : 'de'))
  const matcher = computed(() => (enabled.value ? createIngredientMatcher(ingredients.value, lang.value) : null))
  const segmentsFor = (text) => (matcher.value ? matcher.value(text) : [{ text: String(text ?? '') }])
  const linesFor = (refs) => refLines(refs, ingredients.value, lang.value)

  const state = createPopoverState()
  const openKey = ref(null)
  let openedAt = 0
  state.subscribe((open) => {
    openKey.value = open?.key ?? null
    openedAt = Date.now()
  })

  const onKeydown = (event) => {
    if (event.key === 'Escape') state.close()
  }
  // Fokus per Tastatur scrollt das Wort ggf. erst ins Bild – das soll das Popover nicht gleich wieder schließen
  const onScroll = () => {
    if (Date.now() - openedAt > 300) state.close()
  }
  const onPointerdown = (event) => {
    if (!event.target?.closest?.('.ingredient-ref, .ingredient-popover')) state.close()
  }
  const listen = (on) => {
    const method = on ? 'addEventListener' : 'removeEventListener'
    document[method]('keydown', onKeydown)
    document[method]('pointerdown', onPointerdown, true)
    window[method]('scroll', onScroll, { capture: true, passive: true })
    window[method]('resize', state.close)
  }
  watch(openKey, (key, previous) => {
    if (typeof document === 'undefined' || Boolean(key) === Boolean(previous)) return
    listen(Boolean(key))
  })
  watch([ingredients, lang, enabled], () => state.close())
  onScopeDispose(() => {
    if (openKey.value && typeof document !== 'undefined') listen(false)
  })

  return { enabled, language: lang, segmentsFor, linesFor, state, openKey }
}
