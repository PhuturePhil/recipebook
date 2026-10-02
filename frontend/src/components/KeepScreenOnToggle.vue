<template>
  <button
    v-if="supported"
    type="button"
    :class="['screen-toggle', { 'screen-toggle--on': enabled, 'screen-toggle--waiting': enabled && !active }]"
    :aria-pressed="enabled"
    :title="title"
    @click="toggle"
  >
    <svg class="screen-toggle__icon" viewBox="0 0 24 24" aria-hidden="true">
      <rect x="6" y="2.5" width="12" height="19" rx="2.5" />
      <line x1="10.5" y1="18" x2="13.5" y2="18" />
    </svg>
    {{ enabled ? 'Display bleibt an' : 'Display geht aus' }}
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useWakeLock } from '@/composables/useWakeLock'

const { supported, enabled, active, toggle } = useWakeLock()

const title = computed(() => {
  if (!enabled.value) return 'Das Display geht wie gewohnt aus. Tippen, damit es beim Kochen anbleibt'
  if (!active.value) return 'Der Browser erlaubt es gerade nicht, z. B. im Energiesparmodus'
  return 'Das Display geht auf dieser Seite nicht aus. Tippen zum Ausschalten'
})
</script>

<style scoped>
.screen-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--color-bg-card, #fff);
  color: var(--color-text-muted, #999);
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-family: inherit;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.screen-toggle:hover {
  background: var(--color-bg-secondary, #f0f0f0);
}

.screen-toggle--on {
  color: var(--pos);
  background: var(--pos-weich);
  border-color: var(--pos-linie);
}

.screen-toggle--on:hover {
  background: var(--pos-weich);
}

.screen-toggle--waiting {
  opacity: 0.65;
}

.screen-toggle__icon {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

.screen-toggle--on .screen-toggle__icon rect {
  fill: rgba(72, 187, 120, 0.2);
}

@media print {
  .screen-toggle {
    display: none !important;
  }
}
</style>
