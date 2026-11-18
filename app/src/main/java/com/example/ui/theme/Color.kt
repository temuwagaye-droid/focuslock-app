package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Bold Typography High-Contrast Color Palette
val GamifiedPurple = Color(0xFF6C4DFF)
val GamifiedMint = Color(0xFF3DDC97)
val GamifiedYellow = Color(0xFFFFC857)
val GamifiedOffWhite = Color(0xFFF7F7FB)

val BoldBackground = GamifiedOffWhite
val BoldTextPrimary = Color(0xFF1A1824)
val BoldTextSecondary = Color(0xFF4F4B5C)
val BoldTextMuted = Color(0xFF6C687A)

val BoldPrimary = GamifiedPurple
val BoldPrimaryContainer = Color(0xFFEEEAFF)
val BoldOnPrimaryContainer = Color(0xFF23115E)

val BoldSurface = Color(0xFFF1F1F7)
val BoldSurfaceWhite = Color(0xFFFFFFFF)
val BoldBorder = Color(0xFFE6E4F0)

// Rose/Pink highlight accent (Streak Badge)
val StreakBg = Color(0xFFFFF3D6)
val StreakText = Color(0xFF4A2E00)
val StreakBorder = Color(0xFFFFC857)

// Soft red alert override colors
val OverrideBg = Color(0xFFFFDAD6)
val OverrideText = Color(0xFF410002)
val OverrideBorder = Color(0xFFFFB4AB)
val BlockRed = Color(0xFFBA1A1A)

enum class AppAccentTheme(
    val id: String,
    val title: String,
    val isProOnly: Boolean,
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color
) {
    VIOLET(
        id = "violet",
        title = "Electric Purple",
        isProOnly = false,
        lightPrimary = Color(0xFF6C4DFF),
        lightPrimaryContainer = Color(0xFFEEEAFF),
        lightOnPrimaryContainer = Color(0xFF23115E),
        darkPrimary = Color(0xFFB5A4FF),
        darkPrimaryContainer = Color(0xFF3F2B96),
        darkOnPrimaryContainer = Color(0xFFEEEAFF)
    ),
    EMERALD(
        id = "emerald",
        title = "Forest Emerald",
        isProOnly = true,
        lightPrimary = Color(0xFF0F766E),
        lightPrimaryContainer = Color(0xFFCCFBF1),
        lightOnPrimaryContainer = Color(0xFF042F2E),
        darkPrimary = Color(0xFF5EEAD4),
        darkPrimaryContainer = Color(0xFF115E59),
        darkOnPrimaryContainer = Color(0xFFCCFBF1)
    ),
    OCEAN(
        id = "ocean",
        title = "Deep Ocean",
        isProOnly = true,
        lightPrimary = Color(0xFF1D4ED8),
        lightPrimaryContainer = Color(0xFFDBEAFE),
        lightOnPrimaryContainer = Color(0xFF1E3A8A),
        darkPrimary = Color(0xFF93C5FD),
        darkPrimaryContainer = Color(0xFF1E40AF),
        darkOnPrimaryContainer = Color(0xFFDBEAFE)
    ),
    AMBER(
        id = "amber",
        title = "Warm Amber",
        isProOnly = true,
        lightPrimary = Color(0xFFB45309),
        lightPrimaryContainer = Color(0xFFFEF3C7),
        lightOnPrimaryContainer = Color(0xFF451A03),
        darkPrimary = Color(0xFFFCD34D),
        darkPrimaryContainer = Color(0xFF92400E),
        darkOnPrimaryContainer = Color(0xFFFEF3C7)
    );

    companion object {
        fun fromId(id: String?): AppAccentTheme {
            return entries.firstOrNull { it.id == id } ?: VIOLET
        }
    }
}
