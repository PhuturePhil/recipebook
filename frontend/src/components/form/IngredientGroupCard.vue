<template>
  <div class="ingredient-group-card" :data-section="sectionId">
    <div class="group-head">
      <button
        type="button"
        class="drag-handle group-drag-handle"
        aria-label="Gruppe verschieben"
        title="Ziehen zum Verschieben"
        @keydown.up.prevent="emit('move', -1, $event)"
        @keydown.down.prevent="emit('move', 1, $event)"
      >
        <GripIcon />
      </button>
      <input
        :value="name"
        type="text"
        class="ingredient-group-input"
        placeholder="Gruppe, z. B. Salsa"
        :maxlength="MAX_GROUP_NAME"
        aria-label="Name der Zutatengruppe"
        @input="emit('update:name', $event.target.value)"
        @keydown.enter="emit('enter', $event)"
        @keydown.alt.up.prevent="emit('move', -1, $event)"
        @keydown.alt.down.prevent="emit('move', 1, $event)"
      />
      <button type="button" class="btn-remove-icon" title="Gruppe auflösen" aria-label="Gruppe auflösen, Zutaten bleiben" @click="emit('remove')">
        <AppIcon name="loeschen" :size="18" />
      </button>
    </div>
    <slot />
    <button type="button" class="btn-add group-add" @click="emit('add')">+ Zutat</button>
  </div>
</template>

<script setup>
import AppIcon from '@/components/shell/AppIcon.vue'
import GripIcon from '@/components/form/GripIcon.vue'
import { MAX_GROUP_NAME } from '@/utils/ingredientGroups'

// A group of ingredients as a card: head with handle, name and trash; the ingredient list comes in as slot
defineProps({
  name: { type: String, default: '' },
  sectionId: { type: String, required: true }
})

const emit = defineEmits(['update:name', 'move', 'remove', 'add', 'enter'])
</script>

<style scoped>
.ingredient-group-card {
  background: var(--flaeche);
  border: 1px solid var(--linie);
  border-radius: var(--radius-karte);
  padding: 8px 10px 10px;
  margin: 12px 0;
}

.group-head {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 44px;
  gap: 8px;
  align-items: center;
}

.group-head input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--linie);
  border-radius: 6px;
  font-size: 1rem;
  font-weight: 600;
}

.drag-handle,
.btn-remove-icon {
  width: 44px;
  height: 44px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.drag-handle {
  color: var(--text3);
  cursor: grab;
  touch-action: none;
}

.drag-handle:hover {
  color: var(--text2);
  background: var(--flaeche2);
}

.btn-remove-icon {
  color: var(--neg);
  cursor: pointer;
}

.btn-remove-icon:hover {
  background: var(--neg-weich);
}

.drag-handle:focus-visible,
.btn-remove-icon:focus-visible,
.group-add:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: 1px;
}

/* „+ Zutat“ im btn-add-Stil */
.group-add {
  margin: 6px 0 0 10px;
  padding: 8px 16px;
  min-height: 36px;
  border: none;
  border-radius: 6px;
  background: var(--flaeche2);
  color: var(--text);
  font-size: 0.875rem;
  cursor: pointer;
}

.group-add:hover {
  background: var(--linie);
}
</style>
