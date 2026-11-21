package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AmbientSoundType
import com.example.data.remote.ManagedUserRecord
import com.example.telemetry.TelemetryManager
import com.example.ui.FocusViewModel
import com.example.ui.components.AppSwitch
import com.example.ui.components.CircularIconBadge
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val stats by viewModel.statsSummary.collectAsState()
    val blockedApps by viewModel.blockedAppsList.collectAsState()
    val allRooms by viewModel.allRooms.collectAsState()
    val managedUsers by viewModel.managedUsers.collectAsState()
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

    var userSearchQuery by remember { mutableStateOf("") }
    var userPlanFilter by remember { mutableStateOf("ALL") } // ALL, PRO, FREE
    var showRegisterUserDialog by remember { mutableStateOf(false) }

    val totalUsersCount = managedUsers.size
    val proUsersCount = managedUsers.count { it.isPro }
    val freeUsersCount = managedUsers.count { !it.isPro }
    val filteredUsers = remember(managedUsers, userSearchQuery, userPlanFilter) {
        managedUsers.filter { user ->
            val matchesTier = when (userPlanFilter) {
                ManagedUserRecord.PLAN_PRO -> user.isPro
                ManagedUserRecord.PLAN_FREE -> !user.isPro
                else -> true
            }
            val q = userSearchQuery.trim()
            val matchesQuery = q.isBlank() ||
                user.displayName.contains(q, ignoreCase = true) ||
                user.email.contains(q, ignoreCase = true)
            matchesTier && matchesQuery
        }
    }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("admin_back_btn")
            ) {
                CircularIconBadge(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    containerSize = 40.dp,
                    iconSize = 20.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Admin Control Center",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Users, plans, overrides, rooms & diagnostics",
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
                        CircularIconBadge(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            containerSize = 30.dp,
                            iconSize = 17.dp,
                            containerColor = Color.Black,
                            iconTint = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
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
                        AdminMetricBox("$totalUsersCount", "Users", Modifier.weight(1f))
                        AdminMetricBox("$proUsersCount", "Pro Plan", Modifier.weight(1f))
                        AdminMetricBox("${stats.totalFocusMinutes}m", "Total Focus", Modifier.weight(1f))
                        AdminMetricBox("$telemetryCount", "Logs", Modifier.weight(1f))
                    }
                }
            }
        }

        // User & Subscription Management Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_user_management_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            imageVector = Icons.Default.ManageAccounts,
                            contentDescription = "User Management",
                            containerSize = 34.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "USER & PLAN MANAGEMENT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$totalUsersCount total • $proUsersCount Pro • $freeUsersCount Free",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Button(
                        onClick = { showRegisterUserDialog = true },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("admin_open_register_user_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REGISTER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = userSearchQuery,
                    onValueChange = { userSearchQuery = it },
                    leadingIcon = {
                        CircularIconBadge(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            containerSize = 30.dp,
                            iconSize = 16.dp
                        )
                    },
                    placeholder = { Text("Search users by name or email...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_user_search_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to "All ($totalUsersCount)",
                        ManagedUserRecord.PLAN_PRO to "Pro ($proUsersCount)",
                        ManagedUserRecord.PLAN_FREE to "Free ($freeUsersCount)"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = userPlanFilter == key,
                            onClick = { userPlanFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("admin_filter_${key.lowercase()}")
                        )
                    }
                }

                if (filteredUsers.isEmpty()) {
                    Text(
                        text = "No matching users found. Tap REGISTER to add a user.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredUsers.forEach { user ->
                            ManagedUserAdminRow(
                                user = user,
                                onTogglePlan = { targetTier ->
                                    viewModel.adminSetUserPlan(context, user.userId, targetTier)
                                    Toast.makeText(
                                        context,
                                        "${user.displayName}: ${if (targetTier == ManagedUserRecord.PLAN_PRO) "PRO activated" else "Set to FREE"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onToggleEnabled = { nextEnabled ->
                                    viewModel.adminSetUserEnabled(context, user.userId, nextEnabled)
                                    Toast.makeText(
                                        context,
                                        "${user.displayName}: ${if (nextEnabled) "Account activated" else "Account suspended"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onDelete = {
                                    viewModel.adminDeleteManagedUser(context, user.userId)
                                    Toast.makeText(context, "Removed ${user.displayName}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
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
                        CircularIconBadge(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            containerSize = 20.dp,
                            iconSize = 12.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                        CircularIconBadge(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            containerSize = 20.dp,
                            iconSize = 12.dp
                        )
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
                        Toast.makeText(context, "Cleared all blocked apps (${blockedApps.size})", Toast.LENGTH_SHORT).show()
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (room.status == "active") "END" else "START",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black)
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
                                    CircularIconBadge(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Room",
                                        containerSize = 32.dp,
                                        iconSize = 16.dp
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

    if (showRegisterUserDialog) {
        AdminRegisterUserDialog(
            onRegisterSuspend = { name, email, tier, password, notes ->
                viewModel.adminRegisterUser(context, name, email, tier, password, notes)
            },
            onSuccessDismiss = {
                showRegisterUserDialog = false
            },
            onDismiss = { showRegisterUserDialog = false }
        )
    }
}

@Composable
private fun ManagedUserAdminRow(
    user: ManagedUserRecord,
    onTogglePlan: (String) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val initial = user.displayName.trim().firstOrNull()?.uppercase() ?: "U"
    val canDelete = user.userId != ManagedUserRecord.PRIMARY_DEVICE_USER_ID &&
        user.source != ManagedUserRecord.SOURCE_GOOGLE

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (user.isPro) Color(0xFFD4AF37).copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_row_${user.userId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = user.email,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${user.source} • ${if (user.isEnabled) "ACTIVE" else "SUSPENDED"}" +
                                if (user.notes.isNotBlank()) " • ${user.notes}" else "",
                            fontSize = 10.sp,
                            color = if (user.isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black)
                        .border(
                            1.dp,
                            if (user.isPro) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = user.planTier,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val targetTier = if (user.isPro) ManagedUserRecord.PLAN_FREE else ManagedUserRecord.PLAN_PRO
                Button(
                    onClick = { onTogglePlan(targetTier) },
                    colors = appBlackButtonColors(),
                    border = if (user.isPro) {
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    } else {
                        BorderStroke(1.dp, Color(0xFFD4AF37))
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("admin_user_plan_btn_${user.userId}")
                ) {
                    CircularIconBadge(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        containerSize = 20.dp,
                        iconSize = 12.dp,
                        containerColor = Color.Black,
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (user.isPro) "SET FREE" else "ACTIVATE PRO",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }

                OutlinedButton(
                    onClick = { onToggleEnabled(!user.isEnabled) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("admin_user_status_btn_${user.userId}")
                ) {
                    Text(
                        text = if (user.isEnabled) "SUSPEND" else "ENABLE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("admin_user_delete_btn_${user.userId}")
                    ) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove User",
                            containerSize = 32.dp,
                            iconSize = 16.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRegisterUserDialog(
    onRegisterSuspend: suspend (String, String, String, String, String) -> Result<ManagedUserRecord>,
    onSuccessDismiss: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf(ManagedUserRecord.PLAN_FREE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_register_user_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularIconBadge(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Register User",
                        containerSize = 36.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Register App User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_reg_name_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_reg_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password (optional, min 6 chars for Supabase)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_reg_password_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Role / Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "INITIAL SUBSCRIPTION TIER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTier == ManagedUserRecord.PLAN_FREE,
                        onClick = { selectedTier = ManagedUserRecord.PLAN_FREE },
                        label = { Text("FREE TIER", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("admin_reg_tier_free")
                    )
                    FilterChip(
                        selected = selectedTier == ManagedUserRecord.PLAN_PRO,
                        onClick = { selectedTier = ManagedUserRecord.PLAN_PRO },
                        label = { Text("PRO TIER", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("admin_reg_tier_pro")
                    )
                }

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            isSaving = true
                            scope.launch {
                                val res = onRegisterSuspend(name, email, selectedTier, password, notes)
                                isSaving = false
                                res.onSuccess { created ->
                                    Toast.makeText(
                                        context,
                                        "Registered ${created.displayName} (${created.planTier})",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSuccessDismiss()
                                }.onFailure { err ->
                                    errorMessage = err.message ?: "Could not register user."
                                }
                            }
                        },
                        enabled = !isSaving,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("admin_reg_submit_btn")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text("REGISTER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Text(text = value, color = GamifiedMint, fontWeight = FontWeight.Black, fontSize = 16.sp)
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
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
