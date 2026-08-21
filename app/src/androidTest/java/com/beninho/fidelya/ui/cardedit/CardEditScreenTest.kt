package com.beninho.fidelya.ui.cardedit

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.model.LoyaltyCard
import com.beninho.fidelya.ui.theme.FidelyaTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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

    @Test fun duplicateAlertOffersToSaveAnyway() {
        val existing = LoyaltyCard(
            id = 7L, storeName = "Carrefour", cardNumber = "9999999",
            barcodeFormat = "EAN_13", backgroundColor = "#B71C1C"
        )
        runBlocking { whenever(repo.findDuplicate(any(), any())).thenReturn(existing) }

        rule.setContent {
            FidelyaTheme {
                CardEditScreen(
                    cardId = -1, repository = repo, logoStore = logoStore,
                    onSaved = {}, onBack = {},
                    prefilledCardNumber = "9999999", prefilledFormat = "EAN_13"
                )
            }
        }

        // Le numéro arrive du scan : l'alerte s'affiche sans attendre le save.
        rule.onNodeWithText("Carte déjà enregistrée").assertIsDisplayed()
        rule.onNodeWithText("Voir la carte existante").assertIsDisplayed()

        // En création, rien en base à supprimer : l'action n'est pas proposée.
        rule.onNodeWithText("Supprimer cette carte").assertDoesNotExist()
        rule.onNodeWithText("Supprimer l'autre carte").assertIsDisplayed()

        rule.onNodeWithText("Annuler").performClick()
        rule.onNodeWithText("Carte déjà enregistrée").assertDoesNotExist()

        // Le libellé de `ModernistTextField` est un `Text` à part : viser son texte
        // attraperait l'étiquette, pas la zone de saisie. Ordre des champs de
        // l'écran : nom, numéro, emoji.
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("Carrefour Market")
        rule.onNodeWithText("Enregistrer").performClick()
        rule.onNodeWithText("Enregistrer quand même").performClick()

        runBlocking { verify(repo).save(any()) }
    }

    @Test fun duplicateAlertOnEditCanDeleteEitherCard() {
        val edited = LoyaltyCard(
            id = 7L, storeName = "Carrefour", cardNumber = "9999999",
            barcodeFormat = "EAN_13", backgroundColor = "#B71C1C"
        )
        val other = edited.copy(id = 8L, storeName = "Carrefour Ancienne")
        runBlocking {
            whenever(repo.getById(7L)).thenReturn(edited)
            whenever(repo.findDuplicate(any(), any())).thenReturn(other)
        }

        rule.setContent {
            FidelyaTheme {
                CardEditScreen(
                    cardId = 7L, repository = repo, logoStore = logoStore,
                    onSaved = {}, onBack = {}
                )
            }
        }

        rule.onNodeWithText("Enregistrer").performClick()
        rule.onNodeWithText("Supprimer l'autre carte").assertIsDisplayed()
        rule.onNodeWithText("Supprimer cette carte").performClick()

        runBlocking { verify(repo).delete(edited) }
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

    @Test fun typingAStoreNameSuggestsBundledBrands() {
        runBlocking { whenever(logoStore.storeResource(any())).thenReturn(null) }

        rule.setContent {
            FidelyaTheme {
                CardEditScreen(
                    cardId = -1, repository = repo, logoStore = logoStore,
                    onSaved = {}, onBack = {}
                )
            }
        }

        // Premier champ de l'écran : le nom du magasin.
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("carre")
        rule.onNodeWithText("Carrefour").assertIsDisplayed()
        rule.onNodeWithText("Carrefour Pass").assertIsDisplayed()

        rule.onNodeWithText("Carrefour").performClick()

        // L'enseigne choisie remplit le nom, et la liste se referme.
        rule.onNodeWithText("Carrefour Pass").assertDoesNotExist()
        rule.onAllNodes(hasSetTextAction())[0].assertTextEquals("Carrefour")
        // C'est bien le logo embarqué de l'enseigne qu'on est allé chercher.
        runBlocking { verify(logoStore).storeResource(com.beninho.fidelya.R.drawable.brand_carrefour) }
    }

    @Test fun anUnknownStoreNameSuggestsNothing() {
        rule.setContent {
            FidelyaTheme {
                CardEditScreen(
                    cardId = -1, repository = repo, logoStore = logoStore,
                    onSaved = {}, onBack = {}
                )
            }
        }

        rule.onAllNodes(hasSetTextAction())[0].performTextInput("Boulangerie du coin")
        rule.onNodeWithText("Carrefour Pass").assertDoesNotExist()
    }
}
