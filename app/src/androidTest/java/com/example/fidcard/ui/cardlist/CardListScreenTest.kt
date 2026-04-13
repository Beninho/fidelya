package com.example.fidcard.ui.cardlist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.fidcard.domain.model.LoyaltyCard
import com.example.fidcard.ui.theme.FidCardTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardListScreenTest {
    @get:Rule val rule = createComposeRule()
    private val repo = mock<com.example.fidcard.data.repository.CardRepository>()

    @Test fun `empty state shows placeholder text`() {
        whenever(repo.observeAll()).thenReturn(flowOf(emptyList()))
        rule.setContent { FidCardTheme { CardListScreen(repository = repo, onCardClick = {}, onAddClick = {}) } }
        rule.onNodeWithText("Aucune carte. Appuyez sur + pour scanner.").assertIsDisplayed()
    }

    @Test fun `cards are displayed in grid`() {
        val cards = listOf(
            LoyaltyCard(id = 1, storeName = "Carrefour", cardNumber = "1234", barcodeFormat = "EAN_13", backgroundColor = "#E53935"),
            LoyaltyCard(id = 2, storeName = "Fnac", cardNumber = "5678", barcodeFormat = "QR_CODE", backgroundColor = "#1565C0")
        )
        whenever(repo.observeAll()).thenReturn(flowOf(cards))
        rule.setContent { FidCardTheme { CardListScreen(repository = repo, onCardClick = {}, onAddClick = {}) } }
        rule.onNodeWithText("Carrefour").assertIsDisplayed()
        rule.onNodeWithText("Fnac").assertIsDisplayed()
    }
}
