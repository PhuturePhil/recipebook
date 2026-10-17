import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  deviceType,
  displayMode,
  recipeOrigin,
  languageDirection,
  createVisitTimer,
  createDebouncer,
  createChangeAggregator,
  createSearchReporter,
  createAnalytics,
  jsonStore,
  BATCH_SIZE,
  FLUSH_AT,
  MAX_QUEUE,
  RECIPE_TOTAL_CAP_MS,
  PAGE_VISIBLE_CAP_MS,
  SESSION_IDLE_MS,
} from './analytics.js'

const MIN = 60 * 1000
const HOUR = 60 * MIN

// Handgesteuerte Timer: run() lässt fällige Timer laufen
const fakeTimers = () => {
  let t = 0
  let nextId = 1
  const timers = new Map()
  return {
    setTimer: (fn, ms) => { const id = nextId++; timers.set(id, { fn, at: t + ms }); return id },
    clearTimer: (id) => timers.delete(id),
    advance(ms) {
      t += ms
      for (const [id, timer] of [...timers]) {
        if (timer.at <= t) { timers.delete(id); timer.fn() }
      }
    },
    get pending() { return timers.size },
  }
}

const memoryStorage = () => {
  const map = new Map()
  return {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k),
    map,
  }
}

const setup = ({ status = 204, enabled = true, user = '7', local = memoryStorage() } = {}) => {
  let clock = 1_000_000
  let ids = 0
  const sent = []
  const state = { status, enabled, user, visible: true }
  const a = createAnalytics({
    local: jsonStore(local),
    session: jsonStore(memoryStorage()),
    now: () => clock,
    newId: () => `id${++ids}`,
    isEnabled: () => state.enabled,
    owner: () => state.user,
    context: () => ({ device: 'mobile', display: 'pwa' }),
    isVisible: () => state.visible,
    send: async (events) => {
      if (state.status instanceof Error) throw state.status
      sent.push(events)
      return state.status
    },
  })
  return {
    a,
    sent,
    state,
    local,
    tick: (ms) => { clock += ms },
    get now() { return clock },
    queue: () => JSON.parse(local.getItem('an:queue') ?? '[]'),
    flat: () => sent.flat(),
  }
}

test('deviceType: UA zuerst, sonst Viewport unter 1024 px = mobil', () => {
  const iphone = 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148'
  const mac = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15'
  assert.equal(deviceType({ ua: iphone, width: 1400 }), 'mobile')
  assert.equal(deviceType({ ua: 'Mozilla/5.0 (Linux; Android 15) Mobile', width: 412 }), 'mobile')
  assert.equal(deviceType({ ua: mac, width: 1024, touchPoints: 5 }), 'mobile')
  assert.equal(deviceType({ ua: mac, width: 1440, touchPoints: 0 }), 'desktop')
  assert.equal(deviceType({ ua: mac, width: 800 }), 'mobile')
  assert.equal(deviceType({ ua: mac, width: 1023 }), 'mobile')
  assert.equal(deviceType({ ua: mac, width: 1024 }), 'desktop')
  assert.equal(deviceType(), 'desktop')
})

test('displayMode: standalone = PWA', () => {
  assert.equal(displayMode({ standalone: true }), 'pwa')
  assert.equal(displayMode({ iosStandalone: true }), 'pwa')
  assert.equal(displayMode({}), 'browser')
})

test('recipeOrigin: Liste, Suche, Teilen-Link, direkt', () => {
  assert.equal(recipeOrigin('', false), 'direkt')
  assert.equal(recipeOrigin(undefined), 'direkt')
  assert.equal(recipeOrigin('home', false), 'liste')
  assert.equal(recipeOrigin('home', true), 'suche')
  assert.equal(recipeOrigin('shared-recipe'), 'teilen_link')
  assert.equal(recipeOrigin('recipe-edit'), 'formular')
  assert.equal(recipeOrigin('recipe-new'), 'formular')
  assert.equal(recipeOrigin('changelog'), 'sonstige')
})

test('languageDirection', () => {
  assert.equal(languageDirection('de'), 'en→de')
  assert.equal(languageDirection('original'), 'de→en')
})

test('VisitTimer: sichtbare Zeit pausiert im Hintergrund, Gesamtspanne läuft weiter', () => {
  const timer = createVisitTimer({ start: 0 })
  timer.setVisible(false, 10 * MIN)
  timer.setVisible(false, 15 * MIN)   // doppeltes Verdecken ändert nichts
  timer.setVisible(true, 40 * MIN)
  timer.setVisible(true, 41 * MIN)    // doppeltes Aufdecken auch nicht
  assert.deepEqual(timer.snapshot(50 * MIN), { sichtbar_ms: 20 * MIN, gesamt_ms: 50 * MIN })
})

test('VisitTimer: im Hintergrund gestartet zählt erst ab Sichtbarkeit', () => {
  const timer = createVisitTimer({ start: 0, visible: false })
  assert.equal(timer.snapshot(5 * MIN).sichtbar_ms, 0)
  timer.setVisible(true, 5 * MIN)
  assert.deepEqual(timer.snapshot(8 * MIN), { sichtbar_ms: 3 * MIN, gesamt_ms: 8 * MIN })
})

test('VisitTimer: Rezept – Gesamtspanne bei 6 h gekappt, sichtbare Zeit ungekappt', () => {
  const timer = createVisitTimer({ start: 0, capTotalMs: RECIPE_TOTAL_CAP_MS })
  const snap = timer.snapshot(9 * HOUR)
  assert.equal(snap.gesamt_ms, 6 * HOUR)
  assert.equal(snap.sichtbar_ms, 9 * HOUR)
})

test('VisitTimer: Seite – sichtbare Zeit bei 30 Min. gekappt', () => {
  const timer = createVisitTimer({ start: 0, capVisibleMs: PAGE_VISIBLE_CAP_MS })
  assert.equal(timer.snapshot(2 * HOUR).sichtbar_ms, 30 * MIN)
  assert.equal(timer.snapshot(10 * MIN).sichtbar_ms, 10 * MIN)
})

test('VisitTimer: Uhr rückwärts ergibt nie negative Werte', () => {
  const timer = createVisitTimer({ start: 1000 })
  assert.deepEqual(timer.snapshot(500), { sichtbar_ms: 0, gesamt_ms: 0 })
})

test('Debouncer: nur der letzte Wert nach der Pause, flush sofort', () => {
  const timers = fakeTimers()
  const calls = []
  const d = createDebouncer(1000, (v) => calls.push(v), timers)
  d.push('a'); timers.advance(500)
  d.push('ab'); timers.advance(999)
  assert.deepEqual(calls, [])
  timers.advance(1)
  assert.deepEqual(calls, ['ab'])
  d.push('x'); d.flush()
  assert.deepEqual(calls, ['ab', 'x'])
  d.flush()
  assert.deepEqual(calls, ['ab', 'x'])   // nichts offen → kein weiterer Aufruf
  d.push('y'); d.cancel(); timers.advance(2000)
  assert.deepEqual(calls, ['ab', 'x'])
})

test('SearchReporter: nicht je Buchstabe, keine Doppelten, leere Suche setzt zurück', () => {
  const timers = fakeTimers()
  const reported = []
  const r = createSearchReporter(1200, (q) => reported.push(q), timers)
  for (const q of ['l', 'li', 'lin', 'lins', 'linse']) { r.push(q); timers.advance(200) }
  assert.deepEqual(reported, [])
  timers.advance(1200)
  assert.deepEqual(reported, ['linse'])
  r.push('linse '); timers.advance(1200)
  assert.deepEqual(reported, ['linse'])          // gleiche Suche nicht nochmal
  r.push(''); timers.advance(1200)
  r.push('linse'); r.flush()                      // nach dem Leeren zählt sie wieder; Enter = sofort
  assert.deepEqual(reported, ['linse', 'linse'])
  r.push('   '); r.flush()
  assert.deepEqual(reported, ['linse', 'linse'])
})

test('ChangeAggregator: mehrere Klicks = ein Event von → auf; zurück auf Start = keins', () => {
  const timers = fakeTimers()
  const events = []
  const agg = createChangeAggregator(1500, (from, to) => events.push([from, to]), timers)
  agg.change(4, 5); agg.change(5, 6); agg.change(6, 7)
  timers.advance(1500)
  assert.deepEqual(events, [[4, 7]])
  agg.change(7, 6); agg.change(6, 7); timers.advance(1500)
  assert.deepEqual(events, [[4, 7]])
  agg.change(7, 8); agg.flush()
  assert.deepEqual(events, [[4, 7], [7, 8]])
})

test('track: Event mit Kontext, Sitzung und Besitzer in die Warteschlange; flush schickt ohne Besitzer', async () => {
  const s = setup()
  s.a.track('suche', { query: 'linsen', treffer: 3 })
  const [queued] = s.queue()
  assert.equal(queued.event, 'suche')
  assert.equal(queued.device, 'mobile')
  assert.equal(queued.display, 'pwa')
  assert.equal(queued.owner, '7')
  assert.equal(queued.ts, s.now)
  assert.ok(queued.sessionId)
  await s.a.flush()
  assert.equal(s.sent.length, 1)
  assert.equal(s.sent[0][0].owner, undefined)
  assert.deepEqual(s.sent[0][0].props, { query: 'linsen', treffer: 3 })
  assert.deepEqual(s.queue(), [])
})

test('track: abgeschaltet (nicht eingeloggt) wird nichts gesammelt', async () => {
  const s = setup({ enabled: false })
  s.a.track('suche', { query: 'x' })
  assert.deepEqual(s.queue(), [])
  await s.a.flush()
  assert.equal(s.sent.length, 0)
})

test('flush: Fehler und 5xx behalten die Events, 4xx verwirft sie', async () => {
  const s = setup({ status: new Error('offline') })
  s.a.track('a')
  await s.a.flush()
  assert.equal(s.queue().length, 1)
  s.state.status = 503
  await s.a.flush()
  assert.equal(s.queue().length, 1)
  s.state.status = 429
  await s.a.flush()
  assert.equal(s.queue().length, 1)
  s.state.status = 400
  await s.a.flush()
  assert.equal(s.queue().length, 0)
})

test('flush: ab FLUSH_AT Events automatisch, Pakete höchstens BATCH_SIZE groß', async () => {
  const s = setup()
  for (let i = 0; i < FLUSH_AT; i++) s.a.track('e', { i })
  await new Promise((r) => setImmediate(r))
  assert.equal(s.sent.length, 1)
  assert.equal(s.sent[0].length, FLUSH_AT)

  const big = setup({ status: new Error('offline') })
  for (let i = 0; i < 120; i++) big.a.track('e', { i })
  await new Promise((r) => setImmediate(r))   // laufender Fehlversuch legt sein Paket zurück
  big.state.status = 204
  await big.a.flush({ all: true })
  assert.ok(big.sent.every((b) => b.length <= BATCH_SIZE))
  assert.equal(big.flat().length, 120)
})

test('Warteschlange bleibt begrenzt', () => {
  const s = setup({ status: new Error('offline'), enabled: true })
  s.state.enabled = true
  for (let i = 0; i < MAX_QUEUE + 30; i++) s.a.track('e', { i })
  assert.ok(s.queue().length <= MAX_QUEUE)
})

test('flush: Events eines anderen Kontos werden verworfen', async () => {
  const s = setup({ status: new Error('offline') })
  s.a.track('von_7')
  s.state.user = '8'
  s.state.status = 204
  s.a.track('von_8')
  await s.a.flush()
  assert.deepEqual(s.flat().map((e) => e.event), ['von_8'])
  assert.deepEqual(s.queue(), [])
})

test('Sitzung: bleibt im Vordergrund, neu nach mehr als 30 Min. im Hintergrund', async () => {
  const s = setup()
  s.a.track('a')
  s.tick(2 * HOUR)
  s.a.track('b')                      // lange sichtbar ohne Events: gleiche Sitzung
  s.a.onVisibility(false)
  s.tick(10 * MIN)
  s.a.onVisibility(true)
  s.a.track('c')                      // kurz weg: gleiche Sitzung
  s.a.onVisibility(false)
  s.tick(SESSION_IDLE_MS + 1)
  s.a.onVisibility(true)
  s.a.track('d')
  await s.a.flush({ all: true })
  const [a, b, c, d] = ['a', 'b', 'c', 'd'].map((n) => s.flat().find((e) => e.event === n))
  assert.equal(a.sessionId, b.sessionId)
  assert.equal(a.sessionId, c.sessionId)
  assert.notEqual(a.sessionId, d.sessionId)
})

test('Besuch: Ende meldet beide Dauern mit Startsitzung; Display aus pausiert nur die sichtbare Zeit', async () => {
  const s = setup()
  const visit = s.a.startVisit('rezept_verlassen', { recipeId: 5, besuch: 'b1' }, { capTotalMs: RECIPE_TOTAL_CAP_MS })
  s.tick(5 * MIN)
  s.a.onVisibility(false)
  s.tick(20 * MIN)
  s.a.onVisibility(true)
  s.tick(3 * MIN)
  visit.end()
  visit.end()   // doppelt beenden zählt nicht doppelt
  await s.a.flush({ all: true })
  const ends = s.flat().filter((e) => e.event === 'rezept_verlassen')
  assert.equal(ends.length, 1)
  assert.deepEqual(ends[0].props, { recipeId: 5, besuch: 'b1', sichtbar_ms: 8 * MIN, gesamt_ms: 28 * MIN })
  assert.equal(s.a.activeVisits, 0)
})

test('Besuch: fields wählt die Uhren (Seiten nur sichtbare Zeit, gekappt)', async () => {
  const s = setup()
  const visit = s.a.startVisit('seite_geoeffnet', { seite: 'zutaten' }, { capVisibleMs: PAGE_VISIBLE_CAP_MS, fields: ['sichtbar_ms'] })
  s.tick(45 * MIN)
  visit.end()
  await s.a.flush()
  assert.deepEqual(s.flat()[0].props, { seite: 'zutaten', sichtbar_ms: 30 * MIN })
})

test('Besuch: Ende-Felder überschreiben (z. B. aktion beim Popup)', async () => {
  const s = setup()
  const visit = s.a.startVisit('popup_neuerungen', { aktion: 'abgebrochen' }, { fields: ['sichtbar_ms'] })
  s.tick(4000)
  visit.end({ aktion: 'weggeklickt' })
  await s.a.flush()
  assert.deepEqual(s.flat()[0].props, { aktion: 'weggeklickt', sichtbar_ms: 4000 })
})

test('Seite schließen: offene Besuche enden als app_geschlossen und gehen raus', async () => {
  const s = setup()
  s.a.startVisit('rezept_verlassen', { recipeId: 1 })
  s.tick(MIN)
  s.a.onPageHide()
  await new Promise((r) => setImmediate(r))
  const [end] = s.flat()
  assert.equal(end.props.abschluss, 'app_geschlossen')
  assert.equal(end.props.gesamt_ms, MIN)
  assert.equal(s.local.getItem('an:visits'), null)
})

test('Im Hintergrund beendet: nächster Start meldet den Besuch mit dem Stand beim Verdecken', async () => {
  const local = memoryStorage()
  const first = setup({ local, status: new Error('offline') })
  first.a.startVisit('rezept_verlassen', { recipeId: 3 })
  first.tick(7 * MIN)
  first.a.onVisibility(false)          // danach beendet das System die App ohne pagehide
  const hiddenAt = first.now

  const second = setup({ local })
  second.a.recoverVisits()
  await second.a.flush({ all: true })
  const end = second.flat().find((e) => e.event === 'rezept_verlassen')
  assert.equal(end.props.abschluss, 'abgebrochen')
  assert.equal(end.props.sichtbar_ms, 7 * MIN)
  assert.equal(end.props.gesamt_ms, 7 * MIN)
  assert.equal(end.ts, hiddenAt)
  assert.equal(local.getItem('an:visits'), null)
})

test('Wieder sichtbar: Sicherung wird zurückgenommen, kein Nachmelden beim nächsten Start', async () => {
  const local = memoryStorage()
  const first = setup({ local })
  const visit = first.a.startVisit('rezept_verlassen', { recipeId: 3 })
  first.a.onVisibility(false)
  first.a.onVisibility(true)
  assert.equal(local.getItem('an:visits'), null)
  visit.end()
  await first.a.flush({ all: true })

  const second = setup({ local })
  second.a.recoverVisits()
  await second.a.flush({ all: true })
  assert.equal(second.flat().length, 0)
  assert.equal(first.flat().filter((e) => e.event === 'rezept_verlassen').length, 1)
})

test('Anderer Tab hat den Besuch schon nachgemeldet: beim Zurückkommen nicht doppelt zählen', async () => {
  const local = memoryStorage()
  const tabA = setup({ local })
  const visit = tabA.a.startVisit('rezept_verlassen', { recipeId: 3 })
  tabA.a.onVisibility(false)
  await tabA.a.flush({ all: true })

  const tabB = setup({ local })
  tabB.a.recoverVisits()
  await tabB.a.flush({ all: true })

  tabA.a.onVisibility(true)
  visit.end()
  await tabA.a.flush({ all: true })
  const all = [...tabA.flat(), ...tabB.flat()].filter((e) => e.event === 'rezept_verlassen')
  assert.equal(all.length, 1)
})

test('jsonStore: kaputter Inhalt und fehlender Speicher stören nicht', () => {
  const storage = memoryStorage()
  storage.setItem('k', '{kaputt')
  const store = jsonStore(storage)
  assert.equal(store.get('k'), null)
  const memoryOnly = jsonStore(null)
  memoryOnly.set('a', [1])
  assert.deepEqual(memoryOnly.get('a'), [1])
  const throwing = jsonStore({ getItem() { throw new Error('x') }, setItem() { throw new Error('x') }, removeItem() { throw new Error('x') } })
  throwing.set('a', 1)
  assert.equal(throwing.get('a'), null)
  throwing.remove('a')
})

test('Versand wirft: track und flush werfen nie', async () => {
  const s = setup({ status: new Error('boom') })
  assert.doesNotThrow(() => s.a.track('x'))
  await assert.doesNotReject(s.a.flush())
})

test('Besuch endet, während er gesichert ist: Sicherung weg, kein Nachmelden', async () => {
  const local = memoryStorage()
  const first = setup({ local })
  const visit = first.a.startVisit('rezept_verlassen', { recipeId: 4 })
  first.a.onVisibility(false)
  visit.end()
  assert.equal(local.getItem('an:visits'), null)
  const second = setup({ local })
  second.a.recoverVisits()
  await second.a.flush({ all: true })
  await first.a.flush({ all: true })
  const ends = [...first.flat(), ...second.flat()].filter((e) => e.event === 'rezept_verlassen')
  assert.equal(ends.length, 1)
  assert.equal(ends[0].props.abschluss, undefined)
})
