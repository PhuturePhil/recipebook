// Quick keys ½ ¼ ¾ next to the Menge field: they add a fraction to a whole number or replace the fraction already there

export const FRACTION_KEYS = [
  { label: '½', value: '1/2' },
  { label: '¼', value: '1/4' },
  { label: '¾', value: '3/4' }
]

const TRAILING_FRACTION = /(\s*)(?:\d+\s*\/\s*\d+|[½¼¾⅓⅔⅛⅕])$/
const TRAILING_DECIMAL = /(\d+)[.,]\d+$/
const LONE_ZERO = /(^|[^\d.,])0$/

export function applyFraction(amount, fraction) {
  let rest = String(amount ?? '').trimEnd()
  const gap = rest.match(TRAILING_FRACTION)?.[1] ?? ''
  rest = rest.replace(TRAILING_FRACTION, '').replace(TRAILING_DECIMAL, '$1').replace(LONE_ZERO, '$1')
  if (!rest.trim()) return fraction
  if (/[-–]\s*$/.test(rest)) return `${rest.trimEnd()}${gap ? ' ' : ''}${fraction}`
  return `${rest.trimEnd()} ${fraction}`
}
