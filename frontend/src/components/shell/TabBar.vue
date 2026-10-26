<template>
  <!-- Mobil: generische BottomBar (dieselbe Leiste wie auf der Rezeptseite), „Mehr“ ganz rechts öffnet das Sheet.
       Ab 1024 px: Seitenleiste nach app-shell-Spezifikation (Markup/CSS wie workspace/snippets/app-shell.js);
       style.css blendet .shell-nav mobil aus. Auf Formular/Rezeptseite (replaced) ersetzt deren eigene Leiste die BottomBar. -->
  <BottomBar
    v-if="!replaced"
    as="nav"
    class="tab-bottom-bar"
    label="Hauptnavigation"
    :items="bottomItems"
    @select="(id) => id === 'mehr' && emit('more')"
  />
  <nav class="shell-nav shell-ui" aria-label="Hauptnavigation">
    <div class="shell-brand"><span class="app-chip app-chip--icon" aria-hidden="true"><img src="/icon-v2.svg" alt="" decoding="async"></span><span>Rezepte</span></div>
    <div class="shell-tabs">
      <RouterLink
        v-for="t in TABS"
        :key="t.id"
        :to="t.to"
        :class="['shell-tab', { 'is-active': active === t.id }]"
        :data-tab="t.id"
        :aria-current="active === t.id ? 'page' : undefined"
      >
        <AppIcon :name="t.icon" :size="21" /><span>{{ t.label }}</span>
      </RouterLink>
      <button
        type="button"
        :class="['shell-tab', 'shell-tab-mehr', { 'is-active': active === 'mehr' }]"
        data-tab="mehr"
        aria-haspopup="dialog"
        :aria-expanded="moreOpen ? 'true' : 'false'"
        @click="emit('more')"
      >
        <AppIcon name="mehr" :size="21" /><span>Mehr</span>
        <i class="shell-dot" :hidden="!neu && !mehr.some((m) => m.badge)"></i>
      </button>
    </div>
    <div class="shell-side-more">
      <div class="shell-side-label">Mehr</div>
      <template v-for="m in mehr" :key="m.id">
        <RouterLink
          v-if="m.to"
          :to="m.to"
          :class="['shell-tab', { 'is-active': sideActive === m.id }]"
          :data-tab="m.id"
          :aria-current="sideActive === m.id ? 'page' : undefined"
        >
          <AppIcon :name="m.icon" :size="21" /><span>{{ m.label }}</span>
          <span v-if="m.id === 'neuerungen'" class="badge-neu" :hidden="!neu">NEU</span>
          <span v-else-if="m.badge" class="badge-neu">NEU</span>
        </RouterLink>
        <button v-else type="button" class="shell-tab" :data-tab="m.id" @click="m.onClick">
          <AppIcon :name="m.icon" :size="21" /><span>{{ m.label }}</span>
        </button>
      </template>
      <ThemeSeg />
      <LegalLinks class="shell-legal" />
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import BottomBar from './BottomBar.vue'
import ThemeSeg from './ThemeSeg.vue'
import LegalLinks from '@/components/LegalLinks.vue'
import { useNewBadge } from '@/composables/useNewBadge'
import { NEW_RECIPE_PATH, MEAL_PLAN_PATH } from '@/router/paths.js'

const props = defineProps({
  active: { type: String, default: '' },
  sideActive: { type: String, default: '' },
  mehr: { type: Array, required: true },
  moreOpen: { type: Boolean, default: false },
  replaced: { type: Boolean, default: false },
})
const emit = defineEmits(['more'])
const neu = useNewBadge()

// Rezepte · Plan · Neu · Mehr (Essensplan 26.10.2026: „Zutaten“ ist ins Mehr-Menü gewandert)
const TABS = [
  { id: 'rezepte', label: 'Rezepte', icon: 'buch', to: '/' },
  { id: 'plan', label: 'Plan', icon: 'plan', to: MEAL_PLAN_PATH },
  { id: 'neu', label: 'Neu', icon: 'plus', to: NEW_RECIPE_PATH },
]

const bottomItems = computed(() => [
  ...TABS.map((t) => ({ ...t, current: props.active === t.id })),
  {
    id: 'mehr',
    label: 'Mehr',
    icon: 'mehr',
    current: props.active === 'mehr',
    popup: 'dialog',
    expanded: props.moreOpen,
    dot: neu.value || props.mehr.some((m) => m.badge),
  },
])
</script>

<style scoped>
.legal-links.shell-legal { margin: 12px 0 0; padding: 0 4px; justify-content: flex-start; }
</style>
