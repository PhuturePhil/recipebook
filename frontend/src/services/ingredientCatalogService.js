const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

function getToken() {
  return localStorage.getItem('token')
}

async function parseError(response, fallback) {
  try {
    const body = await response.json()
    return body?.message || fallback
  } catch {
    return fallback
  }
}

class IngredientCatalogService {
  async getAll() {
    try {
      const response = await fetch(`${API_BASE_URL}/ingredient-catalog`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Laden der Zutaten.')
        throw new Error(msg)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to load ingredient catalog:', error)
      throw error
    }
  }

  async create(data) {
    try {
      const response = await fetch(`${API_BASE_URL}/ingredient-catalog`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`
        },
        body: JSON.stringify(data)
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Erstellen des Eintrags.')
        throw new Error(msg)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to create catalog entry:', error)
      throw error
    }
  }

  async update(id, data) {
    try {
      const response = await fetch(`${API_BASE_URL}/ingredient-catalog/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`
        },
        body: JSON.stringify(data)
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Speichern des Eintrags.')
        throw new Error(msg)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to update catalog entry:', error)
      throw error
    }
  }

  async delete(id) {
    try {
      const response = await fetch(`${API_BASE_URL}/ingredient-catalog/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Löschen des Eintrags.')
        throw new Error(msg)
      }
    } catch (error) {
      console.error('Failed to delete catalog entry:', error)
      throw error
    }
  }
}

export const ingredientCatalogService = new IngredientCatalogService()
