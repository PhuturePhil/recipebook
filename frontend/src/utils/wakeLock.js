export const KEEP_SCREEN_ON_KEY = 'keepScreenOn'

const readEnabled = (storage) => {
  try {
    return storage?.getItem(KEEP_SCREEN_ON_KEY) !== 'off'
  } catch {
    return true
  }
}

const writeEnabled = (storage, enabled) => {
  try {
    if (enabled) storage?.removeItem(KEEP_SCREEN_ON_KEY)
    else storage?.setItem(KEEP_SCREEN_ON_KEY, 'off')
  } catch {
    // Private mode or full storage: the choice then only lasts for this page
  }
}

// Keeps the display on while a recipe is open (Screen Wake Lock API). The browser drops the lock
// whenever the tab is hidden, so it is requested again once the page is visible. Refusals (e.g.
// iOS low power mode) are expected and stay silent; unsupported browsers simply do nothing.
export function createWakeLock({ nav, doc, storage, onChange = () => {} } = {}) {
  const supported = typeof nav?.wakeLock?.request === 'function'
  let enabled = readEnabled(storage)
  let running = false
  let sentinel = null
  let pending = null

  const notify = () => onChange({ enabled, active: sentinel !== null })

  const release = () => {
    const current = sentinel
    sentinel = null
    current?.release().catch(() => {})
  }

  const acquire = () => {
    if (!supported || !running || !enabled || sentinel || pending) return pending
    if (doc && doc.visibilityState !== 'visible') return null
    pending = nav.wakeLock.request('screen')
      .then((lock) => {
        if (!running || !enabled) {
          lock.release().catch(() => {})
          return
        }
        sentinel = lock
        lock.addEventListener?.('release', () => {
          if (sentinel === lock) {
            sentinel = null
            notify()
          }
        })
      })
      .catch(() => {})
      .finally(() => {
        pending = null
        notify()
      })
    return pending
  }

  const onVisibilityChange = () => {
    if (doc.visibilityState === 'visible') acquire()
  }

  return {
    supported,
    get enabled() { return enabled },
    get active() { return sentinel !== null },

    start() {
      if (!supported || running) return null
      running = true
      doc?.addEventListener('visibilitychange', onVisibilityChange)
      return acquire()
    },

    stop() {
      if (!running) return
      running = false
      doc?.removeEventListener('visibilitychange', onVisibilityChange)
      release()
      notify()
    },

    setEnabled(value) {
      enabled = Boolean(value)
      writeEnabled(storage, enabled)
      if (!enabled) release()
      notify()
      return enabled ? acquire() : null
    },
  }
}
