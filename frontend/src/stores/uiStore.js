import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const navTitle = ref('')

  function setNavTitle(title) {
    navTitle.value = title
  }

  function clearNavTitle() {
    navTitle.value = ''
  }

  return { navTitle, setNavTitle, clearNavTitle }
})
