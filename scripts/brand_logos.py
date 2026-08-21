#!/usr/bin/env python3
"""Récupère les logos des enseignes et génère le catalogue Kotlin.

Deux sorties, à regénérer ensemble :

  * `app/src/main/res/drawable-nodpi/brand_<slug>.webp` — le logo, normalisé en
    carré de 512 px sur fond transparent.
  * `app/src/main/java/com/beninho/fidelya/data/brand/BrandCatalog.kt` — la
    liste des enseignes, avec la référence de drawable et la couleur dominante
    du logo.

Sources, dans l'ordre :

  1. Wikidata `P154` (« logo image ») rendu par Wikimedia Commons en 512 px de
     large. C'est la meilleure source : le fichier est presque toujours un SVG,
     donc net à n'importe quelle taille, et sur fond transparent.
  2. À défaut, la plus grande icône déclarée par le site de l'enseigne — son
     `apple-touch-icon` fait souvent 180 px ou plus.
  3. À défaut encore, la favicon via le service Google. Souvent petite, mais
     c'est mieux que rien — la carte retomberait sinon sur son initiale.

L'entité Wikidata est trouvée par recherche sur le nom de l'enseigne, et son
libellé est vérifié avant qu'on lise son logo : une recherche sur « Go Sport »
remonte aussi une araignée de mer. Un Q-id peut être figé dans la table quand la
recherche ne trouve pas la bonne entité.

    python3 scripts/brand_logos.py            # tout
    python3 scripts/brand_logos.py carrefour  # une enseigne, pour vérifier
"""

from __future__ import annotations

import io
import json
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
DRAWABLE_DIR = ROOT / "app/src/main/res/drawable-nodpi"
CATALOG = ROOT / "app/src/main/java/com/beninho/fidelya/data/brand/BrandCatalog.kt"

USER_AGENT = "FidelyaBrandLogos/1.0 (https://github.com/Beninho/fidelya)"
SIDE = 512

# Pas de marge : `CardLogo` en pose déjà une de 12 % autour d'un logo
# transparent. Deux marges cumulées serreraient le logo au centre du carré.
PADDING = 0.0


@dataclass(frozen=True)
class Brand:
    slug: str
    name: str
    program: str
    sector: str
    domain: str
    # Q-id Wikidata, quand la recherche par libellé ne trouve pas la bonne
    # entité — parce qu'elle est enregistrée sous une autre raison sociale.
    # `None` = on cherche par le nom.
    qid: str | None = None
    # URL de logo choisie à la main, quand ni Wikidata ni la favicon ne donnent
    # quelque chose de reconnaissable. Priorité sur les deux.
    logo_url: str | None = None


# Les enseignes de l'issue #5, dans son ordre — par secteur, puis par notoriété.
BRANDS: list[Brand] = [
    # Grande distribution & supermarchés
    Brand("carrefour", "Carrefour", "Carrefour Pass", "Grande distribution", "carrefour.fr"),
    # Enregistrée sous « E.Leclerc » : la recherche sur « Leclerc » ne la trouve pas.
    Brand("leclerc", "Leclerc", "Carte Leclerc / Moi+", "Grande distribution", "e.leclerc", "Q1273376"),
    Brand("auchan", "Auchan", "Carte Auchan / My Auchan", "Grande distribution", "auchan.fr"),
    Brand("intermarche", "Intermarché", "Carte Intermarché", "Grande distribution", "intermarche.com"),
    Brand("casino", "Casino", "Club Casino", "Grande distribution", "casino.fr"),
    Brand("monoprix", "Monoprix", "Carte Monoprix", "Grande distribution", "monoprix.fr"),
    Brand("franprix", "Franprix", "Carte Franprix", "Grande distribution", "franprix.fr"),
    Brand("lidl", "Lidl", "Lidl Plus", "Grande distribution", "lidl.fr"),
    Brand("aldi", "Aldi", "Offres via l'appli", "Grande distribution", "aldi.fr"),
    # Mode & habillement
    Brand("kiabi", "Kiabi", "Carte Kiabi", "Mode", "kiabi.com"),
    Brand("uniqlo", "Uniqlo", "Carte Uniqlo", "Mode", "uniqlo.com"),
    Brand("zara", "Zara", "Zara Club", "Mode", "zara.com"),
    Brand("hm", "H&M", "H&M Club", "Mode", "hm.com"),
    Brand("lacoste", "Lacoste", "Carte Lacoste", "Mode", "lacoste.com"),
    Brand("etam", "Etam", "Carte Etam", "Mode", "etam.com"),
    Brand("promod", "Promod", "Carte Promod", "Mode", "promod.fr"),
    Brand("citadium", "Citadium", "Carte Citadium", "Mode", "citadium.com"),
    Brand("decathlon", "Decathlon", "Carte Decathlon", "Mode", "decathlon.fr"),
    # Beauté & parfumerie
    Brand("sephora", "Sephora", "Carte Sephora", "Beauté", "sephora.fr"),
    Brand("marionnaud", "Marionnaud", "Carte Marionnaud", "Beauté", "marionnaud.fr"),
    Brand("nocibe", "Nocibé", "Carte Nocibé", "Beauté", "nocibe.fr"),
    Brand("douglas", "Douglas", "Carte Douglas", "Beauté", "douglas.de"),
    Brand("yves-rocher", "Yves Rocher", "Carte Yves Rocher", "Beauté", "yves-rocher.fr"),
    Brand("body-shop", "The Body Shop", "Love Your Body Club", "Beauté", "thebodyshop.com"),
    Brand("occitane", "L'Occitane", "Carte L'Occitane", "Beauté", "loccitane.com"),
    # Culture & loisirs
    Brand("fnac", "Fnac", "Carte Fnac", "Culture & loisirs", "fnac.com"),
    Brand("cultura", "Cultura", "Carte Cultura", "Culture & loisirs", "cultura.com"),
    Brand("micromania", "Micromania", "Carte Micromania", "Culture & loisirs", "micromania.fr"),
    Brand("darty", "Darty", "Carte Darty", "Culture & loisirs", "darty.com"),
    Brand("boulanger", "Boulanger", "Carte Boulanger", "Culture & loisirs", "boulanger.com"),
    Brand("go-sport", "Go Sport", "Carte Go Sport", "Culture & loisirs", "go-sport.com"),
    # Maison & décoration
    Brand("ikea", "IKEA", "IKEA Family", "Maison", "ikea.com"),
    Brand("conforama", "Conforama", "Carte Conforama", "Maison", "conforama.fr"),
    # Wikidata ne connaît que l'université Butler, et la favicon fait 16 px.
    Brand(
        "but", "But", "Carte But", "Maison", "but.fr",
        logo_url="https://commons.wikimedia.org/wiki/Special:FilePath/Logo-BUT.svg?width=512"
    ),
    Brand("fly", "Fly", "Carte Fly", "Maison", "fly.fr"),
    Brand("maisons-du-monde", "Maisons du Monde", "Carte Maisons du Monde", "Maison", "maisonsdumonde.com"),
    Brand("leroy-merlin", "Leroy Merlin", "Carte Leroy Merlin", "Maison", "leroymerlin.fr"),
    Brand("castorama", "Castorama", "Carte Castorama", "Maison", "castorama.fr"),
    Brand("truffaut", "Truffaut", "Carte Truffaut", "Maison", "truffaut.com"),
    # Restauration & alimentation spécialisée
    Brand("starbucks", "Starbucks", "Starbucks Rewards", "Restauration", "starbucks.fr"),
    Brand("mcdonalds", "McDonald's", "McDonald's App", "Restauration", "mcdonalds.fr"),
    Brand("paul", "Paul", "Carte Paul", "Restauration", "paul.fr"),
    Brand("subway", "Subway", "Subway Club", "Restauration", "subway.fr"),
    Brand("mie-caline", "La Mie Câline", "Carte La Mie Câline", "Restauration", "lamiecaline.com"),
    # « Picard Surgelés » n'a pas de P154 ; la favicon n'est qu'un flocon.
    Brand(
        "picard", "Picard", "Carte Picard", "Restauration", "picard.fr",
        logo_url="https://commons.wikimedia.org/wiki/Special:FilePath/"
        "Picard%20Surgel%C3%A9s%20Logo26.png?width=512"
    ),
    Brand("grand-frais", "Grand Frais", "Carte Grand Frais", "Restauration", "grandfrais.com"),
    # Automobile & transport
    Brand("totalenergies", "TotalEnergies", "Carte TotalEnergies Club", "Transport", "totalenergies.fr"),
    Brand("shell", "Shell", "Shell ClubSmart", "Transport", "shell.fr"),
    Brand("norauto", "Norauto", "Carte Norauto", "Transport", "norauto.fr"),
    Brand("feu-vert", "Feu Vert", "Carte Feu Vert", "Transport", "feuvert.fr"),
    Brand("sncf", "SNCF", "Carte Avantage", "Transport", "sncf.com"),
    # Santé & pharmacies
    Brand("optic-2000", "Optic 2000", "Carte Optic 2000", "Santé", "optic2000.com"),
    Brand("grandvision", "GrandVision", "Carte GrandVision", "Santé", "grandvision.com"),
    Brand("audika", "Audika", "Carte Audika", "Santé", "audika.fr"),
    # Divers
    Brand("accor", "Accor", "ALL — Accor Live Limitless", "Divers", "all.accor.com"),
    Brand("sncf-connect", "SNCF Connect", "Carte Avantage", "Divers", "sncf-connect.com"),
    Brand("air-france", "Air France", "Flying Blue", "Divers", "airfrance.fr"),
    Brand("amazon", "Amazon", "Amazon Prime", "Divers", "amazon.fr"),
    Brand("cdiscount", "Cdiscount", "Carte Cdiscount", "Divers", "cdiscount.com"),
    # Enregistrée sous « Showroomprive.com ».
    Brand("showroomprive", "Showroomprivé", "Carte Showroomprivé", "Divers", "showroomprive.com", "Q27791996"),
    Brand("veepee", "Veepee", "Carte Veepee", "Divers", "veepee.fr"),
]


def get(url: str, timeout: int = 30) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.read()


def wikidata_logo_url(brand: Brand) -> str | None:
    """L'URL du logo Wikidata de l'enseigne, rendu en 512 px de large.

    Le Q-id de la table n'est pas cru sur parole : un identifiant recopié de
    travers désigne une autre entité, et on embarquerait son logo sans que rien
    ne le signale — l'araignée de mer `Q3110428` pour « Go Sport ». Le libellé
    doit donc correspondre au nom de l'enseigne, sinon on repart de la
    recherche.
    """
    pinned = [brand.qid] if brand.qid else []
    found = [q for q in search_qids(brand.name) if q not in pinned]

    for qid in pinned + found:
        entity = entity_data(qid)
        if entity is None:
            continue
        # Le Q-id de la table a été vérifié à la main : on le suit même si
        # l'entité est enregistrée sous une raison sociale (« Showroomprive.com »).
        if qid not in pinned and not labels_match(brand.name, entity):
            continue
        for claim in entity.get("claims", {}).get("P154", []):
            filename = claim.get("mainsnak", {}).get("datavalue", {}).get("value")
            if not filename:
                continue
            quoted = urllib.parse.quote(filename.replace(" ", "_"))
            return f"https://commons.wikimedia.org/wiki/Special:FilePath/{quoted}?width={SIDE}"
    return None


def entity_data(qid: str) -> dict | None:
    api = (
        "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json"
        f"&props=labels|aliases|claims&languages=fr|en&ids={qid}"
    )
    try:
        return json.loads(get(api))["entities"][qid]
    except Exception:
        return None


def labels_match(name: str, entity: dict) -> bool:
    """Vrai si l'entité porte bien le nom de l'enseigne, alias compris."""
    wanted = simplify(name)
    values = [label["value"] for label in entity.get("labels", {}).values()]
    values += [
        alias["value"]
        for aliases in entity.get("aliases", {}).values()
        for alias in aliases
    ]
    return any(simplify(value) == wanted for value in values)


def simplify(value: str) -> str:
    """Le nom réduit à ses lettres et chiffres, sans accents ni casse."""
    stripped = unicodedata.normalize("NFKD", value)
    ascii_only = "".join(c for c in stripped if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]", "", ascii_only.lower())


def search_qids(name: str, limit: int = 5) -> list[str]:
    api = (
        "https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json"
        f"&language=fr&uselang=fr&limit={limit}&search={urllib.parse.quote(name)}"
    )
    try:
        return [hit["id"] for hit in json.loads(get(api)).get("search", [])]
    except Exception:
        return []


def site_icon_url(brand: Brand) -> str | None:
    """La plus grande icône déclarée par le site de l'enseigne.

    Le service de favicons renvoie parfois un 16 px illisible là où le site
    déclare une `apple-touch-icon` de 180 px ou plus. On lit donc l'en-tête de
    la page d'accueil, et on garde la plus grande icône annoncée.
    """
    home = f"https://www.{brand.domain}/" if "." in brand.domain and brand.domain.count(".") == 1 \
        else f"https://{brand.domain}/"
    try:
        html = get(home, timeout=15).decode("utf-8", "replace")
    except Exception:
        return None

    best: tuple[int, str] | None = None
    for tag in re.findall(r"<link\b[^>]*>", html[:200_000], re.IGNORECASE):
        if not re.search(r'rel=["\'][^"\']*icon', tag, re.IGNORECASE):
            continue
        href = re.search(r'href=["\']([^"\']+)', tag, re.IGNORECASE)
        if not href or href.group(1).endswith(".svg"):
            continue  # Pillow ne lit pas le SVG
        sizes = re.search(r'sizes=["\'](\d+)', tag, re.IGNORECASE)
        side = int(sizes.group(1)) if sizes else 0
        if "apple-touch" in tag.lower():
            side = max(side, 180)  # la taille par défaut d'une apple-touch-icon
        if best is None or side > best[0]:
            best = (side, urllib.parse.urljoin(home, href.group(1)))
    return best[1] if best and best[0] >= 128 else None


def favicon_url(brand: Brand) -> str:
    return f"https://www.google.com/s2/favicons?domain={brand.domain}&sz=256"


def normalize(raw: bytes) -> Image.Image:
    """Détoure le logo et le pose au centre d'un carré transparent de 512 px.

    Le carré est ce que `CardLogo` attend : il cadre en `Crop` une image opaque
    et en `Fit` une image transparente, et un logo large sorti d'un SVG est
    l'un ou l'autre selon la marque.
    """
    image = Image.open(io.BytesIO(raw)).convert("RGBA")

    # Beaucoup de logos arrivent avec une large bande transparente autour ;
    # sans détourage ils rendraient minuscules dans la vignette.
    bbox = image.getchannel("A").getbbox()
    if bbox:
        image = image.crop(bbox)

    inner = round(SIDE * (1 - 2 * PADDING))
    # Jamais d'agrandissement : une favicon de 32 px repassée en 512 ne gagne
    # aucun détail et ne pèse que plus lourd. Le carré suit alors la source, et
    # c'est Android qui l'étirera à l'affichage.
    ratio = min(inner / image.width, inner / image.height, 1.0)
    side = SIDE if ratio < 1.0 else max(image.width, image.height)
    resized = image.resize(
        (max(1, round(image.width * ratio)), max(1, round(image.height * ratio))),
        Image.LANCZOS,
    )

    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(
        resized,
        ((side - resized.width) // 2, (side - resized.height) // 2),
        resized,
    )
    return square


def dominant_color(image: Image.Image) -> str:
    """La couleur du logo, pour préremplir le fond de la carte.

    Moyenne des pixels opaques, en écartant les quasi-blancs et quasi-noirs :
    un logo est souvent une forme colorée sur une plaque blanche, et c'est la
    forme qui identifie la marque. `nearestModernistColor` ramènera ensuite
    cette couleur sur un pas de la palette.
    """
    pixels = [p for p in image.getdata() if p[3] > 200]
    vivid = [
        p for p in pixels
        if not (p[0] > 235 and p[1] > 235 and p[2] > 235)
        and not (p[0] < 25 and p[1] < 25 and p[2] < 25)
    ]
    sample = vivid or pixels
    if not sample:
        return "#605D5D"
    r = sum(p[0] for p in sample) // len(sample)
    g = sum(p[1] for p in sample) // len(sample)
    b = sum(p[2] for p in sample) // len(sample)
    return f"#{r:02X}{g:02X}{b:02X}"


def fetch(brand: Brand) -> tuple[str, str]:
    """Écrit le drawable de l'enseigne. Renvoie (source, couleur dominante)."""
    attempts: list[tuple[str, str | None]] = [
        ("manuel", brand.logo_url),
        ("wikidata", wikidata_logo_url(brand)),
        ("site", site_icon_url(brand)),
        ("favicon", favicon_url(brand)),
    ]
    for source, url in attempts:
        if not url:
            continue
        try:
            image = normalize(get(url))
        except Exception as error:  # source indisponible ou fichier illisible
            print(f"    {source} : {error}")
            continue
        DRAWABLE_DIR.mkdir(parents=True, exist_ok=True)
        image.save(DRAWABLE_DIR / f"brand_{brand.slug.replace('-', '_')}.webp", "WEBP", lossless=True)
        return source, dominant_color(image)
    raise RuntimeError(f"aucun logo pour {brand.name}")


KOTLIN_HEADER = '''package com.beninho.fidelya.data.brand

import androidx.annotation.DrawableRes
import com.beninho.fidelya.R

/**
 * Les enseignes dont l'application embarque déjà le logo.
 *
 * Fichier généré par `scripts/brand_logos.py` — les logos et leurs couleurs
 * sont extraits des sources décrites là-bas. Ne pas éditer à la main : ajouter
 * l'enseigne à la table du script et le relancer, sinon le catalogue et les
 * drawables se désynchronisent. Voir `docs/brand-logos.md`.
 *
 * Les logos restent la propriété de leurs titulaires ; ils ne servent ici qu'à
 * désigner l'enseigne de la carte que l'utilisateur possède.
 */
data class Brand(
    val name: String,
    /** Le nom du programme de fidélité, en second de la suggestion. */
    val program: String,
    val sector: String,
    @DrawableRes val logo: Int,
    /**
     * La couleur dominante du logo, telle quelle. C'est
     * `nearestModernistColor` qui la ramène sur un pas de la palette au moment
     * de préremplir le fond de la carte.
     */
    val color: String
)

/** Les enseignes, par secteur puis par notoriété — l'ordre de l'issue #5. */
val BRAND_CATALOG: List<Brand> = listOf(
'''


def generate_catalog(rows: list[tuple[Brand, str]]) -> None:
    lines = [KOTLIN_HEADER]
    for brand, color in rows:
        drawable = f"brand_{brand.slug.replace('-', '_')}"
        lines.append(
            f'    Brand("{escape(brand.name)}", "{escape(brand.program)}", '
            f'"{escape(brand.sector)}", R.drawable.{drawable}, "{color}"),\n'
        )
    lines.append(")\n")
    CATALOG.parent.mkdir(parents=True, exist_ok=True)
    CATALOG.write_text("".join(lines), encoding="utf-8")
    print(f"\n{CATALOG.relative_to(ROOT)} : {len(rows)} enseignes")


def escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")


def main() -> int:
    only = set(sys.argv[1:])
    selected = [b for b in BRANDS if not only or b.slug in only]
    if not selected:
        print(f"aucune enseigne pour {sorted(only)}", file=sys.stderr)
        return 1

    rows: list[tuple[Brand, str]] = []
    failures: list[str] = []
    for brand in selected:
        print(f"{brand.name} ({brand.slug})")
        try:
            source, color = fetch(brand)
        except RuntimeError as error:
            print(f"    ÉCHEC : {error}")
            failures.append(brand.name)
            continue
        print(f"    {source} → {color}")
        rows.append((brand, color))
        time.sleep(0.2)  # les API Wikimedia n'aiment pas les rafales

    # Le catalogue n'est réécrit qu'en passe complète : une passe partielle
    # n'aurait que les enseignes demandées et perdrait les autres.
    if not only and rows:
        generate_catalog(rows)

    if failures:
        print(f"\nsans logo : {', '.join(failures)}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
