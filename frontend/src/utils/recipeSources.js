import { normalizeText } from './recipeSearch.js'

// Einheitliche Quelle für selbst ausgedachte Rezepte; wer es erfunden hat, steht im Autor
export const OWN_RECIPE = 'Eigenrezept'

const byName = (a, b) => a.localeCompare(b, 'de', { sensitivity: 'base' })

// Bekannte Quellen einmal je Schreibweise (Groß-/Kleinschreibung, Akzente egal), „Eigenrezept“ ist immer dabei.
// author ist nur gesetzt, wenn alle Rezepte der Quelle denselben Autor haben – dann darf die Auswahl ihn mit eintragen.
export const uniqueSources = (known) => {
  const entries = new Map()
  for (const { source, author } of [...(known ?? []), { source: OWN_RECIPE, author: null }]) {
    const name = String(source ?? '').trim()
    if (!name) continue
    const key = normalizeText(name)
    const entry = entries.get(key) ?? { source: name, authors: new Set() }
    if (author?.trim()) entry.authors.add(author.trim())
    entries.set(key, entry)
  }
  return [...entries.values()].map(({ source, authors }) => ({
    source,
    authors: [...authors],
    author: source !== OWN_RECIPE && authors.size === 1 ? [...authors][0] : null,
  }))
}

// Teilstring-Treffer ohne Rücksicht auf Groß-/Kleinschreibung und Akzente; Wortanfang und Quellen des eingetragenen
// Autors zuerst. Steht die Quelle schon genau so im Feld, wird sie nicht noch einmal angeboten.
export const sourceSuggestions = (known, query, author) => {
  const typed = String(query ?? '').trim()
  const needle = normalizeText(typed)
  const currentAuthor = normalizeText(author)
  const rank = (item) => {
    const name = normalizeText(item.source)
    return (needle && !name.startsWith(needle) ? 2 : 0)
      + (currentAuthor && !item.authors.some(a => normalizeText(a) === currentAuthor) ? 1 : 0)
  }
  return uniqueSources(known)
    .filter(item => normalizeText(item.source).includes(needle) && item.source !== typed)
    .sort((a, b) => rank(a) - rank(b) || byName(a.source, b.source))
}

export const authorSuggestions = (known, query) => {
  const typed = String(query ?? '').trim()
  const needle = normalizeText(typed)
  const authors = new Map()
  for (const { author } of known ?? []) {
    const name = String(author ?? '').trim()
    if (name && !authors.has(normalizeText(name))) authors.set(normalizeText(name), name)
  }
  return [...authors.values()]
    .filter(name => normalizeText(name).includes(needle) && name !== typed)
    .sort((a, b) => (normalizeText(a).startsWith(needle) ? 0 : 1) - (normalizeText(b).startsWith(needle) ? 0 : 1)
      || byName(a, b))
}

// Bücher eines Autors, um bei genau einem Treffer die Quelle mit einzutragen
export const sourcesOfAuthor = (known, author) =>
  uniqueSources(known).filter(item => item.source !== OWN_RECIPE
    && item.authors.some(a => normalizeText(a) === normalizeText(author)))
