const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

async function parseError(response, fallback) {
  try {
    const body = await response.json()
    return body?.message || fallback
  } catch {
    return fallback
  }
}

class AuthService {
  getToken() {
    return localStorage.getItem('token')
  }

  setToken(token) {
    localStorage.setItem('token', token)
  }

  removeToken() {
    localStorage.removeItem('token')
  }

  async login(email, password) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password })
      })
      if (!response.ok) {
        throw new Error('E-Mail-Adresse oder Passwort ist falsch.')
      }
      const data = await response.json()
      this.setToken(data.token)
      return data
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    }
  }

  logout() {
    this.removeToken()
  }

  async getCurrentUser() {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/me`, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      })
      if (!response.ok) {
        throw new Error('Not authenticated')
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to get current user:', error)
      throw error
    }
  }

  async getAllUsers() {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/users`, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Laden der Benutzer.')
        throw new Error(msg)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to get users:', error)
      throw error
    }
  }

  async createUser(userData) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getToken()}`
        },
        body: JSON.stringify(userData)
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Erstellen des Benutzers.')
        throw new Error(msg)
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to create user:', error)
      throw error
    }
  }

  async updateProfile(data) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/me`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getToken()}`
        },
        body: JSON.stringify(data)
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Speichern des Profils.')
        throw new Error(msg)
      }
      return await response.json()
    } catch (error) {
      console.error('Profile update failed:', error)
      throw error
    }
  }

  async updateUser(id, data) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/users/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getToken()}`
        },
        body: JSON.stringify(data)
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Speichern des Benutzers.')
        throw new Error(msg)
      }
      return await response.json()
    } catch (error) {
      console.error('User update failed:', error)
      throw error
    }
  }

  async deleteUser(id) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/users/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Loeschen des Benutzers.')
        throw new Error(msg)
      }
    } catch (error) {
      console.error('User delete failed:', error)
      throw error
    }
  }

  async requestPasswordReset(email) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/password-reset`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email })
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Fehler beim Anfordern des Passwort-Reset.')
        throw new Error(msg)
      }
    } catch (error) {
      console.error('Password reset request failed:', error)
      throw error
    }
  }

  async resetPassword(token, newPassword) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/reset-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ token, newPassword })
      })
      if (!response.ok) {
        const msg = await parseError(response, 'Der Link ist ungueltig oder abgelaufen.')
        throw new Error(msg)
      }
    } catch (error) {
      console.error('Password reset failed:', error)
      throw error
    }
  }

  isAuthenticated() {
    return !!this.getToken()
  }
}

export const authService = new AuthService()
