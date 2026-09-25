<template>
  <div class="info-view">
    <h1>Nährwerte &amp; Badges</h1>

    <div v-if="loading" class="state">Lädt…</div>
    <div v-else-if="error" class="state state--error">{{ error }}</div>

    <template v-else-if="info">
      <section>
        <h2>Badges</h2>
        <p>
          Die Grenzen stammen aus der {{ regulation }}. Sie gelten dort für Lebensmittel-Kennzeichnungen;
          wir wenden sie auf das Gericht an.
        </p>
        <div class="table-scroll">
          <table class="info-table">
            <thead>
              <tr>
                <th>Badge</th>
                <th class="col-claim">Angabe laut Verordnung</th>
                <th>Grenze</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rule in info.badges" :key="rule.badge">
                <td><span :class="['badge', `badge--${badgeKey(rule.badge)}`]">{{ rule.badge }}</span></td>
                <td class="col-claim">{{ rule.claim }}</td>
                <td>{{ rule.threshold }}</td>
              </tr>
              <tr>
                <td><span class="badge badge--schnell">Schnell</span></td>
                <td class="col-claim">keine (App-eigenes Badge)</td>
                <td>Zubereitungszeit höchstens {{ QUICK_MAX_MINUTES }} Minuten</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="note">{{ info.badgeBasis }}</p>
      </section>

      <section>
        <h2>So wird gerechnet</h2>
        <ul>
          <li>Jede Zutat wird in Gramm umgerechnet (z. B. 1 EL Öl = 10 g, 1 Zwiebel ≈ 90 g) und mit den Nährwerten pro 100 g multipliziert.</li>
          <li>Die Werte werden bei jedem Aufruf neu aus den aktuellen Zutaten berechnet. Korrekturen im Zutatenkatalog wirken sofort in allen Rezepten.</li>
          <li>Unter jedem Rezept steht, aus wie vielen Zutaten gerechnet wurde. Liegt die Abdeckung unter {{ info.completeThresholdPercent }} %, gilt das Ergebnis als unvollständig und wird ausgegraut.</li>
          <li>{{ info.negligibleRule }}</li>
          <li>Mengenspannen wie „200–250 g“ werden mit dem Mittelwert gerechnet; fehlt die Einheit, wird „Stück“ angenommen.</li>
        </ul>
      </section>

      <section>
        <h2>Woher die Werte kommen</h2>
        <dl class="sources">
          <dt><span class="source-chip source-chip--bls">BLS</span></dt>
          <dd>Wert aus dem Bundeslebensmittelschlüssel (Referenzdatenbank, pro 100 g).</dd>
          <dt><span class="source-chip source-chip--manual">manuell</span></dt>
          <dd>Von einem Admin eingetragener oder korrigierter Wert.</dd>
          <dt><span class="source-chip source-chip--ai_estimate">KI-Schätzung</span></dt>
          <dd>Für Zutaten ohne passenden BLS-Eintrag: einmalig per KI geschätzt oder aus dem früheren Katalog übernommen. Diese Werte sind am unsichersten.</dd>
        </dl>
      </section>

      <section v-for="dataset in info.datasets" :key="dataset.sourceKey">
        <h2>Datenquelle</h2>
        <p>
          <strong>{{ dataset.name }}</strong>, Version {{ dataset.version }}, herausgegeben vom {{ dataset.publisher }}
          ({{ dataset.rowCount?.toLocaleString('de-DE') }} Lebensmittel).
        </p>
        <p class="citation">{{ dataset.citation }}</p>
        <p>
          Lizenz: <a :href="dataset.licenseUrl" target="_blank" rel="noopener">{{ dataset.license }}</a> ·
          <a :href="dataset.sourceUrl" target="_blank" rel="noopener">{{ dataset.sourceUrl }}</a>
          <template v-if="dataset.doi">
            · DOI <a :href="`https://doi.org/${dataset.doi}`" target="_blank" rel="noopener">{{ dataset.doi }}</a>
          </template>
        </p>
        <p class="note">Die Daten wurden für die App auf Makronährstoffe und ausgewählte Mikronährstoffe gekürzt; die Werte selbst sind unverändert.</p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { nutritionService } from '@/services/nutritionService'
import { QUICK_MAX_MINUTES } from '@/stores/recipeStore'

const info = ref(null)
const loading = ref(true)
const error = ref(null)

const regulation = computed(() => info.value?.badges?.[0]?.regulation ?? 'Verordnung (EG) Nr. 1924/2006')

const badgeKey = (badge) => badge.toLowerCase().replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue').replace(/ß/g, 'ss')

onMounted(async () => {
  try {
    info.value = await nutritionService.getInfo()
  } catch (err) {
    error.value = 'Informationen konnten nicht geladen werden.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.info-view {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
  text-align: left;
  color: var(--color-text-primary, #333);
}

.info-view h1 {
  font-size: 2rem;
  margin: 0 0 16px;
}

.info-view h2 {
  font-size: 1.25rem;
  margin: 24px 0 8px;
  padding-bottom: 6px;
  border-bottom: 2px solid var(--color-border, #ddd);
}

.state {
  padding: 24px;
  color: var(--color-text-secondary, #666);
}

.state--error {
  color: var(--color-error, #e53e3e);
}

.table-scroll {
  overflow-x: auto;
}

.info-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.info-table th,
.info-table td {
  text-align: left;
  padding: 8px 10px;
  border-bottom: 1px solid var(--color-border-light, #eee);
  vertical-align: top;
}

.info-table th {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary, #666);
}

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.badge--energiearm { background: #f0fff4; color: #276749; border: 1px solid #9ae6b4; }
.badge--fettarm { background: #fefcbf; color: #744210; }
.badge--proteinreich { background: #bee3f8; color: #2a4365; }
.badge--ballaststoffreich { background: #e9d8fd; color: #44337a; }
.badge--schnell { background: #c6f6d5; color: #22543d; }

.note {
  font-size: 0.85rem;
  color: var(--color-text-secondary, #666);
}

.citation {
  font-size: 0.85rem;
  font-style: italic;
}

.sources {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 8px 12px;
}

.sources dd {
  margin: 0;
}

.source-chip {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: 600;
}

.source-chip--bls { background: #e6fffa; color: #234e52; }
.source-chip--manual { background: #ebf8ff; color: #2a4365; }
.source-chip--ai_estimate { background: #fffaf0; color: #7b341e; border: 1px dashed #ed8936; }

@media (max-width: 600px) {
  .info-view {
    padding: 16px;
  }

  .sources {
    grid-template-columns: 1fr;
  }

  .col-claim {
    display: none;
  }
}
</style>
