import { defineStore } from 'pinia'
import { recipeService } from '@/services/recipeService'
import { useUiStore } from '@/stores/uiStore'
import { filterRecipes, matchTag, normalizeText, sortRecipes, SORT_OPTIONS } from '@/utils/recipeSearch'

export const QUICK_MAX_MINUTES = 30

const BADGE_NAMES = ['Energiearm', 'Proteinreich', 'Ballaststoffreich', 'Fettarm', 'Schnell']
const BADGE_SEARCH_ALIASES = { kalorienarm: 'Energiearm' }
const SORT_PREF_KEY = 'recipeSort'

const readSortPreference = () => {
  try {
    const value = localStorage.getItem(SORT_PREF_KEY)
    return SORT_OPTIONS.some((o) => o.value === value) ? value : 'default'
  } catch {
    return 'default'
  }
}

const recipeSearchText = (recipe) => normalizeText([
  recipe.title,
  recipe.description,
  recipe.author,
  recipe.source,
  recipe.createdBy,
  recipe.ingredientNames,
  recipe.translatedSearchText,
  ...(recipe.tags ?? []),
].filter(Boolean).join(' | '))

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
  translatedSearchText: recipe.translatedSearchText ?? null,
  tags: recipe.tags ?? [],
  createdAt: recipe.createdAt ?? null,
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
    sortMode: readSortPreference(),
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

    searchTexts: (state) => new Map(state.recipes.map((r) => [r.id, recipeSearchText(r)])),

    filteredRecipes() {
      const badgeMap = this.computedBadges
      const searchTexts = this.searchTexts
      const matchKeyword = (recipe, text) => {
        const tagHit = matchTag(recipe, text)
        if (tagHit !== undefined) return tagHit
        const badge = BADGE_SEARCH_ALIASES[text] ?? BADGE_NAMES.find((b) => normalizeText(b) === text)
        return badge ? (badgeMap.get(recipe.id) ?? []).includes(badge) : undefined
      }
      const filtered = filterRecipes(this.recipes, this.activeSearchTerms, {
        searchText: (recipe) => searchTexts.get(recipe.id) ?? '',
        matchKeyword,
      })
      return sortRecipes(filtered, this.sortMode)
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

    addSearchTerm(term) {
      if (!this.searchTerms.includes(term)) this.searchTerms = [...this.searchTerms, term]
    },

    setPendingSearchTerm(term) {
      this.pendingSearchTerm = term
    },

    setSortMode(mode) {
      this.sortMode = SORT_OPTIONS.some((o) => o.value === mode) ? mode : 'default'
      try {
        localStorage.setItem(SORT_PREF_KEY, this.sortMode)
      } catch {
        // Ohne Speicher gilt die Wahl nur bis zum Neuladen
      }
    },

    clearError() {
      this.error = null
    }
  }
})
