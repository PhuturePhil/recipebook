import { onMounted, onUnmounted } from 'vue'
import { startVisit, PAGE_VISIBLE_CAP_MS } from '@/utils/analytics'

// Nutzungsstatistik: ein Event „seite_geoeffnet“ beim Verlassen, mit sichtbarer Verweildauer (gekappt bei 30 Min.)
export function usePageVisit(seite) {
  let visit = null
  onMounted(() => {
    visit = startVisit('seite_geoeffnet', { seite }, { capVisibleMs: PAGE_VISIBLE_CAP_MS, fields: ['sichtbar_ms'] })
  })
  onUnmounted(() => visit?.end())
}
