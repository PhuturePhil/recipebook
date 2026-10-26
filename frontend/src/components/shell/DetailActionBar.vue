<template>
  <!-- Ersetzt auf der Rezeptseite mobil die Tab-Leiste: dieselbe BottomBar wie die Tab-Leiste – Bearbeiten, Planen,
       Bewerten (orange), ganz rechts „Mehr“ mit „Rezept löschen…“. Ab 1024 px gibt es die Leiste nicht. -->
  <BottomBar
    ref="bar"
    class="detail-action-bar"
    label="Rezept-Aktionen"
    :items="actions.bar"
    :menu="actions.menu"
    @select="(id) => emit(id)"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import BottomBar from './BottomBar.vue'
import { detailActions } from '@/utils/detailActions'

const props = defineProps({
  canEdit: { type: Boolean, default: false },
})
const emit = defineEmits(['delete', 'edit', 'plan', 'rate'])
const actions = computed(() => detailActions(props.canEdit))
const bar = ref(null)

defineExpose({ focus: (id) => bar.value?.focus(id) })
</script>
