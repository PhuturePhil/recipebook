<template>
  <!-- Ein Formular, zwei Einbauorte: im Abschnitt „Bewertungen“ (Karte mit Titel) und im Bewertungs-Dialog -->
  <form :class="['rating-form', { 'rating-form--dialog': dialog }]" novalidate @submit.prevent="submit">
    <h3 v-if="!dialog" class="rating-form__title">Rezept bewerten</h3>
    <RatingStars v-model="stars" :size="28" label="Sterne für dieses Rezept" :disabled="submitting" />
    <label class="rating-form__label" :for="commentId">Kommentar (optional)</label>
    <textarea
      :id="commentId"
      v-model="comment"
      class="rating-form__comment"
      rows="3"
      maxlength="2000"
      placeholder="Wie hat es geschmeckt? Was würdest du anders machen?"
      :disabled="submitting"
    ></textarea>
    <p v-if="error" class="rating-form__error" role="alert">{{ error }}</p>
    <div class="rating-form__actions">
      <button v-if="dialog" type="button" class="rating-form__cancel" @click="emit('cancel')">Abbrechen</button>
      <button type="submit" class="btn-rate" :disabled="!stars || submitting">
        {{ submitting ? 'Wird gespeichert…' : dialog ? 'Abschicken' : 'Bewertung abschicken' }}
      </button>
    </div>
  </form>
</template>

<script setup>
import { computed, ref } from 'vue'
import RatingStars from '@/components/RatingStars.vue'
import { recipeService } from '@/services/recipeService'
import { track } from '@/utils/analytics'

const props = defineProps({
  recipeId: { type: [Number, String], required: true },
  // Im Dialog: kein eigener Titel/Kartenrahmen, kurzes Label „Abschicken“ neben „Abbrechen“
  dialog: { type: Boolean, default: false },
})
const emit = defineEmits(['submitted', 'cancel'])

const stars = ref(0)
const comment = ref('')
const submitting = ref(false)
const error = ref(null)
// Beide Formulare können gleichzeitig im DOM stehen → eindeutige Feld-ID
const commentId = computed(() => (props.dialog ? 'rating-comment-dialog' : 'rating-comment'))

const submit = async () => {
  if (!stars.value || submitting.value) return
  submitting.value = true
  error.value = null
  try {
    await recipeService.addRating(props.recipeId, { stars: stars.value, comment: comment.value.trim() || null })
    track('bewertung_abgegeben', { recipeId: Number(props.recipeId), sterne: stars.value, mitKommentar: !!comment.value.trim() })
    stars.value = 0
    comment.value = ''
    emit('submitted')
  } catch (e) {
    error.value = e.message || 'Die Bewertung konnte nicht gespeichert werden.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.rating-form {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 14px 16px 16px;
  margin-bottom: 20px;
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--color-border, #ddd);
  border-radius: 12px;
}

.rating-form--dialog {
  padding: 0;
  margin: 12px 0 0;
  background: none;
  border: 0;
  border-radius: 0;
}

.rating-form__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 650;
  color: var(--color-text-primary, #333);
}

.rating-form .rating-stars--input {
  margin-left: -8px;
}

.rating-form__label {
  font-size: 0.85rem;
  color: var(--color-text-secondary, #666);
}

.rating-form__comment {
  box-sizing: border-box;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 8px;
  background: var(--color-bg, #fff);
  color: var(--color-text-primary, #333);
  font: inherit;
  font-size: 16px;
  resize: vertical;
}

.rating-form__comment:focus {
  outline: 2px solid var(--akzent);
  outline-offset: 1px;
}

.rating-form__error {
  margin: 0;
  color: var(--color-error, #e53e3e);
}

.btn-rate {
  min-height: 44px;
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  background: var(--color-primary, #4a5568);
  color: var(--akzent-kontrast);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.btn-rate:disabled {
  opacity: 0.45;
  cursor: default;
}

/* Dialog: Knöpfe rechtsbündig wie im ConfirmDialog (Abbrechen + oranger Primärknopf) */
.rating-form--dialog .rating-form__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  align-self: stretch;
  margin-top: 12px;
}

.rating-form__cancel,
.rating-form--dialog .btn-rate {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.rating-form__cancel {
  border: 1px solid var(--linie);
  background: var(--flaeche2);
  color: var(--text);
}

.rating-form__cancel:hover {
  background: var(--linie);
}

.rating-form--dialog .btn-rate {
  border: 0;
  background: var(--akzent);
  color: var(--akzent-kontrast);
}

.rating-form--dialog .btn-rate:disabled {
  cursor: default;
}

.rating-form__cancel:focus-visible,
.btn-rate:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
}
</style>
