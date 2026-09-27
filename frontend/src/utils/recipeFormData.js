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
