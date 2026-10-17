// Kleine Helfer rund um Familien (Chips, Avatare, Sammlungsfilter) – ohne Store, damit testbar
export const MAX_MEMBERS = 10

// Sammlung über der Liste: 'alle', 'meine' oder die id einer Familie
export const SCOPE_ALL = 'alle'
export const SCOPE_MINE = 'meine'

// Kurzname für Chips: „Familie Pastoors“ → „Pastoors“
export const shortFamilyName = (name) => (name ?? '').replace(/^Familie\s+/i, '').trim() || name || ''

// Initialen-Avatar: Farbe fest je id (6 Pastelltöne, hell/dunkel in familien.css)
export const avatarTone = (id) => `mini-avatar--t${((Number(id) || 0) % 6 + 6) % 6}`

export function scopeRecipes(recipes, scope, viewerId) {
  if (scope === SCOPE_MINE) return recipes.filter((r) => r.ownerId != null && r.ownerId === viewerId)
  if (scope == null || scope === SCOPE_ALL) return recipes
  const familyId = Number(scope)
  return recipes.filter((r) => (r.families ?? []).some((f) => f.id === familyId))
}

// Familie gründen: so viele eigene Rezepte kämen mit (Häkchen „Alle meine Rezepte … freigeben“)
export const ownRecipeCount = (recipes, viewerId) =>
  viewerId == null ? 0 : scopeRecipes(recipes ?? [], SCOPE_MINE, viewerId).length

export const shareOwnLabel = (n) =>
  `Alle meine Rezepte für diese Familie freigeben (${n} ${n === 1 ? 'Rezept' : 'Rezepte'})`

export const normalizeScope = (scope) =>
  scope === SCOPE_MINE || scope === SCOPE_ALL ? scope : (Number(scope) || SCOPE_ALL)
