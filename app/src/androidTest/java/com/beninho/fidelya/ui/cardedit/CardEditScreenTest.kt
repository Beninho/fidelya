package com.beninho.fidelya.ui.cardedit

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.ui.theme.FidelyaTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class CardEditScreenTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var repo: CardRepository
    private lateinit var logoStore: LogoStore

    @Before fun setUp() { repo = mock(); logoStore = mock() }

    @Test fun saveWithEmptyNameShowsError() {
        rule.setContent {
            FidelyaTheme {
                CardEditScreen(
                    cardId = -1, repository = repo, logoStore = logoStore,
                    onSaved = {}, onBack = {}
                )
            }
        }
        rule.onNodeWithText("Enregistrer").performClick()
        rule.onNodeWithText("Le nom est obligatoire").assertIsDisplayed()
        rule.onNodeWithText("Le numéro est obligatoire").assertIsDisplayed()
    }

    @Test fun prefilledCardNumberAppearsInField() {
        rule.setContent {
            FidelyaTheme {
                CardEditScreen(
                    cardId = -1, repository = repo, logoStore = logoStore,
                    onSaved = {}, onBack = {},
                    prefilledCardNumber = "9999999", prefilledFormat = "EAN_13"
                )
            }
        }
        rule.onNodeWithText("9999999").assertIsDisplayed()
    }
}
