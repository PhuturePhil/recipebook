import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUiStore } from '@/stores/uiStore'
import changelog from '@/data/changelog.json'
import '@/utils/update-check.js'

// Seiten mit ungespeicherten Eingaben bzw. laufendem Login: dort nie automatisch neu laden
const BUSY_ROUTES = ['recipe-edit', 'recipe-new', 'oidc-callback', 'reset-password', 'invite']

// Auto-Update + „Was ist neu“: Logik aus utils/update-check.js (Master: workspace/snippets/update-check.js),
// nur das Popup rendert Vue (WhatsNewModal). Referenz ist die ins Bundle eingebaute Version; weicht
// /changelog.json davon ab, lädt die App still neu, sobald nichts mehr läuft. Weitere Busy-Quellen
// melden sich über window.__appBusy (Kochmodus/Wake Lock, Übersetzung).
export function useUpdateCheck() {
  const route = useRoute()
  const uiStore = useUiStore()
  const popup = ref(null)

  window.initUpdateCheck({
    versionUrl: '/changelog.json',
    changelogUrl: '/changelog.json',
    builtinVersion: changelog[0].version,
    isBusy: () => BUSY_ROUTES.includes(route.name) || uiStore.loadingActive,
    openPopup: (title, entries, onClose) => { popup.value = { title, entries, onClose } },
  })

  const close = () => {
    popup.value?.onClose?.()
    popup.value = null
  }
  return { popup, close }
}
