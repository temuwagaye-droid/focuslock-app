package com.example.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import com.example.ui.FocusStatsSummary
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.AmbientSoundPlayer
import com.example.audio.AmbientSoundType
import com.example.data.model.TodoEntity
import com.example.security.ParentalPinDialog
import com.example.ui.components.AccessibilityRequiredDialog
import com.example.ui.components.AppLauncherIcon
import com.example.ui.components.AppSwitch
import com.example.ui.components.BlockedAppsRequiredDialog
import com.example.ui.components.CinematicCardEntrance
import com.example.ui.components.CircularIconBadge
import com.example.ui.components.DailyQuestsRingsCard
import com.example.ui.components.FitnessDurationRing
import com.example.ui.components.FocusForestRewardCard
import com.example.ui.components.GamifiedLevelHudCard
import com.example.ui.components.PremiumUpgradeDialog
import com.example.ui.components.ReaderAndShieldLoadoutRow
import com.example.ui.components.ReadingAppRequiredDialog
import com.example.ui.components.ReadingSelectedGoToBlockAppsDialog
import com.example.ui.components.RealTimeFocusCountdownRing
import com.example.ui.components.TimerBadgesStripCard
import com.example.ui.components.TimerXpAndStreakCard
import com.example.ui.components.XpRewardChip
import com.example.ui.components.calculateTreeStage
import com.example.ui.components.readerRankTitle
import com.example.ui.components.treeStageTitle
import com.example.ui.theme.GamifiedMint
import com.example.ui.theme.GamifiedPurple
import com.example.ui.theme.GamifiedYellow
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SessionLogEntity
import com.example.service.FocusSessionManager
import com.example.service.ReadLockAccessibilityService
import com.example.ui.AppInfoModel
import com.example.ui.FocusViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val KEY_ACCESSIBILITY_ENABLED_ONCE = "accessibility_enabled_once"

fun hasAccessibilityBeenEnabledOnce(context: Context): Boolean {
    return context.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE)
        .getBoolean(KEY_ACCESSIBILITY_ENABLED_ONCE, false)
}

fun markAccessibilityEnabledOnce(context: Context) {
    context.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ACCESSIBILITY_ENABLED_ONCE, true)
        .apply()
}

// Utility to check accessibility permission reliably (live binder, AccessibilityManager, and Settings.Secure)
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    if (ReadLockAccessibilityService.isServiceConnected.value) {
        markAccessibilityEnabledOnce(context)
        return true
    }
    return try {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val activeViaManager = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { info ->
                val serviceInfo = info.resolveInfo?.serviceInfo
                (serviceInfo?.packageName == context.packageName &&
                    serviceInfo.name == ReadLockAccessibilityService::class.java.name) ||
                    info.id.orEmpty().contains("ReadLockAccessibilityService")
            } == true
        if (activeViaManager) {
            markAccessibilityEnabledOnce(context)
            return true
        }

        val serviceClass = ReadLockAccessibilityService::class.java
        val expectedComponentName = android.content.ComponentName(context, serviceClass)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
            if ((enabledService != null && enabledService == expectedComponentName) ||
                componentNameString.contains("ReadLockAccessibilityService")
            ) {
                markAccessibilityEnabledOnce(context)
                return true
            }
        }
        false
    } catch (_: Exception) {
        false
    }
}

@Composable
fun AppSelectionScreen(
    viewModel: FocusViewModel,
    onNavigateToTimer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()
    val blockedApps by viewModel.blockedAppsList.collectAsState()
    val isWhitelistMode by viewModel.isWhitelistMode.collectAsState()
    val isProTier by viewModel.isProTier.collectAsState()
    val accentThemeId by viewModel.accentThemeId.collectAsState()
    val showProLimitPrompt by viewModel.showProLimitPrompt.collectAsState()
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val parentalPinHash by viewModel.parentalPinHash.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSelectedOnly by remember { mutableStateOf(false) }
    var pendingParentalAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun runWithParentalCheck(action: () -> Unit) {
        if (isParentalLockEnabled && parentalPinHash.isNotBlank()) {
            pendingParentalAction = action
        } else {
            action()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledLauncherApps(context)
    }

    val blockedPackagesSet = remember(blockedApps) {
        blockedApps.filter { it.isBlocked }.map { it.packageName }.toSet()
    }

    val selectedAppModels = remember(installedApps, blockedApps, blockedPackagesSet) {
        val installedByPkg = installedApps.associateBy { it.packageName }
        val fromInstalled = installedApps.filter { blockedPackagesSet.contains(it.packageName) }
        val extraBlocked = blockedApps
            .filter { it.isBlocked && !installedByPkg.containsKey(it.packageName) }
            .map { AppInfoModel(packageName = it.packageName, label = it.appName) }
        (fromInstalled + extraBlocked).sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    val filteredApps = remember(installedApps, selectedAppModels, showSelectedOnly, searchQuery) {
        val baseList = if (showSelectedOnly) selectedAppModels else installedApps
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "App Shield",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (showSelectedOnly) {
                        "Showing selected blocked apps only (${blockedPackagesSet.size})."
                    } else {
                        "Choose apps to block during reading sessions."
                    },
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .defaultMinSize(minHeight = 36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (showSelectedOnly) GamifiedPurple
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .border(
                        width = 1.dp,
                        color = if (showSelectedOnly) GamifiedPurple
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (showSelectedOnly) "Show all installed apps" else "Show selected apps only"
                    ) { showSelectedOnly = !showSelectedOnly }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("shield_slots_filter_button")
            ) {
                Text(
                    text = when {
                        showSelectedOnly -> "SELECTED (${blockedPackagesSet.size})"
                        !isProTier -> "${blockedPackagesSet.size}/${FocusViewModel.FREE_TIER_MAX_BLOCKED_APPS} SLOTS"
                        else -> "${blockedPackagesSet.size} SLOTS"
                    },
                    color = if (showSelectedOnly) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (!isProTier) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    (blockedPackagesSet.size.toFloat() / FocusViewModel.FREE_TIER_MAX_BLOCKED_APPS.toFloat()).coerceIn(0f, 1f)
                },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Whitelist Toggle Card (Gated behind Pro tier)
        CinematicCardEntrance(index = 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularIconBadge(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                containerSize = 28.dp,
                                iconSize = 15.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WHITELIST MODE",
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black
                            )
                            if (!isProTier) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = Color(0xFFF3E5AB),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isProTier) {
                                "Blocks every app except your reading app."
                            } else {
                                "Pro: auto-block all apps except your reader."
                            },
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    AppSwitch(
                        checked = isProTier && isWhitelistMode,
                        onCheckedChange = { enabled ->
                            if (!isProTier) {
                                // Free tier cannot enable Whitelist Mode; open Pro upgrade prompt
                                viewModel.toggleWhitelistMode(true)
                            } else {
                                runWithParentalCheck { viewModel.toggleWhitelistMode(enabled) }
                            }
                        },
                        modifier = Modifier.testTag("whitelist_mode_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!(isProTier && isWhitelistMode)) {
            CinematicCardEntrance(index = 1) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = {
                        CircularIconBadge(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            containerSize = 32.dp,
                            iconSize = 18.dp
                        )
                    },
                    placeholder = { Text("Search apps...", color = MaterialTheme.colorScheme.secondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showSelectedOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GamifiedPurple.copy(alpha = 0.14f))
                        .border(1.dp, GamifiedPurple.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("shield_selected_filter_banner"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredApps.size} selected app${if (filteredApps.size == 1) "" else "s"} only",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SHOW ALL",
                        color = GamifiedPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showSelectedOnly = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("shield_show_all_apps_btn")
                    )
                }
            }

            if (filteredApps.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                        .padding(24.dp)
                        .testTag("shield_empty_list_state")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            containerSize = 56.dp,
                            iconSize = 28.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (showSelectedOnly && blockedPackagesSet.isEmpty()) {
                                "No Selected Apps Yet"
                            } else {
                                "No Matching Apps"
                            },
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (showSelectedOnly && blockedPackagesSet.isEmpty()) {
                                "Tap Show All to select distracting apps you want blocked during reading sessions."
                            } else {
                                "No apps match your current filter."
                            },
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        if (showSelectedOnly) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showSelectedOnly = false },
                                colors = appBlackButtonColors(),
                                border = appBlackButtonBorder(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("shield_empty_show_all_btn")
                            ) {
                                Text(
                                    text = "SHOW ALL APPS",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Apps list styled with Launcher Icon + App Label
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                        val isChecked = blockedPackagesSet.contains(app.packageName)
                        CinematicCardEntrance(index = index.coerceAtMost(5)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isChecked) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .border(
                                        width = if (isChecked) 1.5.dp else 1.dp,
                                        color = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        runWithParentalCheck {
                                            viewModel.toggleAppBlock(app.packageName, app.label, !isChecked)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AppLauncherIcon(
                                    packageName = app.packageName,
                                    contentDescription = "${app.label} launcher icon",
                                    size = 40.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isChecked) "Shielded during focus" else app.packageName,
                                        color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp,
                                        fontFamily = if (isChecked) FontFamily.SansSerif else FontFamily.Monospace,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { want ->
                                        runWithParentalCheck {
                                            viewModel.toggleAppBlock(app.packageName, app.label, want)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = MaterialTheme.colorScheme.background
                                    ),
                                    modifier = Modifier.testTag("app_checkbox_${app.packageName}")
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Whitelist mode info card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularIconBadge(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        containerSize = 72.dp,
                        iconSize = 38.dp,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "WHITELIST ENGAGED",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Only FocusLock and your designated reading app remain open during sessions.",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (onNavigateToTimer != null && (blockedPackagesSet.isNotEmpty() || (isProTier && isWhitelistMode))) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToTimer,
                colors = appBlackButtonColors(),
                border = appBlackButtonBorder(),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apps_go_to_timer_button")
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    containerSize = 24.dp,
                    iconSize = 14.dp,
                    containerColor = Color.Black,
                    iconTint = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "READY? GO TO FOCUS TIMER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }

    pendingParentalAction?.let { action ->
        ParentalPinDialog(
            title = "Parental Control Active",
            subtitle = "Enter your parental PIN to modify app blocking settings.",
            storedPinHash = parentalPinHash,
            onPinConfirmed = {
                pendingParentalAction = null
                action()
            },
            onDismiss = { pendingParentalAction = null }
        )
    }

    if (showProLimitPrompt) {
        PremiumUpgradeDialog(
            isProActive = isProTier,
            selectedThemeId = accentThemeId,
            onTogglePro = { viewModel.setProTier(it) },
            onSelectTheme = { viewModel.selectAccentTheme(it) },
            onDismiss = { viewModel.dismissProLimitPrompt() }
        )
    }
}


@Composable
fun BedtimeDashboardCard(
    onNavigateToBedtime: () -> Unit
) {
    val schedule by com.example.bedtime.BedtimeManager.schedule.collectAsState()
    val isLockActive by com.example.bedtime.BedtimeManager.isBedtimeLockActive.collectAsState()
    val stats by com.example.bedtime.BedtimeManager.stats.collectAsState()
    val nextSeconds = remember(schedule, isLockActive) {
        if (isLockActive) com.example.bedtime.BedtimeManager.getTimeUntilWakeUpSeconds()
        else com.example.bedtime.BedtimeManager.getTimeUntilNextBedtimeSeconds()
    }
    val h = nextSeconds / 3600L
    val m = (nextSeconds % 3600L) / 60L

    val sleepDurationMinutes = remember(schedule) {
        var startTotal = schedule.bedtimeHour * 60 + schedule.bedtimeMinute
        var endTotal = schedule.wakeUpHour * 60 + schedule.wakeUpMinute
        if (endTotal <= startTotal) {
            endTotal += 24 * 60
        }
        endTotal - startTotal
    }
    val sleepH = sleepDurationMinutes / 60
    val sleepM = sleepDurationMinutes % 60

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF090D1A)
        ),
        shape = RoundedCornerShape(24.dp),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToBedtime() }
            .testTag("bedtime_dashboard_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF090D1A),
                            Color(0xFF151A36),
                            Color(0xFF0C1122)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1B4B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Bedtime Clock",
                                tint = Color(0xFFDDD6FE),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BEDTIME CLOCK",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.8.sp,
                                    color = Color(0xFFA5B4FC)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isLockActive) Color(0xFF9070FF).copy(alpha = 0.3f)
                                            else if (schedule.isEnabled) Color(0xFF0A6B43).copy(alpha = 0.35f)
                                            else Color.White.copy(alpha = 0.1f)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isLockActive) "ACTIVE" else if (schedule.isEnabled) "SCHEDULED" else "OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isLockActive) Color(0xFFC084FC) else if (schedule.isEnabled) Color(0xFF34D399) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    // Sleep Streak Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFBBF24).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🔥 ${stats.currentStreakDays}D STREAK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }

                // Dual Clock Pods (Bedtime & Wake-up) with Sleep Duration in the middle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF050811).copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bedtime Pod
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BEDTIME",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatBedtimeClock(schedule.bedtimeHour, schedule.bedtimeMinute),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // Sleep Duration Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (sleepM > 0) "~${sleepH}h ${sleepM}m rest" else "~${sleepH}h rest",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    // Wake-up Pod
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "WAKE UP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatBedtimeClock(schedule.wakeUpHour, schedule.wakeUpMinute),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Bottom Countdown & Action indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLockActive) "⏰ Wake-up alarm in ${h}h ${m}m" else "🌙 Curfew begins in ${h}h ${m}m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Configure",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FocusModeScrollableTabsSection(
    selectedMode: FocusSessionManager.FocusMode?,
    onSelectMode: (FocusSessionManager.FocusMode?) -> Unit,
    onStartFocusMode: (FocusSessionManager.FocusMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = remember {
        listOf(
            FocusSessionManager.FocusMode.DEEP_WORK,
            FocusSessionManager.FocusMode.STUDY,
            FocusSessionManager.FocusMode.POMODORO,
            FocusSessionManager.FocusMode.READING,
            FocusSessionManager.FocusMode.QUICK_FOCUS,
            FocusSessionManager.FocusMode.STRICT_FOCUS,
            FocusSessionManager.FocusMode.RELAX_RECHARGE,
            FocusSessionManager.FocusMode.CUSTOM
        )
    }

    val selectedIndex = if (selectedMode == null) -1 else modes.indexOf(selectedMode)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            // Scrollable Tab Row (Top Tabs) - Clean, small, no header text
            ScrollableTabRow(
                selectedTabIndex = if (selectedIndex >= 0) selectedIndex else 0,
                edgePadding = 8.dp,
                containerColor = Color.Transparent,
                contentColor = GamifiedPurple,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedIndex in tabPositions.indices) {
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(GamifiedPurple)
                        )
                    }
                }
            ) {
                modes.forEachIndexed { index, mode ->
                    val isSelected = index == selectedIndex
                    val modeIcon = when (mode) {
                        FocusSessionManager.FocusMode.DEEP_WORK -> Icons.Default.Psychology
                        FocusSessionManager.FocusMode.STUDY -> Icons.Default.School
                        FocusSessionManager.FocusMode.POMODORO -> Icons.Default.Timer
                        FocusSessionManager.FocusMode.READING -> Icons.Default.Book
                        FocusSessionManager.FocusMode.QUICK_FOCUS -> Icons.Default.Bolt
                        FocusSessionManager.FocusMode.STRICT_FOCUS -> Icons.Default.Lock
                        FocusSessionManager.FocusMode.RELAX_RECHARGE -> Icons.Default.SelfImprovement
                        FocusSessionManager.FocusMode.CUSTOM -> Icons.Default.Tune
                    }

                    Tab(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                onSelectMode(null)
                            } else {
                                onSelectMode(mode)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) GamifiedPurple.copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = mode.displayName,
                                tint = if (isSelected) GamifiedPurple else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) GamifiedPurple else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) GamifiedPurple.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${mode.defaultMinutes}m",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) GamifiedPurple else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Modern, Gamified, Non-Boring Mode Details Card with explicit Business Logic!
            AnimatedVisibility(
                visible = selectedMode != null,
                enter = expandVertically(animationSpec = tween(240)) + fadeIn(tween(180)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(140))
            ) {
                if (selectedMode != null) {
                    val activeModeIcon = when (selectedMode) {
                        FocusSessionManager.FocusMode.DEEP_WORK -> Icons.Default.Psychology
                        FocusSessionManager.FocusMode.STUDY -> Icons.Default.School
                        FocusSessionManager.FocusMode.POMODORO -> Icons.Default.Timer
                        FocusSessionManager.FocusMode.READING -> Icons.Default.Book
                        FocusSessionManager.FocusMode.QUICK_FOCUS -> Icons.Default.Bolt
                        FocusSessionManager.FocusMode.STRICT_FOCUS -> Icons.Default.Lock
                        FocusSessionManager.FocusMode.RELAX_RECHARGE -> Icons.Default.SelfImprovement
                        FocusSessionManager.FocusMode.CUSTOM -> Icons.Default.Tune
                    }

                    val xpReward = when (selectedMode) {
                        FocusSessionManager.FocusMode.DEEP_WORK -> "+220 XP"
                        FocusSessionManager.FocusMode.STUDY -> "+150 XP"
                        FocusSessionManager.FocusMode.POMODORO -> "+100 XP"
                        FocusSessionManager.FocusMode.READING -> "+90 XP"
                        FocusSessionManager.FocusMode.QUICK_FOCUS -> "+50 XP"
                        FocusSessionManager.FocusMode.STRICT_FOCUS -> "+180 XP"
                        FocusSessionManager.FocusMode.RELAX_RECHARGE -> "+40 XP"
                        FocusSessionManager.FocusMode.CUSTOM -> "+75 XP"
                    }

                    val blockedAppChips = when (selectedMode) {
                        FocusSessionManager.FocusMode.DEEP_WORK -> listOf("Instagram", "TikTok", "YouTube", "Games", "Netflix", "Web Browsers")
                        FocusSessionManager.FocusMode.STUDY -> listOf("Social Media", "Video Games", "TikTok/Reels", "Streaming Apps")
                        FocusSessionManager.FocusMode.POMODORO -> listOf("Configured Distraction Apps", "Social Apps during sprint")
                        FocusSessionManager.FocusMode.READING -> listOf("All apps except designated Reader", "Social & Chat Apps")
                        FocusSessionManager.FocusMode.QUICK_FOCUS -> listOf("Configured Distraction Blocklist")
                        FocusSessionManager.FocusMode.STRICT_FOCUS -> listOf("ALL 3rd-Party Applications (100% Strict Barrier)")
                        FocusSessionManager.FocusMode.RELAX_RECHARGE -> listOf("All distracting apps (Guided in FocusLock)")
                        FocusSessionManager.FocusMode.CUSTOM -> listOf("User Configured Blocklist")
                    }

                    val allowedAppChips = when (selectedMode) {
                        FocusSessionManager.FocusMode.DEEP_WORK -> listOf("Phone & Emergency Calls", "System Tools", "Clock/Calc")
                        FocusSessionManager.FocusMode.STUDY -> listOf("Study & PDF Tools", "Notes & Calculator", "Browser Research", "Calls")
                        FocusSessionManager.FocusMode.POMODORO -> listOf("Productivity Apps", "Full unlock on 5m/20m breaks")
                        FocusSessionManager.FocusMode.READING -> listOf("Designated Reader App", "Emergency Calls")
                        FocusSessionManager.FocusMode.QUICK_FOCUS -> listOf("All non-blocked apps", "Low friction workflow")
                        FocusSessionManager.FocusMode.STRICT_FOCUS -> listOf("Emergency Dialer & System Launcher only")
                        FocusSessionManager.FocusMode.RELAX_RECHARGE -> listOf("FocusLock Guided Box Breathing & Calming Soundscape")
                        FocusSessionManager.FocusMode.CUSTOM -> listOf("User Whitelisted Applications")
                    }

                    val phoneCapabilities = when (selectedMode) {
                        FocusSessionManager.FocusMode.DEEP_WORK -> listOf(
                            "🔕 Auto DND Active",
                            "⏱️ 90m Focus / 15m Break",
                            "🚪 Strict Exit Confirmation",
                            "⏸️ Pausing Allowed"
                        )
                        FocusSessionManager.FocusMode.STUDY -> listOf(
                            "🔕 Auto DND Active",
                            "⏱️ 50m Focus / 10m Break",
                            "📚 Study Tools Permitted",
                            "⏸️ Pausing Allowed"
                        )
                        FocusSessionManager.FocusMode.POMODORO -> listOf(
                            "🔄 4 Focus Cycles Engine",
                            "⏱️ 25m Sprint / 5m Break",
                            "☕ 20m Long Break on 4th",
                            "🔔 Sound Chimes on Breaks"
                        )
                        FocusSessionManager.FocusMode.READING -> listOf(
                            "📖 Reader App Auto-Protected",
                            "🎵 Ambient Soundscape",
                            "⏱️ 30m Focus / 5m Break",
                            "🚪 Standard Exit"
                        )
                        FocusSessionManager.FocusMode.QUICK_FOCUS -> listOf(
                            "⚡ Rapid 15m Sprint",
                            "🟢 Penalty-Free Exit",
                            "☕ 3m Optional Break",
                            "🔔 Soft Alerts"
                        )
                        FocusSessionManager.FocusMode.STRICT_FOCUS -> listOf(
                            "🚫 Pausing Strictly Disabled",
                            "🔒 100% Barrier Lock",
                            "⏱️ 60m Focus / 10m Break",
                            "🚪 Exit Confirmation Modal"
                        )
                        FocusSessionManager.FocusMode.RELAX_RECHARGE -> listOf(
                            "🫁 4-4-4-4 Box Breathing",
                            "🎵 Soothing White Noise",
                            "⏱️ 10m Nervous Recovery",
                            "🟢 Zero Penalty"
                        )
                        FocusSessionManager.FocusMode.CUSTOM -> listOf(
                            "⚙️ User-Tuned Duration",
                            "🎯 Custom Distraction Filter",
                            "🎵 Ambient Sound Picker",
                            "⏸️ Pausing Allowed"
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, start = 10.dp, end = 10.dp, bottom = 8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, GamifiedPurple.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Header: Icon + Name + Intensity + XP + Close
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        CircularIconBadge(
                                            imageVector = activeModeIcon,
                                            contentDescription = selectedMode.displayName,
                                            containerSize = 36.dp,
                                            iconSize = 20.dp,
                                            containerColor = GamifiedPurple
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = selectedMode.displayName,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(GamifiedMint.copy(alpha = 0.22f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = selectedMode.intensity,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0A6B43)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${selectedMode.defaultMinutes}m Focus • ${if (selectedMode.breakMinutes > 0) "${selectedMode.breakMinutes}m Break" else "Recovery"}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        XpRewardChip(text = xpReward)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onSelectMode(null) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hide Details",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Phone Access & Capabilities Matrix
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "PHONE ACCESS & CAPABILITIES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = GamifiedPurple,
                                        letterSpacing = 0.7.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        phoneCapabilities.take(2).forEach { capText ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = capText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        phoneCapabilities.drop(2).take(2).forEach { capText ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = capText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Restricted (Closed) Apps Section
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "RESTRICTED APPS (BLOCKED)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            letterSpacing = 0.7.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        blockedAppChips.take(3).forEach { chip ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = chip,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                // Allowed (Opened) Apps Section
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF0A6B43),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ALLOWED APPS (OPEN / ACCESSIBLE)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = Color(0xFF0A6B43),
                                            letterSpacing = 0.7.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        allowedAppChips.take(2).forEach { chip ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(GamifiedMint.copy(alpha = 0.22f))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = chip,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0A6B43)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Modern Full-Width Start Action Button
                                Button(
                                    onClick = { onStartFocusMode(selectedMode) },
                                    colors = appBlackButtonColors(),
                                    border = appBlackButtonBorder(),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "START ${selectedMode.displayName.uppercase()} (${selectedMode.defaultMinutes}M)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
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

@Composable
fun TimerSetupScreen(
    viewModel: FocusViewModel,
    onStartSession: () -> Unit,
    onNavigateToBlocklist: () -> Unit = {},
    onNavigateToReadingAppPicker: (() -> Unit)? = null,
    onNavigateToStats: () -> Unit = {},
    onNavigateToRooms: () -> Unit = {},
    onNavigateToBedtime: () -> Unit = {},
    autoStartFromShortcut: Boolean = false,
    onAutoStartConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val installedApps by viewModel.installedApps.collectAsState()
    val designatedReadingApp by viewModel.designatedReadingApp.collectAsState()
    val blockedAppsList by viewModel.blockedAppsList.collectAsState()
    val isWhitelistMode by viewModel.isWhitelistMode.collectAsState()
    val stats by viewModel.statsSummary.collectAsState()
    val sessionLogs by viewModel.sessionLogs.collectAsState()
    val selectedSoundId by viewModel.selectedAmbientSoundId.collectAsState()
    val isProTier by viewModel.isProTier.collectAsState()
    val accentThemeId by viewModel.accentThemeId.collectAsState()
    val showProLimitPrompt by viewModel.showProLimitPrompt.collectAsState()
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val parentalPinHash by viewModel.parentalPinHash.collectAsState()
    val serviceConnected by ReadLockAccessibilityService.isServiceConnected.collectAsState()

    val prefs = remember { context.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE) }
    var hasTimerBeenSet by remember { mutableStateOf(false) }
    var sessionMinutes by remember {
        mutableIntStateOf(prefs.getInt("saved_session_minutes", 15))
    }
    var showAppPicker by remember { mutableStateOf(false) }
    var pendingRedirectAppLabel by remember { mutableStateOf<String?>(null) }
    var showAccessibilityRequiredDialog by remember { mutableStateOf(false) }
    var showReadingAppRequiredDialog by remember { mutableStateOf(false) }
    var showBlockedAppsRequiredDialog by remember { mutableStateOf(false) }
    var showParentalPinForReader by remember { mutableStateOf(false) }

    var accessibilityReady by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context) || hasAccessibilityBeenEnabledOnce(context))
    }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledLauncherApps(context)
        if (isAccessibilityServiceEnabled(context) || hasAccessibilityBeenEnabledOnce(context)) {
            accessibilityReady = true
        }
    }

    LaunchedEffect(serviceConnected) {
        if (serviceConnected) {
            markAccessibilityEnabledOnce(context)
            accessibilityReady = true
            showAccessibilityRequiredDialog = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val enabledNow = isAccessibilityServiceEnabled(context) || hasAccessibilityBeenEnabledOnce(context)
                accessibilityReady = enabledNow
                if (enabledNow) {
                    showAccessibilityRequiredDialog = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasReadingApp = !designatedReadingApp.isNullOrBlank()
    val blockedAppsCount = remember(blockedAppsList) { blockedAppsList.count { it.isBlocked } }
    val hasBlockedAppsConfig = (isProTier && isWhitelistMode) || blockedAppsCount > 0
    val selectedReadingAppLabel = remember(designatedReadingApp, installedApps) {
        if (designatedReadingApp.isNullOrBlank()) {
            "None Selected"
        } else {
            installedApps.find { it.packageName == designatedReadingApp }?.label ?: designatedReadingApp!!
        }
    }

    fun updateSessionMinutes(newMins: Int) {
        val clamped = newMins.coerceIn(5, 120)
        sessionMinutes = clamped
        hasTimerBeenSet = true
        prefs.edit().putInt("saved_session_minutes", clamped).apply()
    }

    fun openReadingAppChooser() {
        if (onNavigateToReadingAppPicker != null) {
            onNavigateToReadingAppPicker()
        } else {
            showAppPicker = true
        }
    }

    var selectedFocusMode by remember { mutableStateOf<FocusSessionManager.FocusMode?>(null) }

    fun tryStartFocusSession(mode: FocusSessionManager.FocusMode? = selectedFocusMode, customDuration: Int? = null) {
        if (!accessibilityReady) {
            showAccessibilityRequiredDialog = true
            return
        }
        if (mode == null && !hasTimerBeenSet) {
            Toast.makeText(context, "Select a focus mode or set your timer duration to start", Toast.LENGTH_SHORT).show()
            return
        }
        val targetMode = mode ?: FocusSessionManager.FocusMode.CUSTOM
        val duration = customDuration ?: if (mode != null) mode.defaultMinutes else sessionMinutes
        viewModel.startModeSession(context, targetMode, duration)
        onStartSession()
    }

    LaunchedEffect(autoStartFromShortcut, hasReadingApp, hasBlockedAppsConfig) {
        if (autoStartFromShortcut) {
            onAutoStartConsumed()
            tryStartFocusSession()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 8 Professional Focus Modes Scrollable Top Tabs (First item, below FocusLock top bar, above Novice Reader)
            CinematicCardEntrance(index = 0) {
                FocusModeScrollableTabsSection(
                    selectedMode = selectedFocusMode,
                    onSelectMode = { mode ->
                        selectedFocusMode = mode
                        if (mode != null) {
                            sessionMinutes = mode.defaultMinutes
                            hasTimerBeenSet = true
                            prefs.edit().putInt("saved_session_minutes", mode.defaultMinutes).apply()
                        } else {
                            hasTimerBeenSet = false
                        }
                    },
                    onStartFocusMode = { mode ->
                        selectedFocusMode = mode
                        sessionMinutes = mode.defaultMinutes
                        hasTimerBeenSet = true
                        if (!accessibilityReady) {
                            showAccessibilityRequiredDialog = true
                        } else {
                            tryStartFocusSession(mode, mode.defaultMinutes)
                        }
                    }
                )
            }

            // 1. Duolingo-inspired XP Bar, Rank & Pulsing Streak Flame Card (Novice Reader, below Top Tabs)
            CinematicCardEntrance(index = 1) {
                TimerXpAndStreakCard(
                    stats = stats,
                    onOpenStats = onNavigateToStats
                )
            }

            // Bedtime Lock Quick Status Card
            CinematicCardEntrance(index = 2) {
                BedtimeDashboardCard(onNavigateToBedtime = onNavigateToBedtime)
            }

            // 2. Hero Duration Ring Stepper Card (Apple Fitness ring + Duolingo stepper pills)
            CinematicCardEntrance(index = 3) {
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
                            Text(
                                text = "FOCUS TIMER",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.9.sp
                            )
                            XpRewardChip(
                                text = when {
                                    selectedFocusMode != null -> "+${selectedFocusMode!!.defaultMinutes * 2 + 50} XP"
                                    hasTimerBeenSet -> "+${sessionMinutes * 2 + 50} XP"
                                    else -> "+XP on Start"
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Minus (-), Apple-Fitness-style Duration Ring, and Plus (+)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val canDecrement = hasTimerBeenSet && sessionMinutes > 5
                            IconButton(
                                onClick = {
                                    val current = if (hasTimerBeenSet) sessionMinutes else 25
                                    selectedFocusMode = null
                                    updateSessionMinutes(current - 5)
                                },
                                enabled = !hasTimerBeenSet || canDecrement,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (!hasTimerBeenSet || canDecrement) Color.Black else Color.Black.copy(alpha = 0.35f))
                                    .testTag("duration_decrement_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease session duration by 5 minutes",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            FitnessDurationRing(
                                sessionMinutes = if (hasTimerBeenSet) sessionMinutes else 0,
                                onMinutesChange = {
                                    selectedFocusMode = null
                                    updateSessionMinutes(it)
                                }
                            )

                            val canIncrement = !hasTimerBeenSet || sessionMinutes < 120
                            IconButton(
                                onClick = {
                                    val current = if (hasTimerBeenSet) sessionMinutes else 20
                                    selectedFocusMode = null
                                    updateSessionMinutes(current + 5)
                                },
                                enabled = canIncrement,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (canIncrement) Color.Black else Color.Black.copy(alpha = 0.35f))
                                    .testTag("duration_increment_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase session duration by 5 minutes",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = (if (hasTimerBeenSet) sessionMinutes else 25).toFloat(),
                            onValueChange = {
                                selectedFocusMode = null
                                updateSessionMinutes(it.toInt())
                            },
                            valueRange = 5f..120f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = GamifiedPurple,
                                activeTrackColor = GamifiedPurple,
                                inactiveTrackColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("duration_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Borderless quick duration chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 25, 45, 60).forEach { mins ->
                                val isSelected = hasTimerBeenSet && sessionMinutes == mins
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) GamifiedPurple else MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            selectedFocusMode = null
                                            updateSessionMinutes(mins)
                                        }
                                        .padding(vertical = 8.dp)
                                        .testTag("preset_${mins}_btn")
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Apple Fitness-style Daily Quests with Progress Rings
            CinematicCardEntrance(index = 2) {
                DailyQuestsRingsCard(
                    sessionLogs = sessionLogs,
                    stats = stats,
                    onOpenStats = onNavigateToStats
                )
            }

            // 4. Modern Gamified Reader & Shield Loadout Tiles
            CinematicCardEntrance(index = 3) {
                ReaderAndShieldLoadoutRow(
                    hasReadingApp = hasReadingApp,
                    designatedReadingApp = designatedReadingApp,
                    selectedReadingAppLabel = selectedReadingAppLabel,
                    hasBlockedAppsConfig = hasBlockedAppsConfig,
                    isProTier = isProTier,
                    isWhitelistMode = isWhitelistMode,
                    blockedAppsCount = blockedAppsCount,
                    onReaderClick = {
                        if (isParentalLockEnabled && parentalPinHash.isNotBlank()) {
                            showParentalPinForReader = true
                        } else {
                            openReadingAppChooser()
                        }
                    },
                    onShieldClick = onNavigateToBlocklist
                )
            }

            // 5. Compact Gamified Badges Strip
            CinematicCardEntrance(index = 4) {
                TimerBadgesStripCard(
                    stats = stats,
                    sessionLogs = sessionLogs,
                    onOpenStats = onNavigateToStats
                )
            }

            // 6. Medium Borderless Ambient Soundscape Card
            CinematicCardEntrance(index = 5) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(22.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ambient_sound_picker_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularIconBadge(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    containerSize = 24.dp,
                                    iconSize = 13.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SOUNDSCAPE",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = AmbientSoundType.fromId(selectedSoundId).displayName,
                                color = GamifiedPurple,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AmbientSoundType.entries.forEach { sound ->
                                val isSelected = selectedSoundId == sound.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) GamifiedPurple else MaterialTheme.colorScheme.surface)
                                        .clickable { viewModel.selectAmbientSound(sound, previewIfSessionActive = false) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("ambient_chip_${sound.id}")
                                ) {
                                    Text(
                                        text = buildString {
                                            append("${sound.emoji} ${sound.displayName}")
                                            if (sound.isProOnly && !isProTier) append(" ★PRO")
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Borderless Co-op Study Rooms Link Card
            CinematicCardEntrance(index = 6) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToRooms() }
                        .testTag("timer_to_rooms_link")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Co-op focus with friends?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "STUDY ROOMS →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = GamifiedPurple
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Start Button (Solid black button with white font)
        Button(
            onClick = { tryStartFocusSession() },
            colors = appBlackButtonColors(),
            border = appBlackButtonBorder(),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_session_button")
        ) {
            CircularIconBadge(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                containerSize = 28.dp,
                iconSize = 17.dp,
                containerColor = Color.Black,
                iconTint = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    selectedFocusMode != null -> "START ${selectedFocusMode!!.displayName.uppercase()} (${selectedFocusMode!!.defaultMinutes}M)"
                    hasTimerBeenSet -> "START FOCUS SESSION (${sessionMinutes}M)"
                    else -> "START FOCUS SESSION"
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black
            )
        }
    }

    // Modal fallback Reading App Picker dialog (if screen navigation callback isn't supplied)
    if (showAppPicker) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAppPicker = false }
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Select Reading App",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!hasReadingApp) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectReadingApp(null)
                                        showAppPicker = false
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "None (Clear selection)",
                                        color = if (!hasReadingApp) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "A reading app must be selected to start a session",
                                        color = if (!hasReadingApp) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        items(installedApps) { app ->
                            val isSelected = designatedReadingApp == app.packageName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectReadingApp(app.packageName)
                                        showAppPicker = false
                                        pendingRedirectAppLabel = app.label
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppLauncherIcon(
                                    packageName = app.packageName,
                                    contentDescription = "${app.label} icon",
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    CircularIconBadge(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        containerSize = 28.dp,
                                        iconSize = 16.dp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showAppPicker = false },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CANCEL",
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }

    if (showReadingAppRequiredDialog) {
        ReadingAppRequiredDialog(
            onSelectReadingApp = {
                showReadingAppRequiredDialog = false
                openReadingAppChooser()
            },
            onDismiss = { showReadingAppRequiredDialog = false }
        )
    }

    if (showBlockedAppsRequiredDialog) {
        BlockedAppsRequiredDialog(
            onSelectBlockedApps = {
                showBlockedAppsRequiredDialog = false
                onNavigateToBlocklist()
            },
            onDismiss = { showBlockedAppsRequiredDialog = false }
        )
    }

    if (showAccessibilityRequiredDialog) {
        AccessibilityRequiredDialog(
            onDismiss = { showAccessibilityRequiredDialog = false }
        )
    }

    if (showParentalPinForReader) {
        ParentalPinDialog(
            title = "Parental Control Active",
            subtitle = "Enter your parental PIN to change the designated reading app.",
            storedPinHash = parentalPinHash,
            onPinConfirmed = {
                showParentalPinForReader = false
                openReadingAppChooser()
            },
            onDismiss = { showParentalPinForReader = false }
        )
    }

    pendingRedirectAppLabel?.let { selectedLabel ->
        ReadingSelectedGoToBlockAppsDialog(
            selectedAppLabel = selectedLabel,
            onGoToBlocklist = {
                pendingRedirectAppLabel = null
                onNavigateToBlocklist()
            },
            onDismiss = {
                pendingRedirectAppLabel = null
            }
        )
    }

    if (showProLimitPrompt) {
        PremiumUpgradeDialog(
            isProActive = isProTier,
            selectedThemeId = accentThemeId,
            onTogglePro = { viewModel.setProTier(it) },
            onSelectTheme = { viewModel.selectAccentTheme(it) },
            onDismiss = { viewModel.dismissProLimitPrompt() }
        )
    }
}

/**
 * Dedicated child destination for picking the Designated Reading App with Launcher Icon + App Label.
 */
@Composable
fun ReadingAppSelectionScreen(
    viewModel: FocusViewModel,
    onBack: () -> Unit,
    onReadingAppSelected: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()
    val designatedReadingApp by viewModel.designatedReadingApp.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var pendingRedirectAppLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledLauncherApps(context)
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        CinematicCardEntrance(index = 0) {
            Column {
                Text(
                    text = "Designated Reading App",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Pick the reading or study app that remains open and allowed during your focus session.",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CinematicCardEntrance(index = 1) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    CircularIconBadge(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        containerSize = 32.dp,
                        iconSize = 18.dp
                    )
                },
                placeholder = { Text("Search reading apps...", color = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reading_app_search_input")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                val noneSelected = designatedReadingApp.isNullOrBlank()
                CinematicCardEntrance(index = 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (noneSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.selectReadingApp(null)
                                onBack()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CircularIconBadge(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                containerSize = 40.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "None (Clear selection)",
                                    color = if (noneSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Choose an app below to start focus sessions",
                                    color = if (noneSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (noneSelected) {
                            CircularIconBadge(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                containerSize = 28.dp,
                                iconSize = 16.dp
                            )
                        }
                    }
                }
            }

            itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                val isSelected = designatedReadingApp == app.packageName
                CinematicCardEntrance(index = (index + 3).coerceAtMost(6)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                viewModel.selectReadingApp(app.packageName)
                                pendingRedirectAppLabel = app.label
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("reading_app_option_${app.packageName}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AppLauncherIcon(
                            packageName = app.packageName,
                            contentDescription = "${app.label} launcher icon",
                            size = 42.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isSelected) {
                            CircularIconBadge(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected reading app",
                                containerSize = 30.dp,
                                iconSize = 18.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBack,
            colors = appBlackButtonColors(),
            border = appBlackButtonBorder(),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reading_app_picker_back_btn")
        ) {
            CircularIconBadge(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                containerSize = 24.dp,
                iconSize = 14.dp,
                containerColor = Color.Black,
                iconTint = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("BACK TO TIMER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }

    pendingRedirectAppLabel?.let { selectedLabel ->
        ReadingSelectedGoToBlockAppsDialog(
            selectedAppLabel = selectedLabel,
            onGoToBlocklist = {
                pendingRedirectAppLabel = null
                onReadingAppSelected()
            },
            onDismiss = {
                pendingRedirectAppLabel = null
            }
        )
    }
}

@Composable
fun FocusSessionScreen(
    viewModel: FocusViewModel,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
    val blockedAttempts by viewModel.blockedAttemptsCount.collectAsState()
    val stats by viewModel.statsSummary.collectAsState()
    val isDndActive by viewModel.isDndActivatedBySession.collectAsState()
    val selectedSoundId by viewModel.selectedAmbientSoundId.collectAsState()
    val isAudioPlaying by AmbientSoundPlayer.isPlaying.collectAsState()
    val audioVolume by AmbientSoundPlayer.volume.collectAsState()
    val isProTier by viewModel.isProTier.collectAsState()
    val isParentalLockEnabled by viewModel.isParentalLockEnabled.collectAsState()
    val parentalPinHash by viewModel.parentalPinHash.collectAsState()

    var showParentalQuitDialog by remember { mutableStateOf(false) }

    val totalDuration = FocusSessionManager.totalDurationSeconds
    val progress = if (totalDuration > 0) {
        timeLeftSeconds.toFloat() / totalDuration
    } else {
        1f
    }

    val formattedTime = remember(timeLeftSeconds) {
        val mins = timeLeftSeconds / 60
        val secs = timeLeftSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val isDark = LocalDarkTheme.current
        val streakBg = if (isDark) Color(0xFF4A2532) else Color(0xFFFFD8E4)
        val streakBorder = if (isDark) Color(0xFFFFB1C8) else Color(0xFFD78391)
        val streakText = if (isDark) Color(0xFFFFD8E4) else Color(0xFF31111D)

        // Upper stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Streak counter badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(streakBg)
                    .border(1.dp, streakBorder, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    containerSize = 22.dp,
                    iconSize = 13.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stats.currentStreak} DAY STREAK 🔥",
                    color = streakText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black
                )
            }

            // Live Distraction Attempts Counter badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("live_blocked_attempts_badge")
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    containerSize = 22.dp,
                    iconSize = 13.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ATTEMPTS BLOCKED: $blockedAttempts",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black
                )
            }
        }

        val context = LocalContext.current
        val currentMode = remember { FocusSessionManager.getCurrentFocusMode() }
        val isPaused by viewModel.isSessionPaused.collectAsState()
        val pomodoroCycles by viewModel.pomodoroCycleCount.collectAsState()
        var showStrictExitModal by remember { mutableStateOf(false) }

        if (isDndActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.DoNotDisturbOn,
                    contentDescription = "DND Active",
                    containerSize = 22.dp,
                    iconSize = 13.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Do Not Disturb Engaged",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Mode Identity Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(GamifiedPurple.copy(alpha = 0.16f))
                .border(1.dp, GamifiedPurple.copy(alpha = 0.7f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (currentMode) {
                        FocusSessionManager.FocusMode.DEEP_WORK -> Icons.Default.Psychology
                        FocusSessionManager.FocusMode.STUDY -> Icons.Default.School
                        FocusSessionManager.FocusMode.POMODORO -> Icons.Default.Timer
                        FocusSessionManager.FocusMode.READING -> Icons.Default.Book
                        FocusSessionManager.FocusMode.QUICK_FOCUS -> Icons.Default.Bolt
                        FocusSessionManager.FocusMode.STRICT_FOCUS -> Icons.Default.Lock
                        FocusSessionManager.FocusMode.RELAX_RECHARGE -> Icons.Default.SelfImprovement
                        FocusSessionManager.FocusMode.CUSTOM -> Icons.Default.Tune
                    },
                    contentDescription = null,
                    tint = GamifiedPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${currentMode.displayName.uppercase()} • ${currentMode.intensity.uppercase()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = GamifiedPurple
                )
            }
        }

        if (currentMode == FocusSessionManager.FocusMode.POMODORO) {
            Text(
                text = "Pomodoro Cycle $pomodoroCycles of 4 (Long break after 4th cycle)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GamifiedMint
            )
        }

        if (currentMode == FocusSessionManager.FocusMode.RELAX_RECHARGE) {
            val breathAnim = rememberInfiniteTransition(label = "breath_session")
            val scale by breathAnim.animateFloat(
                initialValue = 0.82f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breath_scale"
            )
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(GamifiedMint.copy(alpha = 0.22f))
                    .border(2.dp, GamifiedMint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (scale > 1.05f) "Inhale" else "Exhale",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Central Real-Time Rotating Countdown Arc
        val potentialSessionXp = ((totalDuration / 60).coerceAtLeast(1) * 2 + 50).toInt()
        RealTimeFocusCountdownRing(
            timeLeftSeconds = timeLeftSeconds.toInt(),
            totalDurationSeconds = totalDuration.toInt(),
            formattedTime = formattedTime,
            potentialSessionXp = potentialSessionXp
        )

        // Virtual Focus Tree growing live with session progress
        FocusForestRewardCard(
            stats = stats,
            liveSessionProgress = (1f - progress).coerceIn(0.08f, 1f)
        )

        // Live Ambient & White Noise Sound Controller
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("session_ambient_sound_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularIconBadge(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ambient Sound",
                            containerSize = 30.dp,
                            iconSize = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAudioPlaying) "SOUND: ${AmbientSoundType.fromId(selectedSoundId).displayName}" else "AMBIENT SOUND: OFF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = {
                            if (isAudioPlaying) {
                                AmbientSoundPlayer.stop()
                            } else {
                                val target = AmbientSoundType.fromId(selectedSoundId).takeIf { it != AmbientSoundType.NONE }
                                    ?: AmbientSoundType.WHITE_NOISE
                                viewModel.selectAmbientSound(target, previewIfSessionActive = true)
                            }
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(if (isAudioPlaying) "MUTE" else "PLAY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmbientSoundType.entries.forEach { sound ->
                        val active = selectedSoundId == sound.id && (sound == AmbientSoundType.NONE || isAudioPlaying)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                                .clickable { viewModel.selectAmbientSound(sound, previewIfSessionActive = true) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${sound.emoji} ${sound.displayName}${if (sound.isProOnly && !isProTier) " ★" else ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (isAudioPlaying) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = audioVolume,
                        onValueChange = { AmbientSoundPlayer.setVolume(it) },
                        valueRange = 0.05f..1f
                    )
                }
            }
        }

        // Strict Lock Information Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Icon",
                    containerSize = 36.dp,
                    iconSize = 18.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "STRICT LOCK ENGAGED",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Shielded apps remain locked until 00:00.",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (isPaused) viewModel.resumeFocusSession(context)
                    else viewModel.pauseFocusSession(context)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) GamifiedMint else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (isPaused) Color(0xFF0A6B43) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = if (isPaused) Color.Black else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPaused) "RESUME" else "PAUSE",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = if (isPaused) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    if (isParentalLockEnabled && parentalPinHash.isNotBlank()) {
                        showParentalQuitDialog = true
                    } else if (currentMode == FocusSessionManager.FocusMode.STRICT_FOCUS) {
                        showStrictExitModal = true
                    } else {
                        onStopSession()
                    }
                },
                colors = appBlackButtonColors(),
                border = appBlackButtonBorder(),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("end_session_early_button")
            ) {
                CircularIconBadge(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "End Session Early",
                    containerSize = 26.dp,
                    iconSize = 15.dp,
                    containerColor = Color.Black,
                    iconTint = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isParentalLockEnabled) "END (PIN)" else "END EARLY",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        if (showStrictExitModal) {
            AlertDialog(
                onDismissRequest = { showStrictExitModal = false },
                title = { Text("Strict Focus Protection Active", fontWeight = FontWeight.Bold) },
                text = { Text("Strict Focus mode is designed to prevent distractions and accidental cancellation. Are you sure you want to abort your focus block?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showStrictExitModal = false
                            viewModel.stopFocusSession(context, false)
                            onStopSession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Yes, End Session", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showStrictExitModal = false }) {
                        Text("Stay Focused")
                    }
                }
            )
        }
    }

    if (showParentalQuitDialog) {
        ParentalPinDialog(
            title = "Parental Lock Active",
            subtitle = "Enter your parental PIN to end this focus session early.",
            storedPinHash = parentalPinHash,
            onPinConfirmed = {
                showParentalQuitDialog = false
                onStopSession()
            },
            onDismiss = { showParentalQuitDialog = false }
        )
    }
}

@Composable
fun CoffeeBreakScreen(
    viewModel: FocusViewModel,
    onSkipBreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()

    val totalDuration = FocusSessionManager.totalDurationSeconds
    val progress = if (totalDuration > 0) {
        timeLeftSeconds.toFloat() / totalDuration
    } else {
        1f
    }

    val formattedTime = remember(timeLeftSeconds) {
        val mins = timeLeftSeconds / 60
        val secs = timeLeftSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    val isDark = LocalDarkTheme.current
    val coffeeBadgeBg = if (isDark) Color(0xFF2B211D) else Color(0xFFEFEBE9)
    val coffeeBorderColor = if (isDark) Color(0xFFBCAAA4) else Color(0xFF8D6E63)
    val coffeeIconTint = if (isDark) Color(0xFFD7CCC8) else Color(0xFF5D4037)
    val coffeeTrackColor = if (isDark) Color(0xFF322824) else Color(0xFFF5EBE6)
    val coffeeActiveColor = if (isDark) Color(0xFFD7CCC8) else Color(0xFF8D6E63)
    val coffeeTitleText = if (isDark) Color(0xFFEFEBE9) else Color(0xFF3E2723)
    val coffeeBodyText = if (isDark) Color(0xFFBCAAA4) else Color(0xFF5D4037)
    val coffeeCardBg = if (isDark) Color(0xFF231B18) else Color(0xFFEFEBE9).copy(alpha = 0.5f)
    val coffeeCardBorder = if (isDark) Color(0xFF4E342E) else Color(0xFFD7CCC8)
    val coffeeButtonBg = if (isDark) Color(0xFFD7CCC8) else Color(0xFF3E2723)
    val coffeeButtonContent = if (isDark) Color(0xFF2B1B17) else Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status / spacing
        Spacer(modifier = Modifier.height(16.dp))

        // Center visual + timer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Warm Coffee Icon Container with soft pulsing brown accent
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(2.dp, coffeeBorderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = "Coffee Cup",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Central Countdown Arc (warm brown coffee progress)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(260.dp)
                    .padding(16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = coffeeTrackColor,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = coffeeActiveColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "COFFEE BREAK",
                        color = coffeeBorderColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        color = coffeeTitleText,
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                        modifier = Modifier.testTag("break_timer_text")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    XpRewardChip(text = "Session XP Banked")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Friendly Message Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = coffeeCardBg
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, coffeeCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Excellent Work! 🎉",
                        style = MaterialTheme.typography.titleMedium,
                        color = coffeeTitleText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You've successfully finished your focus session. Now take a deep breath, stand up, stretch, and rest your eyes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = coffeeBodyText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Action Button to Skip or return early
        Button(
            onClick = onSkipBreak,
            colors = appBlackButtonColors(),
            border = appBlackButtonBorder(),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("skip_break_button")
        ) {
            Text(
                text = "SKIP BREAK & START FRESH",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

data class Achievement(
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progressText: String
)

@Composable
fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val isDark = LocalDarkTheme.current
    val mintBg = if (isDark) Color(0xFF143326) else Color(0xFFE6FBF1)
    val mintText = if (isDark) GamifiedMint else Color(0xFF0A6B43)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag("achievement_card_${achievement.title.lowercase().replace(" ", "_").replace("-", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (achievement.isUnlocked) mintBg else MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = achievement.icon,
                    fontSize = 21.sp,
                    modifier = Modifier.alpha(if (achievement.isUnlocked) 1f else 0.4f)
                )
                if (!achievement.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = achievement.description,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (achievement.isUnlocked) mintBg else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = achievement.progressText,
                        color = if (achievement.isUnlocked) mintText else MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryStatsScreen(
    viewModel: FocusViewModel,
    onNavigateToTimer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.statsSummary.collectAsState()
    val sessionLogs by viewModel.sessionLogs.collectAsState()
    val todoItems by viewModel.todoItems.collectAsState()

    val dateHelper = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    val achievements = remember(stats, sessionLogs) {
        listOf(
            Achievement(
                title = "Kickstarter",
                description = "Complete at least 1 focus session.",
                icon = "🎯",
                isUnlocked = stats.completedSessionsCount >= 1,
                progressText = "${stats.completedSessionsCount.coerceAtMost(1)}/1 Session"
            ),
            Achievement(
                title = "3-Day Streak",
                description = "Build a consistent habit of 3 days.",
                icon = "⚡",
                isUnlocked = stats.currentStreak >= 3,
                progressText = "${stats.currentStreak.coerceAtMost(3)}/3 Days"
            ),
            Achievement(
                title = "5-Day Streak",
                description = "Unstoppable! Maintain a 5-day streak.",
                icon = "🔥",
                isUnlocked = stats.currentStreak >= 5,
                progressText = "${stats.currentStreak.coerceAtMost(5)}/5 Days"
            ),
            Achievement(
                title = "Focused Mind",
                description = "Focus for a total of 100 minutes.",
                icon = "🧠",
                isUnlocked = stats.totalFocusMinutes >= 100,
                progressText = "${stats.totalFocusMinutes.coerceAtMost(100)}/100 Min"
            ),
            Achievement(
                title = "Deep Focus Master",
                description = "Focus for a total of 1000 minutes.",
                icon = "👑",
                isUnlocked = stats.totalFocusMinutes >= 1000,
                progressText = "${stats.totalFocusMinutes.coerceAtMost(1000)}/1000 Min"
            ),
            Achievement(
                title = "Laser Precision",
                description = "Complete a session with 0 block attempts.",
                icon = "🛡️",
                isUnlocked = stats.completedSessionsCount > 0 && sessionLogs.any { it.isCompleted && it.blockedAttempts == 0 },
                progressText = if (stats.completedSessionsCount > 0 && sessionLogs.any { it.isCompleted && it.blockedAttempts == 0 }) "1/1 Done" else "0/1 Session"
            )
        )
    }

    // Calculate weekly stats (past 7 days chronologically)
    val localTimeZone = remember { TimeZone.getDefault() }
    val dayLabelsAndMinutes = remember(sessionLogs) {
        val past7Days = (0..6).map { daysAgo ->
            val cal = Calendar.getInstance(localTimeZone)
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal
        }.reversed()

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        past7Days.map { cal ->
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
            val dayLogs = sessionLogs.filter { log ->
                log.timestamp in startOfDay..endOfDay && log.isCompleted
            }
            val totalSeconds = dayLogs.fold(0L) { acc, log -> acc + log.durationSeconds }
            val totalMinutes = totalSeconds / 60f
            Pair(dayFormat.format(cal.time), totalMinutes)
        }
    }

    val totalWeeklyMinutes = remember(dayLabelsAndMinutes) {
        var sum = 0f
        for (pair in dayLabelsAndMinutes) {
            sum += pair.second
        }
        sum
    }

    var weeklyGoalMinutes by remember { mutableFloatStateOf(150f) }
    var isWeeklyProgressExpanded by remember { mutableStateOf(false) }
    var isFocusMetricExpanded by remember { mutableStateOf(false) }
    val goalProgress = (totalWeeklyMinutes / weeklyGoalMinutes).coerceIn(0f, 1f)

    val animatedGoalProgress by animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "AnimatedGoalProgress"
    )

    // Streak flame pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "StreakPulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameScale"
    )
    val flameAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameAlpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header + Gamified Level HUD
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Progress & Quests",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "XP rank, growing forest, weekly insights & badges.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 1.25 Gamified Level, Streak & XP HUD Card (matching Timer screen)
        item {
            CinematicCardEntrance(index = 0) {
                TimerXpAndStreakCard(
                    stats = stats,
                    onOpenStats = {}
                )
            }
        }

        // 1.4 Daily Quests Activity Rings Card (Apple Fitness x Duolingo)
        item {
            CinematicCardEntrance(index = 1) {
                DailyQuestsRingsCard(
                    sessionLogs = sessionLogs,
                    stats = stats,
                    onOpenStats = {}
                )
            }
        }

        // 1.5 Virtual Focus Tree Reward Card
        item {
            CinematicCardEntrance(index = 1) {
                FocusForestRewardCard(stats = stats)
            }
        }

        // 2. Weekly Reading Progress Dashboard Card (Borderless + Soft Shadow)
        item {
            CinematicCardEntrance(index = 2) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(22.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        .testTag("weekly_progress_dashboard_card")
                ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                role = Role.Button,
                                onClick = { isWeeklyProgressExpanded = !isWeeklyProgressExpanded }
                            )
                            .padding(vertical = 2.dp)
                            .testTag("weekly_progress_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WEEKLY READING PROGRESS",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Last 7 Days Activity",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Goal: ${weeklyGoalMinutes.toInt()}m",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Crossfade(
                                targetState = isWeeklyProgressExpanded,
                                animationSpec = tween(durationMillis = 200),
                                label = "weekly_progress_chevron"
                            ) { expanded ->
                                CircularIconBadge(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (expanded) "Collapse weekly reading progress" else "Expand weekly reading progress",
                                    containerSize = 32.dp,
                                    iconSize = 20.dp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isWeeklyProgressExpanded,
                        enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                        exit = shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(24.dp))

                    val maxMinutes = remember(dayLabelsAndMinutes) {
                        dayLabelsAndMinutes.maxOf { it.second }.coerceAtLeast(30f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayLabelsAndMinutes.forEachIndexed { index, pair ->
                            val (day, minutes) = pair
                            val isToday = index == 6
                            val ratio = (minutes / maxMinutes).coerceIn(0f, 1f)
                            
                            val animatedRatio by animateFloatAsState(
                                targetValue = ratio,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "BarRatio_${index}"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (minutes > 0f) {
                                    Text(
                                        text = "${minutes.toInt()}m",
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(if (minutes > 0) (animatedRatio * 80).dp.coerceAtLeast(12.dp) else 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (minutes > 0) {
                                                if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                            }
                                        )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = day,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Focus Goal Progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${totalWeeklyMinutes.toInt()} / ${weeklyGoalMinutes.toInt()} min (${(goalProgress * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (goalProgress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animatedGoalProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customize Weekly Goal Target",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${weeklyGoalMinutes.toInt()} min",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = weeklyGoalMinutes,
                        onValueChange = { weeklyGoalMinutes = it },
                        valueRange = 30f..300f,
                        steps = 8, // 30, 60, 90, 120, 150, 180, 210, 240, 270, 300
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                        }
                    }
                }
            }
            }
        }

        // 3. Side-by-side Medium Borderless Quick Metrics (Streak & Total Focus)
        item {
            val isDark = LocalDarkTheme.current
            val flameBg = if (isDark) Color(0xFF3A2D12) else Color(0xFFFFF5DB)
            val flameText = if (isDark) GamifiedYellow else Color(0xFF8A5A00)
            val mintBg = if (isDark) Color(0xFF143326) else Color(0xFFE6FBF1)
            val mintText = if (isDark) GamifiedMint else Color(0xFF0A6B43)
            val purpleTintBg = if (isDark) Color(0xFF28204B) else Color(0xFFEEEAFF)

            CinematicCardEntrance(index = 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 100.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(flameBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "STREAK",
                                    color = flameText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🔥",
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .scale(flameScale)
                                        .alpha(flameAlpha)
                                )
                                Text(
                                    text = "${stats.currentStreak} Days",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("stats_streak_days_text")
                                )
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 100.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(mintBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "FOCUS TIME",
                                    color = mintText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⏱️ ${stats.totalFocusMinutes} Min",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Distraction Attempts Blocked Metric Card (Borderless + Soft Shadow)
            CinematicCardEntrance(index = 3) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_distraction_attempts_card")
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
                                text = "SHIELD INTERCEPTS",
                                color = GamifiedPurple,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Distractions blocked across your focus sessions.",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(purpleTintBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "🛡️ ${stats.totalBlockedAttempts}",
                                color = GamifiedPurple,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        // 4. Clean gamified score & export card (Borderless + Soft Shadow)
        item {
            CinematicCardEntrance(index = 4) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(22.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        .testTag("focus_metric_score_card")
                ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                role = Role.Button,
                                onClick = { isFocusMetricExpanded = !isFocusMetricExpanded }
                            )
                            .padding(vertical = 2.dp)
                            .testTag("focus_metric_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FOCUS METRIC SCORE",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                            AnimatedVisibility(visible = !isFocusMetricExpanded) {
                                Text(
                                    text = "${stats.focusScore} Points",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                CircularIconBadge(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    containerSize = 20.dp,
                                    iconSize = 12.dp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "LEVEL ${(stats.focusScore / 200) + 1}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Crossfade(
                                targetState = isFocusMetricExpanded,
                                animationSpec = tween(durationMillis = 200),
                                label = "focus_metric_chevron"
                            ) { expanded ->
                                CircularIconBadge(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (expanded) "Collapse focus metric score" else "Expand focus metric score",
                                    containerSize = 32.dp,
                                    iconSize = 20.dp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isFocusMetricExpanded,
                        enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                        exit = shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${stats.focusScore} Points",
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("gamified_focus_score_text")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "I am locking in on my focus with FocusLock! 📚🔒 My current stats:\n" +
                                        "🏆 Level: ${(stats.focusScore / 200) + 1}\n" +
                                        "🔥 Focus Score: ${stats.focusScore} Points\n" +
                                        "⚡ Active Streak: ${stats.currentStreak} Days\n" +
                                        "⏱️ Total Focus: ${stats.totalFocusMinutes} Minutes\n" +
                                        "🎯 Sessions Completed: ${stats.completedSessionsCount}\n\n" +
                                        "Join me in building deep reading and focus habits!"
                                    )
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share focus stats via")
                                context.startActivity(shareIntent)
                            },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_stats_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularIconBadge(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Progress",
                                    containerSize = 26.dp,
                                    iconSize = 14.dp,
                                    containerColor = Color.Black,
                                    iconTint = Color.White,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SHARE",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val statsText = "I am locking in on my focus with FocusLock! 📚🔒 My current stats:\n" +
                                        "🏆 Level: ${(stats.focusScore / 200) + 1}\n" +
                                        "🔥 Focus Score: ${stats.focusScore} Points\n" +
                                        "⚡ Active Streak: ${stats.currentStreak} Days\n" +
                                        "⏱️ Total Focus: ${stats.totalFocusMinutes} Minutes\n" +
                                        "🎯 Sessions Completed: ${stats.completedSessionsCount}\n\n" +
                                        "Join me in building deep reading and focus habits!"
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("FocusLock Stats", statsText)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Stats copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = appBlackButtonColors(),
                            border = appBlackButtonBorder(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("copy_stats_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularIconBadge(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy to Clipboard",
                                    containerSize = 26.dp,
                                    iconSize = 14.dp,
                                    containerColor = Color.Black,
                                    iconTint = Color.White,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "COPY STATS",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            exportStatsToPdf(context, stats, sessionLogs, dateHelper)
                        },
                        colors = appBlackButtonColors(),
                        border = appBlackButtonBorder(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularIconBadge(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Export to PDF",
                                containerSize = 26.dp,
                                iconSize = 14.dp,
                                containerColor = Color.Black,
                                iconTint = Color.White,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EXPORT TO PDF REPORT",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                        }
                    }
                }
                }
            }
        }

        // --- 4.1. ACHIEVEMENTS SECTION ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Focus Badges & Milestones",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                val unlockedCount = achievements.count { it.isUnlocked }
                Text(
                    text = "$unlockedCount / ${achievements.size} Unlocked",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Track your focus progress and unlock special achievement badges.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // We can render achievements in chunks of 2 to display them in rows (2-column layout)
        val chunkedAchievements = achievements.chunked(2)
        items(chunkedAchievements) { rowAchievements ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowAchievements.forEach { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        modifier = Modifier.weight(1f)
                    )
                }
                // If there's only 1 item in the row, add an empty spacer weight to align perfectly
                if (rowAchievements.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // --- 4.2. TO-DO LIST (READING QUEUE) SECTION ---
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "📝 Reading Queue & Tasks",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage books, chapters, or articles you want to complete next.",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("todo_subtitle_text"),
                fontSize = 12.sp
            )
        }

        // Input Row for To-Do List
        item {
            var newTodoText by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTodoText,
                    onValueChange = { newTodoText = it },
                    placeholder = { Text("E.g., Read Chapter 3 of Clean Code", fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("todo_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (newTodoText.isNotBlank()) {
                            viewModel.addTodoItem(newTodoText.trim())
                            newTodoText = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = appBlackButtonColors(),
                    border = appBlackButtonBorder(),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("todo_add_button")
                ) {
                    CircularIconBadge(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        containerSize = 26.dp,
                        iconSize = 16.dp,
                        containerColor = Color.Black,
                        iconTint = Color.White,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADD", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }

        // Progress bar for To-Do List
        if (todoItems.isNotEmpty()) {
            item {
                val totalTodoCount = todoItems.size
                val completedTodoCount = todoItems.count { it.isCompleted }
                val todoProgress = if (totalTodoCount > 0) completedTodoCount.toFloat() / totalTodoCount else 0f
                val animatedTodoProgress by animateFloatAsState(
                    targetValue = todoProgress,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "TodoProgress"
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Queue Completion Progress",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$completedTodoCount / $totalTodoCount tasks (${(todoProgress * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GamifiedPurple
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedTodoProgress },
                            color = GamifiedPurple,
                            trackColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }
        }

        // To-Do items listing or empty state
        if (todoItems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(22.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("todo_empty_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📚", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Your reading queue is empty!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add some tasks above to plan your next deep focus sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        } else {
            items(todoItems) { todo ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .testTag("todo_item_${todo.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = todo.isCompleted,
                                onCheckedChange = { viewModel.toggleTodoItem(todo) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GamifiedMint,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.testTag("todo_checkbox_${todo.id}")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = todo.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (todo.isCompleted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                                modifier = Modifier.testTag("todo_title_${todo.id}")
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteTodoItem(todo.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("todo_delete_button_${todo.id}")
                        ) {
                            CircularIconBadge(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Task",
                                containerSize = 32.dp,
                                iconSize = 17.dp
                            )
                        }
                    }
                }
            }
        }

        // 5. Section Header for Logs
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Session Log History",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }

        // 6. Logs list or Empty card
        if (sessionLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(22.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        CircularIconBadge(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            containerSize = 64.dp,
                            iconSize = 32.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No focus logs found yet",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your completed reading sessions will show up here.",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        if (onNavigateToTimer != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToTimer,
                                colors = appBlackButtonColors(),
                                border = appBlackButtonBorder(),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("stats_start_first_session_btn")
                            ) {
                                Text("START A FOCUS SESSION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        } else {
            items(sessionLogs) { log ->
                val sessionXp = if (log.isCompleted) ((log.durationSeconds / 60).coerceAtLeast(1) * 2 + 50) else 0L
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val formattedDate = remember(log.timestamp) { dateHelper.format(Date(log.timestamp)) }
                        Text(
                            text = formattedDate,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${log.durationSeconds / 60}m ${log.durationSeconds % 60}s Focus",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.outlineVariant,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Blocks: ${log.blockedAttempts}",
                                color = if (log.blockedAttempts > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (sessionXp > 0L) {
                            XpRewardChip(text = "+$sessionXp XP")
                        }
                        val statusIcon = if (log.isCompleted) Icons.Default.CheckCircle else Icons.Default.Error
                        val statusColor = if (log.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        CircularIconBadge(
                            imageVector = statusIcon,
                            contentDescription = null,
                            containerSize = 34.dp,
                            iconSize = 18.dp,
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                        )
                    }
                }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun exportStatsToPdf(
    context: Context,
    stats: FocusStatsSummary,
    sessionLogs: List<SessionLogEntity>,
    dateHelper: SimpleDateFormat
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Background color
        paint.color = AndroidColor.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header banner (dark premium background)
        paint.color = AndroidColor.parseColor("#1E1B4B") // Midnight dark indigo
        canvas.drawRect(0f, 0f, 595f, 130f, paint)

        // Header text
        paint.color = AndroidColor.WHITE
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("FOCUSLOCK PROGRESS REPORT", 40f, 65f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = AndroidColor.parseColor("#99F6E4") // Soft teal accent
        val currentDate = SimpleDateFormat("MMMM d, yyyy - h:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $currentDate", 40f, 95f, paint)

        // Focus statistics title
        paint.color = AndroidColor.parseColor("#4F46E5") // Indigo blue accent line
        canvas.drawRect(40f, 160f, 555f, 164f, paint)

        paint.color = AndroidColor.BLACK
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Overall Focus Metrics", 40f, 195f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        paint.color = AndroidColor.parseColor("#1F2937") // Dark gray body text

        var yPos = 225f
        canvas.drawText("• Current Focus Level: Level ${(stats.focusScore / 200) + 1}", 55f, yPos, paint); yPos += 24f
        canvas.drawText("• Focus Score Points: ${stats.focusScore} pts", 55f, yPos, paint); yPos += 24f
        canvas.drawText("• Active Focus Streak: ${stats.currentStreak} Days", 55f, yPos, paint); yPos += 24f
        canvas.drawText("• Total Focus Duration: ${stats.totalFocusMinutes} Minutes", 55f, yPos, paint); yPos += 24f
        canvas.drawText("• Completed Focus Sessions: ${stats.completedSessionsCount} Sessions", 55f, yPos, paint); yPos += 24f

        // Session history section
        yPos += 20f
        paint.color = AndroidColor.parseColor("#0D9488") // Teal accent line
        canvas.drawRect(40f, yPos, 555f, yPos + 4f, paint)
        yPos += 35f

        paint.color = AndroidColor.BLACK
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Recent Reading Session Logs", 40f, yPos, paint)
        yPos += 30f

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = AndroidColor.parseColor("#1F2937")

        if (sessionLogs.isEmpty()) {
            canvas.drawText("No completed reading sessions recorded yet.", 55f, yPos, paint)
        } else {
            // Table Header
            paint.isFakeBoldText = true
            paint.color = AndroidColor.parseColor("#312E81") // Indigo table header
            canvas.drawText("Date & Time", 55f, yPos, paint)
            canvas.drawText("Duration", 260f, yPos, paint)
            canvas.drawText("Blocked Apps", 380f, yPos, paint)
            canvas.drawText("Status", 480f, yPos, paint)
            yPos += 20f

            // Table Border
            paint.strokeWidth = 1f
            paint.color = AndroidColor.parseColor("#E5E7EB")
            canvas.drawLine(40f, yPos, 555f, yPos, paint)
            yPos += 15f

            paint.isFakeBoldText = false
            paint.color = AndroidColor.parseColor("#374151")

            // Take up to 18 recent logs to fit neatly on standard A4 page
            val recentLogs = sessionLogs.take(18)
            for (log in recentLogs) {
                if (yPos > 790f) {
                    break
                }
                val formattedDate = dateHelper.format(Date(log.timestamp))
                val durationText = "${log.durationSeconds / 60}m ${log.durationSeconds % 60}s"
                val statusText = if (log.isCompleted) "Completed" else "Interrupted"

                canvas.drawText(formattedDate, 55f, yPos, paint)
                canvas.drawText(durationText, 260f, yPos, paint)
                canvas.drawText(log.blockedAttempts.toString(), 380f, yPos, paint)
                canvas.drawText(statusText, 480f, yPos, paint)

                yPos += 22f
            }
        }

        // Footer message
        paint.textSize = 10f
        paint.color = AndroidColor.parseColor("#9CA3AF")
        paint.isFakeBoldText = false
        canvas.drawText("FocusLock - Deep Reading & Distraction Blocking Assistant", 40f, 815f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        val file = File(context.cacheDir, "FocusLock_Progress_Report.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        // Share/View via FileProvider
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FocusLock Reading Progress Report")
            putExtra(Intent.EXTRA_TEXT, "Check out my FocusLock reading stats & focus streak progress report!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(intent, "Export / Share Progress PDF")
        context.startActivity(chooserIntent)

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to export PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SettingsScreen(
    viewModel: FocusViewModel,
    onOpenAdminDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isScheduleEnabled by viewModel.isScheduleEnabled.collectAsState()
    val startHour by viewModel.scheduleStartHour.collectAsState()
    val endHour by viewModel.scheduleEndHour.collectAsState()
    val stats by viewModel.statsSummary.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val serviceConnected by ReadLockAccessibilityService.isServiceConnected.collectAsState()

    val isProTierEnabled by viewModel.isProTierEnabled.collectAsState()
    val isCloudSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val isDndEnabled by viewModel.isDndEnabled.collectAsState()
    val partnerEmail by viewModel.partnerEmail.collectAsState()
    val isPartnerNotificationsEnabled by viewModel.isPartnerNotificationsEnabled.collectAsState()
    val parentalPin by viewModel.parentalPin.collectAsState()
    val reminderInterval by viewModel.reminderInterval.collectAsState()
    val isPrivateTrackingEnabled by viewModel.isPrivateTrackingEnabled.collectAsState()

    var showPartnerDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    var accessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context) || hasAccessibilityBeenEnabledOnce(context))
    }

    LaunchedEffect(Unit) {
        accessibilityEnabled = isAccessibilityServiceEnabled(context) || hasAccessibilityBeenEnabledOnce(context)
    }

    LaunchedEffect(serviceConnected) {
        if (serviceConnected) {
            markAccessibilityEnabledOnce(context)
            accessibilityEnabled = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = isAccessibilityServiceEnabled(context) || hasAccessibilityBeenEnabledOnce(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentLevel = stats.totalFocusMinutes / 60 + 1
    val minsInCurrentLevel = stats.totalFocusMinutes % 60
    val levelProgress = (minsInCurrentLevel / 60f).coerceIn(0.05f, 1f)
    val rankTitle = readerRankTitle(stats.totalFocusMinutes.toInt())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Gamified Focus Identity & Level Progress Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D)),
            shape = RoundedCornerShape(24.dp),
            border = null,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0A0F1D),
                                Color(0xFF171C3A),
                                Color(0xFF0E1326)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val initial = (currentUser?.displayName ?: currentUser?.email ?: "F")
                                .trim()
                                .firstOrNull()
                                ?.uppercase() ?: "F"
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GamifiedPurple)
                            ) {
                                Text(
                                    text = initial,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser?.displayName ?: "Focus Guardian",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rankTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA5B4FC)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF34D399).copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "LVL $currentLevel",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }

                        XpRewardChip(text = "${stats.focusScore} XP")
                    }

                    // Level Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Next Level Progress",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "$minsInCurrentLevel / 60 min",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDDD6FE)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            color = GamifiedPurple,
                            trackColor = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }

                    // 3 Aligned Mini Metrics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 ${stats.currentStreak}d Streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = "⏱ ${stats.totalFocusMinutes}m Focused",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = if (accessibilityEnabled) "🛡 Shield On" else "⚠ Shield Off",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (accessibilityEnabled) Color(0xFF34D399) else Color(0xFFF87171)
                        )
                    }
                }
            }
        }

        // 2. Quick Focus Powers (2x2 Symmetrical Aligned Grid)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsQuickPowerTile(
                    icon = if (accessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Security,
                    title = "Focus Shield",
                    subtitle = if (accessibilityEnabled) "Blocker active" else "Tap to enable",
                    statusText = if (accessibilityEnabled) "ON" else "OFF",
                    isActive = accessibilityEnabled,
                    activeColor = Color(0xFF0A6B43),
                    onClick = { com.example.security.AccessibilityHelper.openAccessibilitySettings(context) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("accessibility_service_card")
                )
                SettingsQuickPowerTile(
                    icon = Icons.Default.NotificationsOff,
                    title = "Auto DND",
                    subtitle = if (isDndEnabled) "Alerts silenced" else "Alerts allowed",
                    statusText = if (isDndEnabled) "ON" else "OFF",
                    isActive = isDndEnabled,
                    activeColor = GamifiedPurple,
                    onClick = { viewModel.setDndEnabled(!isDndEnabled) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dnd_switch")
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsQuickPowerTile(
                    icon = Icons.Default.Star,
                    title = "Pro Tier",
                    subtitle = if (isProTierEnabled) "All modes open" else "Standard tier",
                    statusText = if (isProTierEnabled) "PRO" else "FREE",
                    isActive = isProTierEnabled,
                    activeColor = GamifiedPurple,
                    onClick = { viewModel.setProTierEnabled(!isProTierEnabled) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pro_tier_switch")
                )
                SettingsQuickPowerTile(
                    icon = Icons.Default.CloudSync,
                    title = "Cloud Sync",
                    subtitle = if (isCloudSyncEnabled) "Stats synced" else "Local only",
                    statusText = if (isCloudSyncEnabled) "SYNC" else "OFF",
                    isActive = isCloudSyncEnabled,
                    activeColor = Color(0xFF0284C7),
                    onClick = { viewModel.setCloudSyncEnabled(!isCloudSyncEnabled) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cloud_sync_switch")
                )
            }
        }

        // 3. Focus Automation & Break Cadence Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(22.dp),
            border = null,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Daily Focus Schedule
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CircularIconBadge(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                containerSize = 34.dp,
                                iconSize = 18.dp,
                                containerColor = GamifiedPurple
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Focus Schedule",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isScheduleEnabled) "${formatHour(startHour)} → ${formatHour(endHour)}" else "Automated daily focus window",
                                    fontSize = 11.sp,
                                    color = if (isScheduleEnabled) GamifiedPurple else MaterialTheme.colorScheme.secondary,
                                    fontWeight = if (isScheduleEnabled) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        AppSwitch(
                            checked = isScheduleEnabled,
                            onCheckedChange = { viewModel.toggleSchedule(it) },
                            modifier = Modifier.testTag("schedule_switch")
                        )
                    }

                    AnimatedVisibility(visible = isScheduleEnabled) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Start", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                                Slider(
                                    value = startHour.toFloat(),
                                    onValueChange = { viewModel.saveScheduleHours(it.toInt(), endHour) },
                                    valueRange = 0f..23f,
                                    steps = 22,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GamifiedPurple,
                                        activeTrackColor = GamifiedPurple,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("End", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                                Slider(
                                    value = endHour.toFloat(),
                                    onValueChange = { viewModel.saveScheduleHours(startHour, it.toInt()) },
                                    valueRange = 0f..23f,
                                    steps = 22,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GamifiedPurple,
                                        activeTrackColor = GamifiedPurple,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                // Break Reminder Cadence (Clean 1-tap Pills instead of raw text field)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Break Reminder Cadence",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (reminderInterval == "0" || reminderInterval.isBlank()) "Off" else "Every ${reminderInterval}m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = GamifiedPurple
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("0" to "Off", "15" to "15m", "25" to "25m", "45" to "45m", "60" to "60m").forEach { (value, label) ->
                            val isSelected = reminderInterval == value || (value == "0" && reminderInterval.isBlank())
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) GamifiedPurple else MaterialTheme.colorScheme.surface)
                                    .clickable { viewModel.setReminderInterval(value) }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. X-Style Grouped Accountability & Safety Card (Aligned Interactive Rows)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(22.dp),
            border = null,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Row 1: Accountability Partner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPartnerDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            containerSize = 34.dp,
                            iconSize = 18.dp,
                            containerColor = GamifiedPurple
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Accountability Partner", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (partnerEmail.isNotBlank()) "$partnerEmail${if (isPartnerNotificationsEnabled) " • Alerts On" else ""}" else "Tap to link a focus partner",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )

                // Row 2: Parental Lock PIN
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPinDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Pin,
                            contentDescription = null,
                            containerSize = 34.dp,
                            iconSize = 18.dp,
                            containerColor = GamifiedPurple
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Parental Lock PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (parentalPin.isNotBlank()) "Protected (4-digit PIN active)" else "Off • Tap to set protection PIN",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )

                // Row 3: Offline Privacy Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        CircularIconBadge(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            containerSize = 34.dp,
                            iconSize = 18.dp,
                            containerColor = GamifiedPurple
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Offline Privacy Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "100% on-device focus telemetry",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    AppSwitch(
                        checked = isPrivateTrackingEnabled,
                        onCheckedChange = { viewModel.setPrivateTrackingEnabled(it) },
                        modifier = Modifier.testTag("private_tracking_switch")
                    )
                }
            }
        }

        // 5. Minimal Clean App Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "FocusLock • Crafted by Temeselew Buta (Temu)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Privacy-first digital wellbeing & deep focus",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
            )
        }
    }

    // Modal Dialog: Accountability Partner
    if (showPartnerDialog) {
        var draftEmail by remember(partnerEmail) { mutableStateOf(partnerEmail) }
        var draftNotify by remember(isPartnerNotificationsEnabled) { mutableStateOf(isPartnerNotificationsEnabled) }
        AlertDialog(
            onDismissRequest = { showPartnerDialog = false },
            title = {
                Text("Accountability Partner", fontWeight = FontWeight.Black, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draftEmail,
                        onValueChange = { draftEmail = it },
                        label = { Text("Partner Email", fontSize = 11.sp) },
                        placeholder = { Text("friend@example.com", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("partner_email_field"),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notify partner on early quit", fontSize = 12.sp)
                        AppSwitch(
                            checked = draftNotify,
                            onCheckedChange = { draftNotify = it },
                            modifier = Modifier.testTag("partner_notifications_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setPartnerEmail(draftEmail.trim())
                        viewModel.setPartnerNotificationsEnabled(draftNotify)
                        showPartnerDialog = false
                    },
                    colors = appBlackButtonColors(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("SAVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPartnerDialog = false }) {
                    Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }

    // Modal Dialog: Parental Lock PIN
    if (showPinDialog) {
        var draftPin by remember(parentalPin) { mutableStateOf(parentalPin) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text("Parental Lock PIN", fontWeight = FontWeight.Black, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Protect focus settings and blocklists with a 4-digit PIN. Leave blank to disable.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = draftPin,
                        onValueChange = { if (it.length <= 4) draftPin = it.filter { ch -> ch.isDigit() } },
                        label = { Text("4-Digit PIN", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 1234", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parental_pin_field"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setParentalPin(draftPin)
                        showPinDialog = false
                    },
                    colors = appBlackButtonColors(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("SAVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun SettingsQuickPowerTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    statusText: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) activeColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (isActive) BorderStroke(1.dp, activeColor.copy(alpha = 0.35f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 0.dp else 2.dp),
        modifier = modifier
            .height(94.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isActive) activeColor else MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) Color.White else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isActive) activeColor else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}

private fun formatBedtimeClock(hour: Int, minute: Int): String {
    val h = if (hour == 0 || hour == 12) 12 else hour % 12
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format(java.util.Locale.getDefault(), "%d:%02d %s", h, minute, amPm)
}
