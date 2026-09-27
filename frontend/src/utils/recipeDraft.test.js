import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  draftKey,
  saveDraft,
  loadDraft,
  clearDraft,
  draftDiffers,
  restoreDraft,
  autosaveAction,
  formatDraftTime
} from './recipeDraft.js'
import { emptyIngredient } from './recipeFormData.js'

function memoryStorage({ quota = Infinity } = {}) {
  const items = new Map()
  return {
    items,
    getItem: (key) => (items.has(key) ? items.get(key) : null),
    setItem: (key, value) => {
      if (value.length > quota) {
        const error = new Error('quota')
        error.name = 'QuotaExceededError'
        throw error
      }
      items.set(key, String(value))
    },
    removeItem: (key) => items.delete(key)
  }
}

const emptyForm = () => ({
  title: '',
  description: '',
  baseServings: 4,
  servingsTo: null,
  prepTimeMinutes: null,
  imageUrl: '',
  author: '',
  source: '',
  page: '',
  sourceUrl: '',
  ingredients: [emptyIngredient()],
  instructions: ['']
})

test('drafts are kept per user and per recipe, new recipes under "neu"', () => {
  assert.equal(draftKey(3, undefined), 'recipebook:draft:3:neu')
  assert.equal(draftKey(3, 93), 'recipebook:draft:3:93')
  assert.notEqual(draftKey(3, 93), draftKey(4, 93))
})

test('a saved draft comes back with its time', () => {
  const storage = memoryStorage()
  const form = { ...emptyForm(), title: 'Dhansak' }
  assert.equal(saveDraft(storage, 'k', form, 1000), true)
  assert.deepEqual(loadDraft(storage, 'k'), { savedAt: 1000, data: form })
})

test('clearing removes the draft', () => {
  const storage = memoryStorage()
  saveDraft(storage, 'k', emptyForm(), 1)
  clearDraft(storage, 'k')
  assert.equal(loadDraft(storage, 'k'), null)
})

test('a broken draft is dropped instead of breaking the form', () => {
  const storage = memoryStorage()
  storage.setItem('k', '{kaputt')
  assert.equal(loadDraft(storage, 'k'), null)
  assert.equal(storage.items.has('k'), false)
  storage.setItem('k', JSON.stringify({ data: {} }))
  assert.equal(loadDraft(storage, 'k'), null)
})

test('a photo too big for the storage is left out, the rest is saved', () => {
  const storage = memoryStorage({ quota: 500 })
  const form = { ...emptyForm(), title: 'Mit Foto', imageUrl: `data:image/jpeg;base64,${'A'.repeat(1000)}` }
  assert.equal(saveDraft(storage, 'k', form, 5), true)
  const draft = loadDraft(storage, 'k')
  assert.equal(draft.data.title, 'Mit Foto')
  assert.equal('imageUrl' in draft.data, false)
  // restoring such a draft keeps the photo the form already shows
  const current = { ...emptyForm(), imageUrl: '/api/images/7' }
  assert.equal(restoreDraft(draft, current).imageUrl, '/api/images/7')
})

test('saving fails quietly when the storage refuses', () => {
  const storage = memoryStorage({ quota: 10 })
  assert.equal(saveDraft(storage, 'k', emptyForm(), 5), false)
  const broken = { getItem: () => { throw new Error('blocked') }, removeItem: () => { throw new Error('blocked') } }
  assert.equal(loadDraft(broken, 'k'), null)
  assert.doesNotThrow(() => clearDraft(broken, 'k'))
})

test('a draft is only offered when it differs from the form', () => {
  const form = emptyForm()
  assert.equal(draftDiffers(null, form), false)
  assert.equal(draftDiffers({ savedAt: 1, data: emptyForm() }, form), false)
  assert.equal(draftDiffers({ savedAt: 1, data: { ...emptyForm(), title: 'Neu' } }, form), true)
  // blank rows alone are no real difference
  assert.equal(draftDiffers({ savedAt: 1, data: { ...emptyForm(), instructions: ['', ''] } }, form), false)
})

test('restoring fills missing fields and never leaves the lists empty', () => {
  const restored = restoreDraft(
    { savedAt: 1, data: { title: 'Alt', ingredients: [{ name: 'Ei' }], instructions: [] } },
    emptyForm()
  )
  assert.equal(restored.title, 'Alt')
  assert.equal(restored.baseServings, 4)
  assert.deepEqual(restored.ingredients, [{ name: 'Ei', amount: '', unit: '' }])
  assert.deepEqual(restored.instructions, [''])
})

test('the link to the original is part of the draft; older drafts without it keep the form value', () => {
  const storage = memoryStorage()
  const form = { ...emptyForm(), sourceUrl: 'https://www.zeit.de/rezept' }
  saveDraft(storage, 'k', form, 1000)
  assert.equal(loadDraft(storage, 'k').data.sourceUrl, 'https://www.zeit.de/rezept')
  assert.equal(draftDiffers(loadDraft(storage, 'k'), emptyForm()), true)

  const { sourceUrl, ...older } = { ...emptyForm(), title: 'Alt' }
  const restored = restoreDraft({ savedAt: 1, data: older }, { ...emptyForm(), sourceUrl: 'https://biancazapatka.com/x' })
  assert.equal(restored.sourceUrl, 'https://biancazapatka.com/x')
})

test('autosave saves changes, forgets a draft once nothing is changed, and waits while a draft is offered', () => {
  assert.equal(autosaveAction({ dirty: true, draftPending: false }), 'save')
  assert.equal(autosaveAction({ dirty: false, draftPending: false }), 'clear')
  assert.equal(autosaveAction({ dirty: true, draftPending: true }), 'skip')
  assert.equal(autosaveAction({ dirty: false, draftPending: true }), 'skip')
})

test('the draft time reads naturally', () => {
  const now = new Date(2026, 8, 27, 18, 5).getTime()
  assert.equal(formatDraftTime(new Date(2026, 8, 27, 9, 7).getTime(), now), 'von heute, 09:07')
  assert.equal(formatDraftTime(new Date(2026, 8, 26, 23, 59).getTime(), now), 'von gestern, 23:59')
  assert.equal(formatDraftTime(new Date(2026, 8, 20, 14, 30).getTime(), now), 'vom 20.09., 14:30')
  assert.equal(formatDraftTime(new Date(2025, 11, 31, 8, 0).getTime(), now), 'vom 31.12.2025, 08:00')
})
