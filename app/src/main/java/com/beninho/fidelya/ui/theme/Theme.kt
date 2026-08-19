package com.beninho.fidelya.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

    // `--color-divider` vaut le texte à 40 % sur le fond, soit le neutre 600.
    outline = Neutral600,
    outlineVariant = Neutral300,

    inverseSurface = Neutral900,
    inverseOnSurface = Neutral100,
    scrim = Neutral900
)

/**
 * Schéma sombre : la même palette retournée sur la rampe neutre. L'accent
 * descend au pas 400, seul niveau qui garde un contraste confortable sur le
 * fond neutre 900.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Accent400,
    onPrimary = Accent900,
    primaryContainer = Accent700,
    onPrimaryContainer = Accent200,
    inversePrimary = Accent600,

    secondary = Accent2400,
    onSecondary = Accent2900,
    secondaryContainer = Accent2700,
    onSecondaryContainer = Accent2200,

    tertiary = Neutral300,
    onTertiary = Neutral800,
    tertiaryContainer = Neutral700,
    onTertiaryContainer = Neutral200,

    background = Neutral900,
    onBackground = Neutral100,
    surface = Neutral900,
    onSurface = Neutral100,
    surfaceVariant = Neutral800,
    onSurfaceVariant = Neutral300,
    surfaceTint = Accent400,

    surfaceContainerLowest = ModernistText,
    surfaceContainerLow = Neutral900,
    surfaceContainer = Neutral800,
    surfaceContainerHigh = Neutral700,
    surfaceContainerHighest = Neutral600,

    outline = Neutral600,
    outlineVariant = Neutral700,

    inverseSurface = Neutral100,
    inverseOnSurface = Neutral900,
    scrim = Neutral900
)

@Composable
fun FidelyaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
