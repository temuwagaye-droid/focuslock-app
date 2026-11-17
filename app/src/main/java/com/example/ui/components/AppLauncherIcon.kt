package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * Displays an installed app's actual Android launcher icon inside a crisp circular container,
 * falling back cleanly to a black circular Apps badge if unavailable.
 */
@Composable
fun AppLauncherIcon(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val iconBitmap: ImageBitmap? = remember(packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        }.getOrNull()
    }

    if (iconBitmap != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .padding(2.dp)
        ) {
            Image(
                bitmap = iconBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    } else {
        CircularIconBadge(
            imageVector = Icons.Default.Apps,
            contentDescription = contentDescription,
            containerSize = size,
            iconSize = size * 0.55f,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier
        )
    }
}
