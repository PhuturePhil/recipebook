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

// The form keeps ingredients in sections: index 0 holds the ingredients without a group (always there, maybe empty),
// every further section is one group card. key only identifies a section in the template and is never saved.
let sectionCount = 0
export const sectionKey = () => `s${++sectionCount}`

export const ingredientSection = (name = null, items = []) => ({ key: sectionKey(), name, items })

// API → form: all ingredients without a group move up into section 0, groups follow in their original order and
// a group that ends up next to one with the same name is merged into it
export function ingredientsToSections(ingredients) {
  const sections = [ingredientSection()]
  for (const ingredient of ingredients ?? []) {
    const { groupName, ...item } = ingredient
    const group = groupOf(groupName)
    const last = sections[sections.length - 1]
    if (!group) sections[0].items.push(item)
    else if (sections.length > 1 && last.name === group) last.items.push(item)
    else sections.push(ingredientSection(group, [item]))
  }
  return sections
}

// Form → API: one flat list in section order; a card with a blank name counts as "no group"
export function sectionsToIngredients(sections) {
  const ingredients = []
  for (const section of sections ?? []) {
    const group = groupOf(section.name)
    for (const { groupName, ...item } of section.items ?? []) {
      ingredients.push(group ? { ...item, groupName: group } : item)
    }
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
