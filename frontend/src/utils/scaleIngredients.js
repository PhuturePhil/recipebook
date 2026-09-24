export function scaleIngredients(ingredients, baseServings, servings) {
  if (!ingredients) return []

  return ingredients.map(ingredient => {
    const amount = parseFloat(ingredient.amount)
    if (isNaN(amount)) {
      return ingredient
    }
    const scaledAmount = (amount / baseServings) * servings
    const formattedAmount = scaledAmount % 1 === 0
      ? scaledAmount
      : scaledAmount.toFixed(1).replace('.0', '')
    return {
      ...ingredient,
      amount: formattedAmount,
    }
  })
}
