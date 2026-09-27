import { normalizeText } from './recipeSearch.js'

export const MAX_TAGS = 5
export const MAX_TAG_LENGTH = 30
export const TAG_SUGGEST_LIMIT = 8

export const cleanTag = (value) => {
  const tag = String(value ?? '').replace(/^[#\s]+/, '').replace(/\s+/g, ' ').trim().slice(0, MAX_TAG_LENGTH).trim()
  return tag ? tag.charAt(0).toUpperCase() + tag.slice(1) : ''
}

const sameTag = (a, b) => normalizeText(a) === normalizeText(b)

// Übernimmt die Schreibweise eines schon bekannten Tags; liefert false, wenn nichts hinzukam
export function addTag(tags, value, known = []) {
  const tag = cleanTag(value)
  if (!tag || tags.length >= MAX_TAGS || tags.some((t) => sameTag(t, tag))) return false
  tags.push(known.find((k) => sameTag(k, tag)) ?? tag)
  return true
}

export function tagSuggestions(known, current, query) {
  const q = normalizeText(cleanTag(query))
  const available = known.filter((k) => !current.some((t) => sameTag(t, k)))
  if (!q) return available.slice(0, TAG_SUGGEST_LIMIT)
  const starts = available.filter((k) => normalizeText(k).startsWith(q))
  const contains = available.filter((k) => !normalizeText(k).startsWith(q) && normalizeText(k).includes(q))
  return [...starts, ...contains].slice(0, TAG_SUGGEST_LIMIT)
}

export const tagSearchTerm = (tag) => `#${tag}`

export const TAG_BAR_LIMIT = 8

/**
 * Tags der gegebenen Rezepte mit Anzahl, häufigste zuerst (Gleichstand alphabetisch).
 * Schon als "#Tag" aktive Suchbegriffe fehlen, Schreibvarianten zählen als ein Tag.
 */
export function tagCounts(recipes, activeTerms = []) {
  const active = new Set(activeTerms.filter((t) => t.startsWith('#')).map((t) => normalizeText(t.slice(1))))
  const counts = new Map()
  for (const recipe of recipes) {
    const seen = new Set()
    for (const tag of recipe.tags ?? []) {
      const key = normalizeText(tag)
      if (!key || active.has(key) || seen.has(key)) continue
      seen.add(key)
      const entry = counts.get(key)
      if (entry) entry.count++
      else counts.set(key, { tag, count: 1 })
    }
  }
  return [...counts.values()].sort((a, b) => b.count - a.count || a.tag.localeCompare(b.tag, 'de'))
}

export const tagBarEntries = (entries, expanded, limit = TAG_BAR_LIMIT) => {
  const shown = expanded || entries.length <= limit ? entries : entries.slice(0, limit)
  return { shown, hidden: entries.length - shown.length }
}
