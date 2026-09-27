import { ref, onMounted, onUnmounted } from 'vue'
import { createWakeLock } from '@/utils/wakeLock'

// Keeps the screen on while the calling view is mounted; leaving the route unmounts it and releases the lock.
export function useWakeLock() {
  const enabled = ref(true)
  const active = ref(false)

  const wakeLock = createWakeLock({
    nav: typeof navigator !== 'undefined' ? navigator : undefined,
    doc: typeof document !== 'undefined' ? document : undefined,
    storage: typeof localStorage !== 'undefined' ? localStorage : undefined,
    onChange: (state) => {
      enabled.value = state.enabled
      active.value = state.active
    },
  })
  enabled.value = wakeLock.enabled

  onMounted(() => wakeLock.start())
  onUnmounted(() => wakeLock.stop())

  return {
    supported: wakeLock.supported,
    enabled,
    active,
    toggle: () => wakeLock.setEnabled(!enabled.value),
  }
}
