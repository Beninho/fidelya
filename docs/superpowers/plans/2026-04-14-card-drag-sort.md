# Card Drag-and-Drop Sorting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow users to reorder loyalty cards in the 2-column grid by long-pressing and dragging, with the custom order persisted in DataStore Preferences.

**Architecture:** A new `CardOrderStore` interface (backed by DataStore Preferences) stores card IDs in sorted order. `CardListViewModel` combines `CardRepository.observeAll()` with `CardOrderStore.orderFlow` using `kotlinx.coroutines.flow.combine`, applying the saved order on each emission. `onMove(from, to)` swaps the in-memory list and saves to DataStore async. `CardListScreen` integrates `sh.calvin.reorderable` on `LazyVerticalGrid`.

**Tech Stack:** `sh.calvin.reorderable:2.4.3`, `androidx.datastore:datastore-preferences:1.1.1`, `kotlinx.coroutines.flow.combine`, existing Mockito + Turbine for unit tests.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `gradle/libs.versions.toml` | Modify | Add reorderable + datastore versions and library entries |
| `app/build.gradle.kts` | Modify | Add two `implementation()` dependencies |
| `app/src/main/java/com/example/fidcard/data/order/CardOrderStore.kt` | **Create** | Interface + Impl — DataStore-backed persistence for card ID order |
| `app/src/main/java/com/example/fidcard/ui/cardlist/CardListViewModel.kt` | Modify | Add `cardOrderStore` param, `combine()`, `onMove()`, `applyOrder()` |
| `app/src/test/java/com/example/fidcard/ui/cardlist/CardListViewModelTest.kt` | Modify | Tests for ordering logic and `onMove` |
| `app/src/main/java/com/example/fidcard/ui/cardlist/CardListScreen.kt` | Modify | Integrate reorderable state + grid, update `LoyaltyCardItem` signature |

---

## Task 1: Add Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add version entries in libs.versions.toml**

In `gradle/libs.versions.toml`, in the `[versions]` section, add after `room = "2.6.1"`:
```toml
reorderable = "2.4.3"
datastore = "1.1.1"
```

- [ ] **Step 2: Add library entries in libs.versions.toml**

In `gradle/libs.versions.toml`, in the `[libraries]` section, add before `# Tests`:
```toml
reorderable = { group = "sh.calvin.reorderable", name = "reorderable", version.ref = "reorderable" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

- [ ] **Step 3: Add implementation entries in build.gradle.kts**

In `app/build.gradle.kts`, after `implementation(libs.kotlinx.serialization.json)`, add:
```kotlin
// Drag-and-drop reordering in Compose LazyGrid
implementation(libs.reorderable)
// Card order persistence
implementation(libs.androidx.datastore.preferences)
```

- [ ] **Step 4: Sync and verify**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL` — no unresolved reference errors.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add reorderable and datastore-preferences dependencies"
```

---

## Task 2: Create CardOrderStore

**Files:**
- Create: `app/src/main/java/com/example/fidcard/data/order/CardOrderStore.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.example.fidcard.data.order

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.cardOrderDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "card_order")

interface CardOrderStore {
    val orderFlow: Flow<List<Long>>
    suspend fun save(ids: List<Long>)
}

class CardOrderStoreImpl(context: Context) : CardOrderStore {
    private val dataStore = context.applicationContext.cardOrderDataStore
    private val orderKey = stringPreferencesKey("ordered_ids")

    override val orderFlow: Flow<List<Long>> = dataStore.data.map { prefs ->
        val raw = prefs[orderKey] ?: return@map emptyList()
        Json.decodeFromString<List<Long>>(raw)
    }

    override suspend fun save(ids: List<Long>) {
        dataStore.edit { prefs ->
            prefs[orderKey] = Json.encodeToString(ids)
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/fidcard/data/order/CardOrderStore.kt
git commit -m "feat: add CardOrderStore for DataStore-backed card order persistence"
```

---

## Task 3: Update CardListViewModel (TDD)

**Files:**
- Modify: `app/src/test/java/com/example/fidcard/ui/cardlist/CardListViewModelTest.kt`
- Modify: `app/src/main/java/com/example/fidcard/ui/cardlist/CardListViewModel.kt`

- [ ] **Step 1: Write failing tests**

Replace `CardListViewModelTest.kt` entirely:

```kotlin
package com.example.fidcard.ui.cardlist

import app.cash.turbine.test
import com.example.fidcard.data.order.CardOrderStore
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CardListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CardRepository
    private lateinit var orderStore: CardOrderStore
    private lateinit var vm: CardListViewModel

    private val cardA = LoyaltyCard(
        id = 1L, storeName = "Carrefour", cardNumber = "1234567890",
        barcodeFormat = "QR_CODE", backgroundColor = "#FF0000"
    )
    private val cardB = LoyaltyCard(
        id = 2L, storeName = "Leclerc", cardNumber = "0987654321",
        barcodeFormat = "QR_CODE", backgroundColor = "#0000FF"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        orderStore = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits cards in original order when no saved order`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(cardA, cardB)))
        whenever(orderStore.orderFlow).thenReturn(flowOf(emptyList()))
        vm = CardListViewModel(repository, orderStore)

        vm.uiState.test {
            assertEquals(listOf(cardA, cardB), awaitItem().cards)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `applies saved order when orderFlow has ids`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(cardA, cardB)))
        whenever(orderStore.orderFlow).thenReturn(flowOf(listOf(2L, 1L)))
        vm = CardListViewModel(repository, orderStore)

        vm.uiState.test {
            assertEquals(listOf(cardB, cardA), awaitItem().cards)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `new card not in saved order appears at end`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(cardA, cardB)))
        whenever(orderStore.orderFlow).thenReturn(flowOf(listOf(1L))) // only cardA in saved order
        vm = CardListViewModel(repository, orderStore)

        vm.uiState.test {
            assertEquals(listOf(cardA, cardB), awaitItem().cards)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMove saves new order to store`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(cardA, cardB)))
        whenever(orderStore.orderFlow).thenReturn(flowOf(emptyList()))
        vm = CardListViewModel(repository, orderStore)

        vm.uiState.test {
            awaitItem() // consume initial state: [cardA, cardB]
            vm.onMove(from = 0, to = 1)
            verify(orderStore).save(listOf(2L, 1L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete card calls repository delete`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(emptyList()))
        whenever(orderStore.orderFlow).thenReturn(flowOf(emptyList()))
        vm = CardListViewModel(repository, orderStore)

        vm.deleteCard(cardA)

        verify(repository).delete(cardA)
    }
}
```

- [ ] **Step 2: Run to confirm red**

Run: `./gradlew testDebugUnitTest --tests "com.example.fidcard.ui.cardlist.CardListViewModelTest"`
Expected: FAILED — `CardListViewModel` constructor does not match (still takes one param).

- [ ] **Step 3: Update CardListViewModel**

Replace `CardListViewModel.kt` entirely:

```kotlin
package com.example.fidcard.ui.cardlist

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fidcard.backup.BackupManager
import com.example.fidcard.data.order.CardOrderStore
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardListViewModel(
    private val repository: CardRepository,
    private val cardOrderStore: CardOrderStore
) : ViewModel() {

    val uiState: StateFlow<CardListUiState> =
        repository.observeAll()
            .combine(cardOrderStore.orderFlow) { cards, orderedIds ->
                CardListUiState(applyOrder(cards, orderedIds))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardListUiState())

    fun onMove(from: Int, to: Int) {
        val current = uiState.value.cards.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        current.add(to, current.removeAt(from))
        viewModelScope.launch { cardOrderStore.save(current.map { it.id }) }
    }

    fun deleteCard(card: LoyaltyCard) {
        viewModelScope.launch { repository.delete(card) }
    }

    fun importCards(cards: List<LoyaltyCard>) {
        viewModelScope.launch { repository.insertAll(cards) }
    }

    fun exportCards(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val json = BackupManager.export(repository.getAll())
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
        }
    }

    private fun applyOrder(cards: List<LoyaltyCard>, orderedIds: List<Long>): List<LoyaltyCard> {
        if (orderedIds.isEmpty()) return cards
        val indexMap = orderedIds.withIndex().associate { (i, id) -> id to i }
        return cards.sortedBy { indexMap[it.id] ?: orderedIds.size }
    }
}

fun cardListViewModelFactory(repository: CardRepository, cardOrderStore: CardOrderStore) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CardListViewModel(repository, cardOrderStore) as T
        }
    }
```

- [ ] **Step 4: Run to confirm green**

Run: `./gradlew testDebugUnitTest --tests "com.example.fidcard.ui.cardlist.CardListViewModelTest"`
Expected: 5 tests PASSED.

- [ ] **Step 5: Run all unit tests to check no regressions**

Run: `./gradlew testDebugUnitTest`
Expected: All tests PASSED.

- [ ] **Step 6: Commit**

```bash
git add app/src/test/java/com/example/fidcard/ui/cardlist/CardListViewModelTest.kt \
        app/src/main/java/com/example/fidcard/ui/cardlist/CardListViewModel.kt
git commit -m "feat: add combine-based order logic and onMove to CardListViewModel"
```

---

## Task 4: Integrate Reorderable in CardListScreen

**Files:**
- Modify: `app/src/main/java/com/example/fidcard/ui/cardlist/CardListScreen.kt`

- [ ] **Step 1: Update LoyaltyCardItem signature and elevation**

Replace the existing `LoyaltyCardItem` function with:

```kotlin
@Composable
fun LoyaltyCardItem(
    card: LoyaltyCard,
    isDragging: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = runCatching {
        Color(android.graphics.Color.parseColor(card.backgroundColor))
    }.getOrDefault(Color.Gray)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 0.dp
        ),
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

- [ ] **Step 2: Add reorderable imports**

At the top of `CardListScreen.kt`, add these imports after the existing ones:

```kotlin
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import com.example.fidcard.data.order.CardOrderStoreImpl
```

- [ ] **Step 3: Update CardListScreen ViewModel factory call**

In `CardListScreen`, change the default `vm` parameter from:
```kotlin
vm: CardListViewModel = viewModel(factory = cardListViewModelFactory(repository))
```
to:
```kotlin
vm: CardListViewModel = viewModel(
    factory = cardListViewModelFactory(repository, CardOrderStoreImpl(LocalContext.current))
)
```

- [ ] **Step 4: Replace LazyVerticalGrid with reorderable version**

Inside `CardListScreen`, replace the `LazyVerticalGrid` block (inside the `else` branch) with:

```kotlin
val reorderState = rememberReorderableLazyGridState(
    onMove = { from, to -> vm.onMove(from.index, to.index) }
)
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    state = reorderState.gridState,
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.padding(padding)
) {
    items(state.cards, key = { it.id }) { card ->
        ReorderableItem(reorderState, key = card.id) { isDragging ->
            LoyaltyCardItem(
                card = card,
                isDragging = isDragging,
                onClick = { onCardClick(card.id) },
                onDelete = { vm.deleteCard(card) },
                modifier = Modifier.longPressDraggableHandle()
            )
        }
    }
}
```

- [ ] **Step 5: Run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: All tests PASSED.

- [ ] **Step 6: Build debug APK**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/fidcard/ui/cardlist/CardListScreen.kt
git commit -m "feat: integrate reorderable drag-and-drop in CardListScreen"
```
