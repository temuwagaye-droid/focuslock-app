package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BoldColorScheme = lightColorScheme(
    primary = BoldPrimary,
    onPrimary = BoldBackground,
    primaryContainer = BoldPrimaryContainer,
    onPrimaryContainer = BoldOnPrimaryContainer,
    secondary = BoldTextMuted,
    onSecondary = BoldBackground,
    tertiary = BlockRed,
    background = BoldBackground,
    surface = BoldSurface,
    onBackground = BoldTextPrimary,
    onSurface = BoldTextPrimary,
    surfaceVariant = BoldSurfaceWhite,
    onSurfaceVariant = BoldTextPrimary,
    outline = BoldTextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BoldColorScheme,
        typography = Typography,
        content = content
    )
}
