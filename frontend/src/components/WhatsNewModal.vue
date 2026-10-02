<template>
  <div class="whatsnew-overlay" @click.self="emit('close')">
    <div class="whatsnew" role="dialog" aria-modal="true" aria-labelledby="whatsnew-title">
      <h2 id="whatsnew-title" class="whatsnew__title">{{ title }}</h2>
      <div class="whatsnew__body">
        <div v-for="entry in entries" :key="entry.version" class="changelog-entry uc-entry">
          <div class="changelog-header">
            <h3 class="changelog-title">{{ entry.titel }}</h3>
            <span class="changelog-date">{{ entry.datum }}</span>
          </div>
          <ul class="changelog-changes">
            <li v-for="(change, i) in entry.punkte" :key="i">{{ change }}</li>
          </ul>
        </div>
      </div>
      <button ref="btn" type="button" class="whatsnew__ok" @click="emit('close')">Alles klar</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

defineProps({
  title: { type: String, required: true },
  entries: { type: Array, required: true },
})
const emit = defineEmits(['close'])
const btn = ref(null)
let prevFocus = null
let prevOverflow = ''

const onKey = (e) => {
  if (e.key === 'Escape') { e.preventDefault(); emit('close') }
  else if (e.key === 'Tab') { e.preventDefault(); btn.value?.focus() }   // einziges Bedienelement
}

onMounted(() => {
  prevFocus = document.activeElement
  prevOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  document.addEventListener('keydown', onKey, true)
  btn.value?.focus()
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKey, true)
  document.body.style.overflow = prevOverflow
  if (prevFocus?.focus && document.contains(prevFocus)) prevFocus.focus()
})
</script>

<style scoped>
/* Optik wie die Seite „Neuerungen“ (ChangelogView), Farben über --uc-* (design-tokens.css) */
.whatsnew-overlay {
  position: fixed;
  inset: 0;
  background: var(--overlay);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: max(16px, var(--shell-top)) 16px 16px;
}

.whatsnew {
  background: var(--bg);
  border-radius: var(--radius-dialog);
  box-shadow: var(--schatten-dialog);
  width: 100%;
  max-width: 520px;
  max-height: min(85vh, 720px);
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.whatsnew__title {
  font-size: 1.5rem;
  color: var(--uc-text);
  padding: 20px 20px 12px;
}

.whatsnew__body {
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.changelog-entry {
  background: var(--uc-surface);
  border: 1px solid var(--uc-border);
  border-radius: var(--radius-karte);
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.changelog-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--uc-border);
}

.changelog-title {
  font-size: 1.05rem;
  font-weight: 650;
  color: var(--akzent-text);
  margin: 0;
}

.changelog-date {
  font-size: 0.875rem;
  color: var(--uc-muted);
  white-space: nowrap;
}

.changelog-changes {
  margin: 0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.changelog-changes li {
  color: var(--uc-text);
  line-height: 1.5;
}

.changelog-changes li::marker {
  color: var(--uc-accent);
}

.whatsnew__ok {
  margin: 16px 20px 20px;
  padding: 13px;
  border: none;
  border-radius: var(--radius);
  background: var(--uc-accent);
  color: var(--uc-accent-text);
  font-size: 1rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}
</style>
