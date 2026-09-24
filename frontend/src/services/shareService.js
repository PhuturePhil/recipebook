const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

function getAuthHeaders() {
  const token = localStorage.getItem('token')
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

class ShareService {
  async getShareLink(recipeId) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/${recipeId}/share`, {
        headers: { ...getAuthHeaders() }
      })
      if (response.status === 204) {
        return null
      }
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to fetch share link:', error)
      throw error
    }
  }

  async createShareLink(recipeId) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/${recipeId}/share`, {
        method: 'POST',
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to create share link:', error)
      throw error
    }
  }

  async revokeShareLink(recipeId) {
    try {
      const response = await fetch(`${API_BASE_URL}/recipes/${recipeId}/share`, {
        method: 'DELETE',
        headers: { ...getAuthHeaders() }
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
    } catch (error) {
      console.error('Failed to revoke share link:', error)
      throw error
    }
  }

  async getSharedRecipe(token) {
    const response = await fetch(`${API_BASE_URL}/share/${encodeURIComponent(token)}`)
    if (!response.ok) {
      const error = new Error(`HTTP error! status: ${response.status}`)
      error.status = response.status
      throw error
    }
    return await response.json()
  }
}

export const shareService = new ShareService()
