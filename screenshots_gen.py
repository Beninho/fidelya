"""
Generates Play Store phone mockup screenshots for Fidelya.
Output: screenshots/ directory, 1080x1920px PNG (9:16).
Usage: python3 screenshots_gen.py
"""

from PIL import Image, ImageDraw, ImageFont
import os, math

W, H = 1080, 1920
OUT = "screenshots"
os.makedirs(OUT, exist_ok=True)

# --- Palette Material 3 Fidelya ---
BG          = "#FFFBFE"   # surface
BG_DARK     = "#1C1B1F"
PRIMARY     = "#6750A4"
PRIMARY_CTR = "#EADDFF"
ON_PRIMARY  = "#FFFFFF"
SECONDARY   = "#625B71"
SURFACE_VAR = "#E7E0EC"
OUTLINE     = "#CAC4D0"
ON_BG       = "#1C1B1F"
ON_BG_2     = "#49454F"

CARD_COLORS = [
    "#6750A4", "#B5162B", "#1A6B3A", "#1565C0",
    "#E65100", "#00695C", "#AD1457", "#4527A0",
]

def load_font(size, bold=False):
    try:
        name = "Arial Bold" if bold else "Arial"
        return ImageFont.truetype(f"/System/Library/Fonts/Supplemental/{name}.ttf", size)
    except:
        try:
            path = "/System/Library/Fonts/Helvetica.ttc"
            return ImageFont.truetype(path, size)
        except:
            return ImageFont.load_default()

def rounded_rect(draw, xy, radius, fill):
    x0, y0, x1, y1 = xy
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=fill)

def status_bar(draw, dark=False):
    color = "#FFFFFF" if dark else ON_BG
    f = load_font(32)
    draw.text((60, 48), "9:41", fill=color, font=f)
    # battery/signal simplified dots
    for i, x in enumerate([980, 1000, 1020]):
        draw.ellipse([x, 52, x+14, 66], fill=color)

def nav_bar(img, dark=False):
    draw = ImageDraw.Draw(img)
    y = H - 100
    color = ON_BG_2 if not dark else "#CAC4D0"
    # home indicator
    draw.rounded_rectangle([W//2-80, y+30, W//2+80, y+44], radius=8, fill=color)

def top_app_bar(draw, title, bg=BG, fg=ON_BG, subtitle=None):
    draw.rectangle([0, 90, W, 90+112], fill=bg)
    f_title = load_font(52, bold=True)
    draw.text((60, 118), title, fill=fg, font=f_title)
    if subtitle:
        f_sub = load_font(34)
        draw.text((60, 178), subtitle, fill=ON_BG_2, font=f_sub)

def fab(draw, x, y, r=72):
    draw.ellipse([x-r, y-r, x+r, y+r], fill=PRIMARY)
    # plus sign
    draw.rectangle([x-28, y-6, x+28, y+6], fill=ON_PRIMARY)
    draw.rectangle([x-6, y-28, x+6, y+28], fill=ON_PRIMARY)

def card_chip(draw, x, y, w, h, color, store, number, emoji="★"):
    rounded_rect(draw, [x, y, x+w, y+h], 24, color)
    # subtle highlight
    rounded_rect(draw, [x, y, x+w, y+h//3], 24, _lighten(color, 30))
    f_store = load_font(38, bold=True)
    f_num   = load_font(28)
    f_emoji = load_font(56)
    draw.text((x+24, y+20), emoji, fill="#FFFFFF99", font=f_emoji)
    draw.text((x+24, y+h-110), store, fill="#FFFFFFEE", font=f_store)
    draw.text((x+24, y+h-62),  number, fill="#FFFFFFBB", font=f_num)

def _lighten(hex_color, amount):
    h = hex_color.lstrip("#")
    r, g, b = int(h[0:2],16), int(h[2:4],16), int(h[4:6],16)
    r = min(255, r+amount); g = min(255, g+amount); b = min(255, b+amount)
    return f"#{r:02X}{g:02X}{b:02X}"

def barcode_lines(draw, x, y, w, h):
    import random; random.seed(42)
    cx = x
    while cx < x+w:
        bar_w = random.randint(3, 12)
        if random.random() > 0.45:
            draw.rectangle([cx, y, cx+bar_w, y+h], fill="#000000")
        cx += bar_w + random.randint(2, 8)

def qr_block(draw, cx, cy, size):
    cell = size // 21
    import random; random.seed(7)
    for r in range(21):
        for c in range(21):
            if (r<7 and c<7) or (r<7 and c>13) or (r>13 and c<7):
                color = "#000000" if not (1<=r<=5 and 1<=c<=5) and not (1<=r<=5 and 15<=c<=19) and not (15<=r<=19 and 1<=c<=5) else "#000000"
                outer = (r in (0,6) and 0<=c<=6) or (c in (0,6) and 0<=r<=6) or \
                        (r in (0,6) and 14<=c<=20) or (c in (14,20) and 0<=r<=6) or \
                        (r in (14,20) and 0<=c<=6) or (c in (0,6) and 14<=r<=20)
                inner = (2<=r<=4 and 2<=c<=4) or (2<=r<=4 and 16<=c<=18) or (16<=r<=18 and 2<=c<=4)
                if outer or inner:
                    draw.rectangle([cx-size//2+c*cell, cy-size//2+r*cell,
                                    cx-size//2+(c+1)*cell-1, cy-size//2+(r+1)*cell-1], fill="#000000")
            elif random.random() > 0.55:
                draw.rectangle([cx-size//2+c*cell, cy-size//2+r*cell,
                                cx-size//2+(c+1)*cell-1, cy-size//2+(r+1)*cell-1], fill="#000000")

# ─────────────────────────────────────────
# SCREEN 1 — Liste des cartes
# ─────────────────────────────────────────
def screen_card_list():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)
    top_app_bar(draw, "Fidelya")

    stores = [
        ("Carrefour",  "724 819 003 421", CARD_COLORS[0], "🛒"),
        ("Fnac",       "FR-48271-9934",   CARD_COLORS[1], "📚"),
        ("Décathlon",  "DK 00291-A",      CARD_COLORS[2], "⚽"),
        ("IKEA",       "800 234 112 09",  CARD_COLORS[3], "🏠"),
        ("Sephora",    "SP-7712-884",     CARD_COLORS[4], "💄"),
        ("Cultura",    "CUL-29847",       CARD_COLORS[5], "🎨"),
    ]

    cols, gap = 2, 24
    cw = (W - 60*2 - gap) // 2
    ch = 220
    start_y = 230

    for i, (store, num, color, emoji) in enumerate(stores):
        col = i % cols
        row = i // cols
        x = 60 + col * (cw + gap)
        y = start_y + row * (ch + gap)
        card_chip(draw, x, y, cw, ch, color, store, num, emoji)

    fab(draw, W-100, H-220)
    nav_bar(img)
    img.save(f"{OUT}/01_card_list.png")
    print("✓ 01_card_list.png")

# ─────────────────────────────────────────
# SCREEN 2 — Détail carte (code-barres EAN-13)
# ─────────────────────────────────────────
def screen_card_detail():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    # back arrow + edit button
    draw.text((52, 108), "←", fill=ON_BG, font=load_font(56))
    draw.text((W-110, 108), "✏", fill=ON_BG, font=load_font(48))

    # Large card header
    rounded_rect(draw, [0, 180, W, 560], 40, CARD_COLORS[0])
    f_store = load_font(72, bold=True)
    f_num   = load_font(36)
    draw.text((80, 260), "🛒  Carrefour", fill="#FFFFFF", font=f_store)
    draw.text((80, 360), "724 819 003 421", fill="#FFFFFFBB", font=f_num)

    # Barcode section
    rounded_rect(draw, [60, 600, W-60, 1020], 24, "#FFFFFF")
    draw.rectangle([60, 600, W-60, 1020], fill=None)

    f_label = load_font(34)
    draw.text((W//2, 640), "Code-barres", fill=ON_BG_2, font=f_label, anchor="mm")

    # EAN-13 barcode
    barcode_lines(draw, 120, 690, W-240, 260)
    draw.text((W//2, 980), "724 819 003 421", fill=ON_BG, font=load_font(38), anchor="mm")

    # Checkout button
    rounded_rect(draw, [60, 1060, W-60, 1180], 40, PRIMARY)
    draw.text((W//2, 1120), "Afficher en caisse", fill=ON_PRIMARY,
              font=load_font(46, bold=True), anchor="mm")

    # Info section
    f_info = load_font(34)
    draw.text((80, 1230), "Format", fill=ON_BG_2, font=f_info)
    draw.text((80, 1278), "EAN-13", fill=ON_BG, font=load_font(38, bold=True))
    draw.text((80, 1360), "Ajouté le", fill=ON_BG_2, font=f_info)
    draw.text((80, 1408), "17 avril 2026", fill=ON_BG, font=load_font(38, bold=True))

    nav_bar(img)
    img.save(f"{OUT}/02_card_detail.png")
    print("✓ 02_card_detail.png")

# ─────────────────────────────────────────
# SCREEN 3 — Mode caisse (plein écran)
# ─────────────────────────────────────────
def screen_checkout():
    img = Image.new("RGB", (W, H), "#FFFFFF")
    draw = ImageDraw.Draw(img)

    # top bar
    draw.rectangle([0, 0, W, 90], fill=CARD_COLORS[0])
    status_bar(draw, dark=True)
    draw.text((W//2, 130), "Carrefour", fill="#FFFFFF",
              font=load_font(56, bold=True), anchor="mm")
    draw.text((W//2, 192), "Mode caisse · Luminosité max", fill="#FFFFFFAA",
              font=load_font(34), anchor="mm")

    # Large QR code
    qr_block(draw, W//2, H//2 - 60, 700)

    draw.text((W//2, H//2 + 420), "724 819 003 421", fill=ON_BG,
              font=load_font(52, bold=True), anchor="mm")

    # dismiss hint
    rounded_rect(draw, [W//2-200, H-220, W//2+200, H-140], 40, SURFACE_VAR)
    draw.text((W//2, H-180), "Appuyer pour fermer", fill=ON_BG_2,
              font=load_font(34), anchor="mm")

    nav_bar(img)
    img.save(f"{OUT}/03_checkout_mode.png")
    print("✓ 03_checkout_mode.png")

# ─────────────────────────────────────────
# SCREEN 4 — Ajout par scan
# ─────────────────────────────────────────
def screen_scan():
    img = Image.new("RGB", (W, H), BG_DARK)
    draw = ImageDraw.Draw(img)
    status_bar(draw, dark=True)

    # camera viewfinder
    vf_x, vf_y, vf_w, vf_h = 0, 90, W, 1400
    draw.rectangle([vf_x, vf_y, vf_x+vf_w, vf_y+vf_h], fill="#111111")

    # Scan frame overlay
    fx, fy, fw, fh = 140, 500, W-280, 600
    # corners
    clen, cthk = 60, 8
    c = "#FFFFFF"
    # TL
    draw.rectangle([fx, fy, fx+clen, fy+cthk], fill=c)
    draw.rectangle([fx, fy, fx+cthk, fy+clen], fill=c)
    # TR
    draw.rectangle([fx+fw-clen, fy, fx+fw, fy+cthk], fill=c)
    draw.rectangle([fx+fw-cthk, fy, fx+fw, fy+clen], fill=c)
    # BL
    draw.rectangle([fx, fy+fh-cthk, fx+clen, fy+fh], fill=c)
    draw.rectangle([fx, fy+fh-clen, fx+cthk, fy+fh], fill=c)
    # BR
    draw.rectangle([fx+fw-clen, fy+fh-cthk, fx+fw, fy+fh], fill=c)
    draw.rectangle([fx+fw-cthk, fy+fh-clen, fx+fw, fy+fh], fill=c)

    # scan line animation hint
    draw.rectangle([fx+10, fy+fh//2-3, fx+fw-10, fy+fh//2+3], fill=PRIMARY)

    draw.text((W//2, 380), "Pointez vers un code-barres", fill="#FFFFFF",
              font=load_font(42), anchor="mm")
    draw.text((W//2, 1200), "Détection automatique", fill="#FFFFFF99",
              font=load_font(34), anchor="mm")

    # bottom panel
    rounded_rect(draw, [0, 1400, W, H], 40, BG)
    draw.text((W//2, 1500), "Code non lisible ?", fill=ON_BG,
              font=load_font(42, bold=True), anchor="mm")
    rounded_rect(draw, [100, 1560, W-100, 1680], 40, PRIMARY_CTR)
    draw.text((W//2, 1620), "Saisie manuelle", fill=PRIMARY,
              font=load_font(42, bold=True), anchor="mm")

    # back
    draw.text((52, 108), "←", fill="#FFFFFF", font=load_font(56))
    nav_bar(img)
    img.save(f"{OUT}/04_scan.png")
    print("✓ 04_scan.png")

# ─────────────────────────────────────────
# SCREEN 5 — Édition / création carte
# ─────────────────────────────────────────
def screen_card_edit():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)
    draw.text((52, 108), "←", fill=ON_BG, font=load_font(56))
    draw.text((W//2, 134), "Nouvelle carte", fill=ON_BG,
              font=load_font(52, bold=True), anchor="mm")

    # preview card
    card_chip(draw, 60, 190, W-120, 220, CARD_COLORS[3], "Ma carte", "1234 5678 9012", "🏠")

    def field(label, value, y, active=False):
        f_lbl = load_font(30)
        f_val = load_font(40)
        lbl_color = PRIMARY if active else ON_BG_2
        draw.text((80, y), label, fill=lbl_color, font=f_lbl)
        line_color = PRIMARY if active else OUTLINE
        draw.rectangle([80, y+100, W-80, y+104], fill=line_color)
        draw.text((80, y+42), value, fill=ON_BG, font=f_val)

    field("Nom de l'enseigne", "IKEA", 460, active=True)
    field("Numéro de carte", "800 234 112 09", 620)

    # format selector
    f_lbl = load_font(30)
    draw.text((80, 790), "Format du code-barres", fill=ON_BG_2, font=f_lbl)
    formats = ["EAN-13", "QR Code", "Code 128", "EAN-8"]
    fx = 80
    for i, fmt in enumerate(formats):
        fw = len(fmt)*22 + 48
        color = PRIMARY if fmt == "EAN-13" else SURFACE_VAR
        fg = ON_PRIMARY if fmt == "EAN-13" else ON_BG
        rounded_rect(draw, [fx, 840, fx+fw, 920], 40, color)
        draw.text((fx+fw//2, 880), fmt, fill=fg, font=load_font(32, bold=(fmt=="EAN-13")), anchor="mm")
        fx += fw + 16

    # color picker
    draw.text((80, 960), "Couleur", fill=ON_BG_2, font=f_lbl)
    for i, col in enumerate(CARD_COLORS):
        cx = 80 + i*(80+12)
        draw.ellipse([cx, 1010, cx+72, 1082], fill=col)
        if i == 3:
            draw.ellipse([cx-6, 1004, cx+78, 1088], outline=PRIMARY, width=4, fill=None)

    # save button
    rounded_rect(draw, [60, 1740, W-60, 1860], 40, PRIMARY)
    draw.text((W//2, 1800), "Enregistrer", fill=ON_PRIMARY,
              font=load_font(50, bold=True), anchor="mm")

    nav_bar(img)
    img.save(f"{OUT}/05_card_edit.png")
    print("✓ 05_card_edit.png")

# ─────────────────────────────────────────
# SCREEN 6 — Liste scrollée + réorganisation
# ─────────────────────────────────────────
def screen_reorder():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)
    top_app_bar(draw, "Fidelya", subtitle="6 cartes")

    stores = [
        ("Carrefour",  "724 819 003 421", CARD_COLORS[0], "🛒"),
        ("Fnac",       "FR-48271-9934",   CARD_COLORS[1], "📚"),
        ("Décathlon",  "DK 00291-A",      CARD_COLORS[2], "⚽"),
        ("IKEA",       "800 234 112 09",  CARD_COLORS[3], "🏠"),
        ("Sephora",    "SP-7712-884",     CARD_COLORS[4], "💄"),
        ("Cultura",    "CUL-29847",       CARD_COLORS[5], "🎨"),
    ]

    cols, gap = 2, 24
    cw = (W - 60*2 - gap) // 2
    ch = 220
    start_y = 260

    for i, (store, num, color, emoji) in enumerate(stores):
        col = i % cols
        row = i // cols
        x = 60 + col * (cw + gap)
        y = start_y + row * (ch + gap)

        # card being dragged (index 1 = Fnac)
        if i == 1:
            # shadow effect
            rounded_rect(draw, [x-8, y+8, x+cw+8, y+ch+8], 28, "#00000033")
            rounded_rect(draw, [x-4, y-4, x+cw+4, y+ch+4], 26, color)
            card_chip(draw, x-4, y-4, cw+8, ch+8, color, store, num, emoji)
            # drag hint
            draw.text((x+cw-50, y+12), "⠿", fill="#FFFFFF99", font=load_font(42))
        else:
            card_chip(draw, x, y, cw, ch, color, store, num, emoji)

    # tooltip
    rounded_rect(draw, [W//2-300, H-280, W//2+300, H-180], 20, BG_DARK+"CC")
    draw.text((W//2, H-230), "Maintenez pour réorganiser", fill="#FFFFFF",
              font=load_font(34), anchor="mm")

    fab(draw, W-100, H-340)
    nav_bar(img)
    img.save(f"{OUT}/06_reorder.png")
    print("✓ 06_reorder.png")


if __name__ == "__main__":
    print("Generating Fidelya Play Store screenshots...")
    screen_card_list()
    screen_card_detail()
    screen_checkout()
    screen_scan()
    screen_card_edit()
    screen_reorder()
    print(f"\nDone → {OUT}/ (6 screenshots, 1080×1920px)")
