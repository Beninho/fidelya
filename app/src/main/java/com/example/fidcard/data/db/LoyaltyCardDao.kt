package com.example.fidcard.data.db

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: LoyaltyCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<LoyaltyCardEntity>)

    @Update
    suspend fun update(card: LoyaltyCardEntity)

    @Delete
    suspend fun delete(card: LoyaltyCardEntity)
}
