<template>
  <div class="confirm-overlay" @click.self="emit('cancel')">
    <div
      ref="dialog"
      class="confirm-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
      :aria-describedby="text ? 'confirm-text' : undefined"
    >
      <h2 id="confirm-title" class="confirm-title">{{ title }}</h2>
      <p v-if="text" id="confirm-text" class="confirm-text">{{ text }}</p>
      <div class="confirm-actions">
        <button ref="cancelBtn" type="button" class="confirm-cancel" @click="emit('cancel')">{{ cancelLabel }}</button>
        <button type="button" class="confirm-ok" :class="{ destructive }" @click="emit('confirm')">{{ confirmLabel }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

defineProps({
  title: { type: String, required: true },
  text: { type: String, default: '' },
  confirmLabel: { type: String, default: 'OK' },
  cancelLabel: { type: String, default: 'Abbrechen' },
  destructive: { type: Boolean, default: false },
})
const emit = defineEmits(['confirm', 'cancel'])
const dialog = ref(null)
const cancelBtn = ref(null)
let prevFocus = null

// Fokus startet auf der harmlosen Wahl und bleibt im Dialog; Esc bricht ab
const onKey = (e) => {
  if (e.key === 'Escape') {
    e.preventDefault()
    e.stopPropagation()
    emit('cancel')
  } else if (e.key === 'Tab') {
    const buttons = [...(dialog.value?.querySelectorAll('button') ?? [])]
    if (!buttons.length) return
    const at = buttons.indexOf(document.activeElement)
    const next = (at + (e.shiftKey ? -1 : 1) + buttons.length) % buttons.length
    e.preventDefault()
    buttons[next].focus()
  }
}

onMounted(() => {
  prevFocus = document.activeElement
  document.addEventListener('keydown', onKey, true)
  cancelBtn.value?.focus()
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKey, true)
  if (prevFocus?.focus && document.contains(prevFocus)) prevFocus.focus({ preventScroll: true })
})
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: var(--overlay);
}

.confirm-dialog {
  width: 100%;
  max-width: 400px;
  padding: 22px 20px 18px;
  border-radius: var(--radius-dialog);
  background: var(--flaeche);
  border: 1px solid var(--linie);
  box-shadow: var(--schatten-dialog);
  color: var(--text);
}

.confirm-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.3;
}

.confirm-text {
  margin: 8px 0 0;
  color: var(--text2);
  font-size: 0.95rem;
}

.confirm-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.confirm-cancel,
.confirm-ok {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.confirm-cancel {
  border: 1px solid var(--linie);
  background: var(--flaeche2);
  color: var(--text);
}

.confirm-cancel:hover {
  background: var(--linie);
}

.confirm-ok {
  border: 0;
  background: var(--akzent);
  color: var(--akzent-kontrast);
}

.confirm-ok.destructive {
  border: 1px solid var(--neg);
  background: var(--neg-weich);
  color: var(--neg);
}

.confirm-ok.destructive:hover {
  background: var(--neg);
  color: var(--flaeche);
}

.confirm-cancel:focus-visible,
.confirm-ok:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}
</style>
