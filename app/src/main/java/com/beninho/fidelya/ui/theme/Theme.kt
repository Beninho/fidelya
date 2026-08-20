package com.beninho.fidelya.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Schéma clair : le fond, la surface et le texte reprennent tels quels les
 * rôles de base du design system ; les conteneurs et les contours sont pris
 * dans les rampes tonales (`.tag-accent` = accent-100 sur accent-800, etc.).
 */
private val LightColorScheme = lightColorScheme(
    primary = ModernistAccent,
    onPrimary = ModernistBg,
    primaryContainer = Accent100,
    onPrimaryContainer = Accent800,
    inversePrimary = Accent300,

    secondary = ModernistAccent2,
    onSecondary = ModernistBg,
    secondaryContainer = Accent2100,
    onSecondaryContainer = Accent2800,

    tertiary = Neutral700,
    onTertiary = Neutral100,
    tertiaryContainer = Neutral200,
    onTertiaryContainer = Neutral800,

    background = ModernistBg,
    onBackground = ModernistText,
    surface = ModernistBg,
    onSurface = ModernistText,
    surfaceVariant = ModernistSurface,
    onSurfaceVariant = Neutral700,
    surfaceTint = ModernistAccent,

    surfaceContainerLowest = ModernistBg,
    surfaceContainerLow = Neutral100,
    surfaceContainer = ModernistSurface,
    surfaceContainerHigh = Neutral200,
    surfaceContainerHighest = Neutral300,

    // `--color-divider` vaut l'encre à 40 %, soit #9F9D9D une fois aplatie sur
    // `--color-bg` : le neutre 500 en est à 9 unités, le 600 à 61. Les filets qui
    // peuvent rester translucides passent par `modernistDividerColor()`, qui garde
    // l'alpha et fonctionne donc sur n'importe quel fond.
    outline = Neutral500,
    outlineVariant = Neutral300,

    inverseSurface = Neutral900,
    inverseOnSurface = Neutral100,
    scrim = Neutral900
)

/**
 * Schéma « Encre », porté depuis la maquette du même nom.
 *
 * L'accent est le pas 500 et son encre est le fond du thème clair : la maquette
 * écrit `background: var(--color-accent-500); color: #201e1d`, et le calcul le
 * confirme — 5.26:1 contre 2.83:1 avec du texte clair.
 *
 * `outline` vaut le neutre 700 : c'est l'équivalent opaque du filet de la
 * maquette (le texte à 28 % sur l'encre donne #5B5959, à 7 unités du neutre
 * 700). Les filets qui peuvent rester translucides passent par
 * `modernistDividerColor()`, dont l'alpha suit le thème.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Accent500,
    onPrimary = ModernistInkBg,
    primaryContainer = Accent800,
    onPrimaryContainer = Accent200,
    inversePrimary = Accent600,

    secondary = Accent2500,
    onSecondary = ModernistInkBg,
    secondaryContainer = Accent2800,
    onSecondaryContainer = Accent2200,

    tertiary = Neutral300,
    onTertiary = Neutral800,
    tertiaryContainer = Neutral700,
    onTertiaryContainer = Neutral200,

    background = ModernistInkBg,
    onBackground = ModernistInkText,
    surface = ModernistInkBg,
    onSurface = ModernistInkText,
    surfaceVariant = ModernistInkSurface,
    onSurfaceVariant = Neutral400,
    surfaceTint = Accent500,

    surfaceContainerLowest = ModernistInkBg,
    surfaceContainerLow = ModernistInkBg,
    surfaceContainer = ModernistInkSurface,
    surfaceContainerHigh = Neutral800,
    surfaceContainerHighest = Neutral700,

    outline = Neutral700,
    outlineVariant = Neutral800,

    inverseSurface = ModernistInkText,
    inverseOnSurface = ModernistInkBg,
    scrim = ModernistInkBg
)

/**
 * Vrai sous le thème Encre. Les deux thèmes ne se contentent pas d'échanger des
 * couleurs : ils n'ont pas les mêmes opacités de filet (40 % en clair, 28 % en
 * encre), ce qu'aucun rôle Material ne peut porter.
 */
val LocalModernistInk = staticCompositionLocalOf { false }

@Composable
fun FidelyaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        // Les barres système sont transparentes (edge-to-edge imposé par targetSdk 36) :
        // seule la couleur de leurs icônes est à régler, et elle suit le thème choisi
        // et non celui du système — « Sombre » forcé sur un système clair laisserait
        // sinon des icônes claires sur un fond clair.
        val window = (view.context as? Activity)?.window
        SideEffect {
            if (window != null) {
                WindowInsetsControllerCompat(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalModernistInk provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
