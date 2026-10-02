<template>
  <div class="tag-input">
    <div class="tag-input__chips">
      <span v-for="(tag, index) in modelValue" :key="tag" class="tag-input__chip">
        {{ tag }}
        <button type="button" class="tag-input__remove" :aria-label="`${tag} entfernen`" @click="remove(index)">
          &times;
        </button>
      </span>
      <div v-if="modelValue.length < MAX_TAGS" class="tag-input__field">
        <input
          id="tags"
          v-model="query"
          type="text"
          autocomplete="off"
          :placeholder="modelValue.length ? 'Weiteren Tag hinzufügen' : 'Leer lassen: Tags werden automatisch vergeben'"
          @focus="open = true"
          @blur="onBlur"
          @keydown="onKeydown"
        />
        <ul v-if="open && suggestions.length" class="tag-input__dropdown">
          <li
            v-for="(item, index) in suggestions"
            :key="item"
            :class="{ 'tag-input__option--active': index === highlighted }"
            @mousedown.prevent="add(item)"
          >{{ item }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { MAX_TAGS, addTag, tagSuggestions } from '@/utils/recipeTags'
import { moveHighlight } from '@/utils/ingredientSuggest'

const props = defineProps({
  modelValue: {
    type: Array,
    required: true
  },
  known: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const query = ref('')
const open = ref(false)
const highlighted = ref(-1)

const suggestions = computed(() => tagSuggestions(props.known, props.modelValue, query.value))

watch(query, () => { highlighted.value = -1 })

const add = (value) => {
  const tags = [...props.modelValue]
  if (addTag(tags, value, props.known)) emit('update:modelValue', tags)
  query.value = ''
}

const remove = (index) => {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== index))
}

const onBlur = () => {
  open.value = false
  if (query.value.trim()) add(query.value)
}

const onKeydown = (event) => {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    open.value = true
    highlighted.value = moveHighlight(highlighted.value, event.key === 'ArrowDown' ? 1 : -1, suggestions.value.length)
  } else if (event.key === 'Enter' || event.key === ',') {
    event.preventDefault()
    const choice = highlighted.value >= 0 ? suggestions.value[highlighted.value] : query.value
    if (String(choice ?? '').trim()) add(choice)
  } else if (event.key === 'Backspace' && !query.value && props.modelValue.length) {
    remove(props.modelValue.length - 1)
  } else if (event.key === 'Escape') {
    open.value = false
  }
}
</script>

<style scoped>
.tag-input__chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 6px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  background: var(--flaeche);
}

.tag-input__chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px 4px 10px;
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
  border-radius: 999px;
  font-size: 0.875rem;
}

.tag-input__remove {
  border: none;
  background: none;
  padding: 0 4px;
  font-size: 1rem;
  line-height: 1;
  color: var(--color-text-muted, #999);
  cursor: pointer;
}

.tag-input__remove:hover {
  color: var(--color-text-primary, #333);
}

.tag-input__field {
  position: relative;
  flex: 1;
  min-width: 12rem;
}

.tag-input__field input {
  width: 100%;
  padding: 6px 4px;
  border: none;
  font-size: 0.95rem;
  font-family: inherit;
  box-sizing: border-box;
}

.tag-input__field input:focus {
  outline: none;
}

.tag-input__dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 20;
  min-width: 12rem;
  max-height: 240px;
  overflow-y: auto;
  margin: 4px 0 0;
  padding: 4px 0;
  list-style: none;
  background: var(--flaeche);
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.tag-input__dropdown li {
  padding: 8px 12px;
  cursor: pointer;
}

.tag-input__dropdown li:hover,
.tag-input__option--active {
  background: var(--color-bg-secondary, #f0f0f0);
}
</style>
