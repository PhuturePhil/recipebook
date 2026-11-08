<template>
  <!-- Rezept aus dem Essensplan über die Eintrags-ID (eigene Route, damit Liste, Suche und /rezepte/:id es nie zeigen).
       Eigene Komponente, damit der Wechsel zur regulären Rezeptseite die Seite neu aufbaut. -->
  <RecipeDetail :key="route.params.entryId" :plan-entry-id="route.params.entryId" />
</template>

<script setup>
import { useRoute } from 'vue-router'
import RecipeDetail from './RecipeDetail.vue'

const route = useRoute()
</script>
