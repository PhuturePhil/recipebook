import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createRouter, createMemoryHistory } from 'vue-router'
import {
  LEGACY_REDIRECTS, NEW_RECIPE_PATH, INGREDIENTS_PATH, RECIPE_DETAIL_ROUTE, RECIPE_EDIT_ROUTE, recipePath, recipeEditPath,
} from './paths.js'

const Stub = { render: () => null }

// Dieselben Routen-Muster wie router/index.js, Komponenten nur als Platzhalter
function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: Stub },
      { path: INGREDIENTS_PATH, name: 'ingredients', component: Stub },
      { path: RECIPE_DETAIL_ROUTE, name: 'recipe-detail', component: Stub },
      { path: NEW_RECIPE_PATH, name: 'recipe-new', component: Stub },
      { path: RECIPE_EDIT_ROUTE, name: 'recipe-edit', component: Stub },
      { path: '/share/:token', name: 'shared-recipe', component: Stub },
      ...LEGACY_REDIRECTS,
      { path: '/:pathMatch(.*)*', name: 'not-found', component: Stub },
    ],
  })
}

async function open(path) {
  const router = makeRouter()
  await router.push(path)
  return router.currentRoute.value
}

test('deutsche Pfade', () => {
  assert.equal(recipePath(42), '/rezepte/42')
  assert.equal(recipeEditPath(42), '/rezepte/42/bearbeiten')
  assert.equal(NEW_RECIPE_PATH, '/rezepte/neu')
  assert.equal(INGREDIENTS_PATH, '/zutaten')
})

test('neue Pfade treffen ihre Seiten', async () => {
  assert.equal((await open('/rezepte/42')).name, 'recipe-detail')
  assert.equal((await open('/rezepte/neu')).name, 'recipe-new')
  assert.equal((await open('/rezepte/42/bearbeiten')).name, 'recipe-edit')
  assert.equal((await open('/zutaten')).name, 'ingredients')
  assert.equal((await open('/rezepte/abc')).name, 'not-found')
})

test('alte Rezept-Adresse leitet um und behält Anker und Query', async () => {
  const r = await open('/recipe/42?lang=de#bewertungen')
  assert.equal(r.name, 'recipe-detail')
  assert.equal(r.fullPath, '/rezepte/42?lang=de#bewertungen')
  assert.equal(r.redirectedFrom?.path, '/recipe/42')
})

test('alte Neu-, Bearbeiten- und Zutaten-Adressen leiten um', async () => {
  assert.equal((await open('/recipe/new')).fullPath, '/rezepte/neu')
  assert.equal((await open('/recipe/7/edit')).fullPath, '/rezepte/7/bearbeiten')
  assert.equal((await open('/ingredients?q=reis')).fullPath, '/zutaten?q=reis')
})

test('Teilen-Links bleiben unverändert', async () => {
  const r = await open('/share/AbC_123-xyz')
  assert.equal(r.name, 'shared-recipe')
  assert.equal(r.fullPath, '/share/AbC_123-xyz')
})
