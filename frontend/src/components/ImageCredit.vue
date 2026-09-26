<template>
  <p v-if="links" class="image-credit">
    Foto:
    <a v-if="links.nameUrl" :href="links.nameUrl" target="_blank" rel="noopener noreferrer">{{ links.name }}</a>
    <span v-else>{{ links.name }}</span>
    /
    <a :href="links.providerUrl" target="_blank" rel="noopener noreferrer">{{ links.provider }}</a>
  </p>
</template>

<script setup>
import { computed } from 'vue'
import { imageCreditLinks } from '@/utils/imageCredit'

const props = defineProps({
  credit: { type: Object, default: null },
})

const links = computed(() => imageCreditLinks(props.credit))
</script>

<style scoped>
.image-credit {
  margin: 6px 2px 0;
  font-size: 0.75rem;
  color: var(--color-text-muted, #999);
  text-align: right;
}

.image-credit a {
  color: inherit;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.image-credit a:hover {
  color: var(--color-text-secondary, #666);
}

@media print {
  .image-credit {
    margin-top: 4px;
    font-size: 0.7rem;
  }

  .image-credit a {
    text-decoration: none;
  }
}
</style>
