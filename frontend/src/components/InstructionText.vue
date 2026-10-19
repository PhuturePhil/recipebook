<template>
  <template v-for="(segment, i) in segments" :key="i">
    <template v-if="!segment.refs">{{ segment.text }}</template>
    <button
      v-else
      :ref="(el) => setAnchor(i, el)"
      type="button"
      class="ingredient-ref"
      :aria-label="refLabel(segment)"
      :aria-expanded="isOpen(i)"
      :aria-describedby="isOpen(i) ? popoverId : undefined"
      @click.stop="context.state.click(keyOf(i))"
      @pointerenter="(e) => e.pointerType === 'mouse' && context.state.hover(keyOf(i))"
      @pointerleave="(e) => e.pointerType === 'mouse' && context.state.leave(keyOf(i))"
      @focus="(e) => focusVisible(e) && context.state.focus(keyOf(i))"
      @blur="context.state.blur(keyOf(i))"
    >{{ segment.text }}</button>
  </template>
  <Teleport to="body">
    <span
      v-if="openSegment"
      :id="popoverId"
      ref="popoverEl"
      role="tooltip"
      :class="['ingredient-popover', { 'ingredient-popover--up': placement?.up }]"
      :style="popoverStyle"
    >
      <span v-for="(line, l) in lines" :key="l" class="ingredient-popover__line">
        <span :class="['ingredient-popover__amount', { 'ingredient-popover__amount--none': !line.hasAmount }]">{{ line.amount }}</span>
        <span class="ingredient-popover__name">{{ line.name }}<template v-if="line.group"> ({{ line.group }})</template></span>
      </span>
    </span>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { popoverPlacement, refLabel } from '@/utils/ingredientRefs'

const props = defineProps({
  text: { type: String, default: '' },
  step: { type: Number, required: true },
  context: { type: Object, required: true },
})

const segments = computed(() => props.context.segmentsFor(props.text))
const keyOf = (i) => `${props.step}:${i}`
const isOpen = (i) => props.context.openKey.value === keyOf(i)
const popoverId = computed(() => `ingredient-popover-${props.step}`)
const openIndex = computed(() => {
  const [step, index] = String(props.context.openKey.value ?? '').split(':').map(Number)
  return step === props.step ? index : null
})
const openSegment = computed(() => (openIndex.value == null ? null : segments.value[openIndex.value]?.refs && segments.value[openIndex.value]))
const lines = computed(() => (openSegment.value ? props.context.linesFor(openSegment.value.refs) : []))

const focusVisible = (event) => {
  try {
    return event.target.matches(':focus-visible')
  } catch {
    return true
  }
}

const anchors = {}
const setAnchor = (i, el) => {
  if (el) anchors[i] = el
  else delete anchors[i]
}
const popoverEl = ref(null)
const placement = ref(null)
const popoverStyle = computed(() =>
  placement.value ? { top: `${placement.value.top}px`, left: `${placement.value.left}px` } : { visibility: 'hidden' }
)

watch(openIndex, async (index) => {
  placement.value = null
  if (index == null) return
  await nextTick()
  const anchor = anchors[index]
  const el = popoverEl.value
  if (!anchor || !el) return
  const rect = el.getBoundingClientRect()
  placement.value = popoverPlacement({
    anchor: anchor.getBoundingClientRect(),
    width: rect.width,
    height: rect.height,
    viewportWidth: document.documentElement.clientWidth,
    viewportHeight: window.innerHeight,
  })
}, { flush: 'post' })
</script>

<style scoped>
.ingredient-ref {
  display: inline;
  margin: -0.5em 0;
  padding: 0.5em 0;
  border: 0;
  background: none;
  font: inherit;
  line-height: inherit;
  color: inherit;
  text-align: inherit;
  cursor: pointer;
  text-decoration: underline dotted;
  text-decoration-color: color-mix(in srgb, var(--akzent) 70%, transparent);
  text-decoration-thickness: 2px;
  text-underline-offset: 4px;
  -webkit-tap-highlight-color: transparent;
  -webkit-box-decoration-break: clone;
  box-decoration-break: clone;
}

.ingredient-ref:hover,
.ingredient-ref[aria-expanded='true'] {
  text-decoration-color: var(--akzent);
  text-decoration-style: solid;
}

.ingredient-ref:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 2px;
  border-radius: 4px;
}

.ingredient-popover {
  position: fixed;
  z-index: 1300;
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: min(320px, calc(100vw - 16px));
  padding: 10px 14px;
  border: 1px solid var(--linie);
  border-radius: var(--radius, 12px);
  background: var(--flaeche);
  color: var(--text);
  box-shadow: var(--schatten-dialog);
  font-size: 15px;
  line-height: 1.35;
}

.ingredient-popover__line {
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.ingredient-popover__amount {
  flex: none;
  font-weight: 700;
  color: var(--akzent-text);
  font-variant-numeric: tabular-nums;
}

.ingredient-popover__amount--none {
  font-weight: 600;
  color: var(--text2);
}

.ingredient-popover__name {
  color: var(--text);
}

@media print {
  .ingredient-ref {
    text-decoration: none;
  }

  .ingredient-popover {
    display: none;
  }
}
</style>
