# Fidelya

Application Android de gestion de cartes de fidélité. Une app sur laquelle on scanne ou saisit les cartes de fidélité des magasins afin de les centraliser et de les rendre disponibles en un seul geste à la caisse.

## Fonctionnalités

- **Ajout par scan** — utilise la caméra et ML Kit pour détecter automatiquement le code-barres et son format
- **Saisie manuelle** — entrez le numéro et choisissez le format parmi QR Code, EAN-13, EAN-8, Code 128, Code 39, PDF 417, Data Matrix
- **Affichage plein écran** — la luminosité monte automatiquement lors de l'affichage pour faciliter la lecture en caisse
- **Personnalisation** — couleur de fond parmi les 22 pas des rampes du design system, emoji ou initiale en icône
- **Réorganisation** — maintenez appuyé sur une carte pour la déplacer par glisser-déposer, l'ordre est persistant
- **Sauvegarde** — exportez et importez toutes vos cartes au format JSON

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

## Design system

Le thème est importé du projet Claude Design **« Modernist »** — `styles.css` et
`theme.json` de ce projet font foi. Le port vit dans `ui/theme/` :

| Fichier | Contenu |
|---|---|
| `Color.kt` | Rôles de base + trois rampes tonales de 9 pas (neutre, accent, accent secondaire) |
| `Type.kt` | Archivo (400/600/800) embarquée, échelle Modernist mappée sur les styles Material 3 |
| `Shape.kt` | Rayons à 0 — Modernist n'arrondit rien |
| `Theme.kt` | Schémas clair et sombre assemblés à partir des rampes |
| `CardPalette.kt` | Façade Compose de la palette de cartes + calcul du texte contrasté |

La palette des cartes elle-même vit dans `domain/color/ModernistPalette.kt`, en
Kotlin pur : la couche `data` en a besoin pour migrer la base et ne peut pas
dépendre de Compose.

Modernist est monochrome : les cartes ne se distinguent plus par la teinte mais
par la valeur. `cardForegroundColor()` choisit l'encre ou le fond de page selon
la luminance, comme le fait `.btn-primary` dans le CSS d'origine — jamais de
blanc pur sur une couleur.

Archivo est embarquée dans l'APK (`res/font/archivo.ttf`) sous forme de police
variable : les trois graisses (400/600/800) sortent du même fichier via l'axe
`wght`. Pas de polices téléchargeables, donc pas de dépendance à Google Play
Services, pas de réseau au premier affichage et un rendu identique partout.

### Licence de la police

Archivo est distribuée sous **SIL Open Font License 1.1**, dont les termes
imposent de livrer la licence avec la police. Le texte intégral vit dans
`app/src/main/res/raw/archivo_ofl.txt` et est embarqué dans l'APK.

- Police : [Archivo](https://fonts.google.com/specimen/Archivo) — Omnibus-Type
- Licence : [SIL OFL 1.1](https://openfontlicense.org/)

L'OFL n'exige pas d'écran de mentions légales dans l'app ; si un écran « À
propos » est ajouté un jour, y afficher le contenu de `archivo_ofl.txt` est la
façon la plus simple de rendre la licence visible aux utilisateurs.

### Migration des couleurs (base v1 → v2)

`MIGRATION_1_2` réaligne les fonds de carte déjà enregistrés sur la palette.
Le schéma ne change pas — seules les valeurs de `backgroundColor` bougent.

`nearestModernistColor()` compare les couleurs en **OKLab**, où l'écart
euclidien suit à peu près l'écart perçu. La teinte d'origine est perdue puisque
Modernist n'a qu'une famille chromatique, mais le rapport clair/soutenu est
préservé : une couleur vive tombe sur un pas accent, une couleur désaturée sur
un neutre de valeur comparable. Une couleur illisible retombe sur le défaut.

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
