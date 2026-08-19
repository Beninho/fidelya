package com.beninho.fidelya.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.beninho.fidelya.domain.color.MODERNIST_CARD_COLORS
import com.beninho.fidelya.domain.color.parseHexChannels

/**
 * Façade Compose de la palette de cartes. La liste et le calcul du pas le plus
 * proche vivent dans `domain.color.ModernistPalette`, hors de toute dépendance
 * Android, pour que la migration de base puisse s'en servir.
 */
val CardPalette: List<String> = MODERNIST_CARD_COLORS

/**
 * Modernist ne pose jamais de blanc pur sur une couleur : `.btn-primary` écrit
 * en `--color-bg` sur l'accent, le reste écrit en `--color-text`. On applique
 * la même règle, en basculant sur la luminance du fond.
 *
 * Le seuil 0.2 est le point où les deux options offrent le même contraste
 * (~3.8:1). Les fonds proches de ce seuil — les neutres 500 et 600 — ne
 * satisfont donc AA que pour le texte large : le nom du magasin (gras) passe,
 * le numéro en 12–14sp non.
 */
fun cardForegroundColor(background: Color): Color =
    if (background.luminance() > 0.2f) ModernistText else ModernistBg

/** Parse un fond de carte stocké en base, en retombant sur un neutre si le format est invalide. */
fun parseCardColor(hex: String): Color {
    val channels = parseHexChannels(hex) ?: return Neutral500
    val (r, g, b) = channels
    return Color(red = r, green = g, blue = b)
}
