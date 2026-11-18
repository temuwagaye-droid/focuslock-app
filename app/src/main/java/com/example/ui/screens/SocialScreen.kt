package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.remote.CloudUserProfile
import com.example.data.remote.FocusRoomDto
import com.example.data.remote.RoomParticipantDto
import com.example.data.remote.SupabaseUser
import com.example.ui.FocusViewModel
import com.example.ui.components.CinematicCardEntrance
import com.example.ui.components.CircularIconBadge
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.XpRewardChip
import com.example.ui.components.readerRankTitle
import com.example.ui.components.treeStageTitle
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors

@Composable
fun SocialScreen(
    viewModel: FocusViewModel,
    onStartGroupSession: (Int) -> Unit,
    onNavigateToStats: (() -> Unit)? = null,
    onNavigateToTimer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthBusy by viewModel.isAuthBusy.collectAsState()
    val cloudStatusMessage by viewModel.cloudStatusMessage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Study Rooms & Ranks",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Co-op focus rooms, XP rankings & cloud sync.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        cloudStatusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
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
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { viewModel.clearCloudStatusMessage() }
                            .padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val user = currentUser
        if (user == null) {
            CinematicCardEntrance(index = 0) {
                GoogleSignInGateCard(
                    isBusy = isAuthBusy,
                    onSignInClick = { viewModel.signInWithGoogle(context) }
                )
            }
            if (onNavigateToTimer != null) {
                Spacer(modifier = Modifier.height(14.dp))
                CinematicCardEntrance(index = 1) {
                    Button(
                        onClick = onNavigateToTimer,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("rooms_back_to_timer_btn")
                    ) {
                        Text("SOLO FOCUS? OPEN TIMER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            key(user.uid) {
                SignedInSocialContent(
                    viewModel = viewModel,
                    user = user,
                    onStartGroupSession = onStartGroupSession,
                    onNavigateToStats = onNavigateToStats
                )
            }
        }
    }
}

@Composable
private fun GoogleSignInGateCard(
    isBusy: Boolean,
    onSignInClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_sign_in_gate_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Cloud Connect",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Connect Your Focus Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sign in with Google to join co-op focus rooms, climb XP ranks, and sync your tree.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Co-op Timer", "XP Ranks", "Tree Sync").forEach { perk ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(vertical = 7.dp)
                    ) {
                        Text(
                            text = perk,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GoogleSignInButton(
                onClick = onSignInClick,
                isLoading = isBusy,
                modifier = Modifier.testTag("google_sign_in_button")
            )
        }
    }
}

@Composable
private fun SignedInSocialContent(
    viewModel: FocusViewModel,
    user: SupabaseUser,
    onStartGroupSession: (Int) -> Unit,
    onNavigateToStats: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Group Rooms, 1 = Leaderboard
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }

    val publicRooms by viewModel.publicRooms.collectAsState()
    val activeRoom by viewModel.activeRoom.collectAsState()
    val participants by viewModel.activeRoomParticipants.collectAsState()
    val leaderboard by viewModel.leaderboardEntries.collectAsState()
    val stats by viewModel.statsSummary.collectAsState()
    val playerLevel = (stats.focusScore / 200) + 1

    Column(modifier = Modifier.fillMaxSize()) {
        // Compact Gamified Rank Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName ?: user.email ?: "Reader",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "LVL $playerLevel • ${readerRankTitle(playerLevel)} • 🔥 ${stats.currentStreak}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            XpRewardChip(text = "${stats.focusScore} XP")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Group Rooms" to Icons.Default.Groups, "Leaderboard" to Icons.Default.EmojiEvents)
                .forEachIndexed { idx, (label, icon) ->
                    val isSelected = selectedSubTab == idx
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedSubTab = idx }
                            .padding(vertical = 10.dp)
                            .testTag("social_subtab_$idx"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularIconBadge(
                            imageVector = icon,
                            contentDescription = null,
                            containerSize = 24.dp,
                            iconSize = 14.dp,
                            containerColor = Color.Black,
                            iconTint = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedSubTab == 0) {
            val room = activeRoom
            if (room != null) {
                ActiveRoomView(
                    room = room,
                    currentUid = user.uid,
                    participants = participants,
                    onStartTogether = {
                        if (room.hostId == user.uid) {
                            viewModel.setRoomStatus(room.roomId, "active")
                        }
                        onStartGroupSession(room.durationMinutes)
                    },
                    onShareCode = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my FocusLock study room '${room.name}' (${room.durationMinutes}m)! Room Code: ${room.roomCode}"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Invite Friends to Focus Room"))
                    },
                    onLeaveRoom = { viewModel.leaveActiveRoom() }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        CinematicCardEntrance(index = 0) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = joinCodeInput,
                                        onValueChange = { joinCodeInput = it.uppercase().take(8) },
                                        placeholder = { Text("Enter Room Code") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("room_code_input")
                                    )
                                    Button(
                                        onClick = { viewModel.joinFocusRoomByCode(joinCodeInput) },
                                        colors = appBlackButtonColors(),
                                        border = appBlackButtonBorder(),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .height(54.dp)
                                            .testTag("join_room_by_code_btn")
                                    ) {
                                        Text("JOIN", color = Color.White, fontWeight = FontWeight.Black)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { showCreateRoomDialog = true },
                                    shape = RoundedCornerShape(50),
                                    colors = appBlackButtonColors(),
                                    border = appBlackButtonBorder(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("open_create_room_btn")
                                ) {
                                    CircularIconBadge(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        containerSize = 24.dp,
                                        iconSize = 15.dp,
                                        containerColor = Color.Black,
                                        iconTint = Color.White,
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CREATE GROUP FOCUS ROOM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }
                        }
                    }

                    item {
                        Text(
                            text = "LIVE PUBLIC ROOMS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (publicRooms.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No active public rooms right now. Create one above and invite your friends!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(publicRooms, key = { it.roomId }) { itemRoom ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.joinFocusRoom(itemRoom.roomId) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = itemRoom.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Host: ${itemRoom.hostName} • ${itemRoom.durationMinutes}m • Code ${itemRoom.roomCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${itemRoom.participantCount} in room",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LeaderboardView(
                entries = leaderboard,
                currentUid = user.uid,
                onSyncNow = { viewModel.syncCloudNow() },
                onNavigateToStats = onNavigateToStats
            )
        }
    }

    if (showCreateRoomDialog) {
        CreateRoomDialog(
            onCreate = { name, duration, isPublic ->
                viewModel.createFocusRoom(name, duration, isPublic)
                showCreateRoomDialog = false
            },
            onDismiss = { showCreateRoomDialog = false }
        )
    }
}

@Composable
private fun ActiveRoomView(
    room: FocusRoomDto,
    currentUid: String,
    participants: List<RoomParticipantDto>,
    onStartTogether: () -> Unit,
    onShareCode: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ROOM CODE: ${room.roomCode}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = room.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${room.durationMinutes}m Goal • Status: ${room.status.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = onShareCode,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50)
                        ) {
                            CircularIconBadge(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Code",
                                containerSize = 22.dp,
                                iconSize = 13.dp,
                                containerColor = Color.Black,
                                iconTint = Color.White,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("INVITE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onStartTogether,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            CircularIconBadge(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                containerSize = 24.dp,
                                iconSize = 15.dp,
                                containerColor = Color.Black,
                                iconTint = Color.White,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (room.hostId == currentUid) "START GROUP FOCUS" else "JOIN FOCUS TIMER",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = onLeaveRoom,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(48.dp)
                        ) {
                            CircularIconBadge(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Leave Room",
                                containerSize = 26.dp,
                                iconSize = 15.dp,
                                containerColor = Color.Black,
                                iconTint = Color.White,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "PARTICIPANTS (${participants.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        items(participants, key = { it.userId }) { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (p.userId == room.hostId) "${p.displayName} (Host)" else p.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "🌳 ${treeStageTitle(p.treeStage)} • Blocks: ${p.distractionAttempts}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = p.status.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardView(
    entries: List<CloudUserProfile>,
    currentUid: String,
    onSyncNow: () -> Unit,
    onNavigateToStats: (() -> Unit)? = null
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GLOBAL READERS RANKING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onNavigateToStats != null) {
                        Button(
                            onClick = onNavigateToStats,
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("MY STATS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = onSyncNow,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("SYNC SCORE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                CinematicCardEntrance(index = 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No leaderboard entries yet. Tap 'Sync Score' to publish your focus stats!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
        } else {
            itemsIndexed(entries, key = { _, item -> item.userId }) { index, entry ->
                val isSelf = entry.userId == currentUid
                val medal = when (index) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> "#${index + 1}"
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelf) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = if (isSelf) 1.5.dp else 1.dp,
                        color = if (isSelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = medal,
                                fontSize = if (index < 3) 20.sp else 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(36.dp)
                            )
                            Column {
                                Text(
                                    text = if (isSelf) "${entry.displayName} (You)" else entry.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Lvl ${(entry.focusScore / 200) + 1} • 🔥 ${entry.currentStreak}d • ${entry.totalFocusMinutes}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        XpRewardChip(text = "${entry.focusScore} XP")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(
    onCreate: (String, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var roomName by remember { mutableStateOf("Quiet Study Lounge") }
    var duration by remember { mutableIntStateOf(25) }
    var isPublic by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Create Focus Room",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it.take(50) },
                    label = { Text("Room Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Session Duration: ${duration}m",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 5f..120f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visible in Public Rooms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_create_room_btn")
                    ) {
                        Text("CANCEL", color = Color.White, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { onCreate(roomName, duration, isPublic) },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_create_room_btn")
                    ) {
                        Text("CREATE", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
