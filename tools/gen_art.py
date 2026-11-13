"""Draws Glow's art with the Codex CLI image tool: the voices, category icons, orders, marbles, the jar and the icon.

Codex saves images into ~/.codex/generated_images/<session>/; the image of the run's own session is copied to
art/sheets/<name>.png. Sheets already there are skipped; delete one to draw it again.

    python tools/gen_art.py               # everything missing
    python tools/gen_art.py voices orders # only these
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

STYLE = (
    "Style: warm hand-painted storybook illustration, soft gouache texture with gentle grain, rounded friendly shapes, "
    "a soft inner glow as if lit by candlelight, a cosy palette of rose pink (#DB3F7A), honey gold (#D08A10), "
    "peach, cream, sage green and dusty violet. Clean silhouettes, no harsh black outlines, no text or letters anywhere."
)
BG = "on a flat solid pure magenta (#FF00FF) background; nothing else on the background, no shadows on it, no grid lines"

HEAD = "Use your image generation tool to create ONE image. Do nothing else (no file copying, no code).\n\n"

SHEETS = {
    "voices": HEAD + f"""Image: six character portraits for a gentle self-praise app, each a head-and-shoulders bust facing the viewer, warm and kind.
{STYLE}
Layout: 3 columns x 2 rows of equal square cells {BG}. One character per cell, centred, with clear margin around it.
Cells, left to right, top to bottom:
1) the Friend: a cheerful young person with messy hair, a cosy oversized knitted sweater, a wide warm smile, arms open for a hug
2) the Granny: a plump loving grandmother with silver hair in a bun, round glasses, a flowered shawl, holding a plate of pies
3) the Commentator: an excited sports commentator with a headset microphone, a striped scarf, mid-shout of joy
4) the Mentor: a wise round owl with a small graduation cap, half-moon spectacles and a calm proud look
5) the Butler: an elegant gentleman butler with a neat moustache, a tailcoat and white gloves, a slight respectful bow
6) the Glow spirit: a small round glowing star-like creature with tiny arms and a happy face, radiating soft golden light""",

    "categories": HEAD + f"""Image: eight small round icons for kinds of everyday wins in a gentle self-praise app.
{STYLE}
Layout: 4 columns x 2 rows of equal square cells {BG}. One icon per cell, centred, each a simple object (no frame, no circle behind it), clear margin.
Icons, left to right, top to bottom:
1) Home: a tiny cosy house with a lit window and a chimney puff
2) Body: a sprouting heart-shaped leaf on a little stem, or a running shoe with a sparkle
3) Work: a neat stack of papers with a fountain pen and a small check mark ribbon
4) Mind: a glowing open book with a little light bulb above it
5) People: two cups of tea touching, with a small heart of steam
6) Courage: a small brave lion's head or a tiny flame in a lantern
7) Self-care: a bath with bubbles and a candle, or a hug-shaped blanket
8) Other: a bright sparkle star""",

    "hobby": HEAD + f"""Image: one small icon for the kind of win "Hobby" in a gentle self-praise app: a little artist's palette with a paintbrush, a ball of yarn with knitting needles and a guitar pick, grouped as one cosy object, no frame, no circle behind it, centred with clear margin.
{STYLE}
Square image, {BG}.""",

    "orders": HEAD + f"""Image: sixteen award medals (orders) for a gentle self-praise app, each a round golden medallion with an engraved picture, hanging from a short silk ribbon.
{STYLE}
Layout: 4 columns x 4 rows of equal square cells {BG}. One medal per cell, centred, all medals the same size and shape, clear margin. Each medal has its own ribbon colour and its own picture in the middle:
1) Many Wins: a jar full of glowing marbles (pink ribbon)
2) Small Things: a tiny ladybug on a leaf (peach ribbon)
3) Great Deeds: a mountain peak with a flag (gold ribbon)
4) Steady Light: a candle with a steady flame (amber ribbon)
5) Evening Pride: a crescent moon over a cup of tea (violet ribbon)
6) Kind Words: a folded letter with a heart seal (rose ribbon)
7) Weathered Storms: an umbrella with rain and a rainbow behind (sky-blue ribbon)
8) Kind Inner Voice: a little bird singing to a heart (sage ribbon)
9) Early Bird: a sunrise with a small bird (light orange ribbon)
10) Cosy Home: a house with a warm window (warm brown ribbon)
11) Cared-for Body: a heart with a leaf (green ribbon)
12) Labour Valour: crossed hammer and quill (navy ribbon)
13) Curious Mind: an open book with a light bulb (teal ribbon)
14) Warm Ties: two clasped hands (coral ribbon)
15) Quiet Courage: a lion's head (crimson ribbon)
16) Self-Tenderness: a person hugging themselves inside a heart (lilac ribbon)""",

    "marbles": HEAD + f"""Image: sixteen shiny glass marbles for a jar of wins, each a perfect sphere seen straight on, the same size, with a soft white highlight top-left, a gentle inner swirl and a faint glow.
{STYLE}
Layout: 4 columns x 4 rows of equal square cells {BG}. One marble per cell, centred, all exactly the same size, clear margin.
Colours, left to right, top to bottom:
1 rose pink, 2 coral red, 3 tangerine orange, 4 honey gold,
5 lemon yellow, 6 lime green, 7 sage green, 8 teal,
9 sky blue, 10 cornflower blue, 11 indigo, 12 dusty violet,
13 lilac, 14 plum, 15 warm brown, 16 pearl grey""",

    "jar": HEAD + f"""Image: one empty glass jar for collecting marbles, seen straight from the front, standing upright: a round-shouldered mason jar with a short wide neck, a twine bow tied round the neck and a small blank cork lid, clear glass with soft highlights and a faint golden glow inside. The jar is centred and fills most of the height.
{STYLE}
Portrait image, {BG}. The inside of the jar is empty and see-through (show the magenta through the glass, lightly tinted).""",

    "icon": HEAD + f"""Image: an app icon for "Glow", a gentle self-praise app: a small glass jar full of glowing pink, gold and violet marbles, giving off a warm golden glow, with two tiny sparkles. Simple, bold, readable at small size, centred, filling about 70% of the square.
{STYLE}
Square image, {BG}.""",
}


def image_of(output):
    """The image made in the Codex session named in its output (other sessions may draw at the same time)."""
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(name, prompt):
    dst = OUT / f"{name}.png"
    if dst.exists():
        return f"skip {name}"
    r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", capture_output=True, cwd=str(ROOT))
    p = image_of(r.stdout + r.stderr)
    if p is None:
        return f"FAIL {name}: {(r.stdout + r.stderr)[-300:]}"
    shutil.copy(p, dst)
    return f"ok {name} {p}"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:] or list(SHEETS)
    with ThreadPoolExecutor(3) as pool:
        for line in pool.map(lambda n: run(n, SHEETS[n]), want):
            print(line, flush=True)


if __name__ == "__main__":
    main()
