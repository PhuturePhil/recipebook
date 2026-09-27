import { test } from 'node:test'
import assert from 'node:assert/strict'
import { applyFraction, FRACTION_KEYS } from './fractionKeys.js'
import { parseAmount } from './scaleIngredients.js'

test('an empty field takes the fraction as it is', () => {
  assert.equal(applyFraction('', '1/2'), '1/2')
  assert.equal(applyFraction(null, '1/4'), '1/4')
  assert.equal(applyFraction('  ', '3/4'), '3/4')
})

test('a whole number gets the fraction added', () => {
  assert.equal(applyFraction('1', '1/2'), '1 1/2')
  assert.equal(applyFraction('12 ', '3/4'), '12 3/4')
})

test('an existing fraction is replaced instead of stacked', () => {
  assert.equal(applyFraction('1 1/2', '1/4'), '1 1/4')
  assert.equal(applyFraction('1/2', '3/4'), '3/4')
  assert.equal(applyFraction('2½', '1/4'), '2 1/4')
  assert.equal(applyFraction('½', '1/2'), '1/2')
  assert.equal(applyFraction('1 1 / 2', '3/4'), '1 3/4')
})

test('a decimal part is replaced by the fraction', () => {
  assert.equal(applyFraction('1,5', '1/4'), '1 1/4')
  assert.equal(applyFraction('2.25', '1/2'), '2 1/2')
  assert.equal(applyFraction('0,5', '3/4'), '3/4')
  assert.equal(applyFraction('0', '1/2'), '1/2')
})

test('ranges and prefixes keep their shape', () => {
  assert.equal(applyFraction('1-2', '1/2'), '1-2 1/2')
  assert.equal(applyFraction('1-', '1/2'), '1-1/2')
  assert.equal(applyFraction('1 - 1/4', '1/2'), '1 - 1/2')
  assert.equal(applyFraction('ca.', '1/2'), 'ca. 1/2')
  assert.equal(applyFraction('10', '1/2'), '10 1/2')
  assert.equal(applyFraction('20', '1/2'), '20 1/2')
})

test('every result is read back as the intended amount', () => {
  assert.equal(parseAmount(applyFraction('1', '1/2')).min, 1.5)
  assert.equal(parseAmount(applyFraction('1,5', '1/4')).min, 1.25)
  assert.deepEqual(
    [parseAmount(applyFraction('1-2', '1/2')).min, parseAmount(applyFraction('1-2', '1/2')).max],
    [1, 2.5]
  )
  for (const key of FRACTION_KEYS) {
    assert.ok(parseAmount(applyFraction('', key.value)).min > 0)
  }
})
