package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.audio.AmbientSoundType
import com.example.telemetry.TelemetryManager
import com.example.ui.FocusViewModel
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    viewModel: FocusViewModel,
    onNavigateToTimer: () -> Unit,
    onNavigateToBlocklist: () -> Unit,
    onNavigateToRooms: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val stats by viewModel.statsSummary.collectAsState()
    val blockedApps by viewModel.blockedAppsList.collectAsState()
    val allRooms by viewModel.allRooms.collectAsState()
    val isProTier by viewModel.isProTier.collectAsState()
    val isWhitelistMode by viewModel.isWhitelistMode.collectAsState()
    val isDarkMode by viewModel.isDarkModeEnabled.collectAsState()
    val isAutoDnd by viewModel.isAutoDndEnabled.collectAsState()
    val isLeaderboardVisible by viewModel.isLeaderboardVisible.collectAsState()
    val isCloudSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val accentThemeId by viewModel.accentThemeId.collectAsState()
    val selectedSoundId by viewModel.selectedAmbientSoundId.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val isBreakActive by viewModel.isBreakActive.collectAsState()
    val refreshTick by viewModel.telemetryRefreshTick.collectAsState()

    val telemetryCount = remember(refreshTick) {
        TelemetryManager.getAnalyticsEvents(context).size + TelemetryManager.getCrashReports(context).size
    }

    var quickRoomName by remember { mutableStateOf("Admin Focus Sprint") }
    var quickRoomMinutes by remember { mutableIntStateOf(25) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("admin_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("admin_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Column {
                Text(
                    text = "Admin Control Center",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Full system control, overrides, rooms & diagnostics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Hero Admin Telemetry Banner
        Card(
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF121216), Color(0xFF1D1A2B), Color(0xFF282015))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFFF3E5AB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM COMMAND OVERRIDE",
                            color = Color(0xFFF3E5AB),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdminMetricBox("${stats.totalFocusMinutes}m", "Total Focus", Modifier.weight(1f))
                        AdminMetricBox("${blockedApps.count { it.isBlocked }}", "Locked Apps", Modifier.weight(1f))
                        AdminMetricBox("$telemetryCount", "Logs", Modifier.weight(1f))
                    }
                }
            }
        }

        // Master App Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MASTER APP CONTROLS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
                AdminSwitchRow("Pro Pass Tier Override", isProTier) { viewModel.setProTier(it) }
                AdminSwitchRow("Strict Whitelist Mode", isWhitelistMode) { viewModel.adminSetWhitelistOverride(it) }
                AdminSwitchRow("Dark Theme", isDarkMode) { viewModel.toggleDarkMode(it) }
                AdminSwitchRow("Auto Do Not Disturb", isAutoDnd) { viewModel.setAutoDndEnabled(it) }
                AdminSwitchRow("Leaderboard Visibility", isLeaderboardVisible) { viewModel.setLeaderboardVisible(it) }
                AdminSwitchRow("Automatic Cloud Sync", isCloudSyncEnabled) { viewModel.setCloudSyncEnabled(it) }
            }
        }

        // Theme & Soundscape Override
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "THEME & SOUNDSCAPE OVERRIDE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppAccentTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = theme.id == accentThemeId,
                            onClick = { viewModel.selectAccentTheme(theme) },
                            label = { Text(theme.id.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmbientSoundType.entries.forEach { sound ->
                        FilterChip(
                            selected = sound.id == selectedSoundId,
                            onClick = { viewModel.selectAmbientSound(sound) },
                            label = { Text(sound.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }

        // Session & Shield Dispatch
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SESSION & SHIELD DISPATCH",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.adminForceStartSession(context, 15)
                            Toast.makeText(context, "Admin force-started 15m session", Toast.LENGTH_SHORT).show()
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START 15M", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.adminForceStopSessionOrBreak(context) },
                        enabled = isSessionActive || isBreakActive,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP ACTIVE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToTimer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("TIMER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onNavigateToBlocklist,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHIELD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onNavigateToRooms,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("ROOMS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                OutlinedButton(
                    onClick = {
                        viewModel.adminClearAllBlockedApps()
                        Toast.makeText(context, "Cleared all blocked apps", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("CLEAR ALL BLOCKED APPS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Room Moderation & Quick Room Create
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "FOCUS ROOM MODERATION (${allRooms.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickRoomName,
                        onValueChange = { quickRoomName = it },
                        label = { Text("Quick Room Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            viewModel.adminQuickCreateRoom(quickRoomName, quickRoomMinutes, true)
                            Toast.makeText(context, "Admin room created!", Toast.LENGTH_SHORT).show()
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("CREATE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
                allRooms.forEach { room ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${room.name} (${room.roomCode})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${room.status.uppercase()} • ${room.durationMinutes}m • ${room.participantCount} members",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (room.status == "active") "END" else "START",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable {
                                            val next = if (room.status == "active") "completed" else "active"
                                            viewModel.adminSetRoomStatus(room.roomId, next)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.adminDeleteRoom(room.roomId) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Room",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Security & Recovery
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SECURITY & RECOVERY ACTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(
                    onClick = {
                        viewModel.adminResetParentalLock()
                        Toast.makeText(context, "Parental PIN reset", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("RESET PARENTAL LOCK PIN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        viewModel.adminClearTelemetry(context)
                        Toast.makeText(context, "Telemetry cleared", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("CLEAR TELEMETRY & CRASH LOGS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        viewModel.adminResetSessionHistory()
                        Toast.makeText(context, "Session history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("RESET ALL SESSION HISTORY", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AdminMetricBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(text = value, color = GamifiedMint, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(text = label, color = Color.White.copy(alpha = 0.78f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AdminSwitchRow(
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
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
