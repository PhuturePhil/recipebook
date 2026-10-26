<template>
  <!-- Essensplan der Familie (Entwürfe media/out/mock_plan_week.html, mock_plan_month.html, mock_plan_monat_liste.html).
       Ansicht und Zeitraum stehen in der Adresse (?ansicht=monat&datum=…), damit „Zurück“ vom Rezept wieder hierher führt. -->
  <div class="plan-view">
    <header class="plan-head">
      <h1>{{ t('title') }}</h1>
      <div class="plan-head__tools">
        <button v-if="!current" type="button" class="plan-pill plan-pill--today" @click="goToday">
          <span>{{ t('today') }}</span>
        </button>
        <button
          v-if="families.length > 1 && family"
          type="button"
          class="plan-pill"
          aria-haspopup="dialog"
          :aria-label="`${t('family')}: ${family.name}`"
          @click="openSheet({ type: 'family' }, $event.currentTarget)"
        >
          <span><AppIcon name="familie" :size="16" />{{ shortFamilyName(family.name) }}<AppIcon name="runter" :size="14" /></span>
        </button>
      </div>
    </header>

    <div class="plan-seg" role="group" :aria-label="t('views')">
      <button
        v-for="v in VIEWS"
        :key="v"
        type="button"
        :aria-pressed="view === v ? 'true' : 'false'"
        @click="setView(v)"
      >{{ t(VIEW_LABELS[v]) }}</button>
    </div>

    <div class="plan-nav">
      <button type="button" class="plan-arr" :aria-label="t(view === 'woche' ? 'prevWeek' : 'prevMonth')" @click="shift(-1)">
        <span><AppIcon name="zurueck" :size="16" /></span>
      </button>
      <div class="plan-nav__lbl" aria-live="polite">
        <template v-if="view === 'woche'">
          <b>{{ weekLabel(anchor) }}</b><i>{{ weekSubLabel(anchor, today) }}</i>
        </template>
        <b v-else>{{ monthLabel(anchor) }}</b>
      </div>
      <button type="button" class="plan-arr" :aria-label="t(view === 'woche' ? 'nextWeek' : 'nextMonth')" @click="shift(1)">
        <span><AppIcon name="weiter" :size="16" /></span>
      </button>
    </div>

    <div v-if="noFamily" class="plan-state">
      <p>{{ t('noFamily') }}</p>
      <RouterLink to="/familien" class="plan-state__btn">{{ t('toFamilies') }}</RouterLink>
    </div>

    <div v-else-if="error" class="plan-state" role="alert">
      <p>{{ t('loadError') }}</p>
      <button type="button" class="plan-state__btn" @click="load">{{ t('retry') }}</button>
    </div>

    <div v-else-if="familyId != null" :class="{ 'plan-loading': loading }" :aria-busy="loading ? 'true' : 'false'">
      <!-- Woche: Mo–So als Zeilen -->
      <template v-if="view === 'woche'">
        <div v-for="day in week" :key="isoDate(day)" :class="['plan-day', { 'is-today': sameDay(day, today) }]">
          <div class="plan-dcol" :aria-label="dayLabel(day)">
            <b aria-hidden="true">{{ weekdayShort(day) }}</b><em aria-hidden="true">{{ day.getDate() }}</em>
          </div>
          <div class="plan-dmeals">
            <PlanMealCard
              v-for="entry in entriesOn(day)"
              :key="entry.id"
              :entry="entry"
              :language-pref="languagePref"
              @manage="onManage"
            />
            <button
              v-if="!entriesOn(day).length"
              type="button"
              class="plan-empty"
              :aria-label="`${t('planDish')} · ${dayLabel(day)}`"
              @click="openPicker(day, $event.currentTarget)"
            >
              <AppIcon name="plus" :size="15" />{{ t('planDish') }}
            </button>
          </div>
        </div>
      </template>

      <!-- Monat: Raster, darunter der gewählte Tag -->
      <template v-else-if="view === 'monat'">
        <div class="plan-cal">
          <div class="plan-wdh" aria-hidden="true">
            <span v-for="d in week" :key="d.getDay()">{{ weekdayShort(d) }}</span>
          </div>
          <div class="plan-grid">
            <template v-for="(cell, i) in grid" :key="i">
              <div v-if="!cell.date" class="plan-d plan-d--blank" aria-hidden="true"></div>
              <div v-else-if="!cell.inMonth" class="plan-d off" aria-hidden="true"><i>{{ cell.date.getDate() }}</i></div>
              <button
                v-else
                type="button"
                :class="['plan-d', {
                  has: !!dayPhoto(entriesOn(cell.date)),
                  sel: sameDay(cell.date, selectedDay),
                  today: sameDay(cell.date, today),
                }]"
                :aria-label="cellLabel(cell.date)"
                :aria-pressed="sameDay(cell.date, selectedDay) ? 'true' : 'false'"
                :aria-current="sameDay(cell.date, today) ? 'date' : undefined"
                @click="selectedDay = cell.date"
              >
                <template v-if="dayPhoto(entriesOn(cell.date))">
                  <PlanPhoto :url="dayPhoto(entriesOn(cell.date))" cls="plan-d__img" />
                  <span class="sc"></span>
                </template>
                <span v-else-if="entriesOn(cell.date).length" class="plan-d__dot"></span>
                <i>{{ cell.date.getDate() }}</i>
              </button>
            </template>
          </div>
        </div>
        <template v-if="selectedDay">
          <div class="plan-selrow">
            <b>{{ dayLabel(selectedDay) }}</b><i v-if="sameDay(selectedDay, today)">· {{ t('today') }}</i>
          </div>
          <div class="plan-selmeals">
            <PlanMealCard
              v-for="entry in entriesOn(selectedDay)"
              :key="entry.id"
              :entry="entry"
              :language-pref="languagePref"
              @manage="onManage"
            />
            <button
              v-if="!entriesOn(selectedDay).length"
              type="button"
              class="plan-empty"
              :aria-label="`${t('planDish')} · ${dayLabel(selectedDay)}`"
              @click="openPicker(selectedDay, $event.currentTarget)"
            >
              <AppIcon name="plus" :size="15" />{{ t('planDish') }}
            </button>
          </div>
        </template>
      </template>

      <!-- Liste: alle Tage des Monats ohne Foto -->
      <div v-else class="plan-list">
        <template v-for="day in monthDaysList" :key="isoDate(day)">
          <button
            v-if="!entriesOn(day).length"
            type="button"
            :class="['plan-r', 'leer', { we: isWeekend(day), today: sameDay(day, today) }]"
            :aria-label="`${dayLabel(day)} · ${t('nothingPlanned')} · ${t('planDish')}`"
            @click="openPicker(day, $event.currentTarget)"
          >
            <span class="d">{{ weekdayShort(day) }} <b>{{ day.getDate() }}</b></span>
            <h3 aria-hidden="true">–</h3>
            <span v-if="sameDay(day, today)" class="plan-hb">{{ t('today') }}</span>
          </button>
          <template v-else>
          <template v-for="(entry, n) in entriesOn(day)" :key="entry.id">
            <RouterLink
              v-if="entry.recipe"
              :to="recipePath(entry.recipe.id)"
              :class="['plan-r', { we: isWeekend(day), today: sameDay(day, today) }]"
            >
              <span class="d" :aria-label="dayLabel(day)"><template v-if="n === 0">{{ weekdayShort(day) }} <b>{{ day.getDate() }}</b></template></span>
              <h3>{{ entryTitle(entry, languagePref) }}</h3>
              <span v-if="n === 0 && sameDay(day, today)" class="plan-hb">{{ t('today') }}</span>
              <span class="plan-r__chev" aria-hidden="true"><AppIcon name="weiter" :size="14" /></span>
            </RouterLink>
            <div v-else :class="['plan-r', { we: isWeekend(day), today: sameDay(day, today) }]">
              <span class="d" :aria-label="dayLabel(day)"><template v-if="n === 0">{{ weekdayShort(day) }} <b>{{ day.getDate() }}</b></template></span>
              <h3>{{ entry.freeText }}</h3>
              <span v-if="n === 0 && sameDay(day, today)" class="plan-hb">{{ t('today') }}</span>
            </div>
          </template>
          </template>
        </template>
      </div>
    </div>

    <!-- Picker „Gericht planen“ -->
    <PlanSheet
      v-if="sheet?.type === 'picker'"
      :title="t('pickerTitle')"
      :subtitle="t('pickerFor', { day: dayLabel(sheet.date) })"
      :return-focus="sheet.returnFocus"
      initial-focus=".plan-picker__search"
      @close="closeSheet"
    >
      <PlanPicker :family-id="familyId" :busy="busy" @pick="addEntry" />
    </PlanSheet>

    <!-- Eintrag verwalten: Weiteres Gericht, Verschieben, Entfernen (nur hier) -->
    <PlanSheet
      v-else-if="sheet?.type === 'manage'"
      :title="entryTitle(sheet.entry, languagePref)"
      :subtitle="dayLabel(fromIso(sheet.entry.date))"
      :return-focus="sheet.returnFocus"
      @close="closeSheet"
    >
      <div class="plan-sheet__body">
        <button type="button" class="plan-mi" @click="openPicker(fromIso(sheet.entry.date), sheet.returnFocus)">
          <AppIcon name="plus" :size="21" />{{ t('addAnother') }}
        </button>
        <button type="button" class="plan-mi" @click="openSheet({ type: 'move', entry: sheet.entry }, sheet.returnFocus)">
          <AppIcon name="kalender" :size="21" />{{ t('move') }}
        </button>
        <button type="button" class="plan-mi plan-mi--danger" :disabled="busy" @click="removeEntry(sheet.entry)">
          <AppIcon name="loeschen" :size="21" />{{ t('remove') }}
        </button>
      </div>
    </PlanSheet>

    <PlanSheet
      v-else-if="sheet?.type === 'move'"
      :title="t('moveTitle')"
      :subtitle="entryTitle(sheet.entry, languagePref)"
      :return-focus="sheet.returnFocus"
      @close="closeSheet"
    >
      <PlanDatePicker :selected="fromIso(sheet.entry.date)" :busy="busy" @pick="moveEntry(sheet.entry, $event)" />
    </PlanSheet>

    <PlanSheet
      v-else-if="sheet?.type === 'family'"
      :title="t('chooseFamily')"
      :return-focus="sheet.returnFocus"
      @close="closeSheet"
    >
      <div class="plan-sheet__body">
        <button
          v-for="f in families"
          :key="f.id"
          type="button"
          class="plan-mi"
          :aria-pressed="f.id === familyId ? 'true' : 'false'"
          @click="chooseFamily(f.id)"
        >
          <AppIcon name="familie" :size="21" />{{ f.name }}
          <AppIcon v-if="f.id === familyId" name="haken" :size="18" class="plan-mi__check" />
        </button>
      </div>
    </PlanSheet>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/shell/AppIcon.vue'
import PlanMealCard from '@/components/plan/PlanMealCard.vue'
import PlanPhoto from '@/components/plan/PlanPhoto.vue'
import PlanSheet from '@/components/plan/PlanSheet.vue'
import PlanPicker from '@/components/plan/PlanPicker.vue'
import PlanDatePicker from '@/components/plan/PlanDatePicker.vue'
import { useFamilyStore, shortFamilyName } from '@/stores/familyStore'
import { useUiStore } from '@/stores/uiStore'
import { mealPlanService } from '@/services/mealPlanService'
import { readLanguagePreference } from '@/utils/recipeLanguage'
import { recipePath } from '@/router/paths.js'
import { planText as t } from '@/utils/planTexts'
import {
  VIEWS, isoDate, fromIso, dayOnly, sameDay, sameMonth, monthStart, isWeekend, weekDays, monthDays, monthGrid,
  rangeFor, shiftAnchor, isCurrentPeriod, weekLabel, weekSubLabel, monthLabel, dayLabel, weekdayShort,
  groupByDate, entryTitle, insertEntry, dayPhoto,
} from '@/utils/mealPlan'

const VIEW_LABELS = { woche: 'week', monat: 'month', liste: 'list' }
const FAMILY_KEY = 'mealPlan:familie'

const route = useRoute()
const router = useRouter()
const familyStore = useFamilyStore()
const uiStore = useUiStore()

const today = ref(dayOnly(new Date()))
const languagePref = ref(readLanguagePreference())

const view = computed(() => (VIEWS.includes(route.query.ansicht) ? route.query.ansicht : 'woche'))
const anchor = computed(() => fromIso(route.query.datum) ?? today.value)
const current = computed(() => isCurrentPeriod(view.value, anchor.value, today.value))

function setQuery(patch) {
  const query = { ...route.query, ...patch }
  Object.keys(query).forEach((k) => query[k] === undefined && delete query[k])
  router.replace({ query })
}

// Aktueller Zeitraum ohne Datum in der Adresse
const datumFor = (v, d) => (isCurrentPeriod(v, d, today.value) ? undefined : isoDate(d))
const setView = (v) => setQuery({ ansicht: v === 'woche' ? undefined : v, datum: datumFor(v, anchor.value) })
const shift = (step) => {
  const next = shiftAnchor(view.value, anchor.value, step)
  setQuery({ datum: datumFor(view.value, next) })
}
const goToday = () => setQuery({ datum: undefined })

// Familie: gemerkt je Gerät, sonst die erste; Umschalter nur bei mehr als einer
const families = computed(() => familyStore.families)
const storedFamily = Number(localStorage.getItem(FAMILY_KEY)) || null
const familyId = ref(null)
watch(families, (list) => {
  if (!list.length) familyId.value = null
  else if (!list.some((f) => f.id === familyId.value)) {
    familyId.value = list.find((f) => f.id === storedFamily)?.id ?? list[0].id
  }
}, { immediate: true })
const family = computed(() => families.value.find((f) => f.id === familyId.value) ?? null)
const noFamily = computed(() => familyStore.loaded && families.value.length === 0)

function chooseFamily(id) {
  familyId.value = id
  localStorage.setItem(FAMILY_KEY, String(id))
  closeSheet()
}

// Einträge des Zeitraums (eine Abfrage je Woche bzw. Monat)
const range = computed(() => rangeFor(view.value, anchor.value))
const entries = ref([])
const loading = ref(false)
const error = ref(null)
let loadSeq = 0

async function load() {
  if (familyId.value == null) return
  const seq = ++loadSeq
  const { from, to } = range.value
  loading.value = true
  error.value = null
  try {
    const data = await mealPlanService.list(familyId.value, from, to)
    if (seq === loadSeq) entries.value = data
  } catch (e) {
    if (seq === loadSeq) error.value = e.message
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

watch([familyId, () => range.value.from, () => range.value.to], load, { immediate: true })

const byDate = computed(() => groupByDate(entries.value))
const entriesOn = (day) => byDate.value.get(isoDate(day)) ?? []
const inRange = (iso) => iso >= range.value.from && iso <= range.value.to

const week = computed(() => weekDays(anchor.value))
const grid = computed(() => monthGrid(anchor.value))
const monthDaysList = computed(() => monthDays(anchor.value))

// Monatsraster: gewählt ist heute, in anderen Monaten der Erste
const selectedDay = ref(null)
watch([view, anchor], () => {
  if (selectedDay.value && sameMonth(selectedDay.value, anchor.value)) return
  selectedDay.value = sameMonth(anchor.value, today.value) ? today.value : monthStart(anchor.value)
}, { immediate: true })

const cellLabel = (day) => {
  const titles = entriesOn(day).map((e) => entryTitle(e, languagePref.value))
  const base = dayLabel(day) + (sameDay(day, today.value) ? ` · ${t('today')}` : '')
  return titles.length ? t('entriesOn', { day: base, titles: titles.join(', ') }) : `${base} · ${t('nothingPlanned')}`
}

// Sheets
const sheet = ref(null)
const busy = ref(false)
const openSheet = (state, returnFocus = null) => { sheet.value = { ...state, returnFocus } }
const closeSheet = () => { sheet.value = null }
const openPicker = (date, returnFocus) => openSheet({ type: 'picker', date }, returnFocus)
const onManage = (entry, el) => openSheet({ type: 'manage', entry }, el)

async function addEntry({ recipeId = null, freeText = null }) {
  if (busy.value || !sheet.value?.date) return
  const date = sheet.value.date
  busy.value = true
  try {
    const entry = await mealPlanService.add({ familyId: familyId.value, date: isoDate(date), recipeId, freeText })
    if (inRange(entry.date)) entries.value = insertEntry(entries.value, entry)
    closeSheet()
    uiStore.showToast(t('planned', { day: dayLabel(date) }))
  } catch (e) {
    uiStore.showToast(e.message)
  } finally {
    busy.value = false
  }
}

async function removeEntry(entry) {
  if (busy.value) return
  busy.value = true
  try {
    await mealPlanService.remove(entry.id)
    entries.value = entries.value.filter((e) => e.id !== entry.id)
    closeSheet()
    uiStore.showToast(t('removed'))
  } catch (e) {
    uiStore.showToast(e.message)
  } finally {
    busy.value = false
  }
}

async function moveEntry(entry, date) {
  if (busy.value) return
  busy.value = true
  try {
    const moved = await mealPlanService.move(entry.id, isoDate(date))
    const rest = entries.value.filter((e) => e.id !== entry.id)
    entries.value = inRange(moved.date) ? insertEntry(rest, moved) : rest
    closeSheet()
    uiStore.showToast(t('moved', { day: dayLabel(date) }))
  } catch (e) {
    uiStore.showToast(e.message)
  } finally {
    busy.value = false
  }
}

// Zurück in die App: neuer Tag und Änderungen der anderen Familienmitglieder
function onVisible() {
  if (document.visibilityState !== 'visible') return
  const now = dayOnly(new Date())
  if (!sameDay(now, today.value)) today.value = now
  languagePref.value = readLanguagePreference()
  if (!sheet.value) load()
}

onMounted(() => document.addEventListener('visibilitychange', onVisible))
onUnmounted(() => document.removeEventListener('visibilitychange', onVisible))
</script>
