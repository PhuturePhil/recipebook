<template>
  <!-- Rezeptfoto im Plan: kleine Fassung (size=thumb), erst nahe am Sichtbereich geladen; feste Größe per Klasse -->
  <img v-if="src" ref="box" :src="src" :class="cls" alt="" loading="lazy" decoding="async">
  <span v-else ref="box" :class="cls" aria-hidden="true"></span>
</template>

<script setup>
import { ref } from 'vue'
import { useRecipeImage } from '@/composables/useRecipeImage'

const props = defineProps({
  url: { type: String, default: null },
  cls: { type: String, default: 'plan-photo' },
})
const box = ref(null)
const src = useRecipeImage(() => props.url, box)
</script>
