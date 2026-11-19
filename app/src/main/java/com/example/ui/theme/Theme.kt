package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalDarkTheme = compositionLocalOf { false }
val LocalAccentTheme = compositionLocalOf { AppAccentTheme.VIOLET }

private fun buildLightColorScheme(accent: AppAccentTheme): ColorScheme = lightColorScheme(
    primary = accent.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = accent.lightPrimaryContainer,
    onPrimaryContainer = accent.lightOnPrimaryContainer,
    secondary = BoldTextSecondary,
    onSecondary = Color.White,
    background = BoldBackground,
    onBackground = BoldTextPrimary,
    surface = BoldSurfaceWhite,
    onSurface = BoldTextPrimary,
    surfaceVariant = BoldSurface,
    onSurfaceVariant = BoldTextSecondary,
    outline = BoldBorder,
    outlineVariant = BoldBorder,
    error = BlockRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private fun buildDarkColorScheme(accent: AppAccentTheme): ColorScheme = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = Color(0xFF190C4C),
    primaryContainer = accent.darkPrimaryContainer,
    onPrimaryContainer = accent.darkOnPrimaryContainer,
    secondary = Color(0xFFCAC4D0),
    onSecondary = Color(0xFF1D1B22),
    background = Color(0xFF0F0E13),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B22),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF24212B),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF3E3A47),
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
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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

@Composable
fun appBlackButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Color.Black,
    contentColor = Color.White,
    disabledContainerColor = Color.Black.copy(alpha = 0.4f),
    disabledContentColor = Color.White.copy(alpha = 0.65f)
)

@Composable
fun appBlackButtonBorder(): BorderStroke? =
    if (LocalDarkTheme.current) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else null
