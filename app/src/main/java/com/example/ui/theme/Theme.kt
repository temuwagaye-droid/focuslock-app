package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalDarkTheme = compositionLocalOf { false }
val LocalAccentTheme = compositionLocalOf { AppAccentTheme.VIOLET }

private fun buildLightColorScheme(accent: AppAccentTheme) = lightColorScheme(
    primary = accent.lightPrimary,
    onPrimary = BoldBackground,
    primaryContainer = accent.lightPrimaryContainer,
    onPrimaryContainer = accent.lightOnPrimaryContainer,
    secondary = BoldTextMuted,
    onSecondary = BoldBackground,
    tertiary = BlockRed,
    onTertiary = Color.White,
    background = BoldBackground,
    surface = BoldSurface,
    onBackground = BoldTextPrimary,
    onSurface = BoldTextPrimary,
    surfaceVariant = BoldSurfaceWhite,
    onSurfaceVariant = BoldTextPrimary,
    outline = BoldTextPrimary,
    outlineVariant = BoldBorder,
    error = BlockRed,
    onError = Color.White,
    errorContainer = OverrideBg,
    onErrorContainer = OverrideText
)

private fun buildDarkColorScheme(accent: AppAccentTheme) = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = Color(0xFF1A1625),
    primaryContainer = accent.darkPrimaryContainer,
    onPrimaryContainer = accent.darkOnPrimaryContainer,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = Color(0xFF0F0E13),
    surface = Color(0xFF1D1B22),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2C2A33),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF3E3A47),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    accentTheme: AppAccentTheme = AppAccentTheme.VIOLET,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) buildDarkColorScheme(accentTheme) else buildLightColorScheme(accentTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAccentTheme provides accentTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
