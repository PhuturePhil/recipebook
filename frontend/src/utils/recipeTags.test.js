import { test } from 'node:test'
import assert from 'node:assert/strict'
import { MAX_TAGS, cleanTag, addTag, tagSuggestions, tagSearchTerm } from './recipeTags.js'
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
