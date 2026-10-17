// „Willkommen in der Familie Pastoors“ – bei anderen Namen („Kochclub Köln“) ohne Artikel-Raten
export function welcomeText(name) {
  const n = (name ?? '').trim()
  return /^familie\s/i.test(n) ? `Willkommen in der ${n}` : `Willkommen bei „${n}“`
}
