package com.beninho.fidelya.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * La v3 n'ajoute qu'une colonne, mais elle l'ajoute à une table qui contient
 * déjà les cartes de l'utilisateur : ce qui compte est qu'aucune ligne ne bouge
 * et que `lastUsedAt` arrive à `null` — « jamais utilisée », pas « 1970 ».
 */
class LastUsedMigrationTest {

    private val dbName = "migration-2-3-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun lastUsedArrivesNullAndEveryRowSurvives() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO loyalty_cards
                    (id, storeName, cardNumber, barcodeFormat, backgroundColor, logoUri, logoEmoji, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, NULL, NULL, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(1L, "Carrefour", "1234567891", "EAN_13", "#EE6952", 1_700_000_000L, 1_700_000_000L)
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        db.query("SELECT storeName, createdAt, lastUsedAt FROM loyalty_cards").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToNext()
            assertEquals("Carrefour", cursor.getString(0))
            assertEquals(1_700_000_000L, cursor.getLong(1))
            assertTrue("lastUsedAt devrait être null", cursor.isNull(2))
        }
    }
}
