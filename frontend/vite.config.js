import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { readFileSync } from 'node:fs'
import { SHARE_SHELL_FILE, shareShellHtml } from './src/utils/shareShell.js'

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

// Teilen-Links bekommen eine Kopie der gebauten index.html, in die nginx per SSI die
// Open-Graph-Tags des Rezepts einsetzt (Link-Vorschau in Messengern), siehe utils/shareShell.js.
const shareShell = () => ({
  name: 'share-shell',
  enforce: 'post',
  generateBundle(_, bundle) {
    const index = bundle['index.html']
    if (!index) return
    this.emitFile({ type: 'asset', fileName: SHARE_SHELL_FILE, source: shareShellHtml(String(index.source)) })
  }
})

export default defineConfig({
  plugins: [vue(), changelogJson(), shareShell()],
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
