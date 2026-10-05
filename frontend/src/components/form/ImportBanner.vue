<template>
  <div class="import-banner" role="status">
    <div class="import-banner-head">
      <span class="import-banner-text">Aus Foto importiert – bitte prüfen</span>
      <button type="button" class="import-banner-close" aria-label="Hinweis schließen" @click="emit('close')">
        <AppIcon name="schliessen" :size="18" />
      </button>
    </div>
    <details v-if="rawText" class="import-raw">
      <summary>Erkannten Text anzeigen</summary>
      <textarea readonly :value="rawText" rows="6" aria-label="Erkannter Text"></textarea>
      <button type="button" class="btn-copy" @click="copy">{{ copied ? 'Kopiert!' : 'Text kopieren' }}</button>
      <p v-if="copyFailed" class="copy-error" role="alert">Text konnte nicht kopiert werden.</p>
    </details>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'

const props = defineProps({
  rawText: { type: String, default: '' },
})
const emit = defineEmits(['close'])
const copied = ref(false)
const copyFailed = ref(false)

const copy = async () => {
  try {
    await navigator.clipboard.writeText(props.rawText)
    copyFailed.value = false
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    copyFailed.value = true
  }
}
</script>

<style scoped>
.import-banner {
  padding: 10px 12px;
  margin-bottom: 20px;
  border: 1px solid var(--linie);
  border-left: 4px solid var(--akzent);
  border-radius: 8px;
  background: var(--flaeche2);
  color: var(--text);
  font-size: 0.95rem;
}

.import-banner-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.import-banner-text {
  font-weight: 600;
}

.import-banner-close {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  margin: -10px -10px -10px 0;
  border: 0;
  border-radius: 8px;
  background: none;
  color: var(--text2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.import-banner-close:hover {
  color: var(--text);
}

.import-raw {
  margin-top: 8px;
}

.import-raw summary {
  cursor: pointer;
  color: var(--akzent-text);
  font-size: 0.9rem;
  padding: 4px 0;
}

.import-raw textarea {
  display: block;
  width: 100%;
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid var(--linie);
  border-radius: 6px;
  font-size: 0.875rem;
  font-family: inherit;
  background: var(--flaeche);
  color: var(--text);
  resize: vertical;
}

.btn-copy {
  margin-top: 8px;
  padding: 6px 14px;
  min-height: 36px;
  border: 1px solid var(--linie);
  border-radius: 6px;
  background: var(--flaeche);
  color: var(--text);
  cursor: pointer;
  font-size: 0.875rem;
}

.btn-copy:hover {
  background: var(--linie);
}

.copy-error {
  margin: 6px 0 0;
  color: var(--neg);
  font-size: 0.85rem;
}

.import-banner-close:focus-visible,
.btn-copy:focus-visible,
.import-raw summary:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}
</style>
