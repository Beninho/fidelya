package com.example.fidcard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.fidcard.domain.model.LoyaltyCard

@Entity(tableName = "loyalty_cards")
data class LoyaltyCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storeName: String,
    val cardNumber: String,
    val barcodeFormat: String,
    val backgroundColor: String,
    val logoUri: String?,
    val logoEmoji: String?,
    val createdAt: Long,
    val updatedAt: Long
)

fun LoyaltyCardEntity.toDomain() = LoyaltyCard(
    id, storeName, cardNumber, barcodeFormat, backgroundColor, logoUri, logoEmoji, createdAt, updatedAt
)

fun LoyaltyCard.toEntity() = LoyaltyCardEntity(
    id, storeName, cardNumber, barcodeFormat, backgroundColor, logoUri, logoEmoji, createdAt, updatedAt
)
