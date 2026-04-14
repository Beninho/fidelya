package com.beninho.fidelya.data.repository

import androidx.room.Room
import com.beninho.fidelya.data.db.AppDatabase
import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CardRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: CardRepositoryImpl

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CardRepositoryImpl(db.loyaltyCardDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeCard(
        storeName: String = "Test Store",
        cardNumber: String = "123456",
        barcodeFormat: String = "QR_CODE",
        backgroundColor: String = "#FFFFFF"
    ) = LoyaltyCard(
        storeName = storeName,
        cardNumber = cardNumber,
        barcodeFormat = barcodeFormat,
        backgroundColor = backgroundColor
    )

    @Test
    fun `save and observe card`() = runTest {
        val card = makeCard(storeName = "Starbucks", cardNumber = "SBUX-001")
        repository.save(card)

        val cards = repository.observeAll().first()
        assertEquals(1, cards.size)
        assertEquals("Starbucks", cards[0].storeName)
        assertEquals("SBUX-001", cards[0].cardNumber)
    }

    @Test
    fun `delete card removes it`() = runTest {
        val card = makeCard(storeName = "Nike", cardNumber = "NIKE-001")
        val id = repository.save(card)

        val savedCard = repository.getById(id)
        assertNotNull("Card should not be null before delete", savedCard)
        assertEquals("Nike", savedCard!!.storeName)

        repository.delete(savedCard)

        val deletedCard = repository.getById(id)
        assertNull("Card should be null after delete", deletedCard)
    }

    @Test
    fun `insertAll bulk inserts correctly`() = runTest {
        val cards = listOf(
            makeCard(storeName = "Adidas", cardNumber = "AD-001"),
            makeCard(storeName = "Puma", cardNumber = "PU-001")
        )
        repository.insertAll(cards)

        val allCards = repository.observeAll().first()
        assertEquals(2, allCards.size)
    }
}
