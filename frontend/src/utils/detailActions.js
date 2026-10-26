// Rezept-Detailseite mobil: BottomBar an der Stelle der Tab-Leiste (generische Leiste, 21.10.2026):
// Bearbeiten, Planen (Essensplan, 26.10.2026), Bewerten als Hauptaktion (orange), ganz rechts „Mehr“ mit
// „Rezept löschen…“ (destruktiv → nur im Menü)

const ACTIONS = [
  { id: 'edit', label: 'Bearbeiten', icon: 'bearbeiten', needsEdit: true },
  { id: 'plan', label: 'Planen', icon: 'planen', popup: 'dialog' },
  { id: 'rate', label: 'Bewerten', icon: 'stern', primary: true, popup: 'dialog' },
  { id: 'delete', label: 'Rezept löschen…', icon: 'loeschen', danger: true, popup: 'dialog', needsEdit: true },
]

// Ohne Bearbeitungsrecht (fremdes Rezept, kein Admin) bleiben „Planen“ und „Bewerten“ – und kein „Mehr“
export function detailActions(canEdit) {
  const allowed = ACTIONS.filter((a) => canEdit || !a.needsEdit)
  return {
    menu: allowed.filter((a) => a.danger),
    bar: allowed.filter((a) => !a.danger),
  }
}

// Routen, auf denen mobil (<1024 px) eine eigene Leiste die Tab-Leiste ersetzt; die Seitenleiste ab 1024 px bleibt
const TAB_BAR_REPLACED = ['recipe-new', 'recipe-edit', 'recipe-detail']

export function replacesTabBar(routeName) {
  return TAB_BAR_REPLACED.includes(routeName)
}

// Fokus-Falle in Dialogen: nächster Index im Kreis; außerhalb (−1) startet Tab vorne, Shift+Tab hinten
export function cycleFocus(count, current, backwards = false) {
  if (count <= 0) return -1
  if (current < 0) return backwards ? count - 1 : 0
  return (current + (backwards ? -1 : 1) + count) % count
}

// Rezeptformular: dieselbe Leisten-Optik für Abbrechen und Speichern bzw. Rezept erstellen (beim Speichern Sanduhr)
export function formActions(isEdit, saving = false) {
  const submitLabel = isEdit ? 'Speichern' : 'Rezept erstellen'
  return [
    { id: 'cancel', label: 'Abbrechen', icon: 'schliessen' },
    saving
      ? { id: 'submit', label: 'Wird gespeichert…', icon: 'sanduhr' }
      : { id: 'submit', label: submitLabel, icon: 'haken' },
  ]
}
