export const emptyIngredient = () => ({ name: '', amount: '', unit: '' })

const blank = (value) => !String(value ?? '').trim()

export const isBlankIngredient = (ingredient) =>
  blank(ingredient?.name) && blank(ingredient?.amount) && blank(ingredient?.unit)

export function cleanRecipeData(formData) {
  return {
    ...formData,
    ingredients: formData.ingredients.filter((i) => !isBlankIngredient(i)),
    instructions: formData.instructions.filter((i) => !blank(i))
  }
}

export function isIngredientNameRequired(ingredients, index) {
  const ingredient = ingredients[index]
  if (!isBlankIngredient(ingredient)) return true
  return ingredients.every(isBlankIngredient) && index === 0
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
