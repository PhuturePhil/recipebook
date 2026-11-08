<template>
  <!-- Eintragskarte (Entwurf mock_plan_week.html .meal): Tippen öffnet das Rezept (regulär über die Rezept-ID, „nur über
       den Plan“ über die Eintrags-ID), Freitext öffnet nichts, ein Geist (mock_ghost.html) das Sheet „Nicht mehr
       verfügbar“; ··· rechts (44 px) verwaltet den Eintrag – Entfernen gibt es nur dort und im Geist-Sheet -->
  <div :class="['plan-meal', { 'plan-meal--ghost': ghost }]">
    <RouterLink v-if="path" :to="path" class="plan-meal__link">
      <PlanPhoto :url="entry.recipe.thumbUrl" />
      <div class="plan-mt">
        <h3>{{ title }}</h3>
        <p v-if="meta">{{ meta }}</p>
      </div>
    </RouterLink>
    <button
      v-else-if="ghost"
      type="button"
      class="plan-meal__link plan-meal__link--ghost"
      aria-haspopup="dialog"
      @click="emit('gone', entry, $event.currentTarget)"
    >
      <span class="plan-ghost-img" aria-hidden="true">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
          stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 10h16v6a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4v-6zM2.5 10h19M9 6.5c0-1.2 1-2 3-2s3 .8 3 2" />
        </svg>
      </span>
      <div class="plan-mt">
        <h3>{{ title }}</h3>
        <p>{{ meta }}</p>
      </div>
    </button>
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
import { entryMeta, entryPath, entryTitle, isGhost } from '@/utils/mealPlan'
import { planText as t } from '@/utils/planTexts'

const props = defineProps({
  entry: { type: Object, required: true },
  languagePref: { type: String, default: null },
})
const emit = defineEmits(['manage', 'gone'])
const title = computed(() => entryTitle(props.entry, props.languagePref))
const meta = computed(() => entryMeta(props.entry))
const path = computed(() => entryPath(props.entry))
const ghost = computed(() => isGhost(props.entry))
</script>
