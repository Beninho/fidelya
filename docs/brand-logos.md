---
title: Logos d'enseignes
---

# Logos d'enseignes

L'application embarque le logo de 65 enseignes françaises. Dans le formulaire
d'une carte, la saisie du nom du magasin propose les enseignes qui
correspondent ; choisir l'une d'elles remplit le nom, attache son logo et pose
la couleur de la carte. C'est ce que demandait
[l'issue #5](https://github.com/Beninho/fidelya/issues/5).

Rien ne sort du téléphone : le logo est un fichier embarqué dans l'APK, recopié
dans le stockage interne comme n'importe quelle image choisie par
l'utilisateur.

## Ce qui compose la fonctionnalité

| Fichier | Rôle |
| --- | --- |
| `scripts/brand_logos.py` | Récupère les logos et **génère** le catalogue Kotlin. |
| `app/src/main/res/drawable-nodpi/brand_*.webp` | Les logos, en WebP sans perte, carrés, fond transparent. |
| `app/src/main/java/com/beninho/fidelya/data/brand/BrandCatalog.kt` | **Généré.** Nom, programme, drawable et couleur de chaque enseigne. |
| `app/src/main/java/com/beninho/fidelya/data/brand/BrandSearch.kt` | La recherche derrière les suggestions. Écrit à la main. |

`BrandCatalog.kt` ne s'édite pas à la main : ajouter l'enseigne à la table
`BRANDS` du script et relancer celui-ci, sinon le catalogue et les drawables se
désynchronisent.

## Régénérer les logos

```bash
python3 scripts/brand_logos.py            # toutes les enseignes, réécrit le catalogue
python3 scripts/brand_logos.py carrefour  # une seule, pour vérifier une source
```

La passe complète prend deux à trois minutes — un appel réseau par enseigne — et
réécrit `BrandCatalog.kt`. Une passe partielle ne réécrit que les drawables
demandés : elle sert à éprouver une source, pas à mettre le catalogue à jour.

Sources, dans l'ordre d'essai :

1. **URL fixée à la main** (`logo_url` dans la table), quand les deux suivantes
   ne donnent rien de reconnaissable.
2. **Wikidata `P154`**, rendu par Wikimedia Commons à 512 px de large. Presque
   toujours un SVG, donc net. L'entité est trouvée par recherche sur le nom, et
   **son libellé est vérifié** avant qu'on lise son logo : une recherche sur
   « Go Sport » remonte aussi une araignée de mer. Un `qid` figé dans la table
   court-circuite la recherche pour les enseignes enregistrées sous une autre
   raison sociale (`Showroomprive.com`, `E.Leclerc`).
3. **La plus grande icône déclarée par le site** de l'enseigne — son
   `apple-touch-icon` fait souvent 180 px ou plus.
4. **La favicon** via le service Google.

Six enseignes n'ont pour source qu'une favicon de 32 à 48 px (The Body Shop,
Citadium, Cultura, GrandVision, Optic 2000, SNCF) : leur logo est un peu mou à
l'affichage. Le script n'agrandit jamais une image — un 32 px repassé en 512 ne
gagne aucun détail — et l'utilisateur peut toujours choisir sa propre photo.

**Après une régénération, regarder les logos.** Une mauvaise entité Wikidata
passe la vérification de libellé si les noms coïncident, et rien d'autre ne le
signalera. Un montage suffit :

```bash
python3 - <<'EOF'
from PIL import Image
from pathlib import Path
files = sorted(Path("app/src/main/res/drawable-nodpi").glob("brand_*.webp"))
cols, cell = 7, 150
sheet = Image.new("RGB", (cols * cell, ((len(files) + cols - 1) // cols) * cell), "white")
for i, f in enumerate(files):
    im = Image.open(f).convert("RGBA")
    im.thumbnail((cell - 12, cell - 12))
    tile = Image.new("RGBA", (cell - 12, cell - 12), (235, 235, 235, 255))
    tile.alpha_composite(im, ((cell - 12 - im.width) // 2, (cell - 12 - im.height) // 2))
    sheet.paste(tile.convert("RGB"), ((i % cols) * cell + 6, (i // cols) * cell + 6))
sheet.save("/tmp/brand-logos.png")
EOF
```

## Ajouter une enseigne

1. Ajouter une ligne à `BRANDS` dans `scripts/brand_logos.py` : slug, nom,
   programme de fidélité, secteur, domaine du site. Le secteur ne sert qu'à
   ranger la table : il ne part pas dans le catalogue Kotlin.
2. `python3 scripts/brand_logos.py <slug>` et regarder le drawable obtenu.
3. S'il est mauvais ou absent : fixer `qid=` (le Q-id Wikidata de l'enseigne) ou
   `logo_url=` (une URL de logo directe).
4. `python3 scripts/brand_logos.py` pour réécrire le catalogue.
5. `./gradlew :app:testDebugUnitTest` — `BrandCatalogTest` vérifie l'unicité des
   noms et des drawables, et que chaque couleur retombe sur la palette.

## Poids

Les 65 logos pèsent environ 1,0 Mo dans l'APK. Ils sont en `drawable-nodpi` :
une seule densité, pas de déclinaison mdpi/xxxhdpi qui multiplierait le poids
pour des vignettes de 64 dp.

## Marques

Les logos restent la propriété de leurs titulaires. Ils ne servent ici qu'à
désigner l'enseigne de la carte que l'utilisateur possède déjà — usage nominatif,
sans affiliation ni parrainage. Une enseigne qui demande le retrait de son logo
se traite en retirant sa ligne de `BRANDS` et son drawable, puis en régénérant le
catalogue.
