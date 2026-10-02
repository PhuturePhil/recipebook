import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { readFileSync } from 'node:fs'

// changelog.json ist die einzige Versionsquelle: ins Bundle importiert (eingebaute Version) UND
// als /changelog.json ausgeliefert (Server-Version für den Update-Check). Vite kann nicht aus
// public/ importieren, deshalb liegt die Datei in src/data/ und wird hier nach dist/ kopiert.
const changelogFile = fileURLToPath(new URL('./src/data/changelog.json', import.meta.url))
const changelogJson = () => ({
  name: 'changelog-json',
  configureServer(server) {
    server.middlewares.use('/changelog.json', (req, res) => {
      res.setHeader('Content-Type', 'application/json')
      res.setHeader('Cache-Control', 'no-store')
      res.end(readFileSync(changelogFile))
    })
  },
  generateBundle() {
    this.emitFile({ type: 'asset', fileName: 'changelog.json', source: readFileSync(changelogFile, 'utf8') })
  }
})

export default defineConfig({
  plugins: [vue(), changelogJson()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
