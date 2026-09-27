import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  emptyIngredient,
  cleanRecipeData,
  isIngredientNameRequired,
  isInstructionRequired,
  addIngredientBelow,
  moveRow,
  preventsImplicitSubmit,
  formSnapshot,
  runSave
} from './recipeFormData.js'

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
    ingredients: [
      { name: 'Zwiebel', amount: '1', unit: '' },
      emptyIngredient(),
      { name: ' ', amount: '', unit: ' ' }
    ],
    instructions: ['Schneiden', '', '   ']
  })
  assert.deepEqual(data.ingredients, [{ name: 'Zwiebel', amount: '1', unit: '' }])
  assert.deepEqual(data.instructions, ['Schneiden'])
})

test('empty extra rows are not required, a started row needs a name', () => {
  const rows = [{ name: 'Zwiebel', amount: '', unit: '' }, emptyIngredient(), { name: '', amount: '200', unit: 'g' }]
  assert.equal(isIngredientNameRequired(rows, 0), true)
  assert.equal(isIngredientNameRequired(rows, 1), false)
  assert.equal(isIngredientNameRequired(rows, 2), true)
})

test('an entirely empty list still asks for one ingredient and one step', () => {
  const rows = [emptyIngredient(), emptyIngredient()]
  assert.equal(isIngredientNameRequired(rows, 0), true)
  assert.equal(isIngredientNameRequired(rows, 1), false)
  assert.equal(isInstructionRequired(['', ''], 0), true)
  assert.equal(isInstructionRequired(['', ''], 1), false)
  assert.equal(isInstructionRequired(['Schneiden', ''], 1), false)
})

test('adding an empty row does not count as an unsaved change, editing does', () => {
  const form = { title: 'Test', ingredients: [{ name: 'Zwiebel', amount: '1', unit: '' }], instructions: ['A'] }
  const snapshot = formSnapshot(form)
  form.ingredients.push(emptyIngredient())
  form.instructions.push('')
  assert.equal(formSnapshot(form), snapshot)
  form.ingredients[0].amount = '2'
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
