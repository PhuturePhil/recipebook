import sharp from 'sharp'
import { readFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const svgPath = join(__dirname, '../public/icon.svg')
const svg = readFileSync(svgPath)

const sizes = [
  { size: 180, name: 'icon-180.png' },
  { size: 192, name: 'icon-192.png' },
  { size: 512, name: 'icon-512.png' },
]

for (const { size, name } of sizes) {
  const outPath = join(__dirname, '../public', name)
  await sharp(svg)
    .resize(size, size)
    .png()
    .toFile(outPath)
  console.log(`Generated ${name} (${size}x${size})`)
}
