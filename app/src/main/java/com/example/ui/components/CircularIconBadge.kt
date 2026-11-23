package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalDarkTheme

@Composable
fun CircularIconBadge(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerSize: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color = Color.Black,
    iconTint: Color = Color.White,
    border: BorderStroke? = null
) {
    val effectiveBorder = border ?: if (LocalDarkTheme.current) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
    } else {
        null
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .background(containerColor)
            .then(if (effectiveBorder != null) Modifier.border(effectiveBorder, CircleShape) else Modifier)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
