import { defineStore } from 'pinia'
import { recipeService } from '@/services/recipeService'
import { useUiStore } from '@/stores/uiStore'

export const QUICK_MAX_MINUTES = 30

const BADGE_NAMES = ['Energiearm', 'Proteinreich', 'Ballaststoffreich', 'Fettarm', 'Schnell']
const BADGE_SEARCH_ALIASES = { kalorienarm: 'Energiearm' }

const toSummary = (recipe) => ({
  id: recipe.id,
  title: recipe.title,
  description: recipe.description,
  imageUrl: recipe.imageUrl,
  prepTimeMinutes: recipe.prepTimeMinutes,
  baseServings: recipe.baseServings,
  servingsTo: recipe.servingsTo,
  ingredientCount: recipe.ingredientCount ?? recipe.ingredients?.length ?? 0,
  author: recipe.author ?? '',
  source: recipe.source ?? '',
  createdBy: recipe.createdBy ?? '',
  ingredientNames: recipe.ingredientNames ?? '',
  nutrition: recipe.nutrition ?? null,
})

export const useRecipeStore = defineStore('recipe', {
  state: () => ({
    recipes: [],
    currentRecipe: null,
    loading: false,
    error: null,
    searchTerms: [],
    pendingSearchTerm: '',
    _lastFetched: null,
  }),

  getters: {
    computedBadges: (state) => {
      const badgeMap = new Map()
      for (const r of state.recipes) {
        const badges = [...(r.nutrition?.badges ?? [])]
        if (r.prepTimeMinutes != null && r.prepTimeMinutes <= QUICK_MAX_MINUTES) badges.push('Schnell')
        badgeMap.set(r.id, badges)
      }
      return badgeMap
    },

    activeSearchTerms: (state) => {
      const pending = state.pendingSearchTerm.trim()
      return pending && !state.searchTerms.includes(pending)
        ? [...state.searchTerms, pending]
        : state.searchTerms
    },

    filteredRecipes() {
      const terms = this.activeSearchTerms
      if (!terms.length) return this.recipes
      const timeRegex = /^([<>])\s*(\d+)$/
      const badgeMap = this.computedBadges
      return this.recipes.filter((recipe) =>
        terms.every((term) => {
          const timeMatch = term.match(timeRegex)
          if (timeMatch) {
            const op = timeMatch[1]
            const minutes = parseInt(timeMatch[2], 10)
            const prep = recipe.prepTimeMinutes ?? null
            if (prep === null) return false
            return op === '<' ? prep < minutes : prep > minutes
          }
          const termLower = term.toLowerCase()
          const matchedBadge = BADGE_SEARCH_ALIASES[termLower] ?? BADGE_NAMES.find(b => b.toLowerCase() === termLower)
          if (matchedBadge) {
            return (badgeMap.get(recipe.id) ?? []).includes(matchedBadge)
          }
          const q = termLower
          return (
            (recipe.title || '').toLowerCase().includes(q) ||
            (recipe.description || '').toLowerCase().includes(q) ||
            (recipe.author || '').toLowerCase().includes(q) ||
            (recipe.source || '').toLowerCase().includes(q) ||
            (recipe.createdBy || '').toLowerCase().includes(q) ||
            (recipe.ingredientNames || '').toLowerCase().includes(q)
          )
        })
      )
    },

    searchQuery() {
      return this.activeSearchTerms.join(', ')
    },

    getRecipeById: (state) => (id) => {
      return state.recipes.find((recipe) => recipe.id === parseInt(id))
    }
  },

  actions: {
    async fetchRecipes({ background = false } = {}) {
      const stale = !this._lastFetched || Date.now() - this._lastFetched > 2 * 60 * 1000
      if (this.recipes.length > 0 && !stale && !this._forceRefresh) return
      this._forceRefresh = false
      if (!background) this.loading = true
      this.error = null
      const uiStore = useUiStore()
      uiStore.showLoading('Rezepte werden geladen…')
      try {
        const data = await recipeService.getAll()
        this.recipes = data
        this._lastFetched = Date.now()
      } catch (error) {
        this.error = error.message
        console.error('Failed to fetch recipes:', error)
      } finally {
        this.loading = false
        uiStore.hideLoading()
      }
    },

    invalidateRecipes() {
      this._forceRefresh = true
    },

    async fetchRecipeById(id) {
      this.loading = true
      this.error = null
      try {
        const data = await recipeService.getById(id)
        this.currentRecipe = data
        return data
      } catch (error) {
        this.currentRecipe = null
        this.error = error.message
        console.error('Failed to fetch recipe:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async createRecipe(recipe) {
      this.loading = true
      this.error = null
      try {
        const data = await recipeService.create(recipe)
        this.recipes.unshift(toSummary(data))
        this._forceRefresh = true
        return data
      } catch (error) {
        this.error = error.message
        console.error('Failed to create recipe:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async updateRecipe(id, recipe) {
      this.loading = true
      this.error = null
      const numericId = Number(id)
      try {
        const data = await recipeService.update(id, recipe)
        const index = this.recipes.findIndex((r) => r.id === numericId)
        if (index !== -1) {
          this.recipes[index] = toSummary(data)
        }
        this._forceRefresh = true
        if (this.currentRecipe && this.currentRecipe.id === numericId) {
          this.currentRecipe = data
        }
        return data
      } catch (error) {
        this.error = error.message
        console.error('Failed to update recipe:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async deleteRecipe(id) {
      this.loading = true
      this.error = null
      const numericId = Number(id)
      try {
        await recipeService.delete(id)
        this.recipes = this.recipes.filter((r) => r.id !== numericId)
        if (this.currentRecipe && this.currentRecipe.id === numericId) {
          this.currentRecipe = null
        }
      } catch (error) {
        this.error = error.message
        console.error('Failed to delete recipe:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    setSearchTerms(terms) {
      this.searchTerms = terms
    },

    setPendingSearchTerm(term) {
      this.pendingSearchTerm = term
    },

    clearError() {
      this.error = null
    }
  }
})
