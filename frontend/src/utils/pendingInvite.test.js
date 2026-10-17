import { test } from 'node:test'
import assert from 'node:assert/strict'
import { rememberInvite, takeInvite, invitePath } from './pendingInvite.js'

const memoryStorage = () => {
  const data = new Map()
  return {
    getItem: (k) => (data.has(k) ? data.get(k) : null),
    setItem: (k, v) => data.set(k, String(v)),
    removeItem: (k) => data.delete(k),
  }
}

test('gemerkte Einladung kommt genau einmal zurück', () => {
  const s = memoryStorage()
  rememberInvite('abcdefghijklmnopqrstuvwx', s, 1000)
  assert.equal(takeInvite(s, 2000), 'abcdefghijklmnopqrstuvwx')
  assert.equal(takeInvite(s, 2000), null)
})

test('alte Einladungen und ungültige Codes werden verworfen', () => {
  const s = memoryStorage()
  rememberInvite('abcdefghijklmnopqrstuvwx', s, 0)
  assert.equal(takeInvite(s, 2 * 60 * 60 * 1000), null)
  rememberInvite('../../admin', s, 0)
  assert.equal(takeInvite(s, 1), null)
  s.setItem('familien:einladung', '{kaputt')
  assert.equal(takeInvite(s, 1), null)
})

test('Einladungspfad', () => {
  assert.equal(invitePath('abc_DEF-123'), '/einladung/abc_DEF-123')
})
