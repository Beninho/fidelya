package com.example.fidcard.ui.cardedit

import app.cash.turbine.test
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CardEditViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CardRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save with empty name sets error`() = runTest {
        val vm = CardEditViewModel(repository, cardId = -1L)
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
        val vm = CardEditViewModel(repository, cardId = -1L)
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

        val vm = CardEditViewModel(repository, cardId = 5L)

        vm.uiState.test {
            val state = awaitItem()
            assertEquals("Auchan", state.storeName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
