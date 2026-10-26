import { test } from 'node:test'
import assert from 'node:assert/strict'
import { detailActions, replacesTabBar, cycleFocus, formActions } from './detailActions.js'
import { bottomBarLayout } from './bottomBar.js'

test('detailActions: mit Bearbeitungsrecht Leiste Bearbeiten + Planen + Bewerten, „Mehr“-Menü mit Löschen', () => {
  const { menu, bar } = detailActions(true)
  assert.deepEqual(menu.map((a) => a.id), ['delete'])
  assert.deepEqual(menu.map((a) => a.label), ['Rezept löschen…'])
  assert.deepEqual(bar.map((a) => a.id), ['edit', 'plan', 'rate'])
  assert.deepEqual(bar.map((a) => a.label), ['Bearbeiten', 'Planen', 'Bewerten'])
})

test('detailActions: ohne Bearbeitungsrecht Planen und Bewerten, kein Menü', () => {
  const { menu, bar } = detailActions(false)
  assert.deepEqual(menu, [])
  assert.deepEqual(bar.map((a) => a.id), ['plan', 'rate'])
})

test('detailActions: Icons, nur Löschen als Gefahr markiert', () => {
  const { menu, bar } = detailActions(true)
  const byId = Object.fromEntries([...menu, ...bar].map((a) => [a.id, a]))
  assert.equal(byId.delete.icon, 'loeschen')
  assert.equal(byId.edit.icon, 'bearbeiten')
  assert.equal(byId.rate.icon, 'stern')
  assert.equal(byId.plan.icon, 'planen')
  assert.equal(byId.delete.danger, true)
  assert.ok(!byId.edit.danger && !byId.rate.danger && !byId.plan.danger)
})

test('detailActions: Bewerten ist die einzige Hauptaktion (orange), Bearbeiten und Planen neutral', () => {
  const { bar } = detailActions(true)
  assert.equal(bar.at(-1).id, 'rate')
  assert.equal(bar.at(-1).primary, true)
  assert.ok(!bar[0].primary && !bar[1].primary)
  assert.equal(bar.filter((a) => a.primary).length, 1)
  assert.equal(detailActions(false).bar.at(-1).primary, true)
})

test('detailActions: passt in die generische Leiste – Löschen landet auch per Layout im Menü', () => {
  const { bar, menu } = detailActions(true)
  const layout = bottomBarLayout(bar, menu)
  assert.deepEqual(layout.bar.map((a) => a.id), ['edit', 'plan', 'rate'])
  assert.deepEqual(layout.menu.map((a) => a.id), ['delete'])
  assert.deepEqual(bottomBarLayout([...bar, ...menu]).menu.map((a) => a.id), ['delete'])
})

test('detailActions: Leiste wie Entwurf – Bearbeiten | Planen | Bewerten | Mehr, höchstens vier', () => {
  const { bar, menu } = detailActions(true)
  const layout = bottomBarLayout(bar, menu)
  assert.equal(layout.bar.length + (layout.menu.length ? 1 : 0), 4)
  assert.equal(detailActions(false).bar.find((a) => a.id === 'plan').popup, 'dialog')
})

test('replacesTabBar: Formular und Rezeptseite ersetzen die Tab-Leiste, sonst nicht', () => {
  assert.equal(replacesTabBar('recipe-new'), true)
  assert.equal(replacesTabBar('recipe-edit'), true)
  assert.equal(replacesTabBar('recipe-detail'), true)
  for (const name of ['home', 'meal-plan', 'ingredients', 'changelog', 'shared-recipe', undefined]) {
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
