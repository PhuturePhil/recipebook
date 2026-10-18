import { test } from 'node:test'
import assert from 'node:assert/strict'
import { aiStatusLabel, canRetry, awaitsReview, sortAiRequests, reviewSummary } from './aiRequests.js'

test('Status heißen in Alltagssprache', () => {
  assert.equal(aiStatusLabel('REVIEW'), 'wartet auf Freigabe')
  assert.equal(aiStatusLabel('REJECTED'), 'abgelehnt')
  assert.equal(aiStatusLabel('DONE'), 'erledigt')
  assert.equal(aiStatusLabel('UNBEKANNT'), 'UNBEKANNT')
})

test('Erneut versuchen nur bei fehlgeschlagenen oder abgelehnten Anfragen', () => {
  assert.ok(canRetry({ status: 'FAILED' }))
  assert.ok(canRetry({ status: 'REJECTED' }))
  assert.equal(canRetry({ status: 'REVIEW' }), false)
  assert.equal(canRetry({ status: 'DONE' }), false)
  assert.ok(awaitsReview({ status: 'REVIEW' }))
})

test('Zur Freigabe wartende Vorschläge stehen oben', () => {
  const list = [{ id: 1, status: 'DONE' }, { id: 2, status: 'REVIEW' }, { id: 3, status: 'FAILED' }, { id: 4, status: 'REVIEW' }]
  assert.deepEqual(sortAiRequests(list).map((r) => r.id), [2, 4, 1, 3])
  assert.deepEqual(list.map((r) => r.id), [1, 2, 3, 4], 'Original bleibt unverändert')
})

test('Hinweis zählt die wartenden Vorschläge', () => {
  assert.equal(reviewSummary([{ status: 'DONE' }]), '')
  assert.equal(reviewSummary([{ status: 'REVIEW' }]), '1 KI-Vorschlag wartet auf deine Freigabe.')
  assert.equal(reviewSummary([{ status: 'REVIEW' }, { status: 'REVIEW' }]), '2 KI-Vorschläge warten auf deine Freigabe.')
})
