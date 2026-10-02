import { ref, onMounted, onUnmounted } from 'vue'
import { createWakeLock } from '@/utils/wakeLock'

// Keeps the screen on while the calling view is mounted; leaving the route unmounts it and releases the lock.
export function useWakeLock() {
  const enabled = ref(true)
  const active = ref(false)
  // Kochmodus (Bildschirm bleibt an) = beschäftigt → kein automatisches Neuladen (update-check.js)
  const busyToken = {}
  const markBusy = (on) => {
    const busy = typeof window !== 'undefined' ? window.__appBusy : undefined
    if (busy) on ? busy.add(busyToken) : busy.delete(busyToken)
  }

  const wakeLock = createWakeLock({
    nav: typeof navigator !== 'undefined' ? navigator : undefined,
    doc: typeof document !== 'undefined' ? document : undefined,
    storage: typeof localStorage !== 'undefined' ? localStorage : undefined,
    onChange: (state) => {
      enabled.value = state.enabled
      active.value = state.active
      markBusy(state.active)
    },
  })
  enabled.value = wakeLock.enabled

  onMounted(() => wakeLock.start())
  onUnmounted(() => {
    wakeLock.stop()
    markBusy(false)
  })

  return {
    supported: wakeLock.supported,
    enabled,
    active,
    toggle: () => wakeLock.setEnabled(!enabled.value),
  }
}
