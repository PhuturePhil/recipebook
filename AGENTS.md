# AGENTS.md - RecipeBook Development Guide

## Project Overview

- **Frontend:** Vue 3 + Vite + Pinia + Vue Router + Plain CSS
- **Backend:** Java/SpringBoot + PostgreSQL
- **Development:** Docker für PostgreSQL

---

## Build/Lint/Test Commands

### Frontend (Vue.js)

```bash
# Dependencies
npm install                    # Install dependencies
npm install <package>          # Add new dependency

# Development
npm run dev                    # Start development server (localhost:5173)
npm run build                  # Production build
npm run preview                # Preview production build

# Linting
npm run lint                   # Run ESLint
npm run lint:fix               # Fix ESLint errors automatically

# Testing
npm run test                   # Run all tests
npm run test:unit              # Run unit tests (Vitest)
npm run test:watch             # Watch mode for tests
npm run test:coverage          # Run tests with coverage
npm run test:single <file>     # Run single test file
npm run test:e2e               # Run E2E tests (Cypress/Playwright)
```

### Backend (SpringBoot)

```bash
# Build
./mvnw clean install           # Build project
./mvnw clean package            # Package without tests

# Run
./mvnw spring-boot:run          # Start application
java -jar target/recipebook.jar # Run JAR

# Testing
./mvnw test                     # Run all tests
./mvnw test -Dtest=<ClassName>  # Run single test class
./mvnw test -Dtest=<ClassName>#<methodName>  # Run single test method
./mvnw verify                   # Run tests and verify
```

### Docker

```bash
# PostgreSQL
docker-compose up -d           # Start PostgreSQL
docker-compose down            # Stop PostgreSQL
docker-compose logs -f         # View logs
```

---

## Code Style Guidelines

### General Principles

- **Simplicity:** Keep code simple and readable
- **Consistency:** Follow established patterns in the codebase
- **Modularity:** Separate concerns into distinct components/services
- **No Comments:** Avoid comments unless absolutely necessary (e.g., complex business logic)

### Project Structure

```
frontend/
├── src/
│   ├── assets/          # Static assets (images, global CSS)
│   ├── components/      # Reusable Vue components
│   │   ├── RecipeCard.vue
│   │   ├── RecipeForm.vue
│   │   └── SearchBar.vue
│   ├── views/           # Page-level components
│   │   ├── HomeView.vue
│   │   ├── RecipeDetail.vue
│   │   └── RecipeEdit.vue
│   ├── stores/          # Pinia stores
│   │   └── recipeStore.js
│   ├── router/          # Vue Router config
│   ├── services/        # API services
│   │   └── recipeService.js
│   ├── App.vue
│   └── main.js
├── index.html
└── vite.config.js

backend/
├── src/main/java/com/recipebook/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   └── RecipeBookApplication.java
└── pom.xml
```

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Vue Components | PascalCase | `RecipeCard.vue` |
| Composables/Stores | camelCase + use- prefix | `useRecipeStore.js` |
| Variables/Functions | camelCase | `fetchRecipes()` |
| CSS Classes | kebab-case | `.recipe-card` |
| Database Tables | snake_case | `recipe_details` |
| API Endpoints | kebab-case | `/api/recipes` |

### Imports

```javascript
// Vue components
import RecipeCard from '@/components/RecipeCard.vue'

// Pinia stores
import { useRecipeStore } from '@/stores/recipeStore'

// Services
import { recipeService } from '@/services/recipeService'

// Relative imports for same-layer modules
import { formatDate } from '@/utils/formatDate'
```

**Rule:** Use `@/` alias for absolute imports from `src/`. Avoid deep relative imports (`../../..`).

### Formatting

- **Indentation:** 2 spaces (no tabs)
- **Line Length:** Maximum 100 characters
- **Trailing Commas:** Yes, in multiline objects/arrays
- **Quotes:** Single quotes for strings
- **Semicolons:** No (except where necessary)
- **Curly Braces:** Same line for control structures

```javascript
// Good
const recipe = {
  title: 'Pasta',
  ingredients: ['flour', 'eggs']
}

if (recipe.title) {
  console.log(recipe.title)
}

// Bad
const recipe = { title: 'Pasta', ingredients: ['flour', 'eggs'] }

if (recipe.title)
{
  console.log(recipe.title)
}
```

### TypeScript (if used)

- Use `interface` for object shapes
- Use `type` for unions, primitives
- Enable strict mode
- Define prop types for Vue components

```typescript
interface Recipe {
  id: number
  title: string
  description: string
  instructions: string[]
  ingredients: Ingredient[]
}

interface Ingredient {
  name: string
  amount: string
  unit: string
}
```

### Vue 3 Composition API

- Use `<script setup>` syntax
- Use `ref` for primitives, `reactive` for objects
- Destructure props with `defineProps`
- Use `computed` for derived state

```vue
<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  recipe: {
    type: Object,
    required: true
  }
})

const searchQuery = ref('')
const filteredRecipes = computed(() => 
  recipes.filter(r => r.title.includes(searchQuery.value))
)
</script>
```

### Error Handling

```javascript
// API calls with error handling
async function fetchRecipes() {
  try {
    const response = await fetch('/api/recipes')
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    return await response.json()
  } catch (error) {
    console.error('Failed to fetch recipes:', error)
    throw error // Re-throw for component handling
  }
}
```

**Rules:**
- Always use try/catch for async operations
- Show user-friendly error messages in UI
- Log errors for debugging
- Use finally for cleanup (e.g., loading states)

### CSS Guidelines

- Use CSS variables for theming
- Follow BEM naming for complex components
- Keep specificity low
- Use flexbox/grid for layouts

```css
:root {
  --color-primary: #4a5568;
  --color-secondary: #718096;
  --spacing-md: 16px;
}

.recipe-card {
  padding: var(--spacing-md);
}

.recipe-card__title {
  font-size: 1.25rem;
  color: var(--color-primary);
}
```

---

## API Contract

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/recipes` | List all recipes |
| GET | `/api/recipes/{id}` | Get single recipe |
| POST | `/api/recipes` | Create recipe |
| PUT | `/api/recipes/{id}` | Update recipe |
| DELETE | `/api/recipes/{id}` | Delete recipe |
| GET | `/api/recipes/search?q={query}` | Search recipes |

### Recipe Model

```json
{
  "id": 1,
  "title": "Spaghetti Carbonara",
  "description": "Classic Italian pasta dish",
  "instructions": ["Boil pasta", "Mix eggs and cheese", "Combine"],
  "ingredients": [
    { "name": "Spaghetti", "amount": "400", "unit": "g" },
    { "name": "Eggs", "amount": "4", "unit": "pcs" }
  ]
}
```

---

## Development Workflow

1. Create feature branch from `main`
2. Make changes following code style guidelines
3. Run linter: `npm run lint`
4. Run tests: `npm run test`
5. Commit with descriptive message
6. Create pull request

---

## Notes

- Always verify changes work with `npm run dev` before committing
- Run full test suite before submitting PRs
- Keep API backwards compatible
- Document any new dependencies added
