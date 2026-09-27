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
