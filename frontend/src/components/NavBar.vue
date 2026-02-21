<template>
  <nav class="navbar">
    <div class="navbar__content">
      <router-link to="/" class="navbar__logo">
        <span class="navbar__title">Pastoors Familienrezepte</span>
        <span class="navbar__heart">❤️</span>
      </router-link>

      <div class="navbar__actions">
        <router-link v-if="showSearch" to="/recipe/new" class="navbar__btn navbar__btn--primary">
          Rezept hinzufügen
        </router-link>
        <div v-if="showSearch" class="navbar__search">
          <SearchBar />
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SearchBar from '@/components/SearchBar.vue'

const route = useRoute()

const showSearch = computed(() => {
  return route.path === '/'
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

.navbar__heart {
  font-size: 1.25rem;
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
