# Manual Entry Bottom Sheet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Afficher un `ModalBottomSheet` au clic sur le FAB "+" proposant "Scanner un code-barres" ou "Saisir manuellement".

**Architecture:** État local `showAddSheet` dans `CardListScreen`. Le FAB ouvre la sheet ; chaque option ferme la sheet et déclenche le callback approprié (`onAddClick` ou `onManualEntry`). `MainActivity` câble le nouveau callback vers `cardEdit/-1`.

**Tech Stack:** Jetpack Compose, Material3 (`ModalBottomSheet`, `ListItem`), Compose UI Tests (Mockito-Kotlin)

---

### Task 1: Mettre à jour les tests existants + ajouter les tests de la sheet

**Files:**
- Modify: `app/src/androidTest/java/com/example/fidcard/ui/cardlist/CardListScreenTest.kt`

- [ ] **Step 1: Ajouter `onManualEntry` aux appels `CardListScreen` existants et écrire les tests qui échoueront**

Remplacer tout le fichier `CardListScreenTest.kt` par :

```kotlin
package com.example.fidcard.ui.cardlist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import com.example.fidcard.ui.theme.FidCardTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardListScreenTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var repo: CardRepository

    @Before fun setUp() { repo = mock() }

    @Test fun `empty state shows placeholder text`() {
        whenever(repo.observeAll()).thenReturn(flowOf(emptyList()))
        rule.setContent {
            FidCardTheme {
                CardListScreen(
                    repository = repo,
                    onCardClick = {},
                    onAddClick = {},
                    onManualEntry = {}
                )
            }
        }
        rule.onNodeWithText("Aucune carte. Appuyez sur + pour scanner.").assertIsDisplayed()
    }

    @Test fun `cards are displayed in grid`() {
        val cards = listOf(
            LoyaltyCard(id = 1, storeName = "Carrefour", cardNumber = "1234", barcodeFormat = "EAN_13", backgroundColor = "#E53935"),
            LoyaltyCard(id = 2, storeName = "Fnac", cardNumber = "5678", barcodeFormat = "QR_CODE", backgroundColor = "#1565C0")
        )
        whenever(repo.observeAll()).thenReturn(flowOf(cards))
        rule.setContent {
            FidCardTheme {
                CardListScreen(
                    repository = repo,
                    onCardClick = {},
                    onAddClick = {},
                    onManualEntry = {}
                )
            }
        }
        rule.onNodeWithText("Carrefour").assertIsDisplayed()
        rule.onNodeWithText("Fnac").assertIsDisplayed()
    }

    @Test fun `fab click shows bottom sheet with two options`() {
        whenever(repo.observeAll()).thenReturn(flowOf(emptyList()))
        rule.setContent {
            FidCardTheme {
                CardListScreen(
                    repository = repo,
                    onCardClick = {},
                    onAddClick = {},
                    onManualEntry = {}
                )
            }
        }
        rule.onNodeWithContentDescription("Ajouter").performClick()
        rule.onNodeWithText("Scanner un code-barres").assertIsDisplayed()
        rule.onNodeWithText("Saisir manuellement").assertIsDisplayed()
    }

    @Test fun `scanner option triggers onAddClick`() {
        whenever(repo.observeAll()).thenReturn(flowOf(emptyList()))
        var addClicked = false
        rule.setContent {
            FidCardTheme {
                CardListScreen(
                    repository = repo,
                    onCardClick = {},
                    onAddClick = { addClicked = true },
                    onManualEntry = {}
                )
            }
        }
        rule.onNodeWithContentDescription("Ajouter").performClick()
        rule.onNodeWithText("Scanner un code-barres").performClick()
        assert(addClicked)
    }

    @Test fun `manual entry option triggers onManualEntry`() {
        whenever(repo.observeAll()).thenReturn(flowOf(emptyList()))
        var manualClicked = false
        rule.setContent {
            FidCardTheme {
                CardListScreen(
                    repository = repo,
                    onCardClick = {},
                    onAddClick = {},
                    onManualEntry = { manualClicked = true }
                )
            }
        }
        rule.onNodeWithContentDescription("Ajouter").performClick()
        rule.onNodeWithText("Saisir manuellement").performClick()
        assert(manualClicked)
    }
}
```

- [ ] **Step 2: Vérifier que les tests ne compilent pas (erreur attendue : `onManualEntry` inconnu)**

```bash
./gradlew :app:compileDebugAndroidTestKotlin 2>&1 | grep -A2 "error:"
```

Attendu : erreur de compilation sur `onManualEntry`.

---

### Task 2: Implémenter le ModalBottomSheet dans CardListScreen

**Files:**
- Modify: `app/src/main/java/com/example/fidcard/ui/cardlist/CardListScreen.kt:32-127`

- [ ] **Step 3: Mettre à jour `CardListScreen.kt`**

Remplacer tout le fichier `CardListScreen.kt` par :

```kotlin
package com.example.fidcard.ui.cardlist

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fidcard.backup.BackupManager
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    repository: CardRepository,
    onCardClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onManualEntry: () -> Unit,
    vm: CardListViewModel = viewModel(factory = cardListViewModelFactory(repository))
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddSheet by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        vm.exportCards(uri, context.contentResolver)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val cards = BackupManager.import(context, uri)
                vm.importCards(cards)
            }.onFailure {
                Toast.makeText(context, "Fichier invalide", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            ListItem(
                headlineContent = { Text("Scanner un code-barres") },
                leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    onAddClick()
                }
            )
            ListItem(
                headlineContent = { Text("Saisir manuellement") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    onManualEntry()
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes cartes") },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Exporter") },
                            onClick = {
                                menuExpanded = false
                                exportLauncher.launch("fidcard_backup.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Importer") },
                            onClick = {
                                menuExpanded = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { padding ->
        if (state.cards.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucune carte. Appuyez sur + pour scanner.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(state.cards, key = { it.id }) { card ->
                    LoyaltyCardItem(
                        card = card,
                        onClick = { onCardClick(card.id) },
                        onDelete = { vm.deleteCard(card) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoyaltyCardItem(card: LoyaltyCard, onClick: () -> Unit, onDelete: () -> Unit) {
    val bgColor = runCatching {
        Color(android.graphics.Color.parseColor(card.backgroundColor))
    }.getOrDefault(Color.Gray)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                text = card.logoEmoji ?: card.storeName.take(1).uppercase(),
                fontSize = 28.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    card.storeName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "···· ${card.cardNumber.takeLast(4)}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
```

- [ ] **Step 4: Vérifier que le code compile**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Attendu : `BUILD SUCCESSFUL` (ou uniquement l'erreur restante dans MainActivity).

---

### Task 3: Câbler `onManualEntry` dans MainActivity

**Files:**
- Modify: `app/src/main/java/com/example/fidcard/MainActivity.kt:35-41`

- [ ] **Step 5: Ajouter le callback `onManualEntry` dans `CardListScreen` de `FidCardNavHost`**

Dans `MainActivity.kt`, remplacer le bloc `composable("cardList")` (lignes 35-41) par :

```kotlin
composable("cardList") {
    CardListScreen(
        repository = app.repository,
        onCardClick = { id -> navController.navigate("cardDetail/$id") },
        onAddClick = { navController.navigate("scan") },
        onManualEntry = { navController.navigate("cardEdit/-1") }
    )
}
```

- [ ] **Step 6: Vérifier que tout compile**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep -E "error:|BUILD"
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 7: Lancer les tests instrumentés**

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.example.fidcard.ui.cardlist.CardListScreenTest" 2>&1 | tail -20
```

Attendu : 5 tests passent.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/fidcard/ui/cardlist/CardListScreen.kt \
        app/src/main/java/com/example/fidcard/MainActivity.kt \
        app/src/androidTest/java/com/example/fidcard/ui/cardlist/CardListScreenTest.kt
git commit -m "feat: bottom sheet on FAB + with scanner and manual entry options"
```
