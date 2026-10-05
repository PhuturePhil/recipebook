<template>
  <!-- Ersetzt im Rezeptformular die Tab-Leiste: mobil eine deckende Pille an ihrer Stelle,
       ab 1024 px eine klebende Zeile am Ende der Formularspalte (die Seitenleiste bleibt dort) -->
  <div class="form-action-bar" role="group" aria-label="Formular-Aktionen">
    <button type="button" class="fab-cancel" @click="emit('cancel')">Abbrechen</button>
    <button type="button" class="fab-submit" :disabled="saving" @click="emit('submit')">
      {{ saving ? 'Wird gespeichert…' : submitLabel }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  submitLabel: { type: String, required: true },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['cancel', 'submit'])
</script>

<style scoped>
.form-action-bar {
  position: fixed;
  z-index: 80;
  left: 18px;
  right: 18px;
  bottom: max(20px, calc(env(safe-area-inset-bottom, 0px) + 8px));
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 999px;
  background: var(--flaeche);
  border: 1px solid var(--linie);
  box-shadow: var(--bar-schatten);
}

.fab-cancel {
  flex: 0 0 auto;
  min-height: 44px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  background: none;
  color: var(--text2);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.fab-cancel:hover {
  background: var(--flaeche2);
}

.fab-submit {
  flex: 1 1 auto;
  min-height: 48px;
  padding: 0 20px;
  border: 0;
  border-radius: 999px;
  background: var(--akzent);
  color: var(--akzent-kontrast);
  font: inherit;
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
}

.fab-submit:hover:not(:disabled) {
  background: var(--akzent-hover);
}

.fab-submit:disabled {
  opacity: 0.7;
  cursor: wait;
}

.fab-cancel:focus-visible,
.fab-submit:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}

@media (min-width: 1024px) {
  .form-action-bar {
    position: sticky;
    left: auto;
    right: auto;
    bottom: 0;
    border-radius: 0;
    border: 0;
    border-top: 1px solid var(--linie);
    box-shadow: none;
    background: var(--bg);
    padding: 12px 0;
    justify-content: flex-end;
  }

  .fab-submit {
    flex: 0 0 auto;
  }
}

@media print {
  .form-action-bar {
    display: none;
  }
}
</style>
