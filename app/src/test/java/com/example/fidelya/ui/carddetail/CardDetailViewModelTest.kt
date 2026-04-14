package com.example.fidelya.ui.carddetail

import app.cash.turbine.test
import com.example.fidelya.data.repository.CardRepository
import com.example.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailViewModelTest {

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
    fun `loads card by id`() = runTest {
        val card = LoyaltyCard(
            id = 1L,
            storeName = "Carrefour",
            cardNumber = "1234567890",
            barcodeFormat = "QR_CODE",
            backgroundColor = "#1565C0"
        )
        whenever(repository.getById(1L)).thenReturn(card)

        val vm = CardDetailViewModel(repository, cardId = 1L)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals("Carrefour", state.card?.storeName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete calls repository then sets deleted flag`() = runTest {
        val card = LoyaltyCard(
            id = 2L,
            storeName = "Leclerc",
            cardNumber = "9876543210",
            barcodeFormat = "EAN_13",
            backgroundColor = "#2E7D32"
        )
        whenever(repository.getById(2L)).thenReturn(card)

        val vm = CardDetailViewModel(repository, cardId = 2L)
        advanceUntilIdle()

        vm.deleteCard()
        advanceUntilIdle()

        verify(repository).delete(card)

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.isDeleted)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
