import { test } from 'node:test'
import assert from 'node:assert/strict'
import { register } from 'node:module'

register('../testing/vueHooks.js', import.meta.url)

// Minimale document/window-Attrappe: nur die Listener, die das Popover zum Schließen braucht
function fakeTarget() {
  const handlers = {}
  return {
    handlers,
    addEventListener: (type, fn) => { (handlers[type] ??= new Set()).add(fn) },
    removeEventListener: (type, fn) => handlers[type]?.delete(fn),
    fire: (type, event = {}) => [...(handlers[type] ?? [])].forEach((fn) => fn(event)),
  }
}

// erst nach Vue setzen: runtime-dom greift beim Import nach document.createElement
const { computed, effectScope, nextTick, ref } = await import('vue')
globalThis.document = fakeTarget()
globalThis.window = fakeTarget()
const { useIngredientRefs } = await import('./useIngredientRefs.js')

const INGREDIENTS = [{ amount: '1', unit: 'TL', name: 'Salz' }, { amount: '', unit: '', name: 'Pfeffer' }]

function setup(enabled = true) {
  const scope = effectScope()
  const on = ref(enabled)
  const context = scope.run(() =>
    useIngredientRefs({ ingredients: ref(INGREDIENTS), language: ref('de'), enabled: computed(() => on.value) })
  )
  return { context, scope, on }
}

const later = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
const element = (insideRef) => ({ closest: (sel) => (insideRef && sel.includes('.ingredient-ref') ? {} : null) })

test('aus: Schritte bleiben ein einziges Textsegment', () => {
  const { context } = setup(false)
  assert.deepEqual(context.segmentsFor('Salz und Pfeffer'), [{ text: 'Salz und Pfeffer' }])
})

test('an: Segmente mit Verweisen, Zeilen in der Sprache der Anzeige', () => {
  const { context } = setup()
  assert.deepEqual(context.segmentsFor('Salz und Pfeffer'), [
    { text: 'Salz', refs: [0] }, { text: ' und ' }, { text: 'Pfeffer', refs: [1] },
  ])
  assert.equal(context.linesFor([1])[0].amount, 'nach Bedarf')
})

test('Esc schließt, Listener nur solange ein Popover offen ist', async () => {
  const { context } = setup()
  context.state.click('0:0')
  await nextTick()
  assert.equal(context.openKey.value, '0:0')
  assert.equal(document.handlers.keydown.size, 1)
  document.fire('keydown', { key: 'Enter' })
  assert.equal(context.openKey.value, '0:0')
  document.fire('keydown', { key: 'Escape' })
  await nextTick()
  assert.equal(context.openKey.value, null)
  assert.equal(document.handlers.keydown.size, 0)
})

test('Tap daneben schließt, Tap auf ein Zutatenwort nicht', async () => {
  const { context } = setup()
  context.state.click('0:0')
  await nextTick()
  document.fire('pointerdown', { target: element(true) })
  assert.equal(context.openKey.value, '0:0')
  document.fire('pointerdown', { target: element(false) })
  await nextTick()
  assert.equal(context.openKey.value, null)
})

test('Scrollen schließt (nicht im allerersten Moment nach dem Öffnen)', async () => {
  const { context } = setup()
  context.state.click('0:2')
  await nextTick()
  window.fire('scroll')
  assert.equal(context.openKey.value, '0:2')
  await later(320)
  window.fire('scroll')
  await nextTick()
  assert.equal(context.openKey.value, null)
})

test('Ausschalten und Abbau räumen auf', async () => {
  const { context, scope, on } = setup()
  context.state.click('0:0')
  await nextTick()
  on.value = false
  await nextTick()
  assert.equal(context.openKey.value, null)
  context.state.click('0:0')
  await nextTick()
  scope.stop()
  assert.equal(document.handlers.keydown.size, 0)
  assert.equal(window.handlers.scroll.size, 0)
})
