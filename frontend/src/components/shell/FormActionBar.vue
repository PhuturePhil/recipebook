<template>
  <!-- Ersetzt im Rezeptformular die Tab-Leiste: mobil die Pille der Tab-Leiste mit Einträgen im Tab-Stil
       (Icon über Label, kein Orange – wie DetailActionBar), ab 1024 px eine klebende Zeile am Ende der
       Formularspalte (die Seitenleiste bleibt dort; Optik dort unverändert, Icons ausgeblendet) -->
  <div class="form-action-bar" role="group" aria-label="Formular-Aktionen">
    <button
      v-for="a in actions"
      :key="a.id"
      type="button"
      :class="`fab-${a.id}`"
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
/* Mobil: Pille und Einträge wie .shell-nav/.shell-tab mobil (app-shell.css), eigene Klassen, damit die
   Seitenleisten-Regeln ab 1024 px nicht greifen */
@media (max-width: 1023px) {
  .form-action-bar {
    position: fixed;
    z-index: 80;
    left: 18px;
    right: 18px;
    bottom: max(20px, calc(env(safe-area-inset-bottom, 0px) + 8px));
    display: flex;
    gap: 2px;
    border-radius: 999px;
    background: var(--flaeche);
    border: 1px solid var(--linie);
    box-shadow: var(--bar-schatten);
    padding: 6px 7px;
  }

  @supports ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
    .form-action-bar {
      background: var(--bar-bg);
      -webkit-backdrop-filter: saturate(1.6) blur(var(--bar-blur));
      backdrop-filter: saturate(1.6) blur(var(--bar-blur));
    }
  }

  .fab-cancel,
  .fab-submit {
    flex: 1 1 0;
    min-width: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    padding: 6px 2px 5px;
    border: 0;
    border-radius: 999px;
    background: none;
    color: var(--text2);
    font: inherit;
    font-size: 10.5px;
    font-weight: 600;
    line-height: 1.15;
    cursor: pointer;
    white-space: nowrap;
    transition: background 0.15s, color 0.15s;
  }

  .form-action-bar span {
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
  }

  .fab-cancel:active,
  .fab-submit:active:not(:disabled) {
    background: var(--flaeche2);
  }

  .fab-submit:disabled {
    opacity: 0.7;
    cursor: wait;
  }
}

@media (max-width: 1023px) and (hover: hover) {
  .fab-cancel:hover,
  .fab-submit:hover:not(:disabled) {
    background: var(--flaeche2);
    color: var(--text);
  }
}

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
