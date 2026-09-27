// Link zur Originalseite: dieselben Regeln wie im Backend (RecipeSourceUrl)
export const MAX_SOURCE_URL = 2048

const WEB_ADDRESS = /^https?:\/\/[^\s/?#@:]+\.[^\s/?#@:]+(:\d{1,5})?([/?#]\S*)?$/i
const HOST = /^https?:\/\/([^\s/?#@:]+)/i
const LOOKS_LIKE_DOMAIN = /^(www\.)?[^\s/?#@:]+\.[a-z]{2,}([/?#:]\S*)?$/i

export const SOURCE_URL_INVALID =
  'Bitte gib einen vollständigen Link an, der mit http:// oder https:// beginnt, z. B. https://www.zeit.de/…'
export const SOURCE_URL_TOO_LONG = `Der Link darf höchstens ${MAX_SOURCE_URL} Zeichen lang sein.`

export const cleanSourceUrl = (value) => String(value ?? '').trim()

// Fehlermeldung für das Formular, oder '' wenn der Link passt (leer ist erlaubt)
export function sourceUrlProblem(value) {
  const url = cleanSourceUrl(value)
  if (!url) return ''
  if (url.length > MAX_SOURCE_URL) return SOURCE_URL_TOO_LONG
  return WEB_ADDRESS.test(url) ? '' : SOURCE_URL_INVALID
}

// "www.zeit.de/…" oder "zeit.de/…" ohne Protokoll bekommt beim Verlassen des Felds https:// davor
export function completeSourceUrl(value) {
  const url = cleanSourceUrl(value)
  if (!url || /^[a-z][a-z0-9+.-]*:/i.test(url)) return url
  return LOOKS_LIKE_DOMAIN.test(url) ? `https://${url}` : url
}

// Angezeigte Domain: klein, ohne "www." und Port; null bei ungültigem Link
export function sourceDomain(value) {
  const url = cleanSourceUrl(value)
  if (sourceUrlProblem(url) || !url) return null
  return url.match(HOST)[1].toLowerCase().replace(/^www\./, '')
}
