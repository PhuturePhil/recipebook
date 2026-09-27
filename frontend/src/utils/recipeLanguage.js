// Which version of English recipes to show: the stored German translation ('de', default) or the 'original'.
// The choice is global and kept on the device.
export const LANGUAGE_PREF_KEY = 'recipeLanguage'
export const GERMAN = 'de'
export const ORIGINAL = 'original'

const valid = (value) => value === GERMAN || value === ORIGINAL

export function readLanguagePreference(storage = globalThis.localStorage) {
  try {
    const value = storage?.getItem(LANGUAGE_PREF_KEY)
    return valid(value) ? value : GERMAN
  } catch {
    return GERMAN
  }
}

export function writeLanguagePreference(value, storage = globalThis.localStorage) {
  if (!valid(value)) return
  try {
    storage?.setItem(LANGUAGE_PREF_KEY, value)
  } catch {
    // private mode or full storage: the choice then only lasts for this page
  }
}

// A share link may carry ?lang=de|original; otherwise the device preference decides
export function sharedLanguage(queryLang, storage = globalThis.localStorage) {
  return valid(queryLang) ? queryLang : readLanguagePreference(storage)
}

export const isTranslatable = (recipe) => recipe?.language === 'en'

export const wantsTranslation = (recipe, preference) => isTranslatable(recipe) && preference === GERMAN

// Overlays the translated texts on the original recipe. Ingredient rows keep their id and position,
// so scaling and nutrition keep working with the translated amounts.
export function applyTranslation(recipe, translation) {
  if (!recipe || translation?.status !== 'translated') return recipe
  const lines = translation.ingredients ?? []
  return {
    ...recipe,
    title: translation.title,
    description: translation.description ?? '',
    ingredients: (recipe.ingredients ?? []).map((ingredient, i) =>
      lines[i] ? { ...ingredient, amount: lines[i].amount ?? '', unit: lines[i].unit ?? '', name: lines[i].name } : ingredient
    ),
    instructions: translation.instructions ?? recipe.instructions,
  }
}

export function shareUrl(url, recipe, preference) {
  if (!url || !isTranslatable(recipe)) return url
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}lang=${preference === ORIGINAL ? ORIGINAL : GERMAN}`
}
