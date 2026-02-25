import { defineStore } from 'pinia'
import { authService } from '@/services/authService'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isAuthenticated: false,
    loading: false,
    error: null
  }),

  getters: {
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isUser: (state) => state.user?.role === 'USER',
    fullName: (state) => {
      if (!state.user) return ''
      const parts = [state.user.vorname, state.user.nachname].filter(Boolean)
      return parts.length > 0 ? parts.join(' ') : state.user.email
    },
    needsProfileSetup: (state) =>
      state.user && (!state.user.vorname || !state.user.nachname || state.user.mustChangePassword)
  },

  actions: {
    async login(email, password) {
      this.loading = true
      this.error = null
      try {
        const data = await authService.login(email, password)
        this.user = data.user
        this.isAuthenticated = true
        return true
      } catch (error) {
        this.error = error.message
        return false
      } finally {
        this.loading = false
      }
    },

    logout() {
      authService.logout()
      this.user = null
      this.isAuthenticated = false
    },

    async checkAuth() {
      if (!authService.isAuthenticated()) {
        this.isAuthenticated = false
        return false
      }

      try {
        this.user = await authService.getCurrentUser()
        this.isAuthenticated = true
        return true
      } catch (error) {
        this.logout()
        return false
      }
    },

    async updateProfile(data) {
      this.loading = true
      this.error = null
      try {
        this.user = await authService.updateProfile(data)
        return true
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async createUser(userData) {
      this.loading = true
      this.error = null
      try {
        const user = await authService.createUser(userData)
        return user
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async updateUser(id, data) {
      this.loading = true
      this.error = null
      try {
        const user = await authService.updateUser(id, data)
        return user
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async deleteUser(id) {
      this.loading = true
      this.error = null
      try {
        await authService.deleteUser(id)
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    }
  }
})
