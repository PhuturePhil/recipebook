// Foto-Scan (Sicherheitspaket K1): höchstens 5 Fotos und 8 MB je Scan. Fotos werden vorher verkleinert –
// 2048 px reichen der Texterkennung, und mehrere Handyfotos passen so in eine Anfrage.

export const MAX_SCAN_IMAGES = 5
export const MAX_SCAN_BYTES = 8 * 1024 * 1024
export const SCAN_IMAGE_EDGE = 2048
export const SCAN_JPEG_QUALITY = 0.85

const GENERIC_ERROR = 'Das Rezeptbild konnte nicht analysiert werden. Bitte versuche es erneut.'
export const TOO_LARGE_ERROR = 'Die Fotos sind zusammen zu groß (höchstens 8 MB). Bitte nimm weniger Fotos.'

export function splitDataUrl(dataUrl) {
  const match = /^data:([^;,]+);base64,(.*)$/s.exec(dataUrl || '')
  if (!match) return null
  return { mimeType: match[1], imageData: match[2] }
}

// Wie viele der neu gewählten Dateien noch dazu passen
export function acceptScanFiles(currentCount, files) {
  const room = Math.max(0, MAX_SCAN_IMAGES - currentCount)
  const accepted = files.slice(0, room)
  return {
    accepted,
    notice: files.length > room
      ? `Höchstens ${MAX_SCAN_IMAGES} Fotos pro Scan – ${files.length - room === 1 ? 'ein Foto wurde' : `${files.length - room} Fotos wurden`} nicht übernommen.`
      : '',
  }
}

// Größe der JSON-Anfrage (Base64-Text plus etwas Rahmen je Bild)
export function scanPayloadBytes(images) {
  return images.reduce((sum, img) => sum + (img.imageData?.length || 0) + (img.mimeType?.length || 0) + 40, 2)
}

export function scanTooLarge(images) {
  return scanPayloadBytes(images) > MAX_SCAN_BYTES
}

// Klare Server-Meldungen (Tageskontingent, zu groß, ungeeignetes Foto) zeigen, sonst die allgemeine
export function scanErrorMessage(error) {
  if (error && [400, 413, 429].includes(error.status) && error.message && !error.message.startsWith('HTTP error')) {
    return error.message
  }
  // nginx antwortet bei zu großen Anfragen ohne JSON
  if (error?.status === 413) return TOO_LARGE_ERROR
  return GENERIC_ERROR
}
