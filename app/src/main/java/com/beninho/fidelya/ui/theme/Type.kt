package com.beninho.fidelya.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.beninho.fidelya.R

/**
 * Typographie « Modernist », importée du projet Claude Design du même nom.
 *
 * Archivo (grotesque) pour les titres comme pour le texte courant ; les titres
 * sont en 800 avec un interlettrage resserré (-0.015em) et une interligne de
 * 1.12, le corps de texte en 400 avec une interligne de 1.55.
 */

/**
 * Archivo est embarquée sous forme de police variable (axes `wdth`/`wght`) : pas
 * de dépendance aux polices téléchargeables de Play Services, donc un rendu
 * identique hors ligne et dès le premier affichage.
 */
@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: FontWeight) = Font(
    resId = R.font.archivo,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val Archivo = FontFamily(
    archivo(FontWeight.Normal),
    archivo(FontWeight.SemiBold),
    archivo(FontWeight.ExtraBold)
)

/** Poids des titres du design system (`--font-heading-weight: 800`). */
private val HeadingWeight = FontWeight.ExtraBold

private fun heading(size: Int, tracking: Double) = TextStyle(
    fontFamily = Archivo,
    fontWeight = HeadingWeight,
    fontSize = size.sp,
    lineHeight = (size * 1.12).sp,
    letterSpacing = tracking.sp
)

private fun body(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Archivo,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * 1.55).sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    // h1 / h2 du design system
    displayLarge = heading(42, -0.63),
    displayMedium = heading(36, -0.54),
    displaySmall = heading(32, -0.48),

    // h2 / h3
    headlineLarge = heading(32, -0.48),
    headlineMedium = heading(28, -0.42),
    headlineSmall = heading(25, -0.38),

    // h4 / .card-title / h5
    titleLarge = heading(20, -0.30),
    titleMedium = heading(17, -0.26),
    titleSmall = heading(16, -0.24),

    // corps de texte (15px de base), .card-body, .figcaption
    bodyLarge = body(15),
    bodyMedium = body(14),
    bodySmall = body(13),

    // .btn (police et poids des titres), label de .field, .tag / h6 en capitales
    labelLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = HeadingWeight,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.88.sp
    )
)
