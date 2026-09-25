const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

function getAuthHeaders() {
  const token = localStorage.getItem('token')
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

async function httpError(response) {
  let message = `HTTP error! status: ${response.status}`
  try {
    const body = await response.json()
    if (body?.message) message = body.message
  } catch {
    // keep the generic message when the body is not JSON
  }
  const error = new Error(message)
  error.status = response.status
  return error
}

class RecipeService {
  async getAll() {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes`, {
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw await httpError(response)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to fetch recipes:', error)
      throw error
    }
  }

  async getById(id) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/${id}`, {
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw await httpError(response)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to fetch recipe:', error)
      throw error
    }
  }

  // Recipe images need the auth header, so they are fetched here instead of via <img src>.
  // The URL carries a content hash, which lets the browser cache answer repeat visits.
  async getImageObjectUrl(imageUrl) {
    const url = imageUrl.startsWith('/api/') ? API_BASE_URL + imageUrl.slice(4) : imageUrl
    const response = await fetch(url, { headers: { ...getAuthHeaders() } })
    if (!response.ok) {
      throw await httpError(response)
    }
    return URL.createObjectURL(await response.blob())
  }

  async create(recipe) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeaders()
        },
        body: JSON.stringify(recipe)
      })
      if (!response.ok) {
        throw await httpError(response)
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
          'Content-Type': 'application/json',
          ...getAuthHeaders()
        },
        body: JSON.stringify(recipe)
      })
      if (!response.ok) {
        throw await httpError(response)
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
        method: 'DELETE',
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw await httpError(response)
      }
    } catch (error) {
      console.error('Failed to delete recipe:', error)
      throw error
    }
  }

  async getSources() {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/sources`, {
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw await httpError(response)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to fetch sources:', error)
      throw error
    }
  }

  async getUnits() {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/units`, {
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw await httpError(response)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to fetch units:', error)
      throw error
    }
  }

  async scanRecipe(images) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/scan`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeaders()
        },
        body: JSON.stringify(images)
      })
      if (!response.ok) {
        throw await httpError(response)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to scan recipe:', error)
      throw error
    }
  }
}

export const recipeService = new RecipeService()
