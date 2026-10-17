<template>
  <!-- Ein Pflichtfeld: Familie gründen / umbenennen. Der Platzhalter erklärt den Freundeskreis-Fall.
       Beim Gründen (shareCount > 0) zusätzlich das Häkchen „Alle meine Rezepte freigeben“, vorab gesetzt. -->
  <ConfirmDialog :title="title" :confirm-label="confirmLabel" @confirm="submit" @cancel="emit('cancel')">
    <form @submit.prevent="submit">
      <input
        v-model="name"
        type="text"
        class="ui-input name-dialog__input"
        maxlength="60"
        autocomplete="off"
        aria-label="Name der Familie"
        placeholder="z. B. Familie Pastoors oder Kochclub Köln"
        :disabled="busy"
      />
      <label v-if="shareCount > 0" class="vis-fam name-dialog__share">
        <input v-model="shareOwn" type="checkbox" class="name-dialog__check" :disabled="busy" />
        <span :class="['vis-check', { 'vis-check--on': shareOwn }]" aria-hidden="true">
          <AppIcon name="haken" :size="13" />
        </span>
        <span>
          {{ shareOwnLabel(shareCount) }}
          <span class="name-dialog__hint">Auch private – das lässt sich später je Rezept ändern.</span>
        </span>
      </label>
      <p v-if="error || localError" class="name-dialog__error" role="alert">{{ error || localError }}</p>
    </form>
  </ConfirmDialog>
</template>

<script setup>
import { ref } from 'vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import { shareOwnLabel } from '@/utils/families'

const props = defineProps({
  title: { type: String, required: true },
  confirmLabel: { type: String, required: true },
  initial: { type: String, default: '' },
  error: { type: String, default: '' },
  busy: { type: Boolean, default: false },
  // Anzahl eigener Rezepte; 0 = kein Häkchen (Umbenennen, oder es gibt nichts mitzunehmen)
  shareCount: { type: Number, default: 0 },
})
const emit = defineEmits(['submit', 'cancel'])
const name = ref(props.initial)
const localError = ref('')
const shareOwn = ref(true)

function submit() {
  if (props.busy) return
  const value = name.value.trim()
  localError.value = value ? '' : 'Bitte gib einen Namen ein.'
  if (value) emit('submit', value, { shareOwnRecipes: props.shareCount > 0 && shareOwn.value })
}
</script>
