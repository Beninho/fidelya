package com.beninho.fidelya.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {
    @Query("SELECT * FROM loyalty_cards ORDER BY storeName ASC")
    fun observeAll(): Flow<List<LoyaltyCardEntity>>

    @Query("SELECT * FROM loyalty_cards ORDER BY storeName ASC")
    suspend fun getAll(): List<LoyaltyCardEntity>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id")
    suspend fun getById(id: Long): LoyaltyCardEntity?

    /**
     * La première carte portant ce numéro, `excludeId` mis à part.
     *
     * `TRIM` des deux côtés : le formulaire trime avant l'insert, mais une carte
     * reçue par partage arrive telle quelle du codec.
     */
    @Query(
        "SELECT * FROM loyalty_cards " +
            "WHERE TRIM(cardNumber) = TRIM(:cardNumber) AND id != :excludeId LIMIT 1"
    )
    suspend fun findByCardNumber(cardNumber: String, excludeId: Long = 0): LoyaltyCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: LoyaltyCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<LoyaltyCardEntity>)

    @Update
    suspend fun update(card: LoyaltyCardEntity)

    @Delete
    suspend fun delete(card: LoyaltyCardEntity)

    @Query("UPDATE loyalty_cards SET lastUsedAt = :at WHERE id = :id")
    suspend fun markUsed(id: Long, at: Long)
}
