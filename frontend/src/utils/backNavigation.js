// Zurück-Navigation ohne doppelte Verlaufseinträge: Ist der vorige Verlaufseintrag schon das Ziel, geht es per
// router.back() dorthin (Scrollposition und Suche bleiben, der aktuelle Eintrag fällt weg); sonst wird der aktuelle
// Eintrag durch das Ziel ersetzt. So entsteht weder Liste → Rezept → Formular → Rezept noch ein Hin und Her.

// Pfad ohne Query und Anker ('/?q=x#y' → '/'); ungültig/leer → ''
export const pathOf = (fullPath) => {
  if (typeof fullPath !== 'string' || !fullPath) return ''
  return fullPath.split(/[?#]/)[0] || '/'
}

export const LIST_PATH = '/'

export const isListPath = (fullPath) => pathOf(fullPath) === LIST_PATH

// history.state.back (vue-router) gegen das Ziel: 'back' oder 'replace'
export const backAction = (stateBack, target) =>
  stateBack && pathOf(stateBack) === pathOf(target) ? 'back' : 'replace'

export const navigateBackTo = (router, target, stateBack = window.history.state?.back) =>
  backAction(stateBack, target) === 'back' ? router.back() : router.replace(target)

// Ziel nach Speichern bzw. Abbrechen im Rezeptformular: Rezeptseite, bei abgebrochener Neuanlage die Liste
export const formExitTarget = ({ isEdit, recipeId, savedId }) => {
  if (savedId != null) return `/recipe/${savedId}`
  return isEdit ? `/recipe/${recipeId}` : LIST_PATH
}

// Zurück-Pfeil der Kopfzeile: back = Zielpfad (Rezeptseite → immer zur Liste) oder true (vorige Seite, ohne
// Verlauf zur Liste)
export const headerBack = (router, back, stateBack = window.history.state?.back) => {
  if (typeof back === 'string') return navigateBackTo(router, back, stateBack)
  return stateBack ? router.back() : router.replace(LIST_PATH)
}
