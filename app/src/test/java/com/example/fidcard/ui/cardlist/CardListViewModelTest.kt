package com.example.fidcard.ui.cardlist

import app.cash.turbine.test
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CardListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CardRepository
    private lateinit var vm: CardListViewModel

    private val sampleCard = LoyaltyCard(
        id = 1L,
        storeName = "Carrefour",
        cardNumber = "1234567890",
        barcodeFormat = "QR_CODE",
        backgroundColor = "#FF0000"
    )

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
    fun `emits cards from repository`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(sampleCard)))
        vm = CardListViewModel(repository)

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.cards.size)
            assertEquals(sampleCard, state.cards[0])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete card calls repository delete`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(emptyList()))
        vm = CardListViewModel(repository)

        vm.deleteCard(sampleCard)

        verify(repository).delete(sampleCard)
    }
}
