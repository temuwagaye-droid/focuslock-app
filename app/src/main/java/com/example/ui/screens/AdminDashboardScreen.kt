package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientSoundType
import com.example.service.ReadLockAccessibilityService
import com.example.telemetry.TelemetryManager
import com.example.ui.FocusViewModel
import com.example.ui.components.CinematicCardEntrance
import com.example.ui.components.XpRewardChip
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.GamifiedYellow
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors

/**
 * Full-Control Admin Dashboard for managing tiers, overrides, sessions, shield rules,
 * group focus rooms, parental locks, and telemetry logs.
 */
@Composable
fun AdminDashboardScreen(
    viewModel: FocusViewModel,
    onBack: () -> Unit,
    onOpenReadingAppPicker: () -> Unit = {},
    onOpenBlocklist: () -> Unit = {},
    onOpenRooms: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isDark = LocalDarkTheme.current

    val currentUser by viewModel.currentUser.collectAsState()
    val stats by viewModel.statsSummary.collectAsState()
    val isProTier by viewModel.isProTier.collectAsState()
    val isWhitelistMode by viewModel.isWhitelistMode.collectAsState()
    val isDarkModeEnabled by viewModel.isDarkModeEnabled.collectAsState()
    val isAutoDndEnabled by viewModel.isAutoDndEnabled.collectAsState()
    val isCloudSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val isLeaderboardVisible by viewModel.isLeaderboardVisible.collectAsState()
    val accentThemeId by viewModel.accentThemeId.collectAsState()
    val selectedSoundId by viewModel.selectedAmbientSoundId.collectAsState()
    val designatedReadingApp by viewModel.designatedReadingApp.collectAsState()
    val blockedAppsList by viewModel.blockedAppsList.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val isBreakActive by viewModel.isBreakActive.collectAsState()
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val allRooms by viewModel.allRooms.collectAsState()
    val telemetryTick by viewModel.telemetryRefreshTick.collectAsState()
    val serviceConnected by ReadLockAccessibilityService.isServiceConnected.collectAsState()

    val analyticsCount = remember(telemetryTick) { TelemetryManager.getAnalyticsEvents(context).size }
    val crashCount = remember(telemetryTick) { TelemetryManager.getCrashReports(context).size }
    val recentEvents = remember(telemetryTick) { TelemetryManager.getAnalyticsEvents(context).take(5) }

    var adminSessionMinutes by remember { mutableIntStateOf(25) }
    var showConfirmResetStats by remember { mutableStateOf(false) }
    var adminStatusBanner by remember { mutableStateOf<String?>(null) }

    val mintBg = if (isDark) Color(0xFF143326) else Color(0xFFE6FBF1)
    val mintText = if (isDark) GamifiedMint else Color(0xFF0A6B43)
    val purpleBg = if (isDark) Color(0xFF28204B) else Color(0xFFEEEAFF)
    val yellowBg = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFF5DB)
    val yellowText = if (isDark) GamifiedYellow else Color(0xFF8A5A00)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("admin_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Admin Command Hero Card
        CinematicCardEntrance(index = 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(22.dp),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(GamifiedPurple)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Control Center",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ADMIN COMMAND",
                                    color = GamifiedPurple,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = currentUser?.displayName ?: currentUser?.email ?: "Local Admin Mode",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        XpRewardChip(text = "${stats.focusScore} XP")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Live Telemetry Metrics Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminMetricBox(
                            label = "SESSION",
                            value = when {
                                isSessionActive -> "${timeLeftSeconds / 60}m ACTIVE"
                                isBreakActive -> "BREAK"
                                else -> "IDLE"
                            },
                            bgColor = if (isSessionActive) mintBg else purpleBg,
                            valueColor = if (isSessionActive) mintText else GamifiedPurple,
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricBox(
                            label = "BLOCKED",
                            value = "${blockedAppsList.count { it.isBlocked }} APPS",
                            bgColor = yellowBg,
                            valueColor = yellowText,
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricBox(
                            label = "ROOMS",
                            value = "${allRooms.size} LIVE",
                            bgColor = mintBg,
                            valueColor = mintText,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = buildString {
                            append("Protection: ${if (serviceConnected) "Connected" else "Standby"}")
                            append(" • Tier: ${if (isProTier) "PRO" else "FREE"}")
                            append(" • Streak: ${stats.currentStreak}d")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        adminStatusBanner?.let { banner ->
            Card(
                colors = CardDefaults.cardColors(containerColor = purpleBg),
                shape = RoundedCornerShape(16.dp),
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "OK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = GamifiedPurple,
                        modifier = Modifier
                            .clickable { adminStatusBanner = null }
                            .padding(start = 8.dp)
                    )
                }
            }
        }

        // 2. App Tier & System Switches Card
        CinematicCardEntrance(index = 1) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(22.dp),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminSectionHeader(
                        icon = Icons.Default.Tune,
                        title = "MASTER APP CONTROLS",
                        badgeText = if (isProTier) "PRO UNLOCKED" else "STANDARD",
                        badgeBg = if (isProTier) mintBg else purpleBg,
                        badgeColor = if (isProTier) mintText else GamifiedPurple
                    )

                    AdminToggleRow(
                        title = "Pro Pass Override",
                        subtitle = "Unlock unlimited blocklist, Whitelist, Pro themes & sounds",
                        checked = isProTier,
                        tag = "admin_pro_toggle",
                        onCheckedChange = {
                            viewModel.setProTier(it)
                            adminStatusBanner = "Pro Pass ${if (it) "enabled" else "disabled"}."
                        }
                    )

                    AdminToggleRow(
                        title = "Strict Whitelist Mode",
                        subtitle = "Block all apps except FocusLock & selected reading app",
                        checked = isWhitelistMode,
                        tag = "admin_whitelist_toggle",
                        onCheckedChange = {
                            viewModel.adminSetWhitelistOverride(it)
                            adminStatusBanner = "Whitelist mode ${if (it) "engaged" else "disabled"}."
                        }
                    )

                    AdminToggleRow(
                        title = "Dark Mode Theme",
                        subtitle = "Switch entire FocusLock surface palette",
                        checked = isDarkModeEnabled,
                        tag = "admin_dark_mode_toggle",
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )

                    AdminToggleRow(
                        title = "Auto Do-Not-Disturb",
                        subtitle = "Silence notifications automatically during focus",
                        checked = isAutoDndEnabled,
                        tag = "admin_dnd_toggle",
                        onCheckedChange = { viewModel.setAutoDndEnabled(it) }
                    )

                    AdminToggleRow(
                        title = "Leaderboard Visibility",
                        subtitle = "Publish focus XP to the community leaderboard",
                        checked = isLeaderboardVisible,
                        tag = "admin_leaderboard_toggle",
                        onCheckedChange = { viewModel.setLeaderboardVisible(it) }
                    )

                    AdminToggleRow(
                        title = "Auto Cloud Sync",
                        subtitle = "Sync profile, tasks & stats when connected",
                        checked = isCloudSyncEnabled,
                        tag = "admin_cloud_sync_toggle",
                        onCheckedChange = { viewModel.setCloudSyncEnabled(it) }
                    )
                }
            }
        }

        // 3. Accent Theme & Ambient Sound Overrides
        CinematicCardEntrance(index = 2) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(22.dp),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AdminSectionHeader(
                        icon = Icons.Default.Palette,
                        title = "THEME & SOUNDSCAPE OVERRIDE",
                        badgeText = AppAccentTheme.fromId(accentThemeId).title.uppercase(),
                        badgeBg = purpleBg,
                        badgeColor = GamifiedPurple
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppAccentTheme.entries.forEach { theme ->
                            val selected = accentThemeId == theme.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) GamifiedPurple else MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        if (theme.isProOnly && !isProTier) {
                                            viewModel.setProTier(true)
                                        }
                                        viewModel.selectAccentTheme(theme)
                                        adminStatusBanner = "Theme set to ${theme.title}."
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = theme.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = GamifiedPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ambient Audio: ${AmbientSoundType.fromId(selectedSoundId).displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AmbientSoundType.entries.forEach { sound ->
                            val selected = selectedSoundId == sound.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) GamifiedMint else MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        if (sound.isProOnly && !isProTier) {
                                            viewModel.setProTier(true)
                                        }
                                        viewModel.selectAmbientSound(sound, previewIfSessionActive = true)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${sound.emoji} ${sound.displayName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Session & Blocker Force Controls
        CinematicCardEntrance(index = 3) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(22.dp),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AdminSectionHeader(
                        icon = Icons.Default.Shield,
                        title = "SESSION & SHIELD DISPATCH",
                        badgeText = if (designatedReadingApp.isNullOrBlank()) "NO READER" else "READY",
                        badgeBg = if (designatedReadingApp.isNullOrBlank()) yellowBg else mintBg,
                        badgeColor = if (designatedReadingApp.isNullOrBlank()) yellowText else mintText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Reader: ${designatedReadingApp ?: "Auto / None Selected"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            val selected = adminSessionMinutes == mins
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) GamifiedPurple else MaterialTheme.colorScheme.surface)
                                    .clickable { adminSessionMinutes = mins }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.adminForceStartSession(context, adminSessionMinutes)
                                adminStatusBanner = "Force-started ${adminSessionMinutes}m focus session."
                            },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_force_start_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FORCE START", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = {
                                viewModel.adminForceStopSessionOrBreak(context)
                                adminStatusBanner = "Stopped active focus/break timer."
                            },
                            enabled = isSessionActive || isBreakActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_force_stop_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STOP ACTIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenReadingAppPicker,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("READER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = onOpenBlocklist,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("BLOCKLIST", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.adminClearAllBlockedApps()
                            adminStatusBanner = "Cleared all apps from blocklist."
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_clear_blocked_apps_btn")
                    ) {
                        Text(
                            text = "CLEAR ALL BLOCKED APPS (${blockedAppsList.count { it.isBlocked }})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // 5. Group Focus Rooms Moderation Card
        CinematicCardEntrance(index = 4) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(22.dp),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AdminSectionHeader(
                        icon = Icons.Default.Groups,
                        title = "GROUP ROOMS MODERATION",
                        badgeText = "${allRooms.size} ROOMS",
                        badgeBg = purpleBg,
                        badgeColor = GamifiedPurple
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.adminQuickCreateRoom("Admin Study Hall", 25, true)
                                adminStatusBanner = "Created new public Admin Study Hall room."
                            },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_quick_create_room_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CREATE ROOM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = onOpenRooms,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("OPEN ROOMS TAB", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    if (allRooms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        allRooms.forEach { room ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${room.name} (${room.roomCode})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Host: ${room.hostName} • ${room.durationMinutes}m • ${room.participantCount} user(s) • ${room.status.uppercase()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete room ${room.name}",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable {
                                                    viewModel.adminDeleteRoom(room.roomId)
                                                    adminStatusBanner = "Deleted room ${room.roomCode}."
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("waiting" to "WAIT", "active" to "ACTIVE", "completed" to "CLOSE").forEach { (status, label) ->
                                            val isCurrent = room.status == status
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isCurrent) GamifiedPurple else MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { viewModel.adminSetRoomStatus(room.roomId, status) }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Security, Telemetry & Reset Recovery Card
        CinematicCardEntrance(index = 5) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(22.dp),
                border = null,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AdminSectionHeader(
                        icon = Icons.Default.BugReport,
                        title = "TELEMETRY, PIN & RECOVERY",
                        badgeText = "$analyticsCount EVT / $crashCount ERR",
                        badgeBg = yellowBg,
                        badgeColor = yellowText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (recentEvents.isNotEmpty()) {
                        recentEvents.forEach { evt ->
                            Text(
                                text = "• [${evt.formattedTime()}] ${evt.name} ${if (evt.detail.isNotBlank()) "(${evt.detail})" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = "No analytics or crash telemetry entries stored.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.adminResetParentalLock()
                                adminStatusBanner = "Parental PIN lock removed."
                            },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_reset_pin_btn")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isParentalLockEnabled) "UNLOCK PIN" else "PIN CLEAR",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.adminClearTelemetry(context)
                                adminStatusBanner = "Telemetry & crash buffer cleared."
                            },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_clear_telemetry_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("CLEAR LOGS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showConfirmResetStats = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_reset_stats_btn")
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET ALL FOCUS STATS & LOGS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    if (showConfirmResetStats) {
        AlertDialog(
            onDismissRequest = { showConfirmResetStats = false },
            title = { Text("Reset All Focus Stats?", fontWeight = FontWeight.Black) },
            text = { Text("This clears all saved focus session history logs and resets streaks/XP to 0.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.adminResetSessionHistory()
                        showConfirmResetStats = false
                        adminStatusBanner = "All session logs and XP stats reset."
                    }
                ) {
                    Text("RESET", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetStats = false }) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun AdminSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    badgeText: String,
    badgeBg: Color,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(badgeBg)
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text = badgeText,
                color = badgeColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun AdminMetricBox(
    label: String,
    value: String,
    bgColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = valueColor.copy(alpha = 0.78f),
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdminToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GamifiedPurple,
                uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
