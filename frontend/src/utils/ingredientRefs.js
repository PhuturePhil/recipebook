// Zutaten-Verweise in Zubereitungsschritten (Plan: workspace/plans/rezepte-zutaten-popover.md).
// Ein Schritt wird in Segmente zerlegt: reiner Text oder ein Treffer mit den Indizes der passenden Zutaten.
// Gerendert wird als Text + Buttons, nie als HTML (Schritte sind Nutzertext).

// Stufe 1: nur Admins. Freischaltung für alle = hier `true` zurückgeben (+ SharedRecipeView einbinden)
export const ingredientRefsVisible = (user) => user?.role === 'ADMIN'

export const NO_AMOUNT = { de: 'nach Bedarf', en: 'to taste' }

const DE_ENDINGS = ['', 'n', 'en', 'e', 's', 'es']
const EN_ENDINGS = ['', 's', 'es']
const MIN_STEM = 4
// Kurze Zutaten, die trotzdem zählen – nur als ganzes Wort bzw. am Ende eines Kompositums (Olivenöl)
const SHORT_WORDS = new Set(['ei', 'öl'])
const IRREGULAR = { eier: 'ei' }
const EN_MODIFIERS = new Set([
  'large', 'small', 'medium', 'fresh', 'freshly', 'ground', 'chopped', 'minced', 'unsalted', 'salted', 'heavy',
  'light', 'dark', 'whole', 'extra', 'virgin', 'plain', 'all', 'purpose', 'finely', 'roughly', 'grated', 'sliced',
  'diced', 'cold', 'warm', 'hot', 'room', 'temperature', 'dried', 'sweet', 'kosher', 'sea', 'fine', 'coarse', 'ripe',
  'raw', 'cooked', 'toasted', 'low', 'fat', 'free', 'organic', 'frozen', 'canned', 'softened', 'melted', 'peeled',
  'and', 'the', 'plus', 'more', 'about', 'optional', 'divided', 'packed', 'into', 'pieces', 'cut', 'thinly',
])

const WORD = /\p{L}+/gu
const SCORE = { phrase: 4, word: 3, stepCompound: 2, ingredientPart: 1 }

const lower = (s) => s.toLocaleLowerCase('de-DE')
const isCapitalized = (word) => word[0] !== word[0].toLocaleLowerCase('de-DE')

// „Schlagsahne (mind. 30 % Fett)“ → „Schlagsahne“; „Bergkäse oder Gruyère, gerieben“ → beide; „Butter für die Form“ → „Butter“
export function ingredientCores(name, lang = 'de') {
  const core = String(name ?? '')
    .replace(/\(.*?(\)|$)/g, ' ')
    .split(/[,;]/)[0]
    .split(lang === 'en' ? /\s(?:for|to)\s/i : /\s(?:für|zum|zur)\s/i)[0]
  return core
    .split(lang === 'en' ? /\s(?:or|and\/or)\s|\//i : /\s(?:oder|bzw\.?)\s|\//i)
    .map((s) => s.replace(/\s+/g, ' ').trim())
    .filter(Boolean)
}

function baseForms(word, endings) {
  const w = lower(word)
  const forms = new Set([w])
  if (IRREGULAR[w]) forms.add(IRREGULAR[w])
  const en = endings === EN_ENDINGS
  for (const e of endings) {
    if (e && w.endsWith(e) && w.length - e.length >= (en ? 3 : MIN_STEM)) forms.add(w.slice(0, -e.length))
  }
  if (en && w.endsWith('ies') && w.length > 5) forms.add(`${w.slice(0, -3)}y`)
  return [...forms]
}

function stemsOf(core, lang) {
  const words = core.match(WORD) ?? []
  if (lang === 'en') return words.filter((w) => w.length >= 3 && !EN_MODIFIERS.has(lower(w)))
  const nouns = words.filter(isCapitalized)
  const picked = nouns.length ? nouns : words.slice(-1)
  return picked.filter((w) => w.length >= MIN_STEM || SHORT_WORDS.has(lower(w)) || IRREGULAR[lower(w)])
}

function prepare(ingredients, lang) {
  const endings = lang === 'en' ? EN_ENDINGS : DE_ENDINGS
  return (ingredients ?? []).map((ingredient) => {
    const cores = ingredientCores(ingredient?.name, lang)
    return {
      phrases: cores.filter((c) => /\s/.test(c)).map(lower),
      words: cores.flatMap((c) => stemsOf(c, lang)).map((w) => ({ full: lower(w), bases: baseForms(w, endings) })),
    }
  })
}

const inflected = (token, base, endings) => endings.some((e) => token === base + e)

// Wie gut passt ein Wort des Schritts zu einem Wort der Zutat?
function wordScore(token, word, lang) {
  const endings = lang === 'en' ? EN_ENDINGS : DE_ENDINGS
  const short = (b) => b.length < (lang === 'en' ? 3 : MIN_STEM)
  if (word.bases.some((b) => (short(b) ? token === b : inflected(token, b, endings)))) return SCORE.word
  if (lang === 'en') return 0
  // Kompositum im Schritt: Kartoffel → Kartoffelscheiben
  if (word.bases.some((b) => !short(b) && token.length > b.length && token.startsWith(b))) return SCORE.stepCompound
  // Schrittwort ist Teil der Zutat: Käse → Bergkäse, Sahne → Schlagsahne, Muskat → Muskatnuss, Öl → Olivenöl
  const tokenForms = baseForms(token, endings)
  const partOf = (t) => word.full.length > t.length && (
    (t.length >= MIN_STEM && (word.full.startsWith(t) || word.full.endsWith(t))) ||
    (SHORT_WORDS.has(t) && word.full.endsWith(t))
  )
  return tokenForms.some(partOf) ? SCORE.ingredientPart : 0
}

function phraseCandidates(text, prepared) {
  const hay = lower(text)
  const found = []
  prepared.forEach((p, index) => {
    for (const phrase of p.phrases) {
      let from = 0
      let at
      while ((at = hay.indexOf(phrase, from)) !== -1) {
        const end = at + phrase.length
        const boundary = (i) => i < 0 || i >= hay.length || !/\p{L}/u.test(hay[i])
        if (boundary(at - 1) && boundary(end)) found.push({ start: at, end, score: SCORE.phrase, index })
        from = at + 1
      }
    }
  })
  return found
}

function wordCandidates(text, prepared, lang) {
  const found = []
  for (const match of text.matchAll(WORD)) {
    const raw = match[0]
    if (lang !== 'en' && !isCapitalized(raw)) continue
    const token = lower(raw)
    prepared.forEach((p, index) => {
      const score = Math.max(0, ...p.words.map((w) => wordScore(token, w, lang)))
      if (score) found.push({ start: match.index, end: match.index + raw.length, score, index })
    })
  }
  return found
}

// Gleiche Stelle, mehrere Zutaten: nur die mit der besten Bewertung (Dubletten in zwei Gruppen → beide)
function mergeSpans(candidates) {
  const spans = new Map()
  for (const c of candidates) {
    const key = `${c.start}:${c.end}`
    const span = spans.get(key)
    if (!span || c.score > span.score) spans.set(key, { start: c.start, end: c.end, score: c.score, refs: [c.index] })
    else if (c.score === span.score && !span.refs.includes(c.index)) span.refs.push(c.index)
  }
  return [...spans.values()]
}

// Längster Treffer gewinnt, keine Verschachtelung
function pickSpans(spans) {
  const sorted = [...spans].sort((a, b) => (b.end - b.start) - (a.end - a.start) || b.score - a.score || a.start - b.start)
  const taken = []
  for (const s of sorted) {
    if (!taken.some((t) => s.start < t.end && t.start < s.end)) taken.push(s)
  }
  return taken.sort((a, b) => a.start - b.start)
}

export function createIngredientMatcher(ingredients, lang = 'de') {
  const language = lang === 'en' ? 'en' : 'de'
  const prepared = prepare(ingredients, language)
  return (text) => {
    const step = String(text ?? '')
    if (!step || !prepared.length) return step ? [{ text: step }] : []
    const spans = pickSpans(mergeSpans([...phraseCandidates(step, prepared), ...wordCandidates(step, prepared, language)]))
    const segments = []
    let pos = 0
    for (const s of spans) {
      if (s.start > pos) segments.push({ text: step.slice(pos, s.start) })
      segments.push({ text: step.slice(s.start, s.end), refs: s.refs.sort((a, b) => a - b) })
      pos = s.end
    }
    if (pos < step.length) segments.push({ text: step.slice(pos) })
    return segments
  }
}

export const segmentStep = (text, ingredients, lang) => createIngredientMatcher(ingredients, lang)(text)

// Popover-Zeilen aus den skalierten Zutaten (Personen-Regler zählt); Mengen sind Text und bleiben, wie sie sind
export function refLines(refs, scaledIngredients, lang = 'de') {
  const rows = (refs ?? []).map((i) => scaledIngredients?.[i]).filter(Boolean)
  const withGroup = rows.length > 1 && new Set(rows.map((r) => r.name)).size < rows.length
  return rows.map((r) => {
    const amount = [r.amount, r.unit].map((v) => String(v ?? '').trim()).filter(Boolean).join(' ')
    return {
      amount: amount || NO_AMOUNT[lang === 'en' ? 'en' : 'de'],
      hasAmount: Boolean(amount),
      name: r.name,
      group: withGroup ? (r.groupName || null) : null,
    }
  })
}

export const refLabel = (segment) => `Menge anzeigen: ${segment.text}`

// Unter dem Wort, außer unten ist kein Platz; seitlich immer ganz im Bild
export function popoverPlacement({ anchor, width, height, viewportWidth, viewportHeight, gap = 6, margin = 8 }) {
  const below = viewportHeight - anchor.bottom - gap - margin
  const above = anchor.top - gap - margin
  const up = height > below && above > below
  const top = up ? anchor.top - gap - height : anchor.bottom + gap
  const centered = anchor.left + (anchor.right - anchor.left) / 2 - width / 2
  const left = Math.max(margin, Math.min(centered, viewportWidth - width - margin))
  return { top: Math.round(Math.max(margin, top)), left: Math.round(left), up }
}

// Höchstens ein Popover offen. Per Hover geöffnet → schließt beim Verlassen; per Tap/Klick oder Tastatur → schließt
// bei erneutem Tap, beim Wegfokussieren, Esc, Scrollen oder Tap daneben (die letzten drei: Komponente)
export function createPopoverState() {
  let open = null
  const listeners = new Set()
  const set = (next) => {
    open = next
    listeners.forEach((fn) => fn(open))
  }
  return {
    get open() { return open },
    isOpen: (key) => open?.key === key,
    subscribe(fn) {
      listeners.add(fn)
      return () => listeners.delete(fn)
    },
    click(key) {
      if (open?.key === key && open.by === 'click') set(null)
      else set({ key, by: 'click' })
    },
    hover(key) {
      if (open?.key !== key) set({ key, by: 'hover' })
    },
    leave(key) {
      if (open?.key === key && open.by === 'hover') set(null)
    },
    focus(key) {
      if (open?.key !== key) set({ key, by: 'focus' })
    },
    blur(key) {
      if (open?.key === key && open.by !== 'hover') set(null)
    },
    close() {
      if (open) set(null)
    },
  }
}
