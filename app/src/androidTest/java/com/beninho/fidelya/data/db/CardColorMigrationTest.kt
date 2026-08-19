package com.beninho.fidelya.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.beninho.fidelya.domain.color.DEFAULT_CARD_COLOR
import com.beninho.fidelya.domain.color.nearestModernistColor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Vérifie le branchement de [MIGRATION_1_2] : que la migration s'exécute sur une
 * vraie base v1, que le schéma reste valide en v2 et que seule la couleur bouge.
 *
 * Le calcul de correspondance lui-même est couvert côté JVM par
 * `ModernistPaletteTest` — ici on ne teste que la plomberie SQL.
 */
@RunWith(AndroidJUnit4::class)
class CardColorMigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private fun insertCard(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        id: Long,
        storeName: String,
        color: String
    ) {
        db.execSQL(
            """
            INSERT INTO loyalty_cards
                (id, storeName, cardNumber, barcodeFormat, backgroundColor, logoUri, logoEmoji, createdAt, updatedAt)
            VALUES (?, ?, ?, ?, ?, NULL, NULL, ?, ?)
            """.trimIndent(),
            arrayOf(id, storeName, "123456789$id", "EAN_13", color, 1_700_000_000L, 1_700_000_000L)
        )
    }

    private fun colorsAfterMigration(): Map<String, String> {
        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)
        val colors = mutableMapOf<String, String>()
        db.query("SELECT storeName, backgroundColor FROM loyalty_cards").use { cursor ->
            while (cursor.moveToNext()) colors[cursor.getString(0)] = cursor.getString(1)
        }
        return colors
    }

    @Test
    fun legacyColorIsRemappedOntoThePalette() {
        helper.createDatabase(dbName, 1).use { db ->
            // Le bleu indigo de l'ancienne palette — aucune teinte équivalente dans Modernist.
            insertCard(db, 1L, "Carrefour", "#1565C0")
        }

        assertEquals(
            nearestModernistColor("#1565C0"),
            colorsAfterMigration()["Carrefour"]
        )
    }

    @Test
    fun colorAlreadyOnThePaletteIsLeftUntouched() {
        helper.createDatabase(dbName, 1).use { db ->
            insertCard(db, 1L, "Auchan", DEFAULT_CARD_COLOR)
        }

        assertEquals(DEFAULT_CARD_COLOR, colorsAfterMigration()["Auchan"])
    }

    @Test
    fun unreadableColorFallsBackToTheDefault() {
        helper.createDatabase(dbName, 1).use { db ->
            insertCard(db, 1L, "Lidl", "pas une couleur")
        }

        assertEquals(DEFAULT_CARD_COLOR, colorsAfterMigration()["Lidl"])
    }

    @Test
    fun everyRowSurvivesAndOnlyTheColorChanges() {
        helper.createDatabase(dbName, 1).use { db ->
            insertCard(db, 1L, "Carrefour", "#E53935")
            insertCard(db, 2L, "Auchan", "#00897B")
            insertCard(db, 3L, "Lidl", "#FAFAFA")
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)
        db.query("SELECT id, storeName, cardNumber, barcodeFormat, createdAt FROM loyalty_cards ORDER BY id")
            .use { cursor ->
                assertEquals(3, cursor.count)

                cursor.moveToNext()
                assertEquals(1L, cursor.getLong(0))
                assertEquals("Carrefour", cursor.getString(1))
                assertEquals("1234567891", cursor.getString(2))
                assertEquals("EAN_13", cursor.getString(3))
                assertEquals(1_700_000_000L, cursor.getLong(4))

                cursor.moveToNext()
                assertEquals("Auchan", cursor.getString(1))

                cursor.moveToNext()
                assertEquals("Lidl", cursor.getString(1))
            }
    }
}
