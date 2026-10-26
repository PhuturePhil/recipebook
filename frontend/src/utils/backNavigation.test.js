import { test } from 'node:test'
import assert from 'node:assert/strict'
import { pathOf, isListPath, backAction, navigateBackTo, formExitTarget, headerBack, recipeBackAction, LIST_PATH } from './backNavigation.js'

const fakeRouter = () => {
  const calls = []
  return {
    calls,
    back: () => calls.push(['back']),
    replace: (to) => calls.push(['replace', to]),
    push: (to) => calls.push(['push', to]),
  }
}

test('pathOf schneidet Query und Anker ab', () => {
  assert.equal(pathOf('/'), '/')
  assert.equal(pathOf('/?q=nudeln'), '/')
  assert.equal(pathOf('/#oben'), '/')
  assert.equal(pathOf('/rezepte/5#bewertungen'), '/rezepte/5')
  assert.equal(pathOf('/rezepte/5/bearbeiten?x=1'), '/rezepte/5/bearbeiten')
  assert.equal(pathOf(''), '')
  assert.equal(pathOf(null), '')
  assert.equal(pathOf(undefined), '')
})

test('isListPath erkennt die Liste auch mit Query/Filter', () => {
  assert.equal(isListPath('/'), true)
  assert.equal(isListPath('/?q=%23Pasta&sort=neu'), true)
  assert.equal(isListPath('/rezepte/5'), false)
  assert.equal(isListPath('/rezepte/neu'), false)
  assert.equal(isListPath('/changelog'), false)
  assert.equal(isListPath(null), false)
})

test('backAction: vorigen Eintrag nur nutzen, wenn er das Ziel ist', () => {
  assert.equal(backAction('/', '/'), 'back')
  assert.equal(backAction('/?q=x', '/'), 'back')
  assert.equal(backAction('/rezepte/5', '/rezepte/5'), 'back')
  assert.equal(backAction('/rezepte/5#bewertungen', '/rezepte/5'), 'back')
  assert.equal(backAction('/rezepte/5/bearbeiten', '/'), 'replace')
  assert.equal(backAction('/rezepte/5', '/'), 'replace')
  assert.equal(backAction('/rezepte/neu', '/'), 'replace')
  assert.equal(backAction('/', '/rezepte/12'), 'replace')
  assert.equal(backAction(null, '/'), 'replace')
  assert.equal(backAction(undefined, '/rezepte/5'), 'replace')
})

test('formExitTarget: Speichern → Rezeptseite, Abbrechen → Rezeptseite bzw. Liste', () => {
  assert.equal(formExitTarget({ isEdit: true, recipeId: '5', savedId: '5' }), '/rezepte/5')
  assert.equal(formExitTarget({ isEdit: false, recipeId: undefined, savedId: 42 }), '/rezepte/42')
  assert.equal(formExitTarget({ isEdit: true, recipeId: '5' }), '/rezepte/5')
  assert.equal(formExitTarget({ isEdit: false }), LIST_PATH)
})

// Speichern/Abbrechen im Formular: nie push, Formular-Eintrag fällt aus dem Verlauf
test('Bearbeiten speichern aus Liste → Rezept → Formular: zurück zur bestehenden Rezeptseite', () => {
  const r = fakeRouter()
  navigateBackTo(r, formExitTarget({ isEdit: true, recipeId: '5', savedId: '5' }), '/rezepte/5')
  assert.deepEqual(r.calls, [['back']])
})

test('Bearbeiten speichern ohne Verlauf (Formular direkt geöffnet): Formular wird ersetzt', () => {
  const r = fakeRouter()
  navigateBackTo(r, formExitTarget({ isEdit: true, recipeId: '5', savedId: '5' }), null)
  assert.deepEqual(r.calls, [['replace', '/rezepte/5']])
})

test('Neues Rezept speichern: Formular wird durch die neue Rezeptseite ersetzt', () => {
  const r = fakeRouter()
  navigateBackTo(r, formExitTarget({ isEdit: false, savedId: 77 }), '/')
  assert.deepEqual(r.calls, [['replace', '/rezepte/77']])
})

test('Abbrechen beim Bearbeiten und bei Neuanlage geht zurück statt neu zu öffnen', () => {
  const r = fakeRouter()
  navigateBackTo(r, formExitTarget({ isEdit: true, recipeId: '5' }), '/rezepte/5')
  navigateBackTo(r, formExitTarget({ isEdit: false }), '/?q=suppe')
  navigateBackTo(r, formExitTarget({ isEdit: false }), '/rezepte/9')
  assert.deepEqual(r.calls, [['back'], ['back'], ['replace', '/']])
})

test('nie push: kein Eintrag wächst den Verlauf', () => {
  const r = fakeRouter()
  for (const back of ['/', '/rezepte/5', '/rezepte/5/bearbeiten', null, '/changelog']) {
    navigateBackTo(r, '/', back)
    navigateBackTo(r, '/rezepte/5', back)
    headerBack(r, '/', back)
    headerBack(r, true, back)
  }
  assert.equal(r.calls.some(([m]) => m === 'push'), false)
})

// Rezeptseite: Zurück-Pfeil führt immer zur Liste
test('Rezeptseite aus der Liste (auch mit Filter): router.back() erhält Scrollposition und Suche', () => {
  const r = fakeRouter()
  headerBack(r, '/', '/')
  headerBack(r, '/', '/?q=%23Suppe')
  assert.deepEqual(r.calls, [['back'], ['back']])
})

test('Rezeptseite nach Speichern einer Neuanlage oder als Direktlink: Liste ersetzt den Eintrag', () => {
  const r = fakeRouter()
  headerBack(r, '/', '/rezepte/neu')
  headerBack(r, '/', '/rezepte/5/bearbeiten')
  headerBack(r, '/', '/rezepte/3')
  headerBack(r, '/', null)
  assert.deepEqual(r.calls, [['replace', '/'], ['replace', '/'], ['replace', '/'], ['replace', '/']])
})

test('andere Seiten (back = true): vorige Seite, ohne Verlauf Liste ersetzt', () => {
  const r = fakeRouter()
  headerBack(r, true, '/rezepte/5')
  headerBack(r, true, null)
  assert.deepEqual(r.calls, [['back'], ['replace', '/']])
})

test('Rezeptseite aus dem Essensplan: zurück in den Plan (Ansicht und Woche/Monat bleiben)', () => {
  const r = fakeRouter()
  headerBack(r, '/', '/plan')
  headerBack(r, '/', '/plan?ansicht=monat&datum=2026-11-01')
  assert.deepEqual(r.calls, [['back'], ['back']])
  assert.equal(recipeBackAction('/plan?ansicht=liste'), 'back')
  assert.equal(recipeBackAction('/planung'), 'replace')
  assert.equal(recipeBackAction(null), 'replace')
})
