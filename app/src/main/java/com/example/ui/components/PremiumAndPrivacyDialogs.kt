package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.notifications.NotificationHelper
import com.example.service.EarlyQuitAlert
import com.example.telemetry.TelemetryManager
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors

@Composable
fun PremiumUpgradeDialog(
    isProActive: Boolean,
    selectedThemeId: String,
    onTogglePro: (Boolean) -> Unit,
    onSelectTheme: (AppAccentTheme) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("premium_upgrade_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "FocusLock Pro",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isProActive) "FocusLock Pro Active" else "Upgrade to FocusLock Pro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Unlock unlimited distractions blocking, cloud backup, ambient soundscapes & Pro color themes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProFeatureRow("Unlimited Blocked Apps (Free limit: 5)")
                        ProFeatureRow("Cross-Device Cloud Sync & Backup")
                        ProFeatureRow("All Ambient Soundscapes (Rain, Warm Focus, Zen)")
                        ProFeatureRow("Pro Themes (Emerald, Ocean, Warm Amber)")
                        ProFeatureRow("Advanced Analytics & PDF Insights")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "THEME PALETTES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppAccentTheme.entries.forEach { theme ->
                        val isSelected = selectedThemeId == theme.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectTheme(theme) }
                                .padding(vertical = 8.dp)
                        ) {
                            val isDark = LocalDarkTheme.current
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) theme.darkPrimary else theme.lightPrimary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.title.split(" ").last(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (theme.isProOnly && !isProActive) {
                                Text(
                                    text = "PRO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onTogglePro(!isProActive)
                        onDismiss()
                    },
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("toggle_pro_pass_btn")
                ) {
                    Text(
                        text = if (isProActive) "SWITCH TO FREE TIER" else "ACTIVATE PRO PASS (PREVIEW)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProFeatureRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularIconBadge(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            containerSize = 22.dp,
            iconSize = 13.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AccountabilityQuitDialog(
    alert: EarlyQuitAlert,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("accountability_quit_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Accountability Alert",
                    containerSize = 64.dp,
                    iconSize = 34.dp,
                    containerColor = Color.Black,
                    iconTint = Color.White,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Accountability Partner Alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                val targetName = alert.partnerName.ifBlank { "your accountability partner" }
                Text(
                    text = "You ended this reading session early after ${alert.elapsedMinutes}m (${alert.blockedAttempts} distraction blocks). Send an honest check-in to $targetName!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val intent = NotificationHelper.buildAccountabilityShareIntent(
                            partnerEmail = alert.partnerEmail,
                            partnerName = alert.partnerName,
                            elapsedMinutes = alert.elapsedMinutes,
                            blockedAttempts = alert.blockedAttempts
                        )
                        try {
                            context.startActivity(Intent.createChooser(intent, "Notify Accountability Partner"))
                        } catch (_: Exception) {
                        }
                        onDismiss()
                    },
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_accountability_alert_btn")
                ) {
                    Text(
                        text = "NOTIFY PARTNER NOW",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("DISMISS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DiagnosticsAndPrivacyDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val events = remember(refreshTrigger) { TelemetryManager.getAnalyticsEvents(context) }
    val crashes = remember(refreshTrigger) { TelemetryManager.getCrashReports(context) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("diagnostics_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        CircularIconBadge(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            containerSize = 34.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analytics & Crash Logs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                    }
                    Button(
                        onClick = {
                            TelemetryManager.clearAllLogs(context)
                            refreshTrigger++
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("clear_diagnostics_logs_button")
                    ) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear logs",
                            containerSize = 22.dp,
                            iconSize = 13.dp,
                            containerColor = Color.Black,
                            iconTint = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CLEAR",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Analytics events: ${events.size} • Crash logs: ${crashes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (crashes.isEmpty() && events.isEmpty()) {
                        Text(
                            text = "No crash logs or analytics events recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        crashes.forEach { crash ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "CRASH: ${crash.name} (${crash.formattedTime()})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = crash.detail.take(240),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        events.take(20).forEach { evt ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${evt.name} • ${evt.formattedTime()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (evt.detail.isNotBlank()) {
                                        Text(
                                            text = evt.detail,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("DONE", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
