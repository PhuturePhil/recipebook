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

async function request(path, { method = 'GET', body } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: { ...getAuthHeaders(), ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) throw await httpError(response)
  return response.status === 204 ? null : response.json()
}

// Familien: alle Mitglieder sind gleichberechtigt; fremde Familien gibt es für den Server nicht (404)
export const familyService = {
  list: () => request('/families'),
  get: (id) => request(`/families/${id}`),
  create: (name) => request('/families', { method: 'POST', body: { name } }),
  rename: (id, name) => request(`/families/${id}`, { method: 'PUT', body: { name } }),
  renewInvite: (id) => request(`/families/${id}/invite`, { method: 'POST' }),
  // Eigene ID = austreten; { deleted: true }, wenn man das letzte Mitglied war
  removeMember: (id, userId) => request(`/families/${id}/members/${userId}`, { method: 'DELETE' }),
  // Öffentliche Einladungsseite (auch ohne Login)
  inviteInfo: (code) => request(`/family-invites/${encodeURIComponent(code)}`),
  join: (code) => request(`/family-invites/${encodeURIComponent(code)}/join`, { method: 'POST' }),
}
