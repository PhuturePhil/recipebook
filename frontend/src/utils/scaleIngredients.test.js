import { test } from 'node:test'
import assert from 'node:assert/strict'
import { parseAmount, formatNumber, scaleIngredients } from './scaleIngredients.js'

const scaled = (amount, base = 4, servings = 4) =>
  scaleIngredients([{ amount, unit: 'TL', name: 'x' }], base, servings)[0].amount

test('reads fractions, mixed numbers and unicode fractions', () => {
  assert.equal(parseAmount('1/2').min, 0.5)
  assert.equal(parseAmount('2 1/2').min, 2.5)
  assert.equal(parseAmount('½').min, 0.5)
  assert.equal(parseAmount('1½').min, 1.5)
  assert.equal(parseAmount('1 / 4').min, 0.25)
})

test('reads ranges with hyphen, dash and "bis"', () => {
  assert.deepEqual([parseAmount('200-250').min, parseAmount('200-250').max], [200, 250])
  assert.deepEqual([parseAmount('8–10').min, parseAmount('8–10').max], [8, 10])
  assert.deepEqual([parseAmount('2 bis 3').min, parseAmount('2 bis 3').max], [2, 3])
})

test('reads German decimals and keeps text after the number', () => {
  assert.equal(parseAmount('1,5').min, 1.5)
  assert.equal(parseAmount('1 EL').rest, 'EL')
  assert.equal(parseAmount('ca. 200').prefix, 'ca.')
})

test('leaves non-numeric amounts alone', () => {
  assert.equal(parseAmount('etwas'), null)
  assert.equal(parseAmount(''), null)
  assert.equal(parseAmount(null), null)
  assert.equal(scaled('nach Geschmack'), 'nach Geschmack')
})

test('formats with German decimal comma and common fractions', () => {
  assert.equal(formatNumber(0.5), '½')
  assert.equal(formatNumber(0.25), '¼')
  assert.equal(formatNumber(1 / 3), '⅓')
  assert.equal(formatNumber(1.5), '1,5')
  assert.equal(formatNumber(2.25), '2,25')
  assert.equal(formatNumber(12.34), '12,3')
  assert.equal(formatNumber(187.5), '188')
  assert.equal(formatNumber(1000), '1000')
})

test('shows stored amounts unchanged at base servings', () => {
  assert.equal(scaled('1/2'), '½')
  assert.equal(scaled('2 1/2'), '2,5')
  assert.equal(scaled('200-250'), '200–250')
  assert.equal(scaled('1.5'), '1,5')
  assert.equal(scaled('1 EL'), '1 EL')
  assert.equal(scaled('2 Stangen'), '2 Stangen')
  assert.equal(scaled('200'), '200')
})

test('scales fractions and both ends of a range', () => {
  assert.equal(scaled('1/2', 4, 8), '1')
  assert.equal(scaled('1/2', 4, 2), '¼')
  assert.equal(scaled('200-250', 4, 2), '100–125')
  assert.equal(scaled('3', 4, 2), '1,5')
  assert.equal(scaled('5-9', 4, 6), '7,5–13,5')
})

test('range midpoint scales like the backend mean', () => {
  const p = parseAmount('200-250')
  const factor = 6 / 4
  assert.equal((p.min * factor + p.max * factor) / 2, ((p.min + p.max) / 2) * factor)
})

test('falls back to factor 1 without base servings', () => {
  assert.equal(scaled('1/2', null, 4), '½')
  assert.equal(scaled('3', 0, 4), '3')
})
