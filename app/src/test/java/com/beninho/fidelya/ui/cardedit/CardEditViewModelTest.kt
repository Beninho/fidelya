package com.beninho.fidelya.ui.cardedit

import app.cash.turbine.test
import com.beninho.fidelya.data.brand.BRAND_CATALOG
import com.beninho.fidelya.data.brand.Brand
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.color.nearestModernistColor
import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CardEditViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CardRepository
    private lateinit var logoStore: LogoStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        logoStore = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save with empty name sets error`() = runTest {
        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        // storeName is blank by default, cardNumber too — just call save
        vm.save()

        vm.uiState.test {
            val state = awaitItem()
            assertNotNull(state.storeNameError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `save valid card calls repository`() = runTest {
        whenever(repository.save(org.mockito.kotlin.any())).thenReturn(1L)
        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onStoreNameChange("Carrefour")
        vm.onCardNumberChange("1234567890")

        vm.save()

        val captor = argumentCaptor<LoyaltyCard>()
        verify(repository).save(captor.capture())
        assertEquals("Carrefour", captor.firstValue.storeName)
    }

    @Test
    fun `load existing card populates state`() = runTest {
        val card = LoyaltyCard(
            id = 5L,
            storeName = "Auchan",
            cardNumber = "9876543210",
            barcodeFormat = "EAN_13",
            backgroundColor = "#388E3C"
        )
        whenever(repository.getById(5L)).thenReturn(card)

        val vm = CardEditViewModel(repository, logoStore, cardId = 5L)
        advanceUntilIdle()  // wait for init coroutine to complete

        vm.uiState.test {
            val state = awaitItem()
            assertEquals("Auchan", state.storeName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private val existing = LoyaltyCard(
        id = 42L,
        storeName = "Carrefour",
        cardNumber = "1234567890",
        barcodeFormat = "EAN_13",
        backgroundColor = "#B71C1C"
    )

    @Test
    fun `prefilled number already taken raises the duplicate alert`() = runTest {
        whenever(repository.findDuplicate(eq("1234567890"), any())).thenReturn(existing)

        val vm = CardEditViewModel(
            repository, logoStore, cardId = -1L, prefilledCardNumber = "1234567890"
        )
        advanceUntilIdle()

        vm.uiState.test {
            assertEquals(existing, awaitItem().duplicate)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository, never()).save(any())
    }

    @Test
    fun `save on a duplicate opens the alert instead of writing`() = runTest {
        whenever(repository.findDuplicate(eq("1234567890"), any())).thenReturn(existing)

        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onStoreNameChange("Carrefour Market")
        vm.onCardNumberChange("1234567890")
        vm.save()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(existing, state.duplicate)
            assertFalse(state.isSaved)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository, never()).save(any())
    }

    @Test
    fun `accepting the duplicate saves anyway`() = runTest {
        whenever(repository.findDuplicate(eq("1234567890"), any())).thenReturn(existing)
        whenever(repository.save(any())).thenReturn(2L)

        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onStoreNameChange("Carrefour Market")
        vm.onCardNumberChange("1234567890")
        vm.save()
        advanceUntilIdle()
        vm.onDuplicateAccepted()
        advanceUntilIdle()

        val captor = argumentCaptor<LoyaltyCard>()
        verify(repository).save(captor.capture())
        assertEquals("Carrefour Market", captor.firstValue.storeName)
        vm.uiState.test {
            assertNull(awaitItem().duplicate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing the number cancels a previous acceptance`() = runTest {
        whenever(repository.findDuplicate(any(), any())).thenReturn(existing)

        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onStoreNameChange("Carrefour Market")
        vm.onCardNumberChange("1234567890")
        vm.save()
        advanceUntilIdle()
        vm.onDuplicateAccepted()
        advanceUntilIdle()

        // Un autre numéro, lui aussi déjà pris : l'alerte doit revenir.
        vm.onCardNumberChange("999")
        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.duplicate)
            assertFalse(state.duplicateAccepted)
            cancelAndIgnoreRemainingEvents()
        }

        vm.save()
        advanceUntilIdle()
        vm.uiState.test {
            assertEquals(existing, awaitItem().duplicate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editing a card excludes it from the duplicate search`() = runTest {
        whenever(repository.getById(42L)).thenReturn(existing)

        val vm = CardEditViewModel(repository, logoStore, cardId = 42L)
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()

        verify(repository).findDuplicate("1234567890", 42L)
        verify(repository).save(any())
    }

    @Test
    fun `deleting the other card resumes the interrupted save`() = runTest {
        whenever(repository.findDuplicate(any(), any())).thenReturn(existing, null)
        whenever(repository.save(any())).thenReturn(2L)

        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onStoreNameChange("Carrefour Market")
        vm.onCardNumberChange("1234567890")
        vm.save()
        advanceUntilIdle()
        vm.onDeleteDuplicate()
        advanceUntilIdle()

        verify(repository).delete(existing)
        verify(logoStore).delete(existing.logoUri)
        // L'enregistrement reprend tout seul : le doublon n'existe plus.
        verify(repository).save(any())
        vm.uiState.test {
            assertNull(awaitItem().duplicate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting the other card from the opening alert does not save`() = runTest {
        whenever(repository.findDuplicate(any(), any())).thenReturn(existing)

        val vm = CardEditViewModel(
            repository, logoStore, cardId = -1L, prefilledCardNumber = "1234567890"
        )
        advanceUntilIdle()
        vm.onDeleteDuplicate()
        advanceUntilIdle()

        verify(repository).delete(existing)
        verify(repository, never()).save(any())
    }

    @Test
    fun `deleting the edited card removes it and leaves the form`() = runTest {
        val edited = existing.copy(id = 7L, logoUri = "/logos/7.png")
        whenever(repository.getById(7L)).thenReturn(edited)
        whenever(repository.findDuplicate(any(), any())).thenReturn(existing)

        val vm = CardEditViewModel(repository, logoStore, cardId = 7L)
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        vm.onDeleteSelf()
        advanceUntilIdle()

        verify(repository).delete(edited)
        verify(logoStore).delete("/logos/7.png")
        verify(repository, never()).save(any())
        vm.uiState.test {
            assertTrue(awaitItem().isDeleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting the edited card is a no-op while creating`() = runTest {
        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onDeleteSelf()
        advanceUntilIdle()

        // Rien en base à supprimer : ni suppression, ni sortie du formulaire.
        verify(repository, never()).delete(any())
        vm.uiState.test {
            assertFalse(awaitItem().isDeleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `typing a store name suggests matching brands`() = runTest {
        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onStoreNameChange("carr")

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.brandSuggestions.any { it.name == "Carrefour" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting a brand fills the name, the colour and the bundled logo`() = runTest {
        val brand = Brand("Carrefour", "Carrefour Pass", "Grande distribution", 42, "#0D60A3")
        whenever(logoStore.storeResource(42)).thenReturn("/logos/carrefour.img")

        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onStoreNameChange("carr")
        vm.onBrandSelected(brand)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals("Carrefour", state.storeName)
            assertEquals("/logos/carrefour.img", state.logoPath)
            // La couleur du logo est ramenée sur un pas de la palette.
            assertEquals(nearestModernistColor("#0D60A3"), state.backgroundColor)
            // La liste se referme : l'enseigne est choisie.
            assertTrue(state.brandSuggestions.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting a brand replaces the previous logo file`() = runTest {
        val brand = BRAND_CATALOG.first()
        whenever(logoStore.storeResource(brand.logo)).thenReturn("/logos/new.img")
        whenever(repository.getById(7L)).thenReturn(existing.copy(id = 7L, logoUri = "/logos/old.img"))

        val vm = CardEditViewModel(repository, logoStore, cardId = 7L)
        advanceUntilIdle()
        vm.onBrandSelected(brand)
        advanceUntilIdle()

        verify(logoStore).delete("/logos/old.img")
    }

    @Test
    fun `a brand whose logo cannot be copied still fills the name`() = runTest {
        val brand = Brand("Casino", "Club Casino", "Grande distribution", 7, "#14712F")
        whenever(logoStore.storeResource(7)).thenReturn(null)

        val vm = CardEditViewModel(repository, logoStore, cardId = -1L)
        vm.onBrandSelected(brand)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals("Casino", state.storeName)
            assertNull(state.logoPath)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
