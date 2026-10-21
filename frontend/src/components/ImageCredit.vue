<template>
  <p v-if="links" :class="['image-credit', { 'image-credit--overlay': overlay }]">
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
  // Rezeptseite (Hero): Glas-Chip oben rechts im Foto
  overlay: { type: Boolean, default: false },
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

/* Werte aus dem abgenommenen Entwurf (mock_detail.html, .credit) */
.image-credit--overlay {
  margin: 0;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(20, 20, 24, .42);
  color: rgba(255, 255, 255, .9);
  font-size: 10.5px;
  line-height: 1.35;
  text-align: left;
}

.image-credit--overlay a:hover {
  color: #fff;
}

@media print {
  .image-credit--overlay {
    padding: 0;
    background: none;
    color: #8E8E93;
    text-align: right;
  }

  .image-credit {
    margin-top: 4px;
    font-size: 0.7rem;
  }

  .image-credit a {
    text-decoration: none;
  }
}
</style>
