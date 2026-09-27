import { test } from 'node:test'
import assert from 'node:assert/strict'
import { MAX_TAGS, TAG_BAR_LIMIT, cleanTag, addTag, tagSuggestions, tagSearchTerm, tagCounts, tagBarEntries } from './recipeTags.js'
import { filterRecipes, matchTag } from './recipeSearch.js'

test('cleanTag entfernt #, Leerraum und schreibt groß', () => {
  assert.equal(cleanTag('  #curry '), 'Curry')
  assert.equal(cleanTag('one   pot'), 'One pot')
  assert.equal(cleanTag('x'.repeat(40)).length, 30)
  assert.equal(cleanTag('  '), '')
  assert.equal(cleanTag(null), '')
})

test('addTag verhindert Dubletten und übernimmt bekannte Schreibweise', () => {
  const tags = ['Suppe']
  assert.equal(addTag(tags, 'suppe'), false)
  assert.equal(addTag(tags, 'fruhstuck', ['Frühstück']), true)
  assert.equal(addTag(tags, ''), false)
  assert.deepEqual(tags, ['Suppe', 'Frühstück'])
})

test('addTag hält höchstens fünf Tags', () => {
  const tags = ['A', 'B', 'C', 'D', 'E']
  assert.equal(tags.length, MAX_TAGS)
  assert.equal(addTag(tags, 'F'), false)
  assert.deepEqual(tags, ['A', 'B', 'C', 'D', 'E'])
})

test('tagSuggestions: Wortanfang vor Teilstring, vorhandene Tags ausgeblendet', () => {
  const known = ['Auflauf', 'Backen', 'Curry', 'Kuchen', 'Suppe']
  assert.deepEqual(tagSuggestions(known, ['Suppe'], ''), ['Auflauf', 'Backen', 'Curry', 'Kuchen'])
  assert.deepEqual(tagSuggestions(known, [], 'ku'), ['Kuchen'])
  assert.deepEqual(tagSuggestions(['Backen', 'Kuchen backen'], [], 'back'), ['Backen', 'Kuchen backen'])
  assert.deepEqual(tagSuggestions(known, ['Curry'], '#cur'), [])
})

test('Tag-Begriff "#tag" trifft nur Rezepte mit genau diesem Tag', () => {
  const recipes = [
    { id: 1, tags: ['Curry', 'Indisch'], text: 'dal | linsen | curry | indisch' },
    { id: 2, tags: ['Suppe'], text: 'tomatensuppe | currypulver | suppe' },
    { id: 3, text: 'brot' },
  ]
  const run = (terms) => filterRecipes(recipes, terms, { searchText: (r) => r.text, matchKeyword: matchTag })
    .map((r) => r.id)
  assert.deepEqual(run([tagSearchTerm('Curry')]), [1])
  assert.deepEqual(run(['curry']), [1, 2])
  assert.deepEqual(run(['-#Curry']), [2, 3])
  assert.equal(matchTag(recipes[0], 'curry'), undefined)
})

const tagged = [
  { id: 1, tags: ['Ofengericht', 'Indisch'] },
  { id: 2, tags: ['Ofengericht', 'Eintopf'] },
  { id: 3, tags: ['ofengericht', 'Ofengericht'] },
  { id: 4, tags: ['Eintopf', 'Suppe'] },
  { id: 5 },
]

test('tagCounts zählt je Rezept einmal, häufigste zuerst, Gleichstand alphabetisch', () => {
  assert.deepEqual(tagCounts(tagged), [
    { tag: 'Ofengericht', count: 3 },
    { tag: 'Eintopf', count: 2 },
    { tag: 'Indisch', count: 1 },
    { tag: 'Suppe', count: 1 },
  ])
  assert.deepEqual(tagCounts([]), [])
})

test('tagCounts blendet schon aktive Tag-Suchbegriffe aus, Freitext nicht', () => {
  const tags = (terms) => tagCounts(tagged, terms).map((e) => e.tag)
  assert.deepEqual(tags([tagSearchTerm('Ofengericht')]), ['Eintopf', 'Indisch', 'Suppe'])
  assert.deepEqual(tags(['#eintopf', 'Ofengericht']), ['Ofengericht', 'Indisch', 'Suppe'])
})

test('Tag-Leiste klappt ab TAG_BAR_LIMIT ein und zeigt aufgeklappt alle', () => {
  const entries = Array.from({ length: TAG_BAR_LIMIT + 3 }, (_, i) => ({ tag: `T${i}`, count: 20 - i }))
  const collapsed = tagBarEntries(entries, false)
  assert.equal(collapsed.shown.length, TAG_BAR_LIMIT)
  assert.equal(collapsed.hidden, 3)
  assert.deepEqual(collapsed.shown, entries.slice(0, TAG_BAR_LIMIT))
  assert.deepEqual(tagBarEntries(entries, true), { shown: entries, hidden: 0 })
  const few = entries.slice(0, 3)
  assert.deepEqual(tagBarEntries(few, false), { shown: few, hidden: 0 })
})

test('Tag aus der Leiste wird als exakter Tag-Suchbegriff gesetzt', () => {
  const [top] = tagCounts(tagged)
  const hits = filterRecipes(tagged, [tagSearchTerm(top.tag)], {
    searchText: () => '',
    matchKeyword: (recipe, text) => matchTag(recipe, text),
  })
  assert.deepEqual(hits.map((r) => r.id), [1, 2, 3])
  assert.equal(hits.length, top.count)
})
