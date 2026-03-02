import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const navTitle = ref('')
  const loadingActive = ref(false)
  const loadingMessage = ref('')

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

  return { navTitle, setNavTitle, clearNavTitle, loadingActive, loadingMessage, showLoading, hideLoading }
})
