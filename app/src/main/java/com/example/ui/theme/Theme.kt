package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = YoutubeRed,
    onPrimary = Color.White,
    primaryContainer = YoutubeRedDark,
    onPrimaryContainer = Color.White,
    secondary = HighlightYellow,
    onSecondary = StudioBlack,
    secondaryContainer = StudioCardSurface,
    onSecondaryContainer = HighlightYellow,
    tertiary = NeonCyan,
    onTertiary = StudioBlack,
    background = StudioBlack,
    onBackground = StudioTextPrimary,
    surface = StudioDarkSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioCardSurface,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve high-production creator branding
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

