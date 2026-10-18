import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  formatRating,
  ratingLabel,
  ratingCountLabel,
  ratingSummary,
  markUpdated,
  formatRatingDate,
  createdLine,
  raterName,
  RATINGS_ANCHOR,
  ratingsRoute,
  wantsRatings,
} from './ratings.js'

test('formatRating rundet auf eine Nachkommastelle mit Komma', () => {
  assert.equal(formatRating(4.25), '4,3')
  assert.equal(formatRating(4), '4,0')
  assert.equal(formatRating(4.333333333), '4,3')
  assert.equal(formatRating(4.35), '4,4')
  assert.equal(formatRating(4.96), '5,0')
  assert.equal(formatRating(1), '1,0')
})

test('formatRating ohne Wert liefert leeren Text', () => {
  assert.equal(formatRating(null), '')
  assert.equal(formatRating(undefined), '')
  assert.equal(formatRating(NaN), '')
})

test('ratingLabel zeigt Durchschnitt und Anzahl, ohne Bewertungen nichts', () => {
  assert.equal(ratingLabel(4.333, 6), '4,3 (6)')
  assert.equal(ratingLabel(null, 0), '')
  assert.equal(ratingLabel(null, null), '')
})

test('ratingCountLabel unterscheidet Einzahl und Mehrzahl', () => {
  assert.equal(ratingCountLabel(1), '1 Bewertung')
  assert.equal(ratingCountLabel(6), '6 Bewertungen')
})

test('ratingSummary mittelt über alle Bewertungen, auch Mehrfachbewertungen', () => {
  assert.deepEqual(ratingSummary([]), { avg: null, count: 0 })
  assert.deepEqual(ratingSummary(null), { avg: null, count: 0 })
  const s = ratingSummary([{ stars: 5, userId: 1 }, { stars: 4, userId: 1 }, { stars: 4, userId: 2 }])
  assert.equal(s.count, 3)
  assert.equal(formatRating(s.avg), '4,3')
})

test('markUpdated markiert alle außer der ältesten Bewertung eines Nutzers', () => {
  // neueste zuerst
  const list = [
    { id: 5, userId: 1 },
    { id: 4, userId: 2 },
    { id: 3, userId: 1 },
    { id: 2, userId: 1 },
    { id: 1, userId: 3 },
  ]
  assert.deepEqual(markUpdated(list).map((r) => [r.id, r.updated]), [
    [5, true],
    [4, false],
    [3, true],
    [2, false],
    [1, false],
  ])
})

test('markUpdated: gelöschte Nutzer und Einzelbewertungen nie markiert', () => {
  const list = [{ id: 3, userId: null }, { id: 2, userId: null }, { id: 1, userId: 7 }]
  assert.deepEqual(markUpdated(list).map((r) => r.updated), [false, false, false])
  assert.deepEqual(markUpdated([{ id: 1, userId: 1 }]).map((r) => r.updated), [false])
  assert.deepEqual(markUpdated(null), [])
})

test('markUpdated verändert die Eingabe nicht', () => {
  const list = [{ id: 2, userId: 1 }, { id: 1, userId: 1 }]
  markUpdated(list)
  assert.equal('updated' in list[0], false)
})

test('formatRatingDate liefert TT.MM.JJJJ', () => {
  assert.equal(formatRatingDate('2026-09-27T12:16:00'), '27.09.2026')
  assert.equal(formatRatingDate('2026-01-05T08:00:00.123456'), '05.01.2026')
  assert.equal(formatRatingDate(null), '')
  assert.equal(formatRatingDate('kein Datum'), '')
})

test('createdLine nennt den Namen nur, wenn er bekannt ist', () => {
  assert.equal(createdLine('Anna Pastoors', '2026-09-27T12:16:00'), 'Angelegt von Anna Pastoors am 27.09.2026')
  assert.equal(createdLine('', '2026-09-27T12:16:00'), 'Angelegt am 27.09.2026')
  assert.equal(createdLine(null, '2026-09-27T12:16:00'), 'Angelegt am 27.09.2026')
  assert.equal(createdLine('Anna', null), '')
})

test('raterName zeigt für gelöschte Nutzer „Ehemaliges Mitglied“', () => {
  assert.equal(raterName({ userName: 'Paco' }), 'Paco')
  assert.equal(raterName({ userName: null }), 'Ehemaliges Mitglied')
  assert.equal(raterName({ userName: '  ' }), 'Ehemaliges Mitglied')
})

test('ratingsRoute führt zur Rezeptseite mit Anker auf den Abschnitt Bewertungen', () => {
  assert.deepEqual(ratingsRoute(42), { path: '/rezepte/42', hash: '#bewertungen' })
  assert.equal(RATINGS_ANCHOR, 'bewertungen')
})

test('wantsRatings erkennt nur den Bewertungs-Anker', () => {
  assert.equal(wantsRatings('#bewertungen'), true)
  assert.equal(wantsRatings(ratingsRoute(7).hash), true)
  assert.equal(wantsRatings(''), false)
  assert.equal(wantsRatings(undefined), false)
  assert.equal(wantsRatings('#zutaten'), false)
})
