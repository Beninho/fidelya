# Fidelya

Application Android de gestion de cartes de fidélité. Scannez ou saisissez vos cartes, retrouvez-les en un geste au moment de payer.

## Fonctionnalités

- **Ajout par scan** — utilise la caméra et ML Kit pour détecter automatiquement le code-barres et son format
- **Saisie manuelle** — entrez le numéro et choisissez le format parmi QR Code, EAN-13, EAN-8, Code 128, Code 39, PDF 417, Data Matrix
- **Affichage plein écran** — la luminosité monte automatiquement lors de l'affichage pour faciliter la lecture en caisse
- **Personnalisation** — couleur de fond parmi 22 teintes, emoji ou initiale en icône
- **Réorganisation** — maintenez appuyé sur une carte pour la déplacer par glisser-déposer, l'ordre est persistant
- **Sauvegarde** — exportez et importez toutes vos cartes au format JSON

## Stack technique

| Couche | Technologies |
|---|---|
| UI | Jetpack Compose, Material 3 |
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

## Structure du projet

```
app/src/main/java/com/example/fidelya/
├── data/
│   ├── db/          # Room — entité, DAO, AppDatabase
│   ├── order/       # CardOrderStore (DataStore)
│   └── repository/  # CardRepository + implémentation
├── domain/model/    # LoyaltyCard (modèle métier)
├── ui/
│   ├── cardlist/    # Liste avec drag-and-drop
│   ├── carddetail/  # Affichage du code-barres
│   ├── cardedit/    # Formulaire de création/édition
│   ├── scan/        # Écran de scan caméra
│   └── theme/       # Couleurs, typo, thème Material 3
├── backup/          # Export / import JSON
├── FidelyaApp.kt    # Application (DI manuelle)
└── MainActivity.kt  # Navigation Compose
```
