<template>
  <div class="changelog">
    <h1>Updates</h1>
    <div class="changelog-list">
      <div v-for="(entry, index) in changelog" :key="index" class="changelog-entry">
        <div class="changelog-header">
          <h2 class="changelog-title">{{ entry.title }}</h2>
          <span class="changelog-date">{{ entry.date }}</span>
        </div>
        <ul class="changelog-changes">
          <li v-for="(change, i) in entry.changes" :key="i">{{ change }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { changelog } from '@/data/changelog'
</script>

<style scoped>
.changelog {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.changelog h1 {
  font-size: 2rem;
  color: var(--color-text-primary, #333);
  margin-bottom: 32px;
}

.changelog-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.changelog-entry {
  background: var(--color-bg-card, #fff);
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.changelog-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--color-border, #ddd);
}

.changelog-title {
  font-size: 1.25rem;
  color: var(--color-text-primary, #333);
  margin: 0;
}

.changelog-date {
  font-size: 0.875rem;
  color: var(--color-text-secondary, #666);
  white-space: nowrap;
}

.changelog-changes {
  margin: 0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.changelog-changes li {
  color: var(--color-text-primary, #333);
  line-height: 1.5;
}

.changelog-changes li::marker {
  color: var(--color-primary, #4a5568);
}
</style>
