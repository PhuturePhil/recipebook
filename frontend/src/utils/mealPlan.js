import { formatPrepTime } from './recipeHero.js'
import { sourceDomain } from './sourceUrl.js'
import { wantsTranslation } from './recipeLanguage.js'
import { planLanguage, planText } from './planTexts.js'
import { normalizeText } from './recipeSearch.js'

// Essensplan: Datumslogik immer in lokaler Zeit (nie toISOString – das verschiebt nachts um einen Tag).
// Wochen beginnen am Montag; Datumswerte zum Server als yyyy-MM-dd.

export const VIEWS = ['woche', 'monat', 'liste']

const pad = (n) => String(n).padStart(2, '0')
const locale = (lang) => (lang === 'en' ? 'en-GB' : 'de-DE')

export const isoDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

export function fromIso(value) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value ?? ''))
  if (!m) return null
  const d = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  return d.getMonth() === Number(m[2]) - 1 ? d : null
}

export const dayOnly = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
export const addDays = (d, n) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + n)
export const sameDay = (a, b) => !!a && !!b && isoDate(a) === isoDate(b)
export const monthStart = (d) => new Date(d.getFullYear(), d.getMonth(), 1)
export const addMonths = (d, n) => new Date(d.getFullYear(), d.getMonth() + n, 1)
export const sameMonth = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth()
export const isWeekend = (d) => d.getDay() === 0 || d.getDay() === 6

// Montag der Woche
export function weekStart(d) {
  const day = d.getDay() || 7
  return addDays(dayOnly(d), 1 - day)
}

export const weekDays = (d) => Array.from({ length: 7 }, (_, i) => addDays(weekStart(d), i))

// ISO-8601-Kalenderwoche (die Woche mit dem Donnerstag zählt)
export function isoWeek(d) {
  const thursday = addDays(weekStart(d), 3)
  const firstThursday = addDays(weekStart(new Date(thursday.getFullYear(), 0, 4)), 3)
  return 1 + Math.round((thursday - firstThursday) / (7 * 864e5))
}

export function monthDays(d) {
  const first = monthStart(d)
  const count = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate()
  return Array.from({ length: count }, (_, i) => addDays(first, i))
}

// Monatsraster Mo–So: Tage davor leer (null), danach bis Sonntag als gedimmte Folgetage
export function monthGrid(d) {
  const days = monthDays(d)
  const lead = (days[0].getDay() || 7) - 1
  const cells = Array.from({ length: lead }, () => ({ date: null, inMonth: false }))
  days.forEach((date) => cells.push({ date, inMonth: true }))
  let next = addDays(days[days.length - 1], 1)
  while (cells.length % 7 !== 0) {
    cells.push({ date: next, inMonth: false })
    next = addDays(next, 1)
  }
  return cells
}

// Abfragezeitraum je Ansicht: Woche Mo–So, Monat und Liste der ganze Monat
export function rangeFor(view, anchor) {
  if (view === 'woche') {
    const start = weekStart(anchor)
    return { from: isoDate(start), to: isoDate(addDays(start, 6)) }
  }
  const days = monthDays(anchor)
  return { from: isoDate(days[0]), to: isoDate(days[days.length - 1]) }
}

export const shiftAnchor = (view, anchor, step) =>
  view === 'woche' ? addDays(weekStart(anchor), 7 * step) : addMonths(anchor, step)

export const isCurrentPeriod = (view, anchor, today) =>
  view === 'woche' ? sameDay(weekStart(anchor), weekStart(today)) : sameMonth(anchor, today)

// Tag + Monatskürzel („26. Okt.“) – das Kürzel allein hätte im Deutschen keinen Punkt
const dayMonth = (d, lang) => new Intl.DateTimeFormat(locale(lang), { day: 'numeric', month: 'short' }).format(d)

// „26. Okt. – 1. Nov.“ bzw. „2.–8. Nov.“ innerhalb eines Monats
export function weekLabel(anchor, lang = planLanguage()) {
  const start = weekStart(anchor)
  const end = addDays(start, 6)
  if (start.getMonth() === end.getMonth()) {
    return `${start.getDate()}${lang === 'en' ? '' : '.'}–${dayMonth(end, lang)}`
  }
  return `${dayMonth(start, lang)} – ${dayMonth(end, lang)}`
}

// „Diese Woche · KW 44“, sonst nur „KW 46“
export function weekSubLabel(anchor, today, lang = planLanguage()) {
  const diff = Math.round((weekStart(anchor) - weekStart(today)) / (7 * 864e5))
  const kw = planText('calendarWeek', { n: isoWeek(anchor) }, lang)
  const rel = { 0: 'thisWeek', [-1]: 'lastWeek', 1: 'nextWeekShort' }[diff]
  return rel ? `${planText(rel, {}, lang)} · ${kw}` : kw
}

export const monthLabel = (d, lang = planLanguage()) =>
  new Intl.DateTimeFormat(locale(lang), { month: 'long', year: 'numeric' }).format(d)

// „Montag, 26. Oktober“
export const dayLabel = (d, lang = planLanguage()) =>
  new Intl.DateTimeFormat(locale(lang), { weekday: 'long', day: 'numeric', month: 'long' }).format(d)

// „Mo“, „Di“ … (ohne Punkt)
export const weekdayShort = (d, lang = planLanguage()) =>
  new Intl.DateTimeFormat(locale(lang), { weekday: 'short' }).format(d).replace('.', '').slice(0, 2)

export function groupByDate(entries) {
  const map = new Map()
  for (const e of entries ?? []) {
    if (!map.has(e.date)) map.set(e.date, [])
    map.get(e.date).push(e)
  }
  return map
}

// Titel in Anzeigesprache: englische Rezepte mit gespeicherter Übersetzung deutsch, wenn das Gerät „Deutsch“ will
export function entryTitle(entry, languagePreference) {
  if (!entry?.recipe) return entry?.freeText ?? ''
  const r = entry.recipe
  return wantsTranslation(r, languagePreference) && r.translatedTitle ? r.translatedTitle : r.title
}

// „50 Min. · Anna Jones“ – Autor, sonst die Quelle (Webadresse nur als Domain)
export function entryMeta(entry) {
  const r = entry?.recipe
  if (!r) return ''
  const author = String(r.author ?? '').trim()
  const source = String(r.source ?? '').trim()
  const who = author || sourceDomain(source) || source
  return [formatPrepTime(r.prepTimeMinutes), who].filter(Boolean).join(' · ')
}

/**
 * „Planen“ auf der Rezeptseite: In welchen Familien darf das Rezept stehen (für sie freigegeben oder öffentlich)?
 * Ohne solche Familie kann nur der Besitzer es beim Planen für eine seiner Familien freigeben.
 */
export function planTargets(recipe, families, isOwner) {
  const mine = families ?? []
  const plannable = mine.filter((f) => plannableIn(recipe, f.id))
  const shareable = plannable.length === 0 && isOwner ? mine : []
  return { plannable, shareable }
}

// Neuer Eintrag landet sortiert im geladenen Zeitraum (gleicher Tag: hinten)
export function insertEntry(entries, entry) {
  const list = [...(entries ?? []), entry]
  return list.sort((a, b) => (a.date === b.date ? 0 : a.date < b.date ? -1 : 1))
}

// Hinweis nach dem Speichern, wenn der Server wegen entzogener Freigabe Planeinträge entfernt hat
export function removedPlanEntriesText(count, lang = planLanguage()) {
  const n = Number(count) || 0
  if (n <= 0) return ''
  return n === 1 ? planText('removedAfterSaveOne', {}, lang) : planText('removedAfterSave', { n }, lang)
}

// Kleine Fassung des Rezeptfotos (192 px, Server-Endpunkt size=thumb) statt des Originals je Zeile
export function thumbFor(imageUrl) {
  const url = String(imageUrl ?? '')
  if (!/^\/api\/recipes\/\d+\/image(\?|$)/.test(url) || url.includes('size=')) return url || null
  return `${url}${url.includes('?') ? '&' : '?'}size=thumb`
}

// Darf das Rezept in den Plan dieser Familie? (öffentlich oder für sie freigegeben – wie der Server prüft)
export function plannableIn(recipe, familyId) {
  if (!recipe || familyId == null) return false
  if (recipe.visibility === 'PUBLIC') return true
  const id = Number(familyId)
  return (recipe.families ?? []).some((f) => f.id === id) || (recipe.familyIds ?? []).includes(id)
}

// Picker: planbare Rezepte der Familie, alle Suchwörter müssen vorkommen (Titel, Übersetzung, Autor, Quelle, Tags)
export function pickerRecipes(recipes, familyId, query = '') {
  const words = normalizeText(query).split(' ').filter(Boolean)
  const hay = (r) => normalizeText([r.title, r.translatedSearchText, r.author, r.source, ...(r.tags ?? [])]
    .filter(Boolean).join(' | '))
  return (recipes ?? [])
    .filter((r) => plannableIn(r, familyId))
    .filter((r) => words.every((w) => hay(r).includes(w)))
    .sort((a, b) => String(a.title ?? '').localeCompare(String(b.title ?? ''), 'de'))
}

// Monatsraster: Foto des ersten Eintrags mit Bild
export const dayPhoto = (entries) => (entries ?? []).find((e) => e.recipe?.thumbUrl)?.recipe.thumbUrl ?? null
