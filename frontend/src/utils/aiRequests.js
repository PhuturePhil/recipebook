// Tab „KI-Anfragen“ im Nährwertkatalog. Vorschläge, die Nicht-Admins auslösen, warten auf Freigabe (M6).

export const AI_STATUS = {
  PENDING: 'wartet',
  RUNNING: 'läuft',
  REVIEW: 'wartet auf Freigabe',
  DONE: 'erledigt',
  FAILED: 'fehlgeschlagen',
  REJECTED: 'abgelehnt',
}

export function aiStatusLabel(status) {
  return AI_STATUS[status] || status
}

export function canRetry(r) {
  return r.status === 'FAILED' || r.status === 'REJECTED'
}

export function awaitsReview(r) {
  return r.status === 'REVIEW'
}

// Zur Freigabe wartende zuerst, sonst Reihenfolge des Servers
export function sortAiRequests(list) {
  return [...list].sort((a, b) => Number(awaitsReview(b)) - Number(awaitsReview(a)))
}

export function reviewSummary(list) {
  const n = list.filter(awaitsReview).length
  if (!n) return ''
  return n === 1 ? '1 KI-Vorschlag wartet auf deine Freigabe.' : `${n} KI-Vorschläge warten auf deine Freigabe.`
}
