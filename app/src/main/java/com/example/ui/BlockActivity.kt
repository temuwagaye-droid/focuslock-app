package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.bedtime.BedtimeManager
import com.example.data.local.AppDatabase
import com.example.data.repository.FocusRepository
import com.example.service.FocusSessionManager
import com.example.ui.components.CircularIconBadge
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import java.util.Locale

class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val blockedPackage = intent?.getStringExtra("blocked_package").orEmpty()
        val focusModeName = intent?.getStringExtra("focus_mode").orEmpty()
        val isBedtimeIntent = intent?.getBooleanExtra("is_bedtime", false) == true
        val repo = FocusRepository(AppDatabase.getDatabase(applicationContext).focusDao())

        setContent {
            val isDark by repo.isDarkModeEnabled().collectAsState(initial = false)
            val accentId by repo.getAccentThemeId().collectAsState(initial = AppAccentTheme.VIOLET.id)
            val readingApp by repo.getReadingAppPackage().collectAsState(initial = FocusSessionManager.designatedReadingApp)
            val bedtimeActive by BedtimeManager.isBedtimeLockActive.collectAsState()
            val effectiveBedtime = isBedtimeIntent || bedtimeActive

            MyApplicationTheme(
                darkTheme = if (effectiveBedtime) true else isDark,
                accentTheme = AppAccentTheme.fromId(accentId)
            ) {
                Scaffold { innerPadding ->
                    BlockScreenContent(
                        blockedPackage = blockedPackage,
                        focusModeName = focusModeName,
                        isBedtime = effectiveBedtime,
                        modifier = Modifier.padding(innerPadding),
                        onReturnToReading = {
                            val targetPkg = readingApp ?: FocusSessionManager.designatedReadingApp
                            val launch = targetPkg?.let { packageManager.getLaunchIntentForPackage(it) }
                            if (launch != null) {
                                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launch)
                            } else {
                                startActivity(Intent(this, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                })
                            }
                            finish()
                        },
                        onEmergencyUnlock = {
                            BedtimeManager.triggerEmergencyOverride(this@BlockActivity, 5)
                            finish()
                        },
                        onGoHome = {
                            val home = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(home)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BlockScreenContent(
    blockedPackage: String,
    focusModeName: String = "",
    isBedtime: Boolean = false,
    modifier: Modifier = Modifier,
    onReturnToReading: () -> Unit,
    onEmergencyUnlock: () -> Unit = {},
    onGoHome: () -> Unit
) {
    val sessionActive by FocusSessionManager.isSessionActive.collectAsState()
    val timeLeft by FocusSessionManager.timeLeftSeconds.collectAsState()
    val blockedCount by FocusSessionManager.blockedAttemptsCount.collectAsState()

    val wakeUpSeconds = BedtimeManager.getTimeUntilWakeUpSeconds()
    val wakeMins = (wakeUpSeconds / 60L) % 60L
    val wakeHours = wakeUpSeconds / 3600L

    val mins = timeLeft / 60L
    val secs = timeLeft % 60L
    val timeLabel = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    val modeTitle = if (focusModeName.isNotBlank()) "$focusModeName Barrier Active" else "Distraction Shield Engaged"
    val modeDescription = when (focusModeName) {
        "Deep Work" -> "All non-essential applications are locked for maximum mental depth. Phone calls and emergency services remain accessible."
        "Study" -> "Social apps and games are restricted during study focus. Study tools and notes remain accessible."
        "Strict Focus" -> "Strict lockdown is active. Pausing is disabled to preserve your productivity streak."
        "Relax & Recharge" -> "Guided breathing recovery is active. Take slow, relaxing breaths."
        else -> if (blockedPackage.isNotBlank()) "$blockedPackage is locked while your focus session is active." else "Stay with your task — your distracting apps are locked during this focus session."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isBedtime) Color(0xFF070B14) else MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(if (isBedtime) Color(0xFF131B2E) else Color.Black)
                .border(2.dp, if (isBedtime) Color(0xFF9070FF) else GamifiedPurple, CircleShape)
        ) {
            Icon(
                imageVector = if (isBedtime) Icons.Default.Bedtime else Icons.Default.Shield,
                contentDescription = if (isBedtime) "Bedtime Lock" else "Distraction Blocked",
                tint = if (isBedtime) Color(0xFFF0E6FF) else Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isBedtime) "Bedtime Lock Active 🌙" else modeTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = if (isBedtime) Color.White else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isBedtime) {
                "Distracting apps are locked to protect your sleep schedule. Wind down, relax, and get restorative rest."
            } else {
                modeDescription
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isBedtime) Color(0xFF94A3B8) else MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isBedtime) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, if (isBedtime) Color(0xFF1E293B) else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isBedtime) "WAKE-UP TIME IN" else if (sessionActive) "TIME REMAINING" else "FOCUS LOCK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isBedtime) Color(0xFF9070FF) else GamifiedPurple
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBedtime) "${wakeHours}h ${wakeMins}m" else timeLabel,
                    style = MaterialTheme.typography.displayLarge,
                    color = if (isBedtime) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isBedtime) "Emergency calls & essential tools remain accessible" else "$blockedCount distraction${if (blockedCount == 1) "" else "s"} blocked this session",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isBedtime) Color(0xFF38BDF8) else GamifiedMint,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isBedtime) {
            Button(
                onClick = onEmergencyUnlock,
                colors = appBlackButtonColors(),
                border = appBlackButtonBorder(),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("bedtime_emergency_unlock_btn")
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "5-MIN EMERGENCY UNLOCK",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        } else {
            Button(
                onClick = onReturnToReading,
                colors = appBlackButtonColors(),
                border = appBlackButtonBorder(),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("block_return_to_reading_btn")
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    containerSize = 28.dp,
                    iconSize = 16.dp,
                    containerColor = Color.Black,
                    iconTint = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RETURN TO READING",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onGoHome,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("block_go_home_btn")
        ) {
            Text(
                text = "EXIT TO HOME SCREEN",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isBedtime) Color.White else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
