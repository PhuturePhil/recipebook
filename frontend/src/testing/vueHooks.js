// Node-Loader nur für Tests: löst den Vite-Alias '@/' auf und kompiliert .vue-Dateien (ohne Styles),
// damit node --test Komponenten per vue/server-renderer rendern kann. Einbinden: registerVueHooks()
import { readFile } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { createHash } from 'node:crypto'

const SRC = new URL('../', import.meta.url)

export async function resolve(specifier, context, next) {
  if (specifier.startsWith('@/')) {
    const url = new URL(specifier.slice(2), SRC)
    const withExt = /\.(js|vue|json)$/.test(url.pathname) ? url : new URL(`${url.href}.js`)
    if (existsSync(fileURLToPath(withExt))) return { url: withExt.href, shortCircuit: true }
  }
  return next(specifier, context)
}

export async function load(url, context, next) {
  if (!url.endsWith('.vue')) return next(url, context)
  const { parse, compileScript } = await import('vue/compiler-sfc')
  const filename = fileURLToPath(url)
  const { descriptor } = parse(await readFile(filename, 'utf8'), { filename })
  const id = createHash('sha1').update(filename).digest('hex').slice(0, 8)
  const script = compileScript(descriptor, { id, inlineTemplate: true })
  return { format: 'module', source: script.content, shortCircuit: true }
}
