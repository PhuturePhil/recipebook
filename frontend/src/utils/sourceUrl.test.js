import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  MAX_SOURCE_URL,
  SOURCE_URL_INVALID,
  SOURCE_URL_TOO_LONG,
  cleanSourceUrl,
  completeSourceUrl,
  sourceDomain,
  sourceUrlProblem
} from './sourceUrl.js'

test('empty link is fine and gets trimmed', () => {
  assert.equal(sourceUrlProblem(''), '')
  assert.equal(sourceUrlProblem('   '), '')
  assert.equal(sourceUrlProblem(null), '')
  assert.equal(cleanSourceUrl('  https://zeit.de/x  '), 'https://zeit.de/x')
})

test('accepts http and https links with a domain', () => {
  for (const url of [
    'https://www.zeit.de/zeit-magazin/wochenmarkt/2025-11/kuerbis-linsen-dhansak-rezept-wochenmarkt',
    'http://example.com',
    'HTTPS://Example.com/a?b=c#d',
    'https://www.küchengötter.de/rezept',
    ' https://rainbowplantlife.com/wprm_print/8066 '
  ]) {
    assert.equal(sourceUrlProblem(url), '', url)
  }
})

test('rejects links that are no web address with an understandable message', () => {
  for (const url of ['zeit.de/rezept', 'ftp://example.com/x', 'javascript:alert(1)', 'https://', 'https://localhost/x',
    'https://zeit de/x', 'https://user@evil.com/x', 'Rezept aus der Zeit']) {
    assert.equal(sourceUrlProblem(url), SOURCE_URL_INVALID, url)
  }
  assert.match(SOURCE_URL_INVALID, /http:\/\/ oder https:\/\//)
})

test('rejects overlong links', () => {
  const prefix = 'https://www.zeit.de/'
  assert.equal(sourceUrlProblem(prefix + 'x'.repeat(MAX_SOURCE_URL - prefix.length)), '')
  assert.equal(sourceUrlProblem(prefix + 'x'.repeat(MAX_SOURCE_URL + 1 - prefix.length)), SOURCE_URL_TOO_LONG)
})

test('a domain typed without protocol gets https:// in front', () => {
  assert.equal(completeSourceUrl('www.zeit.de/rezept'), 'https://www.zeit.de/rezept')
  assert.equal(completeSourceUrl(' zeit.de '), 'https://zeit.de')
  assert.equal(completeSourceUrl('biancazapatka.com/de/curry/'), 'https://biancazapatka.com/de/curry/')
  assert.equal(completeSourceUrl('http://zeit.de'), 'http://zeit.de')
  assert.equal(completeSourceUrl('javascript:alert(1)'), 'javascript:alert(1)')
  assert.equal(completeSourceUrl('Rezept aus der Zeit'), 'Rezept aus der Zeit')
  assert.equal(completeSourceUrl(''), '')
})

test('shows the domain without www, port and path', () => {
  assert.equal(sourceDomain('https://www.zeit.de/zeit-magazin/wochenmarkt/x'), 'zeit.de')
  assert.equal(sourceDomain('https://biancazapatka.com/de/beluga-linsen-curry/'), 'biancazapatka.com')
  assert.equal(sourceDomain('HTTP://WWW.Example.COM:8080/?a=b'), 'example.com')
  assert.equal(sourceDomain('https://www.weightwatchers.com/de/rezept/x'), 'weightwatchers.com')
  assert.equal(sourceDomain('https://www.küchengötter.de/r'), 'küchengötter.de')
  assert.equal(sourceDomain('https://wwwtest.de/r'), 'wwwtest.de')
})

test('no domain for missing or invalid links', () => {
  assert.equal(sourceDomain(''), null)
  assert.equal(sourceDomain(null), null)
  assert.equal(sourceDomain('javascript:alert(1)'), null)
  assert.equal(sourceDomain('zeit.de'), null)
})
