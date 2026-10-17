// Sichtbarkeit eines Rezepts (vom Server aus Sicht des Betrachters geliefert) für Karten und Rezeptseite
export const VISIBILITY = { PRIVATE: 'PRIVATE', FAMILY: 'FAMILY', PUBLIC: 'PUBLIC' }

export function visibilityLabel(recipe) {
  if (!recipe?.visibility) return null
  if (recipe.visibility === VISIBILITY.PUBLIC) return { icon: 'globus', text: 'Öffentlich' }
  const names = (recipe.families ?? []).map((f) => f.name).filter(Boolean)
  if (recipe.visibility === VISIBILITY.FAMILY && names.length) return { icon: 'familie', text: names.join(', ') }
  return { icon: 'schloss', text: 'Privat' }
}

// „Silvia Hinze“ → „SH“, „Philipp“ → „P“
export function initials(name) {
  const parts = (name ?? '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return '?'
  const letters = parts.length > 1 ? [parts[0], parts[parts.length - 1]] : [parts[0]]
  return letters.map((p) => p[0].toLocaleUpperCase('de-DE')).join('')
}
