// Rezept löschen: Schutz vor Versehen (kein Passwort) – der Dialog zeigt ein kurzes Küchenwort, das man abtippen muss.
// Groß-/Kleinschreibung und Leerzeichen am Rand zählen nicht.

export const DELETE_WORDS = ['Salz', 'Sieb', 'Ofen', 'Topf', 'Quark', 'Zimt', 'Mehl', 'Reis', 'Senf', 'Dill', 'Kohl', 'Brot']

const normalize = (text) => String(text ?? '').trim().toLocaleLowerCase('de-DE')

// Zufälliges Wort, nie zweimal hintereinander dasselbe
export function pickDeleteWord(previous = null, random = Math.random) {
  const pool = DELETE_WORDS.filter((w) => w !== previous)
  return pool[Math.min(pool.length - 1, Math.floor(random() * pool.length))]
}

export function matchesDeleteWord(input, word) {
  return !!word && normalize(input) === normalize(word)
}
