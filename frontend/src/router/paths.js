// Deutsche App-Adressen an einer Stelle; alte englische Pfade (Lesezeichen, Verläufe, geteilte Links) leiten um.
// Teilen-Links (/share/<token>) und API-Pfade bleiben unverändert.

export const NEW_RECIPE_PATH = '/rezepte/neu'
export const INGREDIENTS_PATH = '/zutaten'
export const MEAL_PLAN_PATH = '/plan'
export const RECIPE_DETAIL_ROUTE = '/rezepte/:id(\\d+)'
export const RECIPE_EDIT_ROUTE = '/rezepte/:id(\\d+)/bearbeiten'

export const recipePath = (id) => `/rezepte/${id}`
export const recipeEditPath = (id) => `/rezepte/${id}/bearbeiten`

const keep = (path) => (to) => ({ path: typeof path === 'function' ? path(to.params) : path, query: to.query, hash: to.hash })

export const LEGACY_REDIRECTS = [
  { path: '/recipe/new', redirect: keep(NEW_RECIPE_PATH) },
  { path: '/recipe/:id/edit', redirect: keep((p) => recipeEditPath(p.id)) },
  { path: '/recipe/:id', redirect: keep((p) => recipePath(p.id)) },
  { path: '/ingredients', redirect: keep(INGREDIENTS_PATH) },
]
