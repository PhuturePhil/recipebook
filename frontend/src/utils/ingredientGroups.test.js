import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  groupAt,
  groupRow,
  groupSections,
  hasGroups,
  ingredientsToRows,
  isGroupRow,
  parseGroupHeading,
  rowsToIngredients
} from './ingredientGroups.js'
import { parseIngredientText, ingredientsToText, ingredientsFromText, insertPastedIngredients } from './ingredientText.js'
import { addGroupRow, cleanRecipeData, emptyIngredient, isBlankIngredient, isIngredientNameRequired } from './recipeFormData.js'
import { restoreDraft } from './recipeDraft.js'
import { applyTranslation } from './recipeLanguage.js'
import { scaleIngredients } from './scaleIngredients.js'

const ing = (name, groupName, extra = {}) => ({ name, amount: '1', unit: '', ...extra, ...(groupName ? { groupName } : {}) })

const quesadillas = [
  ing('Tortillas', 'Quesadillas', { id: 1 }),
  ing('Käse', 'Quesadillas', { id: 2 }),
  ing('Schwarze Bohnen', 'Schwarze-Bohnen-Paste', { id: 3 }),
  ing('Tomaten', 'Salsa', { id: 4 }),
  ing('Koriander', 'Salsa', { id: 5 })
]

test('API ingredients become rows with one heading per group and back', () => {
  const rows = ingredientsToRows(quesadillas)
  assert.deepEqual(rows.map((r) => (isGroupRow(r) ? `# ${r.group}` : r.name)), [
    '# Quesadillas', 'Tortillas', 'Käse', '# Schwarze-Bohnen-Paste', 'Schwarze Bohnen', '# Salsa', 'Tomaten', 'Koriander'
  ])
  assert.equal('groupName' in rows[1], false)
  assert.deepEqual(rowsToIngredients(rows), quesadillas)
})

test('recipes without groups get no heading rows and no groupName', () => {
  const plain = [ing('Linsen'), ing('Reis')]
  assert.deepEqual(ingredientsToRows(plain), plain)
  assert.deepEqual(rowsToIngredients(plain), plain)
  assert.equal(hasGroups(plain), false)
  assert.equal(hasGroups(quesadillas), true)
})

test('ungrouped rows after a group keep "no group" through an empty heading', () => {
  const list = [ing('Tortillas', 'Teig'), ing('Salz')]
  const rows = ingredientsToRows(list)
  assert.deepEqual(rows[2], groupRow(''))
  assert.deepEqual(rowsToIngredients(rows), list)
})

test('group names are trimmed, a blank heading means no group', () => {
  const rows = [groupRow('  Salsa '), ing('Tomaten'), groupRow('   '), ing('Salz')]
  assert.deepEqual(rowsToIngredients(rows), [ing('Tomaten', 'Salsa'), ing('Salz')])
})

test('deleting a heading hands its rows to the group above, or to none', () => {
  const rows = ingredientsToRows(quesadillas)
  rows.splice(3, 1)
  assert.deepEqual(rowsToIngredients(rows).map((i) => i.groupName), [
    'Quesadillas', 'Quesadillas', 'Quesadillas', 'Salsa', 'Salsa'
  ])
  const first = ingredientsToRows(quesadillas)
  first.splice(0, 1)
  assert.deepEqual(rowsToIngredients(first).map((i) => i.groupName ?? null), [
    null, null, 'Schwarze-Bohnen-Paste', 'Salsa', 'Salsa'
  ])
})

test('groupAt finds the heading that applies to a row', () => {
  const rows = ingredientsToRows(quesadillas)
  assert.equal(groupAt(rows, 2), 'Quesadillas')
  assert.equal(groupAt(rows, 7), 'Salsa')
  assert.equal(groupAt([ing('Salz')], 0), '')
})

test('display sections follow consecutive groups', () => {
  const sections = groupSections([ing('Salz'), ...quesadillas.slice(3), ing('Brot')])
  assert.deepEqual(sections.map((s) => [s.group, s.items.length]), [[null, 1], ['Salsa', 2], [null, 1]])
  assert.deepEqual(groupSections([]), [])
})

test('heading lines are recognised in the usual spellings', () => {
  const cases = [
    ['Salsa:', 'Salsa'],
    ['Für die Salsa:', 'Für die Salsa'],
    ['### Salsa', 'Salsa'],
    ['## Für den Teig ##', 'Für den Teig'],
    ['**Salsa**', 'Salsa'],
    ['**Für die Salsa:**', 'Für die Salsa'],
    ['**Salsa**:', 'Salsa'],
    ['__Dressing__', 'Dressing'],
    ['For the dressing:', 'For the dressing'],
    ['Zutaten:', ''],
    ['Zutaten für 4 Personen:', ''],
    ['## Ingredients', ''],
    ['200 g Zwiebeln', null],
    ['Salz und Pfeffer', null],
    ['Deko: Minze', null],
    ['Eine sehr lange Zeile die mit einem Doppelpunkt endet und sicher keine Überschrift ist:', null]
  ]
  for (const [line, expected] of cases) {
    assert.equal(parseGroupHeading(line), expected, line)
  }
  assert.equal(parseGroupHeading('1 TL Salz:', (t) => /^\d/.test(t)), null)
})

test('pasted list with headings becomes groups, list titles are dropped', () => {
  const rows = parseIngredientText([
    'Zutaten für 4 Personen:',
    'Quesadillas:',
    '8 Tortillas',
    '',
    '### Schwarze-Bohnen-Paste',
    '1 Dose schwarze Bohnen',
    '- **Für die Salsa:**',
    '2 Tomaten',
    '1 TL Salz:'
  ].join('\n'))
  assert.deepEqual(rows, [
    groupRow('Quesadillas'),
    { amount: '8', unit: '', name: 'Tortillas' },
    groupRow('Schwarze-Bohnen-Paste'),
    { amount: '1', unit: 'Dose', name: 'schwarze Bohnen' },
    groupRow('Für die Salsa'),
    { amount: '2', unit: '', name: 'Tomaten' },
    { amount: '1', unit: 'TL', name: 'Salz:' }
  ])
})

test('text export shows group headings and reads back to the same groups', () => {
  const rows = ingredientsToRows(quesadillas)
  const text = ingredientsToText(rows)
  assert.equal(text, [
    'Quesadillas:', '1 Tortillas', '1 Käse', '', 'Schwarze-Bohnen-Paste:', '1 Schwarze Bohnen', '', 'Salsa:', '1 Tomaten',
    '1 Koriander'
  ].join('\n'))
  const back = ingredientsFromText(text, rows)
  assert.deepEqual(rowsToIngredients(back), quesadillas)
})

test('text export without groups is unchanged', () => {
  assert.equal(ingredientsToText([ing('Linsen'), emptyIngredient(), ing('Reis')]), '1 Linsen\n1 Reis')
})

test('pasting a grouped list in the middle keeps the group of the rows below', () => {
  const rows = ingredientsToRows([ing('Tortillas', 'Teig'), ing('Mehl', 'Teig')])
  const last = insertPastedIngredients(rows, 1, 'Salsa:\n2 Tomaten\n1 Limette')
  assert.equal(last, 4)
  assert.deepEqual(rowsToIngredients(rows).map((i) => [i.name, i.groupName ?? null]), [
    ['Tortillas', 'Teig'], ['Tomaten', 'Salsa'], ['Limette', 'Salsa'], ['Mehl', 'Teig']
  ])
})

test('pasting into an empty last row needs no extra heading', () => {
  const rows = [ing('Salz'), emptyIngredient()]
  insertPastedIngredients(rows, 1, 'Salsa:\n2 Tomaten')
  assert.deepEqual(rows.map((r) => (isGroupRow(r) ? `# ${r.group}` : r.name)), ['Salz', '# Salsa', 'Tomaten'])
})

test('"+ Gruppe" reuses an empty last row, otherwise adds one under the heading', () => {
  const rows = [ing('Salz'), emptyIngredient()]
  assert.equal(addGroupRow(rows), 1)
  assert.deepEqual(rows[1], groupRow())
  assert.equal(rows.length, 3)
  const full = [ing('Salz')]
  assert.equal(addGroupRow(full), 1)
  assert.deepEqual(full.slice(1), [groupRow(), emptyIngredient()])
})

test('headings are not blank rows, never required, and survive submit and draft restore', () => {
  const rows = [groupRow('Salsa'), emptyIngredient()]
  assert.equal(isBlankIngredient(rows[0]), false)
  assert.equal(isIngredientNameRequired(rows, 0), false)
  assert.equal(isIngredientNameRequired(rows, 1), true)

  const data = cleanRecipeData({ title: 'T', ingredients: [groupRow('Salsa'), ing('Tomaten'), emptyIngredient()], instructions: [] })
  assert.deepEqual(data.ingredients, [ing('Tomaten', 'Salsa')])

  const restored = restoreDraft({ savedAt: 1, data: { ingredients: [groupRow('Salsa'), { name: 'Tomaten' }] } }, {})
  assert.deepEqual(restored.ingredients, [groupRow('Salsa'), { name: 'Tomaten', amount: '', unit: '' }])
})

test('scaling and translation keep or replace group names', () => {
  const scaled = scaleIngredients([ing('Tomaten', 'Salsa', { amount: '2' })], 4, 8)
  assert.equal(scaled[0].groupName, 'Salsa')
  assert.equal(scaled[0].amount, '4')

  const recipe = { title: 'Quesadillas', ingredients: [ing('tomatoes', 'Salsa'), ing('salt')] }
  const translated = applyTranslation(recipe, {
    status: 'translated',
    title: 'Quesadillas',
    ingredients: [
      { amount: '1', unit: '', name: 'Tomaten', group: 'Salsa (DE)' },
      { amount: '1', unit: '', name: 'Salz', group: null }
    ],
    instructions: []
  })
  assert.deepEqual(translated.ingredients.map((i) => i.groupName ?? null), ['Salsa (DE)', null])
})
