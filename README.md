# Fidelya

Application Android de gestion de cartes de fidélité. Une app sur laquelle on scanne ou saisit les cartes de fidélité des magasins afin de les centraliser et de les rendre disponibles en un seul geste à la caisse.

## Fonctionnalités

- **Ajout par scan** — utilise la caméra et ML Kit pour détecter automatiquement le code-barres et son format
- **Saisie manuelle** — entrez le numéro et choisissez le format parmi QR Code, EAN-13, EAN-8, Code 128, Code 39, PDF 417, Data Matrix
- **Affichage plein écran** — la luminosité monte automatiquement lors de l'affichage pour faciliter la lecture en caisse
- **Logos d'enseignes** — le logo de 65 enseignes françaises est embarqué : la saisie du nom du magasin les propose, et l'enseigne choisie remplit le nom, le logo et la couleur
- **Personnalisation** — couleur de fond parmi les 22 pas des rampes du design system, emoji ou initiale en icône
- **Réorganisation** — maintenez appuyé sur une carte pour la déplacer par glisser-déposer, l'ordre est persistant
- **Sauvegarde** — exportez et importez toutes vos cartes au format JSON
- **À propos** — écran de présentation : ce que l'app fait de vos données, le lien de soutien [Ko-fi](https://ko-fi.com/benlet), le dépôt, la licence de la police

## Stack technique

| Couche | Technologies |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Design system | « Modernist » (Claude Design), Archivo embarquée (police variable) |
| Architecture | MVVM, Repository pattern |
| Base de données | Room |
| Persistance ordre | DataStore Preferences |
| Scan | CameraX, ML Kit Barcode Scanning |
| Génération codes-barres | ZXing Core |
| Glisser-déposer | `sh.calvin.reorderable` |
| Sérialisation | kotlinx.serialization |

## Prérequis

- Android 8.0+ (API 26)
- Android Studio Hedgehog ou plus récent

## Build & lancement

```bash
./gradlew assembleDebug
# ou directement depuis Android Studio : Run > Run 'app'
```

Le build release active R8 (minification + shrinking des ressources) pour un APK optimisé.

## Tests

### Tests unitaires (JVM)

```bash
./gradlew test
```

Couvre : `CardRepositoryImpl`, `CardListViewModel`, `CardEditViewModel`, `CardDetailViewModel`,
`SupportedBarcodeFormats`, `ModernistPalette` (remappage des couleurs), et le décodage de vraies photos de cartes
(`app/src/test/resources/barcodes/`, décodées via ZXing — ML Kit ne tourne pas sur JVM).

### Tests instrumentés (appareil/émulateur)

```bash
scripts/export-room-schemas.sh   # une fois, si app/schemas/ est vide
scripts/prepare-emulator.sh      # une fois par boot d'émulateur
./gradlew connectedAndroidTest
```

Couvre : `CardListScreen`, `CardEditScreen` (Compose UI tests) et
`CardColorMigrationTest` (migration Room v1 → v2).

Nécessite un émulateur démarré ou un device connecté en USB debug.

`scripts/prepare-emulator.sh` déverrouille l'écran et coupe les animations. Sans ça, tous les tests
utilisant `createComposeRule()` échouent avec `No compose hierarchies found in the app` : l'écran de
verrouillage empêche l'activité de test d'atteindre le premier plan. Rien de tout ça ne survit à un
cold boot.

Les noms de méthodes de test instrumentés doivent rester en camelCase (pas de backticks avec
espaces) : `minSdk = 26` implique DEX < 040, qui interdit les espaces dans les noms de méthode.

### Variante spécifique

```bash
# Tests unitaires d'un seul module
./gradlew :app:testDebugUnitTest

# Tests instrumentés debug uniquement
./gradlew :app:connectedDebugAndroidTest
```

## Structure du projet

```
app/src/main/java/com/beninho/fidelya/
├── data/
│   ├── db/          # Room — entité, DAO, AppDatabase
│   ├── order/       # CardOrderStore (DataStore)
│   └── repository/  # CardRepository + implémentation
├── domain/model/    # LoyaltyCard (modèle métier)
├── barcode/         # SupportedBarcodeFormats (registre des formats)
├── ui/
│   ├── cardlist/    # Liste avec drag-and-drop
│   ├── carddetail/  # Affichage du code-barres
│   ├── cardedit/    # Formulaire de création/édition
│   ├── scan/        # Écran de scan caméra
│   └── theme/       # Couleurs, typo, formes, palette des cartes
├── backup/          # Export / import JSON
├── FidelyaApp.kt    # Application (DI manuelle)
└── MainActivity.kt  # Navigation Compose
```

## Écrans

Portés depuis la maquette Claude Design « Fidelya — refonte » (`Fidelya-refonte.dc.html`).

| # | Écran | Route | Notes |
|---|---|---|---|
| 01 | Onboarding | `onboarding` | Plein accent. Une seule fois, via `AppSettings.onboardingSeen` |
| 02 | État vide | — | État de l'écran 03, pas une route |
| 03 | Liste des cartes | `cardList` | Liste dense, recherche permanente, appui long → caisse |
| 04 | Détail carte | `cardDetail/{id}` | Format / Ajoutée le / Dernier passage |
| 05 | Mode caisse | `cardDetail/{id}?checkout=true` | Barres masquées, orientation verrouillée, luminosité réglable |
| 06 | Scan caméra | `scan` | Un QR de partage Fidelya part vers l'écran 11, pas vers le formulaire |
| 07 | Nouvelle carte | `cardEdit/{id}` | Bloc « Logo et couleur » |
| 08 | Réorganisation | `reorder` | Flèches ↑ ↓, désactivées aux extrémités |
| 09 | Réglages | `settings` | Apparence, luminosité, export, import |
| 10 | Partage | `share/{id}` | QR + lien `fidelya://`, réémis à expiration |
| 11 | Recevoir une carte | `receive?payload=…` | Code illisible ou périmé → refus explicite |

L'appui long de l'écran 03 réaffecte le geste qui servait à glisser-réordonner dans
l'ancienne grille : c'est désormais le rôle de l'écran 08. La dépendance
`sh.calvin.reorderable` est donc retirée.

### Alerte de doublon

Deux cartes peuvent légitimement porter le même numéro (compte partagé, carte
remplacée), donc **aucun index unique en base** : le doublon est signalé, jamais
bloqué. Le contrôle est `CardRepository.findDuplicate(cardNumber, excludeId)`,
une égalité sur le seul `cardNumber` — le format est ignoré, un même code lu en
QR ou en EAN-13 reste la même carte.

L'alerte (`ModernistDialog`) propose *Enregistrer quand même*, *Voir la carte
existante* (route `cardDetail/{id}`), *Annuler*, et de quoi trancher le doublon
sur place :

| Action destructrice | Où | Effet |
|---|---|---|
| *Supprimer l'autre carte* | Partout | Supprime le doublon trouvé, logo compris. Si l'alerte venait d'un enregistrement, il reprend tout seul — le doublon n'existe plus |
| *Supprimer cette carte* | Édition seulement | Supprime la carte éditée et revient à la liste. En création il n'y a rien en base à supprimer : *Annuler* suffit à jeter la saisie |

Les deux suppressions sont immédiates, sans second dialogue : l'alerte est déjà
un point d'arrêt, en empiler un autre alourdirait le geste pour rien. Elles
portent leur filet en `error` (`ModernistDialogAction.destructive`), sinon elles
se liraient comme une navigation.

L'alerte se lève à deux moments, parce que les trois chemins d'écriture n'ont pas
le même point d'entrée :

| Chemin | Moment du contrôle |
|---|---|
| Scan (écran 06 → 07) | À l'ouverture du formulaire : le numéro est déjà connu, inutile d'attendre la saisie du nom |
| Saisie manuelle (écran 07) | À l'enregistrement — rien à contrôler avant que le numéro soit tapé |
| Carte reçue (écran 11) | Au clic sur « Ajouter à mes cartes » ; l'écran est déjà l'étape de confirmation |

`excludeId` porte le cas de l'édition : une carte modifiée ne doit pas se
signaler comme son propre doublon. La recherche vit **dans** la coroutine
d'enregistrement, pas avant, pour ne pas laisser de fenêtre entre le contrôle et
l'insert. Un numéro retouché après un « Enregistrer quand même » remet le
compteur à zéro (`duplicateAccepted = false`), sinon l'acceptation d'un doublon
couvrirait le suivant.

### Partage de carte

`domain/share/CardShare.kt`, en Kotlin pur donc testable sans appareil. La charge
utile voyage dans une URI `fidelya://card?d=<base64url(json)>` — le même contenu
sert au QR et au bouton « Copier le lien », et un lecteur de codes générique y
voit quelque chose d'exploitable.

Le code porte le nom, le numéro, le format, la couleur et l'instant d'émission.
**Pas le logo** : une image ne tient pas dans un QR lisible à trente centimètres.
La maquette se contredit là-dessus — l'écran 10 promet le logo, l'écran 11 liste
précisément ce que le code contient. C'est l'écran 11 qui fait foi.

`iat` donne les dix minutes de validité annoncées par l'écran 10. L'écran de
partage réémet un code frais à expiration plutôt que d'afficher un code mort ;
c'est le téléphone destinataire qui refuse un code périmé. `decode()` rend `null`
sur tout ce qui n'est pas un partage exploitable — autre schéma, base64 tronquée,
JSON invalide, version inconnue, nom ou numéro vide — pour qu'un QR lu au hasard
ne produise jamais une carte à moitié remplie.

Deux détails de la maquette ne sont pas repris : la ligne « Partagée par Camille
— Pixel 8 » de l'écran 11, parce que la charge utile ne transporte aucun
expéditeur et ne doit pas en transporter ; et la ligne « Partager une carte » de
l'écran 09, qui n'a pas de carte sur laquelle agir depuis les réglages — le
partage part du détail, comme le montre l'écran 04.

### Logos

Le sélecteur de photos système rend une URI non durable : la permission de
lecture tombe avec le processus. `LogoStore` recopie donc l'image dans
`filesDir/logos` et c'est ce chemin que porte `logoUri`. Conforme à la promesse
de l'écran 07 — « le logo n'est jamais téléversé ».

`Logos.resolve()` n'accepte qu'un fichier de ce dossier : un backup importé
depuis un autre téléphone porte des chemins qui n'ont aucun sens ici, et un
chemin arbitraire ne doit être ni lu ni supprimé. Hors dossier, on retombe sur
l'initiale de l'enseigne.

Les fichiers orphelins sont supprimés au remplacement d'un logo comme à la
suppression d'une carte.

#### Logos d'enseignes embarqués

65 enseignes françaises sont livrées avec leur logo, en WebP sans perte dans
`res/drawable-nodpi` (~1,0 Mo). Taper le nom du magasin propose celles qui
correspondent ; l'enseigne choisie remplit le nom, attache son logo et pose la
couleur de la carte — la couleur dominante du logo, ramenée sur un pas de la
palette par `nearestModernistColor`.

Le logo embarqué passe par `LogoStore.storeResource()`, donc par le même chemin
qu'une image choisie par l'utilisateur : partage, sauvegarde et suppression n'ont
pas à distinguer les deux.

`data/brand/BrandCatalog.kt` est **généré** par `scripts/brand_logos.py`, qui
récupère aussi les logos — Wikidata, puis l'icône du site, puis la favicon. La
recherche derrière les suggestions est dans `BrandSearch.kt` : nom en préfixe,
puis nom en sous-chaîne, puis mot du programme de fidélité (« Flying Blue »
trouve Air France). Le mode d'emploi, l'ajout d'une enseigne et la note sur les
marques sont dans [`docs/brand-logos.md`](docs/brand-logos.md).

## Design system

Le thème est importé du projet Claude Design **« Modernist »** — `styles.css` et
`theme.json` de ce projet font foi. Le port vit dans `ui/theme/` :

| Fichier | Contenu |
|---|---|
| `Color.kt` | Rôles de base + trois rampes tonales de 9 pas (neutre, accent, accent secondaire) |
| `Type.kt` | Archivo (400/600/800) embarquée, échelle Modernist mappée sur les styles Material 3 |
| `Shape.kt` | Rayons à 0 — Modernist n'arrondit rien |
| `Theme.kt` | Schémas clair et sombre assemblés à partir des rampes |
| `Modernist.kt` | Couche composants : `.hr`, `.input`, `.field > label`, `.btn-block`, échelles d'espacement / élévation / opacités |
| `CardPalette.kt` | Façade Compose de la grille de fonds + calcul du texte contrasté |

`cardForegroundColor()` choisit l'encre ou le fond de page selon la luminance,
comme le fait `.btn-primary` dans le CSS d'origine — jamais de blanc pur sur une
couleur.

### Couche composants

Les quatre premiers fichiers portent les *tokens* ; `Modernist.kt` porte les
*classes* de `styles.css` que Material 3 ne rend pas telles quelles :

| Classe CSS | Équivalent Compose | Ce que Material faisait à la place |
|---|---|---|
| `.hr`, bordure basse de `.nav` | `ModernistDivider()` | Rien sous les `TopAppBar` ; `HorizontalDivider` fait 1dp, `dividers: "strong"` en demande 2 |
| `.input` + `.field > label` | `ModernistTextField`, `ModernistSelectField` | `OutlinedTextField` : fond transparent et libellé flottant dans la bordure, un idiome que Modernist n'a pas |
| `.btn-block` | `ModernistBlockButton` | `Button` centre son libellé ; `buttonAlign: "left"` demande l'inverse |
| Modale | `ModernistDialog` | `AlertDialog` impose des angles arrondis, une élévation teintée et des actions alignées à droite — trois choses que Modernist refuse |
| `h6` | `ModernistSectionLabel` | Compose n'a pas de `text-transform` : les capitales sont explicites |
| `--space-*` | `ModernistSpace` | — |
| `--shadow-*` | `ModernistElevation` | Équivalence approximative : une ombre CSS porte un flou et une teinte que `Modifier.shadow` ne reproduit pas |
| `color-mix(text N%)` | `ModernistAlpha` | Opacités posées au jugé (0.7 / 0.8 / 0.85) |

`--color-divider` reste en **alpha** (`modernistDividerColor()`, l'encre à 40 %)
plutôt qu'aplati sur un pas de la rampe, pour fonctionner sur n'importe quel
fond. Le rôle `outline` du schéma Material, lui, doit être opaque : il vaut
Neutral500 (#9B9797), à 9 unités du #9F9D9D obtenu en aplatissant le token sur
`--color-bg` — Neutral600 en était à 61.

L'action principale (`ModernistBlockButton`) prend `primary`/`onPrimary` et non
le `primaryContainer` par défaut : `.btn-primary` est l'accent plein sur texte
`--color-bg`, pas un pêche pâle.

### Thème « Encre » (sombre)

Porté depuis la maquette `Fidelya-refonte-encre.dc.html` — « Thème 2 — Encre ».
Ce n'est plus une extrapolation maison : les valeurs sont spécifiées.

| Rôle | Clair | Encre |
|---|---|---|
| fond | `#F3F2F2` | `#201E1D` |
| surface | `#EAE9E9` | `#2B2928` |
| texte | `#201E1D` | `#F3F2F2` |
| accent | `#EC3013` (accent) | `#FF563C` (accent **500**) |
| encre sur accent | `#F3F2F2` | `#201E1D` |
| filet | texte à 40 % | texte à **28 %** |
| filet de ligne | texte à 16 % | texte à 14 % |
| texte atténué | neutre 700 | neutre 400 |

Les rôles s'inversent : le fond du thème Encre est exactement l'encre de texte du
thème clair. Ils sont nommés à part dans `Color.kt` (`ModernistInk*`) plutôt que
réutilisés en croix, pour qu'un appel n'ait pas l'air d'une erreur.

L'accent monte d'un cran « pour tenir le contraste sur foncé », et le calcul le
confirme : encre sur accent 500 donne **5.26:1**, du texte clair seulement
**2.83:1**. D'où `onPrimary = ModernistInkBg` en sombre.

Les deux thèmes n'ont pas les mêmes opacités de filet, ce qu'aucun rôle Material
ne peut porter : `LocalModernistInk` le dit aux composants, et
`modernistDividerColor()` / `modernistRowDividerColor()` en dérivent.

**Deux surfaces restent blanches dans les deux thèmes** — le mode caisse et les
cadres qui portent un code — « parce qu'un lecteur optique a besoin de barres
noires sur blanc ». Le texte posé dessus est donc encré explicitement, via
`ModernistCodeSurface` / `ModernistCodeInk`, et non par un rôle de thème.

**Trois écarts assumés.**

Le bouton « Enregistrer » vit dans le `bottomBar` du `Scaffold`, pas à la fin du
flux comme le suggère `.btn-block { margin-top: var(--space-2) }`. Le style est
intact — pleine largeur, libellé à gauche, accent plein — seule la position
change : avec neuf familles de teintes le formulaire dépasse la hauteur d'écran,
et inline l'action principale passait sous la ligne de flottaison. On cliquait
« Enregistrer » sans voir les erreurs de saisie, restées en haut. C'est
`CardEditScreenTest.saveWithEmptyNameShowsError` qui l'a détecté.

`theme.json` déclare `imageTreatment: "grayscale"`
(`.grayscale { filter: grayscale(1) contrast(1.08) }`). La seule cible est le
logo d'enseigne : `CardLogo` le rend à travers `modernistGrayscaleFilter()`,
qui porte cette matrice. Les autres images sont des codes-barres générés en
noir et blanc pur — que l'overlay caisse doit garder tels quels pour rester
lisibles au scanner.

Le design system Modernist est `band: "light"` et ne définit pas de thème
sombre ; celui de l'app vient de la maquette « Encre » (voir plus haut), pas du
design system. `outline` y vaut le neutre 700, l'équivalent opaque du filet de la
maquette — le texte à 28 % sur l'encre donne #5B5959, à 7 unités du neutre 700.

### Palette des cartes

Elle vit dans `domain/color/ModernistPalette.kt`, en Kotlin pur : la couche
`data` en a besoin pour migrer la base et ne peut pas dépendre de Compose.

Le thème Modernist est monochrome, mais un fond de carte n'est pas une couleur
de thème : c'est de la donnée utilisateur, dont le rôle est de distinguer les
cartes d'un coup d'œil. On garde donc la *construction* de Modernist et on
l'étend à huit familles de teintes. En OKLCH :

- **Luminosité** — l'échelle partagée des rampes Modernist, pas 300/500/700/900
  (L = 0.870 / 0.680 / 0.481 / 0.291). Les pas 100 et 200 sont écartés : au delà
  de L 0.93 le gamut sRGB ne laisse plus assez de chroma et toutes les teintes y
  seraient indistinctement blanches.
- **Teintes** — 31.5° (celle de l'accent Modernist), puis 70, 110, 148, 192,
  250, 295 et 340°.
- **Chroma** — constant par pas (0.090 / 0.170 / 0.130 / 0.075), écrêté au
  gamut. Modernist pousse sa rampe accent au chroma maximal, mais ce maximum
  varie du simple au quadruple selon la teinte : repris tel quel il donne un
  arc-en-ciel de néons disparates. À chroma constant les huit familles se lisent
  comme un seul jeu — et le pas 300 du rouge retombe exactement sur le `#FFC4B8`
  de la rampe d'origine, ce qui vérifie la construction.

La famille neutre garde en plus son quasi-blanc (`#F8F4F4`), sans quoi un fond
blanc n'aurait plus d'équivalent. Soit 37 pas, une ligne par famille dans le
sélecteur.

Les 37 passent WCAG AA (≥ 4.5:1, pire cas 5.3:1) avec l'encre que leur choisit
`cardForegroundColor()`. Deux tests unitaires le vérifient et surveillent aussi
que le seuil de 0.2 désigne toujours la meilleure des deux encres.

**Héritage.** Les pas de la première palette monochrome — rampes accent, accent
secondaire et neutres au complet — restent des fonds *valides* sans être des
cibles de remappage : une carte enregistrée du temps du sélecteur monochrome en
porte encore un, et les conserver évite une migration v2 → v3. Les exclure des
cibles était nécessaire, sinon le rouge de l'ancienne rampe captait des teintes
que la grille sait désormais respecter — un rose Material tombait sur `#DD2B0F`
au lieu du magenta.

Archivo est embarquée dans l'APK (`res/font/archivo.ttf`) sous forme de police
variable : les trois graisses (400/600/800) sortent du même fichier via l'axe
`wght`. Pas de polices téléchargeables, donc pas de dépendance à Google Play
Services, pas de réseau au premier affichage et un rendu identique partout.

### Licence de la police

Archivo est distribuée sous **SIL Open Font License 1.1**, dont les termes
imposent de livrer la licence avec la police. Le texte intégral vit dans
`app/src/main/res/raw/archivo_ofl.txt` et est embarqué dans l'APK.

Aucun code ne la référençait jusqu'à l'écran « À propos », donc
`isShrinkResources` la retirait des builds release :
`app/src/main/res/raw/keep.xml` la marque à conserver. Le garde-fou reste utile
si l'écran disparaît un jour. Pour vérifier après un changement de configuration
release :

```bash
./gradlew :app:assembleRelease
# doit lister un res/*.txt (le nom est obfusqué par le shrinker, ex. res/qb.txt)
unzip -l app/build/outputs/apk/release/app-release.apk | grep 'res/.*\.txt'
```

- Police : [Archivo](https://fonts.google.com/specimen/Archivo) — Omnibus-Type
- Licence : [SIL OFL 1.1](https://openfontlicense.org/)

L'OFL n'exige pas d'écran de mentions légales dans l'app, mais l'écran « À
propos » (Réglages › À propos) rend la licence visible : la ligne « Police
Archivo » déplie le contenu de `archivo_ofl.txt`, lu depuis la ressource brute.
C'est le seul code qui référence `R.raw.archivo_ofl`.

### Migrations de base

| Version | Contenu |
|---|---|
| 1 → 2 | Réalignement des fonds sur la palette Modernist. Schéma constant, seules les données bougent |
| 2 → 3 | Colonne `lastUsedAt`, nullable — alimente « Dernier passage » de l'écran 04 |

`lastUsedAt` est nullable à dessein : une carte déjà en base n'a pas d'historique
de passage, et `null` se lit « jamais » plutôt que « le 1er janvier 1970 ».

### Migration des couleurs (base v1 → v2)

`MIGRATION_1_2` réaligne les fonds de carte déjà enregistrés sur la palette.
Le schéma ne change pas — seules les valeurs de `backgroundColor` bougent.

`nearestModernistColor()` compare les couleurs en **OKLab**, où l'écart
euclidien suit à peu près l'écart perçu. La grille couvrant huit teintes, la
teinte d'origine survit au remappage : un bleu tombe sur un bleu, un vert sur un
vert. Seules les couleurs très saturées perdent un peu de leur éclat, le chroma
de la grille étant volontairement uniforme. Une couleur illisible retombe sur le
défaut.

Le même remappage s'applique à l'import d'un backup JSON : un fichier exporté
avant le passage à Modernist porte encore l'ancienne palette. L'opération est
idempotente, une couleur déjà conforme n'est pas touchée.

La migration est couverte à deux niveaux : `ModernistPaletteTest` (JVM) teste le
calcul de correspondance, `CardColorMigrationTest` (instrumenté) teste la
plomberie SQL sur une vraie base v1 via `MigrationTestHelper`.

#### Schémas Room

`exportSchema = true` et `room.schemaLocation` pointe sur `app/schemas/`. Ces
fichiers sont committés : ils sont l'historique de la base et les fixtures de
`MigrationTestHelper`. Le dossier est aussi déclaré en assets d'`androidTest`,
sans quoi le test ne les trouve pas dans l'APK.

Le schéma v1 n'a jamais été exporté — la base est passée en v2 avant que
`exportSchema` ne soit activé. Il est reconstitué par
`scripts/export-room-schemas.sh` : comme la v2 ne change aucune table, les deux
schémas sont identiques au numéro de version près, `identityHash` compris (Room
le calcule à partir des entités, pas de la version).

```bash
scripts/export-room-schemas.sh    # une fois, puis committer app/schemas/
```

Le script ne réécrit jamais un schéma déjà présent.

## Visuels du Play Store

`screenshots/` porte les huit captures de la fiche — six en thème clair, deux en
thème « Encre » — et l'image de présentation en 1024×500, produite par
`feature_graphic_gen.py`.

Les captures viennent de l'app qui tourne : `ScreenshotSeeder`
(`app/src/androidTest/.../screenshots/`) peuple la base, l'émulateur est forcé en
1080×1920 pour respecter le ratio maximal de 2:1 imposé par Play Store, et la
status bar passe en mode démo pour figer l'heure. Le mode d'emploi complet, y
compris l'upload dans Play Console, est dans
[`docs/playstore-visuals.md`](docs/playstore-visuals.md).

Ces visuels sont à reprendre à chaque évolution du design system : une fiche
montre l'app d'avant tant que personne ne rappuie sur le déclencheur.
