import { ref, onMounted, onUnmounted } from 'vue'

// NEU-Badge für „Neuerungen“: neue Version bekannt (update-check.js) und noch nicht gesehen
export function useNewBadge() {
  const neu = ref(false)
  const calc = () => {
    const st = window.__ucState ? window.__ucState() : { seen: null, current: null }
    neu.value = !!(st.current && st.seen && st.current !== st.seen)
  }
  const onStorage = (e) => { if (e.key === 'uc:lastSeen') calc() }
  onMounted(() => {
    calc()
    window.addEventListener('uc:seen', calc)
    window.addEventListener('uc:version', calc)
    window.addEventListener('storage', onStorage)
  })
  onUnmounted(() => {
    window.removeEventListener('uc:seen', calc)
    window.removeEventListener('uc:version', calc)
    window.removeEventListener('storage', onStorage)
  })
  return neu
}
