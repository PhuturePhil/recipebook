<template>
  <!-- „Planen“ auf der Rezeptseite (Entwurf media/out/mock_family_plan.html): ein Sheet mit Datumswahl, bei mehreren
       Familien darüber „Planen für“ mit Chips (Standard-Familie vorausgewählt, mit ★). Sieht die gewählte Familie das
       Rezept nicht, steht eine Fußnote darunter. Nur der Besitzer wird danach gefragt, ob er teilen oder nur über den
       Plan sichtbar machen will; alle anderen planen still „nur über den Plan“. -->
  <PlanSheet
    v-if="step === 'date'"
    :title="t('dateTitle')"
    :subtitle="title"
    :return-focus="returnFocus"
    @close="emit('close')"
  >
    <div v-if="families.length > 1" class="plan-for">
      <div :id="forId" class="plan-for__label">{{ t('planFor') }}</div>
      <div class="plan-for__chips" role="radiogroup" :aria-labelledby="forId">
        <button
          v-for="f in families"
          :key="f.id"
          type="button"
          role="radio"
          :class="['plan-for__chip', { on: f.id === familyId }]"
          :aria-checked="f.id === familyId ? 'true' : 'false'"
          :aria-label="f.defaultFamily ? `${f.name} · ${t('defaultMark')}` : undefined"
          @click="familyId = f.id"
        >
          <HeroIcon v-if="f.defaultFamily" name="stern" :size="13" class="plan-for__star" />{{ f.name }}
        </button>
      </div>
    </div>
    <p v-if="planOnlyFamily" class="plan-for__note">{{ t('planOnlyNote', { name: planOnlyFamily.name }) }}</p>
    <PlanDatePicker :family-id="familyId" :busy="busy" @pick="pickDate" />
  </PlanSheet>

  <PlanSheet
    v-else-if="step === 'share'"
    :title="t('shareOrPlanTitle')"
    :subtitle="`${title} · ${family?.name ?? ''}`"
    :return-focus="returnFocus"
    @close="emit('close')"
  >
    <div class="plan-sheet__body">
      <button type="button" class="plan-mi" :disabled="busy" @click="plan(true)">
        <AppIcon name="familie" :size="21" />{{ t('shareWith', { name: family?.name ?? '' }) }}
      </button>
      <button type="button" class="plan-mi" :disabled="busy" @click="plan(false)">
        <AppIcon name="planen" :size="21" />{{ t('planOnly') }}
      </button>
    </div>
  </PlanSheet>
</template>

<script setup>
import { computed, onMounted, ref, useId } from 'vue'
import AppIcon from '@/components/shell/AppIcon.vue'
import HeroIcon from '@/components/HeroIcon.vue'
import PlanSheet from './PlanSheet.vue'
import PlanDatePicker from './PlanDatePicker.vue'
import { useFamilyStore } from '@/stores/familyStore'
import { useAuthStore } from '@/stores/authStore'
import { useUiStore } from '@/stores/uiStore'
import { mealPlanService } from '@/services/mealPlanService'
import { asksToShare, dayLabel, defaultFamilyId, isoDate, plannableIn } from '@/utils/mealPlan'
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
const forId = `plan-for-${useId()}`

const step = ref(null)
const families = ref([])
const familyId = ref(null)
const date = ref(null)
const busy = ref(false)

const family = computed(() => families.value.find((f) => f.id === familyId.value) ?? null)
const planOnlyFamily = computed(() =>
  family.value && !plannableIn(props.recipe, family.value.id) ? family.value : null)

onMounted(async () => {
  families.value = await familyStore.ensureLoaded(authStore.user?.id)
  if (!families.value.length) {
    uiStore.showToast(t('noFamilyShort'))
    emit('close')
    return
  }
  familyId.value = defaultFamilyId(families.value)
  step.value = 'date'
})

function pickDate(picked) {
  date.value = picked
  if (asksToShare(props.recipe, familyId.value, props.isOwner)) step.value = 'share'
  else plan(false)
}

async function plan(share) {
  if (busy.value || !family.value || !date.value) return
  busy.value = true
  try {
    await mealPlanService.add({
      familyId: family.value.id,
      date: isoDate(date.value),
      recipeId: props.recipe.id,
      shareWithFamily: share,
    })
    uiStore.showToast(t('planned', { day: dayLabel(date.value) }))
    emit('planned', { shared: share })
  } catch (e) {
    uiStore.showToast(e.message)
    busy.value = false
  }
}
</script>
