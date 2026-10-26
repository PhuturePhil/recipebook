<template>
  <!-- Datumswahl (Planen vom Rezept, Verschieben): Schnellwahl Heute/Morgen + Mini-Kalender, Tippen wählt sofort -->
  <div class="plan-sheet__body plan-mini">
    <div class="plan-quick">
      <button type="button" :disabled="busy" @click="emit('pick', today)">{{ t('today') }}</button>
      <button type="button" :disabled="busy" @click="emit('pick', addDays(today, 1))">{{ t('tomorrow') }}</button>
    </div>
    <div class="plan-mini__nav">
      <button type="button" :aria-label="t('prevMonth')" @click="month = addMonths(month, -1)">
        <AppIcon name="zurueck" :size="18" />
      </button>
      <b aria-live="polite">{{ monthLabel(month) }}</b>
      <button type="button" :aria-label="t('nextMonth')" @click="month = addMonths(month, 1)">
        <AppIcon name="weiter" :size="18" />
      </button>
    </div>
    <div class="plan-wdh" aria-hidden="true">
      <span v-for="d in weekDays(today)" :key="d.getDay()">{{ weekdayShort(d) }}</span>
    </div>
    <div class="plan-mini__grid">
      <template v-for="(cell, i) in cells" :key="i">
        <span v-if="!cell.date || !cell.inMonth" aria-hidden="true"></span>
        <button
          v-else
          type="button"
          :class="['plan-mini__day', { today: sameDay(cell.date, today), sel: sameDay(cell.date, selected) }]"
          :aria-label="dayLabel(cell.date) + (sameDay(cell.date, today) ? ` · ${t('today')}` : '')"
          :aria-current="sameDay(cell.date, today) ? 'date' : undefined"
          :aria-pressed="sameDay(cell.date, selected) ? 'true' : undefined"
          :disabled="busy"
          @click="emit('pick', cell.date)"
        >{{ cell.date.getDate() }}</button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import {
  addDays, addMonths, dayLabel, dayOnly, monthGrid, monthLabel, monthStart, sameDay, weekDays, weekdayShort,
} from '@/utils/mealPlan'
import { planText as t } from '@/utils/planTexts'

const props = defineProps({
  // Vorauswahl (Verschieben: bisheriges Datum)
  selected: { type: Date, default: null },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['pick'])
const today = dayOnly(new Date())
const month = ref(monthStart(props.selected ?? today))
const cells = computed(() => monthGrid(month.value))
</script>
