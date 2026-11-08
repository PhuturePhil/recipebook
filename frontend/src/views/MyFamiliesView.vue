<template>
  <!-- „Meine Familien“ (Entwurf media/out/mock_default_family.html, 08.11.2026): Standard-Familie wählen – nur für mich,
       bestimmt den ersten Essensplan und die Vorauswahl beim Planen -->
  <div class="fam-page shell-view-body my-fams">
    <p v-if="familyStore.error && !familyStore.families.length" class="fam-status fam-status--error">{{ familyStore.error }}</p>

    <div v-if="familyStore.families.length" class="ui-karte my-fams__card" role="radiogroup" :aria-label="t('defaultFamily')">
      <button
        v-for="family in familyStore.families"
        :key="family.id"
        type="button"
        role="radio"
        class="my-fams__row"
        :aria-checked="isDefault(family) ? 'true' : 'false'"
        :disabled="busy"
        @click="choose(family)"
      >
        <span class="my-fams__ava" aria-hidden="true">{{ initialOf(family.name) }}</span>
        <span class="my-fams__text">
          <span class="my-fams__name">{{ family.name }}</span>
          <span class="my-fams__meta">{{ membersLine(family.memberCount) }}</span>
        </span>
        <span v-if="isDefault(family)" class="my-fams__badge">
          <HeroIcon name="stern" :size="10" />{{ t('defaultMark') }}
        </span>
        <span :class="['my-fams__radio', { on: isDefault(family) }]" aria-hidden="true"></span>
      </button>
    </div>

    <p class="my-fams__hint">{{ t('defaultHint') }}</p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import HeroIcon from '@/components/HeroIcon.vue'
import { useAuthStore } from '@/stores/authStore'
import { useFamilyStore, shortFamilyName } from '@/stores/familyStore'
import { useUiStore } from '@/stores/uiStore'
import { PLAN_FAMILY_KEY } from '@/utils/mealPlan'
import { planText as t } from '@/utils/planTexts'

const authStore = useAuthStore()
const familyStore = useFamilyStore()
const uiStore = useUiStore()
const busy = ref(false)

const initialOf = (name) => shortFamilyName(name).charAt(0).toLocaleUpperCase('de-DE')
const membersLine = (n) => (n === 1 ? t('membersOne') : t('members', { n }))
const isDefault = (family) => familyStore.defaultFamily?.id === family.id

async function choose(family) {
  if (busy.value || isDefault(family)) return
  busy.value = true
  try {
    await familyStore.setDefault(family.id)
    // Der Essensplan startet danach wieder mit der Standard-Familie
    sessionStorage.removeItem(PLAN_FAMILY_KEY)
  } catch (error) {
    uiStore.showToast(error.message)
  } finally {
    busy.value = false
  }
}

onMounted(() => familyStore.ensureLoaded(authStore.user?.id, { force: true }))
</script>

<style scoped>
.my-fams__card {
  padding: 4px 0;
  overflow: hidden;
}

.my-fams__row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px;
  border: 0;
  border-bottom: 1px solid var(--h-chip, var(--linie));
  background: none;
  color: var(--text);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.my-fams__row:last-child {
  border-bottom: none;
}

.my-fams__row:focus-visible {
  outline: 2px solid var(--akzent);
  outline-offset: -2px;
}

.my-fams__ava {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(232, 89, 12, .12);
  color: var(--akzent-text);
  font-size: 15px;
  font-weight: 800;
}

.my-fams__text {
  flex: 1;
  min-width: 0;
}

.my-fams__name,
.my-fams__meta {
  display: block;
}

.my-fams__name {
  font-size: 15px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.my-fams__meta {
  margin-top: 2px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text2);
}

.my-fams__badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: none;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(232, 89, 12, .1);
  color: var(--akzent-text);
  font-size: 11px;
  font-weight: 750;
}

.my-fams__radio {
  width: 24px;
  height: 24px;
  flex: none;
  border-radius: 50%;
  border: 2px solid var(--linie);
  display: flex;
  align-items: center;
  justify-content: center;
}

.my-fams__radio.on {
  border-color: var(--akzent);
}

.my-fams__radio.on::after {
  content: '';
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--akzent);
}

.my-fams__hint {
  margin: 10px 4px 0;
  font-size: 12.5px;
  font-weight: 550;
  line-height: 1.5;
  color: var(--text2);
}
</style>
