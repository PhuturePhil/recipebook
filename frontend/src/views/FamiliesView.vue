<template>
  <!-- Familien-Übersicht (Mockup plans/rezepte-familien/02-uebersicht.html) -->
  <div class="fam-page shell-view-body">
    <p v-if="familyStore.error && !familyStore.families.length" class="fam-status fam-status--error">{{ familyStore.error }}</p>

    <RouterLink
      v-for="family in familyStore.families"
      :key="family.id"
      :to="`/familien/${family.id}`"
      class="ui-karte fam-card"
    >
      <span :class="['fam-avatar', 'mini-avatar', avatarTone(family.id)]" aria-hidden="true">{{ initialOf(family.name) }}</span>
      <span class="fam-card__body">
        <span class="fam-card__name">{{ family.name }}</span>
        <span class="fam-card__meta">{{ countLine(family) }}</span>
        <span class="avatar-stack" aria-hidden="true">
          <span v-for="m in family.members.slice(0, 6)" :key="m.id" :class="['mini-avatar', avatarTone(m.id)]">{{ m.initials }}</span>
        </span>
      </span>
      <AppIcon class="fam-card__chev" name="weiter" :size="18" />
    </RouterLink>

    <button type="button" class="ui-btn ui-btn--primaer fam-found" @click="openCreate">
      <AppIcon name="plus" :size="18" />Familie gründen
    </button>

    <FamilyNameDialog
      v-if="creating"
      title="Familie gründen"
      confirm-label="Gründen"
      :error="createError"
      :busy="busy"
      @submit="create"
      @cancel="creating = false"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/shell/AppIcon.vue'
import FamilyNameDialog from '@/components/FamilyNameDialog.vue'
import { useAuthStore } from '@/stores/authStore'
import { useFamilyStore, avatarTone, shortFamilyName } from '@/stores/familyStore'
import { familyService } from '@/services/familyService'
import { usePageVisit } from '@/composables/usePageVisit'
import { track } from '@/utils/analytics'

usePageVisit('familien')

const router = useRouter()
const authStore = useAuthStore()
const familyStore = useFamilyStore()
const creating = ref(false)
const createError = ref('')
const busy = ref(false)

const initialOf = (name) => shortFamilyName(name).charAt(0).toLocaleUpperCase('de-DE')
const plural = (n, one, many) => `${n} ${n === 1 ? one : many}`
const countLine = (f) => `${plural(f.memberCount, 'Mitglied', 'Mitglieder')} · ${plural(f.recipeCount, 'Rezept', 'Rezepte')}`

function openCreate() {
  createError.value = ''
  creating.value = true
}

async function create(name) {
  busy.value = true
  createError.value = ''
  try {
    const family = await familyService.create(name)
    familyStore.upsert(family)
    track('familie_angelegt')
    creating.value = false
    router.push(`/familien/${family.id}`)
  } catch (error) {
    createError.value = error.message
  } finally {
    busy.value = false
  }
}

// Frisch laden, damit Zähler (Mitglieder/Rezepte) stimmen
onMounted(() => familyStore.ensureLoaded(authStore.user?.id, { force: true }))
</script>

<style scoped>
.fam-card__name, .fam-card__meta { display: block; }
.fam-found { width: 100%; margin-top: 10px; }
</style>
