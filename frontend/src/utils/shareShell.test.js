import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { APP_TITLE_TAG, shareShellHtml } from './shareShell.js'

const indexHtml = readFileSync(new URL('../../index.html', import.meta.url), 'utf8')

test('index.html enthält den Titel, den die Teilen-Hülle ersetzt', () => {
  assert.equal(indexHtml.split(APP_TITLE_TAG).length, 2)
})

test('Teilen-Hülle bindet die Kopfzeilen per SSI ein und hat einen Ersatz-Titel', () => {
  const shell = shareShellHtml(indexHtml)
  assert.ok(!shell.includes(`${APP_TITLE_TAG}\n`))
  assert.match(shell, /<!--# include virtual="\/share-head\/\$share_token" stub="share_head_fallback" -->/)
  assert.match(shell, /<!--# block name="share_head_fallback" --><title>Pastoors Familienrezepte<\/title>/)
  assert.match(shell, /<meta name="robots" content="noindex, nofollow" \/><!--# endblock -->/)
})

test('Teilen-Hülle lässt den Rest von index.html unverändert', () => {
  const shell = shareShellHtml(indexHtml)
  const [before, after] = indexHtml.split(APP_TITLE_TAG)
  assert.ok(shell.startsWith(before))
  assert.ok(shell.endsWith(after))
})

test('Teilen-Hülle bricht den Build ab, wenn der Titel fehlt', () => {
  assert.throws(() => shareShellHtml('<html><head></head></html>'), /fehlt/)
})
