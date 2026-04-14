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
}
