const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

class RecipeService {
  async getAll() {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to fetch recipes:', error)
      throw error
    }
  }

  async getById(id) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/${id}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to fetch recipe:', error)
      throw error
    }
  }

  async create(recipe) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(recipe)
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to create recipe:', error)
      throw error
    }
  }

  async update(id, recipe) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(recipe)
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to update recipe:', error)
      throw error
    }
  }

  async delete(id) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/${id}`, {
        method: 'DELETE'
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
    } catch (error) {
      console.error('Failed to delete recipe:', error)
      throw error
    }
  }

  async search(query) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/search?q=${encodeURIComponent(query)}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to search recipes:', error)
      throw error
    }
  }
}

export const recipeService = new RecipeService()
