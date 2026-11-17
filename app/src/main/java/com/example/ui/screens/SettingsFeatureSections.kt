package com.example.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DoNotDisturbOff
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notifications.NotificationHelper
import com.example.security.ParentalPinDialog
import com.example.ui.FocusViewModel
import com.example.ui.components.CinematicCardEntrance
import com.example.ui.components.CircularIconBadge
import com.example.ui.components.DiagnosticsAndPrivacyDialog
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.PremiumUpgradeDialog
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import com.example.widget.ShortcutHelper
import java.util.Locale

@Composable
fun SettingsAdvancedSections(
    viewModel: FocusViewModel
) {
    val context = LocalContext.current

    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthBusy by viewModel.isAuthBusy.collectAsState()
    val isSyncing by viewModel.isSyncingCloud.collectAsState()
    val lastSyncText by viewModel.lastCloudSyncText.collectAsState()
    val cloudMessage by viewModel.cloudStatusMessage.collectAsState()

    val isProTier by viewModel.isProTier.collectAsState()
    val accentThemeId by viewModel.accentThemeId.collectAsState()

    val isAutoDndEnabled by viewModel.isAutoDndEnabled.collectAsState()
    val isReminderEnabled by viewModel.isReminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()

    val partnerName by viewModel.partnerName.collectAsState()
    val partnerEmail by viewModel.partnerEmail.collectAsState()
    val notifyPartnerOnQuit by viewModel.notifyPartnerOnQuit.collectAsState()

    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val parentalPinHash by viewModel.parentalPinHash.collectAsState()

    val isLeaderboardVisible by viewModel.isLeaderboardVisible.collectAsState()
    val isCloudSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val analyticsOptIn by viewModel.analyticsOptIn.collectAsState()
    val crashOptIn by viewModel.crashReportingOptIn.collectAsState()

    var showPremiumDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }

    var partnerNameDraft by remember(partnerName) { mutableStateOf(partnerName) }
    var partnerEmailDraft by remember(partnerEmail) { mutableStateOf(partnerEmail) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateReminderSettings(context, true, reminderHour, reminderMinute)
            Toast.makeText(context, "Daily reminder scheduled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied. Enable in Settings.", Toast.LENGTH_SHORT).show()
        }
    }

    val nm = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    var hasDndPolicyAccess by remember { mutableStateOf(nm.isNotificationPolicyAccessGranted) }
    var pendingEnableDndOnResume by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = nm.isNotificationPolicyAccessGranted
                hasDndPolicyAccess = granted
                if (granted && pendingEnableDndOnResume) {
                    pendingEnableDndOnResume = false
                    viewModel.setAutoDndEnabled(true)
                    Toast.makeText(context, "Do Not Disturb integration enabled!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 1. FocusLock Pro & Accent Themes Luxury Card
    Spacer(modifier = Modifier.height(16.dp))
    val proGold = Color(0xFFD4AF37)
    val proGoldSoft = Color(0xFFF3E5AB)
    CinematicCardEntrance(index = 0) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.5.dp, if (isProTier) proGold else proGold.copy(alpha = 0.72f)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF121216),
                            Color(0xFF1D1A2B),
                            Color(0xFF282015)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "FocusLock Pro",
                            containerSize = 44.dp,
                            iconSize = 24.dp,
                            containerColor = Color.Black,
                            iconTint = proGoldSoft,
                            border = BorderStroke(1.5.dp, proGold)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isProTier) proGold.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, proGold.copy(alpha = 0.55f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isProTier) "PRO PASS • ACTIVE" else "PRO MEMBERSHIP",
                                    color = proGoldSoft,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = if (isProTier) "FocusLock Pro Unlocked" else "Upgrade to FocusLock Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isProTier) {
                        "Unlimited distraction blocking, strict Whitelist Mode, Pro palettes & full soundscape library active."
                    } else {
                        "Remove the 5-app cap and unlock Whitelist Mode, cloud backup, ambient soundscapes & exclusive themes."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("∞ Blocked Apps", "Whitelist Mode", "Pro Themes").forEach { perk ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = perk,
                                color = proGoldSoft,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showPremiumDialog = true },
                    colors = appBlackButtonColors(),
                    border = BorderStroke(1.2.dp, proGold.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_pro_upgrade_btn")
                ) {
                    Text(
                        text = if (isProTier) "MANAGE PRO & THEMES" else "EXPLORE PRO PASS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }
    }

    // 2. Cloud Backup, Profile & Cross-Device Sync Card
    Spacer(modifier = Modifier.height(16.dp))
    CinematicCardEntrance(index = 1) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularIconBadge(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        containerSize = 32.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PROFILE & CLOUD SYNC",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val user = currentUser
                if (user == null) {
                    Text(
                        text = "Sign in with Google to sync stats, rooms & settings across devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    cloudMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GoogleSignInButton(
                        onClick = { viewModel.signInWithGoogle(context) },
                        isLoading = isAuthBusy,
                        modifier = Modifier.testTag("settings_google_sign_in_button")
                    )
                } else {
                    Text(
                        text = "Supabase Account: ${user.displayName ?: user.email ?: user.uid.take(8)} • $lastSyncText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    user.email?.let { mail ->
                        Text(
                            text = mail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    cloudMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.syncCloudNow() },
                            enabled = !isSyncing,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isSyncing) "SYNCING..." else "BACKUP NOW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.restoreFromCloud() },
                            enabled = !isSyncing,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RESTORE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.signOut(context) },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SIGN OUT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. Do Not Disturb (DND) Integration Card
    Spacer(modifier = Modifier.height(16.dp))
    CinematicCardEntrance(index = 2) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        CircularIconBadge(
                            imageVector = if (isAutoDndEnabled && hasDndPolicyAccess) Icons.Default.DoNotDisturbOn else Icons.Default.DoNotDisturbOff,
                            contentDescription = if (isAutoDndEnabled) "DND Integration On" else "DND Integration Off",
                            containerSize = 32.dp,
                            iconSize = 18.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    if (!hasDndPolicyAccess) {
                                        pendingEnableDndOnResume = true
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        val next = !isAutoDndEnabled
                                        viewModel.setAutoDndEnabled(next)
                                        Toast.makeText(
                                            context,
                                            if (next) "Auto Do Not Disturb turned ON" else "Auto Do Not Disturb turned OFF",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .testTag("dnd_status_icon_toggle")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "DO NOT DISTURB INTEGRATION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = when {
                                    !hasDndPolicyAccess -> "Tap switch or button below to grant DND permission."
                                    isAutoDndEnabled -> "ON: Notifications silence automatically during focus."
                                    else -> "OFF: Tap switch or icon to enable automatic DND."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Switch(
                        checked = isAutoDndEnabled && hasDndPolicyAccess,
                        onCheckedChange = { want ->
                            if (want && !hasDndPolicyAccess) {
                                pendingEnableDndOnResume = true
                                Toast.makeText(context, "Grant Do Not Disturb access in Android Settings to turn ON.", Toast.LENGTH_LONG).show()
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } else {
                                pendingEnableDndOnResume = false
                                viewModel.setAutoDndEnabled(want)
                                Toast.makeText(
                                    context,
                                    if (want) "Auto Do Not Disturb turned ON" else "Auto Do Not Disturb turned OFF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("auto_dnd_switch")
                    )
                }
                if (!hasDndPolicyAccess) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            pendingEnableDndOnResume = true
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GRANT DND POLICY ACCESS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 4. Accountability Partner Card (Partner Name & Email revealed only on elliptical button click)
    Spacer(modifier = Modifier.height(16.dp))
    var showPartnerFields by remember { mutableStateOf(false) }
    CinematicCardEntrance(index = 3) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            containerSize = 32.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ACCOUNTABILITY PARTNER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (partnerName.isNotBlank()) "Partner: $partnerName" else "Notify a partner if you quit early.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Switch(
                        checked = notifyPartnerOnQuit,
                        onCheckedChange = { viewModel.saveAccountabilityPartner(partnerNameDraft, partnerEmailDraft, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("accountability_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showPartnerFields = !showPartnerFields },
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("accountability_elliptical_button")
                ) {
                    Text(
                        text = if (showPartnerFields) "HIDE PARTNER DETAILS" else if (partnerName.isNotBlank() || partnerEmail.isNotBlank()) "EDIT PARTNER DETAILS" else "ADD PARTNER DETAILS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                AnimatedVisibility(
                    visible = showPartnerFields,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = partnerNameDraft,
                            onValueChange = { partnerNameDraft = it.take(50) },
                            label = { Text("Partner Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("partner_name_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = partnerEmailDraft,
                            onValueChange = { partnerEmailDraft = it.take(100) },
                            label = { Text("Partner Email") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("partner_email_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    viewModel.saveAccountabilityPartner(partnerNameDraft, partnerEmailDraft, notifyPartnerOnQuit)
                                    Toast.makeText(context, "Partner saved!", Toast.LENGTH_SHORT).show()
                                },
                                colors = appBlackButtonColors(),
                                border = appBlackButtonBorder(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SAVE PARTNER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = {
                                    val intent = NotificationHelper.buildAccountabilityShareIntent(
                                        partnerEmailDraft,
                                        partnerNameDraft,
                                        elapsedMinutes = 0,
                                        blockedAttempts = 0
                                    )
                                    context.startActivity(Intent.createChooser(intent, "Send Test Accountability Message"))
                                },
                                colors = appBlackButtonColors(),
                                border = appBlackButtonBorder(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("TEST ALERT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. Parental Control Mode Card
    Spacer(modifier = Modifier.height(16.dp))
    CinematicCardEntrance(index = 4) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            containerSize = 32.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PARENTAL CONTROL MODE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Require a PIN to edit blocklists or quit early.",
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
                            } else {
                                if (parentalPinHash.isNotBlank()) showDisablePinDialog = true
                                else viewModel.setParentalLock(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("parental_control_switch")
                    )
                }
                if (isParentalLockEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showSetPinDialog = true },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CHANGE PARENTAL PIN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 6. Notifications & Daily Reading Reminders Card
    Spacer(modifier = Modifier.height(16.dp))
    CinematicCardEntrance(index = 5) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            containerSize = 32.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "DAILY READING REMINDERS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Get a daily nudge at ${String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.updateReminderSettings(context, enable, reminderHour, reminderMinute)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("daily_reminder_switch")
                    )
                }
                AnimatedVisibility(visible = isReminderEnabled) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "Reminder Hour: ${reminderHour}:00",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = reminderHour.toFloat(),
                            onValueChange = { viewModel.updateReminderSettings(context, true, it.toInt(), 0) },
                            valueRange = 6f..22f,
                            steps = 15,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }
        }
    }

    // 7. Widgets & Lock-Screen Shortcuts Card
    Spacer(modifier = Modifier.height(16.dp))
    CinematicCardEntrance(index = 6) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularIconBadge(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        containerSize = 32.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WIDGETS & QUICK SHORTCUTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pin a 1-tap Start Focus shortcut to your home screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val ok = ShortcutHelper.pinFocusShortcut(context)
                        if (!ok) {
                            Toast.makeText(context, "Long-press FocusLock app icon or open Android Widgets to add shortcut.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_shortcut_button")
                ) {
                    Text("PIN HOME / LOCK SHORTCUT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    // 8. Privacy, Analytics & Crash Reporting Card
    Spacer(modifier = Modifier.height(16.dp))
    CinematicCardEntrance(index = 7) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularIconBadge(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        containerSize = 32.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRIVACY, ANALYTICS & CRASHES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                PrivacyToggleRow("Visible on Leaderboard", isLeaderboardVisible) { viewModel.setLeaderboardVisible(it) }
                PrivacyToggleRow("Auto Cloud Backup Sync", isCloudSyncEnabled) { viewModel.setCloudSyncEnabled(it) }
                PrivacyToggleRow("Usage Analytics Opt-In", analyticsOptIn) { viewModel.setAnalyticsOptIn(it) }
                PrivacyToggleRow("Crash Reporting Opt-In", crashOptIn) { viewModel.setCrashReportingOptIn(it) }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showDiagnosticsDialog = true },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_diagnostics_button")
                    ) {
                        Text("VIEW LOGS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (currentUser != null) {
                        Button(
                            onClick = { viewModel.deleteCloudData() },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DELETE CLOUD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showPremiumDialog) {
        PremiumUpgradeDialog(
            isProActive = isProTier,
            selectedThemeId = accentThemeId,
            onTogglePro = { viewModel.setProTier(it) },
            onSelectTheme = { viewModel.selectAccentTheme(it) },
            onDismiss = { showPremiumDialog = false }
        )
    }

    if (showDiagnosticsDialog) {
        DiagnosticsAndPrivacyDialog(onDismiss = { showDiagnosticsDialog = false })
    }

    if (showSetPinDialog) {
        ParentalPinDialog(
            title = "Set Parental Control PIN",
            subtitle = "Choose a 4-6 digit PIN to lock blocklist & session quit actions.",
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
            title = "Disable Parental Control",
            subtitle = "Enter your current PIN to turn off parental protection.",
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

@Composable
private fun PrivacyToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
