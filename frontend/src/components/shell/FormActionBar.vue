<template>
  <!-- Ersetzt im Rezeptformular die Tab-Leiste: mobil in der Optik der generischen BottomBar (Klassen aus
       styles/bottom-bar.css, Icon über Label, kein Orange), ab 1024 px eine klebende Zeile am Ende der
       Formularspalte (die Seitenleiste bleibt dort; Optik dort unverändert, Icons ausgeblendet) -->
  <div class="form-action-bar bbar" role="group" aria-label="Formular-Aktionen">
    <button
      v-for="a in actions"
      :key="a.id"
      type="button"
      :class="['bbar-item', `fab-${a.id}`]"
      :disabled="a.id === 'submit' && saving"
      @click="emit(a.id)"
    >
      <AppIcon class="fab-icon" :name="a.icon" :size="21" /><span>{{ a.label }}</span>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { formActions } from '@/utils/detailActions'

const props = defineProps({
  isEdit: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['cancel', 'submit'])
const actions = computed(() => formActions(props.isEdit, props.saving))
</script>

<style scoped>
/* Ab 1024 px: klebende Zeile mit Text-Knöpfen wie bisher */
@media (min-width: 1024px) {
  .form-action-bar {
    position: sticky;
    bottom: 0;
    display: flex;
    align-items: center;
    gap: 10px;
    border-top: 1px solid var(--linie);
    background: var(--bg);
    padding: 12px 0;
    justify-content: flex-end;
  }

  .fab-icon {
    display: none;
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
    flex: 0 0 auto;
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
}

.fab-cancel:focus-visible,
.fab-submit:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}

@media print {
  .form-action-bar {
    display: none;
  }
}
</style>
