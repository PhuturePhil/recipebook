// Teilen-Seite für Messenger-Vorschauen: nginx liefert /share/<token> als share-shell.html mit SSI aus.
// Statt des festen <title> holt nginx Titel + Open-Graph-Tags vom Backend (interne Location
// /share-head/<token> → /api/share/<token>/head); liefert sie nichts (Fehler, Backend aus), bleibt
// es beim bisherigen Titel. Sonst identisch mit index.html.
export const SHARE_SHELL_FILE = 'share-shell.html'
export const APP_TITLE_TAG = '<title>Pastoors Familienrezepte</title>'

const FALLBACK_BLOCK = 'share_head_fallback'

export function shareShellHtml(indexHtml) {
  if (!indexHtml.includes(APP_TITLE_TAG)) {
    throw new Error(`share-shell: ${APP_TITLE_TAG} fehlt in index.html`)
  }
  const ssi =
    `<!--# block name="${FALLBACK_BLOCK}" -->${APP_TITLE_TAG}` +
    '<meta name="robots" content="noindex, nofollow" /><!--# endblock -->' +
    `<!--# include virtual="/share-head/$share_token" stub="${FALLBACK_BLOCK}" -->`
  return indexHtml.replace(APP_TITLE_TAG, () => ssi)
}
