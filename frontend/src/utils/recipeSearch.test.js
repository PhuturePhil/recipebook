import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  normalizeText,
  parseTerm,
  fuzzyDistance,
  fuzzyIncludes,
  filterRecipes,
  sortRecipes,
} from './recipeSearch.js'

const recipes = [
  { id: 1, title: 'Pilzrisotto', text: 'pilzrisotto | champignons, reis', prepTimeMinutes: 40, kcal: 520 },
  { id: 2, title: 'Tomatensuppe', text: 'tomatensuppe | tomaten, creme fraiche', prepTimeMinutes: 25, kcal: 310 },
  { id: 3, title: 'Turkish green beans', text: 'turkish green beans | runner beans | turkische grune bohnen | stangenbohnen', prepTimeMinutes: null, kcal: null },
  { id: 4, title: 'Nudeln mit Pilzen', text: 'nudeln mit pilzen | nudeln, pilze, sahne', prepTimeMinutes: 20, kcal: 700 },
].map((r) => ({ ...r, nutrition: r.kcal == null ? null : { perServing: { kcal: r.kcal } } }))

const run = (terms, extra = {}) =>
  filterRecipes(recipes, terms, { searchText: (r) => r.text, ...extra }).map((r) => r.id)

test('normalizeText vereinheitlicht Akzente, Umlaute, ß und Leerraum', () => {
  assert.equal(normalizeText('Crème  fraîche'), 'creme fraiche')
  assert.equal(normalizeText('Grüße aus der Küche'), 'grusse aus der kuche')
  assert.equal(normalizeText('Soße'), 'sosse')
  assert.equal(normalizeText(null), '')
})

test('parseTerm erkennt Ausschluss mit Minus und "ohne"', () => {
  assert.deepEqual(parseTerm('-Pilze'), { kind: 'text', exclude: true, text: 'pilze' })
  assert.deepEqual(parseTerm('- pilze'), { kind: 'text', exclude: true, text: 'pilze' })
  assert.deepEqual(parseTerm('ohne Crème'), { kind: 'text', exclude: true, text: 'creme' })
  assert.deepEqual(parseTerm('Ohne Nüsse'), { kind: 'text', exclude: true, text: 'nusse' })
  assert.deepEqual(parseTerm('Ohnesorg'), { kind: 'text', exclude: false, text: 'ohnesorg' })
  assert.deepEqual(parseTerm('<30'), { kind: 'time', exclude: false, op: '<', minutes: 30 })
  assert.equal(parseTerm('-'), null)
  assert.equal(parseTerm('ohne'), null)
  assert.equal(parseTerm('  '), null)
})

test('fuzzyDistance misst Tippfehler an beliebiger Stelle im Text', () => {
  assert.equal(fuzzyDistance('tomaten', 'die tomaten sind rot'), 0)
  assert.equal(fuzzyDistance('tomten', 'die tomaten sind rot'), 1)
  assert.equal(fuzzyDistance('toamten', 'die tomaten sind rot'), 1)
  assert.equal(fuzzyDistance('tomatne', 'tomaten'), 1)
  assert.ok(fuzzyDistance('tmtn', 'tomaten') > 1)
})

test('fuzzyIncludes toleriert einen Fehler erst ab fünf Zeichen', () => {
  assert.equal(fuzzyIncludes('tomatensuppe', 'tomatne'), true)
  assert.equal(fuzzyIncludes('tomatensuppe', 'suppr'), true)
  assert.equal(fuzzyIncludes('reis', 'ries'), false)
  assert.equal(fuzzyIncludes('tomatensuppe', 'kartofel'), false)
})

test('Freitext-Begriffe sind UND-verknüpft und akzentunabhängig', () => {
  assert.deepEqual(run(['Crème']), [2])
  assert.deepEqual(run(['Creme']), [2])
  assert.deepEqual(run(['nudeln', 'sahne']), [4])
})

test('übersetzter Suchtext findet englische Rezepte auf Deutsch und im Original', () => {
  assert.deepEqual(run(['Bohnen']), [3])
  assert.deepEqual(run(['beans']), [3])
})

test('Ausschluss entfernt Treffer und lässt sich mit anderen Begriffen kombinieren', () => {
  assert.deepEqual(run(['-pilz']), [2, 3])
  assert.deepEqual(run(['ohne Pilze']), [1, 2, 3])
  assert.deepEqual(run(['<45', '-pilz']), [2])
  assert.deepEqual(run(['-Tomaten', '-bohnen']), [1, 4])
})

test('Ausschluss ist nicht tippfehlertolerant', () => {
  assert.deepEqual(run(['-tomtaen']), [1, 2, 3, 4])
})

test('Tippfehler-Toleranz greift nur, wenn der Begriff wörtlich nirgends vorkommt', () => {
  assert.deepEqual(run(['Tomatensupe']), [2])
  assert.deepEqual(run(['Pilze']), [4])
})

test('Schlüsselwörter wie Badges gehen vor Freitext und lassen sich ausschließen', () => {
  const matchKeyword = (recipe, text) => (text === 'schnell' ? (recipe.prepTimeMinutes ?? 99) <= 25 : undefined)
  assert.deepEqual(run(['schnell'], { matchKeyword }), [2, 4])
  assert.deepEqual(run(['-schnell'], { matchKeyword }), [1, 3])
})

test('Zeitfilter schließen Rezepte ohne Zeitangabe aus', () => {
  assert.deepEqual(run(['>30']), [1])
  assert.deepEqual(run(['<30']), [2, 4])
})

test('ohne Begriffe bleibt die Liste unverändert', () => {
  assert.equal(filterRecipes(recipes, [], { searchText: (r) => r.text }), recipes)
  assert.equal(filterRecipes(recipes, ['-', 'ohne'], { searchText: (r) => r.text }), recipes)
})

test('sortRecipes sortiert nach Zeit und kcal, fehlende Werte ans Ende', () => {
  const ids = (mode) => sortRecipes(recipes, mode).map((r) => r.id)
  assert.deepEqual(ids('default'), [1, 2, 3, 4])
  assert.deepEqual(ids('newest'), [4, 3, 2, 1])
  assert.deepEqual(ids('prepTime'), [4, 2, 1, 3])
  assert.deepEqual(ids('kcal'), [2, 1, 4, 3])
  assert.deepEqual(recipes.map((r) => r.id), [1, 2, 3, 4])
})
