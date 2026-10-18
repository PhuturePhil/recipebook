import { test } from 'node:test'
import assert from 'node:assert/strict'
import { splitDataUrl, acceptScanFiles, scanPayloadBytes, scanTooLarge, scanErrorMessage, MAX_SCAN_BYTES } from './scanImages.js'

test('Data-URL wird in Typ und Base64 zerlegt', () => {
  assert.deepEqual(splitDataUrl('data:image/jpeg;base64,/9j/AAAA'), { mimeType: 'image/jpeg', imageData: '/9j/AAAA' })
  assert.equal(splitDataUrl('https://example.org/a.jpg'), null)
  assert.equal(splitDataUrl(undefined), null)
})

test('Höchstens 5 Fotos pro Scan', () => {
  const files = ['a', 'b', 'c']
  assert.deepEqual(acceptScanFiles(0, files), { accepted: files, notice: '' })
  const tooMany = acceptScanFiles(3, files)
  assert.deepEqual(tooMany.accepted, ['a', 'b'])
  assert.equal(tooMany.notice, 'Höchstens 5 Fotos pro Scan – ein Foto wurde nicht übernommen.')
  const full = acceptScanFiles(5, files)
  assert.deepEqual(full.accepted, [])
  assert.match(full.notice, /3 Fotos wurden nicht übernommen/)
})

test('8-MB-Grenze wird vor dem Senden erkannt', () => {
  const small = [{ imageData: 'x'.repeat(1000), mimeType: 'image/jpeg' }]
  assert.ok(scanPayloadBytes(small) > 1000)
  assert.equal(scanTooLarge(small), false)
  assert.equal(scanTooLarge([{ imageData: 'x'.repeat(MAX_SCAN_BYTES), mimeType: 'image/jpeg' }]), true)
})

test('Server-Meldungen zu Kontingent, Größe und Foto werden gezeigt', () => {
  const quota = Object.assign(new Error('Du hast heute schon 10 Rezepte per Foto erfasst – morgen geht es weiter.'), { status: 429 })
  assert.equal(scanErrorMessage(quota), quota.message)
  const big = Object.assign(new Error('Die Fotos sind zusammen zu groß.'), { status: 413 })
  assert.equal(scanErrorMessage(big), big.message)
  assert.match(scanErrorMessage(Object.assign(new Error('HTTP error! status: 400'), { status: 400 })), /konnte nicht analysiert/)
  assert.match(scanErrorMessage(Object.assign(new Error('boom'), { status: 500 })), /konnte nicht analysiert/)
  assert.match(scanErrorMessage(Object.assign(new Error('HTTP error! status: 413'), { status: 413 })), /höchstens 8 MB/)
  assert.match(scanErrorMessage(new TypeError('Failed to fetch')), /konnte nicht analysiert/)
})
