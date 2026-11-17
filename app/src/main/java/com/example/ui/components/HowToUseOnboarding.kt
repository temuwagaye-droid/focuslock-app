package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.screens.isAccessibilityServiceEnabled
import com.example.ui.theme.LocalDarkTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dialog shown when user attempts to start a focus session without Accessibility Service enabled.
 */
@Composable
fun AccessibilityRequiredDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Pulsing animation for the warning shield
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("accessibility_required_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Shield Icon with animated pulse
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .border(2.dp, MaterialTheme.colorScheme.error, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Accessibility Required",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Accessibility Service Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "To block distracting apps and keep you focused on reading, FocusLock requires the Accessibility Service to be turned ON in Android Settings.\n\nWithout this permission, app redirection cannot function.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("enable_accessibility_from_dialog_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TURN ON IN SETTINGS",
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_accessibility_dialog_btn")
                ) {
                    Text(
                        text = "CANCEL",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Dialog shown when user attempts to start a focus session without selecting a designated reading app.
 */
@Composable
fun ReadingAppRequiredDialog(
    onSelectReadingApp: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "book_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "book_pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("reading_app_required_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Reading App Required",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select a Reading App First",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "You need to select a designated reading app before starting a focus session.\n\nChoose the app you want to read in so ReadLock can keep your session focused.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSelectReadingApp,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("select_reading_app_from_dialog_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELECT READING APP",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_reading_app_dialog_btn")
                ) {
                    Text(
                        text = "CANCEL",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Dialog shown when user attempts to start a focus session without selecting any apps to block (and Whitelist Mode is off).
 */
@Composable
fun BlockedAppsRequiredDialog(
    onSelectBlockedApps: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "block_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "block_pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("blocked_apps_required_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .border(2.dp, MaterialTheme.colorScheme.error, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Blocked Apps Required",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Apps to Block First",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "You haven't selected any distracting apps to block yet.\n\nSelect at least one app in the Blocklist tab (or turn on Whitelist Mode) before starting a focus session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSelectBlockedApps,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("select_blocked_apps_from_dialog_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELECT BLOCKED APPS",
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_blocked_apps_dialog_btn")
                ) {
                    Text(
                        text = "CANCEL",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Data class representing one step of the onboarding tutorial
 */
private data class OnboardingStep(
    val stepIndex: Int,
    val icon: ImageVector,
    val badgeLabel: String,
    val title: String,
    val description: String,
    val proTip: String
)

/**
 * Animated Illustration for Step 1: Designate Your Reader
 * Features a floating book with expanding circular halos and interactive selectable app chips.
 */
@Composable
private fun AnimatedReaderIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "reader_anim")

    // Gentle vertical bobbing
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reader_bobbing"
    )

    // Concentric expanding ring scale and alpha
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    var selectedApp by remember { mutableIntStateOf(0) }
    val mockApps = listOf("Kindle", "Play Books", "Moon+")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            // Animated halo ring
            Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                drawCircle(
                    color = Color(0xFF1E88E5).copy(alpha = ringAlpha),
                    radius = (size.minDimension / 2.3f) * ringScale,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Central Glowing Card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(0, bobbingOffset.toInt() * 3) }
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    )
                    .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "Reader App",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive mock app selector showing how designation works
        Text(
            text = "SAMPLE APPS TO DESIGNATE:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mockApps.forEachIndexed { index, name ->
                val isSelected = selectedApp == index
                val chipScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 0.95f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "chip_scale_$index"
                )
                val chipBorderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    label = "chip_border_$index"
                )
                val chipBgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    label = "chip_bg_$index"
                )

                Box(
                    modifier = Modifier
                        .scale(chipScale)
                        .clip(RoundedCornerShape(50))
                        .background(chipBgColor)
                        .border(1.5.dp, chipBorderColor, RoundedCornerShape(50))
                        .clickable { selectedApp = index }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Illustration for Step 2: Enable Accessibility Service
 * Features animated radar waves radiating from the shield, an animated toggle switch with spring physics,
 * and live active/inactive status.
 */
@Composable
private fun AnimatedAccessibilityIllustration(
    isSystemEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "accessibility_radar")

    // Sonar wave 1
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1_scale"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1_alpha"
    )

    // Interactive mock toggle switch that demonstrates how to turn it on
    var isDemonstrationOn by remember { mutableStateOf(isSystemEnabled) }

    val toggleOffset by animateDpAsState(
        targetValue = if (isDemonstrationOn) 22.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "toggle_offset"
    )
    val toggleColor by animateColorAsState(
        targetValue = if (isDemonstrationOn) Color(0xFF2E7D32) else Color(0xFFB0BEC5),
        label = "toggle_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            // Sonar waves
            Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                val waveColor = if (isDemonstrationOn) Color(0xFF4CAF50) else Color(0xFF1E88E5)
                drawCircle(
                    color = waveColor.copy(alpha = wave1Alpha),
                    radius = (size.minDimension / 2.3f) * wave1Scale,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            val isDark = LocalDarkTheme.current
            val activeGreenBg = if (isDark) Color(0xFF1B3B22) else Color(0xFFE8F5E9)
            val activeGreenBorder = if (isDark) Color(0xFF66BB6A) else Color(0xFF4CAF50)
            val activeGreenText = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)

            // Shield Center
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDemonstrationOn) activeGreenBg else MaterialTheme.colorScheme.primaryContainer
                    )
                    .border(
                        2.5.dp,
                        if (isDemonstrationOn) activeGreenBorder else MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Shield",
                    tint = if (isDemonstrationOn) activeGreenText else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val isDarkPill = LocalDarkTheme.current
        val pillActiveBg = if (isDarkPill) Color(0xFF1B3B22) else Color(0xFFE8F5E9)
        val pillActiveBorder = if (isDarkPill) Color(0xFF66BB6A) else Color(0xFF81C784)
        val pillActiveText = if (isDarkPill) Color(0xFFA5D6A7) else Color(0xFF1B5E20)

        // Interactive demonstration pill
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDemonstrationOn) pillActiveBg else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(50),
            border = BorderStroke(
                1.5.dp,
                if (isDemonstrationOn) pillActiveBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .clickable { isDemonstrationOn = !isDemonstrationOn }
                .padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isDemonstrationOn) "Focus Redirection: ACTIVE" else "Tap Switch to Test: OFF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDemonstrationOn) pillActiveText else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Mini Switch
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(toggleColor)
                        .padding(2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = toggleOffset)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDemonstrationOn) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Illustration for Step 3: Choose Custom Duration
 * Features an animated circular radar clock sweep, dynamic minute switcher, and interactive preset buttons.
 */
@Composable
private fun AnimatedDurationIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "clock_sweep")

    // Clock hand angle rotation (0 to 360 degrees)
    val clockAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "clock_angle"
    )

    var selectedDuration by remember { mutableIntStateOf(25) }
    val durations = listOf(15, 25, 45, 60)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            // Sweeping radar clock Canvas
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

            Canvas(modifier = Modifier.size(100.dp)) {
                // Background circle
                drawCircle(
                    color = outlineColor,
                    radius = size.minDimension / 2.2f,
                    style = Stroke(width = 4.dp.toPx())
                )

                // Dynamic progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.1f),
                            primaryColor.copy(alpha = 0.6f),
                            primaryColor
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = clockAngle,
                    useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Center clock tick marks
                val center = Offset(size.width / 2f, size.height / 2f)
                val handRadius = size.minDimension / 2.5f
                val rad = Math.toRadians((clockAngle - 90.0))
                val handEnd = Offset(
                    center.x + (handRadius * cos(rad)).toFloat(),
                    center.y + (handRadius * sin(rad)).toFloat()
                )

                drawLine(
                    color = primaryColor,
                    start = center,
                    end = handEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center pivot dot
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = center
                )
            }

            // Foreground duration badge inside clock
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${selectedDuration}m",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preset Duration Selector Buttons
        Text(
            text = "ANY DURATION SAVES AS YOUR DEFAULT:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            durations.forEach { mins ->
                val isSelected = selectedDuration == mins
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 0.95f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "scale_dur_$mins"
                )

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(50)
                        )
                        .clickable { selectedDuration = mins }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${mins}m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Animated Illustration for Step 4: Flow & Coffee Break
 * Features a steaming coffee cup drawn with Canvas bezier curves and rising steam particles,
 * plus a celebratory streak reward badge.
 */
@Composable
private fun AnimatedCoffeeIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "coffee_steam")

    // Steam rising phase
    val steamPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steam_phase"
    )

    // Star sparkle rotation
    val starAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star_rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            // Rising Steam Canvas
            Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                val steamColor = Color(0xFF8D6E63)
                val centerX = size.width / 2f
                val cupTopY = size.height * 0.45f

                // Draw 3 rising sinusoidal steam trails
                val trailOffsets = listOf(-14f, 0f, 14f)
                trailOffsets.forEachIndexed { idx, xOffset ->
                    val progress = (steamPhase + (idx * 0.33f)) % 1f
                    val alpha = (1f - progress).coerceIn(0f, 1f) * 0.8f
                    val y = cupTopY - (progress * 38.dp.toPx())
                    val waveX = centerX + xOffset.dp.toPx() + (sin((progress * 6.28f + idx.toFloat()).toDouble()).toFloat() * 6.dp.toPx())

                    drawCircle(
                        color = steamColor.copy(alpha = alpha),
                        radius = (3f + progress * 3f).dp.toPx(),
                        center = Offset(waveX, y)
                    )
                }
            }

            val isDarkCoffee = LocalDarkTheme.current
            // Coffee Cup Disc
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isDarkCoffee) {
                                listOf(Color(0xFF3E2D16), Color(0xFF5D4037).copy(alpha = 0.5f))
                            } else {
                                listOf(Color(0xFFFFECB3), Color(0xFFFFCA28).copy(alpha = 0.4f))
                            }
                        )
                    )
                    .border(2.5.dp, Color(0xFFFFA000), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = "Coffee Break",
                    tint = if (isDarkCoffee) Color(0xFFFFCA28) else Color(0xFF795548),
                    modifier = Modifier.size(42.dp)
                )
            }

            // Orbiting Celebration Star
            val orbitRadius = 46.dp
            val rad = Math.toRadians(starAngle.toDouble())
            val starX = (orbitRadius.value * cos(rad)).toFloat().dp
            val starY = (orbitRadius.value * sin(rad)).toFloat().dp

            Box(
                modifier = Modifier
                    .offset(x = starX, y = starY)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Streak reward banner
        val isDarkBanner = LocalDarkTheme.current
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (isDarkBanner) Color(0xFF332612) else Color(0xFFFFF8E1))
                .border(1.5.dp, Color(0xFFFFB300), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REWARD: +1 READING STREAK POINT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDarkBanner) Color(0xFFFFE082) else Color(0xFF6D4C41),
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

/**
 * Interactive step-by-step Onboarding / "How to Use" guide with animations.
 */
@Composable
fun HowToUseOnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    var previousStep by remember { mutableIntStateOf(0) }
    var isAutoPlaying by remember { mutableStateOf(true) }
    val stepTimerProgress = remember { Animatable(0f) }

    val steps = remember {
        listOf(
            OnboardingStep(
                stepIndex = 0,
                icon = Icons.Default.Book,
                badgeLabel = "STEP 1 OF 4",
                title = "Designate Your Reader",
                description = "Choose the e-reader or reading app you want to focus on (such as Kindle, Play Books, Moon+ Reader, or any reader app installed on your phone).",
                proTip = "In Whitelist mode, only your designated reading app is accessible while all other distractions stay locked."
            ),
            OnboardingStep(
                stepIndex = 1,
                icon = Icons.Default.Security,
                badgeLabel = "STEP 2 OF 4",
                title = "Enable Accessibility Service",
                description = "FocusLock needs Android's Accessibility Service enabled to monitor when social media or games are opened and gently redirect you back to your book.",
                proTip = "Accessibility is used strictly on-device to protect your focus. No personal data ever leaves your device."
            ),
            OnboardingStep(
                stepIndex = 2,
                icon = Icons.Default.Timer,
                badgeLabel = "STEP 3 OF 4",
                title = "Choose Custom Duration",
                description = "Set your reading goal using the duration slider or quick preset buttons (15m, 25m, 45m, 60m). Your custom choice is saved automatically—sessions are never forced to a default timer.",
                proTip = "Customize blocked apps or switch between Whitelist and Blacklist mode in the Apps tab."
            ),
            OnboardingStep(
                stepIndex = 3,
                icon = Icons.Default.LocalCafe,
                badgeLabel = "STEP 4 OF 4",
                title = "Flow & Coffee Break",
                description = "When you click 'Start Focus Session', distraction-free mode is engaged. Complete your goal to earn streak points and transition into a relaxing Coffee Break!",
                proTip = "Track your daily focus minutes, blocked distraction attempts, and reading score in the Stats tab."
            )
        )
    }

    // Automatically advance every 2 seconds across all 4 steps, then auto-complete after step 4
    LaunchedEffect(currentStep, isAutoPlaying) {
        stepTimerProgress.snapTo(0f)
        if (isAutoPlaying) {
            stepTimerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
            )
            if (currentStep < steps.size - 1) {
                previousStep = currentStep
                currentStep += 1
            } else {
                onDismiss()
            }
        }
    }

    val isAccessibilityEnabled = isAccessibilityServiceEnabled(context)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .testTag("how_to_use_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Row: Title, Auto 2s toggle & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HOW TO USE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isAutoPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { isAutoPlaying = !isAutoPlaying }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("guide_auto_toggle_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isAutoPlaying) "Pause auto advance" else "Resume auto advance",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isAutoPlaying) "AUTO 2S" else "PAUSED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Text(
                            text = "FocusLock Quick Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("close_how_to_use_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Guide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Smooth 2-second auto-advance progress bar
                LinearProgressIndicator(
                    progress = { stepTimerProgress.value },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Animated Step Progress Indicator Dots with spring expansion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        val isCurrent = index == currentStep
                        val isDone = index < currentStep

                        val dotWidth by animateDpAsState(
                            targetValue = if (isCurrent) 28.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "dot_width_$index"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            },
                            animationSpec = tween(durationMillis = 300),
                            label = "dot_color_$index"
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(4.dp))
                                .background(dotColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Animated Card Content with Horizontal Slide + Fade spec based on navigation direction
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width / 3 } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(250)))
                        } else {
                            (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally { width -> width / 3 } + fadeOut(tween(250)))
                        }
                    },
                    label = "step_content_slider"
                ) { stepIdx ->
                    val current = steps[stepIdx]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dynamic Animated Illustration per step
                        when (stepIdx) {
                            0 -> AnimatedReaderIllustration()
                            1 -> AnimatedAccessibilityIllustration(isSystemEnabled = isAccessibilityEnabled)
                            2 -> AnimatedDurationIllustration()
                            3 -> AnimatedCoffeeIllustration()
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Step Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = current.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Special Action Button for Step 2 (Accessibility)
                        if (stepIdx == 1) {
                            val isDarkGuide = LocalDarkTheme.current
                            val cardBg = if (isAccessibilityEnabled) {
                                if (isDarkGuide) Color(0xFF1B3B22) else Color(0xFFE8F5E9)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                            val cardBorder = if (isAccessibilityEnabled) {
                                if (isDarkGuide) Color(0xFF66BB6A) else Color(0xFF81C784)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            val statusTextTint = if (isAccessibilityEnabled) {
                                if (isDarkGuide) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Security,
                                                contentDescription = null,
                                                tint = statusTextTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isAccessibilityEnabled) "Service Active" else "Service Inactive",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = statusTextTint
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isAccessibilityEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                            ),
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text(
                                                text = if (isAccessibilityEnabled) "Settings" else "Enable Now",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAccessibilityEnabled) Color.White else MaterialTheme.colorScheme.onError
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Pro-tip card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = current.proTip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = {
                                previousStep = currentStep
                                currentStep--
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("guide_prev_step_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Step",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BACK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (currentStep < steps.size - 1) {
                        Button(
                            onClick = {
                                previousStep = currentStep
                                currentStep++
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("guide_next_step_btn")
                        ) {
                            Text(
                                text = "NEXT",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Step",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("guide_finish_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GET STARTED",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Card for Settings and Timer screens inviting users to open the How to Use guide.
 * Features an animated pulse on the icon and a shiny indicator badge.
 */
@Composable
fun HowToUseBannerCard(
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_scale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenGuide() }
            .testTag("how_to_use_banner_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HOW FOCUSLOCK WORKS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "GUIDE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Animated 4-step walkthrough for new users",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PLAY",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
