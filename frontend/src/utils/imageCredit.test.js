import { test } from 'node:test'
import assert from 'node:assert/strict'
import { imageCreditLinks, withUtm } from './imageCredit.js'

test('unsplash credit links photographer and Unsplash with referral UTM parameters', () => {
  const links = imageCreditLinks({
    source: 'unsplash', name: 'Jane Doe', profileUrl: 'https://unsplash.com/@jane', photoUrl: 'https://unsplash.com/photos/x',
  })
  assert.deepEqual(links, {
    name: 'Jane Doe',
    nameUrl: 'https://unsplash.com/@jane?utm_source=pastoors_familienrezepte&utm_medium=referral',
    provider: 'Unsplash',
    providerUrl: 'https://unsplash.com/?utm_source=pastoors_familienrezepte&utm_medium=referral',
  })
})

test('pexels credit links without UTM parameters', () => {
  const links = imageCreditLinks({ source: 'pexels', name: 'Max Muster', profileUrl: 'https://www.pexels.com/@max' })
  assert.equal(links.nameUrl, 'https://www.pexels.com/@max')
  assert.equal(links.provider, 'Pexels')
  assert.equal(links.providerUrl, 'https://www.pexels.com/')
})

test('uploads, unknown sources and missing names get no credit line', () => {
  assert.equal(imageCreditLinks(null), null)
  assert.equal(imageCreditLinks({ source: 'upload' }), null)
  assert.equal(imageCreditLinks({ source: 'unsplash', name: null }), null)
  assert.equal(imageCreditLinks({ source: 'flickr', name: 'X' }), null)
})

test('non-http profile urls are not linked', () => {
  const links = imageCreditLinks({ source: 'unsplash', name: 'Jane', profileUrl: 'javascript:alert(1)' })
  assert.equal(links.nameUrl, null)
})

test('withUtm keeps existing query parameters', () => {
  assert.equal(
    withUtm('https://unsplash.com/@jane?ixid=1'),
    'https://unsplash.com/@jane?ixid=1&utm_source=pastoors_familienrezepte&utm_medium=referral',
  )
})
