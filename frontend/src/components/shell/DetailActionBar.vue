<template>
  <!-- Ersetzt auf der Rezeptseite mobil die Tab-Leiste (Hero-Design): ···-Kreis links (seltene Aktionen, Menü klappt
       darüber auf), Bearbeiten als Umriss-Pille, Bewerten als orange Hauptaktion rechts. Ab 1024 px gibt es die Leiste nicht. -->
  <div ref="root" class="detail-action-bar" role="group" aria-label="Rezept-Aktionen">
    <div v-if="actions.menu.length" class="dab-more">
      <button
        :ref="(el) => { buttons.more = el }"
        type="button"
        class="dab-dots"
        aria-label="Weitere Aktionen"
        aria-haspopup="menu"
        :aria-expanded="menuOpen"
        aria-controls="dab-menu"
        @click="onToggle"
      >
        <HeroIcon name="punkte" :size="18" />
      </button>
      <div v-if="menuOpen" id="dab-menu" class="dab-menu" role="menu" @keydown.esc.prevent="closeMenu(true)">
        <button
          v-for="a in actions.menu"
          :key="a.id"
          :ref="(el) => { buttons[a.id] = el }"
          type="button"
          role="menuitem"
          :class="['dab-menu__item', { 'dab-menu__item--danger': a.danger }]"
          :data-action="a.id"
          aria-haspopup="dialog"
          @click="onMenuItem(a.id)"
        >
          <HeroIcon :name="a.icon" :size="16" />{{ a.label }}
        </button>
      </div>
    </div>
    <button
      v-for="a in actions.bar"
      :key="a.id"
      :ref="(el) => { buttons[a.id] = el }"
      type="button"
      :class="['dab-btn', `dab-btn--${a.tone}`]"
      :data-action="a.id"
      :aria-haspopup="a.id === 'edit' ? undefined : 'dialog'"
      @click="onClick($event, a.id)"
    >
      <HeroIcon :name="a.icon" :size="16" />{{ a.label }}
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import HeroIcon from '@/components/HeroIcon.vue'
import { detailActions } from '@/utils/detailActions'

const props = defineProps({
  canEdit: { type: Boolean, default: false },
})
const emit = defineEmits(['delete', 'edit', 'rate'])
const actions = computed(() => detailActions(props.canEdit))
const buttons = {}
const root = ref(null)
const menuOpen = ref(false)

const onOutside = (event) => {
  if (!root.value?.contains(event.target)) closeMenu(false)
}

watch(menuOpen, (open) => {
  if (open) document.addEventListener('pointerdown', onOutside, true)
  else document.removeEventListener('pointerdown', onOutside, true)
})

onUnmounted(() => document.removeEventListener('pointerdown', onOutside, true))

function closeMenu(refocus) {
  menuOpen.value = false
  if (refocus) buttons.more?.focus({ preventScroll: true })
}

async function onToggle(event) {
  event.currentTarget.focus({ preventScroll: true })
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) {
    await nextTick()
    buttons[actions.value.menu[0]?.id]?.focus({ preventScroll: true })
  }
}

// Menüpunkte öffnen Dialoge; der Fokus kommt danach auf den ···-Kreis zurück
function onMenuItem(id) {
  closeMenu(true)
  emit(id)
}

// Safari fokussiert Knöpfe beim Tippen nicht – damit Dialoge den Fokus hierher zurückgeben können
const onClick = (event, id) => {
  event.currentTarget.focus({ preventScroll: true })
  emit(id)
}

defineExpose({
  focus: (id) => {
    const target = actions.value.menu.some((a) => a.id === id) ? buttons.more : buttons[id]
    target?.focus({ preventScroll: true })
  },
})
</script>

<style scoped>
.detail-action-bar {
  position: fixed;
  z-index: 80;
  left: 14px;
  right: 14px;
  bottom: max(14px, calc(env(safe-area-inset-bottom, 0px) + 6px));
  display: flex;
  align-items: center;
  gap: 9px;
}

.dab-more {
  position: relative;
  flex: none;
}

.dab-dots {
  width: 48px;
  height: 48px;
  border-radius: 999px;
  background: var(--h-card);
  border: 1px solid var(--h-line);
  color: var(--h-ink);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 18px rgba(0, 0, 0, .12);
  cursor: pointer;
}

.dab-menu {
  position: absolute;
  left: 0;
  bottom: calc(100% + 10px);
  width: 230px;
  padding: 6px;
  background: var(--h-card);
  border: 1px solid var(--h-line);
  border-radius: 16px;
  box-shadow: 0 10px 36px rgba(0, 0, 0, .3);
}

.dab-menu__item {
  width: 100%;
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px;
  border: none;
  border-radius: 11px;
  background: none;
  font: inherit;
  font-size: 14.5px;
  font-weight: 600;
  color: var(--h-ink);
  text-align: left;
  cursor: pointer;
}

.dab-menu__item--danger {
  color: var(--h-red);
}

.dab-menu__item:hover,
.dab-menu__item:focus-visible {
  background: var(--h-chip);
  outline: none;
}

.dab-btn {
  flex: 1;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 14px 0;
  border-radius: 999px;
  font: inherit;
  font-size: 14.5px;
  font-weight: 700;
  line-height: 1.2;
  cursor: pointer;
}

.dab-btn--ghost {
  background: var(--h-card);
  border: 1px solid var(--h-line);
  color: var(--h-ink);
  box-shadow: 0 4px 18px rgba(0, 0, 0, .12);
}

.dab-btn--primaer {
  flex: 1.15;
  background: var(--h-acc);
  border: 1px solid var(--h-acc);
  color: #fff;
  box-shadow: 0 4px 18px rgba(0, 0, 0, .18);
}

.dab-dots:active,
.dab-btn--ghost:active {
  background: var(--h-chip);
}

.dab-btn--primaer:active {
  background: color-mix(in srgb, var(--h-acc) 86%, #000);
}

@media (hover: hover) {
  .dab-dots:hover,
  .dab-btn--ghost:hover {
    background: var(--h-chip);
  }

  .dab-btn--primaer:hover {
    background: color-mix(in srgb, var(--h-acc) 86%, #000);
  }
}

.dab-dots:focus-visible,
.dab-btn:focus-visible {
  outline: 2px solid var(--h-acc);
  outline-offset: 2px;
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
