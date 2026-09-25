const UNICODE_FRACTIONS = { '½': ' 1/2', '¼': ' 1/4', '¾': ' 3/4', '⅓': ' 1/3', '⅔': ' 2/3', '⅛': ' 1/8', '⅕': ' 1/5' }

const DISPLAY_FRACTIONS = [
  [1 / 8, '⅛'], [1 / 5, '⅕'], [1 / 4, '¼'], [1 / 3, '⅓'], [1 / 2, '½'], [2 / 3, '⅔'], [3 / 4, '¾'],
]

const NUMBER = '\\d+\\/\\d+|\\d+(?:\\.\\d+)?(?:\\s+\\d+\\/\\d+)?'
const AMOUNT = new RegExp(
  `^((?:ca\\.?|circa|etwa|ungefähr)\\s*)?(${NUMBER})(?:\\s*(?:-|bis)\\s*(${NUMBER}))?\\s*(.*)$`,
  'i'
)

// Mirrors QuantityParser.clean in the backend so both sides read amounts the same way.
function clean(raw) {
  let s = ''
  for (const c of String(raw ?? '')) {
    if (UNICODE_FRACTIONS[c]) s += UNICODE_FRACTIONS[c]
    else if ('–—‒−'.includes(c)) s += '-'
    else s += c
  }
  return s
    .replace(/(\d),(\d)/g, '$1.$2')
    .replace(/(\d)\s*\/\s*(\d)/g, '$1/$2')
    .replace(/\s+/g, ' ')
    .trim()
}

function toNumber(text) {
  const parts = text.trim().split(/\s+/)
  if (parts.length === 2) {
    const whole = toNumber(parts[0])
    const frac = toNumber(parts[1])
    return whole == null || frac == null ? null : whole + frac
  }
  if (text.includes('/')) {
    const [n, d] = text.split('/').map(Number)
    return d === 0 ? null : n / d
  }
  return Number(text)
}

export function parseAmount(raw) {
  const match = clean(raw).match(AMOUNT)
  if (!match) return null
  const min = toNumber(match[2])
  const max = match[3] != null ? toNumber(match[3]) : min
  if (min == null || max == null || Number.isNaN(min) || Number.isNaN(max)) return null
  return { prefix: (match[1] ?? '').trim(), min, max, rest: match[4].trim() }
}

export function formatNumber(value) {
  if (value < 1) {
    const fraction = DISPLAY_FRACTIONS.find(([f]) => Math.abs(value - f) < 0.02)
    if (fraction) return fraction[1]
  }
  const digits = value >= 100 ? 0 : value >= 10 ? 1 : 2
  return value.toLocaleString('de-DE', { maximumFractionDigits: digits, useGrouping: false })
}

// Ranges scale at both ends, so their midpoint stays the amount the nutrition backend calculates with.
export function formatAmount(parsed, factor = 1) {
  const min = formatNumber(parsed.min * factor)
  const max = formatNumber(parsed.max * factor)
  const amount = min === max ? min : `${min}–${max}`
  return [parsed.prefix, amount, parsed.rest].filter(Boolean).join(' ')
}

export function scaleIngredients(ingredients, baseServings, servings) {
  if (!ingredients) return []
  const factor = baseServings > 0 && servings > 0 ? servings / baseServings : 1

  return ingredients.map(ingredient => {
    const parsed = parseAmount(ingredient.amount)
    if (!parsed) {
      return ingredient
    }
    return {
      ...ingredient,
      amount: formatAmount(parsed, factor),
    }
  })
}
