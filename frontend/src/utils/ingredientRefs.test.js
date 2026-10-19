import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  ingredientRefsVisible,
  ingredientCores,
  segmentStep,
  createIngredientMatcher,
  refLines,
  refLabel,
  popoverPlacement,
  createPopoverState,
} from './ingredientRefs.js'
import { scaleIngredients } from './scaleIngredients.js'

// Rezept 166 (Kartoffelgratin), Stand 19.10.2026, für 4 Personen
const GRATIN = [
  { amount: '1.2', unit: 'kg', name: 'Kartoffeln, vorwiegend festkochend' },
  { amount: '400', unit: 'ml', name: 'Schlagsahne (mind. 30 % Fett)' },
  { amount: '200', unit: 'ml', name: 'Milch' },
  { amount: '1', unit: 'Stück', name: 'Knoblauchzehe' },
  { amount: '120', unit: 'g', name: 'Bergkäse oder Gruyère, gerieben' },
  { amount: '1', unit: 'TL', name: 'Salz' },
  { amount: '', unit: '', name: 'Pfeffer' },
  { amount: '', unit: '', name: 'Muskatnuss, frisch gerieben' },
  { amount: '', unit: '', name: 'Butter für die Form' },
]
const STEPS = [
  'Ofen auf 180 °C Ober-/Unterhitze vorheizen. Auflaufform buttern und mit der halbierten Knoblauchzehe ausreiben.',
  'Kartoffeln schälen und in 2–3 mm dünne Scheiben hobeln. Nicht wässern — die Stärke bindet die Sauce.',
  'Sahne, Milch, Salz, Pfeffer und Muskat verrühren, mit den Kartoffelscheiben mischen, in die Form schichten und glatt drücken. Die Flüssigkeit soll die Scheiben knapp bedecken.',
  '45 Minuten backen. Dann den Käse darüberstreuen und weitere 15–20 Minuten backen, bis die Kruste goldbraun ist und die Kartoffeln weich sind (Messertest).',
  '10 Minuten ruhen lassen, dann wird das Gratin schnittfest.',
]

const hits = (text, ingredients = GRATIN, lang = 'de') =>
  segmentStep(text, ingredients, lang).filter((s) => s.refs).map((s) => [s.text, s.refs])

test('Freigabe Stufe 1: nur Admins', () => {
  assert.equal(ingredientRefsVisible({ role: 'ADMIN' }), true)
  assert.equal(ingredientRefsVisible({ role: 'USER' }), false)
  assert.equal(ingredientRefsVisible(null), false)
  assert.equal(ingredientRefsVisible(undefined), false)
})

test('Kernname: vor Komma/Klammer, beide Alternativen, ohne „für die Form“', () => {
  assert.deepEqual(ingredientCores('Kartoffeln, vorwiegend festkochend'), ['Kartoffeln'])
  assert.deepEqual(ingredientCores('Schlagsahne (mind. 30 % Fett)'), ['Schlagsahne'])
  assert.deepEqual(ingredientCores('Bergkäse oder Gruyère, gerieben'), ['Bergkäse', 'Gruyère'])
  assert.deepEqual(ingredientCores('Butter für die Form'), ['Butter'])
  assert.deepEqual(ingredientCores('Semmelbrösel zum Bestreuen'), ['Semmelbrösel'])
  assert.deepEqual(ingredientCores('heavy cream, for whipping', 'en'), ['heavy cream'])
  assert.deepEqual(ingredientCores('butter or margarine', 'en'), ['butter', 'margarine'])
})

test('Rezept 166: alle erwarteten Treffer, nichts zu viel', () => {
  assert.deepEqual(hits(STEPS[0]), [['Knoblauchzehe', [3]]])
  assert.deepEqual(hits(STEPS[1]), [['Kartoffeln', [0]]])
  assert.deepEqual(hits(STEPS[2]), [
    ['Sahne', [1]], ['Milch', [2]], ['Salz', [5]], ['Pfeffer', [6]], ['Muskat', [7]], ['Kartoffelscheiben', [0]],
  ])
  assert.deepEqual(hits(STEPS[3]), [['Käse', [4]], ['Kartoffeln', [0]]])
  assert.deepEqual(hits(STEPS[4]), [])
})

test('Kartoffeln ↔ Kartoffelscheiben (Kompositum im Schritt)', () => {
  assert.deepEqual(hits('mit den Kartoffelscheiben mischen'), [['Kartoffelscheiben', [0]]])
  assert.deepEqual(hits('Die Kartoffel halbieren'), [['Kartoffel', [0]]])
})

test('Sahne ↔ „Schlagsahne (mind. 30 % Fett)“, Käse ↔ „Bergkäse oder Gruyère, gerieben“, Muskat ↔ Muskatnuss', () => {
  assert.deepEqual(hits('Die Sahne erhitzen'), [['Sahne', [1]]])
  assert.deepEqual(hits('Den Käse darüberstreuen'), [['Käse', [4]]])
  assert.deepEqual(hits('Mit Gruyère bestreuen'), [['Gruyère', [4]]])
  assert.deepEqual(hits('Mit Muskat würzen'), [['Muskat', [7]]])
  assert.deepEqual(hits('Etwas Muskatnuss reiben'), [['Muskatnuss', [7]]])
})

test('Negativfall: „buttern“ trifft „Butter für die Form“ nicht, „Form“ auch nicht', () => {
  assert.deepEqual(hits('Auflaufform buttern.'), [])
  assert.deepEqual(hits('in die Form schichten'), [])
  assert.deepEqual(hits('Die Butter schmelzen'), [['Butter', [8]]])
})

test('Negativfall: „Eine“ und „Ein“ treffen „Ei“ bzw. „Eier“ nicht', () => {
  for (const name of ['Ei', 'Eier', 'Ei (Größe M)']) {
    const eggs = [{ amount: '2', unit: '', name }]
    assert.deepEqual(hits('Eine Schüssel bereitstellen. Ein Ei verquirlen.', eggs), [['Ei', [0]]], name)
    assert.deepEqual(hits('Eine Prise Zucker, ein Einmachglas.', eggs), [], name)
  }
  assert.deepEqual(hits('Die Eier trennen', [{ amount: '2', unit: '', name: 'Eier' }]), [['Eier', [0]]])
})

test('nur großgeschriebene Wörter, Endungen -n/-en/-e/-s/-es', () => {
  const list = [{ amount: '2', unit: '', name: 'Tomate' }, { amount: '1', unit: 'Bund', name: 'Basilikum' }]
  assert.deepEqual(hits('Tomaten würfeln, Basilikums Blätter zupfen', list), [['Tomaten', [0]], ['Basilikums', [1]]])
  assert.deepEqual(hits('tomaten klein schneiden', list), [])
})

test('Öl trifft Olivenöl, längster Treffer gewinnt, Phrasen zusammen', () => {
  const list = [
    { amount: '3', unit: 'EL', name: 'Olivenöl' },
    { amount: '2', unit: 'EL', name: 'Tomatenmark' },
    { amount: '400', unit: 'g', name: 'Tomaten' },
    { amount: '200', unit: 'g', name: 'Crème fraîche' },
  ]
  assert.deepEqual(hits('Öl erhitzen, Tomatenmark anrösten, Tomaten dazu', list), [
    ['Öl', [0]], ['Tomatenmark', [1]], ['Tomaten', [2]],
  ])
  assert.deepEqual(hits('Zum Schluss die Crème fraîche unterrühren', list), [['Crème fraîche', [3]]])
})

test('alle Vorkommen markiert, Dubletten in zwei Gruppen → beide Zutaten', () => {
  const list = [
    { amount: '100', unit: 'g', name: 'Zucker', groupName: 'Teig' },
    { amount: '2', unit: 'EL', name: 'Zucker', groupName: 'Streusel' },
  ]
  assert.deepEqual(hits('Zucker und Zucker', list), [['Zucker', [0, 1]], ['Zucker', [0, 1]]])
  assert.deepEqual(refLines([0, 1], list), [
    { amount: '100 g', hasAmount: true, name: 'Zucker', group: 'Teig' },
    { amount: '2 EL', hasAmount: true, name: 'Zucker', group: 'Streusel' },
  ])
})

test('englische Rezepte: kleingeschrieben mit Wortgrenzen, Plural', () => {
  const list = [
    { amount: '2', unit: 'cups', name: 'heavy cream' },
    { amount: '3', unit: '', name: 'large eggs' },
    { amount: '1', unit: 'cup', name: 'blueberries' },
    { amount: '2', unit: 'tbsp', name: 'butter, for the pan' },
  ]
  assert.deepEqual(hits('Whisk the eggs with the heavy cream, fold in the berries.', list, 'en'), [
    ['eggs', [1]], ['heavy cream', [0]],
  ])
  assert.deepEqual(hits('Add one egg, then the cream and a blueberry.', list, 'en'), [
    ['egg', [1]], ['cream', [0]], ['blueberry', [2]],
  ])
  assert.deepEqual(hits('Buttermilk is not butter: butter the pan.', list, 'en'), [['butter', [3]], ['butter', [3]]])
})

test('Segmente ergeben zusammen genau den Schritttext', () => {
  const match = createIngredientMatcher(GRATIN)
  for (const step of STEPS) assert.equal(match(step).map((s) => s.text).join(''), step)
  assert.deepEqual(match(''), [])
  assert.deepEqual(createIngredientMatcher([])('Nur Text'), [{ text: 'Nur Text' }])
})

test('Popover-Zeilen: skalierte Menge, „nach Bedarf“ ohne Menge', () => {
  const scaled = scaleIngredients(GRATIN, 4, 6)
  assert.deepEqual(refLines([0], scaled), [
    { amount: '1,8 kg', hasAmount: true, name: 'Kartoffeln, vorwiegend festkochend', group: null },
  ])
  assert.deepEqual(refLines([7], scaled), [
    { amount: 'nach Bedarf', hasAmount: false, name: 'Muskatnuss, frisch gerieben', group: null },
  ])
  assert.equal(refLines([6], scaled, 'en')[0].amount, 'to taste')
  assert.equal(refLabel({ text: 'Kartoffeln' }), 'Menge anzeigen: Kartoffeln')
})

test('Popover bleibt im Bild: unten, oben wenn kein Platz, seitlich begrenzt', () => {
  const view = { width: 200, height: 60, viewportWidth: 390, viewportHeight: 844 }
  const below = popoverPlacement({ ...view, anchor: { top: 100, bottom: 124, left: 150, right: 230 } })
  assert.deepEqual(below, { top: 130, left: 90, up: false })
  const above = popoverPlacement({ ...view, anchor: { top: 800, bottom: 824, left: 10, right: 60 } })
  assert.deepEqual(above, { top: 734, left: 8, up: true })
  const right = popoverPlacement({ ...view, anchor: { top: 100, bottom: 124, left: 360, right: 385 } })
  assert.equal(right.left, 390 - 200 - 8)
})

test('Popover-Zustand: nur eins offen, Tap öffnet/schließt, Hover nur bis zum Verlassen', () => {
  const state = createPopoverState()
  const seen = []
  state.subscribe((open) => seen.push(open?.key ?? null))
  state.click('0:1')
  assert.equal(state.isOpen('0:1'), true)
  state.click('2:3')
  assert.equal(state.isOpen('0:1'), false)
  assert.equal(state.isOpen('2:3'), true)
  state.click('2:3')
  assert.equal(state.open, null)
  state.hover('1:1')
  state.leave('1:1')
  assert.equal(state.open, null)
  state.hover('1:1')
  state.click('1:1')
  state.leave('1:1')
  assert.equal(state.isOpen('1:1'), true, 'Klick nach Hover hält das Popover offen')
  state.blur('1:1')
  assert.equal(state.open, null)
  state.focus('0:1')
  state.click('0:1')
  assert.equal(state.isOpen('0:1'), true, 'Enter auf fokussiertem Wort schließt nicht sofort')
  state.close()
  assert.deepEqual(seen, ['0:1', '2:3', null, '1:1', null, '1:1', '1:1', null, '0:1', '0:1', null])
})
