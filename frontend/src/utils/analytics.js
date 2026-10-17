// Nutzungsstatistik (Doku: docs/analytics.md). Generisch gehalten, damit das Modul 1:1 in die anderen Apps passt.
// Events landen in einer Warteschlange (localStorage) und gehen gebündelt per fetch keepalive an POST /api/events.
// Ausnahme: Gäste auf einer Teilen-Seite melden nur den Aufruf, sofort und ohne Login an POST /api/events/public.
// Tracking darf die App nie stören: jeder Fehler wird still geschluckt.

export const ENDPOINT = '/api/events'
export const PUBLIC_ENDPOINT = '/api/events/public'
// Diese Events dürfen auch auf der Teilen-Seite erfasst werden (sonst wird sie nicht getrackt)
export const SHARE_EVENTS = ['teilen_link_aufgerufen', 'teilen_login']
export const BATCH_SIZE = 50
export const MAX_QUEUE = 200
export const FLUSH_INTERVAL_MS = 30 * 1000
export const FLUSH_AT = 20
export const SESSION_IDLE_MS = 30 * 60 * 1000
export const RECIPE_TOTAL_CAP_MS = 6 * 60 * 60 * 1000
export const PAGE_VISIBLE_CAP_MS = 30 * 60 * 1000
export const SEARCH_DELAY_MS = 1200
export const SERVINGS_DELAY_MS = 1500
// Ab hier gilt das Layout als Desktop (Seitenleiste statt Tab-Leiste)
export const DESKTOP_MIN_WIDTH = 1024

const QUEUE_KEY = 'an:queue'
const VISITS_KEY = 'an:visits'
const SESSION_KEY = 'an:session'
const SHARE_SEEN_KEY = 'an:share-seen'
const SHARE_GUEST_KEY = 'an:share-guest'

export function deviceType({ ua = '', width = 0, touchPoints = 0 } = {}) {
  if (/Mobi|Android|iPhone|iPad|iPod/i.test(ua)) return 'mobile'
  if (/Macintosh/.test(ua) && touchPoints > 1) return 'mobile'   // iPadOS meldet sich als Mac
  return width > 0 && width < DESKTOP_MIN_WIDTH ? 'mobile' : 'desktop'
}

export function displayMode({ standalone = false, iosStandalone = false } = {}) {
  return standalone || iosStandalone ? 'pwa' : 'browser'
}

// Woher kam man auf die Rezeptseite? fromName = Name der vorigen Route (leer beim ersten Aufruf der App)
export function recipeOrigin(fromName, hasSearch = false) {
  if (!fromName) return 'direkt'
  if (fromName === 'home') return hasSearch ? 'suche' : 'liste'
  if (fromName === 'shared-recipe') return 'teilen_link'
  if (fromName === 'recipe-edit' || fromName === 'recipe-new') return 'formular'
  return 'sonstige'
}

export function languageDirection(value) {
  return value === 'de' ? 'en→de' : 'de→en'
}

// Zwei Uhren pro Besuch: sichtbare Zeit (pausiert, solange die Seite verdeckt bzw. das Display aus ist)
// und Gesamtspanne vom Öffnen bis jetzt inkl. Unterbrechungen; beide optional gekappt
export function createVisitTimer({ start, visible = true, capVisibleMs = Infinity, capTotalMs = Infinity }) {
  let visibleMs = 0
  let visibleSince = visible ? start : null
  return {
    setVisible(isVisible, t) {
      if (isVisible && visibleSince === null) {
        visibleSince = t
      } else if (!isVisible && visibleSince !== null) {
        visibleMs += Math.max(0, t - visibleSince)
        visibleSince = null
      }
    },
    snapshot(t) {
      const running = visibleSince !== null ? Math.max(0, t - visibleSince) : 0
      return {
        sichtbar_ms: Math.round(Math.min(visibleMs + running, capVisibleMs)),
        gesamt_ms: Math.round(Math.min(Math.max(0, t - start), capTotalMs)),
      }
    },
  }
}

export function createDebouncer(delayMs, fn, { setTimer = setTimeout, clearTimer = clearTimeout } = {}) {
  let timer = null
  let pending = false
  let value
  const fire = () => {
    timer = null
    if (!pending) return
    pending = false
    fn(value)
  }
  return {
    push(next) {
      value = next
      pending = true
      clearTimer(timer)
      timer = setTimer(fire, delayMs)
    },
    flush() {
      clearTimer(timer)
      fire()
    },
    cancel() {
      clearTimer(timer)
      timer = null
      pending = false
    },
  }
}

// Mehrere schnelle Änderungen (z. B. Portionen +/+/+) ergeben ein Event „von → auf“; zurück auf den Start = kein Event
export function createChangeAggregator(delayMs, onSettle, timers) {
  let from = null
  const debouncer = createDebouncer(delayMs, (to) => {
    const start = from
    from = null
    if (start !== to) onSettle(start, to)
  }, timers)
  return {
    change(previous, next) {
      if (from === null) from = previous
      debouncer.push(next)
    },
    flush: () => debouncer.flush(),
  }
}

// Suche erst nach einer Tipppause bzw. beim Abschicken melden, nie je Buchstabe; dieselbe Suche nicht doppelt
export function createSearchReporter(delayMs, report, timers) {
  let last = ''
  const debouncer = createDebouncer(delayMs, (raw) => {
    const query = String(raw ?? '').trim()
    if (!query) {
      last = ''
      return
    }
    if (query === last) return
    last = query
    report(query)
  }, timers)
  return { push: (query) => debouncer.push(query), flush: () => debouncer.flush(), cancel: () => debouncer.cancel() }
}

export function randomId() {
  try {
    if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  } catch {
    // fällt auf Math.random zurück
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

// Speicher-Hülle: JSON lesen/schreiben, ohne Speicher (privater Modus) im Arbeitsspeicher
export function jsonStore(storage) {
  const memory = new Map()
  return {
    get(key) {
      try {
        const raw = storage ? storage.getItem(key) : memory.get(key)
        return raw == null ? null : JSON.parse(raw)
      } catch {
        return null
      }
    },
    set(key, value) {
      const raw = JSON.stringify(value)
      try {
        if (storage) storage.setItem(key, raw)
        else memory.set(key, raw)
      } catch {
        memory.set(key, raw)
      }
    },
    remove(key) {
      try {
        if (storage) storage.removeItem(key)
      } catch {
        // nichts zu tun
      }
      memory.delete(key)
    },
  }
}

// Kern ohne Browser-Abhängigkeiten (Tests injizieren Uhr, Speicher und Versand).
// local: geteilt zwischen Tabs (Warteschlange, offene Besuche); session: pro Tab (Sitzungs-ID).
// isEnabled(event): darf dieses Event erfasst werden; ohne Event: darf die Warteschlange abgeschickt werden.
export function createAnalytics({
  send,
  sendAnonymous = async () => 0,
  local,
  session,
  now = Date.now,
  isEnabled = () => true,
  owner = () => null,
  context = () => ({ device: null, display: null }),
  isVisible = () => true,
  newId = randomId,
}) {
  const pageId = newId()
  const visits = new Map()
  let sending = 0

  const readQueue = () => {
    const q = local.get(QUEUE_KEY)
    return Array.isArray(q) ? q : []
  }
  const writeQueue = (q) => (q.length ? local.set(QUEUE_KEY, q.slice(-MAX_QUEUE)) : local.remove(QUEUE_KEY))
  const readVisits = () => {
    const v = local.get(VISITS_KEY)
    return v && typeof v === 'object' && !Array.isArray(v) ? v : {}
  }
  const writeVisits = (v) => (Object.keys(v).length ? local.set(VISITS_KEY, v) : local.remove(VISITS_KEY))

  // Eine Sitzung = ein App-Besuch: neuer Tab/Start oder mehr als 30 Min. im Hintergrund beginnt eine neue
  function sessionId() {
    const s = session.get(SESSION_KEY)
    if (s && typeof s.id === 'string') return s.id
    const fresh = { id: newId() }
    session.set(SESSION_KEY, fresh)
    return fresh.id
  }

  function sessionVisibility(visible, t) {
    const s = session.get(SESSION_KEY)
    if (!s || typeof s.id !== 'string') return
    if (!visible) session.set(SESSION_KEY, { ...s, hiddenAt: t })
    else if (s.hiddenAt != null && t - s.hiddenAt > SESSION_IDLE_MS) session.set(SESSION_KEY, { id: newId() })
    else session.set(SESSION_KEY, { id: s.id })
  }

  function track(event, props = {}, override = {}) {
    try {
      if (!isEnabled(event)) return
      const ctx = context()
      const queue = readQueue()
      queue.push({
        event,
        ts: override.ts ?? now(),
        sessionId: override.sessionId ?? sessionId(),
        device: ctx.device,
        display: ctx.display,
        props,
        owner: override.owner ?? owner(),
      })
      writeQueue(queue)
      if (queue.length >= FLUSH_AT) flush()
    } catch {
      // Tracking stört nie
    }
  }

  // Gast ohne Login: ein Event sofort abschicken, ohne Warteschlange und ohne Konto (geht es schief, fehlt es eben)
  function trackAnonymous(event, props = {}) {
    try {
      const ctx = context()
      const e = { event, ts: now(), sessionId: sessionId(), device: ctx.device, display: ctx.display, props }
      return Promise.resolve(sendAnonymous([e])).catch(() => {})
    } catch {
      return Promise.resolve()
    }
  }

  async function sendBatch(batch) {
    let keep
    try {
      const status = await send(batch.map(({ owner: _owner, ...e }) => e))
      keep = !status || status === 429 || status >= 500
    } catch {
      keep = true
    }
    if (keep) writeQueue([...batch, ...readQueue()])
  }

  // Die Warteschlange im Speicher ist die Quelle: wer sendet, nimmt die Events heraus (kein Doppelversand über Tabs).
  // Events eines anderen Kontos (Abmelden/Kontowechsel) werden verworfen statt unter falschem Namen gezählt.
  function flush({ all = false } = {}) {
    try {
      if (!isEnabled() || (sending && !all)) return Promise.resolve()
      const me = owner()
      let queue = readQueue().filter((e) => e && e.owner === me)
      if (!queue.length) {
        writeQueue([])
        return Promise.resolve()
      }
      const batches = []
      do {
        batches.push(queue.slice(0, BATCH_SIZE))
        queue = queue.slice(BATCH_SIZE)
      } while (all && queue.length)
      writeQueue(queue)
      sending++
      return Promise.all(batches.map(sendBatch)).finally(() => { sending-- })
    } catch {
      return Promise.resolve()
    }
  }

  function visitProps(visit, t) {
    const snap = visit.timer.snapshot(t)
    const out = { ...visit.props }
    for (const field of visit.fields) out[field] = snap[field]
    return out
  }

  function endVisit(id, extra = {}) {
    const visit = visits.get(id)
    if (!visit) return
    visits.delete(id)
    if (visit.saved) {
      const stored = readVisits()
      delete stored[id]
      writeVisits(stored)
    }
    track(visit.event, { ...visitProps(visit, now()), ...extra }, { sessionId: visit.sessionId, owner: visit.owner })
  }

  // Ein Besuch meldet sich beim Ende als ein Event mit Dauer(n). fields: welche Uhren ins Event gehen.
  function startVisit(event, props = {}, { capVisibleMs, capTotalMs, fields = ['sichtbar_ms', 'gesamt_ms'] } = {}) {
    try {
      const id = newId()
      const start = now()
      visits.set(id, {
        event,
        props,
        fields,
        owner: owner(),
        sessionId: sessionId(),
        timer: createVisitTimer({ start, visible: isVisible(), capVisibleMs, capTotalMs }),
      })
      return { id, end: (extra) => { try { endVisit(id, extra) } catch { /* still */ } } }
    } catch {
      return { id: null, end() {} }
    }
  }

  // Verdeckt: Uhren anhalten, offene Besuche sichern (falls das System die App beendet) und alles abschicken.
  // Wieder sichtbar: Sicherung zurücknehmen; hat ein anderer Tab sie schon als abgebrochen gemeldet, nicht doppelt zählen.
  function onVisibility(visible) {
    try {
      const t = now()
      sessionVisibility(visible, t)
      for (const v of visits.values()) v.timer.setVisible(visible, t)
      const stored = readVisits()
      if (visible) {
        for (const id of [...visits.keys()]) {
          if (visits.get(id).saved && !stored[id]) visits.delete(id)
          else visits.get(id).saved = false
          delete stored[id]
        }
      } else {
        for (const [id, v] of visits) {
          stored[id] = { event: v.event, ts: t, sessionId: v.sessionId, owner: v.owner, props: visitProps(v, t) }
          v.saved = true
        }
        flush({ all: true })
      }
      writeVisits(stored)
    } catch {
      // still
    }
  }

  function onPageHide() {
    try {
      const stored = readVisits()
      for (const id of [...visits.keys()]) {
        delete stored[id]
        endVisit(id, { abschluss: 'app_geschlossen' })
      }
      writeVisits(stored)
      flush({ all: true })
    } catch {
      // still
    }
  }

  // Beim Start: Besuche, deren Seite im Hintergrund beendet wurde, mit dem Stand beim Verdecken nachmelden
  function recoverVisits() {
    try {
      const stored = readVisits()
      local.remove(VISITS_KEY)
      for (const entry of Object.values(stored)) {
        if (entry?.event) track(entry.event, { ...entry.props, abschluss: 'abgebrochen' }, entry)
      }
    } catch {
      // still
    }
  }

  return {
    track, trackAnonymous, flush, startVisit, onVisibility, onPageHide, recoverVisits, pageId,
    currentSessionId: sessionId,
    get activeVisits() { return visits.size },
  }
}

// Teilen-Seite: Aufruf einmal pro Sitzung und Link melden – eingeloggt über die Warteschlange (mit Pseudonym),
// als Gast sofort ohne Login (ohne Pseudonym). Der Token geht nur an den Server, der ihn durch recipeId und einen
// kurzen Hash ersetzt. Gäste merken sich die Herkunft im Tab: meldet man sich danach an, folgt teilen_login.
export function createShareTracking({ analytics, session, now = Date.now }) {
  return {
    viewed(token, loggedIn) {
      try {
        if (typeof token !== 'string' || !token) return
        if (!loggedIn) {
          const origin = session.get(SHARE_GUEST_KEY)
          if (origin?.token !== token) session.set(SHARE_GUEST_KEY, { token, ts: now() })
        }
        const sid = analytics.currentSessionId()
        const seen = session.get(SHARE_SEEN_KEY)
        const tokens = seen?.sessionId === sid && Array.isArray(seen.tokens) ? seen.tokens : []
        if (tokens.includes(token)) return
        session.set(SHARE_SEEN_KEY, { sessionId: sid, tokens: [...tokens, token].slice(-20) })
        if (loggedIn) analytics.track('teilen_link_aufgerufen', { token })
        else analytics.trackAnonymous('teilen_link_aufgerufen', { token })
      } catch {
        // still
      }
    },
    loggedIn() {
      try {
        const origin = session.get(SHARE_GUEST_KEY)
        if (!origin) return
        session.remove(SHARE_GUEST_KEY)
        if (typeof origin.token !== 'string') return
        analytics.track('teilen_login', { token: origin.token, seit_ms: Math.max(0, now() - (origin.ts ?? now())) })
      } catch {
        // still
      }
    },
  }
}

// ---------- Browser ----------

let instance = null
let share = null
let shareLoggedIn = () => false
let lastFrom = ''
// Bis die Anmeldung geladen ist (z. B. „Was ist neu“ direkt nach dem Start), werden Events zurückgehalten
let authReady = null
let authDone = false

export function installAnalytics({ router, authStore }) {
  if (instance || typeof window === 'undefined') return instance
  try {
    const nav = window.navigator
    const media = (q) => { try { return window.matchMedia?.(q).matches ?? false } catch { return false } }
    const loggedIn = () => authStore.isAuthenticated && !!localStorage.getItem('token')
    const session = jsonStore(window.sessionStorage)
    instance = createAnalytics({
      local: jsonStore(window.localStorage),
      session,
      isEnabled: (event) => loggedIn() && (!event || SHARE_EVENTS.includes(event)
        || router.currentRoute.value.name !== 'shared-recipe'),
      owner: () => (authStore.user?.id != null ? String(authStore.user.id) : null),
      context: () => ({
        device: deviceType({ ua: nav.userAgent, width: window.innerWidth, touchPoints: nav.maxTouchPoints }),
        display: displayMode({ standalone: media('(display-mode: standalone)'), iosStandalone: nav.standalone === true }),
      }),
      isVisible: () => document.visibilityState !== 'hidden',
      send: async (events) => {
        const token = localStorage.getItem('token')
        if (!token) return 401
        const response = await window.fetch(ENDPOINT, {
          method: 'POST',
          keepalive: true,
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({ events }),
        })
        return response.status
      },
      sendAnonymous: async (events) => {
        const response = await window.fetch(PUBLIC_ENDPOINT, {
          method: 'POST',
          keepalive: true,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ events }),
        })
        return response.status
      },
    })
    share = createShareTracking({ analytics: instance, session })
    shareLoggedIn = loggedIn
    let wasAuthenticated = authStore.isAuthenticated
    authStore.$subscribe(() => {
      const now = authStore.isAuthenticated
      if (now && !wasAuthenticated) share.loggedIn()
      wasAuthenticated = now
    }, { detached: true })
    document.addEventListener('visibilitychange', () => instance.onVisibility(document.visibilityState !== 'hidden'))
    window.addEventListener('pagehide', () => instance.onPageHide())
    window.setInterval(() => { if (document.visibilityState !== 'hidden') instance.flush() }, FLUSH_INTERVAL_MS)
    router.afterEach((to, from) => { lastFrom = from.matched.length ? from.name : '' })
    authReady = Promise.resolve(authStore.init()).catch(() => {}).then(() => {
      authDone = true
      instance?.recoverVisits()
    })
  } catch {
    instance = null
  }
  return instance
}

export const previousRouteName = () => lastFrom

// Teilen-Seite geöffnet (vor dem Laden des Rezepts; ob der Link gilt, prüft der Server)
export function trackSharedView(token) {
  share?.viewed(token, shareLoggedIn())
}

export function track(event, props) {
  if (!instance) return
  if (authDone) {
    instance.track(event, props)
  } else {
    const ts = Date.now()
    authReady?.then(() => instance?.track(event, props, { ts }))
  }
}

// Vor dem Abmelden: Offenes sofort abschicken, solange das Token noch da ist
export function flushNow() {
  instance?.flush({ all: true })
}

export function startVisit(event, props, options) {
  return instance?.startVisit(event, props, options) ?? { id: null, end() {} }
}
