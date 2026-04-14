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
import org.junit.Assert.assertTrue
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
        rule.onNodeWithText("Aucune carte. Appuyez sur + pour en ajouter une.").assertIsDisplayed()
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
        assertTrue(addClicked)
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
        assertTrue(manualClicked)
    }
}
