<template>
  <!-- Fehlerseiten mit Kochbezug: angebrannter Pfannkuchen in der Pfanne. Farben nur über Tokens (hell + dunkel),
       der verkohlte Pfannkuchen bewusst mit festen Brauntönen, die auf beiden Hintergründen lesbar sind -->
  <div class="mishap">
    <svg class="mishap__art" viewBox="0 0 240 190" aria-hidden="true" focusable="false">
      <ellipse class="m-shadow" cx="118" cy="176" rx="84" ry="8" />
      <g class="m-flames">
        <path d="M84 170c-6-6-5-13 1-19 0 6 4 8 6 5 2 6-1 11-7 14z" />
        <path d="M114 172c-8-7-7-17 1-25 0 8 6 10 8 6 3 8-1 15-9 19z" />
        <path d="M146 170c-6-6-5-13 1-19 0 6 4 8 6 5 2 6-1 11-7 14z" />
      </g>
      <path class="m-pan" d="M52 128h132c-2 16-14 26-30 26H82c-16 0-28-10-30-26z" />
      <rect class="m-handle" x="182" y="123" width="50" height="11" rx="5.5" />
      <ellipse class="m-rim" cx="118" cy="128" rx="66" ry="11" />
      <path class="m-cake" d="M86 127c2-8 14-13 31-13 18 0 31 4 33 11 1 5-9 9-31 9-21 0-34-2-33-7z" />
      <g class="m-face">
        <path d="M103 119l6 5m0-5l-6 5" />
        <path d="M127 119l6 5m0-5l-6 5" />
        <path d="M110 130c3-2 6-2 9 0s6 2 9 0" />
      </g>
      <g class="m-smoke">
        <path class="m-puff m-puff--1" d="M98 104c-8-9 6-15-1-25s5-16 0-24" />
        <path class="m-puff m-puff--2" d="M120 100c-9-11 7-17-1-29s6-18 0-29" />
        <path class="m-puff m-puff--3" d="M142 104c-8-9 6-15-1-25s5-16 0-24" />
      </g>
    </svg>
    <h1 class="mishap__title">{{ title }}</h1>
    <p v-if="text" class="mishap__text">{{ text }}</p>
    <div class="mishap__actions"><slot /></div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  text: { type: String, default: '' },
})
</script>

<style scoped>
.mishap {
  max-width: 440px;
  margin: 40px auto 56px;
  padding: 0 24px;
  text-align: center;
  color: var(--text2);
}

.mishap__art {
  display: block;
  width: min(240px, 70vw);
  height: auto;
  margin: 0 auto 18px;
}

.mishap__title {
  margin: 0 0 8px;
  font-size: 1.4rem;
  font-weight: 700;
  line-height: 1.3;
  color: var(--text);
}

.mishap__text {
  margin: 0;
  font-size: 1rem;
}

.mishap__actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.m-shadow { fill: var(--flaeche2); }
.m-flames path { fill: var(--akzent); }
.m-flames path:nth-child(2) { fill: var(--akzent-400); }
.m-pan { fill: var(--text3); }
.m-handle { fill: var(--akzent); }
.m-rim { fill: var(--flaeche2); stroke: var(--text3); stroke-width: 3; }
.m-cake { fill: #5b4334; stroke: #3d2c22; stroke-width: 2; stroke-linejoin: round; }
.m-face path { fill: none; stroke: #f4e7da; stroke-width: 2.4; stroke-linecap: round; }
.m-smoke path { fill: none; stroke: var(--text3); stroke-width: 4; stroke-linecap: round; opacity: 0.75; }

@media (prefers-reduced-motion: no-preference) {
  .m-puff {
    animation: puff 3.2s ease-in-out infinite;
  }
  .m-puff--2 { animation-delay: 0.6s; }
  .m-puff--3 { animation-delay: 1.2s; }
}

@keyframes puff {
  0%, 100% { transform: translateY(0); opacity: 0.75; }
  50% { transform: translateY(-6px); opacity: 0.35; }
}
</style>
