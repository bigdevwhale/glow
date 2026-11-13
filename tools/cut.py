"""Cuts the Codex sheets in art/sheets into Glow's drawables (magenta keyed out, each piece cropped to itself).

    python tools/cut.py

voices (3×2)      voice_friend … voice_butler, voice_glow
categories (4×2)  cat_home … cat_care, cat_none
orders (4×4)      order_wins … order_care (Order enum order)
marbles (4×4)     marble_0 … marble_15 (MarblePalette order)
jar               jar_glass
icon              the launcher foreground, in drawable-nodpi as ic_launcher_art (432 px, the 66/108 safe zone kept)
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

VOICES = ["friend", "granny", "commentator", "mentor", "butler", "glow"]
CATS = ["home", "body", "work", "mind", "people", "brave", "care", "none"]
ORDERS = ["wins", "tiny", "huge", "run", "evening", "kind", "sos", "reframe", "early",
          "home", "body", "work", "mind", "people", "brave", "care"]


def outside(im, lo):
    """Pixels of the magenta background reachable from the image's border: pink inside a marble or a cheek stays."""
    from collections import deque
    px = im.load()
    w, h = im.size
    seen = bytearray(w * h)
    q = deque((x, y) for x in range(w) for y in (0, h - 1))
    q.extend((x, y) for y in range(h) for x in (0, w - 1))
    while q:
        x, y = q.popleft()
        i = y * w + x
        if seen[i]:
            continue
        r, g, b, a = px[x, y]
        if min(r, b) - g <= lo:
            continue
        seen[i] = 1
        if x > 0: q.append((x - 1, y))
        if x < w - 1: q.append((x + 1, y))
        if y > 0: q.append((x, y - 1))
        if y < h - 1: q.append((x, y + 1))
    return seen


def key(im, glass=False):
    """Magenta → transparent. Pinkish fringes and (for [glass]) the magenta seen through glass fade out."""
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    hi, lo = (60, 20) if glass else (90, 40)
    bg = None if glass else outside(im, lo)
    for y in range(h):
        for x in range(w):
            if bg is not None and not bg[y * w + x]:
                continue
            r, g, b, a = px[x, y]
            m = min(r, b) - g  # how magenta
            if m > hi:
                px[x, y] = (0, 0, 0, 0)
            elif m > lo:
                k = (m - lo) / (hi - lo)
                px[x, y] = (int(r - (r - g) * k), g, int(b - (b - g) * k), int(a * (1 - k) * (1 - k * .3)))
    return im


def frames(sheet, cols, rows, size):
    im = key(Image.open(sheet))
    w, h = im.size
    cw, ch = w / cols, h / rows
    out = []
    for j in range(rows):
        for i in range(cols):
            cell = im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch)))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            cell = cell.crop(box) if box else cell
            cell.thumbnail((size, size), Image.LANCZOS)
            out.append(cell)
    return out


def square(im, size):
    """Centres [im] on a transparent square, so round things stay round whatever their crop."""
    s = max(im.size)
    sq = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    sq.paste(im, ((s - im.width) // 2, (s - im.height) // 2))
    return sq.resize((size, size), Image.LANCZOS)


def marbles(sheet, cols, rows, size):
    """Marbles are round: each is found by the near-pure background around it and cut with a circle, so a pink or
    violet glass edge (close to the magenta) isn't eaten by the keying."""
    from PIL import ImageDraw
    src = Image.open(sheet).convert("RGBA")
    w, h = src.size
    cw, ch = w / cols, h / rows
    out = []
    for j in range(rows):
        for i in range(cols):
            cell = src.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch)))
            px = cell.load()
            mask = Image.new("L", cell.size, 0)
            mp = mask.load()
            for y in range(cell.height):
                for x in range(cell.width):
                    r, g, b, a = px[x, y]
                    if not (r > 200 and b > 200 and g < 70):  # anything that isn't the flat magenta
                        mp[x, y] = 255
            # The disc from its area and centroid: a sliver of the neighbouring marble at the cell's edge barely moves it.
            n = sx = sy = 0
            for y in range(cell.height):
                for x in range(cell.width):
                    if mp[x, y]:
                        n += 1; sx += x; sy += y
            cx, cy = sx / n, sy / n
            rad = (n / 3.14159) ** .5 - 3
            disc = Image.new("L", cell.size, 0)
            ImageDraw.Draw(disc).ellipse((cx - rad, cy - rad, cx + rad, cy + rad), fill=255)
            cell.putalpha(disc)
            cell = cell.crop((int(cx - rad), int(cy - rad), int(cx + rad), int(cy + rad))).resize((size, size), Image.LANCZOS)
            out.append(cell)
    return out


def save(im, name):
    im.save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for n, fr in zip(VOICES, frames(SHEETS / "voices.png", 3, 2, 360)):
        save(fr, f"voice_{n}")
    for n, fr in zip(CATS, frames(SHEETS / "categories.png", 4, 2, 192)):
        save(fr, f"cat_{n}")
    for n, fr in zip(ORDERS, frames(SHEETS / "orders.png", 4, 4, 300)):
        save(fr, f"order_{n}")
    for n, fr in enumerate(marbles(SHEETS / "marbles.png", 4, 4, 128)):
        save(fr, f"marble_{n}")
    jar = key(Image.open(SHEETS / "jar.png"), glass=True)
    jar = jar.crop(jar.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox())
    jar.thumbnail((720, 720), Image.LANCZOS)
    save(jar, "jar_glass")
    icon = key(Image.open(SHEETS / "icon.png"))
    icon = icon.crop(icon.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox())
    # The art sits inside the 66 of 108 safe circle: 0.6 of the side.
    icon.thumbnail((int(432 * .6), int(432 * .6)), Image.LANCZOS)
    fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    fg.paste(icon, ((432 - icon.width) // 2, (432 - icon.height) // 2), icon)
    save(fg, "ic_launcher_art")
    print("ok")


if __name__ == "__main__":
    main()
