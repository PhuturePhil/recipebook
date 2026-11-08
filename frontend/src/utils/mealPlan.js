import { formatPrepTime } from './recipeHero.js'
import { cleanSourceUrl, sourceDomain } from './sourceUrl.js'
import { wantsTranslation } from './recipeLanguage.js'
import { planLanguage, planText } from './planTexts.js'
import { normalizeText } from './recipeSearch.js'
import { planRecipePath, recipePath } from '../router/paths.js'

// Essensplan: Datumslogik immer in lokaler Zeit (nie toISOString – das verschiebt nachts um einen Tag).
// Wochen beginnen am Montag; Datumswerte zum Server als yyyy-MM-dd.

export const VIEWS = ['woche', 'monat', 'liste']

// Familie, die der Umschalter im Essensplan für die laufende Sitzung gewählt hat (sonst gilt die Standard-Familie)
export const PLAN_FAMILY_KEY = 'mealPlan:familie'

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

// Zugriff je Eintrag (Server): regulär, nur über den Plan, oder Geist (Rezept gelöscht / niemand hat mehr Zugriff)
export const ACCESS = { FULL: 'full', PLAN: 'plan', GONE: 'gone' }
export const isGhost = (entry) => entry?.access === ACCESS.GONE

// Wohin Tippen auf einen Eintrag führt: reguläre Rezeptseite, Nur-Plan-Seite über die Eintrags-ID, sonst nirgends
export function entryPath(entry) {
  if (!entry?.recipe) return null
  if (entry.access === ACCESS.PLAN) return planRecipePath(entry.id)
  return recipePath(entry.recipe.id)
}

// Titel in Anzeigesprache: englische Rezepte mit gespeicherter Übersetzung deutsch, wenn das Gerät „Deutsch“ will;
// Geist: Titel aus dem Snapshot des Eintrags
export function entryTitle(entry, languagePreference) {
  if (!entry?.recipe) return entry?.gone?.title ?? entry?.freeText ?? ''
  const r = entry.recipe
  return wantsTranslation(r, languagePreference) && r.translatedTitle ? r.translatedTitle : r.title
}

// „50 Min. · Anna Jones“ – Autor, sonst die Quelle (Webadresse nur als Domain)
export function entryMeta(entry) {
  if (isGhost(entry)) return planText('gone')
  const r = entry?.recipe
  if (!r) return ''
  const author = String(r.author ?? '').trim()
  const source = String(r.source ?? '').trim()
  const who = author || sourceDomain(source) || source
  return [formatPrepTime(r.prepTimeMinutes), who].filter(Boolean).join(' · ')
}

// Geist-Sheet „Quelle: Autor · Quelle-Link“ aus dem Snapshot des Eintrags: Link nur auf gültige http(s)-Adressen,
// Linktext ist die Quelle (eine Webadresse als Domain); ohne Autor und Quelle null
export function goneSource(gone) {
  const author = String(gone?.author ?? '').trim()
  const source = String(gone?.source ?? '').trim()
  const url = cleanSourceUrl(gone?.sourceUrl)
  const href = sourceDomain(url) ? url : (sourceDomain(source) ? source : null)
  const label = source && !sourceDomain(source) ? source : (href ? sourceDomain(href) : '')
  if (!author && !label) return null
  return { author, label, href }
}

// Standard-Familie (vom Server markiert), sonst die erste
export function defaultFamilyId(families) {
  const list = families ?? []
  return (list.find((f) => f.defaultFamily) ?? list[0])?.id ?? null
}

// Planen in eine Familie, die das Rezept nicht sieht: nur der Besitzer wird gefragt (teilen oder nur über den Plan),
// alle anderen planen still „nur über den Plan“
export const asksToShare = (recipe, familyId, isOwner) => !!isOwner && familyId != null && !plannableIn(recipe, familyId)

// Neuer Eintrag landet sortiert im geladenen Zeitraum (gleicher Tag: hinten)
export function insertEntry(entries, entry) {
  const list = [...(entries ?? []), entry]
  return list.sort((a, b) => (a.date === b.date ? 0 : a.date < b.date ? -1 : 1))
}

// Kleine Fassung des Rezeptfotos (192 px, Server-Endpunkt size=thumb) statt des Originals je Zeile
export function thumbFor(imageUrl) {
  const url = String(imageUrl ?? '')
  if (!/^\/api\/recipes\/\d+\/image(\?|$)/.test(url) || url.includes('size=')) return url || null
  return `${url}${url.includes('?') ? '&' : '?'}size=thumb`
}

// Sieht die ganze Familie das Rezept regulär? (öffentlich oder für sie freigegeben) – sonst nur über den Plan
export function plannableIn(recipe, familyId) {
  if (!recipe || familyId == null) return false
  if (recipe.visibility === 'PUBLIC') return true
  const id = Number(familyId)
  return (recipe.families ?? []).some((f) => f.id === id) || (recipe.familyIds ?? []).includes(id)
}

// Picker: alle Rezepte, die ich sehe (auch für Familien ohne Sicht – dann nur über den Plan); alle Suchwörter müssen
// vorkommen (Titel, Übersetzung, Autor, Quelle, Tags)
export function pickerRecipes(recipes, query = '') {
  const words = normalizeText(query).split(' ').filter(Boolean)
  const hay = (r) => normalizeText([r.title, r.translatedSearchText, r.author, r.source, ...(r.tags ?? [])]
    .filter(Boolean).join(' | '))
  return (recipes ?? [])
    .filter((r) => words.every((w) => hay(r).includes(w)))
    .sort((a, b) => String(a.title ?? '').localeCompare(String(b.title ?? ''), 'de'))
}

// Monatsraster: Foto des ersten Eintrags mit Bild
export const dayPhoto = (entries) => (entries ?? []).find((e) => e.recipe?.thumbUrl)?.recipe.thumbUrl ?? null

// Datumswahl (Planen, Verschieben): belegte Tage der Familie je Monat einmal laden (ganzer Monat, ≤ 62 Tage).
// Fehlschlag: keine Markierung, der nächste Aufruf versucht es erneut – gewählt werden kann trotzdem.
export function bookedDaysLoader(list) {
  const months = new Map()
  return (month) => {
    const { from, to } = rangeFor('monat', month)
    if (!months.has(from)) {
      months.set(from, Promise.resolve()
        .then(() => list(from, to))
        .then((entries) => (entries ?? []).map((e) => e.date), () => {
          months.delete(from)
          return []
        }))
    }
    return months.get(from)
  }
}
