<script setup>
import { RouterView } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
</script>

<template>
  <NavBar />
  <RouterView />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
    Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  background: var(--color-bg, #f5f5f5);
  color: var(--color-text-primary, #333);
  line-height: 1.5;
}

:root {
  --color-primary: #4a5568;
  --color-primary-dark: #2d3748;
  --color-secondary: #718096;
  --color-bg: #f9fafb;
  --color-bg-card: #ffffff;
  --color-bg-secondary: #e2e8f0;
  --color-text-primary: #1a202c;
  --color-text-secondary: #4a5568;
  --color-text-muted: #a0aec0;
  --color-border: #e2e8f0;
  --color-border-light: #edf2f7;
  --color-error: #e53e3e;
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
}
</style>
