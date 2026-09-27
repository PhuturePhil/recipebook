// Helpers for the ingredient name field: suggestions while typing and the per-row nutrition hint (✓ / ?)

export const SUGGEST_MIN_LENGTH = 2
export const SUGGEST_LIMIT = 6
export const SUGGEST_DELAY_MS = 200
export const RECOGNIZE_DELAY_MS = 600

export const UNKNOWN_HINT = 'keine Nährwertdaten — wird per KI geschätzt'

const text = (value) => String(value ?? '').trim()

// The query sent to the server, or '' while the input is too short to be useful
export function suggestionQuery(name) {
  const query = text(name)
  return query.length >= SUGGEST_MIN_LENGTH ? query : ''
}

// Nothing to offer once the field already holds the only suggestion
export function visibleSuggestions(name, suggestions) {
  const list = Array.isArray(suggestions) ? suggestions : []
  const typed = text(name).toLowerCase()
  if (list.length === 1 && list[0].name.toLowerCase() === typed) return []
  return list
}

// Arrow keys walk through the list and wrap around; -1 means nothing is highlighted
export function moveHighlight(current, delta, length) {
  if (length <= 0) return -1
  if (current < 0) return delta > 0 ? 0 : length - 1
  return (current + delta + length) % length
}

// Recognition depends on the whole row (the unit can change how the name is read), so the row is the cache key
export function recognitionKey(ingredient) {
  const name = text(ingredient?.name)
  if (!name) return ''
  return JSON.stringify([text(ingredient?.amount), text(ingredient?.unit), name])
}

// Rows that still need a server answer, each distinct row once
export function pendingRecognition(ingredients, known) {
  const seen = new Set()
  const lines = []
  for (const ingredient of ingredients ?? []) {
    const key = recognitionKey(ingredient)
    if (!key || seen.has(key) || key in known) continue
    seen.add(key)
    const [amount, unit, name] = JSON.parse(key)
    lines.push({ key, line: { amount, unit, name } })
  }
  return lines
}

const SOURCE_LABELS = { AI_ESTIMATE: 'KI-Schätzung', MANUAL: 'manuell hinterlegt' }

export function recognitionHint(result) {
  if (!result) return null
  if (!result.recognized) return { state: 'unknown', symbol: '?', title: UNKNOWN_HINT }
  const detail = result.source === 'BLS'
    ? (result.referenceName ? `BLS: ${result.referenceName}` : 'BLS')
    : SOURCE_LABELS[result.source]
  const name = result.ingredientName || 'Zutat'
  return { state: 'ok', symbol: '✓', title: `Nährwerte erkannt: ${name}${detail ? ` – ${detail}` : ''}` }
}
