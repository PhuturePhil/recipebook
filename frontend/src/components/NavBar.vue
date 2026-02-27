<template>
  <nav class="navbar">
    <div class="navbar__content">
      <router-link to="/" class="navbar__logo">
        <span class="navbar__title">Pastoors Familienrezepte</span>
      </router-link>

      <div class="navbar__actions">
        <router-link v-if="isAuthenticated && isAdmin" to="/admin/users" class="navbar__link">
          Benutzer
        </router-link>

        <router-link v-if="isAuthenticated" to="/changelog" class="navbar__link">
          Updates
        </router-link>

        <router-link v-if="showSearch && isAuthenticated" to="/recipe/new" class="navbar__btn navbar__btn--primary">
          Rezept hinzufügen
        </router-link>

        <div v-if="showSearch && isAuthenticated" class="navbar__search">
          <SearchBar />
        </div>

        <template v-if="isAuthenticated">
          <button @click="showProfileModal = true" class="navbar__user navbar__user--btn">
            {{ fullName }}
          </button>
          <button @click="handleLogout" class="navbar__btn navbar__btn--secondary">
            Logout
          </button>
        </template>
        <router-link v-else to="/login" class="navbar__btn navbar__btn--primary">
          Login
        </router-link>
      </div>
    </div>
  </nav>

  <ProfileModal v-if="showProfileModal" @close="showProfileModal = false" />
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import SearchBar from '@/components/SearchBar.vue'
import ProfileModal from '@/components/ProfileModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isAuthenticated = computed(() => authStore.isAuthenticated)
const isAdmin = computed(() => authStore.isAdmin)
const fullName = computed(() => authStore.fullName)

const showSearch = computed(() => {
  return route.path === '/'
})

const showProfileModal = ref(false)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
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
  flex-wrap: wrap;
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

.navbar__link {
  padding: 10px 15px;
  text-decoration: none;
  color: var(--color-primary, #4a5568);
  font-weight: 500;
}

.navbar__link:hover {
  color: var(--color-primary-dark, #2d3748);
}

.navbar__user {
  padding: 10px 15px;
  color: #666;
  font-size: 0.9rem;
}

.navbar__user--btn {
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.navbar__user--btn:hover {
  color: var(--color-primary, #4a5568);
}

.navbar__actions {
  display: flex;
  gap: 12px;
  align-items: center;
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
    flex-direction: column;
    text-align: center;
  }

  .navbar__title {
    font-size: 1.1rem;
  }

  .navbar__actions {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }

  .navbar__search {
    width: 100%;
    min-width: unset;
  }
}
</style>
