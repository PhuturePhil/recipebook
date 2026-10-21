// Rezept-Detailseite mobil: fixe Leiste an der Stelle der Tab-Leiste (Hero-Design, abgenommen 21.10.2026).
// Links ein ···-Kreis mit den seltenen Aktionen (Menü klappt darüber auf), dann Bearbeiten (Umriss)
// und Bewerten als Hauptaktion rechts (orange Pille).

const ACTIONS = [
  { id: 'delete', label: 'Rezept löschen…', icon: 'loeschen', place: 'menu', danger: true, needsEdit: true },
  { id: 'edit', label: 'Bearbeiten', icon: 'bearbeiten', place: 'bar', tone: 'ghost', needsEdit: true },
  { id: 'rate', label: 'Bewerten', icon: 'sternLinie', place: 'bar', tone: 'primaer' },
]

// Ohne Bearbeitungsrecht (fremdes Rezept, kein Admin) bleibt nur „Bewerten“ – und kein ···-Menü
export function detailActions(canEdit) {
  const allowed = ACTIONS.filter((a) => canEdit || !a.needsEdit)
  return {
    menu: allowed.filter((a) => a.place === 'menu'),
    bar: allowed.filter((a) => a.place === 'bar'),
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
