import { defineStore } from 'pinia'
import { recipeService } from '@/services/recipeService'

const BADGE_NAMES = ['Kalorienarm', 'Proteinreich', 'Ballaststoffreich', 'Fettarm', 'Schnell']

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
  nutritionKcal: recipe.nutritionKcal ?? null,
  nutritionFat: recipe.nutritionFat ?? null,
  nutritionProtein: recipe.nutritionProtein ?? null,
  nutritionFiber: recipe.nutritionFiber ?? null,
})

export const useRecipeStore = defineStore('recipe', {
  state: () => ({
    recipes: [],
    currentRecipe: null,
    loading: false,
    error: null,
    searchTerms: [],
    _lastFetched: null,
  }),

  getters: {
    computedBadges: (state) => {
      const recipes = state.recipes
      const badgeMap = new Map()

      const percentile10 = (values) => {
        if (!values.length) return null
        const sorted = [...values].sort((a, b) => a - b)
        const idx = Math.max(0, Math.ceil(sorted.length * 0.1) - 1)
        return sorted[idx]
      }
      const percentile90 = (values) => {
        if (!values.length) return null
        const sorted = [...values].sort((a, b) => a - b)
        const idx = Math.min(sorted.length - 1, Math.floor(sorted.length * 0.9))
        return sorted[idx]
      }

      const servings = (r) => (r.baseServings && r.baseServings > 0 ? r.baseServings : 1)

      const kcalPer = recipes.filter(r => r.nutritionKcal != null).map(r => r.nutritionKcal / servings(r))
      const fatPer = recipes.filter(r => r.nutritionFat != null).map(r => r.nutritionFat / servings(r))
      const proteinPer = recipes.filter(r => r.nutritionProtein != null).map(r => r.nutritionProtein / servings(r))
      const fiberPer = recipes.filter(r => r.nutritionFiber != null).map(r => r.nutritionFiber / servings(r))
      const prep = recipes.filter(r => r.prepTimeMinutes != null).map(r => r.prepTimeMinutes)

      const kcalThreshold = percentile10(kcalPer)
      const fatThreshold = percentile10(fatPer)
      const proteinThreshold = percentile90(proteinPer)
      const fiberThreshold = percentile90(fiberPer)
      const prepThreshold = percentile10(prep)

      for (const r of recipes) {
        const badges = []
        const s = servings(r)
        if (r.nutritionKcal != null && kcalThreshold != null && r.nutritionKcal / s <= kcalThreshold) badges.push('Kalorienarm')
        if (r.nutritionFat != null && fatThreshold != null && r.nutritionFat / s <= fatThreshold) badges.push('Fettarm')
        if (r.nutritionProtein != null && proteinThreshold != null && r.nutritionProtein / s >= proteinThreshold) badges.push('Proteinreich')
        if (r.nutritionFiber != null && fiberThreshold != null && r.nutritionFiber / s >= fiberThreshold) badges.push('Ballaststoffreich')
        if (r.prepTimeMinutes != null && prepThreshold != null && r.prepTimeMinutes <= prepThreshold) badges.push('Schnell')
        badgeMap.set(r.id, badges)
      }
      return badgeMap
    },

    filteredRecipes: (state, getters) => {
      if (!state.searchTerms.length) return state.recipes
      const timeRegex = /^([<>])\s*(\d+)$/
      const badgeMap = getters.computedBadges
      return state.recipes.filter((recipe) =>
        state.searchTerms.every((term) => {
          const timeMatch = term.match(timeRegex)
          if (timeMatch) {
            const op = timeMatch[1]
            const minutes = parseInt(timeMatch[2], 10)
            const prep = recipe.prepTimeMinutes ?? null
            if (prep === null) return false
            return op === '<' ? prep < minutes : prep > minutes
          }
          const termLower = term.toLowerCase()
          const matchedBadge = BADGE_NAMES.find(b => b.toLowerCase() === termLower)
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

    searchQuery: (state) => state.searchTerms.join(', '),

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
      try {
        const data = await recipeService.getAll()
        this.recipes = data
        this._lastFetched = Date.now()
      } catch (error) {
        this.error = error.message
        console.error('Failed to fetch recipes:', error)
      } finally {
        this.loading = false
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

    clearError() {
      this.error = null
    }
  }
})
