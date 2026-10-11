<template>
  <div class="confirm-overlay" @click.self="emit('close')">
    <div ref="dialog" class="confirm-dialog rating-dialog" role="dialog" aria-modal="true" aria-labelledby="rating-dialog-title">
      <h2 id="rating-dialog-title" class="confirm-title">Rezept bewerten</h2>
      <RatingForm :recipe-id="recipeId" dialog @cancel="emit('close')" @submitted="emit('submitted')" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import RatingForm from '@/components/RatingForm.vue'
import { cycleFocus } from '@/utils/detailActions'

// Muster wie ConfirmDialog: Esc und Klick daneben schließen, Tab bleibt im Dialog.
// Den Fokus gibt die Seite danach an „Bewerten“ in der Leiste zurück.
defineProps({
  recipeId: { type: [Number, String], required: true },
})
const emit = defineEmits(['close', 'submitted'])
const dialog = ref(null)

const FOCUSABLE = 'button:not([disabled]):not([tabindex="-1"]), textarea:not([disabled])'

const onKey = (e) => {
  if (e.key === 'Escape') {
    e.preventDefault()
    e.stopPropagation()
    emit('close')
  } else if (e.key === 'Tab') {
    const items = [...(dialog.value?.querySelectorAll(FOCUSABLE) ?? [])]
    e.preventDefault()
    const next = cycleFocus(items.length, items.indexOf(document.activeElement), e.shiftKey)
    if (next >= 0) items[next].focus()
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKey, true)
  // Erster Fokus auf dem gewählten Stern (Radiogruppe: der mit tabindex 0, ohne Wahl Stern 1)
  dialog.value?.querySelector('.star-btn[tabindex="0"]')?.focus()
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKey, true)
  // Fokus nicht aus dem Textfeld „vererben“: sonst zeigt Safari am Leisten-Eintrag einen Fokusrahmen nach dem Tippen
  if (document.activeElement?.tagName === 'TEXTAREA') document.activeElement.blur()
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
</style>
