package com.beninho.fidelya.data.repository

import com.beninho.fidelya.data.db.LoyaltyCardDao
import com.beninho.fidelya.data.db.toDomain
import com.beninho.fidelya.data.db.toEntity
import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardRepositoryImpl(private val dao: LoyaltyCardDao) : CardRepository {
    override fun observeAll(): Flow<List<LoyaltyCard>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): LoyaltyCard? = dao.getById(id)?.toDomain()

    override suspend fun save(card: LoyaltyCard): Long {
        val now = System.currentTimeMillis()
        val entity = if (card.id > 0) {
            // Update: preserve createdAt and lastUsedAt from DB. Le formulaire
            // d'édition ne connaît ni l'un ni l'autre et les renverrait à zéro.
            val existing = dao.getById(card.id)
            card.toEntity().copy(
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                lastUsedAt = existing?.lastUsedAt
            )
        } else {
            // Insert: set both timestamps
            card.toEntity().copy(createdAt = now, updatedAt = now)
        }
        return dao.insert(entity)
    }

    override suspend fun delete(card: LoyaltyCard) = dao.delete(card.toEntity())

    override suspend fun insertAll(cards: List<LoyaltyCard>) =
        dao.insertAll(cards.map { it.toEntity() })

    override suspend fun getAll(): List<LoyaltyCard> = dao.getAll().map { it.toDomain() }

    override suspend fun markUsed(id: Long) = dao.markUsed(id, System.currentTimeMillis())
}
