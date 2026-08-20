package com.beninho.fidelya.domain.model

data class LoyaltyCard(
    val id: Long = 0,
    val storeName: String,
    val cardNumber: String,
    val barcodeFormat: String,
    val backgroundColor: String,
    val logoUri: String? = null,
    val logoEmoji: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Dernier affichage en caisse. `null` tant que la carte n'a jamais servi. */
    val lastUsedAt: Long? = null
)
