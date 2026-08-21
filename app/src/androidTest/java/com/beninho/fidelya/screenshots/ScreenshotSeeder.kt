package com.beninho.fidelya.screenshots

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beninho.fidelya.data.db.AppDatabase
import com.beninho.fidelya.data.db.LoyaltyCardEntity
import com.beninho.fidelya.data.settings.SettingsStoreImpl
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Utilitaire de capture, pas un test de comportement : remplit la base de
 * l'application avec un jeu de cartes présentable et marque l'onboarding vu,
 * pour que les captures du Play Store montrent des écrans peuplés.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotSeeder {

    @Test
    fun seed() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = AppDatabase.getInstance(context).loyaltyCardDao()
        dao.getAll().forEach { dao.delete(it) }

        val day = 24L * 60 * 60 * 1000
        val now = 1_755_000_000_000L
        listOf(
            card("Carrefour", "3012345678902", "EAN_13", "#0D60A3", "🛒", now - 40 * day, now - 2 * day),
            card("Decathlon", "7208453196", "CODE_128", "#00A9CE", "⚽", now - 120 * day, now - 9 * day),
            card("Fnac", "4419620573", "CODE_128", "#D18501", "📚", now - 200 * day, now - 24 * day),
            card("Biocoop", "8830147265", "CODE_128", "#14712F", "🌿", now - 65 * day, now - 4 * day),
            card("Monoprix", "5127096384", "CODE_128", "#DA69B9", "🥗", now - 15 * day, now - 6 * day),
            card("Sephora", "6294718035", "CODE_128", "#7B428D", "💄", now - 300 * day, now - 51 * day),
        ).forEach { dao.insert(it) }

        SettingsStoreImpl(context).markOnboardingSeen()
    }

    private fun card(
        store: String,
        number: String,
        format: String,
        color: String,
        emoji: String,
        createdAt: Long,
        lastUsedAt: Long
    ) = LoyaltyCardEntity(
        storeName = store,
        cardNumber = number,
        barcodeFormat = format,
        backgroundColor = color,
        logoUri = null,
        logoEmoji = emoji,
        createdAt = createdAt,
        updatedAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}
