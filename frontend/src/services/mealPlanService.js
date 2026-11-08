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

// Essensplan je Familie (nur Mitglieder, sonst 403); Datum als yyyy-MM-dd
export const mealPlanService = {
  list: (familyId, from, to) =>
    request(`/meal-plan?familyId=${encodeURIComponent(familyId)}&from=${from}&to=${to}`),
  // Entweder recipeId oder freeText; shareWithFamily: eigenes Rezept dabei für die Familie freigeben (nur Besitzer),
  // sonst sieht die Familie es, wenn nötig, nur über den Plan
  add: ({ familyId, date, recipeId = null, freeText = null, shareWithFamily = false }) =>
    request('/meal-plan', { method: 'POST', body: { familyId, date, recipeId, freeText, shareWithFamily } }),
  move: (id, date) => request(`/meal-plan/${id}`, { method: 'PATCH', body: { date } }),
  remove: (id) => request(`/meal-plan/${id}`, { method: 'DELETE' }),
  // Rezept eines Eintrags („nur über den Plan“ oder regulär, dann mit recipeId); Geist → 404
  recipe: (id) => request(`/meal-plan/${id}/recipe`),
  nutrition: (id) => request(`/meal-plan/${id}/recipe/nutrition`),
  translation: (id, lang = 'de') => request(`/meal-plan/${id}/recipe/translation?lang=${encodeURIComponent(lang)}`),
}
