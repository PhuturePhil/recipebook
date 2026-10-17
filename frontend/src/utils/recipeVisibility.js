// „Sichtbar für“ im Rezeptformular: Privat / Familie (Mehrfachauswahl) / Öffentlich.
// Öffentlich schließt die Familien ein (die Haken bleiben gespeichert, das Rezept steht weiter in den Familien-Chips).
export const PRIVATE = 'PRIVATE'
export const FAMILY = 'FAMILY'
export const PUBLIC = 'PUBLIC'

// Beim Anlegen: alle eigenen Familien vorangehakt; ohne Familie privat
export function defaultVisibility(families) {
  const ids = (families ?? []).map((f) => f.id)
  return { visibility: ids.length ? FAMILY : PRIVATE, familyIds: ids }
}

// Beim Bearbeiten gilt der gespeicherte Stand
export function visibilityFromRecipe(recipe) {
  return { visibility: recipe?.visibility ?? null, familyIds: [...(recipe?.familyIds ?? [])] }
}

export function pickVisibility(state, value, families) {
  if (value === FAMILY && !state.familyIds.length) {
    return { visibility: FAMILY, familyIds: (families ?? []).map((f) => f.id) }
  }
  return { ...state, visibility: value }
}

// Den letzten Haken entfernen = privat
export function toggleFamily(state, id) {
  const on = state.familyIds.includes(id)
  const familyIds = on ? state.familyIds.filter((x) => x !== id) : [...state.familyIds, id]
  if (!familyIds.length) return { visibility: PRIVATE, familyIds }
  return { visibility: state.visibility === PUBLIC ? PUBLIC : FAMILY, familyIds }
}

// Was an den Server geht; ohne Wahl (Familien noch nicht geladen) entscheidet der Server (alle eigenen Familien)
export function visibilityPayload(visibility, familyIds) {
  if (!visibility) return {}
  return { visibility, familyIds: visibility === PRIVATE ? [] : [...(familyIds ?? [])].sort((a, b) => a - b) }
}
