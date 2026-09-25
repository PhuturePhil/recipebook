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

async function request(path, { method = 'GET', body, fallback = 'Fehler bei der Anfrage.' } = {}) {
  try {
    const headers = { 'Authorization': `Bearer ${getToken()}` }
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
    if (!response.ok) {
      throw new Error(await parseError(response, fallback))
    }
    if (response.status === 204) return null
    return await response.json()
  } catch (error) {
    console.error(`Request failed: ${method} ${path}`, error)
    throw error
  }
}

class IngredientCatalogService {
  getAll() {
    return request('/ingredient-catalog', { fallback: 'Fehler beim Laden der Zutaten.' })
  }

  create(data) {
    return request('/ingredient-catalog', { method: 'POST', body: data, fallback: 'Fehler beim Erstellen der Zutat.' })
  }

  update(id, data) {
    return request(`/ingredient-catalog/${id}`, { method: 'PUT', body: data, fallback: 'Fehler beim Speichern der Zutat.' })
  }

  delete(id) {
    return request(`/ingredient-catalog/${id}`, { method: 'DELETE', fallback: 'Fehler beim Löschen der Zutat.' })
  }

  addAlias(id, alias) {
    return request(`/ingredient-catalog/${id}/aliases`, { method: 'POST', body: { alias }, fallback: 'Fehler beim Anlegen des Alias.' })
  }

  removeAlias(id, aliasId) {
    return request(`/ingredient-catalog/${id}/aliases/${aliasId}`, { method: 'DELETE', fallback: 'Fehler beim Entfernen des Alias.' })
  }

  adoptLegacy(id, legacyId) {
    return request(`/ingredient-catalog/${id}/adopt-legacy/${legacyId}`, { method: 'POST', fallback: 'Fehler beim Übernehmen des Altwerts.' })
  }

  getConversions() {
    return request('/ingredient-catalog/conversions', { fallback: 'Fehler beim Laden der Umrechnungen.' })
  }

  saveConversion(conversion) {
    if (conversion.id) {
      return request(`/ingredient-catalog/conversions/${conversion.id}`, { method: 'PUT', body: conversion, fallback: 'Fehler beim Speichern der Umrechnung.' })
    }
    return request('/ingredient-catalog/conversions', { method: 'POST', body: conversion, fallback: 'Fehler beim Speichern der Umrechnung.' })
  }

  deleteConversion(id) {
    return request(`/ingredient-catalog/conversions/${id}`, { method: 'DELETE', fallback: 'Fehler beim Löschen der Umrechnung.' })
  }

  getAiRequests() {
    return request('/ingredient-catalog/ai-requests', { fallback: 'Fehler beim Laden der KI-Anfragen.' })
  }

  retryAiRequest(id) {
    return request(`/ingredient-catalog/ai-requests/${id}/retry`, { method: 'POST', fallback: 'Fehler beim erneuten Versuch.' })
  }

  resolveUnknown() {
    return request('/ingredient-catalog/ai-requests/resolve-unknown', { method: 'POST', fallback: 'Fehler beim Anstoßen der Zuordnung.' })
  }

  searchReferenceFoods(query) {
    return request(`/nutrition/reference-foods?q=${encodeURIComponent(query)}`, { fallback: 'Fehler bei der BLS-Suche.' })
  }
}

export const ingredientCatalogService = new IngredientCatalogService()

export const INGREDIENT_CLASSES = [
  { value: 'DEFAULT', label: 'Standard' },
  { value: 'VEGETABLE', label: 'Gemüse/Obst' },
  { value: 'LIQUID', label: 'Flüssigkeit' },
  { value: 'OIL', label: 'Öl' },
  { value: 'FAT', label: 'Fett (Butter)' },
  { value: 'FLOUR', label: 'Mehl/Pulver' },
  { value: 'SUGAR', label: 'Zucker' },
  { value: 'SYRUP', label: 'Honig/Sirup' },
  { value: 'SPICE_GROUND', label: 'Gewürz gemahlen' },
  { value: 'SPICE_SEED', label: 'Gewürz ganz/Samen' },
  { value: 'SALT', label: 'Salz' },
  { value: 'PASTE', label: 'Paste/Mus' },
  { value: 'DAIRY', label: 'Milchprodukt' },
  { value: 'GRAIN', label: 'Getreide/Hülsenfrüchte' },
  { value: 'NUTS', label: 'Nüsse/Kerne' },
  { value: 'HERB_FRESH', label: 'Kräuter frisch' },
  { value: 'CHEESE', label: 'Käse' },
]

export const classLabel = (value) => INGREDIENT_CLASSES.find(c => c.value === value)?.label ?? value ?? '—'
