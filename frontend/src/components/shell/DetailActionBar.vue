<template>
  <!-- Ersetzt auf der Rezeptseite mobil die Tab-Leiste: gleiche Pille, Einträge im Tab-Stil (Icon über Label),
       aber nur Aktionen – kein Aktiv-Zustand, kein Orange. Ab 1024 px gibt es die Leiste nicht. -->
  <div class="detail-action-bar shell-ui" role="group" aria-label="Rezept-Aktionen">
    <div class="shell-tabs">
      <button
        v-for="a in actions"
        :key="a.id"
        :ref="(el) => { buttons[a.id] = el }"
        type="button"
        :class="['shell-tab', { 'shell-tab--gefahr': a.danger }]"
        :data-action="a.id"
        :aria-haspopup="a.id === 'edit' ? undefined : 'dialog'"
        @click="onClick($event, a.id)"
      >
        <AppIcon :name="a.icon" :size="21" /><span>{{ a.label }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { detailActions } from '@/utils/detailActions'

const props = defineProps({
  canEdit: { type: Boolean, default: false },
})
const emit = defineEmits(['delete', 'edit', 'rate'])
const actions = computed(() => detailActions(props.canEdit))
const buttons = {}

// Safari fokussiert Knöpfe beim Tippen nicht – damit Dialoge den Fokus hierher zurückgeben können
const onClick = (event, id) => {
  event.currentTarget.focus({ preventScroll: true })
  emit(id)
}

defineExpose({
  focus: (id) => buttons[id]?.focus({ preventScroll: true }),
})
</script>

<style scoped>
/* Pille wie .shell-nav mobil (app-shell.css) – eigene Klasse, damit die Seitenleisten-Regeln ab 1024 px nicht greifen */
.detail-action-bar {
  position: fixed;
  z-index: 80;
  left: 18px;
  right: 18px;
  bottom: max(20px, calc(env(safe-area-inset-bottom, 0px) + 8px));
  border-radius: 999px;
  background: var(--flaeche);
  border: 1px solid var(--linie);
  box-shadow: var(--bar-schatten);
  padding: 6px 7px;
}

@supports ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .detail-action-bar {
    background: var(--bar-bg);
    -webkit-backdrop-filter: saturate(1.6) blur(var(--bar-blur));
    backdrop-filter: saturate(1.6) blur(var(--bar-blur));
  }
}

.shell-tab--gefahr {
  color: var(--neg);
}

.shell-tab:active {
  background: var(--flaeche2);
}

@media (hover: hover) {
  .shell-tab:hover {
    background: var(--flaeche2);
    color: var(--text);
  }

  .shell-tab--gefahr:hover {
    color: var(--neg);
  }
}

@media (min-width: 1024px) {
  .detail-action-bar {
    display: none;
  }
}

@media print {
  .detail-action-bar {
    display: none;
  }
}
</style>
