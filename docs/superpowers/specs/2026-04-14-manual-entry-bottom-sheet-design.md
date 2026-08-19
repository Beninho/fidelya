# Saisie manuelle via bottom sheet sur le bouton "+"

**Date:** 2026-04-14  
**Statut:** Approuvé

## Contexte

Actuellement, le bouton "+" (FAB) de `CardListScreen` navigue directement vers le scanner caméra (`ScanScreen`). L'utilisateur doit attendre un timeout de 10 secondes avant de voir apparaître l'option de saisie manuelle.

## Objectif

Permettre à l'utilisateur de choisir immédiatement entre scanner un code-barres ou saisir une carte manuellement dès l'appui sur "+".

## Design retenu

Un `ModalBottomSheet` (Material3) s'ouvre au clic sur le FAB avec deux options :

| Option | Icône | Action |
|--------|-------|--------|
| Scanner un code-barres | `Icons.Default.CameraAlt` | Navigue vers `scan` (flux existant inchangé) |
| Saisir manuellement | `Icons.Default.Edit` | Navigue vers `cardEdit/-1` (mode création, sans pré-remplissage) |

## Architecture

- État local `showAddSheet: Boolean` dans `CardListScreen` (pas de ViewModel nécessaire)
- Le FAB set `showAddSheet = true` au lieu d'appeler `onAddClick` directement
- Nouveau callback `onManualEntry: () -> Unit` ajouté à `CardListScreen`
- En `MainActivity` : `onManualEntry = { navController.navigate("cardEdit/-1") }`

## Fichiers modifiés

- `app/src/main/java/com/example/fidcard/ui/cardlist/CardListScreen.kt`
- `app/src/main/java/com/example/fidcard/MainActivity.kt`

## Ce qui ne change pas

- `ScanScreen` et son flux sont inchangés
- `CardEditScreen` en mode création (`cardId = -1`) fonctionne déjà sans pré-remplissage
- Aucun nouveau ViewModel, aucune nouvelle route de navigation
