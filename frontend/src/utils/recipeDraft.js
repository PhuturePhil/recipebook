import { emptyIngredient, formSnapshot } from './recipeFormData.js'

export const DRAFT_DELAY_MS = 2000

const PREFIX = 'recipebook:draft:'

// Per user and recipe, so a shared device never offers someone else's draft
export const draftKey = (userId, recipeId) => `${PREFIX}${userId ?? 'anonym'}:${recipeId ?? 'neu'}`

const isQuotaError = (error) =>
  error?.name === 'QuotaExceededError' || error?.name === 'NS_ERROR_DOM_QUOTA_REACHED' || error?.code === 22

// A resized photo can still be too big for localStorage; then the draft is kept without it rather than not at all.
// The field is left out entirely so restoring keeps the photo the form already shows.
export function saveDraft(storage, key, formData, now = Date.now()) {
  const write = (data) => storage.setItem(key, JSON.stringify({ savedAt: now, data }))
  try {
    write(formData)
    return true
  } catch (error) {
    if (!isQuotaError(error) || !String(formData.imageUrl ?? '').startsWith('data:')) return false
    const { imageUrl, ...withoutPhoto } = formData
    try {
      write(withoutPhoto)
      return true
    } catch {
      return false
    }
  }
}

export function loadDraft(storage, key) {
  let raw
  try {
    raw = storage.getItem(key)
  } catch {
    return null
  }
  if (!raw) return null
  try {
    const draft = JSON.parse(raw)
    if (typeof draft?.savedAt === 'number' && draft.data && typeof draft.data === 'object') return draft
  } catch {
    // unreadable draft, dropped below
  }
  clearDraft(storage, key)
  return null
}

export function clearDraft(storage, key) {
  try {
    storage.removeItem(key)
  } catch {
    // storage unavailable, nothing to clear
  }
}

// Only a draft that differs from what the form shows anyway is worth asking about
export const draftDiffers = (draft, formData) => !!draft && formSnapshot(restoreDraft(draft, formData)) !== formSnapshot(formData)

export function restoreDraft(draft, defaults) {
  const data = { ...defaults, ...draft.data }
  return {
    ...data,
    ingredients: Array.isArray(data.ingredients) && data.ingredients.length
      ? data.ingredients.map((i) => ({ ...emptyIngredient(), ...i }))
      : [emptyIngredient()],
    instructions: Array.isArray(data.instructions) && data.instructions.length
      ? data.instructions.map((i) => String(i ?? ''))
      : ['']
  }
}

// What the autosave does on each tick: keep changes, forget a draft once the form is back to its saved state,
// and leave an old draft alone as long as the user has not decided about it
export function autosaveAction({ dirty, draftPending }) {
  if (draftPending) return 'skip'
  return dirty ? 'save' : 'clear'
}

const pad = (n) => String(n).padStart(2, '0')

export function formatDraftTime(savedAt, now = Date.now()) {
  const saved = new Date(savedAt)
  const today = new Date(now)
  const time = `${pad(saved.getHours())}:${pad(saved.getMinutes())}`
  const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const days = Math.round((startOfDay(today) - startOfDay(saved)) / 86400000)
  if (days === 0) return `von heute, ${time}`
  if (days === 1) return `von gestern, ${time}`
  const year = saved.getFullYear() === today.getFullYear() ? '' : saved.getFullYear()
  return `vom ${pad(saved.getDate())}.${pad(saved.getMonth() + 1)}.${year}, ${time}`
}
