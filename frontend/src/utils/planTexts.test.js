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
  assert.equal(planText('planOnlyNote', { name: 'Familie Hinze' }, 'de'), '„Familie Hinze“ sieht dieses Rezept nur über den Essensplan.')
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

test('planTexts: belegte Tage im Datums-Picker – „belegt“ bzw. „planned“', () => {
  assert.equal(planText('booked', {}, 'de'), 'belegt')
  assert.equal(planText('booked', {}, 'en'), 'planned')
})

test('planTexts: Nur-Plan-Sicht und Geist-Eintrag wörtlich wie abgenommen', () => {
  assert.equal(planText('planOnlyBy', { name: 'Silvia' }, 'de'), 'Über den Essensplan geteilt von Silvia.')
  assert.equal(planText('planOnlyRules', {}, 'de'),
    'Das Rezept stammt aus einer anderen Familie und darf nicht weitergegeben oder kopiert werden. Teilen und PDF sind deshalb deaktiviert.')
  assert.equal(planText('rateOnlyOrigin', {}, 'de'), 'Bewertungen sind für dieses Rezept nur in seiner Ursprungsfamilie möglich.')
  assert.equal(planText('shareOrPlanTitle', {}, 'de'), 'Mit Familie teilen oder nur über den Plan sichtbar machen?')
  assert.equal(planText('gone', {}, 'de'), 'Nicht mehr verfügbar')
  assert.equal(planText('goneText', {}, 'de'), 'Dieses Rezept kann hier nicht mehr geöffnet werden: Es wurde gelöscht, '
    + 'oder niemand in dieser Familie hat mehr Zugriff darauf. Der Plan-Eintrag bleibt als Erinnerung stehen.')
  assert.equal(planText('goneOriginal', {}, 'de'), 'Dort kannst du das Original weiterhin ansehen.')
  assert.equal(planText('removeEntry', {}, 'de'), 'Eintrag entfernen…')
  assert.equal(planText('planFor', {}, 'en'), 'Plan for')
})
