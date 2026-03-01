import { defineStore } from 'pinia'
import { recipeService } from '@/services/recipeService'

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
    filteredRecipes: (state) => {
      if (!state.searchTerms.length) return state.recipes
      const timeRegex = /^([<>])\s*(\d+)$/
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
          const q = term.toLowerCase()
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
