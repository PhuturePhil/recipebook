import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  macroDailyPercent, microDailyPercent, formatPercent, percentLevel,
} from './dailyValue.js'

test('macro percentages use the EU reference intakes', () => {
  assert.equal(Math.round(macroDailyPercent('kcal', 823.3)), 41)
  assert.equal(Math.round(macroDailyPercent('salt', 4.134)), 69)
  assert.equal(Math.round(macroDailyPercent('fiber', 22.89)), 76)
  assert.equal(macroDailyPercent('fat', null), null)
  assert.equal(macroDailyPercent('unknown', 10), null)
})

test('micro percentages convert to the unit delivered by the API', () => {
  assert.equal(Math.round(microDailyPercent('VITC', 73.785, 'mg')), 92)
  // BLS liefert Vitamin B6 in µg, die Referenz ist 1,4 mg
  assert.equal(Math.round(microDailyPercent('VITB6', 639.048, 'µg')), 46)
  assert.equal(Math.round(microDailyPercent('VITB6', 0.639, 'mg')), 46)
  assert.equal(Math.round(microDailyPercent('ID', 53.388, 'µg')), 36)
})

test('nutrients without an official reference get no percentage', () => {
  assert.equal(microDailyPercent('NA', 1653, 'mg'), null)
  assert.equal(microDailyPercent('CHORL', 50, 'mg'), null)
  assert.equal(microDailyPercent('FAPUN3', 0.3, 'g'), null)
  assert.equal(microDailyPercent('VITC', 10, 'IE'), null)
})

test('formats and classifies percentages', () => {
  assert.equal(formatPercent(91.6), '92 %')
  assert.equal(formatPercent(1234.4), '1.234 %')
  assert.equal(formatPercent(null), '–')
  assert.equal(percentLevel(50), 'high')
  assert.equal(percentLevel(25), 'medium')
  assert.equal(percentLevel(24.9), 'low')
  assert.equal(percentLevel(null), 'none')
})
