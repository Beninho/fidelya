package com.example.fidcard.ui.cardedit

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.fidcard.ui.theme.FidCardTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardEditScreenTest {
    @get:Rule val rule = createComposeRule()
    private val repo = mock<com.example.fidcard.data.repository.CardRepository>()

    @Test fun `save with empty name shows error`() {
        whenever(repo.observeAll()).thenReturn(flowOf(emptyList()))
        rule.setContent { FidCardTheme { CardEditScreen(cardId = -1, repository = repo, onSaved = {}, onBack = {}) } }
        rule.onNodeWithText("Enregistrer").performClick()
        rule.onNodeWithText("Le nom est obligatoire").assertIsDisplayed()
    }

    @Test fun `prefilled card number appears in field`() {
        whenever(repo.observeAll()).thenReturn(flowOf(emptyList()))
        rule.setContent {
            FidCardTheme {
                CardEditScreen(
                    cardId = -1, repository = repo, onSaved = {}, onBack = {},
                    prefilledCardNumber = "9999999", prefilledFormat = "EAN_13"
                )
            }
        }
        rule.onNodeWithText("9999999").assertIsDisplayed()
    }
}
