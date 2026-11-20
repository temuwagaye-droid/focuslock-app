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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Shield
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
        val repo = FocusRepository(AppDatabase.getDatabase(applicationContext).focusDao())

        setContent {
            val isDark by repo.isDarkModeEnabled().collectAsState(initial = false)
            val accentId by repo.getAccentThemeId().collectAsState(initial = AppAccentTheme.VIOLET.id)
            val readingApp by repo.getReadingAppPackage().collectAsState(initial = FocusSessionManager.designatedReadingApp)

            MyApplicationTheme(
                darkTheme = isDark,
                accentTheme = AppAccentTheme.fromId(accentId)
            ) {
                Scaffold { innerPadding ->
                    BlockScreenContent(
                        blockedPackage = blockedPackage,
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
    modifier: Modifier = Modifier,
    onReturnToReading: () -> Unit,
    onGoHome: () -> Unit
) {
    val sessionActive by FocusSessionManager.isSessionActive.collectAsState()
    val timeLeft by FocusSessionManager.timeLeftSeconds.collectAsState()
    val blockedCount by FocusSessionManager.blockedAttemptsCount.collectAsState()

    val mins = timeLeft / 60L
    val secs = timeLeft % 60L
    val timeLabel = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, GamifiedPurple, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Distraction Blocked",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Distraction Shield Engaged",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (blockedPackage.isNotBlank()) {
                "$blockedPackage is locked while your reading focus session is active."
            } else {
                "Stay with your book — your distracting apps are locked during this focus session."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (sessionActive) "TIME REMAINING" else "FOCUS LOCK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = GamifiedPurple
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$blockedCount distraction${if (blockedCount == 1) "" else "s"} blocked this session",
                    style = MaterialTheme.typography.labelMedium,
                    color = GamifiedMint,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                containerSize = 26.dp,
                iconSize = 15.dp,
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
                fontSize = 12.sp
            )
        }
    }
}
