// Rezept-Detailseite mobil: Aktionsleiste an der Stelle der Tab-Leiste (TabBar-Optik, nur Aktionen, kein Aktiv-Zustand).
// Daumenfreundlich von links nach rechts: Löschen dezent (rot erst bei Hover/Druck), Bearbeiten sekundär,
// Bewerten als Hauptaktion rechts (gefüllt orange).

const ACTIONS = [
  { id: 'delete', label: 'Löschen', icon: 'loeschen', tone: 'dezent', danger: true, needsEdit: true },
  { id: 'edit', label: 'Bearbeiten', icon: 'bearbeiten', needsEdit: true },
  { id: 'rate', label: 'Bewerten', icon: 'stern', tone: 'primaer' },
]

// Ohne Bearbeitungsrecht (fremdes Rezept, kein Admin) bleibt nur „Bewerten“
export function detailActions(canEdit) {
  return ACTIONS.filter((a) => canEdit || !a.needsEdit)
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
