<template>
  <!-- Generische untere Leiste (mobil < 1024 px), auf jeder Seite gleich: Pille, Einträge gleich groß als Icon über
       Label, höchstens 4; Hauptaktion/aktiver Punkt orange; Überlauf-Menü „Mehr“ ganz rechts, klappt nach oben auf.
       Optik in styles/bottom-bar.css, Aufteilung in utils/bottomBar.js -->
  <component :is="as" ref="root" class="bbar" :role="as === 'nav' ? undefined : 'group'" :aria-label="label">
    <template v-for="item in layout.bar" :key="item.id">
      <RouterLink
        v-if="item.to"
        :to="item.to"
        :class="itemClass(item)"
        :data-action="item.id"
        :aria-current="item.current ? 'page' : undefined"
        @click="closeMenu(false)"
      >
        <AppIcon :name="item.icon" :size="21" /><span>{{ item.label }}</span>
        <i v-if="'dot' in item" class="bbar-dot" :hidden="!item.dot"></i>
      </RouterLink>
      <button
        v-else
        :ref="(el) => { buttons[item.id] = el }"
        type="button"
        :class="itemClass(item)"
        :data-action="item.id"
        :aria-haspopup="item.popup"
        :aria-expanded="'expanded' in item ? String(!!item.expanded) : undefined"
        @click="onItem($event, item.id)"
      >
        <AppIcon :name="item.icon" :size="21" /><span>{{ item.label }}</span>
        <i v-if="'dot' in item" class="bbar-dot" :hidden="!item.dot"></i>
      </button>
    </template>
    <button
      v-if="layout.menu.length"
      :ref="(el) => { buttons[MORE] = el }"
      type="button"
      class="bbar-item bbar-more"
      :data-action="MORE"
      aria-haspopup="menu"
      :aria-expanded="String(menuOpen)"
      :aria-controls="menuId"
      @click="onToggle"
    >
      <AppIcon name="mehr" :size="21" /><span>Mehr</span>
    </button>
    <div v-if="menuOpen" :id="menuId" class="bbar-menu" role="menu" @keydown.esc.prevent="closeMenu(true)">
      <button
        v-for="m in layout.menu"
        :key="m.id"
        :ref="(el) => { buttons[m.id] = el }"
        type="button"
        role="menuitem"
        :class="['bbar-menu__item', { 'bbar-menu__item--danger': m.danger }]"
        :data-action="m.id"
        :aria-haspopup="m.popup"
        @click="onMenuItem(m.id)"
      >
        <AppIcon :name="m.icon" :size="18" />{{ m.label }}
      </button>
    </div>
  </component>
</template>

<script setup>
import { computed, nextTick, onUnmounted, ref, useId, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import { bottomBarLayout } from '@/utils/bottomBar'

const props = defineProps({
  // { id, label, icon, to?, primary?, current?, popup?, expanded?, dot?, danger? }
  items: { type: Array, required: true },
  menu: { type: Array, default: () => [] },
  label: { type: String, default: '' },
  as: { type: String, default: 'div' },
})
const emit = defineEmits(['select'])

const MORE = 'mehr-menue'
const menuId = `bbar-menu-${useId()}`
const layout = computed(() => bottomBarLayout(props.items, props.menu))
const buttons = {}
const root = ref(null)
const menuOpen = ref(false)

const itemClass = (item) => ['bbar-item', { 'is-accent': item.primary || item.current }]

const rootEl = () => root.value?.$el ?? root.value
const onOutside = (event) => {
  if (!rootEl()?.contains(event.target)) closeMenu(false)
}

watch(menuOpen, (open) => {
  if (open) document.addEventListener('pointerdown', onOutside, true)
  else document.removeEventListener('pointerdown', onOutside, true)
})

onUnmounted(() => document.removeEventListener('pointerdown', onOutside, true))

function closeMenu(refocus) {
  menuOpen.value = false
  if (refocus) buttons[MORE]?.focus({ preventScroll: true })
}

async function onToggle(event) {
  event.currentTarget.focus({ preventScroll: true })
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) {
    await nextTick()
    buttons[layout.value.menu[0]?.id]?.focus({ preventScroll: true })
  }
}

// Menüpunkte öffnen meist Dialoge; der Fokus kommt danach auf „Mehr“ zurück
function onMenuItem(id) {
  closeMenu(true)
  emit('select', id)
}

// Safari fokussiert Knöpfe beim Tippen nicht – damit Dialoge den Fokus hierher zurückgeben können
function onItem(event, id) {
  event.currentTarget.focus({ preventScroll: true })
  closeMenu(false)
  emit('select', id)
}

defineExpose({
  focus: (id) => {
    const target = layout.value.menu.some((m) => m.id === id) ? buttons[MORE] : buttons[id]
    target?.focus({ preventScroll: true })
  },
})
</script>

<style scoped>
@media (min-width: 1024px) {
  .bbar {
    display: none;
  }
}

@media print {
  .bbar {
    display: none;
  }
}
</style>
