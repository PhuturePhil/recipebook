<template>
  <!-- „Planen“ auf der Rezeptseite: eine infrage kommende Familie → direkt die Datumswahl; mehrere → „In welchen Plan?“;
       privates eigenes Rezept → erst „Mit Familie ‚X‘ teilen?“ (geteilt wird serverseitig zusammen mit dem Eintrag,
       bei Ablehnung entsteht nichts) -->
  <PlanSheet
    v-if="step === 'family'"
    :title="t('whichPlan')"
    :subtitle="title"
    :return-focus="returnFocus"
    @close="emit('close')"
  >
    <div class="plan-sheet__body">
      <button v-for="f in choices" :key="f.id" type="button" class="plan-mi" @click="chooseFamily(f)">
        <AppIcon name="familie" :size="21" />{{ f.name }}
      </button>
    </div>
  </PlanSheet>

  <ConfirmDialog
    v-else-if="step === 'share'"
    :title="t('shareTitle', { name: family.name })"
    :text="t('shareText', { name: family.name })"
    :confirm-label="t('shareConfirm')"
    :cancel-label="t('cancel')"
    @confirm="share = true; step = 'date'"
    @cancel="emit('close')"
  />

  <PlanSheet
    v-else-if="step === 'date'"
    :title="t('dateTitle')"
    :subtitle="dateSubtitle"
    :return-focus="returnFocus"
    @close="emit('close')"
  >
    <PlanDatePicker :busy="busy" @pick="plan" />
  </PlanSheet>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PlanSheet from './PlanSheet.vue'
import PlanDatePicker from './PlanDatePicker.vue'
import { useFamilyStore } from '@/stores/familyStore'
import { useAuthStore } from '@/stores/authStore'
import { useUiStore } from '@/stores/uiStore'
import { mealPlanService } from '@/services/mealPlanService'
import { dayLabel, isoDate, planTargets } from '@/utils/mealPlan'
import { planText as t } from '@/utils/planTexts'

const props = defineProps({
  recipe: { type: Object, required: true },
  // Titel in Anzeigesprache
  title: { type: String, default: '' },
  isOwner: { type: Boolean, default: false },
  returnFocus: { type: Object, default: null },
})
const emit = defineEmits(['close', 'planned'])
const familyStore = useFamilyStore()
const authStore = useAuthStore()
const uiStore = useUiStore()

const step = ref(null)
const choices = ref([])
const family = ref(null)
const share = ref(false)
const busy = ref(false)
let targets = { plannable: [], shareable: [] }

const dateSubtitle = computed(() =>
  [props.title, choices.value.length > 1 || familyStore.families.length > 1 ? family.value?.name : '']
    .filter(Boolean).join(' · '))

function chooseFamily(f) {
  family.value = f
  step.value = share.value || targets.plannable.some((p) => p.id === f.id) ? 'date' : 'share'
}

onMounted(async () => {
  const families = await familyStore.ensureLoaded(authStore.user?.id)
  targets = planTargets(props.recipe, families, props.isOwner)
  const { plannable, shareable } = targets
  if (!families.length) {
    uiStore.showToast(t('noFamilyShort'))
    emit('close')
    return
  }
  choices.value = plannable.length ? plannable : shareable
  if (!choices.value.length) {
    uiStore.showToast(t('notPlannable'))
    emit('close')
  } else if (choices.value.length === 1) {
    chooseFamily(choices.value[0])
  } else {
    step.value = 'family'
  }
})

async function plan(date) {
  if (busy.value || !family.value) return
  busy.value = true
  try {
    await mealPlanService.add({
      familyId: family.value.id,
      date: isoDate(date),
      recipeId: props.recipe.id,
      shareWithFamily: share.value,
    })
    uiStore.showToast(t('planned', { day: dayLabel(date) }))
    emit('planned', { shared: share.value })
  } catch (e) {
    uiStore.showToast(e.message)
    busy.value = false
  }
}
</script>
