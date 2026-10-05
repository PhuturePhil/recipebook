import { ingredientSection, isGroupRow, sectionsToIngredients } from './ingredientGroups.js'
import { sourceUrlProblem } from './sourceUrl.js'

export const emptyIngredient = () => ({ name: '', amount: '', unit: '' })

const blank = (value) => !String(value ?? '').trim()

// Group headings are never blank ingredient rows
export const isBlankIngredient = (ingredient) =>
  !isGroupRow(ingredient) && blank(ingredient?.name) && blank(ingredient?.amount) && blank(ingredient?.unit)

// The API knows no sections: they become one flat list with a groupName per ingredient
export function cleanRecipeData(formData) {
  const { sections, ...data } = formData
  return {
    ...data,
    ingredients: sectionsToIngredients(sections).filter((i) => !isBlankIngredient(i)),
    instructions: formData.instructions.filter((i) => !blank(i))
  }
}

const allItems = (sections) => (sections ?? []).flatMap((section) => section.items)

export const ingredientCount = (sections) => allItems(sections).length

// A started row needs a name; in an entirely empty list the very first row stands for "at least one ingredient"
export function isIngredientNameRequired(sections, sectionIndex, itemIndex) {
  const ingredient = sections[sectionIndex]?.items[itemIndex]
  if (!ingredient) return false
  if (!isBlankIngredient(ingredient)) return true
  const items = allItems(sections)
  return items.every(isBlankIngredient) && ingredient === items[0]
}

// Rows with amount or unit but without a name, as [sectionIndex, itemIndex]
export function ingredientsWithoutName(sections) {
  const missing = []
  ;(sections ?? []).forEach((section, s) => section.items.forEach((item, i) => {
    if (!isBlankIngredient(item) && blank(item.name)) missing.push([s, i])
  }))
  return missing
}

export function isInstructionRequired(instructions, index) {
  if (!blank(instructions[index])) return true
  return instructions.every(blank) && index === 0
}

// Enter in an ingredient row opens a row below; an already empty next row is reused
export function addIngredientBelow(ingredients, index) {
  const next = index + 1
  if (next < ingredients.length && isBlankIngredient(ingredients[next])) return next
  ingredients.splice(next, 0, emptyIngredient())
  return next
}

// "+ Gruppe": a new card at the end with one empty row; returns its index
export function addSection(sections) {
  sections.push(ingredientSection('', [emptyIngredient()]))
  return sections.length - 1
}

// A section only holding empty rows can go without asking
export const isSectionEmpty = (section) => section.items.every(isBlankIngredient)

// Removing a card keeps its ingredients: they go to the end of the section above (the first card hands them to
// "no group"); its empty rows are dropped. Returns the index of the receiving section.
export function dissolveSection(sections, index) {
  if (index < 1 || index >= sections.length) return -1
  const [section] = sections.splice(index, 1)
  sections[index - 1].items.push(...section.items.filter((item) => !isBlankIngredient(item)))
  ensureIngredientRow(sections)
  return index - 1
}

// The form always shows at least one ingredient row
export function ensureIngredientRow(sections) {
  if (!ingredientCount(sections)) sections[0].items.push(emptyIngredient())
}

// The last remaining row stays (it is the only place left to type)
export function removeIngredientAt(sections, sectionIndex, itemIndex) {
  if (ingredientCount(sections) <= 1) return false
  sections[sectionIndex].items.splice(itemIndex, 1)
  return true
}

// Moves an ingredient one step (-1 up, +1 down); at the edge of its section it changes into the neighbouring one
// (end of the section above, start of the one below). Returns the new [sectionIndex, itemIndex] or null.
export function moveIngredientAcross(sections, sectionIndex, itemIndex, delta) {
  const items = sections[sectionIndex]?.items
  if (!items || itemIndex < 0 || itemIndex >= items.length) return null
  const target = itemIndex + delta
  if (target >= 0 && target < items.length) {
    moveRow(items, itemIndex, delta)
    return [sectionIndex, target]
  }
  const next = sectionIndex + delta
  if (next < 0 || next >= sections.length) return null
  const [item] = items.splice(itemIndex, 1)
  if (delta < 0) {
    sections[next].items.push(item)
    return [next, sections[next].items.length - 1]
  }
  sections[next].items.unshift(item)
  return [next, 0]
}

// Group cards move among themselves; section 0 ("no group") stays on top. Returns the new index or -1.
export function moveSection(sections, index, delta) {
  const target = index + delta
  if (index < 1 || target < 1 || index >= sections.length || target >= sections.length) return -1
  return moveRow(sections, index, delta)
}

// Moves a row one step up (-1) or down (+1); returns its new index, or -1 at the edge
export function moveRow(rows, index, delta) {
  const target = index + delta
  if (index < 0 || index >= rows.length || target < 0 || target >= rows.length) return -1
  const [row] = rows.splice(index, 1)
  rows.splice(target, 0, row)
  return target
}

export function preventsImplicitSubmit(event) {
  if (event.key !== 'Enter' || event.isComposing) return false
  const target = event.target
  return target?.tagName === 'INPUT' && !['submit', 'button', 'file', 'checkbox', 'radio'].includes(target.type)
}

export const formSnapshot = (formData) => JSON.stringify(cleanRecipeData(formData))

export const MAX_SERVINGS = 100
export const MAX_PREP_TIME_MINUTES = 10080

const isEmptyNumber = (value) => value === null || value === undefined || String(value).trim() === ''
const wholeNumberIn = (value, min, max) => Number.isInteger(Number(value)) && Number(value) >= min && Number(value) <= max

// Checks before saving (the form has novalidate): field → message, empty object when everything is fine.
// Required are only title, servings "Von", one ingredient and one step.
export function validateRecipeForm(formData) {
  const errors = {}
  if (blank(formData.title)) errors.title = 'Bitte einen Titel eingeben.'

  const from = formData.baseServings
  if (isEmptyNumber(from)) errors.baseServings = 'Bitte die Personenanzahl eingeben.'
  else if (!wholeNumberIn(from, 1, MAX_SERVINGS)) errors.baseServings = `Bitte eine ganze Zahl von 1 bis ${MAX_SERVINGS} eingeben.`

  const to = formData.servingsTo
  if (!isEmptyNumber(to)) {
    const min = errors.baseServings ? 1 : Number(from)
    if (!wholeNumberIn(to, min, MAX_SERVINGS)) errors.servingsTo = `„Bis“ muss zwischen ${min} und ${MAX_SERVINGS} liegen.`
  }

  const prep = formData.prepTimeMinutes
  if (!isEmptyNumber(prep) && !wholeNumberIn(prep, 1, MAX_PREP_TIME_MINUTES)) {
    errors.prepTimeMinutes = `Bitte Minuten von 1 bis ${MAX_PREP_TIME_MINUTES} eingeben.`
  }

  const urlProblem = sourceUrlProblem(formData.sourceUrl)
  if (urlProblem) errors.sourceUrl = urlProblem

  const items = allItems(formData.sections)
  if (!items.some((item) => !blank(item.name))) errors.ingredients = 'Mindestens eine Zutat angeben.'
  else if (ingredientsWithoutName(formData.sections).length) errors.ingredients = 'Bitte bei jeder Zutat einen Namen eintragen.'

  if (!(formData.instructions ?? []).some((step) => !blank(step))) {
    errors.instructions = 'Mindestens einen Arbeitsschritt angeben.'
  }
  return errors
}

// Order of the fields on the page, for jumping to the first error
export const ERROR_ORDER = ['title', 'baseServings', 'servingsTo', 'prepTimeMinutes', 'sourceUrl', 'ingredients', 'instructions']

export const firstError = (errors) => ERROR_ORDER.find((field) => errors[field]) ?? null

export async function runSave(state, action) {
  state.saving = true
  state.error = ''
  try {
    return { ok: true, result: await action() }
  } catch (error) {
    state.error = error?.message || 'Unbekannter Fehler'
    return { ok: false, error }
  } finally {
    state.saving = false
  }
}

export const LANGUAGE_NAMES = { de: 'Deutsch', en: 'Englisch' }

// Dropdown value: 'auto' while the backend detects the language on save, otherwise the language set by hand
export const languageChoice = (formData) => (formData.languageAuto === false ? formData.language : 'auto')

export function applyLanguageChoice(formData, choice) {
  if (choice === 'auto') {
    formData.languageAuto = true
  } else {
    formData.language = choice
    formData.languageAuto = false
  }
}

export function autoLanguageLabel(detected) {
  return LANGUAGE_NAMES[detected] ? `Automatisch (erkannt: ${LANGUAGE_NAMES[detected]})` : 'Automatisch erkennen'
}
