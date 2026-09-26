export const UTM_SOURCE = 'pastoors_familienrezepte'

const PROVIDERS = {
  unsplash: { label: 'Unsplash', url: 'https://unsplash.com/', utm: true },
  pexels: { label: 'Pexels', url: 'https://www.pexels.com/', utm: false },
}

function isHttpUrl(value) {
  try {
    return ['https:', 'http:'].includes(new URL(value).protocol)
  } catch {
    return false
  }
}

export function withUtm(url) {
  const target = new URL(url)
  target.searchParams.set('utm_source', UTM_SOURCE)
  target.searchParams.set('utm_medium', 'referral')
  return target.toString()
}

// Unsplash-Richtlinie: Fotograf und Unsplash nennen, beide verlinkt, Unsplash-Links mit UTM-Parametern.
// Uploads und Bilder ohne bekannten Fotografen bekommen keine Zeile.
export function imageCreditLinks(credit) {
  const provider = PROVIDERS[credit?.source]
  if (!provider || !credit.name) return null
  const link = (url) => (provider.utm ? withUtm(url) : url)
  return {
    name: credit.name,
    nameUrl: isHttpUrl(credit.profileUrl) ? link(credit.profileUrl) : null,
    provider: provider.label,
    providerUrl: link(provider.url),
  }
}
