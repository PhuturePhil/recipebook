// A dropdown opens below its field unless the free room there (above keyboard and sticky button bar) is too small
export const DROPDOWN_MAX_HEIGHT = 200
export const DROPDOWN_MIN_HEIGHT = 120

export function dropdownPlacement({ fieldTop, fieldBottom, topLimit, bottomLimit }) {
  const below = Math.max(0, bottomLimit - fieldBottom)
  const above = Math.max(0, fieldTop - topLimit)
  const up = below < DROPDOWN_MIN_HEIGHT && above > below
  return { up, maxHeight: Math.min(DROPDOWN_MAX_HEIGHT, Math.max(up ? above : below, DROPDOWN_MIN_HEIGHT / 2)) }
}
