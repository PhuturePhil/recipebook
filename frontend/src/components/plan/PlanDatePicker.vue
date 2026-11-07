<template>
  <!-- Datumswahl (Planen vom Rezept, Verschieben): Schnellwahl Heute/Morgen + Mini-Kalender, Tippen wählt sofort.
       Tage mit Einträgen der Familie tragen einen Akzent-Punkt (07.11.2026), bleiben aber wählbar. -->
  <div class="plan-sheet__body plan-mini">
    <div class="plan-quick">
      <button
        v-for="q in quick"
        :key="q.key"
        type="button"
        :aria-label="q.booked ? `${t(q.key)} · ${t('booked')}` : undefined"
        :disabled="busy"
        @click="emit('pick', q.date)"
      >
        {{ t(q.key) }}<i v-if="q.booked" class="plan-booked-dot" aria-hidden="true"></i>
      </button>
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
          :class="['plan-mini__day', {
            today: sameDay(cell.date, today),
            sel: sameDay(cell.date, selected),
            booked: isBooked(cell.date),
          }]"
          :aria-label="dayLabel(cell.date) + (sameDay(cell.date, today) ? ` · ${t('today')}` : '')
            + (isBooked(cell.date) ? ` · ${t('booked')}` : '')"
          :aria-current="sameDay(cell.date, today) ? 'date' : undefined"
          :aria-pressed="sameDay(cell.date, selected) ? 'true' : undefined"
          :disabled="busy"
          @click="emit('pick', cell.date)"
        >{{ cell.date.getDate() }}<i v-if="isBooked(cell.date)" class="plan-booked-dot" aria-hidden="true"></i></button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import { mealPlanService } from '@/services/mealPlanService'
import {
  addDays, addMonths, bookedDaysLoader, dayLabel, dayOnly, isoDate, monthGrid, monthLabel, monthStart, sameDay,
  weekDays, weekdayShort,
} from '@/utils/mealPlan'
import { planText as t } from '@/utils/planTexts'

const props = defineProps({
  // Vorauswahl (Verschieben: bisheriges Datum)
  selected: { type: Date, default: null },
  busy: { type: Boolean, default: false },
  // Familie, deren belegte Tage markiert werden (ohne: keine Markierung)
  familyId: { type: Number, default: null },
})
const emit = defineEmits(['pick'])
const today = dayOnly(new Date())
const tomorrow = addDays(today, 1)
const month = ref(monthStart(props.selected ?? today))
const cells = computed(() => monthGrid(month.value))

const booked = ref(new Set())
const isBooked = (d) => booked.value.has(isoDate(d))
const quick = computed(() => [
  { key: 'today', date: today, booked: isBooked(today) },
  { key: 'tomorrow', date: tomorrow, booked: isBooked(tomorrow) },
])

let load = null
async function markMonth(m) {
  const current = load
  if (!current) return
  const dates = await current(m)
  if (current === load && dates.length) booked.value = new Set([...booked.value, ...dates])
}

watch(() => props.familyId, (id) => {
  booked.value = new Set()
  load = id == null ? null : bookedDaysLoader((from, to) => mealPlanService.list(id, from, to))
  markMonth(month.value)
  // Heute/Morgen gehören evtl. zu einem anderen Monat als der angezeigte
  markMonth(today)
  markMonth(tomorrow)
}, { immediate: true })
watch(month, markMonth)
</script>
