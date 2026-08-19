package com.beninho.fidelya.ui.cardlist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.beninho.fidelya.data.order.CardOrderStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.model.LoyaltyCard
import com.beninho.fidelya.ui.theme.FidelyaTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.junit.Assert.assertTrue
import org.mockito.kotlin.whenever

class CardListScreenTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var repo: CardRepository
    private lateinit var orderStore: CardOrderStore

    @Before fun setUp() {
        repo = mock()
        orderStore = mock()
        whenever(orderStore.orderFlow).thenReturn(flowOf(emptyList()))
    }

    /** Mounts the screen with the given cards; callbacks default to no-ops. */
    private fun setScreen(
        cards: List<LoyaltyCard> = emptyList(),
        onCardClick: (Long) -> Unit = {},
        onAddClick: () -> Unit = {},
        onManualEntry: () -> Unit = {},
    ) {
        whenever(repo.observeAll()).thenReturn(flowOf(cards))
        rule.setContent {
            FidelyaTheme {
                CardListScreen(
                    repository = repo,
                    cardOrderStore = orderStore,
                    onCardClick = onCardClick,
                    onAddClick = onAddClick,
                    onManualEntry = onManualEntry
                )
            }
        }
    }

    @Test fun emptyStateShowsPlaceholderText() {
        setScreen()

        rule.onNodeWithText("Aucune carte. Appuyez sur + pour en ajouter une.").assertIsDisplayed()
    }

    @Test fun cardsAreDisplayedInGrid() {
        setScreen(
            cards = listOf(
                LoyaltyCard(id = 1, storeName = "Carrefour", cardNumber = "1234", barcodeFormat = "EAN_13", backgroundColor = "#E53935"),
                LoyaltyCard(id = 2, storeName = "Fnac", cardNumber = "5678", barcodeFormat = "QR_CODE", backgroundColor = "#1565C0")
            )
        )

        rule.onNodeWithText("Carrefour").assertIsDisplayed()
        rule.onNodeWithText("Fnac").assertIsDisplayed()
    }

    @Test fun fabClickShowsBottomSheetWithTwoOptions() {
        setScreen()

        rule.onNodeWithContentDescription("Ajouter").performClick()

        rule.onNodeWithText("Scanner un code-barres").assertIsDisplayed()
        rule.onNodeWithText("Saisir manuellement").assertIsDisplayed()
    }

    @Test fun scannerOptionTriggersOnAddClick() {
        var addClicked = false
        setScreen(onAddClick = { addClicked = true })

        rule.onNodeWithContentDescription("Ajouter").performClick()
        rule.onNodeWithText("Scanner un code-barres").performClick()

        assertTrue(addClicked)
    }

    @Test fun manualEntryOptionTriggersOnManualEntry() {
        var manualClicked = false
        setScreen(onManualEntry = { manualClicked = true })

        rule.onNodeWithContentDescription("Ajouter").performClick()
        rule.onNodeWithText("Saisir manuellement").performClick()

        assertTrue(manualClicked)
    }
}
