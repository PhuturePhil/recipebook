import { test } from 'node:test'
import assert from 'node:assert/strict'
import { bottomBarLayout, BOTTOM_BAR_MAX } from './bottomBar.js'

const it = (id, extra = {}) => ({ id, label: id, icon: 'plus', ...extra })
const ids = (list) => list.map((i) => i.id)

test('bottomBarLayout: höchstens 4 nebeneinander', () => {
  assert.equal(BOTTOM_BAR_MAX, 4)
  const { bar, menu } = bottomBarLayout([it('a'), it('b'), it('c'), it('d')])
  assert.deepEqual(ids(bar), ['a', 'b', 'c', 'd'])
  assert.deepEqual(menu, [])
})

test('bottomBarLayout: ab 5 Einträgen bleiben 3 + „Mehr“, der Rest geht ins Menü', () => {
  const { bar, menu } = bottomBarLayout([it('a'), it('b'), it('c'), it('d'), it('e')])
  assert.deepEqual(ids(bar), ['a', 'b', 'c'])
  assert.deepEqual(ids(menu), ['d', 'e'])
})

test('bottomBarLayout: Menü-Einträge zählen als ein Platz für „Mehr“', () => {
  const { bar, menu } = bottomBarLayout([it('a'), it('b'), it('c'), it('d')], [it('m')])
  assert.deepEqual(ids(bar), ['a', 'b', 'c'])
  assert.deepEqual(ids(menu), ['d', 'm'])
  const kurz = bottomBarLayout([it('a'), it('b')], [it('m')])
  assert.deepEqual(ids(kurz.bar), ['a', 'b'])
  assert.deepEqual(ids(kurz.menu), ['m'])
})

test('bottomBarLayout: Destruktives nie als direkter Knopf, im Menü ganz unten', () => {
  const { bar, menu } = bottomBarLayout([it('x', { danger: true }), it('a'), it('b')], [it('m')])
  assert.deepEqual(ids(bar), ['a', 'b'])
  assert.deepEqual(ids(menu), ['m', 'x'])
  const ueberlauf = bottomBarLayout([it('a'), it('b'), it('c'), it('d')], [it('x', { danger: true })])
  assert.deepEqual(ids(ueberlauf.menu), ['d', 'x'])
})

test('bottomBarLayout: leer bleibt leer', () => {
  assert.deepEqual(bottomBarLayout(), { bar: [], menu: [] })
})
