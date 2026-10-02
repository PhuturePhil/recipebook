<template>
  <div class="ui-seg" role="radiogroup" aria-label="Darstellung" @keydown="onKey">
    <button
      v-for="t in THEMES"
      :key="t.id"
      type="button"
      role="radio"
      :data-theme-set="t.id"
      :aria-checked="pref === t.id ? 'true' : 'false'"
      :tabindex="pref === t.id ? 0 : -1"
      @click="setTheme(t.id)"
    >
      <AppIcon :name="t.icon" :size="15" /><span>{{ t.label }}</span>
    </button>
  </div>
</template>

<script setup>
import AppIcon from './AppIcon.vue'
import { useTheme } from '@/composables/useTheme'

const THEMES = [
  { id: 'auto', label: 'Auto', icon: 'auto' },
  { id: 'light', label: 'Hell', icon: 'hell' },
  { id: 'dark', label: 'Dunkel', icon: 'dunkel' },
]
const { pref, setTheme } = useTheme()

function onKey(e) {
  if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
  e.preventDefault()
  const idx = THEMES.findIndex((t) => t.id === pref.value)
  const next = THEMES[(idx + (e.key === 'ArrowRight' ? 1 : THEMES.length - 1)) % THEMES.length].id
  setTheme(next)
  e.currentTarget.querySelector(`[data-theme-set="${next}"]`)?.focus()
}
</script>
