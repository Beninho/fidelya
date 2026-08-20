package com.beninho.fidelya.domain.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ModernistPaletteTest {

    /** L'ancienne palette, telle qu'elle était avant l'import du design system. */
    private val legacyMaterialPalette = listOf(
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

    private val grid = CARD_COLOR_ROWS.flatten()

    private fun brightness(hex: String): Int {
        val (r, g, b) = checkNotNull(parseHexChannels(hex))
        return r + g + b
    }

    // ---- forme de la grille ------------------------------------------------

    @Test
    fun gridHasOneRowPerHueFamily() {
        assertEquals(9, CARD_COLOR_ROWS.size)
        // Huit familles chromatiques à quatre pas, plus les neutres qui gardent
        // en plus leur quasi-blanc.
        assertEquals(List(8) { 4 } + listOf(5), CARD_COLOR_ROWS.map { it.size })
        assertEquals(37, grid.size)
        assertEquals(37, grid.toSet().size)
    }

    @Test
    fun everyGridStepIsAReadableHexColor() {
        grid.forEach { hex ->
            assertTrue("$hex n'est pas une couleur exploitable", parseHexChannels(hex) != null)
            assertTrue("$hex n'est pas écrit en #RRGGBB", Regex("^#[0-9A-F]{6}$").matches(hex))
        }
    }

    @Test
    fun defaultColorIsPickableInTheGrid() {
        assertTrue(DEFAULT_CARD_COLOR in grid)
    }

    // ---- jeu valide -------------------------------------------------------

    @Test
    fun validSetIsTheGridPlusTheMonochromeLegacy() {
        assertEquals(53, MODERNIST_CARD_COLORS.size)
        assertEquals(MODERNIST_CARD_COLORS.size, MODERNIST_CARD_COLORS.toSet().size)
        assertTrue(MODERNIST_CARD_COLORS.containsAll(grid))
    }

    /**
     * Le garde-fou qui dispense d'une migration v2 → v3 : un fond enregistré du
     * temps du sélecteur monochrome doit rester une valeur acceptée.
     */
    @Test
    fun everyLegacyStepRemainsValid() {
        LEGACY_CARD_COLORS.forEach { hex ->
            assertTrue("$hex n'est plus une valeur acceptée", hex in MODERNIST_CARD_COLORS)
        }
    }

    // ---- remappage --------------------------------------------------------

    @Test
    fun everyGridStepMapsToItself() {
        grid.forEach { hex ->
            assertEquals(hex, nearestModernistColor(hex))
        }
    }

    @Test
    fun remappingOnlyEverTargetsTheGrid() {
        (legacyMaterialPalette + LEGACY_CARD_COLORS).forEach { hex ->
            assertTrue(
                "$hex sort de la grille",
                nearestModernistColor(hex) in grid
            )
        }
    }

    /** Le remappage doit être stable : réappliqué, il ne bouge plus. */
    @Test
    fun remappingIsIdempotent() {
        (legacyMaterialPalette + LEGACY_CARD_COLORS).forEach { hex ->
            val once = nearestModernistColor(hex)
            assertEquals(once, nearestModernistColor(once))
        }
    }

    /**
     * Ce que la grille multi-teintes apporte par rapport à la palette
     * monochrome : la teinte d'origine survit au remappage.
     */
    @Test
    fun hueSurvivesTheRemapping() {
        val expected = mapOf(
            "#1565C0" to "#0D60A3", // bleu   -> bleu
            "#039BE5" to "#319CFC", // cyan   -> bleu
            "#388E3C" to "#14712F", // vert   -> vert
            "#00897B" to "#006C6A", // teal   -> turquoise
            "#7B1FA2" to "#644B9E", // violet -> violet
            "#F06292" to "#DA69B9", // rose   -> magenta
            "#F57C00" to "#D18501", // orange -> ambre
            "#FDD835" to "#D7DA94"  // jaune  -> olive
        )
        expected.forEach { (from, to) ->
            assertEquals("$from mal remappé", to, nearestModernistColor(from))
        }
    }

    /** La valeur est préservée : un fond clair reste clair, un fond sombre sombre. */
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

    // ---- contraste --------------------------------------------------------

    /**
     * Reprend la règle de `ui.theme.cardForegroundColor` — encre sombre au delà
     * de 0.2 de luminance, claire en dessous — sans dépendre de Compose. Si la
     * grille ou le seuil bougent, ce test le voit avant l'utilisateur.
     */
    private val inkDark = "#201E1D"   // ModernistText
    private val inkLight = "#F3F2F2"  // ModernistBg

    private fun relativeLuminance(hex: String): Double {
        val (r, g, b) = checkNotNull(parseHexChannels(hex))
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private fun contrast(a: String, b: String): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun everyGridStepClearsAaWithTheInkTheRulePicks() {
        grid.forEach { hex ->
            val ink = if (relativeLuminance(hex) > 0.2) inkDark else inkLight
            val ratio = contrast(hex, ink)
            assertTrue("$hex n'atteint que ${"%.2f".format(ratio)}:1", ratio >= 4.5)
        }
    }

    /** Le seuil doit toujours désigner la meilleure des deux encres. */
    @Test
    fun theThresholdPicksTheBetterInk() {
        grid.forEach { hex ->
            val ink = if (relativeLuminance(hex) > 0.2) inkDark else inkLight
            val best = maxOf(contrast(hex, inkDark), contrast(hex, inkLight))
            assertEquals("mauvaise encre sur $hex", best, contrast(hex, ink), 1e-9)
        }
    }
}
