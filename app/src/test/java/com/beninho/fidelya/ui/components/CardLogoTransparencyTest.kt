package com.beninho.fidelya.ui.components

import android.graphics.Bitmap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.graphics.Color as AndroidColor

/**
 * La détection de transparence d'un logo. Elle décide du cadrage — un glyphe
 * cadré en `Crop` perd ses bords — et son résultat ne se lit pas dans l'arbre
 * Compose : on l'éprouve donc directement.
 */
@RunWith(RobolectricTestRunner::class)
class CardLogoTransparencyTest {

    /**
     * Un carré de [side] px : un cadre transparent, et au centre un glyphe de la
     * couleur donnée occupant le quart de la surface. La forme d'un logo
     * d'enseigne tel qu'on le reçoit.
     */
    private fun glyphOnTransparent(glyph: Int, side: Int = 64): Bitmap {
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val from = side / 4
        val to = side * 3 / 4
        for (y in from until to) {
            for (x in from until to) bitmap.setPixel(x, y, glyph)
        }
        return bitmap
    }

    private fun filled(color: Int, side: Int = 64): Bitmap =
        Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    @Test
    fun anOpaqueImageIsNotTransparent() {
        assertFalse(filled(AndroidColor.RED).hasTransparentPixels())
    }

    /** Le cas courant : un glyphe, quelle que soit sa teinte. */
    @Test
    fun aGlyphOnTransparentIsDetected() {
        assertTrue(glyphOnTransparent(AndroidColor.BLACK).hasTransparentPixels())
        assertTrue(glyphOnTransparent(AndroidColor.WHITE).hasTransparentPixels())
    }

    @Test
    fun anEmptyImageIsTransparent() {
        assertTrue(Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).hasTransparentPixels())
    }

    /**
     * Le cadrage ne se décide pas sur le canal alpha déclaré : un bitmap
     * ARGB_8888 entièrement peint est opaque, quoi qu'en dise `hasAlpha`.
     */
    @Test
    fun aFullyPaintedArgbBitmapCountsAsOpaque() {
        val bitmap = filled(AndroidColor.WHITE)
        assertTrue(bitmap.hasAlpha())
        assertFalse(bitmap.hasTransparentPixels())
    }

    /** L'échantillonnage doit tenir quelle que soit la taille du fichier choisi. */
    @Test
    fun samplingHoldsOnAPhotoSizedImage() {
        assertTrue(glyphOnTransparent(AndroidColor.BLACK, side = 1200).hasTransparentPixels())
        assertFalse(filled(AndroidColor.RED, side = 1200).hasTransparentPixels())
    }
}
