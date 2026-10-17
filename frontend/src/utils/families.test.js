import { test } from 'node:test'
import assert from 'node:assert/strict'
import { scopeRecipes, normalizeScope, shortFamilyName, avatarTone, ownRecipeCount, shareOwnLabel, SCOPE_ALL, SCOPE_MINE } from './families.js'
import { welcomeText } from './familyTexts.js'

const recipes = [
  { id: 1, ownerId: 7, families: [{ id: 1, name: 'Familie Pastoors' }] },
  { id: 2, ownerId: 8, families: [{ id: 1, name: 'Familie Pastoors' }, { id: 2, name: 'Kochclub Köln' }] },
  { id: 3, ownerId: 7, families: [] },
  { id: 4, ownerId: 9 },
]

test('Sammlung „Alle“ zeigt alles, was der Server liefert', () => {
  assert.deepEqual(scopeRecipes(recipes, SCOPE_ALL, 7).map((r) => r.id), [1, 2, 3, 4])
  assert.deepEqual(scopeRecipes(recipes, null, 7).map((r) => r.id), [1, 2, 3, 4])
})

test('Sammlung „Meine“ zeigt nur eigene Rezepte', () => {
  assert.deepEqual(scopeRecipes(recipes, SCOPE_MINE, 7).map((r) => r.id), [1, 3])
  assert.deepEqual(scopeRecipes(recipes, SCOPE_MINE, null).map((r) => r.id), [])
})

test('Familien-Sammlung zeigt die Rezepte dieser Familie', () => {
  assert.deepEqual(scopeRecipes(recipes, 1, 7).map((r) => r.id), [1, 2])
  assert.deepEqual(scopeRecipes(recipes, 2, 7).map((r) => r.id), [2])
  assert.deepEqual(scopeRecipes(recipes, 99, 7).map((r) => r.id), [])
})

test('unbekannte Sammlung fällt auf „Alle“ zurück', () => {
  assert.equal(normalizeScope('meine'), SCOPE_MINE)
  assert.equal(normalizeScope('3'), 3)
  assert.equal(normalizeScope('quatsch'), SCOPE_ALL)
  assert.equal(normalizeScope(undefined), SCOPE_ALL)
})

test('Kurzname und Avatarfarbe', () => {
  assert.equal(shortFamilyName('Familie Pastoors'), 'Pastoors')
  assert.equal(shortFamilyName('Kochclub Köln'), 'Kochclub Köln')
  assert.equal(shortFamilyName('Familie'), 'Familie')
  assert.equal(avatarTone(7), 'mini-avatar--t1')
  assert.equal(avatarTone(null), 'mini-avatar--t0')
})

test('Willkommensgruß nach dem Beitritt', () => {
  assert.equal(welcomeText('Familie Pastoors'), 'Willkommen in der Familie Pastoors')
  assert.equal(welcomeText('Kochclub Köln'), 'Willkommen bei „Kochclub Köln“')
})

test('Familie gründen: Zahl der eigenen Rezepte fürs Häkchen', () => {
  assert.equal(ownRecipeCount(recipes, 7), 2)
  assert.equal(ownRecipeCount(recipes, 8), 1)
  assert.equal(ownRecipeCount(recipes, 42), 0)
  assert.equal(ownRecipeCount(recipes, null), 0, 'ohne Konto nichts')
  assert.equal(ownRecipeCount(undefined, 7), 0)
})

test('Familie gründen: Beschriftung mit Einzahl/Mehrzahl', () => {
  assert.equal(shareOwnLabel(50), 'Alle meine Rezepte für diese Familie freigeben (50 Rezepte)')
  assert.equal(shareOwnLabel(1), 'Alle meine Rezepte für diese Familie freigeben (1 Rezept)')
})
