import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

// changelog.json ist die Versionsquelle des Update-Checks: oberster Eintrag = aktuelle Version
const changelog = JSON.parse(readFileSync(new URL('./changelog.json', import.meta.url), 'utf8'))

test('jeder Eintrag hat version, datum, titel und mindestens einen Punkt', () => {
  assert.ok(Array.isArray(changelog) && changelog.length > 0)
  for (const e of changelog) {
    assert.match(e.version, /^\d{4}-\d{2}-\d{2}\.\d+$/, `version ${e.version}`)
    assert.match(e.datum, /^\d{2}\.\d{2}\.\d{4}$/, `datum bei ${e.version}`)
    assert.ok(typeof e.titel === 'string' && e.titel.trim(), `titel bei ${e.version}`)
    assert.ok(Array.isArray(e.punkte) && e.punkte.length > 0, `punkte bei ${e.version}`)
  }
})

test('Versionen sind eindeutig — jedes Release braucht eine neue', () => {
  const versions = changelog.map((e) => e.version)
  assert.equal(new Set(versions).size, versions.length)
})

test('Version passt zum Datum des Eintrags', () => {
  for (const e of changelog) {
    const [d, m, y] = e.datum.split('.')
    assert.ok(e.version.startsWith(`${y}-${m}-${d}.`), `${e.version} vs. ${e.datum}`)
  }
})
