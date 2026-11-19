package com.example.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.security.ParentalPinDialog
import com.example.ui.FocusViewModel
import com.example.ui.components.CinematicCardEntrance
import com.example.ui.components.CircularIconBadge
import com.example.ui.components.DiagnosticsAndPrivacyDialog
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.PremiumUpgradeDialog
import com.example.ui.components.ReadingRemindersCard
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import com.example.widget.ShortcutHelper

@Composable
fun SettingsAdvancedSections(
    viewModel: FocusViewModel,
    onOpenAdminDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isProTier by viewModel.isProTier.collectAsState()
    val accentThemeId by viewModel.accentThemeId.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthBusy by viewModel.isAuthBusy.collectAsState()
    val isSyncingCloud by viewModel.isSyncingCloud.collectAsState()
    val cloudStatusMessage by viewModel.cloudStatusMessage.collectAsState()
    val lastCloudSyncText by viewModel.lastCloudSyncText.collectAsState()
    val isLeaderboardVisible by viewModel.isLeaderboardVisible.collectAsState()
    val isCloudSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val isAutoDndEnabled by viewModel.isAutoDndEnabled.collectAsState()

    val savedPartnerName by viewModel.partnerName.collectAsState()
    val savedPartnerEmail by viewModel.partnerEmail.collectAsState()
    val savedNotifyOnQuit by viewModel.notifyPartnerOnQuit.collectAsState()

    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val parentalPinHash by viewModel.parentalPinHash.collectAsState()

    val analyticsOptIn by viewModel.analyticsOptIn.collectAsState()
    val crashReportingOptIn by viewModel.crashReportingOptIn.collectAsState()

    var showProDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var partnerExpanded by remember { mutableStateOf(false) }

    var partnerNameInput by remember { mutableStateOf(savedPartnerName) }
    var partnerEmailInput by remember { mutableStateOf(savedPartnerEmail) }
    var notifyQuitInput by remember { mutableStateOf(savedNotifyOnQuit) }

    LaunchedEffect(savedPartnerName, savedPartnerEmail, savedNotifyOnQuit) {
        partnerNameInput = savedPartnerName
        partnerEmailInput = savedPartnerEmail
        notifyQuitInput = savedNotifyOnQuit
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(0.dp))

        // 1. Pro Pass & Accent Theme Card
        CinematicCardEntrance(index = 2) {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pro_pass_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF121216), Color(0xFF1E1A2F), Color(0xFF282014))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Pro Pass",
                                    tint = Color(0xFFF3E5AB),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isProTier) "FocusLock Pro Pass Active" else "FocusLock Pro Pass",
                                    color = Color(0xFFF3E5AB),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = { showProDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD4AF37),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("open_pro_dialog_btn")
                            ) {
                                Text(
                                    text = if (isProTier) "MANAGE" else "UPGRADE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppAccentTheme.entries.forEach { theme ->
                                val selected = theme.id == accentThemeId
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(theme.lightPrimary)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color(0xFFF3E5AB) else Color.White.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.selectAccentTheme(theme) }
                                ) {
                                    if (theme.isProOnly && !isProTier) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "${theme.title} locked",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Supabase Cloud Sync & Google Account Card
        CinematicCardEntrance(index = 3) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloud_sync_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularIconBadge(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            containerSize = 30.dp,
                            iconSize = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SUPABASE CLOUD SYNC",
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = currentUser?.let { "${it.displayName ?: it.email ?: "Connected"} • $lastCloudSyncText" }
                                    ?: "Sign in with Google to back up sessions, apps & tasks",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    cloudStatusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser == null) {
                        GoogleSignInButton(
                            onClick = { viewModel.signInWithGoogle(context) },
                            isLoading = isAuthBusy,
                            modifier = Modifier.testTag("settings_google_sign_in_btn")
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Me on Global Leaderboard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = isLeaderboardVisible,
                                onCheckedChange = { viewModel.setLeaderboardVisible(it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Automatic Cloud Backup", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = isCloudSyncEnabled,
                                onCheckedChange = { viewModel.setCloudSyncEnabled(it) }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncCloudNow() },
                                enabled = !isSyncingCloud,
                                colors = appBlackButtonColors(),
                                border = appBlackButtonBorder(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BACKUP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.restoreFromCloud() },
                                enabled = !isSyncingCloud,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("RESTORE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.deleteCloudData() },
                                enabled = !isSyncingCloud,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("DELETE CLOUD", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.signOut(context) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("SIGN OUT", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Auto DND & Home Shortcut Card
        CinematicCardEntrance(index = 3) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CircularIconBadge(
                                imageVector = Icons.Default.DoNotDisturbOn,
                                contentDescription = null,
                                containerSize = 30.dp,
                                iconSize = 16.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "AUTO DO NOT DISTURB",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Silence notifications automatically while reading",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Switch(
                            checked = isAutoDndEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                                    if (nm != null && !nm.isNotificationPolicyAccessGranted) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                            )
                                        }
                                    }
                                }
                                viewModel.setAutoDndEnabled(enable)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val pinned = ShortcutHelper.pinFocusShortcut(context)
                            Toast.makeText(
                                context,
                                if (pinned) "Focus shortcut pin requested!" else "Long-press FocusLock icon or add widget from home screen.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PIN QUICK FOCUS SHORTCUT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // 4. Accountability Partner Card
        CinematicCardEntrance(index = 4) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { partnerExpanded = !partnerExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CircularIconBadge(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                containerSize = 30.dp,
                                iconSize = 16.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ACCOUNTABILITY PARTNER",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (savedPartnerEmail.isNotBlank()) "${savedPartnerName.ifBlank { "Partner" }} ($savedPartnerEmail)"
                                    else "Alert a friend if you quit a session early",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Icon(
                            imageVector = if (partnerExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Accountability Partner"
                        )
                    }

                    AnimatedVisibility(visible = partnerExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = partnerNameInput,
                                onValueChange = { partnerNameInput = it },
                                label = { Text("Partner Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = partnerEmailInput,
                                onValueChange = { partnerEmailInput = it },
                                label = { Text("Partner Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Alert on early quit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Switch(
                                    checked = notifyQuitInput,
                                    onCheckedChange = { notifyQuitInput = it }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveAccountabilityPartner(
                                            partnerNameInput,
                                            partnerEmailInput,
                                            notifyQuitInput
                                        )
                                        Toast.makeText(context, "Partner saved", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = appBlackButtonColors(),
                                    border = appBlackButtonBorder(),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("SAVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (partnerEmailInput.isBlank()) {
                                            Toast.makeText(context, "Enter a partner email first", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:${Uri.encode(partnerEmailInput.trim())}")
                                                putExtra(Intent.EXTRA_SUBJECT, "FocusLock Accountability Check-In")
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Hi ${partnerNameInput.ifBlank { "there" }}, you're my FocusLock reading accountability partner!"
                                                )
                                            }
                                            runCatching { context.startActivity(mailIntent) }
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("TEST EMAIL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Parental Lock PIN Card
        CinematicCardEntrance(index = 4) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CircularIconBadge(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                containerSize = 30.dp,
                                iconSize = 16.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PARENTAL PIN LOCK",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (isParentalLockEnabled) "PIN required to unblock apps or stop sessions"
                                    else "Protect App Shield & active sessions with a 4+ digit PIN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Switch(
                            checked = isParentalLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    showSetPinDialog = true
                                } else if (parentalPinHash.isNotBlank()) {
                                    showDisablePinDialog = true
                                } else {
                                    viewModel.setParentalLock(false)
                                }
                            },
                            modifier = Modifier.testTag("parental_lock_switch")
                        )
                    }
                    if (isParentalLockEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showSetPinDialog = true },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("CHANGE PARENTAL PIN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 6. Reading Reminders Full Card (Create/Edit/Delete, Custom Date/Time, Repeat, Snooze/Complete/Dismiss, Custom Sounds, Quiet Hours, History)
        CinematicCardEntrance(index = 4) {
            ReadingRemindersCard(viewModel = viewModel)
        }

        // 7. Privacy, Telemetry & Diagnostics Card
        CinematicCardEntrance(index = 5) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularIconBadge(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            containerSize = 30.dp,
                            iconSize = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRIVACY & DIAGNOSTICS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Local Usage Analytics", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Switch(checked = analyticsOptIn, onCheckedChange = { viewModel.setAnalyticsOptIn(it) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Local Crash Reporting", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Switch(checked = crashReportingOptIn, onCheckedChange = { viewModel.setCrashReportingOptIn(it) })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDiagnosticsDialog = true },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("VIEW LOCAL DIAGNOSTIC LOGS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // 8. Admin Control Center Card
        CinematicCardEntrance(index = 5) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAdminDashboard)
                    .testTag("open_admin_dashboard_card")
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
                        CircularIconBadge(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Dashboard",
                            containerSize = 36.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ADMIN CONTROL CENTER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Full control over shield, sessions, rooms, PIN & telemetry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Admin Control Center",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showProDialog) {
        PremiumUpgradeDialog(
            isProActive = isProTier,
            selectedThemeId = accentThemeId,
            onTogglePro = { viewModel.setProTier(it) },
            onSelectTheme = { viewModel.selectAccentTheme(it) },
            onDismiss = { showProDialog = false }
        )
    }

    if (showDiagnosticsDialog) {
        DiagnosticsAndPrivacyDialog(onDismiss = { showDiagnosticsDialog = false })
    }

    if (showSetPinDialog) {
        ParentalPinDialog(
            title = if (isParentalLockEnabled) "Change Parental PIN" else "Set Parental Lock PIN",
            subtitle = "Choose a 4–8 digit PIN to protect App Shield & active focus sessions.",
            storedPinHash = parentalPinHash,
            isSettingNewPin = true,
            onPinConfirmed = { hash ->
                viewModel.setParentalLock(true, hash)
                showSetPinDialog = false
            },
            onDismiss = { showSetPinDialog = false }
        )
    }

    if (showDisablePinDialog) {
        ParentalPinDialog(
            title = "Unlock Parental Mode",
            subtitle = "Enter your current PIN to disable Parental Lock.",
            storedPinHash = parentalPinHash,
            isSettingNewPin = false,
            onPinConfirmed = {
                viewModel.setParentalLock(false)
                showDisablePinDialog = false
            },
            onDismiss = { showDisablePinDialog = false }
        )
    }
}
