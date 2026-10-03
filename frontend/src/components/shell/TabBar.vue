<template>
  <!-- Navigation nach app-shell-Spezifikation (gleiches Markup/CSS wie workspace/snippets/app-shell.js):
       mobil schwebende Pillen-Tab-Leiste, ab 1024 px Seitenleiste mit den Mehr-Punkten -->
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
        <i class="shell-dot" :hidden="!neu"></i>
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
        </RouterLink>
        <button v-else type="button" class="shell-tab" :data-tab="m.id" @click="m.onClick">
          <AppIcon :name="m.icon" :size="21" /><span>{{ m.label }}</span>
        </button>
      </template>
      <ThemeSeg />
    </div>
  </nav>
</template>

<script setup>
import AppIcon from './AppIcon.vue'
import ThemeSeg from './ThemeSeg.vue'
import { useNewBadge } from '@/composables/useNewBadge'

defineProps({
  active: { type: String, default: '' },
  sideActive: { type: String, default: '' },
  mehr: { type: Array, required: true },
  moreOpen: { type: Boolean, default: false },
})
const emit = defineEmits(['more'])
const neu = useNewBadge()

const TABS = [
  { id: 'rezepte', label: 'Rezepte', icon: 'buch', to: '/' },
  { id: 'neu', label: 'Neu', icon: 'plus', to: '/recipe/new' },
  { id: 'zutaten', label: 'Zutaten', icon: 'karotte', to: '/ingredients' },
]
</script>
