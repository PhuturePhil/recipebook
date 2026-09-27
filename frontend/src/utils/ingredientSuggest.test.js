import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  suggestionQuery,
  visibleSuggestions,
  moveHighlight,
  recognitionKey,
  pendingRecognition,
  recognitionHint,
  UNKNOWN_HINT
} from './ingredientSuggest.js'

test('suggestionQuery waits for two characters and trims', () => {
  assert.equal(suggestionQuery(''), '')
  assert.equal(suggestionQuery(' k '), '')
  assert.equal(suggestionQuery(null), '')
  assert.equal(suggestionQuery('  Kicher '), 'Kicher')
  assert.equal(suggestionQuery('Ki'), 'Ki')
})

test('visibleSuggestions hides a list that only repeats what was typed', () => {
  const one = [{ name: 'Zwiebel' }]
  assert.deepEqual(visibleSuggestions('zwiebel ', one), [])
  assert.deepEqual(visibleSuggestions('Zwieb', one), one)
  const two = [{ name: 'Zwiebel' }, { name: 'Zwiebeln' }]
  assert.deepEqual(visibleSuggestions('Zwiebel', two), two)
  assert.deepEqual(visibleSuggestions('Zwiebel', undefined), [])
})

test('moveHighlight starts at the edge and wraps around', () => {
  assert.equal(moveHighlight(-1, 1, 3), 0)
  assert.equal(moveHighlight(-1, -1, 3), 2)
  assert.equal(moveHighlight(0, 1, 3), 1)
  assert.equal(moveHighlight(2, 1, 3), 0)
  assert.equal(moveHighlight(0, -1, 3), 2)
  assert.equal(moveHighlight(1, 1, 0), -1)
})

test('recognitionKey covers the whole row and skips rows without a name', () => {
  assert.equal(recognitionKey({ amount: ' 2 ', unit: 'St', name: ' Zwiebeln ' }), '["2","St","Zwiebeln"]')
  assert.equal(recognitionKey({ amount: null, unit: undefined, name: 'Salz' }), '["","","Salz"]')
  assert.equal(recognitionKey({ amount: '2', unit: 'EL', name: '  ' }), '')
  assert.equal(recognitionKey(null), '')
})

test('pendingRecognition asks once per distinct row and skips known rows', () => {
  const known = { '["1","TL","Salz"]': { recognized: true } }
  const pending = pendingRecognition([
    { amount: '2', unit: 'St', name: 'Zwiebeln' },
    { amount: '2', unit: 'St', name: 'Zwiebeln ' },
    { amount: '1', unit: 'TL', name: 'Salz' },
    { amount: '', unit: '', name: '' },
    { amount: '200', unit: 'g', name: 'Kichererbsen' }
  ], known)

  assert.deepEqual(pending.map((p) => p.line), [
    { amount: '2', unit: 'St', name: 'Zwiebeln' },
    { amount: '200', unit: 'g', name: 'Kichererbsen' }
  ])
  assert.equal(pending[0].key, '["2","St","Zwiebeln"]')
  assert.deepEqual(pendingRecognition(undefined, {}), [])
})

test('recognitionHint shows ✓ with the data source and ? for unknown ingredients', () => {
  assert.equal(recognitionHint(undefined), null)
  assert.deepEqual(recognitionHint({ recognized: false }), { state: 'unknown', symbol: '?', title: UNKNOWN_HINT })
  assert.equal(UNKNOWN_HINT, 'keine Nährwertdaten — wird per KI geschätzt')

  const bls = recognitionHint({ recognized: true, ingredientName: 'Zwiebel', source: 'BLS', referenceName: 'Speisezwiebel roh' })
  assert.equal(bls.symbol, '✓')
  assert.equal(bls.state, 'ok')
  assert.equal(bls.title, 'Nährwerte erkannt: Zwiebel – BLS: Speisezwiebel roh')
  assert.equal(recognitionHint({ recognized: true, ingredientName: 'Kreuzkümmel', source: 'AI_ESTIMATE' }).title,
    'Nährwerte erkannt: Kreuzkümmel – KI-Schätzung')
  assert.equal(recognitionHint({ recognized: true, ingredientName: 'Honig', source: 'MANUAL' }).title,
    'Nährwerte erkannt: Honig – manuell hinterlegt')
  assert.equal(recognitionHint({ recognized: true, source: 'BLS' }).title, 'Nährwerte erkannt: Zutat – BLS')
})
