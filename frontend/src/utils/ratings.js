// Bewertungen: Anzeige-Helfer (Durchschnitt mit einer Nachkommastelle, deutsches Komma)

export const formatRating = (avg) => {
  if (avg == null || Number.isNaN(Number(avg))) return ''
  const rounded = Math.round(Number(avg) * 10 + 1e-9) / 10
  return rounded.toFixed(1).replace('.', ',')
}

export const ratingLabel = (avg, count) => (count ? `${formatRating(avg)} (${count})` : '')

export const ratingCountLabel = (count) => (count === 1 ? '1 Bewertung' : `${count} Bewertungen`)

// Durchschnitt und Anzahl über ALLE Bewertungen (auch mehrere desselben Nutzers)
export const ratingSummary = (ratings) => {
  const list = (ratings ?? []).filter((r) => Number.isFinite(r?.stars))
  if (!list.length) return { avg: null, count: 0 }
  const sum = list.reduce((total, r) => total + r.stars, 0)
  return { avg: sum / list.length, count: list.length }
}

// Liste kommt neueste zuerst. Hat ein Nutzer mehrfach bewertet, gelten alle außer seiner ältesten als „aktualisiert“.
export const markUpdated = (ratings) => {
  const list = ratings ?? []
  const seenOlder = new Set()
  const flags = new Array(list.length)
  for (let i = list.length - 1; i >= 0; i--) {
    const userId = list[i]?.userId
    flags[i] = userId != null && seenOlder.has(userId)
    if (userId != null) seenOlder.add(userId)
  }
  return list.map((r, i) => ({ ...r, updated: flags[i] }))
}

export const formatRatingDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`
}

// „Angelegt von Anna Pastoors am 27.09.2026“ – ohne bekannten Nutzer nur „Angelegt am …“
export const createdLine = (createdBy, createdAt) => {
  const date = formatRatingDate(createdAt)
  if (!date) return ''
  const name = (createdBy ?? '').trim()
  return name ? `Angelegt von ${name} am ${date}` : `Angelegt am ${date}`
}

export const raterName = (rating) => rating?.userName?.trim() || 'Ehemaliges Mitglied'
