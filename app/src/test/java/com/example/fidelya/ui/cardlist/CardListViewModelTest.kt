package com.example.fidelya.ui.cardlist

import app.cash.turbine.test
import com.example.fidelya.data.order.CardOrderStore
import com.example.fidelya.data.repository.CardRepository
import com.example.fidelya.domain.model.LoyaltyCard
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
    private lateinit var orderStore: CardOrderStore
    private lateinit var vm: CardListViewModel

    private val cardA = LoyaltyCard(
        id = 1L, storeName = "Carrefour", cardNumber = "1234567890",
        barcodeFormat = "QR_CODE", backgroundColor = "#FF0000"
    )
    private val cardB = LoyaltyCard(
        id = 2L, storeName = "Leclerc", cardNumber = "0987654321",
        barcodeFormat = "QR_CODE", backgroundColor = "#0000FF"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        orderStore = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits cards in original order when no saved order`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(cardA, cardB)))
        whenever(orderStore.orderFlow).thenReturn(flowOf(emptyList()))
        vm = CardListViewModel(repository, orderStore)

        vm.uiState.test {
            assertEquals(listOf(cardA, cardB), awaitItem().cards)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `applies saved order when orderFlow has ids`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(cardA, cardB)))
        whenever(orderStore.orderFlow).thenReturn(flowOf(listOf(2L, 1L)))
        vm = CardListViewModel(repository, orderStore)

        vm.uiState.test {
            assertEquals(listOf(cardB, cardA), awaitItem().cards)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `new card not in saved order appears at end`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(cardA, cardB)))
        whenever(orderStore.orderFlow).thenReturn(flowOf(listOf(1L))) // only cardA in saved order
        vm = CardListViewModel(repository, orderStore)

        vm.uiState.test {
            assertEquals(listOf(cardA, cardB), awaitItem().cards)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMove saves new order to store`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(listOf(cardA, cardB)))
        whenever(orderStore.orderFlow).thenReturn(flowOf(emptyList()))
        vm = CardListViewModel(repository, orderStore)

        vm.uiState.test {
            awaitItem() // consume initial state: [cardA, cardB]
            vm.onMove(from = 0, to = 1)
            verify(orderStore).save(listOf(2L, 1L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete card calls repository delete`() = runTest {
        whenever(repository.observeAll()).thenReturn(flowOf(emptyList()))
        whenever(orderStore.orderFlow).thenReturn(flowOf(emptyList()))
        vm = CardListViewModel(repository, orderStore)

        vm.deleteCard(cardA)

        verify(repository).delete(cardA)
    }
}
