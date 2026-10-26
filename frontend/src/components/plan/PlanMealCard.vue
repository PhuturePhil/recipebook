<template>
  <!-- Eintragskarte (Entwurf mock_plan_week.html .meal): Tippen öffnet das Rezept, Freitext öffnet nichts;
       ··· rechts (44 px) verwaltet den Eintrag – Entfernen gibt es nur dort -->
  <div class="plan-meal">
    <RouterLink v-if="entry.recipe" :to="recipePath(entry.recipe.id)" class="plan-meal__link">
      <PlanPhoto :url="entry.recipe.thumbUrl" />
      <div class="plan-mt">
        <h3>{{ title }}</h3>
        <p v-if="meta">{{ meta }}</p>
      </div>
    </RouterLink>
    <div v-else class="plan-meal__link plan-meal__link--text">
      <div class="plan-mt"><h3>{{ title }}</h3></div>
    </div>
    <button
      type="button"
      class="plan-meal__more"
      aria-haspopup="dialog"
      :aria-label="`${t('manage')}: ${title}`"
      @click="emit('manage', entry, $event.currentTarget)"
    >
      <AppIcon name="mehr" :size="20" />
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import PlanPhoto from './PlanPhoto.vue'
import { entryMeta, entryTitle } from '@/utils/mealPlan'
import { planText as t } from '@/utils/planTexts'
import { recipePath } from '@/router/paths.js'

const props = defineProps({
  entry: { type: Object, required: true },
  languagePref: { type: String, default: null },
})
const emit = defineEmits(['manage'])
const title = computed(() => entryTitle(props.entry, props.languagePref))
const meta = computed(() => entryMeta(props.entry))
</script>
