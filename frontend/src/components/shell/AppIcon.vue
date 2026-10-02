<template>
  <span class="icon-slot" v-html="html"></span>
</template>

<script setup>
import { computed } from 'vue'
import '@/shell/app-icons.js'

// Linien-Icon aus dem gemeinsamen Set (Kopie von workspace/snippets/app-icons.js, keine Emojis als UI-Icons)
const props = defineProps({ name: { type: String, required: true }, size: { type: Number, default: 20 } })
const html = computed(() => (window.APP_ICONS ? window.APP_ICONS.svg(props.name, props.size) : ''))
</script>

<style>
.icon-slot { display: inline-flex; flex-shrink: 0; line-height: 0; }
</style>
