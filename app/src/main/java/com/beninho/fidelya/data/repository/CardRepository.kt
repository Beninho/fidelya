package com.beninho.fidelya.data.repository

import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun observeAll(): Flow<List<LoyaltyCard>>
    suspend fun getById(id: Long): LoyaltyCard?
    suspend fun save(card: LoyaltyCard): Long
    suspend fun delete(card: LoyaltyCard)
    suspend fun insertAll(cards: List<LoyaltyCard>)
    suspend fun getAll(): List<LoyaltyCard>

    /**
     * La carte qui porte déjà ce numéro, `excludeId` mis à part — la carte en
     * cours d'édition ne doit pas se signaler comme son propre doublon.
     */
    suspend fun findDuplicate(cardNumber: String, excludeId: Long = 0): LoyaltyCard?

    /** Horodate un passage en caisse — alimente « Dernier passage » sur le détail. */
    suspend fun markUsed(id: Long)
}
