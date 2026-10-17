import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { CONTACT_EMAIL, reportMailto, signupError } from './legal.js'

const router = readFileSync(new URL('../router/index.js', import.meta.url), 'utf8')

function parseMailto(href) {
  const url = new URL(href)
  return { to: url.pathname, subject: url.searchParams.get('subject'), body: url.searchParams.get('body') }
}

test('Melde-Link geht an das Kontaktpostfach und nennt Rezept-ID, Titel und Adresse', () => {
  const mail = parseMailto(reportMailto({ recipeId: 42, title: 'Linsen & Reis', url: 'https://pastoors.cloud/recipe/42' }))
  assert.equal(mail.to, CONTACT_EMAIL)
  assert.equal(mail.subject, 'Meldung: Rezept 42 „Linsen & Reis“')
  assert.match(mail.body, /https:\/\/pastoors\.cloud\/recipe\/42/)
  assert.match(mail.body, /Rezept-ID: 42/)
  assert.match(mail.body, /Grund/)
})

test('Melde-Link für Teilen-Seiten kommt ohne Rezept-ID aus (die Seite verrät sie nicht)', () => {
  const mail = parseMailto(reportMailto({ title: 'Dhal', url: 'https://pastoors.cloud/share/abc' }))
  assert.equal(mail.subject, 'Meldung: geteiltes Rezept „Dhal“')
  assert.match(mail.body, /share\/abc/)
  assert.doesNotMatch(mail.body, /Rezept-ID/)
})

test('Melde-Link ist korrekt kodiert (keine rohen Leer- oder Sonderzeichen)', () => {
  const href = reportMailto({ recipeId: 1, title: 'A & B?', url: 'https://x/y?z=1' })
  assert.doesNotMatch(href.split('?').slice(1).join('?'), /[ \n]/)
  assert.equal(parseMailto(href).subject, 'Meldung: Rezept 1 „A & B?“')
})

test('Registrierungsformular prüft wie das Backend', () => {
  const ok = { vorname: 'Lena', nachname: 'Muster', email: 'lena+rezepte@test.de', password: 'geheim123', passwordConfirm: 'geheim123' }
  assert.equal(signupError(ok), null)
  assert.equal(signupError({ ...ok, vorname: ' ' }), 'Bitte gib deinen Vornamen an.')
  assert.equal(signupError({ ...ok, nachname: '' }), 'Bitte gib deinen Nachnamen an.')
  assert.equal(signupError({ ...ok, email: 'lena@' }), 'Bitte gib eine gültige E-Mail-Adresse an.')
  assert.equal(signupError({ ...ok, password: 'kurz', passwordConfirm: 'kurz' }), 'Das Passwort muss mindestens 8 Zeichen lang sein.')
  assert.equal(signupError({ ...ok, passwordConfirm: 'anders123' }), 'Die Passwörter stimmen nicht überein.')
})

function routeBlock(path) {
  const start = router.indexOf(`path: '${path}'`)
  assert.ok(start > 0, `Route ${path} fehlt`)
  return router.slice(start, router.indexOf('}', router.indexOf('meta', start)) + 1)
}

test('Registrierung, Bestätigung, Impressum und Datenschutz sind ohne Anmeldung erreichbar', () => {
  for (const path of ['/registrieren', '/konto-bestaetigen', '/impressum', '/datenschutz']) {
    assert.match(routeBlock(path), /requiresAuth: false/, path)
  }
})
