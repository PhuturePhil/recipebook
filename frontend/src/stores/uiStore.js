import { defineStore } from 'pinia'
import { ref } from 'vue'

export const TOAST_MS = 3500

export const useUiStore = defineStore('ui', () => {
  const navTitle = ref('')
  const loadingActive = ref(false)
  const loadingMessage = ref('')
  // Knopf rechts in der Kopfzeile, den eine Seite setzen kann ({ icon, label, run })
  const headerAction = ref(null)
  const toast = ref('')
  let toastTimer = null

  function setNavTitle(title) {
    navTitle.value = title
  }

  function clearNavTitle() {
    navTitle.value = ''
  }

  function showLoading(message = '') {
    loadingMessage.value = message
    loadingActive.value = true
  }

  function hideLoading() {
    loadingActive.value = false
    loadingMessage.value = ''
  }

  function setHeaderAction(action) {
    headerAction.value = action
  }

  function showToast(text) {
    clearTimeout(toastTimer)
    toast.value = text
    toastTimer = setTimeout(() => { toast.value = '' }, TOAST_MS)
  }

  return {
    navTitle, setNavTitle, clearNavTitle,
    loadingActive, loadingMessage, showLoading, hideLoading,
    headerAction, setHeaderAction,
    toast, showToast,
  }
})
