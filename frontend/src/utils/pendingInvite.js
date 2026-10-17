// Offene Familien-Einladung über den Login hinweg merken: Die SSO-Anmeldung (pastoors.cloud) verlässt die App
// und landet danach auf /login/oidc, ohne das ursprüngliche Ziel – dorthin geht es dann zurück zur Einladung.
const KEY = 'familien:einladung'
const MAX_AGE_MS = 60 * 60 * 1000
const CODE = /^[A-Za-z0-9_-]{16,64}$/

export function rememberInvite(code, storage = window.sessionStorage, now = Date.now()) {
  if (!CODE.test(code ?? '')) return
  try { storage.setItem(KEY, JSON.stringify({ code, ts: now })) } catch { /* privater Modus */ }
}

export function takeInvite(storage = window.sessionStorage, now = Date.now()) {
  try {
    const raw = storage.getItem(KEY)
    storage.removeItem(KEY)
    const saved = raw ? JSON.parse(raw) : null
    return saved && CODE.test(saved.code ?? '') && now - saved.ts < MAX_AGE_MS ? saved.code : null
  } catch {
    return null
  }
}

export const invitePath = (code) => `/einladung/${code}`
