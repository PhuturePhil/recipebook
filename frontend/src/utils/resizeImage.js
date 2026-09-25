export const MAX_IMAGE_EDGE = 1600
export const JPEG_QUALITY = 0.8

// Target size for an image whose longer edge may be at most maxEdge; never upscales.
export function fitWithin(width, height, maxEdge = MAX_IMAGE_EDGE) {
  const scale = Math.min(1, maxEdge / Math.max(width, height))
  return { width: Math.round(width * scale), height: Math.round(height * scale) }
}

function readAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function loadImage(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => resolve({ img, release: () => URL.revokeObjectURL(url) })
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Bild konnte nicht gelesen werden'))
    }
    img.src = url
  })
}

// Phone photos arrive at ~3000 px and several MB; stored unshrunk they bloat every recipe load.
// Returns a JPEG data URL with the longer edge at most MAX_IMAGE_EDGE. Falls back to the original
// file if the browser cannot decode it (the upload still works, just unshrunk).
export async function resizeImageFile(file, maxEdge = MAX_IMAGE_EDGE, quality = JPEG_QUALITY) {
  let loaded
  try {
    loaded = await loadImage(file)
  } catch {
    return readAsDataUrl(file)
  }
  try {
    const { img } = loaded
    const { width, height } = fitWithin(img.naturalWidth, img.naturalHeight, maxEdge)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    // JPEG has no transparency; white instead of black behind transparent PNG areas
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, width, height)
    ctx.drawImage(img, 0, 0, width, height)
    const resized = canvas.toDataURL('image/jpeg', quality)
    // Small, already compressed originals can come out larger; keep whichever is smaller
    if (file.size && resized.length * 0.75 > file.size && Math.max(img.naturalWidth, img.naturalHeight) <= maxEdge) {
      return readAsDataUrl(file)
    }
    return resized
  } finally {
    loaded.release()
  }
}
