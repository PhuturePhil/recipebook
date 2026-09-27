<template>
  <span v-if="domain" class="source-link-wrap">
    <a
      class="source-link"
      :href="url"
      target="_blank"
      rel="noopener noreferrer"
      :title="url"
    >
      <span class="source-link__domain">{{ domain }}</span>
      <svg
        class="source-link__icon"
        role="img"
        aria-label="öffnet externe Website"
        viewBox="0 0 24 24"
        width="14"
        height="14"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
        <polyline points="15 3 21 3 21 9" />
        <line x1="10" y1="14" x2="21" y2="3" />
      </svg>
    </a>
    <a class="source-link-print" :href="url">{{ url }}</a>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { cleanSourceUrl, sourceDomain } from '@/utils/sourceUrl'

const props = defineProps({
  href: { type: String, default: null }
})

const url = computed(() => cleanSourceUrl(props.href))
const domain = computed(() => sourceDomain(url.value))
</script>

<style scoped>
.source-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  /* Größere Tippfläche ohne die Zeile höher zu machen */
  padding: 12px 4px;
  margin: -12px 0;
  color: var(--color-primary, #4a5568);
  font-style: normal;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.source-link:hover .source-link__domain {
  text-decoration-thickness: 2px;
}

.source-link__icon {
  flex-shrink: 0;
}

.source-link-print {
  display: none;
}

@media print {
  .source-link {
    display: none;
  }

  .source-link-print {
    display: inline;
    color: inherit;
    font-style: normal;
    text-decoration: none;
    word-break: break-all;
  }
}
</style>
