package com.beninho.fidelya.domain.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernistPaletteTest {

    /** L'ancienne palette, telle qu'elle était avant l'import du design system. */
    private val legacyPalette = listOf(
        "#E53935", "#E91E63", "#F06292",
        "#F57C00", "#FF7043",
        "#F9A825", "#FDD835",
        "#388E3C", "#43A047", "#00897B",
        "#1565C0", "#039BE5", "#00838F",
        "#5C6BC0", "#7B1FA2", "#9C27B0",
        "#4E342E", "#795548",
        "#37474F", "#546E7A",
        "#212121", "#FAFAFA"
    )

    private fun brightness(hex: String): Int {
        val (r, g, b) = checkNotNull(parseHexChannels(hex))
        return r + g + b
    }

    @Test
    fun paletteHasTwentyTwoDistinctSteps() {
        assertEquals(22, MODERNIST_CARD_COLORS.size)
        assertEquals(22, MODERNIST_CARD_COLORS.toSet().size)
    }

    @Test
    fun defaultColorBelongsToPalette() {
        assertTrue(DEFAULT_CARD_COLOR in MODERNIST_CARD_COLORS)
    }

    @Test
    fun everyPaletteStepMapsToItself() {
        MODERNIST_CARD_COLORS.forEach { hex ->
            assertEquals(hex, nearestModernistColor(hex))
        }
    }

    @Test
    fun everyLegacyColorMapsIntoThePalette() {
        legacyPalette.forEach { hex ->
            assertTrue(
                "$hex sort de la palette",
                nearestModernistColor(hex) in MODERNIST_CARD_COLORS
            )
        }
    }

    /** Le remappage doit être stable : réappliqué, il ne bouge plus. */
    @Test
    fun remappingIsIdempotent() {
        legacyPalette.forEach { hex ->
            val once = nearestModernistColor(hex)
            assertEquals(once, nearestModernistColor(once))
        }
    }

    /** La teinte est perdue, la valeur non : un fond clair reste clair. */
    @Test
    fun lightnessIsPreserved() {
        assertTrue(brightness(nearestModernistColor("#FFFFFF")) > 700)
        assertTrue(brightness(nearestModernistColor("#FAFAFA")) > 700)
        assertTrue(brightness(nearestModernistColor("#000000")) < 200)
        assertTrue(brightness(nearestModernistColor("#212121")) < 200)
    }

    @Test
    fun shorthandAndAlphaFormsAreAccepted() {
        assertEquals(nearestModernistColor("#FFFFFF"), nearestModernistColor("#FFF"))
        assertEquals(nearestModernistColor("#E53935"), nearestModernistColor("#FFE53935"))
    }

    @Test
    fun unreadableColorFallsBackToTheDefault() {
        assertEquals(DEFAULT_CARD_COLOR, nearestModernistColor(""))
        assertEquals(DEFAULT_CARD_COLOR, nearestModernistColor("rouge"))
        assertEquals(DEFAULT_CARD_COLOR, nearestModernistColor("#GGGGGG"))
        assertEquals(DEFAULT_CARD_COLOR, nearestModernistColor("#12345"))
    }
}
