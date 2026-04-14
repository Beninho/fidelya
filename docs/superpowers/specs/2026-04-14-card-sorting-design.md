# Card Sorting — Design Spec
**Date:** 2026-04-14  
**Feature:** Réorganisation des cartes par glisser-déposer

---

## Contexte

L'application FidCard affiche les cartes de fidélité en grille 2 colonnes (`LazyVerticalGrid`). L'ordre actuel est alphabétique, codé en dur dans le DAO Room. Cette spec décrit l'ajout d'un tri personnalisé par glisser-déposer, persisté en dehors de la base de données.

---

## Objectif

Permettre à l'utilisateur de réorganiser ses cartes en appuyant longuement sur une carte et en la déplaçant dans la grille. L'ordre est conservé entre les sessions.

---

## Décisions clés

| Question | Décision |
|---|---|
| Mécanisme de drag | Bibliothèque `sh.calvin.reorderable` |
| Layout | Grille 2 colonnes conservée |
| Activation | Long press (toujours actif, pas de mode dédié) |
| Persistance | DataStore Preferences (liste d'IDs ordonnés) |

---

## Architecture

### Flux de données

```
Room (LoyaltyCardDao)          DataStore (Preferences)
       │ Flow<List<Card>>              │ Flow<List<Long>> (IDs triés)
       └──────────────┬───────────────┘
                      ▼
              CardListViewModel
              combine() → applique l'ordre aux cartes
              onMove(from, to) → met à jour l'état local + persiste dans DataStore
                      │
              CardListUiState(cards: List<LoyaltyCard>)
                      │
              CardListScreen
              ReorderableGrid (sh.calvin.reorderable)
```

### Nouveau composant : `CardOrderStore`

Wrappeur DataStore qui :
- Expose `Flow<List<Long>>` — liste des IDs de cartes dans l'ordre sauvegardé
- Fournit `suspend fun save(ids: List<Long>)` — persiste un nouvel ordre

### Logique de combinaison dans `CardListViewModel`

```
combine(repository.observeAll(), cardOrderStore.orderFlow) { cards, orderedIds ->
    if (orderedIds.isEmpty()) {
        cards  // ordre Room par défaut (alphabétique)
    } else {
        val index = orderedIds.withIndex().associate { (i, id) -> id to i }
        val maxIndex = orderedIds.size
        cards.sortedBy { index[it.id] ?: maxIndex }  // nouvelles cartes à la fin
    }
}
```

`onMove(from: Int, to: Int)` :
1. Swap dans la liste en mémoire (mise à jour immédiate de l'UI)
2. Sauvegarde des nouveaux IDs ordonnés dans DataStore (coroutine)

---

## UI

### Intégration `sh.calvin.reorderable` dans `CardListScreen`

```kotlin
val reorderState = rememberReorderableLazyGridState(
    onMove = { from, to -> vm.onMove(from.index, to.index) }
)

LazyVerticalGrid(
    state = reorderState.gridState,
    columns = GridCells.Fixed(2),
    ...
) {
    items(state.cards, key = { it.id }) { card ->
        ReorderableItem(reorderState, key = card.id) { isDragging ->
            LoyaltyCardItem(
                card = card,
                isDragging = isDragging,
                modifier = Modifier.longPressDraggableHandle(),
                ...
            )
        }
    }
}
```

### Retour visuel

- La carte soulevée reçoit `isDragging = true` → légère élévation (shadow)
- Aucun mode "réorganiser" à activer, le long press suffit

---

## Fichiers modifiés / créés

| Fichier | Action |
|---|---|
| `ui/cardlist/CardListScreen.kt` | Intégration reorderable |
| `ui/cardlist/CardListViewModel.kt` | Ajout `onMove()`, `combine()` avec DataStore |
| `ui/cardlist/CardListUiState.kt` | Inchangé |
| `data/order/CardOrderStore.kt` | **Nouveau** — wrappeur DataStore |
| `gradle/libs.versions.toml` | 2 nouvelles dépendances |
| `app/build.gradle.kts` | Ajout des dépendances |

### Nouvelles dépendances

- `sh.calvin.reorderable:reorderable` — drag-and-drop Compose natif avec support LazyGrid
- `androidx.datastore:datastore-preferences` — persistance légère des préférences

---

## Tests

### Tests unitaires à ajouter (`CardListViewModelTest`)

1. `onMove()` modifie correctement l'ordre dans `uiState.cards`
2. Combinaison cartes Room + ordre DataStore → liste correctement réordonnée
3. Nouvelle carte sans position sauvegardée → apparaît en fin de liste

### Tests existants

Tous les tests existants (`CardListViewModelTest`, `CardListScreenTest`, `CardEditScreenTest`) doivent continuer à passer sans modification majeure.

### Tests non écrits

- Tests de drag physique (domaine du framework reorderable)
- Tests unitaires de `CardOrderStore` (wrappeur trivial)

---

## Hors périmètre

- Tri alphabétique ou par date (uniquement ordre personnalisé)
- Export/import de l'ordre (non inclus dans le backup JSON)
- Mode "réorganiser" explicite avec bouton dédié
