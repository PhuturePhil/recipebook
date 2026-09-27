export const FUZZY_MIN_LENGTH = 5

const TIME_REGEX = /^([<>])\s*(\d+)$/
const EXCLUDE_PREFIX = /^(?:-\s*|ohne\s+)/i

export const SORT_OPTIONS = [
  { value: 'default', label: 'Standard', short: '' },
  { value: 'newest', label: 'Neueste zuerst', short: 'Neu' },
  { value: 'prepTime', label: 'Zubereitungszeit', short: 'Zeit' },
  { value: 'kcal', label: 'kcal pro Portion', short: 'kcal' },
]

export const normalizeText = (text) =>
  String(text ?? '')
    .toLowerCase()
    .replace(/ß/g, 'ss')
    .replace(/æ/g, 'ae')
    .replace(/œ/g, 'oe')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ')
    .trim()

export const parseTerm = (rawTerm) => {
  const trimmed = String(rawTerm ?? '').trim()
  if (/^ohne$/i.test(trimmed)) return null
  const prefix = trimmed.match(EXCLUDE_PREFIX)
  const exclude = Boolean(prefix)
  const body = exclude ? trimmed.slice(prefix[0].length).trim() : trimmed
  if (!body) return null
  const time = body.match(TIME_REGEX)
  if (time) return { kind: 'time', exclude, op: time[1], minutes: parseInt(time[2], 10) }
  return { kind: 'text', exclude, text: normalizeText(body) }
}

// Kleinste Tippfehler-Distanz (Einfügen, Löschen, Ersetzen, Buchstabendreher) des Begriffs zu irgendeiner Stelle im Text
export const fuzzyDistance = (needle, haystack) => {
  const m = needle.length
  const n = haystack.length
  if (!m) return 0
  let prev2 = null
  let prev = Array.from({ length: m + 1 }, (_, i) => i)
  let best = prev[m]
  for (let j = 1; j <= n; j++) {
    const cur = [0]
    for (let i = 1; i <= m; i++) {
      const cost = needle[i - 1] === haystack[j - 1] ? 0 : 1
      let value = Math.min(prev[i] + 1, cur[i - 1] + 1, prev[i - 1] + cost)
      if (prev2 && i > 1 && needle[i - 1] === haystack[j - 2] && needle[i - 2] === haystack[j - 1]) {
        value = Math.min(value, prev2[i - 2] + 1)
      }
      cur.push(value)
    }
    best = Math.min(best, cur[m])
    prev2 = prev
    prev = cur
  }
  return best
}

export const fuzzyIncludes = (haystack, needle) =>
  haystack.includes(needle) || (needle.length >= FUZZY_MIN_LENGTH && fuzzyDistance(needle, haystack) <= 1)

// "#tag" trifft nur Rezepte mit genau diesem Tag; alles andere ist kein Tag-Begriff (undefined)
export const matchTag = (recipe, text) => {
  if (!text.startsWith('#')) return undefined
  const tag = text.slice(1).trim()
  return (recipe.tags ?? []).some((t) => normalizeText(t) === tag)
}

const matchesTime = (recipe, term) => {
  const prep = recipe.prepTimeMinutes ?? null
  if (prep === null) return false
  return term.op === '<' ? prep < term.minutes : prep > term.minutes
}

/**
 * Filtert die Rezeptliste. Begriffe sind UND-verknüpft; "-x" und "ohne x" schließen aus.
 * matchKeyword(recipe, normalizedText) liefert true/false für Schlüsselwörter wie Badges oder undefined für Freitext.
 * Tippfehler-Toleranz nur für Begriffe, die wörtlich in keinem Rezept vorkommen, und nie beim Ausschließen.
 */
export const filterRecipes = (recipes, rawTerms, { searchText, matchKeyword = () => undefined }) => {
  const terms = rawTerms.map(parseTerm).filter(Boolean)
  if (!terms.length) return recipes
  const texts = new Map(recipes.map((r) => [r.id, searchText(r)]))
  const fuzzy = new Set(
    terms.filter((t) => t.kind === 'text' && !t.exclude
      && ![...texts.values()].some((text) => text.includes(t.text))),
  )
  return recipes.filter((recipe) =>
    terms.every((term) => {
      let hit
      if (term.kind === 'time') {
        hit = matchesTime(recipe, term)
      } else {
        hit = matchKeyword(recipe, term.text)
        if (hit === undefined) {
          const text = texts.get(recipe.id)
          hit = fuzzy.has(term) ? fuzzyIncludes(text, term.text) : text.includes(term.text)
        }
      }
      return term.exclude ? !hit : hit
    }),
  )
}

const ascendingNullsLast = (value) => (a, b) => {
  const va = value(a)
  const vb = value(b)
  if (va == null && vb == null) return 0
  if (va == null) return 1
  if (vb == null) return -1
  return va - vb
}

export const sortRecipes = (recipes, mode) => {
  if (mode === 'newest') return [...recipes].sort((a, b) => b.id - a.id)
  if (mode === 'prepTime') return [...recipes].sort(ascendingNullsLast((r) => r.prepTimeMinutes))
  if (mode === 'kcal') return [...recipes].sort(ascendingNullsLast((r) => r.nutrition?.perServing?.kcal))
  return recipes
}
