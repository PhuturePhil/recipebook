// Generische untere Leiste (Philipp-Vorgabe 21.10.2026, gilt app-weit): alle Einträge gleich groß, höchstens
// vier nebeneinander. Was nicht passt und alles Destruktive kommt ins Überlauf-Menü „Mehr“ ganz rechts.

export const BOTTOM_BAR_MAX = 4

export function bottomBarLayout(items = [], menu = [], max = BOTTOM_BAR_MAX) {
  let bar = items.filter((i) => !i.danger)
  let spill = []
  const extra = [...menu, ...items.filter((i) => i.danger)]
  if (bar.length + (extra.length ? 1 : 0) > max) {
    spill = bar.slice(max - 1)
    bar = bar.slice(0, max - 1)
  }
  const overflow = [...spill, ...extra]
  return {
    bar,
    menu: [...overflow.filter((i) => !i.danger), ...overflow.filter((i) => i.danger)],
  }
}
