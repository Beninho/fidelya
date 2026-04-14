package com.example.fidelya.ui.cardedit

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.fidelya.data.repository.CardRepository
import com.example.fidelya.ui.theme.FidelyaTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class CardEditScreenTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var repo: CardRepository

    @Before fun setUp() { repo = mock() }

    @Test fun `save with empty name shows error`() {
        rule.setContent { FidelyaTheme { CardEditScreen(cardId = -1, repository = repo, onSaved = {}, onBack = {}) } }
        rule.onNodeWithText("Enregistrer").performClick()
        rule.onNodeWithText("Le nom est obligatoire").assertIsDisplayed()
        rule.onNodeWithText("Le numéro est obligatoire").assertIsDisplayed()
    }

    @Test fun `prefilled card number appears in field`() {
        rule.setContent {
            FidelyaTheme {
                CardEditScreen(
                    cardId = -1, repository = repo, onSaved = {}, onBack = {},
                    prefilledCardNumber = "9999999", prefilledFormat = "EAN_13"
                )
            }
        }
        rule.onNodeWithText("9999999").assertIsDisplayed()
    }
}
