package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.service.FocusSessionManager
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

        setContent {
            MyApplicationTheme {
                Scaffold(
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

    override fun onBackPressed() {
        // Intercept back button so users cannot bypass the focus session
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

    var showOverrideInput by remember { mutableStateOf(false) }
    var typedPhrase by remember { mutableStateOf("") }
    val requiredPhrase = "I am giving up on my reading"

    // Hold down gesture state
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val progressAnimated by animateFloatAsState(
        targetValue = holdProgress,
        animationSpec = tween(durationMillis = 100),
        label = "hold_progress"
    )

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
            // Gamification/Status Badge (Chunky border & background)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFFD8E4))
                    .border(1.dp, Color(0xFFD78391), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ReadLock Focus Active 🔒",
                    color = Color(0xFF31111D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    containerColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                    .testTag("return_to_reading_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "BACK TO READING APP",
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Break Glass trigger button
            if (!showOverrideInput) {
                Button(
                    onClick = { showOverrideInput = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("break_glass_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = "Exit Session (Emergency Override)",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expanded Emergency Override form
            AnimatedVisibility(visible = showOverrideInput) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFDAD6)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB4AB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "EMERGENCY OVERRIDE",
                            color = Color(0xFF410002),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "To break the lock, type the phrase exactly as shown below:",
                            color = Color(0xFF410002),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "\"$requiredPhrase\"",
                            color = Color(0xFF410002),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = typedPhrase,
                            onValueChange = { typedPhrase = it },
                            placeholder = { Text("Type phrase here to unlock...", color = Color(0xFF410002).copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF410002),
                                unfocusedTextColor = Color(0xFF410002),
                                focusedBorderColor = Color(0xFF410002),
                                unfocusedBorderColor = Color(0xFF410002).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("override_text_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Only show Hold Button if phrase matches
                        AnimatedVisibility(visible = typedPhrase.trim().lowercase() == requiredPhrase.lowercase()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Now, press and hold the button for 10 seconds:",
                                    color = Color(0xFF410002),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val holdBtnBg by animateColorAsState(
                                    targetValue = if (isHolding) Color(0xFFBA1A1A) else Color(0xFF410002),
                                    label = "hold_btn_color"
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(holdBtnBg)
                                        .border(2.dp, Color(0xFF410002), RoundedCornerShape(50))
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isHolding = true
                                                    holdProgress = 0f
                                                    holdJob = scope.launch {
                                                        for (i in 1..100) {
                                                            delay(100)
                                                            holdProgress = i / 100f
                                                        }
                                                        onEmergencyExit()
                                                    }
                                                    try {
                                                        awaitRelease()
                                                    } finally {
                                                        isHolding = false
                                                        holdJob?.cancel()
                                                        holdProgress = 0f
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("hold_to_exit_button")
                                ) {
                                    // Progress bar inside button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFFFFB4AB).copy(alpha = 0.5f),
                                                        Color(0xFFFFB4AB)
                                                    )
                                                )
                                            )
                                            .fillMaxWidth(progressAnimated)
                                            .align(Alignment.CenterStart)
                                    )

                                    Text(
                                        text = if (isHolding) "HOLDING... (${(progressAnimated * 10).toInt()}s)" else "PRESS & HOLD TO ABORT",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showOverrideInput = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "Nevermind, I will keep reading",
                                color = Color(0xFF410002),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
