import { test } from 'node:test'
import assert from 'node:assert/strict'
import { PLAN_LANGUAGES, planText, planTextKeys, planLanguage } from './planTexts.js'

test('planTexts: Deutsch und Englisch haben dieselben Schlüssel, keine leeren Texte', () => {
  assert.deepEqual(PLAN_LANGUAGES, ['de', 'en'])
  assert.deepEqual([...planTextKeys('en')].sort(), [...planTextKeys('de')].sort())
  for (const lang of PLAN_LANGUAGES) {
    for (const key of planTextKeys(lang)) assert.ok(planText(key, {}, lang).trim(), `${lang}.${key}`)
  }
})

test('planText: Platzhalter, unbekannte Sprache fällt auf Deutsch zurück', () => {
  assert.equal(planText('shareTitle', { name: 'Starter :)' }, 'de'), 'Mit Familie „Starter :)“ teilen?')
  assert.equal(planText('planned', { day: 'Monday, 26 October' }, 'en'), 'Planned for Monday, 26 October')
  assert.equal(planText('title', {}, 'fr'), 'Essensplan')
  assert.equal(planText('gibtsnicht', {}, 'de'), 'gibtsnicht')
})

test('planTexts: Nav und Rezeptseite wie im Entwurf, kein „veröffentlichen“ in der Oberfläche', () => {
  assert.equal(planText('navLabel', {}, 'de'), 'Plan')
  assert.equal(planText('planAction', {}, 'de'), 'Planen')
  assert.equal(planText('planDish', {}, 'de'), 'Gericht planen')
  for (const key of planTextKeys('de')) assert.doesNotMatch(planText(key, {}, 'de'), /veröffentlich/i, key)
})

test('planLanguage: ohne Dokument Deutsch', () => {
  assert.equal(planLanguage(), 'de')
})
