package com.beninho.fidelya.ui.cardlist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.order.CardOrderStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.model.LoyaltyCard
import com.beninho.fidelya.ui.theme.FidelyaTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardListScreenTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var repo: CardRepository
    private lateinit var orderStore: CardOrderStore
    private lateinit var logoStore: LogoStore

    @Before fun setUp() {
        repo = mock()
        orderStore = mock()
        logoStore = mock()
        whenever(orderStore.orderFlow).thenReturn(flowOf(emptyList()))
    }

    private val twoCards = listOf(
        LoyaltyCard(id = 1, storeName = "Carrefour", cardNumber = "1234567890123", barcodeFormat = "EAN_13", backgroundColor = "#EE6952"),
        LoyaltyCard(id = 2, storeName = "Fnac", cardNumber = "5678", barcodeFormat = "QR_CODE", backgroundColor = "#0D60A3")
    )

    /** Monte l'écran avec les cartes données ; les rappels sont muets par défaut. */
    private fun setScreen(
        cards: List<LoyaltyCard> = emptyList(),
        onCardClick: (Long) -> Unit = {},
        onCardCheckout: (Long) -> Unit = {},
        onAddClick: () -> Unit = {},
        onManualEntry: () -> Unit = {},
        onReorder: () -> Unit = {},
        onSettings: () -> Unit = {},
    ) {
        whenever(repo.observeAll()).thenReturn(flowOf(cards))
        rule.setContent {
            FidelyaTheme {
                CardListScreen(
                    repository = repo,
                    cardOrderStore = orderStore,
                    logoStore = logoStore,
                    onCardClick = onCardClick,
                    onCardCheckout = onCardCheckout,
                    onAddClick = onAddClick,
                    onManualEntry = onManualEntry,
                    onReorder = onReorder,
                    onSettings = onSettings
                )
            }
        }
    }

    @Test fun emptyStateShowsThePlaceholder() {
        setScreen()

        rule.onNodeWithText("Aucune carte pour l'instant").assertIsDisplayed()
        rule.onNodeWithText("Scanner une carte").assertIsDisplayed()
    }

    /** Recherche et indice d'appui long n'ont aucun sens sans carte. */
    @Test fun emptyStateHidesTheSearchField() {
        setScreen()

        rule.onNodeWithContentDescription("Chercher une enseigne").assertDoesNotExist()
    }

    @Test fun cardsAreListedWithMaskedNumbers() {
        setScreen(cards = twoCards)

        rule.onNodeWithText("Carrefour").assertIsDisplayed()
        rule.onNodeWithText("Fnac").assertIsDisplayed()
        rule.onNodeWithText("···· 0123").assertIsDisplayed()
    }

    @Test fun searchFiltersByStoreName() {
        setScreen(cards = twoCards)

        rule.onNodeWithContentDescription("Chercher une enseigne").performTextInput("fna")

        rule.onNodeWithText("Fnac").assertIsDisplayed()
        rule.onNodeWithText("Carrefour").assertDoesNotExist()
    }

    @Test fun searchWithoutMatchExplainsItself() {
        setScreen(cards = twoCards)

        rule.onNodeWithContentDescription("Chercher une enseigne").performTextInput("zzz")

        rule.onNodeWithText("Aucune enseigne ne correspond à cette recherche.").assertIsDisplayed()
    }

    @Test fun tappingACardOpensIt() {
        var opened = -1L
        setScreen(cards = twoCards, onCardClick = { opened = it })

        rule.onNodeWithText("Fnac").performClick()

        assertEquals(2L, opened)
    }

    /** « Appui long sur une carte = code-barres direct ». */
    @Test fun longPressGoesStraightToCheckout() {
        var checkout = -1L
        setScreen(cards = twoCards, onCardCheckout = { checkout = it })

        rule.onNodeWithText("Carrefour").performTouchInput { longClick() }

        assertEquals(1L, checkout)
    }

    @Test fun bottomBarOffersScanAndManualEntry() {
        var add = false
        var manual = false
        setScreen(cards = twoCards, onAddClick = { add = true }, onManualEntry = { manual = true })

        rule.onNodeWithText("Ajouter une carte").performClick()
        rule.onNodeWithText("Saisir").performClick()

        assertTrue(add)
        assertTrue(manual)
    }

    @Test fun headerReachesOrderAndSettings() {
        var reorder = false
        var settings = false
        setScreen(cards = twoCards, onReorder = { reorder = true }, onSettings = { settings = true })

        rule.onNodeWithText("ORDRE").performClick()
        rule.onNodeWithText("RÉGLAGES").performClick()

        assertTrue(reorder)
        assertTrue(settings)
    }
}
