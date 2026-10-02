import { ref } from 'vue'

// Hell/Dunkel nach app-shell-Spezifikation (Master: workspace/snippets/app-shell.js):
// localStorage 'theme' = auto|light|dark, Auto folgt dem System. Erstes Setzen passiert schon
// im Inline-Skript in index.html (kein Aufblitzen), hier nur Umschalten + Live-Wechsel.
const LS_THEME = 'theme'
const mq = typeof window !== 'undefined' && window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null

function readPref() {
  let t = null
  try { t = localStorage.getItem(LS_THEME) } catch { /* privater Modus */ }
  return t === 'light' || t === 'dark' ? t : 'auto'
}

const pref = ref(readPref())
const dark = ref(false)

function apply() {
  pref.value = readPref()
  dark.value = pref.value === 'dark' || (pref.value === 'auto' && !!mq?.matches)
  const root = document.documentElement
  root.setAttribute('data-theme', dark.value ? 'dark' : 'light')
  root.style.background = ''
  const color = dark.value ? '#14161D' : '#F2F2F7'
  document.querySelectorAll('meta[name="theme-color"]').forEach((m) => m.setAttribute('content', color))
}

export function setTheme(t) {
  try { localStorage.setItem(LS_THEME, t === 'light' || t === 'dark' ? t : 'auto') } catch { /* privater Modus */ }
  apply()
}

if (typeof window !== 'undefined') {
  apply()
  mq?.addEventListener?.('change', () => { if (readPref() === 'auto') apply() })
  window.addEventListener('storage', (e) => { if (e.key === LS_THEME) apply() })
}

export function useTheme() {
  return { pref, dark, setTheme }
}
