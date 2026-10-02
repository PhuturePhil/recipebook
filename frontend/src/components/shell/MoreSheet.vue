<template>
  <!-- „Mehr“-Sheet (nur mobil, ab 1024 px blendet app-shell.css es aus) — Spezifikation: workspace/snippets/app-shell.js -->
  <div ref="bd" class="shell-sheet-backdrop shell-ui" @click.self="emit('close')" @keydown="onKeyDown">
    <div
      ref="sheet"
      class="shell-sheet"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shellSheetTitle"
      @touchstart="y0 = $event.touches[0].clientY"
      @touchend="onTouchEnd"
    >
      <div class="shell-grab" aria-hidden="true"></div>
      <h2 id="shellSheetTitle">Mehr</h2>
      <button v-for="m in mehr" :key="m.id" type="button" class="shell-mi" :data-mi="m.id" @click="pick(m)">
        <AppIcon :name="m.icon" :size="21" />
        <span class="shell-mi-label">{{ m.label }}<span v-if="m.id === 'neuerungen'" class="badge-neu" :hidden="!neu">NEU</span></span>
      </button>
      <div class="shell-mi shell-mi-row">
        <AppIcon :name="dark ? 'dunkel' : 'hell'" :size="21" />
        <span class="shell-mi-label">Darstellung</span>
        <ThemeSeg />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'
import ThemeSeg from './ThemeSeg.vue'
import { useNewBadge } from '@/composables/useNewBadge'
import { useTheme } from '@/composables/useTheme'

defineProps({ mehr: { type: Array, required: true } })
const emit = defineEmits(['close'])
const router = useRouter()
const neu = useNewBadge()
const { dark } = useTheme()
const sheet = ref(null)
const y0 = ref(null)
let prevFocus = null

function pick(m) {
  emit('close')
  if (m.to) router.push(m.to)
  else m.onClick?.()
}

function onTouchEnd(e) {
  if (y0.value != null && e.changedTouches[0].clientY - y0.value > 60) emit('close')
  y0.value = null
}

function onKeyDown(e) {
  if (e.key === 'Escape') { e.preventDefault(); emit('close'); return }
  if (e.key !== 'Tab') return
  const f = [...sheet.value.querySelectorAll('a,button')].filter((x) => x.tabIndex >= 0 && x.offsetParent !== null)
  if (!f.length) return
  if (e.shiftKey && document.activeElement === f[0]) { e.preventDefault(); f[f.length - 1].focus() }
  else if (!e.shiftKey && document.activeElement === f[f.length - 1]) { e.preventDefault(); f[0].focus() }
}

onMounted(() => {
  prevFocus = document.activeElement
  window.__appBusy?.add('shell-sheet')   // kein Auto-Update-Reload bei offenem Sheet
  document.body.classList.add('shell-sheet-open')
  sheet.value.querySelector('a,button')?.focus({ preventScroll: true })
})
onUnmounted(() => {
  window.__appBusy?.delete('shell-sheet')
  document.body.classList.remove('shell-sheet-open')
  if (prevFocus?.focus && document.contains(prevFocus)) {
    try { prevFocus.focus({ preventScroll: true }) } catch { /* egal */ }
  }
})
</script>
