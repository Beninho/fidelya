package com.example.fidcard.backup

import android.content.Context
import android.net.Uri
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupCard(
    val id: Long = 0,
    val storeName: String,
    val cardNumber: String,
    val barcodeFormat: String,
    val backgroundColor: String,
    val logoUri: String? = null,
    val logoEmoji: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

fun LoyaltyCard.toBackup() = BackupCard(
    id, storeName, cardNumber, barcodeFormat, backgroundColor, logoUri, logoEmoji, createdAt, updatedAt
)

fun BackupCard.toDomain() = LoyaltyCard(
    id = 0, // reset ID so insertAll treats it as new
    storeName = storeName,
    cardNumber = cardNumber,
    barcodeFormat = barcodeFormat,
    backgroundColor = backgroundColor,
    logoUri = logoUri,
    logoEmoji = logoEmoji,
    createdAt = createdAt,
    updatedAt = updatedAt
)

object BackupManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun export(cards: List<LoyaltyCard>): String =
        json.encodeToString(cards.map { it.toBackup() })

    fun import(context: Context, uri: Uri): List<LoyaltyCard> {
        val content = context.contentResolver.openInputStream(uri)
            ?.use { it.bufferedReader().readText() }
            ?: error("Impossible de lire le fichier")
        return json.decodeFromString<List<BackupCard>>(content).map { it.toDomain() }
    }
}
