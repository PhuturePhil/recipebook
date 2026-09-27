import { test } from 'node:test'
import assert from 'node:assert/strict'
import { OWN_RECIPE, uniqueSources, sourceSuggestions, authorSuggestions, sourcesOfAuthor } from './recipeSources.js'

const known = [
  { source: 'A Modern Way to Cook', author: 'Anna Jones' },
  { source: 'Eigenrezept', author: 'Paco' },
  { source: 'Eigenrezept', author: 'Philipp Pastoors' },
  { source: 'Genussvoll vegetarisch', author: 'Yotam Ottolenghi' },
  { source: 'Jerusalem', author: 'Yotam Ottolenghi und Sami Tamimi' },
  { source: 'One: Pot, Pan, Planet', author: 'Anna Jones' },
  { source: 'Crème de la Crème', author: null },
]

const names = (items) => items.map(i => i.source)

test('uniqueSources: eine Zeile je Quelle, Eigenrezept immer dabei und ohne Autor-Übernahme', () => {
  const sources = uniqueSources(known)
  assert.equal(sources.filter(s => s.source === OWN_RECIPE).length, 1)
  assert.equal(sources.find(s => s.source === OWN_RECIPE).author, null)
  assert.equal(sources.find(s => s.source === 'Genussvoll vegetarisch').author, 'Yotam Ottolenghi')
  assert.ok(uniqueSources([]).some(s => s.source === OWN_RECIPE))
})

test('uniqueSources fasst Schreibvarianten zusammen', () => {
  const sources = uniqueSources([
    { source: 'Genussvoll vegetarisch', author: 'Yotam Ottolenghi' },
    { source: 'genussvoll  vegetarisch ', author: 'Yotam Ottolenghi' },
  ])
  assert.deepEqual(names(sources), ['Genussvoll vegetarisch', OWN_RECIPE])
})

test('sourceSuggestions: Teilstring, Groß-/Kleinschreibung und Akzente egal', () => {
  assert.deepEqual(names(sourceSuggestions(known, 'genussvoll')), ['Genussvoll vegetarisch'])
  assert.deepEqual(names(sourceSuggestions(known, 'VEGETAR')), ['Genussvoll vegetarisch'])
  assert.deepEqual(names(sourceSuggestions(known, 'creme')), ['Crème de la Crème'])
  assert.deepEqual(names(sourceSuggestions(known, 'eigen')), [OWN_RECIPE])
})

test('sourceSuggestions: Wortanfang vor Teilstring, leeres Feld zeigt alles alphabetisch', () => {
  assert.deepEqual(names(sourceSuggestions(known, 'pan')), ['One: Pot, Pan, Planet'])
  assert.deepEqual(names(sourceSuggestions(known, 'e')),
    ['Eigenrezept', 'A Modern Way to Cook', 'Crème de la Crème', 'Genussvoll vegetarisch', 'Jerusalem', 'One: Pot, Pan, Planet'])
  assert.equal(sourceSuggestions(known, '').length, 6)
  assert.equal(names(sourceSuggestions(known, ''))[0], 'A Modern Way to Cook')
})

test('sourceSuggestions: Quellen des eingetragenen Autors zuerst, andere bleiben sichtbar', () => {
  assert.deepEqual(names(sourceSuggestions(known, '', 'anna jones')).slice(0, 2), ['A Modern Way to Cook', 'One: Pot, Pan, Planet'])
  assert.equal(sourceSuggestions(known, '', 'Anna Jones').length, 6)
})

test('sourceSuggestions blendet die schon eingetragene Quelle aus', () => {
  assert.deepEqual(sourceSuggestions(known, 'Jerusalem'), [])
  assert.deepEqual(names(sourceSuggestions(known, 'jerusalem')), ['Jerusalem'])
})

test('authorSuggestions: eindeutig, akzent- und großschreibungsunabhängig', () => {
  assert.deepEqual(authorSuggestions(known, 'jones'), ['Anna Jones'])
  assert.deepEqual(authorSuggestions(known, 'yotam'), ['Yotam Ottolenghi', 'Yotam Ottolenghi und Sami Tamimi'])
  assert.deepEqual(authorSuggestions(known, 'Paco'), [])
  assert.equal(authorSuggestions(known, '').length, 5)
})

test('sourcesOfAuthor ohne Eigenrezept', () => {
  assert.deepEqual(names(sourcesOfAuthor(known, 'yotam ottolenghi')), ['Genussvoll vegetarisch'])
  assert.deepEqual(sourcesOfAuthor(known, 'Paco'), [])
  assert.equal(sourcesOfAuthor(known, 'Anna Jones').length, 2)
})
