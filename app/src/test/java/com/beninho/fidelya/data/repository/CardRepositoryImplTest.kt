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
    fun `save keeps lastUsedAt of an edited card`() = runTest {
        val id = repository.save(makeCard(storeName = "Decathlon"))
        repository.markUsed(id)

        val used = repository.getById(id)!!
        assertNotNull("markUsed should stamp lastUsedAt", used.lastUsedAt)

        // Le formulaire d'édition renvoie une carte sans lastUsedAt : le dépôt
        // doit le reprendre en base plutôt que de l'effacer.
        repository.save(used.copy(storeName = "Decathlon Nantes", lastUsedAt = null))

        val edited = repository.getById(id)!!
        assertEquals("Decathlon Nantes", edited.storeName)
        assertEquals(used.lastUsedAt, edited.lastUsedAt)
    }

    @Test
    fun `findDuplicate finds a card by its number`() = runTest {
        val id = repository.save(makeCard(storeName = "Fnac", cardNumber = "FNAC-77"))

        val dup = repository.findDuplicate("FNAC-77")
        assertNotNull(dup)
        assertEquals(id, dup!!.id)
        assertEquals("Fnac", dup.storeName)
    }

    @Test
    fun `findDuplicate returns null for an unknown number`() = runTest {
        repository.save(makeCard(cardNumber = "FNAC-77"))

        assertNull(repository.findDuplicate("AUTRE-99"))
    }

    @Test
    fun `findDuplicate ignores the excluded card`() = runTest {
        val id = repository.save(makeCard(cardNumber = "FNAC-77"))

        // Le cas de l'édition : la carte modifiée ne doit pas se signaler elle-même.
        assertNull(repository.findDuplicate("FNAC-77", excludeId = id))
    }

    @Test
    fun `findDuplicate tolerates surrounding spaces`() = runTest {
        repository.save(makeCard(cardNumber = "FNAC-77"))

        assertNotNull(repository.findDuplicate("  FNAC-77  "))
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
