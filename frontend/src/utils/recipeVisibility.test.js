import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  FAMILY, PRIVATE, PUBLIC,
  defaultVisibility, visibilityFromRecipe, pickVisibility, toggleFamily, visibilityPayload,
} from './recipeVisibility.js'
import { visibilityLabel, initials } from './visibility.js'
import { cleanRecipeData, formSnapshot } from './recipeFormData.js'
import { ingredientSection } from './ingredientGroups.js'

const families = [{ id: 1, name: 'Familie Pastoors' }, { id: 2, name: 'Kochclub Köln' }]

test('neues Rezept: alle eigenen Familien vorangehakt, ohne Familie privat', () => {
  assert.deepEqual(defaultVisibility(families), { visibility: FAMILY, familyIds: [1, 2] })
  assert.deepEqual(defaultVisibility([]), { visibility: PRIVATE, familyIds: [] })
})

test('Bearbeiten übernimmt den gespeicherten Stand', () => {
  assert.deepEqual(visibilityFromRecipe({ visibility: PUBLIC, familyIds: [2] }), { visibility: PUBLIC, familyIds: [2] })
  assert.deepEqual(visibilityFromRecipe({}), { visibility: null, familyIds: [] })
})

test('Familie wählen hakt alle an, wenn noch keine gewählt ist', () => {
  assert.deepEqual(pickVisibility({ visibility: PRIVATE, familyIds: [] }, FAMILY, families), { visibility: FAMILY, familyIds: [1, 2] })
  assert.deepEqual(pickVisibility({ visibility: PRIVATE, familyIds: [2] }, FAMILY, families), { visibility: FAMILY, familyIds: [2] })
  assert.deepEqual(pickVisibility({ visibility: FAMILY, familyIds: [2] }, PUBLIC, families), { visibility: PUBLIC, familyIds: [2] })
})

test('letzten Haken entfernen macht das Rezept privat', () => {
  const one = toggleFamily({ visibility: FAMILY, familyIds: [1, 2] }, 2)
  assert.deepEqual(one, { visibility: FAMILY, familyIds: [1] })
  assert.deepEqual(toggleFamily(one, 1), { visibility: PRIVATE, familyIds: [] })
  assert.deepEqual(toggleFamily(one, 2), { visibility: FAMILY, familyIds: [1, 2] })
})

test('an den Server: privat ohne Familien, ohne Wahl gar nichts', () => {
  assert.deepEqual(visibilityPayload(PRIVATE, [1, 2]), { visibility: PRIVATE, familyIds: [] })
  assert.deepEqual(visibilityPayload(FAMILY, [2, 1]), { visibility: FAMILY, familyIds: [1, 2] })
  assert.deepEqual(visibilityPayload(PUBLIC, [1]), { visibility: PUBLIC, familyIds: [1] })
  assert.deepEqual(visibilityPayload(null, []), {})
})

test('Formulardaten tragen die Sichtbarkeit mit; Privat ignoriert alte Haken', () => {
  const form = {
    title: 'Suppe',
    sections: [ingredientSection(null, [{ name: 'Wasser', amount: '1', unit: 'l' }])],
    instructions: ['Kochen'],
    visibility: FAMILY,
    familyIds: [1],
  }
  assert.deepEqual(cleanRecipeData(form).familyIds, [1])
  assert.equal(cleanRecipeData(form).visibility, FAMILY)
  assert.equal(formSnapshot({ ...form, visibility: PRIVATE, familyIds: [1] }), formSnapshot({ ...form, visibility: PRIVATE, familyIds: [] }))
  assert.equal('visibility' in cleanRecipeData({ ...form, visibility: null }), false)
})

test('Sichtbarkeit auf Karte und Rezeptseite', () => {
  assert.deepEqual(visibilityLabel({ visibility: PUBLIC, families }), { icon: 'globus', text: 'Öffentlich' })
  assert.deepEqual(visibilityLabel({ visibility: FAMILY, families }), { icon: 'familie', text: 'Familie Pastoors, Kochclub Köln' })
  assert.deepEqual(visibilityLabel({ visibility: PRIVATE, families: [] }), { icon: 'schloss', text: 'Privat' })
  assert.equal(visibilityLabel({}), null)
  assert.equal(initials('Silvia Hinze'), 'SH')
  assert.equal(initials('Anna Maria Pastoors'), 'AP')
  assert.equal(initials('philipp'), 'P')
  assert.equal(initials(''), '?')
})
