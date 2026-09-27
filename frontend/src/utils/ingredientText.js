import { emptyIngredient, isBlankIngredient } from './recipeFormData.js'
import { groupAt, groupRow, isGroupRow, parseGroupHeading } from './ingredientGroups.js'

// Unit spellings recognised when splitting a pasted line; mirrors the synonyms of UnitNormalizer in the backend.
// The unit is kept as written, only recognised.
const UNIT_WORDS = [
  'g', 'gr', 'gramm', 'gram', 'grams', 'kg', 'kilo', 'kilogramm', 'kilogram', 'mg', 'milligramm',
  'ml', 'milliliter', 'millilitre', 'mls', 'l', 'liter', 'litre', 'ltr', 'lt', 'cl', 'zentiliter', 'dl', 'deziliter',
  'tasse', 'tassen', 'cup', 'cups',
  'el', 'eßl', 'essl', 'esslöffel', 'eßlöffel', 'essloeffel', 'tablespoon', 'tablespoons', 'tbsp', 'tbs', 'tbl',
  'tl', 'teel', 'teelöffel', 'teeloeffel', 'teaspoon', 'teaspoons', 'tsp',
  'prise', 'prisen', 'pinch', 'pinches', 'msp', 'messerspitze', 'messerspitzen',
  'spritzer', 'schuss', 'dash', 'splash', 'handvoll', 'handful', 'handfuls',
  'bund', 'bünde', 'bündel', 'bunch', 'bunches', 'bd',
  'stück', 'stueck', 'stk', 'st', 'stck', 'pc', 'pcs', 'piece', 'pieces',
  'zehe', 'zehen', 'clove', 'cloves', 'knolle', 'knollen', 'bulb', 'bulbs', 'kopf', 'köpfe', 'head', 'heads',
  'stange', 'stangen', 'stalk', 'stalks', 'stick', 'sticks', 'dose', 'dosen', 'tin', 'tins', 'can', 'cans',
  'päckchen', 'pck', 'pkg', 'pk', 'packung', 'packungen', 'pack', 'tüte', 'tüten', 'tütchen',
  'flasche', 'flaschen', 'bottle', 'bottles', 'cm', 'zentimeter', 'stückchen', 'stueckchen',
  'scheibe', 'scheiben', 'slice', 'slices', 'glas', 'gläser', 'jar', 'jars', 'becher', 'tub',
  'blatt', 'blätter', 'leaf', 'leaves', 'zweig', 'zweige', 'sprig', 'sprigs', 'würfel', 'cube', 'cubes'
]

// Size words only count as part of the unit when a unit follows ("1 kleine Dose"), otherwise they stay in the name
const SIZE_WORDS = [
  'klein', 'kleine', 'kleines', 'kleiner', 'kleinen', 'groß', 'große', 'großes', 'großer', 'großen',
  'gross', 'grosse', 'grosses', 'grosser', 'mittelgroß', 'mittelgroße', 'mittelgroßes', 'mittelgroßer',
  'gestrichen', 'gestrichene', 'gestrichener', 'gehäuft', 'gehäufte', 'gehäufter', 'gehäuftes',
  'daumengroß', 'daumengroße', 'daumengroßes', 'daumengroßer', 'small', 'large', 'heaped', 'level'
]

const FRACTION_CHARS = '½¼¾⅓⅔⅛⅕'
const SINGLE = `\\d+(?:[.,]\\d+)?(?:\\s+\\d+\\/\\d+|\\s*[${FRACTION_CHARS}])?|\\d+\\/\\d+|[${FRACTION_CHARS}]`
const AMOUNT = new RegExp(
  `^((?:ca\\.?|circa|etwa|ungefähr)\\s*)?(${SINGLE})(\\s*(?:-|–|—|bis)\\s*(?:${SINGLE}))?(?![\\d/.,])`,
  'i'
)
const BULLET = /^\s*(?:[-*•·▪◦–—]|\[\s?\]|[☐▢□])\s+/

const unitKey = (word) => word.toLowerCase().replace(/[.:,]+$/, '')

function isUnit(word, extraUnits) {
  const key = unitKey(word)
  if (!key) return false
  return UNIT_WORDS.includes(key) || extraUnits.some((u) => u.toLowerCase() === key)
}

// "200 g Zwiebeln" → { amount: '200', unit: 'g', name: 'Zwiebeln' }; lines without a leading amount become name only
export function parseIngredientLine(line, extraUnits = []) {
  const text = String(line ?? '').replace(BULLET, '').replace(/\s+/g, ' ').trim()
  const match = text.match(AMOUNT)
  if (!match) return { amount: '', unit: '', name: text }

  const amount = match[0].replace(/\s*([-–—])\s*/g, '$1').trim()
  const rest = text.slice(match[0].length).split(' ')
  // Glued to the amount only a unit makes sense ("250g"); "3er Pack Eier" stays a plain name
  if (rest[0] && !isUnit(rest[0], extraUnits) && !/^[x×]$/i.test(rest[0])) {
    return { amount: '', unit: '', name: text }
  }
  if (rest[0] === '') rest.shift()

  let unit = ''
  if (rest.length > 1 && SIZE_WORDS.includes(unitKey(rest[0])) && isUnit(rest[1], extraUnits)) {
    unit = rest.splice(0, 2).join(' ')
  } else if (rest.length > 0 && isUnit(rest[0], extraUnits)) {
    unit = rest.splice(0, 1)[0]
  }
  if (/^[x×]$/i.test(rest[0] ?? '')) rest.shift()
  // "1 Dose (400 g) Kichererbsen" → name "Kichererbsen (400 g)"
  const name = rest.join(' ').replace(/^(\([^)]*\))\s+(.+)$/, '$2 $1')
  return { amount, unit: unit.replace(/[.:,]+$/, ''), name }
}

// Lines like "Salsa:", "### Salsa" or "**Salsa**" become group headings { group: 'Salsa' }; a list title
// ("Zutaten:") is dropped
export function parseIngredientText(text, extraUnits = []) {
  const rows = []
  for (const line of String(text ?? '').split(/\r?\n/)) {
    const heading = parseGroupHeading(line.replace(BULLET, ''), (t) => AMOUNT.test(t))
    if (heading === '') continue
    if (heading !== null) {
      rows.push(groupRow(heading))
      continue
    }
    const ingredient = parseIngredientLine(line, extraUnits)
    if (!isBlankIngredient(ingredient)) rows.push(ingredient)
  }
  return rows
}

export const ingredientToLine = (ingredient) =>
  [ingredient.amount, ingredient.unit, ingredient.name]
    .map((part) => String(part ?? '').trim())
    .filter(Boolean)
    .join(' ')

// Group headings become "Salsa:" lines with an empty line above; a heading without name ("no group") stays a gap
export function ingredientsToText(rows) {
  const lines = []
  for (const row of rows) {
    if (isGroupRow(row)) {
      if (lines.length) lines.push('')
      if (row.group.trim()) lines.push(`${row.group.trim()}:`)
    } else if (!isBlankIngredient(row)) {
      lines.push(ingredientToLine(row))
    }
  }
  return lines.join('\n').trim()
}

// Rows that come back unchanged from the text editor keep their original object (and id)
export function ingredientsFromText(text, previous = [], extraUnits = []) {
  const pool = previous.filter((i) => !isGroupRow(i) && !isBlankIngredient(i))
  const rows = parseIngredientText(text, extraUnits).map((parsed) => {
    if (isGroupRow(parsed)) return parsed
    const line = ingredientToLine(parsed)
    const index = pool.findIndex((old) => ingredientToLine(old) === line)
    return index >= 0 ? { ...pool.splice(index, 1)[0] } : parsed
  })
  return rows.length ? rows : [emptyIngredient()]
}

export const isMultiLinePaste = (text) =>
  String(text ?? '').split(/\r?\n/).filter((line) => line.trim()).length > 1

// Pasting into the name field of a row: a blank row is replaced, otherwise the new rows go below it.
// Returns the index of the last inserted row, or -1 if the paste should stay a normal paste.
export function insertPastedIngredients(ingredients, index, text, extraUnits = []) {
  const parsed = parseIngredientText(text, extraUnits)
  const current = ingredients[index]
  const rowIsEmpty = isBlankIngredient(current)
  if (!isMultiLinePaste(text)) {
    // A single line only gets split when the row is otherwise empty and the line starts with an amount
    if (!rowIsEmpty || parsed.length !== 1 || !parsed[0].amount) {
      return -1
    }
    ingredients.splice(index, 1, parsed[0])
    return index
  }
  if (!parsed.length) return -1
  const start = rowIsEmpty ? index : index + 1
  const inserted = [...parsed]
  // Pasted headings must not pull the rows below into the pasted group: they keep the group they had
  const next = ingredients[index + 1]
  if (parsed.some(isGroupRow) && next && !isGroupRow(next)) inserted.push(groupRow(groupAt(ingredients, index)))
  ingredients.splice(start, rowIsEmpty ? 1 : 0, ...inserted)
  return start + parsed.length - 1
}
