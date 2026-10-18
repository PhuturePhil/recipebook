import { test } from 'node:test'
import assert from 'node:assert/strict'
import { DELETE_WORDS, pickDeleteWord, matchesDeleteWord } from './deleteWord.js'

test('Küchenwörter: kurz, eindeutig, ohne Umlaute und Leerzeichen', () => {
  assert.ok(DELETE_WORDS.length >= 6)
  assert.equal(new Set(DELETE_WORDS.map((w) => w.toLowerCase())).size, DELETE_WORDS.length)
  for (const w of DELETE_WORDS) assert.match(w, /^[A-Z][a-z]{2,4}$/, w)
  for (const w of ['Salz', 'Sieb', 'Ofen', 'Topf', 'Quark', 'Zimt']) assert.ok(DELETE_WORDS.includes(w), w)
})

test('pickDeleteWord: Zufall über die ganze Liste, auch am Rand', () => {
  assert.equal(pickDeleteWord(null, () => 0), DELETE_WORDS[0])
  assert.equal(pickDeleteWord(null, () => 0.999999), DELETE_WORDS.at(-1))
  assert.equal(pickDeleteWord(null, () => 1), DELETE_WORDS.at(-1))
  const seen = new Set()
  for (let i = 0; i < 200; i++) seen.add(pickDeleteWord())
  assert.ok(seen.size > DELETE_WORDS.length / 2)
})

test('pickDeleteWord: nie zweimal dasselbe Wort hintereinander', () => {
  for (const prev of DELETE_WORDS) {
    for (const r of [0, 0.3, 0.7, 0.999]) assert.notEqual(pickDeleteWord(prev, () => r), prev)
  }
})

test('matchesDeleteWord: Groß-/Kleinschreibung und Rand-Leerzeichen egal', () => {
  assert.equal(matchesDeleteWord('salz', 'Salz'), true)
  assert.equal(matchesDeleteWord('SALZ', 'Salz'), true)
  assert.equal(matchesDeleteWord('  Salz ', 'Salz'), true)
  assert.equal(matchesDeleteWord('QUARK', 'Quark'), true)
})

test('matchesDeleteWord: alles andere schaltet nicht frei', () => {
  for (const input of ['', ' ', 'Sal', 'Salze', 'S a l z', 'Sieb', null, undefined]) {
    assert.equal(matchesDeleteWord(input, 'Salz'), false, String(input))
  }
  assert.equal(matchesDeleteWord('', ''), false)
  assert.equal(matchesDeleteWord('x', null), false)
})
