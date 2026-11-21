package com.example.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalDarkTheme

/**
 * FocusLock branded toggle switch.
 *
 * Always visible in both states:
 *  - ON  -> solid black track with white thumb and accent-colored checked track.
 *  - OFF -> translucent dark track with a white thumb so the control is never invisible.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val thumbColor = Color.White
    val checkedTrack = Color.Black
    val uncheckedTrack = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.18f)
    }
    val checkedIconTint = Color.White
    val uncheckedIconTint = Color.White.copy(alpha = 0.72f)

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
            .width(44.dp)
            .height(26.dp),
        colors = SwitchDefaults.colors(
            checkedThumbColor = thumbColor,
            uncheckedThumbColor = thumbColor,
            checkedTrackColor = checkedTrack,
            uncheckedTrackColor = uncheckedTrack,
            checkedIconTint = checkedIconTint,
            uncheckedIconTint = uncheckedIconTint,
            checkedBorderColor = if (isDark) Color.White.copy(alpha = 0.28f) else Color.Black,
            uncheckedBorderColor = if (isDark) Color.White.copy(alpha = 0.28f) else Color.Black,
            checkedBorderWidth = 1.dp,
            uncheckedBorderWidth = 1.dp
        )
    )
}