<template>
  <span
    v-if="readonly"
    class="rating-stars rating-stars--readonly"
    role="img"
    :aria-label="`${modelValue} von 5 Sternen`"
  >
    <span
      v-for="n in count"
      :key="n"
      :class="['star', { 'star--filled': n <= modelValue }]"
      v-html="starSvg"
    ></span>
  </span>
  <span v-else class="rating-stars rating-stars--input" role="radiogroup" :aria-label="label">
    <button
      v-for="n in 5"
      :key="n"
      type="button"
      role="radio"
      :class="['star-btn', { 'star--filled': n <= (hover || modelValue) }]"
      :aria-checked="n === modelValue"
      :aria-label="`${n} von 5 Sternen`"
      :tabindex="n === (modelValue || 1) ? 0 : -1"
      :disabled="disabled"
      @click="emit('update:modelValue', n)"
      @mouseenter="hover = n"
      @mouseleave="hover = 0"
      @keydown="onKey($event, n)"
      v-html="starSvg"
    ></button>
  </span>
</template>

<script setup>
import { computed, ref } from 'vue'
import '@/shell/app-icons.js'

// Ganze Sterne 1–5: Anzeige (readonly) oder Eingabe als Radiogruppe mit 44-px-Tippflächen.
// Linien-Icon „stern“ aus dem App-Icon-Set, gefüllt per CSS – keine Emoji-Sterne.
const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false },
  size: { type: Number, default: 16 },
  // Nur Anzeige: wie viele Sterne gezeichnet werden (1 = kompakter Einzelstern)
  count: { type: Number, default: 5 },
  label: { type: String, default: 'Sterne vergeben' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])
const hover = ref(0)

const starSvg = computed(() => (window.APP_ICONS ? window.APP_ICONS.svg('stern', props.size) : ''))

const onKey = (event, n) => {
  const step = { ArrowRight: 1, ArrowUp: 1, ArrowLeft: -1, ArrowDown: -1 }[event.key]
  if (!step) return
  event.preventDefault()
  const next = Math.min(5, Math.max(1, n + step))
  emit('update:modelValue', next)
  const buttons = event.currentTarget.parentElement?.querySelectorAll('button')
  buttons?.[next - 1]?.focus()
}
</script>

<style scoped>
.rating-stars {
  display: inline-flex;
  align-items: center;
  line-height: 0;
}

.rating-stars--readonly {
  gap: 1px;
}

.star {
  display: inline-flex;
  color: var(--text3);
}

.star--filled {
  color: var(--akzent);
}

.star--filled :deep(svg) {
  fill: currentColor;
}

.star-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  min-height: 44px;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: none;
  color: var(--text3);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.star-btn.star--filled {
  color: var(--akzent);
}

.star-btn:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: -2px;
}

.star-btn:disabled {
  cursor: default;
  opacity: 0.6;
}
</style>
