import { defineStore } from 'pinia'
import { recipeService } from '@/services/recipeService'

export const useRecipeStore = defineStore('recipe', {
  state: () => ({
    recipes: [],
    currentRecipe: null,
    loading: false,
    error: null,
    searchQuery: ''
  }),

  getters: {
    filteredRecipes: (state) => {
      if (!state.searchQuery) return state.recipes
      const query = state.searchQuery.toLowerCase()
      return state.recipes.filter(
        (recipe) =>
          recipe.title.toLowerCase().includes(query) ||
          recipe.description.toLowerCase().includes(query)
      )
    },

    getRecipeById: (state) => (id) => {
      return state.recipes.find((recipe) => recipe.id === parseInt(id))
    }
  },

  actions: {
    async fetchRecipes() {
      this.loading = true
      this.error = null
      try {
        const data = await recipeService.getAll()
        this.recipes = data
      } catch (error) {
        this.error = error.message
        console.error('Failed to fetch recipes:', error)
      } finally {
        this.loading = false
      }
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
        this.recipes.push(data)
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
      try {
        const data = await recipeService.update(id, recipe)
        const index = this.recipes.findIndex((r) => r.id === id)
        if (index !== -1) {
          this.recipes[index] = data
        }
        if (this.currentRecipe && this.currentRecipe.id === id) {
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
      try {
        await recipeService.delete(id)
        this.recipes = this.recipes.filter((r) => r.id !== id)
        if (this.currentRecipe && this.currentRecipe.id === id) {
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

    setSearchQuery(query) {
      this.searchQuery = query
    },

    clearError() {
      this.error = null
    }
  }
})
