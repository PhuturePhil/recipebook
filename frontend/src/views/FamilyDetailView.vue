<template>
  <!-- Familien-Detail (Mockup plans/rezepte-familien/03-detail.html, ohne Löschen und ohne Gültigkeits-Hinweis) -->
  <div class="fam-page shell-view-body">
    <div v-if="notFound" class="fam-missing">
      <p>Diese Familie gibt es nicht.</p>
      <RouterLink to="/familien" class="ui-btn">Zu den Familien</RouterLink>
    </div>

    <template v-else-if="family">
      <div class="ui-karte fam-head">
        <button type="button" class="stat-row" @click="showRecipes">
          <AppIcon name="buch" :size="20" />
          <span class="stat-row__label">{{ recipeLine }}</span>
          <AppIcon class="stat-row__chev" name="weiter" :size="17" />
        </button>
        <button v-if="editing" type="button" class="stat-row" @click="openRename">
          <AppIcon name="bearbeiten" :size="20" />
          <span class="stat-row__label">Namen ändern</span>
          <AppIcon class="stat-row__chev" name="weiter" :size="17" />
        </button>
      </div>

      <p class="section-label">Mitglieder · {{ family.memberCount }} von {{ family.maxMembers }}</p>
      <div class="ui-karte fam-members">
        <div v-for="m in family.members" :key="m.id" class="member-row">
          <span :class="['mini-avatar', avatarTone(m.id)]" aria-hidden="true">{{ m.initials }}</span>
          <span class="member-row__name">{{ m.name }}<small v-if="m.self"> (du)</small></span>
          <button
            v-if="editing && !m.self"
            type="button"
            class="icon-btn icon-btn--plain icon-btn--gefahr"
            :aria-label="`${m.name} entfernen`"
            :title="`${m.name} entfernen`"
            @click="removing = m"
          >
            <AppIcon name="loeschen" :size="18" />
          </button>
        </div>
      </div>

      <p class="section-label">Einladungslink</p>
      <div class="ui-karte">
        <template v-if="family.invite">
          <div class="fam-invite-row">
            <code>{{ displayUrl }}</code>
            <button type="button" class="icon-btn" aria-label="Link kopieren" title="Kopieren" @click="copyLink">
              <AppIcon name="kopieren" :size="18" />
            </button>
            <button v-if="canShare" type="button" class="icon-btn" aria-label="Link teilen" title="Teilen" @click="shareLink">
              <AppIcon name="teilen" :size="18" />
            </button>
          </div>
          <button type="button" class="ui-btn fam-renew" :disabled="busy" @click="confirmRenew = true">
            <AppIcon name="aktualisieren" :size="15" />Neuen Link erstellen
          </button>
        </template>
        <button v-else type="button" class="ui-btn fam-renew fam-renew--solo" :disabled="busy" @click="renew">
          <AppIcon name="plus" :size="15" />Link erstellen
        </button>
      </div>

      <p v-if="actionError" class="fam-status fam-status--error" role="alert">{{ actionError }}</p>

      <button type="button" class="ui-btn ui-btn--gefahr fam-leave" :disabled="busy" @click="confirmLeave = true">
        <AppIcon name="abmelden" :size="17" />Familie verlassen
      </button>
    </template>

    <FamilyNameDialog
      v-if="renaming"
      title="Familie umbenennen"
      confirm-label="Speichern"
      :initial="family?.name ?? ''"
      :error="renameError"
      :busy="busy"
      @submit="rename"
      @cancel="renaming = false"
    />
    <ConfirmDialog
      v-if="removing"
      :title="`${removing.name} entfernen?`"
      :text="`Rezepte von ${removing.name} sind dann nicht mehr in „${family?.name}“ zu sehen.`"
      confirm-label="Entfernen"
      destructive
      @confirm="removeMember"
      @cancel="removing = null"
    />
    <ConfirmDialog
      v-if="confirmLeave"
      title="Familie verlassen?"
      :text="leaveText"
      confirm-label="Verlassen"
      destructive
      @confirm="leave"
      @cancel="confirmLeave = false"
    />
    <ConfirmDialog
      v-if="confirmRenew"
      title="Neuen Link erstellen?"
      text="Der bisherige Link funktioniert dann nicht mehr."
      confirm-label="Neuer Link"
      @confirm="renew"
      @cancel="confirmRenew = false"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/shell/AppIcon.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import FamilyNameDialog from '@/components/FamilyNameDialog.vue'
import { useAuthStore } from '@/stores/authStore'
import { useFamilyStore, avatarTone } from '@/stores/familyStore'
import { useRecipeStore } from '@/stores/recipeStore'
import { useUiStore } from '@/stores/uiStore'
import { familyService } from '@/services/familyService'
import { usePageVisit } from '@/composables/usePageVisit'

usePageVisit('familie')

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const familyStore = useFamilyStore()
const recipeStore = useRecipeStore()
const uiStore = useUiStore()

const family = ref(null)
const notFound = ref(false)
const editing = ref(false)
const busy = ref(false)
const actionError = ref('')
const renaming = ref(false)
const renameError = ref('')
const removing = ref(null)
const confirmLeave = ref(false)
const confirmRenew = ref(false)
const canShare = typeof navigator !== 'undefined' && typeof navigator.share === 'function'

const recipeLine = computed(() => {
  const n = family.value?.recipeCount ?? 0
  return `${n} ${n === 1 ? 'Rezept' : 'Rezepte'} in dieser Familie`
})
const displayUrl = computed(() => (family.value?.invite?.url ?? '').replace(/^https?:\/\//, ''))
const leaveText = computed(() => (family.value?.memberCount ?? 0) <= 1
  ? 'Du bist das letzte Mitglied. Damit wird die Familie gelöscht. Deine Rezepte bleiben bei dir.'
  : `Deine Rezepte sind dann nicht mehr in „${family.value?.name}“ zu sehen, sie bleiben aber bei dir.`)

function apply(view) {
  family.value = view
  familyStore.upsert(view)
  uiStore.setNavTitle(view.name)
}

function setHeaderAction() {
  uiStore.setHeaderAction(family.value
    ? { icon: editing.value ? 'haken' : 'bearbeiten', label: editing.value ? 'Fertig' : 'Bearbeiten', run: () => { editing.value = !editing.value } }
    : null)
}
watch([family, editing], setHeaderAction)

async function load() {
  notFound.value = false
  editing.value = false
  actionError.value = ''
  try {
    apply(await familyService.get(route.params.id))
  } catch (error) {
    family.value = null
    if (error.status === 404) notFound.value = true
    else actionError.value = error.message
  }
}

async function run(action) {
  busy.value = true
  actionError.value = ''
  try {
    await action()
  } catch (error) {
    actionError.value = error.message
  } finally {
    busy.value = false
  }
}

function showRecipes() {
  recipeStore.setScope(family.value.id)
  router.push('/')
}

function openRename() {
  renameError.value = ''
  renaming.value = true
}

async function rename(name) {
  busy.value = true
  renameError.value = ''
  try {
    apply(await familyService.rename(family.value.id, name))
    recipeStore.invalidateRecipes()
    renaming.value = false
  } catch (error) {
    renameError.value = error.message
  } finally {
    busy.value = false
  }
}

function renew() {
  confirmRenew.value = false
  return run(async () => {
    apply(await familyService.renewInvite(family.value.id))
    uiStore.showToast('Neuer Link erstellt')
  })
}

function removeMember() {
  const member = removing.value
  removing.value = null
  return run(async () => {
    await familyService.removeMember(family.value.id, member.id)
    recipeStore.invalidateRecipes()
    await load()
    editing.value = true
  })
}

function leave() {
  confirmLeave.value = false
  const { id, name } = family.value
  return run(async () => {
    const me = family.value.members.find((m) => m.self)?.id ?? authStore.user?.id
    await familyService.removeMember(id, me)
    familyStore.remove(id)
    recipeStore.invalidateRecipes()
    uiStore.showToast(`Du hast „${name}“ verlassen`)
    router.replace('/familien')
  })
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(family.value.invite.url)
    uiStore.showToast('Link kopiert')
  } catch {
    actionError.value = 'Kopieren hat nicht geklappt – halte den Link gedrückt, um ihn zu kopieren.'
  }
}

async function shareLink() {
  try {
    await navigator.share({
      title: family.value.name,
      text: `Komm in „${family.value.name}“ bei Pastoors Familienrezepte`,
      url: family.value.invite.url,
    })
  } catch {
    // Teilen abgebrochen
  }
}

watch(() => route.params.id, (id) => { if (id) load() })
onMounted(load)
onUnmounted(() => {
  uiStore.clearNavTitle()
  uiStore.setHeaderAction(null)
})
</script>

<style scoped>
.fam-head { padding: 4px 16px; }
.fam-members { padding: 4px 16px; }
.fam-renew { margin-top: 10px; padding: 8px 12px; font-size: 13.5px; }
.fam-renew--solo { margin-top: 0; }
.fam-leave { width: 100%; margin-top: 22px; }
.fam-missing { text-align: center; padding: 48px 12px; color: var(--text2); }
.fam-missing .ui-btn { margin-top: 12px; text-decoration: none; }
.fam-invite-row code { user-select: all; }
</style>
