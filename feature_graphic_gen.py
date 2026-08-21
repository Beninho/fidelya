"""
Generates Play Store feature graphic for Fidelya.
Output: screenshots/feature_graphic.png — 1024x500px
Usage: python3 feature_graphic_gen.py

Suit le design system « Modernist » de l'app : fond #F3F2F2, encre #201E1D,
accent #EC3013, Archivo 800 pour les titres, angles droits, filets tranchés,
aucune ombre ni dégradé. Les lignes de droite rejouent la liste « Mes cartes »
telle que l'écran la rend, couleurs prises dans la grille de fonds.
"""

from PIL import Image, ImageDraw, ImageFont
import os

W, H = 1024, 500

# Rôles Modernist (ui/theme/Color.kt)
BG = (243, 242, 242)
ROW_SURFACE = (255, 255, 255)
INK = (32, 30, 29)
ACCENT = (236, 48, 19)

FONT_PATH = "app/src/main/res/font/archivo.ttf"

# Marge de sécurité Play Store : la vignette rogne les bords, tout le contenu
# tient dans les 924x470 centraux.
MARGIN = 64

# Fonds pris dans `domain/color/ModernistPalette.kt`, un pas 700 par famille.
CARDS = [
    ((13, 96, 163), "Carrefour", "···· 8902", "EAN_13"),
    ((0, 105, 129), "Decathlon", "···· 3196", "CODE_128"),
    ((20, 113, 47), "Biocoop", "···· 7265", "CODE_128"),
]

TAGS = ["SCAN", "HORS LIGNE", "100 % PRIVÉ"]


def font(size, weight="ExtraBold"):
    f = ImageFont.truetype(FONT_PATH, size)
    f.set_variation_by_name(weight)
    return f


def ink(alpha):
    """L'encre à N %, aplatie sur le fond : les opacités du design system."""
    return tuple(round(BG[i] + (INK[i] - BG[i]) * alpha) for i in range(3))


def card_foreground(rgb):
    """`cardForegroundColor` : l'encre du thème au-delà de 0.2 de luminance, le fond en dessous."""
    def channel(c):
        c /= 255
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (channel(c) for c in rgb)
    luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    return INK if luminance > 0.2 else BG


def tracked_text(draw, xy, text, f, fill, tracking=0.0):
    """Rend `text` lettre à lettre : PIL n'a pas d'interlettrage."""
    x, y = xy
    for char in text:
        draw.text((x, y), char, font=f, fill=fill)
        x += draw.textlength(char, font=f) + tracking
    return x


def tracked_width(draw, text, f, tracking=0.0):
    return sum(draw.textlength(c, font=f) for c in text) + tracking * max(0, len(text) - 1)


def draw_title_block(draw):
    x = MARGIN

    # L'amorce de l'écran de liste : un filet d'accent, puis un intertitre en capitales.
    label_font = font(19, "SemiBold")
    draw.rectangle([x, 96, x + 46, 100], fill=ACCENT)
    tracked_text(draw, (x + 62, 84), "CARTES DE FIDÉLITÉ", label_font, ink(0.60), tracking=2.4)

    # Titre : Archivo 800, interlettrage -0.015em du design system.
    title_font = font(124)
    tracked_text(draw, (x - 6, 126), "Fidelya", title_font, INK, tracking=-1.9)

    body_font = font(30, "Regular")
    draw.text((x, 286), "Toutes vos cartes, sur vous,", font=body_font, fill=ink(0.55))
    draw.text((x, 326), "même sans réseau.", font=body_font, fill=ink(0.55))


def draw_tags(draw, y):
    """Les badges de format de la liste : rectangle filet, capitales interlettrées."""
    f = font(17, "SemiBold")
    x = MARGIN
    for tag in TAGS:
        w = tracked_width(draw, tag, f, tracking=2.0)
        box_w, box_h = w + 40, 46
        draw.rectangle([x, y, x + box_w, y + box_h], outline=ink(0.40), width=2)
        tracked_text(draw, (x + 20, y + 12), tag, f, INK, tracking=2.0)
        x += box_w + 14


def draw_row(base, x, y, w, h, card):
    """Une ligne de « Mes cartes » : liseré de couleur, tuile, nom, numéro, format."""
    color, store, number, fmt = card
    draw = ImageDraw.Draw(base)
    draw.rectangle([x, y, x + w, y + h], fill=ROW_SURFACE)

    # Le liseré qui reprend le fond de la carte, à gauche de la ligne.
    draw.rectangle([x, y + 10, x + 8, y + h - 10], fill=color)

    tile = 58
    tx, ty = x + 30, y + (h - tile) // 2
    draw.rectangle([tx, ty, tx + tile, ty + tile], fill=color)
    initial_font = font(34)
    draw.text((tx + tile / 2, ty + tile / 2 - 2), store[0], font=initial_font,
              fill=card_foreground(color), anchor="mm")

    draw.text((tx + tile + 22, y + 18), store, font=font(25), fill=INK)
    draw.text((tx + tile + 22, y + 52), number, font=font(18, "Regular"), fill=ink(0.50))

    f = font(15, "SemiBold")
    bw = tracked_width(draw, fmt, f, tracking=1.6) + 28
    bx = x + w - 22 - bw
    by = y + (h - 34) // 2
    draw.rectangle([bx, by, bx + bw, by + 34], outline=ink(0.40), width=2)
    tracked_text(draw, (bx + 14, by + 7), fmt, f, ink(0.75), tracking=1.6)


def draw_card_list(base):
    x, w = 520, W - 520 - MARGIN
    y, h, gap = 70, 94, 12

    draw = ImageDraw.Draw(base)
    # Le trait noir qui ouvre la liste sous la barre de titre de l'écran.
    draw.rectangle([x, y - 14, x + w, y - 10], fill=INK)

    for card in CARDS:
        draw_row(base, x, y, w, h, card)
        y += h + gap

    # Le bouton d'action, pleine largeur et en accent, comme au bas de la liste.
    btn_h = 68
    draw.rectangle([x, y + 6, x + w, y + 6 + btn_h], fill=ACCENT)
    draw.text((x + 26, y + 6 + btn_h / 2 - 2), "Ajouter une carte", font=font(25),
              fill=BG, anchor="lm")


def main():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)

    draw_title_block(draw)
    draw_tags(draw, 392)
    draw_card_list(img)

    os.makedirs("screenshots", exist_ok=True)
    out = "screenshots/feature_graphic.png"
    img.save(out, "PNG", optimize=True)
    print(f"✓ {out} — {W}×{H}px — {os.path.getsize(out)//1024} KB")


if __name__ == "__main__":
    main()
