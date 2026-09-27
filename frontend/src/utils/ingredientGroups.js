// Ingredient groups ("Salsa", "Für den Teig"): the API stores a groupName per ingredient, consecutive rows with the
// same name form one group. The form works on a flat list of rows where a heading row { group: 'Salsa' } applies to
// all ingredient rows below it up to the next heading; a heading with an empty name means "no group".

export const MAX_GROUP_NAME = 100

export const groupRow = (name = '') => ({ group: name })

export const isGroupRow = (row) => typeof row?.group === 'string'

const groupOf = (value) => String(value ?? '').trim() || null

export function ingredientsToRows(ingredients) {
  const rows = []
  let current = null
  for (const ingredient of ingredients ?? []) {
    const { groupName, ...row } = ingredient
    const group = groupOf(groupName)
    if (group !== current) {
      rows.push(groupRow(group ?? ''))
      current = group
    }
    rows.push(row)
  }
  return rows
}

export function rowsToIngredients(rows) {
  const ingredients = []
  let group = null
  for (const row of rows ?? []) {
    if (isGroupRow(row)) {
      group = groupOf(row.group)
      continue
    }
    const { groupName, ...ingredient } = row
    ingredients.push(group ? { ...ingredient, groupName: group } : ingredient)
  }
  return ingredients
}

// Group that applies at a row: the nearest heading above it ('' when there is none)
export function groupAt(rows, index) {
  for (let i = Math.min(index, rows.length - 1); i >= 0; i--) {
    if (isGroupRow(rows[i])) return rows[i].group
  }
  return ''
}

// Consecutive ingredients with the same group, for display: [{ group: 'Salsa' | null, items: [...] }]
export function groupSections(ingredients) {
  const sections = []
  for (const ingredient of ingredients ?? []) {
    const group = groupOf(ingredient.groupName)
    const last = sections[sections.length - 1]
    if (last && last.group === group) last.items.push(ingredient)
    else sections.push({ group, items: [ingredient] })
  }
  return sections
}

export const hasGroups = (ingredients) => (ingredients ?? []).some((i) => groupOf(i.groupName))

const MARKDOWN_HEADING = /^#{1,6}\s*(.+?)\s*#*$/
const EMPHASIS = /^(\*\*|__|\*|_)(.+?)\1$/
const COLON = /:$/
const LIST_TITLE = /^(zutaten|ingredients)\b/i
const MAX_HEADING_WORDS = 8

// "Salsa:", "Für die Salsa:", "### Salsa", "**Salsa**" → 'Salsa' / 'Für die Salsa'. A plain title line like
// "Zutaten für 4 Personen:" gives '' (drop the line), everything else null (not a heading).
// startsWithAmount keeps "1 TL Salz:" an ingredient.
export function parseGroupHeading(line, startsWithAmount = () => false) {
  let text = String(line ?? '').trim()
  let marked = false
  const heading = text.match(MARKDOWN_HEADING)
  if (heading && text.startsWith('#')) {
    text = heading[1]
    marked = true
  }
  const emphasis = text.match(EMPHASIS)
  if (emphasis) {
    text = emphasis[2].trim()
    marked = true
  }
  if (COLON.test(text)) {
    text = text.replace(/\s*:+$/, '').trim()
    marked = true
  }
  text = text.replace(/^(\*\*|__)|(\*\*|__)$/g, '').trim()
  if (!marked || !text || startsWithAmount(text)) return null
  if (text.split(/\s+/).length > MAX_HEADING_WORDS || text.length > MAX_GROUP_NAME) return null
  return LIST_TITLE.test(text) ? '' : text
}
