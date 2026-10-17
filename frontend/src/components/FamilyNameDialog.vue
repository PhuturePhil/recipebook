<template>
  <!-- Ein Pflichtfeld: Familie gründen / umbenennen. Der Platzhalter erklärt den Freundeskreis-Fall. -->
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
      <p v-if="error || localError" class="name-dialog__error" role="alert">{{ error || localError }}</p>
    </form>
  </ConfirmDialog>
</template>

<script setup>
import { ref } from 'vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const props = defineProps({
  title: { type: String, required: true },
  confirmLabel: { type: String, required: true },
  initial: { type: String, default: '' },
  error: { type: String, default: '' },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['submit', 'cancel'])
const name = ref(props.initial)
const localError = ref('')

function submit() {
  if (props.busy) return
  const value = name.value.trim()
  localError.value = value ? '' : 'Bitte gib einen Namen ein.'
  if (value) emit('submit', value)
}
</script>
