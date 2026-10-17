import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { useAuthStore } from '@/stores/authStore'

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
      path: '/share/:token',
      name: 'shared-recipe',
      component: () => import('@/views/SharedRecipeView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/ingredients',
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
      path: '/recipe/:id',
      name: 'recipe-detail',
      component: () => import('@/views/RecipeDetail.vue'),
      // Öffentliche Rezepte auch für Gäste; alles andere beantwortet der Server mit 404
      meta: { requiresAuth: false }
    },
    {
      path: '/recipe/new',
      name: 'recipe-new',
      component: () => import('@/views/RecipeEdit.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/recipe/:id/edit',
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
  } else if (to.name === 'login' && authStore.isAuthenticated) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
