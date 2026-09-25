import { authService } from '@/services/authService'

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

function hasBearerToken(init) {
  return new Headers(init?.headers).get('Authorization')?.startsWith('Bearer ') ?? false
}

// Sends the user to the login page as soon as the API rejects an expired or revoked token,
// instead of leaving each view to show a raw HTTP error.
export function installSessionGuard({ authStore, router }) {
  const originalFetch = window.fetch.bind(window)
  let pendingCheck = null

  async function sessionStillValid() {
    if (authService.isTokenExpired()) return false
    const response = await originalFetch(`${API_BASE_URL}/auth/me`, {
      headers: { 'Authorization': `Bearer ${authService.getToken()}` }
    })
    return response.status !== 401 && response.status !== 403
  }

  function endSession() {
    if (!authStore.isAuthenticated) return
    authStore.sessionExpired = true
    authStore.logout()
    const current = router.currentRoute.value
    router.push({ name: 'login', query: current.meta.requiresAuth ? { redirect: current.fullPath } : {} })
  }

  window.fetch = async (input, init) => {
    const response = await originalFetch(input, init)
    if ((response.status === 401 || response.status === 403) && authStore.isAuthenticated && hasBearerToken(init)) {
      // A 403 can also mean "not allowed" with a valid session, so ask the server once
      pendingCheck ??= sessionStillValid()
        .catch(() => true)
        .finally(() => { pendingCheck = null })
      if (!(await pendingCheck)) {
        endSession()
      }
    }
    return response
  }
}
