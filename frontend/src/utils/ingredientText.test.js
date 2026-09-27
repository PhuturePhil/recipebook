import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  parseIngredientLine,
  parseIngredientText,
  ingredientsToText,
  ingredientsFromText,
  insertPastedIngredients,
  isMultiLinePaste
} from './ingredientText.js'
import { emptyIngredient } from './recipeFormData.js'

const row = (amount, unit, name) => ({ amount, unit, name })

const lines = [
  ['200 g Zwiebeln', row('200', 'g', 'Zwiebeln')],
  ['250g Mehl', row('250', 'g', 'Mehl')],
  ['500ml Gemüsebrühe', row('500', 'ml', 'Gemüsebrühe')],
  ['1,5 kg Kartoffeln', row('1,5', 'kg', 'Kartoffeln')],
  ['0.5 l Milch', row('0.5', 'l', 'Milch')],
  ['1/2 TL Salz', row('1/2', 'TL', 'Salz')],
  ['½ Bund Petersilie', row('½', 'Bund', 'Petersilie')],
  ['1½ TL Kreuzkümmel', row('1½', 'TL', 'Kreuzkümmel')],
  ['1 ½ TL Kurkuma', row('1 ½', 'TL', 'Kurkuma')],
  ['1 1/2 Tassen Reis', row('1 1/2', 'Tassen', 'Reis')],
  ['¼ l Sahne', row('¼', 'l', 'Sahne')],
  ['1-2 EL Olivenöl', row('1-2', 'EL', 'Olivenöl')],
  ['1 – 2 EL Olivenöl', row('1–2', 'EL', 'Olivenöl')],
  ['200 bis 250 g Mehl', row('200 bis 250', 'g', 'Mehl')],
  ['ca. 200 g Feta', row('ca. 200', 'g', 'Feta')],
  ['1 Prise Salz', row('1', 'Prise', 'Salz')],
  ['2 Prisen Zucker', row('2', 'Prisen', 'Zucker')],
  ['1 Msp. Muskat', row('1', 'Msp', 'Muskat')],
  ['1 TL. Zimt', row('1', 'TL', 'Zimt')],
  ['2 Zehen Knoblauch, gehackt', row('2', 'Zehen', 'Knoblauch, gehackt')],
  ['1 kleine Dose Tomaten', row('1', 'kleine Dose', 'Tomaten')],
  ['2 gehäufte EL Mehl', row('2', 'gehäufte EL', 'Mehl')],
  ['1 Dose (400 g) Kichererbsen', row('1', 'Dose', 'Kichererbsen (400 g)')],
  ['2 Eier (Größe M)', row('2', '', 'Eier (Größe M)')],
  ['3 Knoblauchzehen', row('3', '', 'Knoblauchzehen')],
  ['2 große Zwiebeln', row('2', '', 'große Zwiebeln')],
  ['2 x Eier', row('2', '', 'Eier')],
  ['Salz und Pfeffer', row('', '', 'Salz und Pfeffer')],
  ['Olivenöl zum Braten', row('', '', 'Olivenöl zum Braten')],
  ['etwas Zitronensaft', row('', '', 'etwas Zitronensaft')],
  ['3er Pack Eier', row('', '', '3er Pack Eier')],
  ['- 400 ml Kokosmilch', row('400', 'ml', 'Kokosmilch')],
  ['• 1 Bund Koriander', row('1', 'Bund', 'Koriander')],
  ['  2   EL   Sojasauce  ', row('2', 'EL', 'Sojasauce')],
  ['2 cups flour', row('2', 'cups', 'flour')],
  ['1 Stück Ingwer (daumengroß)', row('1', 'Stück', 'Ingwer (daumengroß)')]
]

for (const [line, expected] of lines) {
  test(`splits "${line}"`, () => {
    assert.deepEqual(parseIngredientLine(line), expected)
  })
}

test('units known to the app count as units too', () => {
  assert.deepEqual(parseIngredientLine('1 Knäuel Garn'), row('1', '', 'Knäuel Garn'))
  assert.deepEqual(parseIngredientLine('1 Knäuel Garn', ['Knäuel']), row('1', 'Knäuel', 'Garn'))
})

test('a line of just an amount keeps the amount', () => {
  assert.deepEqual(parseIngredientLine('200 g'), row('200', 'g', ''))
})

test('a text block becomes one row per non-empty line', () => {
  const text = '200 g Zwiebeln\r\n\n1 Prise Salz\n   \nSalz und Pfeffer\n'
  assert.deepEqual(parseIngredientText(text), [
    row('200', 'g', 'Zwiebeln'),
    row('1', 'Prise', 'Salz'),
    row('', '', 'Salz und Pfeffer')
  ])
})

test('rows turn into text and back without changes', () => {
  const rows = [row('200', 'g', 'Zwiebeln'), row('1/2', 'TL', 'Salz'), emptyIngredient(), row('', '', 'Pfeffer')]
  const text = ingredientsToText(rows)
  assert.equal(text, '200 g Zwiebeln\n1/2 TL Salz\nPfeffer')
  assert.deepEqual(ingredientsFromText(text, rows), [row('200', 'g', 'Zwiebeln'), row('1/2', 'TL', 'Salz'), row('', '', 'Pfeffer')])
})

test('unchanged lines keep their saved row including its id', () => {
  const rows = [{ id: 7, ...row('200', 'g', 'Zwiebeln') }, { id: 8, ...row('1', 'Prise', 'Salz') }]
  const result = ingredientsFromText('1 Prise Salz\n200 g Zwiebeln\n2 EL Öl', rows)
  assert.deepEqual(result, [
    { id: 8, ...row('1', 'Prise', 'Salz') },
    { id: 7, ...row('200', 'g', 'Zwiebeln') },
    row('2', 'EL', 'Öl')
  ])
  assert.notEqual(result[0], rows[1])
})

test('an emptied text leaves one empty row', () => {
  assert.deepEqual(ingredientsFromText('  \n', [row('1', '', 'Ei')]), [emptyIngredient()])
})

test('multi-line detection ignores surrounding blank lines', () => {
  assert.equal(isMultiLinePaste('200 g Zwiebeln\n'), false)
  assert.equal(isMultiLinePaste('\n200 g Zwiebeln\n1 Ei'), true)
})

test('pasting a list into an empty row replaces it', () => {
  const rows = [row('1', '', 'Ei'), emptyIngredient(), row('', '', 'Salz')]
  const last = insertPastedIngredients(rows, 1, '200 g Zwiebeln\n1/2 TL Salz')
  assert.equal(last, 2)
  assert.deepEqual(rows, [row('1', '', 'Ei'), row('200', 'g', 'Zwiebeln'), row('1/2', 'TL', 'Salz'), row('', '', 'Salz')])
})

test('pasting a list into a filled row adds the lines below it', () => {
  const rows = [row('1', '', 'Ei'), row('', '', 'Salz')]
  const last = insertPastedIngredients(rows, 0, '200 g Zwiebeln\n2 EL Öl')
  assert.equal(last, 2)
  assert.deepEqual(rows.map((r) => r.name), ['Ei', 'Zwiebeln', 'Öl', 'Salz'])
})

test('a single pasted line with an amount is split only into an empty row', () => {
  const empty = [emptyIngredient()]
  assert.equal(insertPastedIngredients(empty, 0, '200 g Zwiebeln'), 0)
  assert.deepEqual(empty, [row('200', 'g', 'Zwiebeln')])

  const filled = [row('', '', 'Zwie')]
  assert.equal(insertPastedIngredients(filled, 0, '200 g Zwiebeln'), -1)
  assert.deepEqual(filled, [row('', '', 'Zwie')])
})

test('a single pasted word stays a normal paste', () => {
  const rows = [emptyIngredient()]
  assert.equal(insertPastedIngredients(rows, 0, 'Zwiebeln'), -1)
  assert.deepEqual(rows, [emptyIngredient()])
})
