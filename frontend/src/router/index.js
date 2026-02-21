import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/recipe/:id',
      name: 'recipe-detail',
      component: () => import('@/views/RecipeDetail.vue')
    },
    {
      path: '/recipe/new',
      name: 'recipe-new',
      component: () => import('@/views/RecipeEdit.vue')
    },
    {
      path: '/recipe/:id/edit',
      name: 'recipe-edit',
      component: () => import('@/views/RecipeEdit.vue')
    }
  ]
})

export default router
