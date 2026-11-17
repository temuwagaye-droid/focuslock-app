package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cinematic Card Entrance:
 * Smoothly fades in (0 -> 1), scales from 96% to 100%, slides upward, and settles a subtle blur.
 */
@Composable
fun CinematicCardEntrance(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val initialOffsetPx = with(density) { 24.dp.toPx() }

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.96f) }
    val slideYAnim = remember { Animatable(initialOffsetPx) }
    val blurAnim = remember { Animatable(8f) }

    LaunchedEffect(Unit) {
        val staggerMs = (index * 45L).coerceAtMost(220L)
        if (staggerMs > 0L) delay(staggerMs)
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            slideYAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            blurAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
        }
    }

    val blurDp = blurAnim.value.dp
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = alphaAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                translationY = slideYAnim.value
            }
            .then(if (blurDp > 0.15.dp) Modifier.blur(blurDp) else Modifier)
    ) {
        content()
    }
}
