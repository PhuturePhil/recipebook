"""One-off: shrink uploaded recipe images (data URLs in recipes.image_url) to at most 1600 px, JPEG 80.

Usage: DATABASE_URL=postgresql://user:pass@host/db python shrink_recipe_images.py [--apply]
Without --apply it only reports what it would do. Needs Pillow and psycopg.
Uploads made before the client-side resize were stored as full phone photos (~3000 px, up to 5 MB).
"""
import base64
import io
import os
import re
import sys

import psycopg
from PIL import Image, ImageOps

MAX_EDGE = 1600
QUALITY = 80
DATA_URL = re.compile(r"^data:(image/[a-zA-Z0-9.+-]+);base64,(.*)$", re.S)


def shrink(raw: bytes) -> tuple[bytes, tuple[int, int], tuple[int, int]]:
    img = Image.open(io.BytesIO(raw))
    before = img.size
    # Phone photos are often stored sideways with an EXIF rotation flag; bake it in before dropping EXIF
    img = ImageOps.exif_transpose(img)
    if img.mode not in ("RGB", "L"):
        background = Image.new("RGB", img.size, "white")
        background.paste(img, mask=img.convert("RGBA").split()[-1])
        img = background
    img.thumbnail((MAX_EDGE, MAX_EDGE), Image.LANCZOS)
    out = io.BytesIO()
    img.save(out, "JPEG", quality=QUALITY, optimize=True, progressive=True)
    return out.getvalue(), before, img.size


def main() -> None:
    apply = "--apply" in sys.argv
    total_before = total_after = 0
    with psycopg.connect(os.environ["DATABASE_URL"]) as conn:
        rows = conn.execute(
            "SELECT id, image_url FROM recipes WHERE image_url LIKE 'data:%' ORDER BY id"
        ).fetchall()
        for recipe_id, url in rows:
            m = DATA_URL.match(url)
            if not m:
                print(f"{recipe_id}: kein Bild-Data-URL, übersprungen")
                continue
            raw = base64.b64decode(m.group(2))
            small, before, after = shrink(raw)
            new_url = "data:image/jpeg;base64," + base64.b64encode(small).decode()
            # Images already within the limit are left alone; recompressing them gains little and costs quality
            keep = max(before) <= MAX_EDGE or len(new_url) >= len(url)
            total_before += len(url)
            total_after += len(url) if keep else len(new_url)
            print(f"{recipe_id}: {before[0]}x{before[1]} {len(url)/1e6:.2f} MB -> "
                  + ("unverändert (schon klein)" if keep else f"{after[0]}x{after[1]} {len(new_url)/1e6:.2f} MB"))
            if apply and not keep:
                conn.execute("UPDATE recipes SET image_url = %s WHERE id = %s", (new_url, recipe_id))
        if apply:
            conn.commit()
    print(f"Summe: {total_before/1e6:.2f} MB -> {total_after/1e6:.2f} MB" + ("" if apply else " (Probelauf, nichts geschrieben)"))


if __name__ == "__main__":
    main()
