package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.FocusViewModel
import com.example.ui.components.AppSwitch
import com.example.ui.components.CircularIconBadge
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.readerRankTitle
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors

@Composable
fun SocialScreen(
    viewModel: FocusViewModel,
    onApplySuggestedDuration: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartFocusSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthBusy by viewModel.isAuthBusy.collectAsState()
    val stats by viewModel.statsSummary.collectAsState()
    val cloudStatusMessage by viewModel.cloudStatusMessage.collectAsState()
    val publicRooms by viewModel.publicRooms.collectAsState()
    val activeRoom by viewModel.activeRoom.collectAsState()
    val participants by viewModel.activeRoomParticipants.collectAsState()
    val roomMessages by viewModel.activeRoomMessages.collectAsState()
    var chatMessageInput by remember { mutableStateOf("") }
    var showAttachDialog by remember { mutableStateOf(false) }
    var attachType by remember { mutableStateOf("note") } // "note", "file", "link"
    var attachTitleInput by remember { mutableStateOf("") }
    var attachUrlInput by remember { mutableStateOf("") }
    val leaderboard by viewModel.leaderboardEntries.collectAsState()
    val isLeaderboardVisible by viewModel.isLeaderboardVisible.collectAsState()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Group Rooms, 1 = Leaderboard
    var joinCodeInput by remember { mutableStateOf("") }
    var showCreateRoomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("social_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (currentUser == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularIconBadge(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Focus Rooms",
                        containerSize = 60.dp,
                        iconSize = 30.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Focus Rooms & Leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sign in with Google via Supabase to create study rooms, invite friends with a 6-character code, and track live reading ranks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    cloudStatusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    GoogleSignInButton(
                        onClick = { viewModel.signInWithGoogle(context) },
                        isLoading = isAuthBusy,
                        modifier = Modifier.testTag("social_google_sign_in_btn")
                    )
                }
            }
            return@Column
        }

        val user = currentUser!!
        val level = (stats.focusScore / 200) + 1

        // Compact Player Banner + Sub-Tab Switcher
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    ) {
                        Text(
                            text = "L$level",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${user.displayName ?: user.email ?: "Reader"} • ${readerRankTitle(level)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${stats.focusScore} XP • ${stats.currentStreak}d Streak",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Group Rooms", "Leaderboard").forEachIndexed { index, title ->
                        val active = selectedSubTab == index
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedSubTab = index }
                                .testTag("social_tab_$index")
                        ) {
                            Text(
                                text = title,
                                fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        cloudStatusMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        if (selectedSubTab == 0) {
            val room = activeRoom
            if (room != null) {
                // Active Focus Room View
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_focus_room_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${room.status.uppercase()} • ${room.durationMinutes}M SPRINT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = room.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Hosted by ${room.hostName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(room.roomCode))
                                    Toast.makeText(context, "Copied code ${room.roomCode}", Toast.LENGTH_SHORT).show()
                                },
                                colors = appBlackButtonColors(),
                                border = appBlackButtonBorder(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Room Code",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(room.roomCode, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            participants.forEach { participant ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                            ) {
                                                Text(
                                                    text = participant.displayName.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = participant.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "Stage ${participant.treeStage} Tree • ${participant.distractionAttempts} alerts",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = participant.status.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0A6B43)
                                        )
                                    }
                                }
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE ROOM CHAT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${roomMessages.size} messages",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp, max = 220.dp)
                        ) {
                            if (roomMessages.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Say hello to your focus room members! 👋",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(roomMessages) { msg ->
                                        val isMe = msg.senderId == currentUser?.uid
                                        val timeStr = remember(msg.timestampMillis) {
                                            runCatching {
                                                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestampMillis))
                                            }.getOrDefault("")
                                        }
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (isMe) "You" else msg.senderName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                )
                                                if (timeStr.isNotBlank()) {
                                                    Text(
                                                        text = timeStr,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (msg.content.isNotBlank()) {
                                                        Text(
                                                            text = msg.content,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    if (!msg.attachmentName.isNullOrBlank()) {
                                                        val attachIcon = when (msg.attachmentType) {
                                                            "file" -> Icons.Default.AttachFile
                                                            "link" -> Icons.Default.Link
                                                            else -> Icons.Default.Description
                                                        }
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(MaterialTheme.colorScheme.surface)
                                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = attachIcon,
                                                                contentDescription = null,
                                                                tint = GamifiedPurple,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Column {
                                                                Text(
                                                                    text = msg.attachmentName ?: "Attachment",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                if (!msg.attachmentUrl.isNullOrBlank()) {
                                                                    Text(
                                                                        text = msg.attachmentUrl ?: "",
                                                                        fontSize = 9.sp,
                                                                        color = MaterialTheme.colorScheme.secondary,
                                                                        maxLines = 1
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
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showAttachDialog = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Share File or Note",
                                    tint = GamifiedPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            OutlinedTextField(
                                value = chatMessageInput,
                                onValueChange = { chatMessageInput = it },
                                placeholder = { Text("Message room...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (chatMessageInput.isNotBlank()) {
                                        viewModel.sendRoomMessage(chatMessageInput.trim())
                                        chatMessageInput = ""
                                    }
                                },
                                enabled = chatMessageInput.isNotBlank(),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (chatMessageInput.isNotBlank()) MaterialTheme.colorScheme.primary
                                        else Color.Gray.copy(alpha = 0.3f)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send Message",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onApplySuggestedDuration(room.durationMinutes)
                                viewModel.setRoomStatus(room.roomId, "active")
                                onStartFocusSession()
                            },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_group_focus_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "START GROUP FOCUS (${room.durationMinutes}M)",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.leaveActiveRoom() },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("LEAVE ROOM", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // Join / Create Room Card
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
                            text = "JOIN BY ROOM CODE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase().take(6) },
                                placeholder = { Text("6-CHAR CODE") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_room_code_input")
                            )
                            Button(
                                onClick = { viewModel.joinFocusRoomByCode(joinCodeInput) },
                                colors = appBlackButtonColors(),
                                border = appBlackButtonBorder(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("join_room_code_btn")
                            ) {
                                Text("JOIN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { showCreateRoomDialog = true },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("open_create_room_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CREATE FOCUS ROOM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                // Live Public Rooms
                Text(
                    text = "LIVE PUBLIC STUDY ROOMS (${publicRooms.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )

                if (publicRooms.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No public rooms yet. Tap CREATE FOCUS ROOM to launch a live reading sprint.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    publicRooms.forEach { r ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = r.name, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Text(
                                        text = "Host: ${r.hostName} • ${r.durationMinutes}m • Code ${r.roomCode}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Button(
                                    onClick = { viewModel.joinFocusRoom(r.roomId) },
                                    colors = appBlackButtonColors(),
                                    border = appBlackButtonBorder(),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Text("JOIN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Leaderboard Sub-Tab
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularIconBadge(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Leaderboard",
                                containerSize = 32.dp,
                                iconSize = 17.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "READER LEADERBOARD",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                        OutlinedButton(
                            onClick = onNavigateToSettings,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("PRIVACY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isLeaderboardVisible) {
                        Text(
                            text = "You have hidden your profile from the leaderboard in Settings. Enable visibility below to compare scores.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Button(
                            onClick = { viewModel.setLeaderboardVisible(true) },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("SHOW MY PROFILE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    } else {
                        leaderboard.sortedByDescending { it.focusScore }.forEachIndexed { idx, entry ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${idx + 1}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(text = entry.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                text = "${entry.totalFocusMinutes}m read • ${entry.currentStreak}d streak • Stage ${entry.treeStage}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${entry.focusScore} XP",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0A6B43),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(GamifiedMint.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    if (showAttachDialog) {
        AlertDialog(
            onDismissRequest = { showAttachDialog = false },
            title = {
                Text(
                    text = "Share Resource with Room",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose resource type to share with fellow participants:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("note" to "Study Note", "file" to "Doc/File", "link" to "Link").forEach { (typeKey, label) ->
                            val isSel = attachType == typeKey
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) GamifiedPurple else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { attachType = typeKey }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = attachTitleInput,
                        onValueChange = { attachTitleInput = it },
                        label = { Text("Title / Resource Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Chapter 4 Key Points", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = attachUrlInput,
                        onValueChange = { attachUrlInput = it },
                        label = { Text(if (attachType == "link") "URL / Link" else "Content / Notes / File Ref", fontSize = 11.sp) },
                        placeholder = { Text(if (attachType == "link") "https://..." else "Notes or file details...", fontSize = 11.sp) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (attachTitleInput.isNotBlank()) {
                            viewModel.sendRoomMessage(
                                text = if (chatMessageInput.isNotBlank()) chatMessageInput.trim() else "Shared $attachType: ${attachTitleInput.trim()}",
                                attachmentType = attachType,
                                attachmentName = attachTitleInput.trim(),
                                attachmentUrl = attachUrlInput.trim().takeIf { it.isNotBlank() }
                            )
                            chatMessageInput = ""
                            attachTitleInput = ""
                            attachUrlInput = ""
                            showAttachDialog = false
                        }
                    },
                    enabled = attachTitleInput.isNotBlank(),
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("SHARE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAttachDialog = false },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )
    }

    if (showCreateRoomDialog) {
        CreateFocusRoomDialog(
            onCreate = { name, duration, isPublic ->
                viewModel.createFocusRoom(name, duration, isPublic)
                showCreateRoomDialog = false
            },
            onDismiss = { showCreateRoomDialog = false }
        )
    }
}

@Composable
private fun CreateFocusRoomDialog(
    onCreate: (name: String, durationMinutes: Int, isPublic: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("Quiet Reading Lounge") }
    var duration by remember { mutableIntStateOf(25) }
    var isPublic by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_room_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Create Group Focus Room",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Room Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_room_name_input")
                )
                Text(
                    text = "SPRINT DURATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = duration == mins,
                            onClick = { duration = mins },
                            label = { Text("${mins}m", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Public Room (visible in lobby)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    AppSwitch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onCreate(name, duration, isPublic) },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_create_room_btn")
                    ) {
                        Text("CREATE ROOM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
