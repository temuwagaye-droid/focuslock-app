package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),        // Warm violet for night reading
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF0F0E13),      // Deep slate black/midnight violet background for night comfort
    surface = Color(0xFF1D1B22),         // Slightly lighter elevated surface
    onBackground = Color(0xFFE6E1E5),    // Crisp soft white
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2C2A33),   // Mid dark surface
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else BoldColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
