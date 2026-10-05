import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  languageChoice,
  applyLanguageChoice,
  autoLanguageLabel,
  emptyIngredient,
  cleanRecipeData,
  isIngredientNameRequired,
  isInstructionRequired,
  addIngredientBelow,
  moveRow,
  preventsImplicitSubmit,
  formSnapshot,
  runSave,
  addSection,
  dissolveSection,
  isSectionEmpty,
  removeIngredientAt,
  moveIngredientAcross,
  moveSection,
  ingredientsWithoutName,
  validateRecipeForm,
  firstError
} from './recipeFormData.js'
import { ingredientSection, ingredientsToSections } from './ingredientGroups.js'

const sec = (name, ...items) => ingredientSection(name, items)
const names = (sections) => sections.map((s) => [s.name, s.items.map((i) => i.name)])

const enter = (tagName, type = 'text', extra = {}) => ({ key: 'Enter', target: { tagName, type }, ...extra })

test('enter in a text input does not submit the form', () => {
  assert.equal(preventsImplicitSubmit(enter('INPUT')), true)
  assert.equal(preventsImplicitSubmit(enter('INPUT', 'number')), true)
})

test('enter keeps its normal meaning in textareas, on buttons and while composing', () => {
  assert.equal(preventsImplicitSubmit(enter('TEXTAREA', 'textarea')), false)
  assert.equal(preventsImplicitSubmit(enter('BUTTON', 'submit')), false)
  assert.equal(preventsImplicitSubmit(enter('INPUT', 'text', { isComposing: true })), false)
  assert.equal(preventsImplicitSubmit({ key: 'a', target: { tagName: 'INPUT', type: 'text' } }), false)
})

test('enter in an ingredient row inserts a new row directly below', () => {
  const rows = [
    { name: 'Zwiebel', amount: '1', unit: '' },
    { name: 'Tomaten', amount: '400', unit: 'g' }
  ]
  const focus = addIngredientBelow(rows, 0)
  assert.equal(focus, 1)
  assert.deepEqual(rows.map((r) => r.name), ['Zwiebel', '', 'Tomaten'])
})

test('enter reuses an empty row below instead of stacking empty rows', () => {
  const rows = [{ name: 'Zwiebel', amount: '', unit: '' }, emptyIngredient()]
  assert.equal(addIngredientBelow(rows, 0), 1)
  assert.equal(rows.length, 2)
})

test('empty extra ingredient rows and steps are dropped on submit', () => {
  const data = cleanRecipeData({
    title: 'Test',
    sections: [sec(null,
      { name: 'Zwiebel', amount: '1', unit: '' },
      emptyIngredient(),
      { name: ' ', amount: '', unit: ' ' }
    )],
    instructions: ['Schneiden', '', '   ']
  })
  assert.deepEqual(data.ingredients, [{ name: 'Zwiebel', amount: '1', unit: '' }])
  assert.deepEqual(data.instructions, ['Schneiden'])
  assert.equal('sections' in data, false)
})

test('empty extra rows are not required, a started row needs a name', () => {
  const sections = [sec(null, { name: 'Zwiebel', amount: '', unit: '' }, emptyIngredient()), sec('Salsa', { name: '', amount: '200', unit: 'g' })]
  assert.equal(isIngredientNameRequired(sections, 0, 0), true)
  assert.equal(isIngredientNameRequired(sections, 0, 1), false)
  assert.equal(isIngredientNameRequired(sections, 1, 0), true)
  assert.deepEqual(ingredientsWithoutName(sections), [[1, 0]])
})

test('an entirely empty list still asks for one ingredient and one step', () => {
  const sections = [sec(null), sec('Teig', emptyIngredient(), emptyIngredient())]
  assert.equal(isIngredientNameRequired(sections, 1, 0), true)
  assert.equal(isIngredientNameRequired(sections, 1, 1), false)
  assert.equal(isInstructionRequired(['', ''], 0), true)
  assert.equal(isInstructionRequired(['', ''], 1), false)
  assert.equal(isInstructionRequired(['Schneiden', ''], 1), false)
})

test('adding an empty row does not count as an unsaved change, editing does', () => {
  const form = { title: 'Test', sections: [sec(null, { name: 'Zwiebel', amount: '1', unit: '' })], instructions: ['A'] }
  const snapshot = formSnapshot(form)
  form.sections[0].items.push(emptyIngredient())
  addSection(form.sections)
  form.instructions.push('')
  assert.equal(formSnapshot(form), snapshot)
  form.sections[0].items[0].amount = '2'
  assert.notEqual(formSnapshot(form), snapshot)
})

test('a failed save keeps the error and ends the saving state', async () => {
  const state = { saving: false, error: '' }
  const outcome = await runSave(state, async () => {
    assert.equal(state.saving, true)
    throw new Error('Simulierter Fehler')
  })
  assert.equal(outcome.ok, false)
  assert.deepEqual(state, { saving: false, error: 'Simulierter Fehler' })
})

test('a successful save clears a previous error and returns the result', async () => {
  const state = { saving: false, error: 'alt' }
  const outcome = await runSave(state, async () => ({ id: 7 }))
  assert.deepEqual(outcome, { ok: true, result: { id: 7 } })
  assert.deepEqual(state, { saving: false, error: '' })
})

test('moving a row up or down swaps it with its neighbour and reports the new position', () => {
  const steps = ['Schneiden', 'Anbraten', 'Würzen']
  assert.equal(moveRow(steps, 2, -1), 1)
  assert.deepEqual(steps, ['Schneiden', 'Würzen', 'Anbraten'])
  assert.equal(moveRow(steps, 0, 1), 1)
  assert.deepEqual(steps, ['Würzen', 'Schneiden', 'Anbraten'])
})

test('moving keeps the row object itself', () => {
  const zwiebel = { name: 'Zwiebel', amount: '1', unit: '' }
  const rows = [{ name: 'Öl', amount: '2', unit: 'EL' }, zwiebel]
  moveRow(rows, 1, -1)
  assert.equal(rows[0], zwiebel)
})

test('rows at the edge do not move', () => {
  const rows = ['a', 'b']
  assert.equal(moveRow(rows, 0, -1), -1)
  assert.equal(moveRow(rows, 1, 1), -1)
  assert.equal(moveRow(rows, 5, -1), -1)
  assert.deepEqual(rows, ['a', 'b'])
})

test('language dropdown shows auto until a language is picked by hand', () => {
  const form = { language: 'en', languageAuto: true }
  assert.equal(languageChoice(form), 'auto')
  applyLanguageChoice(form, 'de')
  assert.deepEqual(form, { language: 'de', languageAuto: false })
  assert.equal(languageChoice(form), 'de')
  applyLanguageChoice(form, 'auto')
  assert.equal(form.languageAuto, true)
  assert.equal(languageChoice({ language: null, languageAuto: undefined }), 'auto')
})

test('auto option names the detected language', () => {
  assert.equal(autoLanguageLabel('en'), 'Automatisch (erkannt: Englisch)')
  assert.equal(autoLanguageLabel(null), 'Automatisch erkennen')
})

const validForm = (extra = {}) => ({
  title: 'Dal',
  baseServings: 4,
  servingsTo: null,
  prepTimeMinutes: null,
  sourceUrl: '',
  sections: [sec(null, { name: 'Linsen', amount: '200', unit: 'g' })],
  instructions: ['Kochen'],
  ...extra
})

test('a complete form has no errors', () => {
  assert.deepEqual(validateRecipeForm(validForm()), {})
  assert.deepEqual(validateRecipeForm(validForm({ servingsTo: '', prepTimeMinutes: '' })), {})
  assert.equal(firstError({}), null)
})

test('an empty form names every required field, first the title', () => {
  const errors = validateRecipeForm(validForm({
    title: '  ',
    baseServings: '',
    sections: [sec(null, emptyIngredient())],
    instructions: ['', ' ']
  }))
  assert.deepEqual(errors, {
    title: 'Bitte einen Titel eingeben.',
    baseServings: 'Bitte die Personenanzahl eingeben.',
    ingredients: 'Mindestens eine Zutat angeben.',
    instructions: 'Mindestens einen Arbeitsschritt angeben.'
  })
  assert.equal(firstError(errors), 'title')
  assert.equal(firstError({ instructions: 'x', ingredients: 'y' }), 'ingredients')
})

test('numbers are checked for a sensible range only when filled', () => {
  assert.match(validateRecipeForm(validForm({ baseServings: 0 })).baseServings, /1 bis 100/)
  assert.match(validateRecipeForm(validForm({ baseServings: 2.5 })).baseServings, /ganze Zahl/)
  assert.match(validateRecipeForm(validForm({ servingsTo: 3 })).servingsTo, /zwischen 4 und 100/)
  assert.equal(validateRecipeForm(validForm({ servingsTo: 6 })).servingsTo, undefined)
  assert.match(validateRecipeForm(validForm({ prepTimeMinutes: 0 })).prepTimeMinutes, /1 bis 10080/)
  assert.match(validateRecipeForm(validForm({ prepTimeMinutes: 10081 })).prepTimeMinutes, /1 bis 10080/)
  assert.equal(validateRecipeForm(validForm({ prepTimeMinutes: 45 })).prepTimeMinutes, undefined)
})

test('the link is checked like before, an empty link is fine', () => {
  assert.match(validateRecipeForm(validForm({ sourceUrl: 'kein link' })).sourceUrl, /https:\/\//)
  assert.equal(validateRecipeForm(validForm({ sourceUrl: 'https://www.zeit.de/x' })).sourceUrl, undefined)
})

test('an ingredient in a group counts, a started row without name does not pass', () => {
  assert.equal(validateRecipeForm(validForm({ sections: [sec(null), sec('Salsa', { name: 'Tomaten', amount: '', unit: '' })] })).ingredients, undefined)
  const errors = validateRecipeForm(validForm({
    sections: [sec(null, { name: 'Linsen', amount: '', unit: '' }, { name: '', amount: '2', unit: 'EL' })]
  }))
  assert.equal(errors.ingredients, 'Bitte bei jeder Zutat einen Namen eintragen.')
})

test('"+ Gruppe" adds an empty card with one row at the end', () => {
  const sections = [sec(null, { name: 'Salz' })]
  assert.equal(addSection(sections), 1)
  assert.equal(sections[1].name, '')
  assert.deepEqual(sections[1].items, [emptyIngredient()])
  assert.equal(isSectionEmpty(sections[1]), true)
  assert.notEqual(sections[0].key, sections[1].key)
})

test('dissolving a group hands its ingredients to the section above, empty rows are dropped', () => {
  const sections = [sec(null, { name: 'Salz' }), sec('Teig', { name: 'Mehl' }, emptyIngredient()), sec('Salsa', { name: 'Tomaten' })]
  assert.equal(dissolveSection(sections, 2), 1)
  assert.deepEqual(names(sections), [[null, ['Salz']], ['Teig', ['Mehl', '', 'Tomaten']]])
  assert.equal(dissolveSection(sections, 1), 0)
  assert.deepEqual(names(sections), [[null, ['Salz', 'Mehl', 'Tomaten']]])
  assert.equal(dissolveSection(sections, 0), -1)
})

test('dissolving the only filled section still leaves one row to type in', () => {
  const sections = [sec(null), sec('Teig', emptyIngredient())]
  dissolveSection(sections, 1)
  assert.deepEqual(names(sections), [[null, ['']]])
})

test('the last ingredient row cannot be removed, wherever it is', () => {
  const sections = [sec(null), sec('Teig', { name: 'Mehl' })]
  assert.equal(removeIngredientAt(sections, 1, 0), false)
  sections[0].items.push({ name: 'Salz' })
  assert.equal(removeIngredientAt(sections, 1, 0), true)
  assert.deepEqual(names(sections), [[null, ['Salz']], ['Teig', []]])
})

test('moving an ingredient crosses into the neighbouring section at the edge', () => {
  const sections = [sec(null, { name: 'Salz' }), sec('Teig', { name: 'Mehl' }, { name: 'Ei' }), sec('Salsa', { name: 'Tomaten' })]
  assert.deepEqual(moveIngredientAcross(sections, 1, 1, -1), [1, 0])
  assert.deepEqual(names(sections)[1], ['Teig', ['Ei', 'Mehl']])
  assert.deepEqual(moveIngredientAcross(sections, 1, 0, -1), [0, 1])
  assert.deepEqual(names(sections).slice(0, 2), [[null, ['Salz', 'Ei']], ['Teig', ['Mehl']]])
  assert.deepEqual(moveIngredientAcross(sections, 1, 0, 1), [2, 0])
  assert.deepEqual(names(sections).slice(1), [['Teig', []], ['Salsa', ['Mehl', 'Tomaten']]])
  assert.equal(moveIngredientAcross(sections, 0, 0, -1), null)
  assert.equal(moveIngredientAcross(sections, 2, 1, 1), null)
})

test('moving into an empty section and back keeps the item object', () => {
  const ei = { name: 'Ei' }
  const sections = [sec(null, ei), sec('Teig')]
  assert.deepEqual(moveIngredientAcross(sections, 0, 0, 1), [1, 0])
  assert.equal(sections[1].items[0], ei)
  assert.deepEqual(moveIngredientAcross(sections, 1, 0, -1), [0, 0])
  assert.equal(sections[0].items[0], ei)
})

test('group cards reorder among themselves, "no group" stays on top', () => {
  const sections = ingredientsToSections([{ name: 'Salz' }, { name: 'Mehl', groupName: 'Teig' }, { name: 'Tomaten', groupName: 'Salsa' }])
  assert.equal(moveSection(sections, 2, -1), 1)
  assert.deepEqual(sections.map((s) => s.name), [null, 'Salsa', 'Teig'])
  assert.equal(moveSection(sections, 1, -1), -1)
  assert.equal(moveSection(sections, 2, 1), -1)
  assert.equal(moveSection(sections, 0, 1), -1)
})
