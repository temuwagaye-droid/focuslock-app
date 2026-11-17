package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.repository.FocusRepository
import com.example.service.FocusSessionManager
import com.example.ui.components.CircularIconBadge
import com.example.ui.theme.AppAccentTheme
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val repository = FocusRepository(AppDatabase.getDatabase(applicationContext).focusDao())
        val prefs = getSharedPreferences("focuslock_prefs", MODE_PRIVATE)
        val initialDark = prefs.getBoolean("cached_dark_mode", false)
        val initialAccent = prefs.getString("cached_accent_theme", AppAccentTheme.VIOLET.id) ?: AppAccentTheme.VIOLET.id

        setContent {
            val isDarkModeEnabled by repository.isDarkModeEnabled().collectAsState(initial = initialDark)
            val accentThemeId by repository.getAccentThemeId().collectAsState(initial = initialAccent)
            val accentTheme = AppAccentTheme.fromId(accentThemeId)

            MyApplicationTheme(darkTheme = isDarkModeEnabled, accentTheme = accentTheme) {
                BackHandler {
                    // Intercept back button so users cannot bypass the focus session
                }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    BlockScreenContent(
                        blockedPackage = blockedPackage,
                        modifier = Modifier.padding(innerPadding),
                        onReturnToReading = {
                            val readingApp = FocusSessionManager.designatedReadingApp
                            if (!readingApp.isNullOrEmpty()) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(readingApp)
                                if (launchIntent != null) {
                                    startActivity(launchIntent)
                                    finish()
                                    return@BlockScreenContent
                                }
                            }
                            // Fallback to launcher home
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        },
                        onEmergencyExit = {
                            FocusSessionManager.stopSession(this, completed = false)
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
    }
}

@Composable
fun BlockScreenContent(
    blockedPackage: String,
    modifier: Modifier = Modifier,
    onReturnToReading: () -> Unit,
    onEmergencyExit: () -> Unit
) {
    val context = LocalContext.current
    val isSessionActive by FocusSessionManager.isSessionActive.collectAsState()
    val timeLeftSeconds by FocusSessionManager.timeLeftSeconds.collectAsState()
    val blockedAttempts by FocusSessionManager.blockedAttemptsCount.collectAsState()

    val formattedTime = remember(timeLeftSeconds) {
        val mins = timeLeftSeconds / 60
        val secs = timeLeftSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    // Dynamic label for the blocked app
    val appLabel = remember(blockedPackage) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(blockedPackage, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "a distracted app"
        }
    }

    // Auto-finish BlockActivity if session ends
    LaunchedEffect(isSessionActive) {
        if (!isSessionActive) {
            onReturnToReading()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val isDark = LocalDarkTheme.current
            // Gamification/Status Badge (Chunky border & background)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isDark) Color(0xFF4A2532) else Color(0xFFFFD8E4))
                    .border(1.dp, if (isDark) Color(0xFFFFB1C8) else Color(0xFFD78391), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ReadLock Focus Active 🔒",
                    color = if (isDark) Color(0xFFFFD8E4) else Color(0xFF31111D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Distraction Attempts Counter Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .testTag("block_screen_attempt_counter")
            ) {
                Text(
                    text = "🛡️ Distraction Attempt #$blockedAttempts Blocked",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main warning header
            Text(
                text = "DISTRACTION BLOCKED",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = appLabel,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Friendly encouraging message
            Text(
                text = "You requested to lock distractions during your reading session. Let's build your habit streak! Keep your nose in your book.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Giant countdown display
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = formattedTime,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "UNTIL FREE TIME",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Return to Reading App button (chunky solid action)
            Button(
                onClick = onReturnToReading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("return_to_reading_button")
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    containerSize = 28.dp,
                    iconSize = 16.dp,
                    containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    iconTint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BACK TO READING APP",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Strict Lock Information Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "STRICT LOCK ENGAGED",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No early exit or bypass is allowed. All distractions are locked until the timer expires. Keep focused!",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
