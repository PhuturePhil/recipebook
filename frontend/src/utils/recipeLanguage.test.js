import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  LANGUAGE_PREF_KEY,
  readLanguagePreference,
  writeLanguagePreference,
  sharedLanguage,
  wantsTranslation,
  applyTranslation,
  shareUrl,
} from './recipeLanguage.js'
import { scaleIngredients } from './scaleIngredients.js'

const memoryStorage = (initial = {}) => {
  const data = { ...initial }
  return {
    getItem: (k) => (k in data ? data[k] : null),
    setItem: (k, v) => { data[k] = String(v) },
    data,
  }
}

const english = {
  id: 92,
  language: 'en',
  title: 'Turkish green beans',
  description: '',
  baseServings: 4,
  ingredients: [
    { id: 1, amount: '1', unit: 'cup', name: 'olive oil' },
    { id: 2, amount: '400', unit: 'g', name: 'runner beans' },
  ],
  instructions: ['Cook.'],
}

const translation = {
  status: 'translated',
  title: 'Türkische grüne Bohnen',
  description: null,
  ingredients: [
    { amount: '240', unit: 'ml', name: 'Olivenöl' },
    { amount: '400', unit: 'g', name: 'Stangenbohnen' },
  ],
  instructions: ['Garen.'],
}

test('preference defaults to German and survives broken storage', () => {
  assert.equal(readLanguagePreference(memoryStorage()), 'de')
  assert.equal(readLanguagePreference(memoryStorage({ [LANGUAGE_PREF_KEY]: 'original' })), 'original')
  assert.equal(readLanguagePreference(memoryStorage({ [LANGUAGE_PREF_KEY]: 'fr' })), 'de')
  assert.equal(readLanguagePreference({ getItem: () => { throw new Error('blocked') } }), 'de')
})

test('only valid choices are written', () => {
  const storage = memoryStorage()
  writeLanguagePreference('original', storage)
  assert.equal(storage.data[LANGUAGE_PREF_KEY], 'original')
  writeLanguagePreference('xx', storage)
  assert.equal(storage.data[LANGUAGE_PREF_KEY], 'original')
  assert.doesNotThrow(() => writeLanguagePreference('de', { setItem: () => { throw new Error('full') } }))
})

test('a language in the share link wins over the device preference', () => {
  const storage = memoryStorage({ [LANGUAGE_PREF_KEY]: 'original' })
  assert.equal(sharedLanguage('de', storage), 'de')
  assert.equal(sharedLanguage(undefined, storage), 'original')
  assert.equal(sharedLanguage('xx', memoryStorage()), 'de')
})

test('only English recipes are translated, and only when German is chosen', () => {
  assert.equal(wantsTranslation(english, 'de'), true)
  assert.equal(wantsTranslation(english, 'original'), false)
  assert.equal(wantsTranslation({ ...english, language: 'de' }, 'de'), false)
  assert.equal(wantsTranslation(null, 'de'), false)
})

test('translation replaces texts but keeps ids, servings and the original object', () => {
  const shown = applyTranslation(english, translation)
  assert.equal(shown.title, 'Türkische grüne Bohnen')
  assert.equal(shown.description, '')
  assert.deepEqual(shown.ingredients[0], { id: 1, amount: '240', unit: 'ml', name: 'Olivenöl' })
  assert.deepEqual(shown.instructions, ['Garen.'])
  assert.equal(shown.baseServings, 4)
  assert.equal(english.title, 'Turkish green beans')
  assert.equal(english.ingredients[0].unit, 'cup')
})

test('failed or original answers leave the recipe as it is', () => {
  assert.equal(applyTranslation(english, { ...translation, status: 'unavailable' }), english)
  assert.equal(applyTranslation(english, null), english)
})

test('servings scale the translated metric amounts', () => {
  const shown = applyTranslation(english, translation)
  const scaled = scaleIngredients(shown.ingredients, shown.baseServings, 2)
  assert.equal(scaled[0].amount, '120')
  assert.equal(scaled[0].unit, 'ml')
  assert.equal(scaled[1].amount, '200')
})

test('share links of English recipes carry the chosen language', () => {
  assert.equal(shareUrl('https://pastoors.cloud/share/abc', english, 'de'), 'https://pastoors.cloud/share/abc?lang=de')
  assert.equal(shareUrl('https://pastoors.cloud/share/abc', english, 'original'), 'https://pastoors.cloud/share/abc?lang=original')
  assert.equal(shareUrl('https://pastoors.cloud/share/abc', { language: 'de' }, 'de'), 'https://pastoors.cloud/share/abc')
})
