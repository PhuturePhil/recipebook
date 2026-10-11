import { test } from 'node:test'
import assert from 'node:assert/strict'
import { detailActions, replacesTabBar, cycleFocus, formActions } from './detailActions.js'

test('detailActions: mit Bearbeitungsrecht Löschen, Bearbeiten, Bewerten in dieser Reihenfolge', () => {
  assert.deepEqual(detailActions(true).map((a) => a.id), ['delete', 'edit', 'rate'])
  assert.deepEqual(detailActions(true).map((a) => a.label), ['Löschen', 'Bearbeiten', 'Bewerten'])
})

test('detailActions: ohne Bearbeitungsrecht nur Bewerten', () => {
  assert.deepEqual(detailActions(false).map((a) => a.id), ['rate'])
})

test('detailActions: Linien-Icons aus dem App-Set, nur Löschen als Gefahr markiert', () => {
  const byId = Object.fromEntries(detailActions(true).map((a) => [a.id, a]))
  assert.equal(byId.delete.icon, 'loeschen')
  assert.equal(byId.edit.icon, 'bearbeiten')
  assert.equal(byId.rate.icon, 'stern')
  assert.equal(byId.delete.danger, true)
  assert.ok(!byId.edit.danger && !byId.rate.danger)
})

test('replacesTabBar: Formular und Rezeptseite ersetzen die Tab-Leiste, sonst nicht', () => {
  assert.equal(replacesTabBar('recipe-new'), true)
  assert.equal(replacesTabBar('recipe-edit'), true)
  assert.equal(replacesTabBar('recipe-detail'), true)
  for (const name of ['home', 'ingredients', 'changelog', 'shared-recipe', undefined]) {
    assert.equal(replacesTabBar(name), false, String(name))
  }
})

test('cycleFocus: läuft im Kreis vor und zurück', () => {
  assert.equal(cycleFocus(4, 0), 1)
  assert.equal(cycleFocus(4, 3), 0)
  assert.equal(cycleFocus(4, 0, true), 3)
  assert.equal(cycleFocus(4, 2, true), 1)
})

test('cycleFocus: Fokus außerhalb springt an den Anfang bzw. ans Ende, leere Liste → -1', () => {
  assert.equal(cycleFocus(3, -1), 0)
  assert.equal(cycleFocus(3, -1, true), 2)
  assert.equal(cycleFocus(0, -1), -1)
  assert.equal(cycleFocus(1, 0), 0)
})

test('formActions: Abbrechen links, rechts Speichern beim Bearbeiten bzw. Rezept erstellen bei Neuanlage', () => {
  assert.deepEqual(formActions(true).map((a) => a.id), ['cancel', 'submit'])
  assert.deepEqual(formActions(true).map((a) => a.label), ['Abbrechen', 'Speichern'])
  assert.deepEqual(formActions(false).map((a) => a.label), ['Abbrechen', 'Rezept erstellen'])
})

test('formActions: Linien-Icons X und Haken, beim Speichern Sanduhr mit Hinweistext', () => {
  assert.deepEqual(formActions(false).map((a) => a.icon), ['schliessen', 'haken'])
  const saving = formActions(true, true)
  assert.equal(saving[0].label, 'Abbrechen')
  assert.equal(saving[1].label, 'Wird gespeichert…')
  assert.equal(saving[1].icon, 'sanduhr')
})

test('formActions: keine Gefahr-Markierung und kein Aktiv-Zustand', () => {
  for (const a of [...formActions(true), ...formActions(false, true)]) {
    assert.ok(!a.danger && !a.active, a.id)
  }
})
