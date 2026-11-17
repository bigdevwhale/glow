"""Builds the README screenshots in docs/screenshots from the demo captures.

    python tools/shots.py

Takes docs/google-play/raw/<lang>/<n>-<name>.png (1080x2340, see tools/play.py), cuts off the phone's status bar
and writes 540 px screens (<name>.png in English, <name>.ru.png in Russian) plus overview.png: four screens on a
dark plum glow, the README's header picture. Needs `pip install pillow`.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "docs/google-play/raw"
OUT = ROOT / "docs/screenshots"

BAR = 80  # the Mi 9's status bar
WIDTH = 540

# README name -> capture
SCREENS = {
    "today": "1-today",
    "praise": "2-praise",
    "jar": "3-jar",
    "kind": "7-kind",
    "sos": "5-sos",
    "me": "8-orders",
    "report": "9-report",
    "translate": "6-translate",
}
OVERVIEW = ["today", "praise", "jar", "me"]

EDGE = (45, 18, 37)  # the plum at the corners
HEART = (176, 86, 90)  # the warm light at the bottom middle


def screen(lang, name):
    im = Image.open(RAW / lang / f"{SCREENS[name]}.png").convert("RGB")
    im = im.crop((0, BAR, im.width, im.height))
    return im.resize((WIDTH, round(im.height * WIDTH / im.width)), Image.LANCZOS)


def backdrop(W, H):
    """A radial glow: warm rose low in the middle, fading to plum at the corners."""
    small = Image.new("RGB", (W // 8, H // 8))
    px = small.load()
    cx, cy = small.width / 2, small.height * 1.05
    reach = (cx ** 2 + cy ** 2) ** .5
    for y in range(small.height):
        for x in range(small.width):
            t = min(1, ((x - cx) ** 2 + (y - cy) ** 2) ** .5 / reach) ** 1.3
            px[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(HEART, EDGE))
    return small.resize((W, H), Image.BICUBIC).convert("RGBA")


def overview(lang):
    W, H = 2400, 1300
    g = backdrop(W, H)
    for i, name in enumerate(OVERVIEW):
        im = screen(lang, name)
        w = 496
        im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
        x, y, r = 52 + i * 600, 90 if i % 2 == 0 else 150, 44
        sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle((x, y + 24, x + w, y + im.height + 24), r, fill=(20, 4, 14, 150))
        g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(26)))
        mask = Image.new("L", im.size, 0)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, im.height - 1), r, fill=255)
        g.paste(im, (x, y), mask)
    return g.convert("RGB")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for lang, suffix in (("en", ""), ("ru", ".ru")):
        for name in SCREENS:
            screen(lang, name).save(OUT / f"{name}{suffix}.png", optimize=True)
        overview(lang).save(OUT / f"overview{suffix}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
