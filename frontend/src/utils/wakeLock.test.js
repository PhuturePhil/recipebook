import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createWakeLock, KEEP_SCREEN_ON_KEY } from './wakeLock.js'

const flush = () => new Promise((resolve) => setTimeout(resolve, 0))

function fakeSentinel() {
  const listeners = []
  return {
    released: false,
    addEventListener: (type, fn) => type === 'release' && listeners.push(fn),
    release() {
      if (!this.released) {
        this.released = true
        listeners.forEach((fn) => fn())
      }
      return Promise.resolve()
    },
  }
}

function setup({ stored, visibility = 'visible', reject } = {}) {
  const sentinels = []
  const nav = {
    wakeLock: {
      request: (type) => {
        assert.equal(type, 'screen')
        if (reject) return Promise.reject(Object.assign(new Error('denied'), { name: reject }))
        const sentinel = fakeSentinel()
        sentinels.push(sentinel)
        return Promise.resolve(sentinel)
      },
    },
  }
  const listeners = new Set()
  const doc = {
    visibilityState: visibility,
    addEventListener: (type, fn) => type === 'visibilitychange' && listeners.add(fn),
    removeEventListener: (type, fn) => type === 'visibilitychange' && listeners.delete(fn),
    setVisibility(state) {
      this.visibilityState = state
      listeners.forEach((fn) => fn())
    },
  }
  const data = new Map(stored ? [[KEEP_SCREEN_ON_KEY, stored]] : [])
  const storage = {
    getItem: (key) => data.get(key) ?? null,
    setItem: (key, value) => data.set(key, value),
    removeItem: (key) => data.delete(key),
  }
  const states = []
  const lock = createWakeLock({ nav, doc, storage, onChange: (s) => states.push(s) })
  return { lock, nav, doc, data, sentinels, listeners, states }
}

test('requests a screen lock on start and releases it on stop', async () => {
  const { lock, sentinels, listeners } = setup()
  assert.equal(lock.supported, true)
  await lock.start()
  assert.equal(sentinels.length, 1)
  assert.equal(lock.active, true)
  assert.equal(listeners.size, 1)

  lock.stop()
  assert.equal(sentinels[0].released, true)
  assert.equal(lock.active, false)
  assert.equal(listeners.size, 0)
})

test('re-acquires the lock when the tab becomes visible again', async () => {
  const { lock, doc, sentinels } = setup()
  await lock.start()
  // The browser drops the lock on its own when the tab is hidden
  sentinels[0].release()
  doc.setVisibility('hidden')
  await flush()
  assert.equal(sentinels.length, 1)
  assert.equal(lock.active, false)

  doc.setVisibility('visible')
  await flush()
  assert.equal(sentinels.length, 2)
  assert.equal(lock.active, true)
  lock.stop()
})

test('waits for a hidden page to become visible before requesting', async () => {
  const { lock, doc, sentinels } = setup({ visibility: 'hidden' })
  await lock.start()
  assert.equal(sentinels.length, 0)
  doc.setVisibility('visible')
  await flush()
  assert.equal(sentinels.length, 1)
  lock.stop()
})

test('switching off releases the lock and is remembered per device', async () => {
  const { lock, data, sentinels, doc } = setup()
  await lock.start()
  lock.setEnabled(false)
  assert.equal(sentinels[0].released, true)
  assert.equal(lock.enabled, false)
  assert.equal(data.get(KEEP_SCREEN_ON_KEY), 'off')

  doc.setVisibility('visible')
  await flush()
  assert.equal(sentinels.length, 1)

  await lock.setEnabled(true)
  assert.equal(data.has(KEEP_SCREEN_ON_KEY), false)
  assert.equal(sentinels.length, 2)
  assert.equal(lock.active, true)
  lock.stop()
})

test('a stored "off" keeps the lock from being requested', async () => {
  const { lock, sentinels } = setup({ stored: 'off' })
  assert.equal(lock.enabled, false)
  await lock.start()
  assert.equal(sentinels.length, 0)
  lock.stop()
})

test('refusals such as iOS low power mode are swallowed', async () => {
  const { lock, states } = setup({ reject: 'NotAllowedError' })
  await lock.start()
  assert.equal(lock.active, false)
  assert.deepEqual(states.at(-1), { enabled: true, active: false })
  lock.stop()
})

test('a lock granted after leaving the page is released right away', async () => {
  const { lock, sentinels } = setup()
  const pending = lock.start()
  lock.stop()
  await pending
  assert.equal(sentinels.length, 1)
  assert.equal(sentinels[0].released, true)
  assert.equal(lock.active, false)
})

test('browsers without the API do nothing and report unsupported', async () => {
  const lock = createWakeLock({ nav: {}, doc: undefined, storage: undefined })
  assert.equal(lock.supported, false)
  assert.equal(lock.start(), null)
  assert.doesNotThrow(() => lock.stop())
  assert.doesNotThrow(() => lock.setEnabled(false))
})

test('unreadable storage falls back to enabled', () => {
  const storage = { getItem: () => { throw new Error('SecurityError') } }
  const lock = createWakeLock({ nav: { wakeLock: { request: () => Promise.reject(new Error()) } }, storage })
  assert.equal(lock.enabled, true)
})
