<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-content">
      <h3>Rezept teilen</h3>
      <p class="hint">
        Mit diesem Link können auch Personen ohne Konto Zutaten und Zubereitung sehen.
        Beschreibung und persönliche Notizen bleiben privat. Der Link ist 30 Tage gültig.
      </p>

      <div v-if="error" class="error-message">{{ error }}</div>

      <div v-if="loading" class="loading">Lädt...</div>

      <template v-else-if="shareLink">
        <div class="share-link-row">
          <input :value="shareLink.url" readonly class="share-link-input" ref="shareLinkInput" />
          <button @click="copyLink" class="btn-copy">{{ copied ? 'Kopiert!' : 'Link kopieren' }}</button>
        </div>
        <p class="expires">Gültig bis {{ formatDate(shareLink.expiresAt) }}</p>
        <div class="share-actions">
          <button @click="handleRevoke" :disabled="busy" class="btn-revoke">Widerrufen</button>
          <button @click="handleRegenerate" :disabled="busy" class="btn-secondary">Neuen Link erzeugen</button>
        </div>
      </template>

      <div v-else class="share-actions">
        <button @click="handleCreate" :disabled="busy" class="btn-primary">
          {{ busy ? 'Erstelle...' : 'Link erstellen' }}
        </button>
      </div>

      <div class="modal-actions">
        <button @click="emit('close')" class="btn-secondary">Schließen</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { shareService } from '@/services/shareService'

const props = defineProps({
  recipeId: {
    type: [Number, String],
    required: true,
  },
})

const emit = defineEmits(['close'])

const shareLink = ref(null)
const loading = ref(true)
const busy = ref(false)
const copied = ref(false)
const error = ref(null)
const shareLinkInput = ref(null)

onMounted(async () => {
  try {
    shareLink.value = await shareService.getShareLink(props.recipeId)
  } catch {
    error.value = 'Der Link konnte nicht geladen werden.'
  }
  loading.value = false
})

async function handleCreate() {
  busy.value = true
  error.value = null
  try {
    shareLink.value = await shareService.createShareLink(props.recipeId)
    copied.value = false
  } catch {
    error.value = 'Der Link konnte nicht erstellt werden.'
  }
  busy.value = false
}

async function handleRegenerate() {
  if (!confirm('Neuen Link erzeugen? Der bisherige Link funktioniert dann nicht mehr.')) return
  await handleCreate()
}

async function handleRevoke() {
  if (!confirm('Link widerrufen? Wer den Link hat, kann das Rezept dann nicht mehr öffnen.')) return
  busy.value = true
  error.value = null
  try {
    await shareService.revokeShareLink(props.recipeId)
    shareLink.value = null
  } catch {
    error.value = 'Der Link konnte nicht widerrufen werden.'
  }
  busy.value = false
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(shareLink.value.url)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    if (shareLinkInput.value) {
      shareLinkInput.value.select()
      document.execCommand('copy')
      copied.value = true
      setTimeout(() => { copied.value = false }, 2000)
    }
  }
}

function formatDate(value) {
  return new Date(value).toLocaleDateString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 16px;
}

.modal-content {
  background: var(--color-bg-card, #fff);
  border-radius: 8px;
  padding: 24px;
  width: 100%;
  max-width: 480px;
}

.modal-content h3 {
  margin-bottom: 12px;
  color: var(--color-text-primary, #333);
}

.hint {
  font-size: 0.875rem;
  color: var(--color-text-secondary, #666);
  margin-bottom: 16px;
}

.loading {
  color: var(--color-text-secondary, #666);
  padding: 12px 0;
}

.error-message {
  color: var(--color-error, #e53e3e);
  font-size: 0.875rem;
  margin-bottom: 12px;
}

.share-link-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.share-link-input {
  flex: 1;
  min-width: 0;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.875rem;
  color: #4a5568;
  background: #f7fafc;
}

.expires {
  font-size: 0.8rem;
  color: var(--color-text-muted, #999);
  margin-bottom: 16px;
}

.share-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn-copy,
.btn-primary,
.btn-secondary,
.btn-revoke {
  padding: 10px 16px;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 0.875rem;
}

.btn-copy,
.btn-primary {
  background: var(--color-primary, #4a5568);
  color: white;
  border: none;
}

.btn-copy:hover,
.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark, #2d3748);
}

.btn-secondary {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
  border: none;
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-border, #ddd);
}

.btn-revoke {
  background: transparent;
  color: var(--color-error, #e53e3e);
  border: 1px solid var(--color-error, #e53e3e);
}

.btn-revoke:hover:not(:disabled) {
  background: rgba(229, 62, 62, 0.1);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media print {
  .modal-overlay {
    display: none;
  }
}
</style>
