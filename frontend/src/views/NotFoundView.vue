<template>
  <div class="not-found">
    <h1>Seite nicht gefunden</h1>
    <p>Diese Adresse gibt es in der Rezeptsammlung nicht.</p>
    <router-link to="/" class="btn-home">Zur Übersicht</router-link>
  </div>
</template>

<style scoped>
.not-found {
  max-width: 480px;
  margin: 64px auto;
  padding: 0 24px;
  text-align: center;
  color: var(--color-text-secondary, #666);
}

.not-found h1 {
  font-size: 1.5rem;
  color: var(--color-text-primary, #333);
  margin-bottom: 12px;
}

.btn-home {
  display: inline-block;
  margin-top: 24px;
  padding: 10px 20px;
  border-radius: 8px;
  background: var(--color-primary, #4a5568);
  color: #fff;
  text-decoration: none;
}
</style>
