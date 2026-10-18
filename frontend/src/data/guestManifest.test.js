import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { MANIFEST_INTRO, MANIFEST_POINTS } from './guestManifest.js'

const point = (p) => `${p.title} ${p.text}`

test('Intro steht wörtlich wie abgenommen', () => {
  assert.equal(MANIFEST_INTRO, 'Kein Feed, keine Follower, keine Likes von Fremden. Hier geht es um das, was wirklich zählt: die Rezepte, mit denen wir aufgewachsen sind — und die Menschen, mit denen wir sie teilen. Omas handgeschriebene Zettel, Lieblingsgerichte aus Büchern und von Websites, digitalisiert und bewahrt. Privat, für Familie und Freunde.')
})

test('vier Vorteile wörtlich und in fester Reihenfolge', () => {
  assert.deepEqual(MANIFEST_POINTS.map(point), [
    'Alles an einem Ort: digital, strukturiert und durchsuchbar — statt Zettelwirtschaft und Screenshots.',
    'Abfotografieren statt abtippen: Aus dem Foto wird ein sauberes Rezept mit Zutaten und Nährwerten.',
    'Für Familie und Freunde statt für die ganze Welt: Eure Rezepte sehen nur die, die ihr einladet.',
    'Werbefrei und datensparsam: keine Werbung, kein Tracking — eure Rezepte und Daten gehören euch.',
  ])
})

test('jeder Vorteil hat ein Icon und einen Titel mit Doppelpunkt', () => {
  for (const p of MANIFEST_POINTS) {
    assert.ok(p.icon, p.title)
    assert.ok(p.title.endsWith(':'), p.title)
  }
})

test('Gast-Startseite spricht nicht von „veröffentlichen“ oder „öffentlich stellen“', () => {
  const vue = readFileSync(new URL('../components/GuestHome.vue', import.meta.url), 'utf8')
  const texts = [MANIFEST_INTRO, ...MANIFEST_POINTS.map(point), vue.split('<script')[0]]
  for (const t of texts) assert.doesNotMatch(t, /veröffentlich|öffentlich stell/i)
})
