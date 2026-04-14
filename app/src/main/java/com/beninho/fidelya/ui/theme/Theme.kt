package com.beninho.fidelya.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Blue90,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Amber40,
    onSecondary = Amber90,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber10,
    tertiary = Teal40,
    onTertiary = Teal90,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal10,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue10,
    primaryContainer = Blue40,
    onPrimaryContainer = Blue90,
    secondary = Amber80,
    onSecondary = Amber10,
    secondaryContainer = Amber40,
    onSecondaryContainer = Amber90,
    tertiary = Teal80,
    onTertiary = Teal10,
    tertiaryContainer = Teal40,
    onTertiaryContainer = Teal90,
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
        content = content
    )
}
