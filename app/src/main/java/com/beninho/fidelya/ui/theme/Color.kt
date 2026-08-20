package com.beninho.fidelya.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette « Modernist », importée du projet Claude Design du même nom.
 * Source de vérité : `styles.css` / `theme.json` de ce projet.
 *
 * Les rampes tonales sont générées en OKLCH sur une échelle de luminosité
 * partagée : le pas 500 de l'accent a la même valeur visuelle que le neutre 500.
 */

// Rôles de base
val ModernistBg = Color(0xFFF3F2F2)
val ModernistSurface = Color(0xFFEAE9E9)
val ModernistText = Color(0xFF201E1D)
val ModernistAccent = Color(0xFFEC3013)
val ModernistAccent2 = Color(0xFFE15B47)

// Rampe neutre
val Neutral100 = Color(0xFFF8F4F4)
val Neutral200 = Color(0xFFEAE7E7)
val Neutral300 = Color(0xFFD7D3D3)
val Neutral400 = Color(0xFFBAB6B6)
val Neutral500 = Color(0xFF9B9797)
val Neutral600 = Color(0xFF7D7979)
val Neutral700 = Color(0xFF605D5D)
val Neutral800 = Color(0xFF444141)
val Neutral900 = Color(0xFF2D2B2B)

// Rampe accent
val Accent100 = Color(0xFFFFF2EF)
val Accent200 = Color(0xFFFFE0D9)
val Accent300 = Color(0xFFFFC4B8)
val Accent400 = Color(0xFFFF9783)
val Accent500 = Color(0xFFFF563C)
val Accent600 = Color(0xFFDD2B0F)
val Accent700 = Color(0xFFAE1800)
val Accent800 = Color(0xFF7C1405)
val Accent900 = Color(0xFF4D170E)

// Rampe accent secondaire
val Accent2100 = Color(0xFFFFF2EF)
val Accent2200 = Color(0xFFFFE0DA)
val Accent2300 = Color(0xFFFFC4B9)
val Accent2400 = Color(0xFFFF9784)
val Accent2500 = Color(0xFFEF6853)
val Accent2600 = Color(0xFFC94B39)
val Accent2700 = Color(0xFF9E3526)
val Accent2800 = Color(0xFF71261B)
val Accent2900 = Color(0xFF471D16)

/**
 * Thème « Encre » — la variante sombre, spécifiée par la maquette
 * `Fidelya-refonte-encre.dc.html` (« Thème 2 — Encre »).
 *
 * Même structure, même parcours : seul le ton change. Le fond passe à l'encre,
 * les surfaces à #2B2928, et l'accent monte d'un cran sur la rampe (pas 500)
 * pour tenir le contraste sur foncé — l'encre sur l'accent 500 donne 5.26:1, du
 * texte clair seulement 2.83:1.
 *
 * Les rôles s'inversent : le fond du thème Encre est exactement l'encre de
 * texte du thème clair, et réciproquement. Ils sont nommés à part plutôt que
 * réutilisés en croix, pour qu'un appel n'ait pas l'air d'une erreur.
 */
val ModernistInkBg = Color(0xFF201E1D)
val ModernistInkSurface = Color(0xFF2B2928)
val ModernistInkText = Color(0xFFF3F2F2)

/**
 * Le fond des lignes de liste, légèrement détaché du fond de l'écran : blanc en
 * clair, la surface Encre en sombre. C'est ce qui donne à la liste dense sa
 * lisibilité en balayage.
 */
val ModernistRowSurface = Color(0xFFFFFFFF)
val ModernistInkRowSurface = ModernistInkSurface
