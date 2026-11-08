import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  VIEWS, isoDate, fromIso, addDays, weekStart, weekDays, isoWeek, monthDays, monthGrid, rangeFor, shiftAnchor,
  isCurrentPeriod, weekLabel, weekSubLabel, monthLabel, dayLabel, weekdayShort, groupByDate, entryTitle, entryMeta,
  plannableIn, pickerRecipes, thumbFor, insertEntry, dayPhoto, bookedDaysLoader, ACCESS, isGhost, entryPath,
  defaultFamilyId, asksToShare, goneSource,
} from './mealPlan.js'

const d = (iso) => fromIso(iso)

test('isoDate/fromIso: lokale Zeit, ungültige Daten → null', () => {
  assert.equal(isoDate(new Date(2026, 9, 26, 23, 59)), '2026-10-26')
  assert.equal(isoDate(d('2026-03-29')), '2026-03-29')
  assert.equal(fromIso('2026-02-30'), null)
  assert.equal(fromIso('26.10.2026'), null)
  assert.equal(fromIso(undefined), null)
})

test('weekStart: Woche beginnt am Montag, Sonntag gehört zur Woche davor', () => {
  assert.equal(isoDate(weekStart(d('2026-10-26'))), '2026-10-26')
  assert.equal(isoDate(weekStart(d('2026-11-01'))), '2026-10-26')
  assert.equal(isoDate(weekStart(d('2026-10-31'))), '2026-10-26')
})

test('weekDays: Wochenband über den Monatswechsel (26. Okt. – 1. Nov.)', () => {
  const days = weekDays(d('2026-10-28')).map(isoDate)
  assert.deepEqual(days, ['2026-10-26', '2026-10-27', '2026-10-28', '2026-10-29', '2026-10-30', '2026-10-31', '2026-11-01'])
})

test('weekDays: Wochenband über den Jahreswechsel und die Zeitumstellung', () => {
  assert.deepEqual(weekDays(d('2027-01-01')).map(isoDate),
    ['2026-12-28', '2026-12-29', '2026-12-30', '2026-12-31', '2027-01-01', '2027-01-02', '2027-01-03'])
  // 25.10.2026: Umstellung auf Winterzeit – trotzdem 7 verschiedene Tage
  assert.equal(new Set(weekDays(d('2026-10-25')).map(isoDate)).size, 7)
  assert.equal(isoDate(addDays(d('2026-10-24'), 1)), '2026-10-25')
  assert.equal(isoDate(addDays(d('2026-10-25'), 1)), '2026-10-26')
})

test('isoWeek: KW nach ISO 8601', () => {
  assert.equal(isoWeek(d('2026-10-26')), 44)
  assert.equal(isoWeek(d('2026-11-01')), 44)
  assert.equal(isoWeek(d('2026-12-31')), 53)
  assert.equal(isoWeek(d('2027-01-03')), 53)
  assert.equal(isoWeek(d('2027-01-04')), 1)
  assert.equal(isoWeek(d('2025-12-29')), 1)
})

test('monthDays: Länge der Monate inkl. Schaltjahr', () => {
  assert.equal(monthDays(d('2026-10-15')).length, 31)
  assert.equal(monthDays(d('2026-11-30')).length, 30)
  assert.equal(monthDays(d('2026-02-01')).length, 28)
  assert.equal(monthDays(d('2028-02-10')).length, 29)
})

test('monthGrid: Oktober 2026 wie im Entwurf – 3 leere Zellen, danach Folgetag 1. Nov. gedimmt', () => {
  const cells = monthGrid(d('2026-10-26'))
  assert.equal(cells.length % 7, 0)
  assert.equal(cells.length, 35)
  assert.deepEqual(cells.slice(0, 3).map((c) => c.date), [null, null, null])
  assert.equal(isoDate(cells[3].date), '2026-10-01')
  assert.equal(cells[3].inMonth, true)
  assert.equal(isoDate(cells.at(-1).date), '2026-11-01')
  assert.equal(cells.at(-1).inMonth, false)
  assert.equal(cells.filter((c) => c.inMonth).length, 31)
})

test('monthGrid: Monat beginnt am Montag → keine leeren Zellen; endet am Sonntag → keine Folgetage', () => {
  const june = monthGrid(d('2026-06-01'))
  assert.equal(isoDate(june[0].date), '2026-06-01')
  const may = monthGrid(d('2026-05-01'))
  assert.equal(isoDate(may.at(-1).date), '2026-05-31')
  assert.equal(may.at(-1).inMonth, true)
})

test('rangeFor: Woche Mo–So (über den Monatswechsel), Monat und Liste der ganze Monat', () => {
  assert.deepEqual(rangeFor('woche', d('2026-10-30')), { from: '2026-10-26', to: '2026-11-01' })
  assert.deepEqual(rangeFor('monat', d('2026-10-30')), { from: '2026-10-01', to: '2026-10-31' })
  assert.deepEqual(rangeFor('liste', d('2026-02-14')), { from: '2026-02-01', to: '2026-02-28' })
  assert.deepEqual(VIEWS, ['woche', 'monat', 'liste'])
})

test('shiftAnchor: Woche um 7 Tage, Monat auf den Ersten (auch vom 31.)', () => {
  assert.equal(isoDate(shiftAnchor('woche', d('2026-10-28'), 1)), '2026-11-02')
  assert.equal(isoDate(shiftAnchor('woche', d('2026-10-28'), -1)), '2026-10-19')
  assert.equal(isoDate(shiftAnchor('monat', d('2026-10-31'), 1)), '2026-11-01')
  assert.equal(isoDate(shiftAnchor('liste', d('2027-01-31'), -1)), '2026-12-01')
})

test('isCurrentPeriod: Heute-Knopf nur außerhalb des aktuellen Zeitraums', () => {
  const today = d('2026-10-26')
  assert.equal(isCurrentPeriod('woche', d('2026-11-01'), today), true)
  assert.equal(isCurrentPeriod('woche', d('2026-11-02'), today), false)
  assert.equal(isCurrentPeriod('monat', d('2026-10-01'), today), true)
  assert.equal(isCurrentPeriod('liste', d('2026-11-01'), today), false)
})

test('Beschriftungen wie im Entwurf (deutsch)', () => {
  assert.equal(weekLabel(d('2026-10-28'), 'de'), '26. Okt. – 1. Nov.')
  assert.equal(weekLabel(d('2026-11-04'), 'de'), '2.–8. Nov.')
  assert.equal(weekSubLabel(d('2026-10-28'), d('2026-10-26'), 'de'), 'Diese Woche · KW 44')
  assert.equal(weekSubLabel(d('2026-11-02'), d('2026-10-26'), 'de'), 'Nächste Woche · KW 45')
  assert.equal(weekSubLabel(d('2026-10-19'), d('2026-10-26'), 'de'), 'Letzte Woche · KW 43')
  assert.equal(weekSubLabel(d('2026-11-16'), d('2026-10-26'), 'de'), 'KW 47')
  assert.equal(monthLabel(d('2026-10-01'), 'de'), 'Oktober 2026')
  assert.equal(dayLabel(d('2026-10-26'), 'de'), 'Montag, 26. Oktober')
  assert.deepEqual(weekDays(d('2026-10-26')).map((x) => weekdayShort(x, 'de')), ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So'])
})

test('Beschriftungen englisch', () => {
  assert.equal(weekLabel(d('2026-10-28'), 'en'), '26 Oct – 1 Nov')
  assert.equal(weekLabel(d('2026-11-04'), 'en'), '2–8 Nov')
  assert.equal(weekSubLabel(d('2026-10-28'), d('2026-10-26'), 'en'), 'This week · Week 44')
  assert.equal(monthLabel(d('2026-10-01'), 'en'), 'October 2026')
  assert.equal(weekdayShort(d('2026-10-26'), 'en'), 'Mo')
})

const entry = (id, date, recipe = null, freeText = null) => ({ id, date, familyId: 1, recipe, freeText })
const pizza = { id: 211, title: 'Pizza Beans', translatedTitle: null, language: 'de', prepTimeMinutes: 50, author: '', source: 'Smitten Kitchen', thumbUrl: '/api/recipes/211/image?v=a&size=thumb' }
const dal = { id: 192, title: 'Traybake lemon dal', translatedTitle: 'Blech-Dal mit Zitrone', language: 'en', prepTimeMinutes: 95, author: 'Meera Sodha', source: 'Guardian', thumbUrl: null }

test('groupByDate: mehrere Einträge pro Tag in Reihenfolge', () => {
  const map = groupByDate([entry(1, '2026-10-26', pizza), entry(2, '2026-10-26', null, 'Essen bei Oma'), entry(3, '2026-10-27', dal)])
  assert.deepEqual(map.get('2026-10-26').map((e) => e.id), [1, 2])
  assert.equal(map.get('2026-10-28'), undefined)
})

test('entryTitle: Freitext, Original oder deutsche Übersetzung je Gerätewahl', () => {
  assert.equal(entryTitle(entry(1, '2026-10-26', null, 'Essen bei Oma')), 'Essen bei Oma')
  assert.equal(entryTitle(entry(2, '2026-10-26', pizza)), 'Pizza Beans')
  assert.equal(entryTitle(entry(3, '2026-10-26', dal), 'original'), 'Traybake lemon dal')
  assert.equal(entryTitle(entry(3, '2026-10-26', dal), 'de'), 'Blech-Dal mit Zitrone')
})

test('entryMeta: „Zeit · Autor“, ohne Autor die Quelle (Webadresse als Domain), Freitext ohne Meta', () => {
  assert.equal(entryMeta(entry(1, '2026-10-26', pizza)), '50 Min. · Smitten Kitchen')
  assert.equal(entryMeta(entry(2, '2026-10-26', dal)), '1 Std. 35 Min. · Meera Sodha')
  assert.equal(entryMeta(entry(3, '2026-10-26', { ...pizza, prepTimeMinutes: null, source: 'https://www.bonappetit.com/recipe/x' })), 'bonappetit.com')
  assert.equal(entryMeta(entry(4, '2026-10-26', null, 'Pizza')), '')
})

const fam = (id, name) => ({ id, name })

test('defaultFamilyId: vom Server markierte Standard-Familie, sonst die erste, ohne Familie null', () => {
  assert.equal(defaultFamilyId([fam(1, 'Starter :)'), { ...fam(2, 'Hinze'), defaultFamily: true }]), 2)
  assert.equal(defaultFamilyId([fam(1, 'Starter :)'), fam(2, 'Hinze')]), 1)
  assert.equal(defaultFamilyId([]), null)
  assert.equal(defaultFamilyId(undefined), null)
})

test('asksToShare: nur der Besitzer wird gefragt, und nur wenn die Familie das Rezept nicht sieht', () => {
  const priv = { visibility: 'PRIVATE', families: [] }
  const shared = { visibility: 'FAMILY', families: [fam(1, 'Starter :)')] }
  assert.equal(asksToShare(priv, 1, true), true)
  assert.equal(asksToShare(priv, 1, false), false, 'alle anderen planen still nur über den Plan')
  assert.equal(asksToShare(shared, 1, true), false)
  assert.equal(asksToShare(shared, 2, true), true)
  assert.equal(asksToShare({ visibility: 'PUBLIC' }, 2, true), false)
  assert.equal(asksToShare(priv, null, true), false)
})

test('plannableIn: öffentlich oder für die Familie freigegeben', () => {
  assert.equal(plannableIn({ visibility: 'PUBLIC' }, 3), true)
  assert.equal(plannableIn({ visibility: 'FAMILY', families: [fam(1, 'A')] }, 1), true)
  assert.equal(plannableIn({ visibility: 'FAMILY', families: [fam(1, 'A')] }, 2), false)
  assert.equal(plannableIn({ visibility: 'PRIVATE', families: [] }, 1), false)
  assert.equal(plannableIn(null, 1), false)
})

test('pickerRecipes: alle sichtbaren Rezepte (Familien ohne Sicht planen nur über den Plan), alle Suchwörter, alphabetisch', () => {
  const recipes = [
    { id: 1, title: 'Kürbislasagne', visibility: 'FAMILY', families: [fam(1, 'A')], author: 'Anna Jones', tags: [] },
    { id: 2, title: 'Shakshuka', visibility: 'PUBLIC', families: [], author: 'Ottolenghi', tags: ['Eier'] },
    { id: 3, title: 'Geheimrezept', visibility: 'PRIVATE', families: [], tags: [] },
    { id: 4, title: 'Traybake dal', visibility: 'FAMILY', families: [fam(1, 'A')], translatedSearchText: 'Blech-Dal Zitrone', tags: [] },
  ]
  assert.deepEqual(pickerRecipes(recipes).map((r) => r.id), [3, 1, 2, 4])
  assert.deepEqual(pickerRecipes(recipes, 'kurbis anna').map((r) => r.id), [1])
  assert.deepEqual(pickerRecipes(recipes, 'eier').map((r) => r.id), [2])
  assert.deepEqual(pickerRecipes(recipes, 'zitrone').map((r) => r.id), [4])
  assert.deepEqual(pickerRecipes(recipes, 'geheim').map((r) => r.id), [3])
  assert.deepEqual(pickerRecipes(recipes, 'gibtsnicht'), [])
})

test('entryPath: regulär zur Rezeptseite, nur über den Plan über die Eintrags-ID, Geist und Freitext nirgends', () => {
  assert.equal(entryPath({ id: 7, access: ACCESS.FULL, recipe: { id: 42 } }), '/rezepte/42')
  assert.equal(entryPath({ id: 7, access: ACCESS.PLAN, recipe: { id: 42 } }), '/plan/rezept/7')
  assert.equal(entryPath({ id: 7, access: ACCESS.GONE, recipe: null, gone: { title: 'X' } }), null)
  assert.equal(entryPath({ id: 7, access: null, recipe: null, freeText: 'Pizza' }), null)
})

test('Geist: Titel aus dem Snapshot, Untertitel „Nicht mehr verfügbar“, kein Foto', () => {
  const ghost = { id: 9, date: '2026-11-03', access: ACCESS.GONE, recipe: null, freeText: null,
    gone: { title: 'Kürbislasagne', author: 'Anna Jones', source: 'zeit.de', sourceUrl: 'https://www.zeit.de/x' } }
  assert.equal(isGhost(ghost), true)
  assert.equal(isGhost(entry(1, 'x', pizza)), false)
  assert.equal(entryTitle(ghost), 'Kürbislasagne')
  assert.equal(entryMeta(ghost), 'Nicht mehr verfügbar')
  assert.equal(dayPhoto([ghost]), null)
})

test('thumbFor: API-Bild als kleine Fassung, andere Adressen unverändert', () => {
  assert.equal(thumbFor('/api/recipes/5/image?v=abc'), '/api/recipes/5/image?v=abc&size=thumb')
  assert.equal(thumbFor('/api/recipes/5/image'), '/api/recipes/5/image?size=thumb')
  assert.equal(thumbFor('/api/recipes/5/image?v=abc&size=thumb'), '/api/recipes/5/image?v=abc&size=thumb')
  assert.equal(thumbFor('https://example.org/a.jpg'), 'https://example.org/a.jpg')
  assert.equal(thumbFor(null), null)
})

test('insertEntry: sortiert nach Datum, am selben Tag hinten', () => {
  const list = [entry(1, '2026-10-26', pizza), entry(2, '2026-10-28', dal)]
  assert.deepEqual(insertEntry(list, entry(3, '2026-10-27', null, 'Reste')).map((e) => e.id), [1, 3, 2])
  assert.deepEqual(insertEntry(list, entry(4, '2026-10-26', null, 'Salat')).map((e) => e.id), [1, 4, 2])
  assert.equal(list.length, 2)
})

test('dayPhoto: Foto des ersten Eintrags mit Bild', () => {
  assert.equal(dayPhoto([entry(1, 'x', null, 'Oma'), entry(2, 'x', pizza)]), pizza.thumbUrl)
  assert.equal(dayPhoto([entry(1, 'x', dal)]), null)
  assert.equal(dayPhoto(undefined), null)
})

test('bookedDaysLoader: lädt den ganzen sichtbaren Monat und liefert die belegten Tage (auch mehrfach belegt)', async () => {
  const calls = []
  const load = bookedDaysLoader(async (from, to) => {
    calls.push([from, to])
    return [{ id: 1, date: '2026-11-07' }, { id: 2, date: '2026-11-07' }, { id: 3, date: '2026-11-12' }]
  })
  assert.deepEqual(await load(d('2026-11-01')), ['2026-11-07', '2026-11-07', '2026-11-12'])
  assert.deepEqual(calls, [['2026-11-01', '2026-11-30']])
})

test('bookedDaysLoader: Monatswechsel lädt nach, schon geladene Monate kein zweites Mal', async () => {
  const calls = []
  const load = bookedDaysLoader(async (from, to) => {
    calls.push([from, to])
    return from === '2026-12-01' ? [{ date: '2026-12-24' }] : []
  })
  await Promise.all([load(d('2026-11-01')), load(d('2026-11-15'))])
  assert.deepEqual(await load(d('2026-12-01')), ['2026-12-24'])
  await load(d('2026-11-30'))
  assert.deepEqual(calls, [['2026-11-01', '2026-11-30'], ['2026-12-01', '2026-12-31']])
})

test('bookedDaysLoader: Februar im Schaltjahr, Jahreswechsel; Zeitraum bleibt im API-Limit von 62 Tagen', async () => {
  const calls = []
  const load = bookedDaysLoader(async (from, to) => { calls.push([from, to]); return [] })
  await load(d('2028-02-10'))
  await load(d('2026-12-31'))
  await load(d('2027-01-01'))
  assert.deepEqual(calls, [['2028-02-01', '2028-02-29'], ['2026-12-01', '2026-12-31'], ['2027-01-01', '2027-01-31']])
})

test('bookedDaysLoader: Fehler → keine Markierung, der nächste Aufruf versucht es erneut', async () => {
  let fail = true
  let count = 0
  const load = bookedDaysLoader(async () => {
    count += 1
    if (fail) throw new Error('offline')
    return [{ date: '2026-11-09' }]
  })
  assert.deepEqual(await load(d('2026-11-01')), [])
  fail = false
  assert.deepEqual(await load(d('2026-11-01')), ['2026-11-09'])
  assert.equal(count, 2)
})

test('goneSource: Autor + Quelle als Link zur Originalseite, Webadresse als Domain, nur http(s)', () => {
  assert.deepEqual(goneSource({ author: 'Anna Jones', source: 'zeit.de (Wochenmarkt)', sourceUrl: 'https://www.zeit.de/x' }),
    { author: 'Anna Jones', label: 'zeit.de (Wochenmarkt)', href: 'https://www.zeit.de/x' })
  assert.deepEqual(goneSource({ author: '', source: 'https://www.bonappetit.com/recipe/x', sourceUrl: null }),
    { author: '', label: 'bonappetit.com', href: 'https://www.bonappetit.com/recipe/x' })
  assert.deepEqual(goneSource({ author: 'Meera Sodha', source: 'Kochbuch', sourceUrl: 'javascript:alert(1)' }),
    { author: 'Meera Sodha', label: 'Kochbuch', href: null })
  assert.equal(goneSource({ title: 'X' }), null)
  assert.equal(goneSource(null), null)
})
