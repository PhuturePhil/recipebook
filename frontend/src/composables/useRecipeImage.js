import { ref, watch, onMounted, onUnmounted } from 'vue'
import { recipeService } from '@/services/recipeService'

// One object URL per image version for the whole page lifetime, so revisiting the list doesn't reload or flicker
const objectUrls = new Map()

function loadObjectUrl(imageUrl) {
  if (!objectUrls.has(imageUrl)) {
    const pending = recipeService.getImageObjectUrl(imageUrl)
    pending.catch(() => objectUrls.delete(imageUrl))
    objectUrls.set(imageUrl, pending)
  }
  return objectUrls.get(imageUrl)
}

const needsAuth = (url) => typeof url === 'string' && url.startsWith('/api/')

// Resolves a recipe image for <img src>: protected API images are fetched with the auth header
// once the element comes near the viewport; data and external URLs are used as they are.
export function useRecipeImage(imageUrl, target) {
  const src = ref(needsAuth(imageUrl()) ? null : imageUrl() || null)
  let observer = null

  const load = async () => {
    const url = imageUrl()
    try {
      const objectUrl = await loadObjectUrl(url)
      if (imageUrl() === url) src.value = objectUrl
    } catch (error) {
      console.warn('Rezeptbild konnte nicht geladen werden:', error)
    }
  }

  const observe = () => {
    observer?.disconnect()
    if (!needsAuth(imageUrl())) return
    if (!target.value || !('IntersectionObserver' in window)) {
      load()
      return
    }
    observer = new IntersectionObserver((entries) => {
      if (entries.some((e) => e.isIntersecting)) {
        observer.disconnect()
        load()
      }
    }, { rootMargin: '300px' })
    observer.observe(target.value)
  }

  watch(imageUrl, (url) => {
    src.value = needsAuth(url) ? null : url || null
    observe()
  })

  onMounted(observe)
  onUnmounted(() => observer?.disconnect())

  return src
}
