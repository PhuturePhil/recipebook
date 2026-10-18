import { test } from 'node:test'
import assert from 'node:assert/strict'
import { sameEmail, emailChanged, needsCurrentPassword, profilePayload, pendingEmailText } from './profileChange.js'

const lokal = { email: 'anna@test.de', ssoLinked: false, ssoOnly: false }
const sso = { email: 'anna@test.de', ssoLinked: true, ssoOnly: true }

test('Schreibweise der E-Mail zählt nicht als Änderung', () => {
  assert.ok(sameEmail('Anna@Test.de ', 'anna@test.de'))
  assert.equal(emailChanged(lokal, 'ANNA@test.de'), false)
  assert.equal(emailChanged(lokal, ''), false)
  assert.equal(emailChanged(lokal, 'neu@test.de'), true)
})

test('Nur Namen ändern braucht kein aktuelles Passwort', () => {
  const form = { vorname: 'Anna', nachname: 'P.', email: 'anna@test.de', password: '', currentPassword: '' }
  assert.equal(needsCurrentPassword(lokal, form), false)
  assert.deepEqual(profilePayload(lokal, form), { vorname: 'Anna', nachname: 'P.' })
})

test('Neue Adresse oder neues Passwort schicken das aktuelle Passwort mit', () => {
  const mail = { vorname: 'Anna', nachname: '', email: ' neu@test.de ', password: '', currentPassword: 'alt12345' }
  assert.equal(needsCurrentPassword(lokal, mail), true)
  assert.deepEqual(profilePayload(lokal, mail),
    { vorname: 'Anna', nachname: undefined, email: 'neu@test.de', currentPassword: 'alt12345' })

  const pw = { vorname: '', nachname: '', email: 'anna@test.de', password: 'neu12345', currentPassword: '' }
  assert.equal(needsCurrentPassword(lokal, pw), true)
  assert.equal(profilePayload(lokal, pw).password, 'neu12345')
  assert.equal(profilePayload(lokal, pw).currentPassword, '')
})

test('pastoors.cloud-Konten schicken weder Adresse noch Passwort', () => {
  const form = { vorname: 'Anna', nachname: '', email: 'neu@test.de', password: 'neu12345', currentPassword: '' }
  assert.equal(needsCurrentPassword(sso, form), false)
  assert.deepEqual(profilePayload(sso, form), { vorname: 'Anna', nachname: undefined })
})

test('Hinweis zur wartenden Adresse nennt die neue Adresse', () => {
  assert.match(pendingEmailText('neu@test.de'), /an neu@test\.de geschickt/)
})
