import { sourceDomain } from './sourceUrl.js'

// Hero-Darstellung von Rezeptkarte und Rezeptseite: Text liegt weiß im Foto, Eigenschaften und Tags ohne Kapseln

export const QUICK_MAX_MINUTES = 30

// Eigenschaften mit Mini-Icon (HeroIcon), Reihenfolge wie in der Badge-Berechnung
export const BADGE_ICONS = {
  Schnell: 'blitz',
  Fettarm: 'tropfen',
  Proteinreich: 'hantel',
  Energiearm: 'blatt',
  Ballaststoffreich: 'aehre',
}

export const badgeIcon = (badge) => BADGE_ICONS[badge] ?? 'blatt'

// Nährwert-Badges vom Server plus „Schnell“ aus der Zubereitungszeit
export function recipeBadges(nutritionBadges, prepTimeMinutes) {
  const badges = [...(nutritionBadges ?? [])]
  if (prepTimeMinutes != null && prepTimeMinutes <= QUICK_MAX_MINUTES && !badges.includes('Schnell')) badges.push('Schnell')
  return badges
}

export function formatPrepTime(minutes) {
  if (!minutes) return ''
  if (minutes < 60) return `${minutes} Min.`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h} Std. ${m} Min.` : `${h} Std.`
}

const formatKcal = (kcal) => Math.round(kcal).toLocaleString('de-DE')

// „Autor, Quelle“ – steht als Quelle eine Webadresse, nur deren Domain; leer → ''
// (Autor = Name der Website, z. B. „Zucker&Jagdwurst“ bei zuckerjagdwurst.com → nur die Domain)
export function sourceLabel(recipe) {
  const clean = (v) => String(v ?? '').replace(/\s+/g, ' ').trim()
  const key = (v) => v.toLowerCase().replace(/[^a-z0-9äöüß]/g, '')
  const author = clean(recipe?.author)
  const source = clean(recipe?.source)
  const domain = sourceDomain(source)
  const shownSource = domain ?? source
  const sameName = author && (key(author) === key(shownSource) || (domain && key(author) === key(domain.split('.')[0])))
  return (sameName ? [shownSource] : [author, shownSource]).filter(Boolean).join(', ')
}

// Meta-Zeile der Karte: „45 Min. · 12 Zutaten · 447 kcal“ (nur vorhandene Werte)
export function cardMeta(recipe) {
  const parts = []
  if (recipe?.prepTimeMinutes) parts.push(formatPrepTime(recipe.prepTimeMinutes))
  if (recipe?.ingredientCount) parts.push(`${recipe.ingredientCount} ${recipe.ingredientCount === 1 ? 'Zutat' : 'Zutaten'}`)
  const kcal = recipe?.nutrition?.perServing?.kcal
  if (kcal != null) parts.push(`${recipe.nutrition.complete ? '' : 'mind. '}${formatKcal(kcal)} kcal`)
  return parts.join(' · ')
}

// Tag-Zeile der Karte: erst Eigenschaften, dann #Tags, höchstens `max` Einträge, der Rest als „+N“
export function cardTagRow(badges, tags, max = 3) {
  const all = [
    ...(badges ?? []).map((name) => ({ kind: 'badge', name })),
    ...(tags ?? []).map((name) => ({ kind: 'tag', name })),
  ]
  return { items: all.slice(0, max), more: Math.max(0, all.length - max) }
}

// Zutatenmenge in der Liste; ohne Menge und Einheit „n. Bedarf“ (gedämpft)
export function amountText(ingredient, language = 'de') {
  const text = [ingredient?.amount, ingredient?.unit].map((v) => String(v ?? '').trim()).filter(Boolean).join(' ')
  if (text) return { text, toTaste: false }
  return { text: language === 'en' ? 'to taste' : 'n. Bedarf', toTaste: true }
}

// „Angelegt 05.10.2026“ für eigene Rezepte, sonst mit Namen: „Angelegt von Anna Pastoors am 05.10.2026“
export function createdShort(createdBy, createdAt, own) {
  if (!createdAt) return ''
  const date = new Date(createdAt)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  const day = `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`
  const name = String(createdBy ?? '').trim()
  return !own && name ? `Angelegt von ${name} am ${day}` : `Angelegt ${day}`
}
