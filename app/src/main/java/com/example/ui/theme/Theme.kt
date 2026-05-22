package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TealAccentPrimary,
    secondary = TealAccentGlow,
    tertiary = GoldAccentTertiary,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    surfaceVariant = SlateDarkCard,
    onBackground = TextWhiteRegular,
    onSurface = TextWhiteRegular,
    outline = SlateDarkBorder
)

// Fallback Light Color Scheme (We prioritize Dark for Fintech layout)
private val LightColorScheme = lightColorScheme(
    primary = TealAccentPrimary,
    secondary = TealAccentGlow,
    tertiary = GoldAccentTertiary,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    surfaceVariant = SlateDarkCard,
    onBackground = TextWhiteRegular,
    onSurface = TextWhiteRegular,
    outline = SlateDarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark layout by default for a luxury dashboard
    dynamicColor: Boolean = false, // Disable dynamic material-you overrides to enforce our premium slate-teal theme
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
