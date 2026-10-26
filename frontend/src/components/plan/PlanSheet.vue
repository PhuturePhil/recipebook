<template>
  <!-- Sheet des Essensplans: mobil von unten, ab 1024 px als Dialog (die shell-sheet-Klassen gibt es am Desktop nicht).
       Esc und Tippen daneben schließen, Tab bleibt im Sheet, danach kehrt der Fokus zurück. -->
  <Teleport to="body">
    <div class="plan-sheet-backdrop" @click.self="emit('close')">
      <div
        ref="sheet"
        class="plan-sheet"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        @touchstart.passive="onTouchStart"
        @touchend="onTouchEnd"
      >
        <div class="plan-sheet__grab" aria-hidden="true"></div>
        <div class="plan-sheet__head">
          <div class="plan-sheet__titles">
            <h2 :id="titleId">{{ title }}</h2>
            <p v-if="subtitle">{{ subtitle }}</p>
          </div>
          <button type="button" class="plan-sheet__close" :aria-label="t('close')" @click="emit('close')">
            <AppIcon name="schliessen" :size="20" />
          </button>
        </div>
        <slot />
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { onMounted, onUnmounted, ref, useId } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import { cycleFocus } from '@/utils/detailActions'
import { planText as t } from '@/utils/planTexts'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  // Selektor des ersten Fokusziels (sonst das erste Bedienelement nach „Schließen“)
  initialFocus: { type: String, default: '' },
  // Element, das den Fokus nach dem Schließen bekommt (z. B. die ···-Taste der Karte)
  returnFocus: { type: Object, default: null },
})
const emit = defineEmits(['close'])
const titleId = `plan-sheet-${useId()}`
const sheet = ref(null)
let prevFocus = null
let y0 = null

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), [tabindex="0"]'

const onKey = (e) => {
  if (e.key === 'Escape') {
    e.preventDefault()
    e.stopPropagation()
    emit('close')
  } else if (e.key === 'Tab') {
    const items = [...(sheet.value?.querySelectorAll(FOCUSABLE) ?? [])].filter((el) => el.offsetParent !== null)
    e.preventDefault()
    const next = cycleFocus(items.length, items.indexOf(document.activeElement), e.shiftKey)
    if (next >= 0) items[next].focus()
  }
}

// Nach unten wischen schließt – nur am Griff/Kopf, damit Listen im Sheet scrollen können
function onTouchStart(e) {
  y0 = e.target.closest?.('.plan-sheet__body') ? null : e.touches[0].clientY
}
function onTouchEnd(e) {
  if (y0 != null && e.changedTouches[0].clientY - y0 > 60) emit('close')
  y0 = null
}

onMounted(() => {
  prevFocus = props.returnFocus ?? document.activeElement
  window.__appBusy?.add(sheet)
  document.addEventListener('keydown', onKey, true)
  const first = (props.initialFocus && sheet.value?.querySelector(props.initialFocus))
    || [...(sheet.value?.querySelectorAll(FOCUSABLE) ?? [])].find((el) => !el.classList.contains('plan-sheet__close'))
  first?.focus({ preventScroll: true })
})
onUnmounted(() => {
  window.__appBusy?.delete(sheet)
  document.removeEventListener('keydown', onKey, true)
  if (prevFocus?.focus && document.contains(prevFocus)) prevFocus.focus({ preventScroll: true })
})
</script>
