"""Builds the Google Play graphics in docs/google-play/graphics from the demo captures.

    python tools/play.py

Takes docs/google-play/raw/<lang>/<n>-<name>.png (1080x2340 screens of the demo build, app.glow.demo, taken on a
Mi 9 with adb) and writes the 512 px icon, the 1024x500 feature graphics, the 1600x900 launch posts and eight
1080x1920 captioned screenshots per language. The phone's status bar is painted over with a clean one. Play wants
screenshots no longer than twice their width, so each screen runs off the bottom edge under its caption.
Captions are in Lora, the app's own serif; the banner is the painted sun over a few glass marbles.
Needs `pip install pillow`.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
RAW = ROOT / "docs/google-play/raw"
RES = ROOT / "app/src/main/res"
ART = RES / "drawable-nodpi"
SERIF = RES / "font/lora_medium.ttf"
SERIF_I = RES / "font/lora_italic.ttf"
SANS = RES / "font/inter_semibold.ttf"

# The app's colours (ui/theme/Theme.kt): the warm white, the ink, the rose accent, the gold.
BG = (0xFB, 0xF5, 0xF3)
INK = (0x2A, 0x1C, 0x22)
MUTED = (0x7A, 0x68, 0x70)
ACCENT = (0xDB, 0x3F, 0x7A)
ACCENT_DARK = (0xA8, 0x2A, 0x5B)
ROSE_SOFT = (0xFC, 0xE4, 0xEC)
GOLD = (0xD0, 0x8A, 0x10)
GOLD_SOFT = (0xFC, 0xF0, 0xD5)
# The launcher icon's ground (drawable/ic_launcher_background.xml).
ICON_A = (0xE8, 0x50, 0x8A)
ICON_B = (0xC2, 0x30, 0x6A)

BAR = 80  # the Mi 9's status bar, painted over

NAME = "Glow"
LINES = {
    "en": ("Every win deserves", "a little praise"),
    "ru": ("Каждая победа", "заслуживает похвалы"),
}

# The order they appear in the store and the caption: a line in ink and one in the rose accent.
PAGES = [
    ("1-today", ("Every win deserves", "a little praise"), ("Каждая победа", "заслуживает похвалы")),
    ("2-praise", ("Tiny or huge,", "every win gets cheered"), ("Крошечная или огромная —", "каждую похвалят")),
    ("3-jar", ("A jar that fills up", "with your wins"), ("Банка, которая", "наполняется победами")),
    ("4-voices", ("Five voices", "to cheer you on"), ("Пять голосов,", "которые за тебя болеют")),
    ("5-sos", ("A quiet room", "for hard moments"), ("Тихая комната", "для трудных минут")),
    ("6-translate", ("Your inner critic,", "translated into kind"), ("Перевод с самокритичного", "на добрый")),
    ("7-kind", ("Keep the kind things", "people said about you"), ("Храни добрые слова,", "которые тебе говорили")),
    ("8-orders", ("16 orders", "with three degrees each"), ("16 орденов,", "у каждого три степени")),
]


def font(size, path=SERIF):
    return ImageFont.truetype(str(path), size)


def fit(text, size, width, path=SERIF):
    while size > 20 and font(size, path).getlength(text) > width:
        size -= 2
    return font(size, path)


def gradient(W, H, top, bottom):
    g = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(g)
    for y in range(H):
        t = y / max(1, H - 1)
        d.line([(0, y), (W, y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    return g.convert("RGBA")


def glow(g, cx, cy, r, colour, alpha=150):
    """A soft round light, like the halo behind the sun in the app."""
    mask = Image.new("L", g.size, 0)
    ImageDraw.Draw(mask).ellipse((cx - r, cy - r, cx + r, cy + r), fill=alpha)
    g.paste(Image.new("RGBA", g.size, colour + (255,)), (0, 0), mask.filter(ImageFilter.GaussianBlur(r / 2.5)))


def art(name, size):
    im = Image.open(ART / f"{name}.webp").convert("RGBA")
    k = size / max(im.size)
    return im.resize((round(im.width * k), round(im.height * k)), Image.LANCZOS)


def clean_bar(im):
    """Paints the status bar over with the colour just below it, then a calm one: 9:41, signal, wifi, a full battery."""
    im = im.convert("RGB")
    row = sorted((im.getpixel((x, BAR + 4)) for x in range(im.width)), key=sum)
    ground = row[len(row) // 2]  # the median, so a sparkle or an edge under the bar does not count
    ImageDraw.Draw(im).rectangle((0, 0, im.width, BAR + 3), fill=ground)
    lum = sum(ground) / 3
    ink = (250, 244, 248) if lum < 128 else INK
    d = ImageDraw.Draw(im)
    d.text((56, 40), "9:41", font=font(40, SANS), fill=ink, anchor="lm")
    x = im.width - 56
    # battery
    d.rounded_rectangle((x - 62, 24, x - 6, 56), 7, outline=ink, width=3)
    d.rounded_rectangle((x - 56, 30, x - 12, 50), 3, fill=ink)
    d.rectangle((x - 4, 34, x, 46), fill=ink)
    # wifi: three arcs and a dot
    cx, cy = x - 110, 54
    for r in (34, 23, 12):
        d.arc((cx - r, cy - r, cx + r, cy + r), 225, 315, fill=ink, width=5)
    d.ellipse((cx - 4, cy - 6, cx + 4, cy + 2), fill=ink)
    # signal: four rising bars
    for i in range(4):
        bx = x - 196 + i * 13
        d.rectangle((bx, 54 - 8 - i * 8, bx + 8, 54), fill=ink)
    return im


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=48):
    """A screen on the board: rounded corners, a soft rose shadow and a thin warm edge."""
    sh = Image.new("RGBA", (card.width + 160, card.height + 160), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((80, 100, 80 + card.width, 100 + card.height), r, fill=(150, 40, 80, 70))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(30)), (x - 80, y - 80))
    g.alpha_composite(rounded(card, r), (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 1, y - 1, x + card.width, y + card.height), r, outline=(0xF0, 0xD6, 0xDE), width=3)


def sparkle(d, cx, cy, s, fill):
    """The four-pointed sparkle the app scatters on the SOS screen and in the confetti."""
    k = s * .28
    d.polygon([(cx, cy - s), (cx + k, cy - k), (cx + s, cy), (cx + k, cy + k), (cx, cy + s), (cx - k, cy + k), (cx - s, cy), (cx - k, cy - k)], fill=fill)


def screenshot(src, caption):
    W, H = 1080, 1920
    g = gradient(W, H, ROSE_SOFT, GOLD_SOFT)
    glow(g, W // 2, 150, 300, (255, 255, 255), 170)
    d = ImageDraw.Draw(g)
    f = min((fit(c, 80, W - 140) for c in caption), key=lambda x: x.size)
    d.text((W / 2, 108), caption[0], font=f, fill=INK, anchor="mm")
    d.text((W / 2, 200), caption[1], font=f, fill=ACCENT, anchor="mm")
    sparkle(d, W // 2, 276, 15, GOLD)
    d.line((W // 2 - 150, 276, W // 2 - 30, 276), fill=GOLD, width=3)
    d.line((W // 2 + 30, 276, W // 2 + 150, 276), fill=GOLD, width=3)
    im = clean_bar(Image.open(src))
    w = 880
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 336
    # only the top corners show: the screen runs on past the bottom edge
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def icon_square(size, inset=True):
    """The adaptive icon flattened: the painted art over its rose gradient. Play rounds the corners itself."""
    g = Image.new("RGBA", (size, size))
    d = ImageDraw.Draw(g)
    for i in range(2 * size):
        t = i / (2 * size - 1)
        c = tuple(round(a + (b - a) * t) for a, b in zip(ICON_A, ICON_B))
        d.line([(i, 0), (0, i)], fill=c, width=2)
    g.alpha_composite(Image.open(ART / "ic_launcher_art.webp").convert("RGBA").resize((size, size), Image.LANCZOS))
    return g


def icon():
    icon_square(512).convert("RGB").save(OUT / "icon-512.png", optimize=True)


def logo(size):
    """The icon as the launcher shows it: a rounded square with a soft shadow under it."""
    sq = icon_square(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size - 1, size - 1), size * .23, fill=255)
    sq.putalpha(mask)
    return sq


MARBLES = [  # (marble, x, y, size) as fractions of the scene: a little heap under the sun
    (2, .10, .80, .30), (5, .34, .84, .26), (9, .56, .82, .30), (0, .78, .85, .24),
    (12, .22, .62, .22), (7, .46, .64, .24), (14, .68, .63, .22), (3, .90, .70, .18),
]


def scene(g, x0, y0, w, h):
    """The right half of the banner: the painted sun in its glow, and glass marbles at its feet."""
    cx, cy = x0 + w * .52, y0 + h * .36
    glow(g, int(cx), int(cy), int(h * .55), (255, 214, 150), 170)
    glow(g, int(cx), int(cy), int(h * .32), (255, 244, 220), 200)
    sun = art("orb_sun", int(h * .62))
    g.alpha_composite(sun, (int(cx - sun.width / 2), int(cy - sun.height / 2)))
    for n, fx, fy, fs in sorted(MARBLES, key=lambda m: m[2]):
        m = art(f"marble_{n}", int(h * fs * .85))
        mx, my = int(x0 + w * fx - m.width / 2), int(y0 + h * fy - m.height / 2)
        sh = Image.new("RGBA", g.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).ellipse((mx + m.width * .12, my + m.height * .78, mx + m.width * .88, my + m.height * 1.02), fill=(150, 60, 80, 60))
        g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(8)))
        g.alpha_composite(m, (mx, my))
    d = ImageDraw.Draw(g)
    for fx, fy, fs in ((.12, .18, .035), (.88, .12, .028), (.95, .42, .02), (.06, .44, .022), (.80, .30, .018)):
        sparkle(d, x0 + w * fx, y0 + h * fy, h * fs, GOLD)


def board(W, H):
    g = gradient(W, H, (0xFF, 0xF4, 0xF6), GOLD_SOFT)
    glow(g, int(W * .7), int(H * .4), int(H * .7), ROSE_SOFT, 200)
    return g


def feature(lang):
    W, H = 1024, 500
    g = board(W, H)
    scene(g, 520, 10, 480, 480)
    g.alpha_composite(logo(104), (60, 62))
    d = ImageDraw.Draw(g)
    d.text((188, 114), NAME, font=font(104), fill=INK, anchor="lm")
    lines = LINES[lang]
    f = min((fit(l, 50, 420, SERIF_I) for l in lines), key=lambda x: x.size)
    d.line((64, 222, 230, 222), fill=ACCENT, width=5)
    for i, l in enumerate(lines):
        d.text((62, 252 + i * 64), l, font=f, fill=INK if i == 0 else ACCENT)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def post(lang):
    """The launch post: the banner's look at 1600x900, with a «now on Google Play» plaque."""
    W, H = 1600, 900
    g = board(W, H)
    scene(g, 840, 40, 740, 820)
    g.alpha_composite(logo(170), (96, 96))
    d = ImageDraw.Draw(g)
    d.text((96, 380), NAME, font=font(170), fill=INK, anchor="lm")
    d.line((100, 492, 380, 492), fill=ACCENT, width=7)
    lines = LINES[lang]
    f = min((fit(l, 74, 640, SERIF_I) for l in lines), key=lambda x: x.size)
    for i, l in enumerate(lines):
        d.text((96, 520 + i * 92), l, font=f, fill=INK if i == 0 else ACCENT)
    # the plaque: the app's rose button, rounded, with a darker lip below
    plaque = "Now on Google Play" if lang == "en" else "Теперь в Google Play"
    pf = font(46, SANS)
    tw = int(pf.getlength(plaque))
    px, py, ph = 100, 742, 92
    d.rounded_rectangle((px, py + 8, px + tw + 88, py + ph + 8), 46, fill=ACCENT_DARK)
    d.rounded_rectangle((px, py, px + tw + 88, py + ph), 46, fill=ACCENT)
    d.text((px + 44, py + ph // 2), plaque, font=pf, fill=(255, 255, 255), anchor="lm")
    g.convert("RGB").save(OUT / f"post-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    for lang in ("en", "ru"):
        feature(lang)
        post(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(RAW / lang / f"{name}.png", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
