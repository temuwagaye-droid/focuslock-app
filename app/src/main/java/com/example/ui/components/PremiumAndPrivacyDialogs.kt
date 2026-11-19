package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.EarlyQuitAlert
import com.example.telemetry.TelemetryManager
import com.example.ui.theme.AppAccentTheme
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
            border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("premium_upgrade_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF121216), Color(0xFF1E1930), Color(0xFF2B2214))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "FocusLock Pro",
                                tint = Color(0xFFF3E5AB),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FocusLock Pro Pass",
                                color = Color(0xFFF3E5AB),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Unlimited app blocking slots, Strict Whitelist Mode, premium soundscapes & custom accent themes.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    ProFeatureRow(
                        "Unlimited Blocked Apps",
                        "Remove the 5-app cap in Distraction Shield."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ProFeatureRow(
                        "Strict Whitelist Mode",
                        "Lock out everything except your chosen reading app."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ProFeatureRow(
                        "Ambient Soundscapes",
                        "Unlock Café Ambience & Binaural 40Hz Waves."
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ACCENT THEMES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppAccentTheme.entries.forEach { theme ->
                            val isSelected = theme.id == selectedThemeId
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(theme.lightPrimary)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectTheme(theme) }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = theme.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onTogglePro(!isProActive) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProActive) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFD4AF37),
                            contentColor = if (isProActive) MaterialTheme.colorScheme.onSurface else Color.Black
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("toggle_pro_pass_btn")
                    ) {
                        Text(
                            text = if (isProActive) "REVERT TO FREE TIER" else "ACTIVATE PRO PASS (PREVIEW)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onDismiss,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProFeatureRow(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun AccountabilityQuitDialog(
    alert: EarlyQuitAlert,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val partnerLabel = alert.partnerName.ifBlank { alert.partnerEmail.ifBlank { "Accountability Partner" } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("accountability_quit_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Accountability Alert",
                    containerSize = 56.dp,
                    iconSize = 28.dp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Session Ended Early",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You stayed focused for ${alert.elapsedMinutes}m with ${alert.blockedAttempts} blocked distractions. Let $partnerLabel know about your check-in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(18.dp))

                if (alert.partnerEmail.isNotBlank()) {
                    Button(
                        onClick = {
                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${Uri.encode(alert.partnerEmail)}")
                                putExtra(Intent.EXTRA_SUBJECT, "FocusLock Early Quit Check-In")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hey $partnerLabel, I ended my FocusLock reading session early after ${alert.elapsedMinutes} minutes (${alert.blockedAttempts} blocked distractions)."
                                )
                            }
                            runCatching { context.startActivity(mailIntent) }
                            onDismiss()
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SEND PARTNER EMAIL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    var refreshKey by remember { mutableIntStateOf(0) }
    val events = remember(refreshKey) { TelemetryManager.getAnalyticsEvents(context) }
    val crashes = remember(refreshKey) { TelemetryManager.getCrashReports(context) }
    val combined = remember(events, crashes) {
        (events + crashes).sortedByDescending { it.timestampMs }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("diagnostics_privacy_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularIconBadge(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Diagnostics",
                        containerSize = 40.dp,
                        iconSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Diagnostics & Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${events.size} events • ${crashes.size} crashes (100% local)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                if (combined.isEmpty()) {
                    Text(
                        text = "No local telemetry or crash events recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(combined.take(40), key = { it.id }) { entry ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${entry.kind.uppercase()} • ${entry.name}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (entry.kind == "crash") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = entry.formattedTime(),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (entry.detail.isNotBlank()) {
                                        Text(
                                            text = entry.detail,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            TelemetryManager.clearAllLogs(context)
                            refreshKey++
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("CLEAR LOGS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
