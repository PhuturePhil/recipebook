import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { useAuthStore } from '@/stores/authStore'
import { LEGACY_REDIRECTS, NEW_RECIPE_PATH, INGREDIENTS_PATH, MEAL_PLAN_PATH, RECIPE_DETAIL_ROUTE, RECIPE_EDIT_ROUTE } from './paths.js'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/login/oidc',
      name: 'oidc-callback',
      component: () => import('@/views/OidcCallbackView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('@/views/ResetPasswordView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/invite',
      name: 'invite',
      component: () => import('@/views/RegisterWithInviteView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/registrieren',
      name: 'signup',
      component: () => import('@/views/SignupView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/konto-bestaetigen',
      name: 'verify-email',
      component: () => import('@/views/VerifyEmailView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/email-bestaetigen',
      name: 'confirm-email',
      component: () => import('@/views/ConfirmEmailView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/impressum',
      name: 'impressum',
      component: () => import('@/views/ImpressumView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/datenschutz',
      name: 'datenschutz',
      component: () => import('@/views/DatenschutzView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/share/:token',
      name: 'shared-recipe',
      component: () => import('@/views/SharedRecipeView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: MEAL_PLAN_PATH,
      name: 'meal-plan',
      component: () => import('@/views/MealPlanView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: INGREDIENTS_PATH,
      name: 'ingredients',
      component: () => import('@/views/IngredientsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/views/AdminUsersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
      // Gäste sehen hier die Gast-Startseite
      meta: { requiresAuth: false }
    },
    {
      path: RECIPE_DETAIL_ROUTE,
      name: 'recipe-detail',
      component: () => import('@/views/RecipeDetail.vue'),
      // Öffentliche Rezepte auch für Gäste; alles andere beantwortet der Server mit 404
      meta: { requiresAuth: false }
    },
    {
      path: NEW_RECIPE_PATH,
      name: 'recipe-new',
      component: () => import('@/views/RecipeEdit.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: RECIPE_EDIT_ROUTE,
      name: 'recipe-edit',
      component: () => import('@/views/RecipeEdit.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/naehrwerte',
      name: 'nutrition-info',
      component: () => import('@/views/NutritionInfoView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/changelog',
      name: 'changelog',
      component: () => import('@/views/ChangelogView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/familien',
      name: 'families',
      component: () => import('@/views/FamiliesView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/familien/:id(\\d+)',
      name: 'family-detail',
      component: () => import('@/views/FamilyDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/einladung/:code',
      name: 'family-invite',
      component: () => import('@/views/FamilyInviteView.vue'),
      meta: { requiresAuth: false }
    },
    ...LEGACY_REDIRECTS,
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  await authStore.init()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'home' })
  } else if ((to.name === 'login' || to.name === 'signup') && authStore.isAuthenticated) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
