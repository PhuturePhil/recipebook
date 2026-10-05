<template>
  <section class="import-section" aria-labelledby="import-title">
    <h2 id="import-title" class="import-title">Rezept aus Foto importieren</h2>
    <p class="import-hint">Eine oder mehrere Fotos vom Rezept auswählen, dann analysieren lassen.</p>
    <input
      id="scan-upload"
      type="file"
      accept="image/*"
      multiple
      class="visually-hidden"
      @change="onFiles"
    />
    <label for="scan-upload" class="btn-scan">
      {{ images.length > 0 ? 'Weitere Bilder hinzufügen' : 'Rezeptfotos auswählen' }}
    </label>
    <div v-if="images.length > 0" class="selected-images">
      <div v-for="(img, index) in images" :key="img.previewUrl" class="selected-image-item">
        <img :src="img.previewUrl" :alt="img.fileName" />
        <span class="image-name">{{ img.fileName }}</span>
        <button type="button" class="btn-remove-scan-image" aria-label="Bild entfernen" @click="emit('remove', index)">
          <AppIcon name="schliessen" :size="16" />
        </button>
      </div>
    </div>
    <button
      v-if="images.length > 0"
      type="button"
      class="btn-analyze"
      :disabled="scanning"
      @click="emit('analyze')"
    >
      {{ scanning ? 'Wird analysiert...' : `${images.length} ${images.length === 1 ? 'Bild' : 'Bilder'} analysieren` }}
    </button>
    <p v-if="error" class="scan-error" role="alert">{{ error }}</p>
    <button type="button" class="btn-manual" @click="emit('manual')">Stattdessen manuell eingeben</button>
  </section>
</template>

<script setup>
import AppIcon from '@/components/shell/AppIcon.vue'

defineProps({
  images: { type: Array, required: true },
  scanning: { type: Boolean, default: false },
  error: { type: String, default: '' },
})
const emit = defineEmits(['files', 'remove', 'analyze', 'manual'])

const onFiles = (event) => {
  const files = Array.from(event.target.files ?? [])
  event.target.value = ''
  if (files.length) emit('files', files)
}
</script>

<style scoped>
.import-section {
  background: var(--flaeche);
  border: 1px solid var(--linie);
  border-radius: var(--radius-karte);
  padding: 20px 16px;
  margin-bottom: 24px;
  text-align: center;
}

.import-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 650;
  color: var(--text);
}

.import-hint {
  margin: 4px 0 16px;
  font-size: 0.9rem;
  color: var(--text2);
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

.btn-scan {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  padding: 0 20px;
  background: var(--flaeche2);
  border: 1px solid var(--linie);
  color: var(--text);
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 600;
}

.btn-scan:hover {
  background: var(--linie);
}

.visually-hidden:focus-visible + .btn-scan {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}

.selected-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  justify-content: center;
}

.selected-image-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100px;
}

.selected-image-item img {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--linie);
}

.image-name {
  font-size: 0.75rem;
  color: var(--text2);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-remove-scan-image {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid var(--linie);
  background: var(--flaeche);
  color: var(--neg);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.btn-remove-scan-image:hover {
  background: var(--neg-weich);
}

/* Aktionsknopf des Import-Schritts: kräftiger als die übrigen, aber nicht orange gefüllt */
.btn-analyze {
  display: block;
  margin: 16px auto 0;
  min-height: 44px;
  padding: 0 24px;
  background: var(--flaeche);
  border: 1.5px solid var(--akzent);
  color: var(--akzent-text);
  border-radius: var(--radius);
  cursor: pointer;
  font: inherit;
  font-size: 0.95rem;
  font-weight: 700;
}

.btn-analyze:hover:not(:disabled) {
  background: var(--akzent-weich);
}

.btn-analyze:disabled {
  opacity: 0.6;
  cursor: wait;
}

.scan-error {
  margin: 12px 0 0;
  color: var(--neg);
  font-size: 0.9rem;
}

.btn-manual {
  display: block;
  margin: 18px auto 0;
  padding: 10px 12px;
  min-height: 44px;
  border: 0;
  background: none;
  color: var(--text2);
  font: inherit;
  font-size: 0.9rem;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

.btn-manual:hover {
  color: var(--text);
}

.btn-scan:focus-visible,
.btn-analyze:focus-visible,
.btn-manual:focus-visible,
.btn-remove-scan-image:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}
</style>
