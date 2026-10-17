<template>
  <!-- Dezente Fußzeile: Impressum, Datenschutz und auf fremden Inhalten „Inhalt melden“ (Mail, kein Ticket-System) -->
  <nav class="legal-links" aria-label="Rechtliches">
    <a v-if="report" :href="reportHref" class="legal-links__report">Inhalt melden</a>
    <RouterLink to="/impressum">Impressum</RouterLink>
    <RouterLink to="/datenschutz">Datenschutz</RouterLink>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { reportMailto } from '@/utils/legal'

const props = defineProps({
  // { recipeId?, title, url } – nur setzen, wo fremde Inhalte stehen
  report: { type: Object, default: null },
})
const reportHref = computed(() => (props.report ? reportMailto(props.report) : ''))
</script>

<style scoped>
.legal-links {
  display: flex; flex-wrap: wrap; justify-content: center; align-items: center;
  gap: 4px 0; margin: 24px auto 8px; padding: 0 16px;
  font-size: 13px; line-height: 1.6;
}
.legal-links a { color: var(--text2); text-decoration: none; padding: 4px 2px; }
.legal-links a:hover, .legal-links a:focus-visible { color: var(--akzent-text); text-decoration: underline; }
.legal-links a + a::before { content: '·'; display: inline-block; margin: 0 8px; color: var(--text3); text-decoration: none; }
@media print { .legal-links { display: none; } }
</style>
