const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

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
        throw new Error('Invalid credentials')
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
        throw new Error('Failed to fetch users')
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
        throw new Error('Failed to create user')
      }
      return await response.json()
    } catch (error) {
      console.error('Failed to create user:', error)
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
        throw new Error('Failed to request password reset')
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
        throw new Error('Failed to reset password')
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
