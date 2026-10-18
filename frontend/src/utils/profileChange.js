// Profiländerung (Sicherheitspaket M1): neue E-Mail-Adresse und neues Passwort nur mit dem aktuellen Passwort.

export function sameEmail(a, b) {
  return (a || '').trim().toLowerCase() === (b || '').trim().toLowerCase()
}

export function emailChanged(user, email) {
  return !!(email || '').trim() && !sameEmail(user?.email, email)
}

export function needsCurrentPassword(user, form) {
  if (!user || user.ssoOnly) return false
  return emailChanged(user, form.email) || !!form.password
}

export function profilePayload(user, form) {
  const payload = {
    vorname: form.vorname || undefined,
    nachname: form.nachname || undefined,
  }
  if (!user?.ssoLinked && emailChanged(user, form.email)) payload.email = form.email.trim()
  if (!user?.ssoOnly && form.password) payload.password = form.password
  if (needsCurrentPassword(user, form)) payload.currentPassword = form.currentPassword || ''
  return payload
}

export function pendingEmailText(address) {
  return `Wir haben einen Bestätigungslink an ${address} geschickt. Bis du ihn öffnest, gilt deine bisherige Adresse.`
}
