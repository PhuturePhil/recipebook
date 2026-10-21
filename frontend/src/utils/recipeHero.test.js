import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  recipeBadges,
  badgeIcon,
  formatPrepTime,
  sourceLabel,
  cardMeta,
  cardTagRow,
  amountText,
  createdShort,
} from './recipeHero.js'

test('recipeBadges: Nährwert-Badges plus „Schnell“ bis 30 Minuten, ohne Doppel', () => {
  assert.deepEqual(recipeBadges(['Fettarm'], 20), ['Fettarm', 'Schnell'])
  assert.deepEqual(recipeBadges(['Fettarm'], 31), ['Fettarm'])
  assert.deepEqual(recipeBadges(null, null), [])
  assert.deepEqual(recipeBadges(['Schnell'], 10), ['Schnell'])
})

test('badgeIcon: Blitz, Tropfen, Hantel für die drei Kern-Eigenschaften', () => {
  assert.equal(badgeIcon('Schnell'), 'blitz')
  assert.equal(badgeIcon('Fettarm'), 'tropfen')
  assert.equal(badgeIcon('Proteinreich'), 'hantel')
  assert.ok(badgeIcon('Unbekannt'))
})

test('formatPrepTime: Minuten und Stunden', () => {
  assert.equal(formatPrepTime(45), '45 Min.')
  assert.equal(formatPrepTime(95), '1 Std. 35 Min.')
  assert.equal(formatPrepTime(120), '2 Std.')
  assert.equal(formatPrepTime(0), '')
})

test('sourceLabel: Autor und Quelle, Webadresse als Domain, leer → leer', () => {
  assert.equal(sourceLabel({ author: 'Anna Jones', source: 'Easy Wins' }), 'Anna Jones, Easy Wins')
  assert.equal(sourceLabel({ author: '', source: 'https://www.zuckerjagdwurst.com/de/rezepte/x' }), 'zuckerjagdwurst.com')
  assert.equal(sourceLabel({ author: 'Paco', source: null }), 'Paco')
  assert.equal(sourceLabel({ author: '  ', source: '' }), '')
  assert.equal(sourceLabel({ author: 'Paco', source: 'Paco' }), 'Paco')
  assert.equal(sourceLabel({ author: 'Zucker&Jagdwurst', source: 'https://www.zuckerjagdwurst.com/de/x' }), 'zuckerjagdwurst.com')
})

test('cardMeta: Zeit · Zutaten · kcal, nur vorhandene Werte, unvollständig mit „mind.“', () => {
  const r = { prepTimeMinutes: 45, ingredientCount: 12, nutrition: { complete: true, perServing: { kcal: 446.6 } } }
  assert.equal(cardMeta(r), '45 Min. · 12 Zutaten · 447 kcal')
  assert.equal(cardMeta({ ...r, nutrition: { complete: false, perServing: { kcal: 1234 } } }), '45 Min. · 12 Zutaten · mind. 1.234 kcal')
  assert.equal(cardMeta({ ingredientCount: 1 }), '1 Zutat')
  assert.equal(cardMeta({}), '')
})

test('cardTagRow: Eigenschaften zuerst, höchstens drei Einträge, Rest als +N', () => {
  const row = cardTagRow(['Proteinreich'], ['Ofengericht', 'Indisch', 'Dal'])
  assert.deepEqual(row.items.map((i) => `${i.kind}:${i.name}`), ['badge:Proteinreich', 'tag:Ofengericht', 'tag:Indisch'])
  assert.equal(row.more, 1)
  assert.deepEqual(cardTagRow([], ['Pasta']), { items: [{ kind: 'tag', name: 'Pasta' }], more: 0 })
  assert.deepEqual(cardTagRow(null, null), { items: [], more: 0 })
})

test('amountText: Menge + Einheit, sonst „n. Bedarf“ (englisch „to taste“)', () => {
  assert.deepEqual(amountText({ amount: '1,2', unit: 'kg' }), { text: '1,2 kg', toTaste: false })
  assert.deepEqual(amountText({ amount: '', unit: 'Prise' }), { text: 'Prise', toTaste: false })
  assert.deepEqual(amountText({ amount: '', unit: '' }), { text: 'n. Bedarf', toTaste: true })
  assert.deepEqual(amountText({}, 'en'), { text: 'to taste', toTaste: true })
})

test('createdShort: eigenes Rezept nur mit Datum, fremdes mit Namen', () => {
  assert.equal(createdShort('Paco', '2026-10-05T07:17:45', true), 'Angelegt 05.10.2026')
  assert.equal(createdShort('Anna Pastoors', '2026-10-05T07:17:45', false), 'Angelegt von Anna Pastoors am 05.10.2026')
  assert.equal(createdShort('', '2026-10-05T07:17:45', false), 'Angelegt 05.10.2026')
  assert.equal(createdShort('Paco', null, true), '')
})
