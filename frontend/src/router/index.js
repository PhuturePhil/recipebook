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
      meta: { requiresAuth: true }
    },
    {
      path: '/recipe/:id',
      name: 'recipe-detail',
      component: () => import('@/views/RecipeDetail.vue'),
      meta: { requiresAuth: true }
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
      path: '/changelog',
      name: 'changelog',
      component: () => import('@/views/ChangelogView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  
  if (!authStore.user && authStore.isAuthenticated) {
    await authStore.checkAuth()
  }
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login' })
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'home' })
  } else if (to.name === 'login' && authStore.isAuthenticated) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
