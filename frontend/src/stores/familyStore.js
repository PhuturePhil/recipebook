import { defineStore } from 'pinia'
import { familyService } from '@/services/familyService'

export { MAX_MEMBERS, shortFamilyName, avatarTone } from '@/utils/families'

export const useFamilyStore = defineStore('family', {
  state: () => ({
    families: [],
    loaded: false,
    loading: false,
    error: null,
    _viewer: null,
  }),

  getters: {
    byId: (state) => (id) => state.families.find((f) => f.id === Number(id)) ?? null,
  },

  actions: {
    // Einmal je Nutzer laden (Wechsel des Kontos lädt neu)
    async ensureLoaded(viewerId, { force = false } = {}) {
      if (viewerId == null) {
        this.reset()
        return []
      }
      if (this.loaded && this._viewer === viewerId && !force) return this.families
      this.loading = true
      this.error = null
      try {
        this.families = await familyService.list()
        this.loaded = true
        this._viewer = viewerId
      } catch (error) {
        this.error = error.message
      } finally {
        this.loading = false
      }
      return this.families
    },

    // Nach Änderungen an einer Familie die Übersicht frisch halten
    upsert(family) {
      const index = this.families.findIndex((f) => f.id === family.id)
      if (index === -1) this.families.push(family)
      else this.families[index] = family
    },

    remove(id) {
      this.families = this.families.filter((f) => f.id !== Number(id))
    },

    reset() {
      this.families = []
      this.loaded = false
      this._viewer = null
      this.error = null
    },
  },
})
