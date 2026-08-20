package com.beninho.fidelya.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.beninho.fidelya.domain.color.CARD_COLOR_ROWS
import com.beninho.fidelya.domain.color.parseHexChannels

/**
 * Façade Compose de la palette de cartes. La grille et le calcul du pas le plus
 * proche vivent dans `domain.color.ModernistPalette`, hors de toute dépendance
 * Android, pour que la migration de base puisse s'en servir.
 */
val CardPaletteRows: List<List<String>> = CARD_COLOR_ROWS

/**
 * Modernist ne pose jamais de blanc pur sur une couleur : `.btn-primary` écrit
 * en `--color-bg` sur l'accent, le reste écrit en `--color-text`. On applique
 * la même règle, en basculant sur la luminance du fond.
 *
 * Le seuil 0.2 est le point où les deux options offrent le même contraste
 * (~3.85:1). Les 37 pas de la grille l'évitent par construction et tiennent
 * tous AA en texte normal (pire cas 5.3:1).
 *
 * Restent en dessous les fonds hérités de la première palette monochrome, que
 * des cartes existantes portent encore : les neutres 500 et 600 à 3.85:1 et
 * l'ancien accent #DD2B0F à 4.25:1. C'est pour eux que le texte posé sur une
 * carte est en gras et à 14sp au moins, seuil « texte large » de WCAG (cf.
 * CardListScreen et CardDetailScreen).
 */
fun cardForegroundColor(background: Color): Color =
    if (background.luminance() > 0.2f) ModernistText else ModernistBg

/** Parse un fond de carte stocké en base, en retombant sur un neutre si le format est invalide. */
fun parseCardColor(hex: String): Color {
    val channels = parseHexChannels(hex) ?: return Neutral500
    val (r, g, b) = channels
    return Color(red = r, green = g, blue = b)
}
