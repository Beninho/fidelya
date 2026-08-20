package com.beninho.fidelya.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.beninho.fidelya.domain.color.nearestModernistColor

/**
 * Ajoute `lastUsedAt`, nullable : une carte déjà en base n'a pas d'historique de
 * passage, et `null` se lit « jamais » plutôt que « le 1er janvier 1970 ».
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE loyalty_cards ADD COLUMN lastUsedAt INTEGER")
    }
}

/**
 * Réaligne les fonds de carte déjà enregistrés sur la palette « Modernist ».
 *
 * Le schéma ne bouge pas : seules les valeurs de `backgroundColor` changent.
 * Le calcul du pas le plus proche se fait en OKLab, donc en Kotlin — SQLite ne
 * sait pas comparer des couleurs — d'où le parcours du curseur ligne à ligne.
 *
 * Une carte dont la couleur tombe déjà sur un pas de la palette n'est pas
 * réécrite : la migration est idempotente et ne touche que ce qui change.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val remapped = mutableListOf<Pair<Long, String>>()

        db.query("SELECT id, backgroundColor FROM loyalty_cards").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val current = cursor.getString(1) ?: continue
                val nearest = nearestModernistColor(current)
                if (nearest != current) remapped += id to nearest
            }
        }

        remapped.forEach { (id, color) ->
            db.execSQL("UPDATE loyalty_cards SET backgroundColor = ? WHERE id = ?", arrayOf<Any>(color, id))
        }
    }
}
