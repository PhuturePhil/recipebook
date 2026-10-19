import { test } from 'node:test'
import assert from 'node:assert/strict'
import { register } from 'node:module'

register('../testing/vueHooks.js', import.meta.url)

const { createSSRApp, computed, effectScope, nextTick, ref, h } = await import('vue')
const { renderToString } = await import('vue/server-renderer')
const { default: InstructionText } = await import('./InstructionText.vue')
const { useIngredientRefs } = await import('../composables/useIngredientRefs.js')
const { ingredientRefsVisible } = await import('../utils/ingredientRefs.js')
const { scaleIngredients } = await import('../utils/scaleIngredients.js')

// Rezept 166 (Kartoffelgratin), Auszug
const GRATIN = [
  { amount: '1.2', unit: 'kg', name: 'Kartoffeln, vorwiegend festkochend' },
  { amount: '400', unit: 'ml', name: 'Schlagsahne (mind. 30 % Fett)' },
  { amount: '', unit: '', name: 'Pfeffer' },
  { amount: '', unit: '', name: 'Butter für die Form' },
]
const STEP = 'Auflaufform buttern. Sahne und Pfeffer mit den Kartoffelscheiben mischen.'

function setup({ user = { role: 'ADMIN' }, servings = 4 } = {}) {
  const scope = effectScope()
  const current = ref(servings)
  const auth = ref(user)
  const context = scope.run(() =>
    useIngredientRefs({
      ingredients: computed(() => scaleIngredients(GRATIN, 4, current.value)),
      language: ref('de'),
      enabled: computed(() => ingredientRefsVisible(auth.value)),
    })
  )
  const render = async () => {
    const ctx = {}
    const app = createSSRApp({ render: () => h('li', h(InstructionText, { text: STEP, step: 2, context })) })
    const html = await renderToString(app, ctx)
    return { html, popover: ctx.teleports?.body ?? '' }
  }
  const keyOf = (word) => `2:${context.segmentsFor(STEP).findIndex((s) => s.text === word)}`
  return { context, render, keyOf, current, auth, scope }
}

const strip = (html) => html.replace(/<!--.*?-->/g, '')

test('Admin: Zutaten sind Buttons mit aria-label, „buttern“ bleibt Text', async () => {
  const { html } = await setup().render()
  const buttons = [...strip(html).matchAll(/<button[^>]*class="ingredient-ref"[^>]*>([^<]*)<\/button>/g)]
  assert.deepEqual(buttons.map((m) => m[1]), ['Sahne', 'Pfeffer', 'Kartoffelscheiben'])
  assert.match(html, /aria-label="Menge anzeigen: Sahne"/)
  assert.match(html, /type="button"/)
  assert.match(strip(html), /Auflaufform buttern\. /)
  assert.doesNotMatch(html, /role="tooltip"/)
})

test('Nicht-Admin: keine Unterstreichung, Schritt als reiner Text', async () => {
  for (const user of [{ role: 'USER' }, null]) {
    const { html, popover } = await setup({ user }).render()
    assert.doesNotMatch(html, /<button|ingredient-ref/)
    assert.equal(strip(html), `<li>${STEP}</li>`)
    assert.doesNotMatch(popover, /tooltip/)
  }
})

test('Popover: öffnet mit skalierter Menge (6 statt 4 Personen), schließt wieder', async () => {
  const s = setup({ servings: 6 })
  s.context.state.click(s.keyOf('Kartoffelscheiben'))
  const open = await s.render()
  assert.match(open.popover, /role="tooltip"/)
  assert.match(open.popover, /id="ingredient-popover-2"/)
  assert.match(open.popover, /1,8 kg/)
  assert.match(open.popover, /Kartoffeln, vorwiegend festkochend/)
  assert.match(open.html, /aria-expanded="true"[^>]*aria-describedby="ingredient-popover-2"|aria-describedby="ingredient-popover-2"/)
  s.context.state.click(s.keyOf('Kartoffelscheiben'))
  const closed = await s.render()
  assert.doesNotMatch(closed.popover, /role="tooltip"/)
})

test('Popover: Menge folgt dem Personen-Regler', async () => {
  const s = setup()
  s.context.state.click(s.keyOf('Sahne'))
  assert.match((await s.render()).popover, /400 ml/)
  s.current.value = 2
  await nextTick()
  s.context.state.click(s.keyOf('Sahne'))
  assert.match((await s.render()).popover, /200 ml/)
})

test('Popover: Zutat ohne Menge zeigt „nach Bedarf“', async () => {
  const s = setup()
  s.context.state.click(s.keyOf('Pfeffer'))
  const { popover } = await s.render()
  assert.match(popover, /nach Bedarf/)
  assert.match(popover, /ingredient-popover__amount--none/)
  assert.match(popover, />Pfeffer</)
})

test('nur ein Popover gleichzeitig', async () => {
  const s = setup()
  s.context.state.click(s.keyOf('Sahne'))
  s.context.state.click(s.keyOf('Pfeffer'))
  const { popover } = await s.render()
  assert.equal(popover.match(/role="tooltip"/g).length, 1)
  assert.match(popover, /nach Bedarf/)
  assert.doesNotMatch(popover, /400 ml/)
})
