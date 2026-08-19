"""
Generates Play Store feature graphic for Fidelya.
Output: screenshots/feature_graphic.png — 1024x500px
Usage: python3 feature_graphic_gen.py
"""

from PIL import Image, ImageDraw, ImageFont
import os

W, H = 1024, 500

PRIMARY      = (103, 80, 164)
PRIMARY_DARK = (79, 55, 139)
WHITE        = (255, 255, 255, 255)
WHITE_DIM    = (255, 255, 255, 190)
PILL_BG      = (255, 255, 255, 55)
BLACK        = (0, 0, 0)

CARD_COLORS = [
    (103, 80, 164),   # purple
    (181, 22,  43),   # red
    ( 26,107,  58),   # green
    ( 21,101, 192),   # blue
]

def load_font(size, bold=False):
    for path in [
        f"/System/Library/Fonts/Supplemental/{'Arial Bold' if bold else 'Arial'}.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
    ]:
        try:
            return ImageFont.truetype(path, size)
        except:
            continue
    return ImageFont.load_default()

def lighten(rgb, amt):
    return tuple(min(255, c + amt) for c in rgb)

def gradient_bg(img):
    draw = ImageDraw.Draw(img)
    for x in range(W):
        t = x / W
        r = int(PRIMARY[0] + t * (PRIMARY_DARK[0] - PRIMARY[0]))
        g = int(PRIMARY[1] + t * (PRIMARY_DARK[1] - PRIMARY[1]))
        b = int(PRIMARY[2] + t * (PRIMARY_DARK[2] - PRIMARY[2]))
        draw.line([(x, 0), (x, H)], fill=(r, g, b, 255))

def decorative_circles(img):
    """Subtle circles placed well outside the text zone (right/bottom only)."""
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    # bottom-right large circle — won't touch left text
    draw.ellipse([720, 280, 1120, 680], fill=(255, 255, 255, 16))
    # top-right small circle
    draw.ellipse([860, -80, 1020, 80],  fill=(255, 255, 255, 22))
    img.alpha_composite(overlay)

def mini_card(w, h, color, store, number, angle=0):
    card = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cd = ImageDraw.Draw(card)
    cd.rounded_rectangle([0, 0, w-1, h-1], radius=20, fill=color + (255,))
    cd.rounded_rectangle([0, 0, w-1, h//3], radius=20, fill=lighten(color, 28) + (255,))

    f_store = load_font(max(16, w // 7), bold=True)
    f_num   = load_font(max(11, w // 11))
    cd.text((20, h - 56), store,  fill=(255, 255, 255, 230), font=f_store)
    cd.text((20, h - 28), number, fill=(255, 255, 255, 160), font=f_num)

    # barcode hint lines
    bx = 20
    while bx < w - 20:
        bw = 3 if bx % 13 == 0 else 2
        cd.rectangle([bx, 22, bx + bw, 50], fill=(255, 255, 255, 60))
        bx += bw + 4

    return card.rotate(angle, expand=True, resample=Image.BICUBIC)

def draw_cards_fan(base):
    cards_data = [
        (CARD_COLORS[0], "Carrefour", "724 819 003", -16),
        (CARD_COLORS[1], "Fnac",      "FR-48271",    -6),
        (CARD_COLORS[2], "Décathlon", "DK 00291",     6),
        (CARD_COLORS[3], "IKEA",      "800 234 112",  16),
    ]
    cx, cy = 760, 260
    for color, store, num, angle in cards_data:
        card = mini_card(230, 148, color, store, num, angle)
        ox = cx - card.width  // 2
        oy = cy - card.height // 2
        base.alpha_composite(card, (ox, oy))

def draw_pills(draw, labels, x, y):
    for label in labels:
        f = load_font(27, bold=True)
        bbox = draw.textbbox((0, 0), label, font=f)
        pw = bbox[2] - bbox[0] + 40
        ph = 42
        # semi-transparent pill via RGBA draw
        draw.rounded_rectangle([x, y, x + pw, y + ph], radius=21,
                                fill=(255, 255, 255, 50))
        draw.text((x + pw // 2, y + ph // 2), label,
                  fill=(0, 0, 0, 230), font=f, anchor="mm")
        x += pw + 14

def main():
    img = Image.new("RGBA", (W, H))
    gradient_bg(img)
    decorative_circles(img)
    draw_cards_fan(img)

    draw = ImageDraw.Draw(img)

    # App name
    draw.text((68, 105), "Fidelya", fill=WHITE, font=load_font(98, bold=True))

    # Tagline
    draw.text((68, 228), "Toutes vos cartes de fidélité", fill=WHITE_DIM, font=load_font(36))
    draw.text((68, 274), "dans votre poche.", fill=WHITE_DIM, font=load_font(36))

    # Pills
    draw_pills(draw, ["Scan", "Saisie manuelle", "Hors ligne", "100 % privé"], 68, 358)

    # bottom line
    draw.rectangle([68, 462, 310, 466], fill=(255, 255, 255, 80))

    os.makedirs("screenshots", exist_ok=True)
    out = "screenshots/feature_graphic.png"
    img.convert("RGB").save(out, "PNG", optimize=True)
    print(f"✓ {out} — {W}×{H}px — {os.path.getsize(out)//1024} KB")

if __name__ == "__main__":
    main()
