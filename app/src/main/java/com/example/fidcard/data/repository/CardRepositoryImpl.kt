package com.example.fidcard.data.repository

import com.example.fidcard.data.db.LoyaltyCardDao
import com.example.fidcard.data.db.toDomain
import com.example.fidcard.data.db.toEntity
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CardRepositoryImpl(private val dao: LoyaltyCardDao) : CardRepository {
    override fun observeAll(): Flow<List<LoyaltyCard>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): LoyaltyCard? = dao.getById(id)?.toDomain()

    override suspend fun save(card: LoyaltyCard): Long {
        val entity = card.toEntity().copy(updatedAt = System.currentTimeMillis())
        return dao.insert(entity)
    }

    override suspend fun delete(card: LoyaltyCard) = dao.delete(card.toEntity())

    override suspend fun insertAll(cards: List<LoyaltyCard>) =
        dao.insertAll(cards.map { it.toEntity() })

    override suspend fun getAll(): List<LoyaltyCard> = observeAll().first()
}
