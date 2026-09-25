const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

function getAuthHeaders() {
  const token = localStorage.getItem('token')
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

let infoCache = null

class NutritionService {
  async getRecipeNutrition(recipeId) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/${recipeId}/nutrition`, {
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to fetch nutrition:', error)
      throw error
    }
  }

  async getInfo() {
    if (infoCache) return infoCache
    try {
      const response = await fetch(`${API_BASE_URL}/nutrition/info`, {
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      infoCache = await response.json()
      return infoCache
    } catch (error) {
      console.error('Failed to fetch nutrition info:', error)
      throw error
    }
  }
}

export const nutritionService = new NutritionService()

export const SOURCE_LABELS = {
  BLS: 'BLS',
  MANUAL: 'manuell',
  AI_ESTIMATE: 'KI-Schätzung',
}

export const STATUS_LABELS = {
  NO_AMOUNT: 'keine Menge',
  UNKNOWN_INGREDIENT: 'unbekannte Zutat',
  NO_NUTRIENT_DATA: 'keine Nährwerte',
  NO_CONVERSION: 'keine Umrechnung',
  NEGLIGIBLE: 'vernachlässigbar',
}

export const formatKcal = (value) => (value == null ? '–' : Math.round(value).toLocaleString('de-DE'))

export const formatGram = (value) => {
  if (value == null) return '–'
  const digits = Math.abs(value) < 10 ? 1 : 0
  return value.toLocaleString('de-DE', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export const formatMicro = (value) => {
  if (value == null) return '–'
  if (Math.abs(value) >= 100) return Math.round(value).toLocaleString('de-DE')
  return value.toLocaleString('de-DE', { maximumFractionDigits: Math.abs(value) < 1 ? 2 : 1 })
}
