// Kontakt für Impressum, Datenschutz und Meldungen (Beschluss 17.10.2026: nur Name + E-Mail)
export const OPERATOR_NAME = 'Philipp Pastoors'
export const CONTACT_EMAIL = 'pastoors.cloud@mail.de'

// Melde-Link (Notice-and-Takedown) als vorbefüllte Mail – kein eigenes Ticket-System
export function reportMailto({ recipeId = null, title = '', url = '' } = {}) {
  const name = title ? ` „${title}“` : ''
  const subject = recipeId != null ? `Meldung: Rezept ${recipeId}${name}` : `Meldung: geteiltes Rezept${name}`
  const lines = [
    'Hallo,',
    '',
    'ich möchte diesen Inhalt melden:',
    url,
    ...(recipeId != null ? [`Rezept-ID: ${recipeId}`] : []),
    '',
    'Grund (z. B. Urheberrecht, rechtswidriger oder beleidigender Inhalt):',
    '',
  ]
  return `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(lines.join('\n'))}`
}

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
export const MIN_PASSWORD = 8

// Gleiche Regeln wie das Backend (SignupService); die Meldungen des Servers bleiben maßgeblich
export function signupError({ vorname, nachname, email, password, passwordConfirm }) {
  if (!vorname?.trim()) return 'Bitte gib deinen Vornamen an.'
  if (!nachname?.trim()) return 'Bitte gib deinen Nachnamen an.'
  if (!EMAIL.test(email?.trim() ?? '')) return 'Bitte gib eine gültige E-Mail-Adresse an.'
  if ((password ?? '').length < MIN_PASSWORD) return `Das Passwort muss mindestens ${MIN_PASSWORD} Zeichen lang sein.`
  if (password !== passwordConfirm) return 'Die Passwörter stimmen nicht überein.'
  return null
}
