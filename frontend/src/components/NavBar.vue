<template>
  <nav class="navbar">
    <div class="navbar__content">
      <router-link to="/" class="navbar__logo">
        <span class="navbar__title">Pastoors Familienrezepte</span>
      </router-link>

      <div class="navbar__actions">
        <div v-if="isAuthenticated" class="navbar__menu">
          <button @click.stop="toggleMenu" class="navbar__burger">&#9776;</button>
          <div v-if="showMenu" class="navbar__dropdown">
            <router-link to="/changelog" class="navbar__dropdown-item" @click="showMenu = false">
              Neuerungen
            </router-link>
            <router-link v-if="isAdmin" to="/admin/users" class="navbar__dropdown-item" @click="showMenu = false">
              Benutzerverwaltung
            </router-link>
            <button class="navbar__dropdown-item" @click="openProfile">
              Persönliche Daten ändern
            </button>
            <button class="navbar__dropdown-item navbar__dropdown-item--logout" @click="handleLogout">
              Ausloggen
            </button>
          </div>
        </div>

        <router-link v-if="showSearch && isAuthenticated" to="/recipe/new" class="navbar__btn navbar__btn--primary">
          Rezept hinzufügen
        </router-link>

        <div v-if="showSearch && isAuthenticated" class="navbar__search">
          <SearchBar />
        </div>

        <router-link v-if="!isAuthenticated" to="/login" class="navbar__btn navbar__btn--primary">
          Login
        </router-link>
      </div>
    </div>
  </nav>

  <ProfileModal v-if="showProfileModal" @close="showProfileModal = false" />
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import SearchBar from '@/components/SearchBar.vue'
import ProfileModal from '@/components/ProfileModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isAuthenticated = computed(() => authStore.isAuthenticated)
const isAdmin = computed(() => authStore.isAdmin)

const showSearch = computed(() => route.path === '/')
const showMenu = ref(false)
const showProfileModal = ref(false)

function toggleMenu() {
  showMenu.value = !showMenu.value
}

function openProfile() {
  showMenu.value = false
  showProfileModal.value = true
}

function handleLogout() {
  showMenu.value = false
  authStore.logout()
  router.push('/login')
}

function closeMenu() {
  showMenu.value = false
}

watch(() => route.path, () => {
  showMenu.value = false
})

onMounted(() => {
  document.addEventListener('click', closeMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeMenu)
})
</script>

<style scoped>
.navbar {
  background: var(--color-bg-card, #fff);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar__content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.navbar__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--color-text-primary, #333);
}

.navbar__title {
  font-size: 1.25rem;
  font-weight: 600;
}

.navbar__actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.navbar__menu {
  position: relative;
}

.navbar__burger {
  background: none;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 1.25rem;
  cursor: pointer;
  color: var(--color-text-primary, #333);
  line-height: 1;
  transition: background-color 0.2s ease;
}

.navbar__burger:hover {
  background: var(--color-bg-secondary, #f0f0f0);
}

.navbar__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 220px;
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--color-border, #ddd);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 200;
}

.navbar__dropdown-item {
  padding: 12px 16px;
  text-decoration: none;
  color: var(--color-text-primary, #333);
  font-size: 0.9rem;
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease;
  border-bottom: 1px solid var(--color-border-light, #eee);
}

.navbar__dropdown-item:last-child {
  border-bottom: none;
}

.navbar__dropdown-item:hover {
  background: var(--color-bg-secondary, #f0f0f0);
}

.navbar__dropdown-item--logout {
  color: var(--color-error, #e53e3e);
}

.navbar__btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.2s ease;
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.navbar__btn:hover {
  background: var(--color-border, #ddd);
}

.navbar__btn--primary {
  background: var(--color-primary, #4a5568);
  color: white;
}

.navbar__btn--primary:hover {
  background: var(--color-primary-dark, #2d3748);
}

.navbar__search {
  min-width: 200px;
}

@media (max-width: 600px) {
  .navbar__content {
    flex-wrap: wrap;
  }

  .navbar__actions {
    flex-wrap: wrap;
  }

  .navbar__search {
    width: 100%;
    min-width: unset;
  }

  .navbar__dropdown {
    left: auto;
    right: 0;
  }
}
</style>
