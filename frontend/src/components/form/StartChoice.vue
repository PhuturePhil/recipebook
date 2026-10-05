<template>
  <div class="start-choice" role="group" aria-label="Wie möchtest du das Rezept anlegen?">
    <button type="button" class="start-card" @click="emit('choose', 'import')">
      <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
        <circle cx="12" cy="13" r="4"/>
      </svg>
      <span class="start-card-title">Aus Foto importieren</span>
      <span class="start-card-text">Fotos von Buchseite oder Rezept abfotografieren</span>
    </button>
    <button type="button" class="start-card" @click="emit('choose', 'form')">
      <AppIcon name="bearbeiten" :size="28" />
      <span class="start-card-title">Manuell eingeben</span>
      <span class="start-card-text">Alle Felder selbst ausfüllen</span>
    </button>
  </div>
</template>

<script setup>
import AppIcon from '@/components/shell/AppIcon.vue'

const emit = defineEmits(['choose'])
</script>

<style scoped>
.start-choice {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 24px;
}

@media (max-width: 480px) {
  .start-choice {
    grid-template-columns: 1fr;
  }
}

.start-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 22px 14px;
  background: var(--flaeche);
  border: 1px solid var(--linie);
  border-radius: var(--radius-karte);
  color: var(--text);
  font: inherit;
  cursor: pointer;
  text-align: center;
}

.start-card .icon,
.start-card .icon-slot {
  display: flex;
  color: var(--akzent-text);
}

.start-card:hover,
.start-card:focus-visible {
  background: var(--akzent-weich);
  border-color: var(--akzent);
  outline: none;
}

.start-card:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}

.start-card-title {
  font-weight: 650;
  font-size: 1.05rem;
}

.start-card-text {
  font-size: 0.875rem;
  color: var(--text2);
}
</style>
