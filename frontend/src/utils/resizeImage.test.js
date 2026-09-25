import { test } from 'node:test'
import assert from 'node:assert/strict'
import { fitWithin } from './resizeImage.js'

test('shrinks the longer edge to the limit and keeps the aspect ratio', () => {
  assert.deepEqual(fitWithin(3024, 4032), { width: 1200, height: 1600 })
  assert.deepEqual(fitWithin(4032, 3024), { width: 1600, height: 1200 })
})

test('never upscales small images', () => {
  assert.deepEqual(fitWithin(800, 600), { width: 800, height: 600 })
  assert.deepEqual(fitWithin(1600, 900), { width: 1600, height: 900 })
})
