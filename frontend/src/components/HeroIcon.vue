<template>
  <svg
    class="hero-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    :fill="icon.fill ? 'currentColor' : 'none'"
    :stroke="icon.fill ? 'none' : 'currentColor'"
    :stroke-width="icon.sw"
    aria-hidden="true"
    focusable="false"
  >
    <template v-for="(el, i) in icon.parts" :key="i">
      <circle v-if="el.c" :cx="el.c[0]" :cy="el.c[1]" :r="el.c[2]" />
      <rect v-else-if="el.r" :x="el.r[0]" :y="el.r[1]" :width="el.r[2]" :height="el.r[3]" :rx="el.r[4]" />
      <path v-else :d="el" />
    </template>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

// Icons des Hero-Designs (Startseite + Rezeptseite), Formen aus den abgenommenen Entwürfen; Farbe über currentColor
const ICONS = {
  uhr: { sw: 2.2, parts: [{ c: [12, 12, 9] }, 'M12 7v5l3 2'] },
  zutaten: { sw: 2.2, parts: ['M7 21c0-6 2-10 10-16M9 9c3-1 6-1 9-4-1 6-3 10-9 11-3 .5-4 1-5 3'] },
  flamme: { sw: 2.2, parts: ['M12 3c1 4-4 5-4 9a4 4 0 0 0 8 .5C16 9 14 7 12 3z', 'M10 15c0 2 .8 3 2 3s2-1 2-2.5'] },
  blitz: { fill: true, parts: ['M13 2L4 14h6l-1 8 9-12h-6l1-8z'] },
  tropfen: { sw: 2.4, parts: ['M12 4c2.6 3.9 5 6.6 5 9.5a5 5 0 0 1-10 0C7 10.6 9.4 7.9 12 4z'] },
  hantel: { sw: 2.4, parts: ['M6 7v10M18 7v10M3 9.5v5M21 9.5v5M6 12h12'] },
  blatt: { sw: 2.4, parts: ['M5 19c0-8 5-13 14-14-1 9-6 14-14 14z', 'M5 19l7-7'] },
  aehre: { sw: 2.2, parts: ['M12 21V7', 'M12 9c-2.6 0-4-1.6-4-4 2.6 0 4 1.6 4 4zM12 9c2.6 0 4-1.6 4-4-2.6 0-4 1.6-4 4z', 'M12 15c-2.6 0-4-1.6-4-4 2.6 0 4 1.6 4 4zM12 15c2.6 0 4-1.6 4-4-2.6 0-4 1.6-4 4z'] },
  stern: { fill: true, parts: ['M12 2l3 7 7 .6-5.3 4.8L18.5 22 12 18l-6.5 4 1.8-7.6L2 9.6 9 9z'] },
  sternLinie: { sw: 2, parts: ['M12 2l3 7 7 .6-5.3 4.8L18.5 22 12 18l-6.5 4 1.8-7.6L2 9.6 9 9z'] },
  zurueck: { sw: 2.6, parts: ['M15 5l-7 7 7 7'] },
  teilen: { sw: 2, parts: [{ c: [6, 12, 2.6] }, { c: [18, 6, 2.6] }, { c: [18, 18, 2.6] }, 'M8.3 10.8l7.4-3.6M8.3 13.2l7.4 3.6'] },
  pdf: { sw: 2, parts: ['M6 3h8l4 4v14H6z', 'M14 3v4h4'] },
  display: { sw: 2, parts: [{ r: [7, 2.5, 10, 19, 2.5] }, 'M11 18.5h2'] },
  loeschen: { sw: 2, parts: ['M4 7h16M9 7V4h6v3M6.5 7l1 13h9l1-13M10 11v5M14 11v5'] },
  bearbeiten: { sw: 2, parts: ['M4 20l4-1L19.5 7.5a2 2 0 0 0-3-3L5 16z'] },
  punkte: { fill: true, parts: [{ c: [5, 12, 1.8] }, { c: [12, 12, 1.8] }, { c: [19, 12, 1.8] }] },
  topf: { sw: 1.6, parts: ['M4 10h16v7a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3z', 'M2 10h20M9 6.5c0-1 .8-1.5 1.5-1.5h3c.7 0 1.5.5 1.5 1.5V10', 'M8 3.5c.5.6.5 1.2 0 1.8M12 2.5c.5.6.5 1.2 0 1.8M16 3.5c.5.6.5 1.2 0 1.8'] },
}

const props = defineProps({
  name: { type: String, required: true },
  size: { type: Number, default: 14 },
})

const icon = computed(() => ICONS[props.name] ?? ICONS.blatt)
</script>

<style scoped>
.hero-icon {
  flex: none;
  display: inline-block;
}
</style>
