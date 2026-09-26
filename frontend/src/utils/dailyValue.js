// Referenzmengen für die Spalte "% Tagesbedarf" (pro Portion).
// Makros und Mikros: EU-Nährstoffbezugswerte (LMIV, Anhang XIII),
// Ballaststoffe: DGE-Richtwert. Natrium, Cholesterin und Omega-3 haben
// keine offizielle Referenzmenge und bekommen daher keinen Prozentwert.

export const MACRO_REFERENCES = {
  kcal: 2000,
  fat: 70,
  carbs: 260,
  sugar: 90,
  fiber: 30,
  protein: 50,
  salt: 6,
}

// BLS-Code → Referenzmenge mit eigener Einheit; wird auf die Einheit
// umgerechnet, die /api/nutrition/info für den Code liefert.
export const MICRO_REFERENCES = {
  VITA: { amount: 800, unit: 'µg' },
  VITD: { amount: 5, unit: 'µg' },
  VITE: { amount: 12, unit: 'mg' },
  VITK: { amount: 75, unit: 'µg' },
  THIA: { amount: 1.1, unit: 'mg' },
  RIBF: { amount: 1.4, unit: 'mg' },
  NIAEQ: { amount: 16, unit: 'mg' },
  VITB6: { amount: 1.4, unit: 'mg' },
  FOL: { amount: 200, unit: 'µg' },
  VITB12: { amount: 2.5, unit: 'µg' },
  VITC: { amount: 80, unit: 'mg' },
  CA: { amount: 800, unit: 'mg' },
  P: { amount: 700, unit: 'mg' },
  K: { amount: 2000, unit: 'mg' },
  MG: { amount: 375, unit: 'mg' },
  FE: { amount: 14, unit: 'mg' },
  ZN: { amount: 10, unit: 'mg' },
  ID: { amount: 150, unit: 'µg' },
}

const UNIT_IN_MICROGRAMS = {
  g: 1e6,
  mg: 1e3,
  µg: 1,
  μg: 1, // griechisches My statt Mikro-Zeichen
  ug: 1,
}

const percentOf = (value, reference) =>
  value == null || !reference ? null : (value / reference) * 100

export const macroDailyPercent = (key, value) => percentOf(value, MACRO_REFERENCES[key])

export const microDailyPercent = (code, value, unit) => {
  const ref = MICRO_REFERENCES[code]
  const from = UNIT_IN_MICROGRAMS[unit]
  const to = ref && UNIT_IN_MICROGRAMS[ref.unit]
  if (!ref || !from || !to) return null
  return percentOf(value, ref.amount * to / from)
}

export const formatPercent = (percent) =>
  percent == null ? '–' : `${Math.round(percent).toLocaleString('de-DE')} %`

export const percentLevel = (percent) => {
  if (percent == null) return 'none'
  if (percent >= 50) return 'high'
  if (percent >= 25) return 'medium'
  return 'low'
}
