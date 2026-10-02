<template>
  <div class="translation-banner" role="note">
    <p class="translation-banner__text">
      <AppIcon class="translation-banner__icon" name="globus" :size="17" />
      {{ banner.screen }}
    </p>
    <button type="button" class="translation-banner__btn" @click="emit('show-original')">Original anzeigen</button>
    <p class="translation-banner__print">{{ banner.print }}</p>
  </div>
</template>

<script setup>
import AppIcon from '@/components/shell/AppIcon.vue'
defineProps({
  banner: { type: Object, required: true },
})

const emit = defineEmits(['show-original'])
</script>

<style scoped>
.translation-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  margin: 12px 0;
  padding: 10px 14px;
  background: var(--warn-bg);
  border: 1px solid var(--warn-linie);
  border-left: 4px solid var(--akzent);
  border-radius: 8px;
  color: var(--warn-text);
}

.translation-banner__text {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}

.translation-banner__icon {
  margin-right: 4px;
}

.translation-banner__btn {
  padding: 6px 12px;
  border: 1px solid var(--akzent);
  border-radius: 6px;
  background: var(--flaeche);
  color: var(--warn-text);
  font-size: 0.85rem;
  font-family: inherit;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.translation-banner__btn:hover {
  background: var(--warn-bg);
}

.translation-banner__print {
  display: none;
}

@media print {
  .translation-banner {
    display: block;
    margin: 4px 0 12px;
    padding: 0;
    background: none;
    border: none;
  }

  .translation-banner__text,
  .translation-banner__btn {
    display: none;
  }

  .translation-banner__print {
    display: block;
    margin: 0;
    font-size: 0.9rem;
    font-style: italic;
    color: var(--color-text-secondary, #666);
  }
}
</style>
