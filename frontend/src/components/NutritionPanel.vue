<template>
  <div class="nutrition-panel">
    <div v-if="!nutrition.hasValues" class="nutrition-empty">
      Für dieses Rezept lassen sich noch keine Nährwerte berechnen.
      <span v-if="nutrition.missingIngredients.length">
        Es fehlen Angaben zu: {{ nutrition.missingIngredients.join(', ') }}.
      </span>
    </div>

    <template v-else>
      <div :class="['coverage', { 'coverage--incomplete': !nutrition.coverage.complete }]">
        <strong v-if="!nutrition.coverage.complete">Unvollständig: </strong>
        berechnet aus {{ nutrition.coverage.calculated }} von {{ nutrition.coverage.relevant }} Zutaten
        <span v-if="nutrition.missingIngredients.length" class="coverage__missing">
          – nicht berechnet: {{ nutrition.missingIngredients.join(', ') }}
        </span>
        <span v-if="!nutrition.coverage.complete" class="coverage__hint">
          Die Werte liegen deshalb vermutlich zu niedrig.
        </span>
      </div>

      <div v-if="nutrition.badges.length" class="nutrition-badges">
        <router-link
          v-for="badge in nutrition.badges"
          :key="badge"
          to="/naehrwerte"
          :class="['badge', `badge--${badgeKey(badge)}`]"
          title="Was bedeuten die Badges?"
        >
          {{ badge }}
        </router-link>
      </div>

      <div class="table-scroll">
        <table :class="['nutrition-table', { 'nutrition-table--muted': !nutrition.coverage.complete }]">
          <thead>
            <tr>
              <th></th>
              <th v-if="per100g">pro 100&nbsp;g</th>
              <th>pro Portion</th>
              <th class="dv-col" title="Anteil am Tagesbedarf pro Portion">% Tages&shy;bedarf*</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Energie</td>
              <td v-if="per100g">
                {{ formatKcal(per100g.kcal) }} kcal
                <span class="kj">{{ formatKcal(per100g.kj) }} kJ</span>
              </td>
              <td>
                {{ formatKcal(perServing.kcal) }} kcal
                <span class="kj">{{ formatKcal(perServing.kj) }} kJ</span>
              </td>
              <td :class="dvClass(macroDailyPercent('kcal', perServing.kcal))">
                {{ formatPercent(macroDailyPercent('kcal', perServing.kcal)) }}
              </td>
            </tr>
            <tr v-for="row in macroRows" :key="row.key" :class="{ 'sub-row': row.sub }">
              <td>{{ row.label }}</td>
              <td v-if="per100g">{{ formatRow(row, per100g[row.key]) }} g</td>
              <td>{{ formatRow(row, perServing[row.key]) }} g</td>
              <td :class="dvClass(macroDailyPercent(row.key, perServing[row.key]))">
                {{ formatPercent(macroDailyPercent(row.key, perServing[row.key])) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p class="dv-note">
        * Anteil der EU-Referenzmenge für einen durchschnittlichen Erwachsenen (8.400 kJ / 2.000 kcal), pro Portion.
        Ballaststoffe: DGE-Richtwert 30 g.
      </p>

      <p v-if="per100g" :class="['weight-note', { 'weight-note--incomplete': !nutrition.totalGramsComplete }]">
        <template v-if="nutrition.totalGramsComplete">
          Pro 100 g bezogen auf {{ formatGram(nutrition.totalGrams) }} g Gesamtgewicht der rohen Zutaten.
        </template>
        <template v-else>
          Pro 100 g nur näherungsweise: bezogen auf {{ formatGram(nutrition.totalGrams) }} g der berechneten Zutaten,
          das Gewicht von {{ nutrition.missingIngredients.join(', ') }} ist nicht bekannt.
        </template>
      </p>

      <p class="sources">
        Quelle der Werte:
        <span v-for="s in sourceShares" :key="s.source" :class="['source-chip', `source-chip--${s.source.toLowerCase()}`]">
          {{ s.label }} {{ s.share }} %
        </span>
      </p>

      <details class="nutrition-details">
        <summary>Aufschlüsselung nach Zutaten</summary>
        <ul class="breakdown">
          <li
            v-for="(item, index) in nutrition.items"
            :key="index"
            :class="['breakdown__item', { 'breakdown__item--missing': item.relevant && !item.calculated }]"
          >
            <div class="breakdown__main">
              <span class="breakdown__name">{{ item.originalName }}</span>
              <span v-if="item.calculated" class="breakdown__kcal">{{ formatKcal(item.nutrients.kcal) }} kcal</span>
              <span v-else class="breakdown__status">{{ STATUS_LABELS[item.status] }}</span>
            </div>
            <div class="breakdown__meta">
              <span v-if="item.amount != null">
                {{ formatAmount(item.amount) }} {{ item.unit }}<span v-if="item.unitAssumed"> (angenommen)</span>
              </span>
              <span v-if="item.grams != null"> = {{ formatGram(item.grams) }} g</span>
              <span v-if="item.gramsBasis" class="breakdown__basis"> ({{ item.gramsBasis }})</span>
              <span v-if="item.ingredientName" class="breakdown__match">
                → {{ item.ingredientName }}
                <span v-if="item.source" :class="['source-chip', `source-chip--${item.source.toLowerCase()}`]">
                  {{ SOURCE_LABELS[item.source] }}
                </span>
                <span v-if="item.referenceName" class="breakdown__ref">{{ item.referenceName }}</span>
              </span>
            </div>
            <div v-if="item.reason && !item.calculated" class="breakdown__reason">{{ item.reason }}</div>
          </li>
        </ul>
      </details>

      <details v-if="microRows.length" class="nutrition-details">
        <summary>Mikronährstoffe anzeigen</summary>
        <p class="micro-note">
          Pro Portion, nur aus BLS-Werten ({{ nutrition.microDataCount }} von {{ nutrition.coverage.calculated }}
          berechneten Zutaten). Zutaten ohne BLS-Bezug fehlen hier.
        </p>
        <div class="table-scroll">
          <table class="nutrition-table micro-table">
            <tbody>
              <tr v-for="row in microRows" :key="row.code">
                <td>{{ row.label }}</td>
                <td>{{ formatMicro(row.value) }} {{ row.unit }}</td>
                <td :class="dvClass(row.percent)" title="% Tagesbedarf pro Portion">{{ formatPercent(row.percent) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </details>
    </template>

    <p class="attribution">
      <template v-if="dataset">
        Nährwertdaten: <a :href="dataset.sourceUrl" target="_blank" rel="noopener">{{ dataset.name }}, Version {{ dataset.version }}</a>,
        {{ dataset.publisher }},
        Lizenz <a :href="dataset.licenseUrl" target="_blank" rel="noopener">{{ dataset.license }}</a>.
      </template>
      Werte sind Schätzungen aus rohen Zutaten.
      <router-link to="/naehrwerte">Wie wird gerechnet?</router-link>
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  SOURCE_LABELS, STATUS_LABELS, formatKcal, formatGram, formatMicro,
} from '@/services/nutritionService'
import {
  macroDailyPercent, microDailyPercent, formatPercent, percentLevel,
} from '@/utils/dailyValue'

const props = defineProps({
  nutrition: {
    type: Object,
    required: true,
  },
  info: {
    type: Object,
    default: null,
  },
})

const MACROS = [
  { key: 'fat', label: 'Fett' },
  { key: 'carbs', label: 'Kohlenhydrate' },
  { key: 'sugar', label: 'davon Zucker', sub: true },
  { key: 'fiber', label: 'Ballaststoffe' },
  { key: 'protein', label: 'Eiweiß' },
  { key: 'salt', label: 'Salz', precise: true },
]

const macroRows = MACROS

const perServing = computed(() => props.nutrition.perServing ?? {})
const per100g = computed(() => props.nutrition.per100g)

const sourceShares = computed(() =>
  Object.entries(props.nutrition.kcalShareBySource ?? {})
    .sort(([, a], [, b]) => b - a)
    .map(([source, share]) => ({ source, share, label: SOURCE_LABELS[source] ?? source }))
)

const dataset = computed(() => props.info?.datasets?.find(d => d.sourceKey === 'BLS') ?? null)

const microRows = computed(() => {
  const micros = props.nutrition.perServing?.micronutrients ?? {}
  const meta = props.info?.micronutrients ?? []
  return meta
    .filter(m => micros[m.code] != null)
    .map(m => ({ ...m, value: micros[m.code], percent: microDailyPercent(m.code, micros[m.code], m.unit) }))
})

const dvClass = (percent) => ['dv', `dv--${percentLevel(percent)}`]

const badgeKey = (badge) => badge.toLowerCase().replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue').replace(/ß/g, 'ss')

const formatRow = (row, value) =>
  row.precise && value != null && Math.abs(value) < 1
    ? value.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : formatGram(value)

const formatAmount = (value) => value.toLocaleString('de-DE', { maximumFractionDigits: 2 })
</script>

<style scoped>
.nutrition-panel {
  font-size: 0.95rem;
}

.nutrition-empty {
  padding: 16px;
  background: var(--color-bg-secondary, #f0f0f0);
  border-radius: 8px;
  color: var(--color-text-secondary, #666);
}

.coverage {
  margin-bottom: 12px;
  color: var(--color-text-secondary, #666);
  font-size: 0.875rem;
}

.coverage--incomplete {
  padding: 10px 12px;
  background: var(--warn-bg);
  border: 1px solid var(--warn-linie);
  border-radius: 6px;
  color: var(--warn-text);
}

.coverage__missing {
  display: inline;
}

.coverage__hint {
  display: block;
  margin-top: 4px;
}

.nutrition-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  text-decoration: none;
}

.badge--energiearm { background: var(--pos-weich); color: var(--pos); border: 1px solid var(--pos-linie); }
.badge--fettarm { background: var(--warn-bg); color: var(--warn-text); }
.badge--proteinreich { background: var(--info-weich); color: var(--info-text); }
.badge--ballaststoffreich { background: var(--lila-weich); color: var(--lila-text); }

.table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.nutrition-table {
  width: 100%;
  border-collapse: collapse;
}

.nutrition-table th,
.nutrition-table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid var(--color-border-light, #eee);
  white-space: nowrap;
}

.nutrition-table th {
  font-weight: 600;
  color: var(--color-text-secondary, #666);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.nutrition-table td:first-child {
  color: var(--color-text-primary, #333);
  font-weight: 500;
}

.nutrition-table td:not(:first-child) {
  color: var(--color-primary, #4a5568);
  font-weight: 600;
}

.nutrition-table--muted td:not(:first-child) {
  color: var(--color-text-muted, #a0aec0);
}

.nutrition-table .dv-col,
.nutrition-table .dv {
  text-align: right;
}

.nutrition-table .dv-col {
  white-space: normal;
}

.nutrition-table td.dv--high { color: var(--pos); }
.nutrition-table td.dv--medium { color: var(--info-text); }
.nutrition-table td.dv--low,
.nutrition-table td.dv--none {
  color: var(--color-text-muted, #a0aec0);
}

.nutrition-table td.dv--none {
  font-weight: 400;
}

.nutrition-table--muted td.dv {
  color: var(--color-text-muted, #a0aec0);
}

.dv-note {
  margin: 8px 0 0;
  font-size: 0.75rem;
  color: var(--color-text-muted, #999);
}

.sub-row td:first-child {
  padding-left: 22px;
  font-weight: 400;
  color: var(--color-text-secondary, #666);
}

.kj {
  display: block;
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--color-text-muted, #999);
}

.weight-note {
  margin: 8px 0 0;
  font-size: 0.8rem;
  color: var(--color-text-secondary, #666);
}

.weight-note--incomplete {
  color: var(--warn-text);
}

.sources {
  margin: 12px 0;
  font-size: 0.8rem;
  color: var(--color-text-secondary, #666);
}

.source-chip {
  display: inline-block;
  margin-left: 4px;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 600;
  vertical-align: middle;
}

.source-chip--bls { background: var(--pos-weich); color: var(--info-text); }
.source-chip--manual { background: var(--info-weich); color: var(--info-text); }
.source-chip--ai_estimate { background: var(--warn-bg); color: var(--warn-text); border: 1px dashed var(--warn-linie); }

.nutrition-details {
  margin-top: 12px;
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 8px;
  padding: 8px 12px;
}

.nutrition-details summary {
  cursor: pointer;
  font-weight: 600;
  color: var(--color-primary, #4a5568);
  padding: 4px 0;
}

.breakdown {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
}

.breakdown__item {
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light, #eee);
}

.breakdown__item:last-child {
  border-bottom: none;
}

.breakdown__item--missing .breakdown__name {
  color: var(--warn-text);
}

.breakdown__main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.breakdown__name {
  font-weight: 500;
  color: var(--color-text-primary, #333);
}

.breakdown__kcal {
  font-weight: 600;
  color: var(--color-primary, #4a5568);
  white-space: nowrap;
}

.breakdown__status {
  font-size: 0.8rem;
  color: var(--warn-text);
  white-space: nowrap;
}

.breakdown__meta,
.breakdown__reason {
  font-size: 0.8rem;
  color: var(--color-text-secondary, #666);
}

.breakdown__basis,
.breakdown__ref {
  color: var(--color-text-muted, #999);
}

.breakdown__ref {
  margin-left: 4px;
}

.breakdown__reason {
  margin-top: 2px;
  color: var(--warn-text);
}

.micro-note {
  font-size: 0.8rem;
  color: var(--color-text-secondary, #666);
}

.micro-table td:first-child {
  white-space: normal;
}

.attribution {
  margin-top: 16px;
  font-size: 0.75rem;
  color: var(--color-text-muted, #999);
}

.attribution a {
  color: inherit;
  text-decoration: underline;
}

@media (max-width: 600px) {
  .nutrition-table th,
  .nutrition-table td {
    padding: 6px 4px;
    font-size: 0.85rem;
  }

  .nutrition-table th {
    font-size: 0.7rem;
    letter-spacing: 0;
    white-space: normal;
  }

  .breakdown__main {
    flex-direction: column;
    gap: 2px;
  }
}

@media print {
  .nutrition-details,
  .attribution {
    display: none;
  }
}
</style>
