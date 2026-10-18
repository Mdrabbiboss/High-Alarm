package com.programmer.highalarm

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.net.toUri
import java.util.Locale
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToTroubleshooting: () -> Unit = {},
    onNavigateToPremium: (Boolean) -> Unit = {},
    viewModel: AlarmViewModel,
    settingsViewModel: SettingsViewModel,
    highlightPreventUninstall: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val billingManager = remember { BillingManager.getInstance(context, scope) }

    LaunchedEffect(Unit) {
        billingManager.queryPurchases()
    }

    val settings by settingsViewModel.settings.collectAsState()
    var showVibrationDialog by remember { mutableStateOf(false) }
    var showDisableFadeInDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val componentName = remember { ComponentName(context, AlarmDeviceAdminReceiver::class.java) }

    var isAdminActive by remember { mutableStateOf(devicePolicyManager.isAdminActive(componentName)) }
    var isHighlighted by remember { mutableStateOf(highlightPreventUninstall && !isAdminActive) }

    val infiniteTransition = rememberInfiniteTransition(label = "highlight_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val cardModifier = if (isHighlighted) {
        Modifier
            .fillMaxWidth()
            .border(2.dp, SoftAmber.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
    } else {
        Modifier
    }

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isAdminActive = devicePolicyManager.isAdminActive(componentName)
        settingsViewModel.updateSettings(context, settings.copy(isPreventUninstallEnabled = isAdminActive))
    }

    LaunchedEffect(Unit) {
        isAdminActive = devicePolicyManager.isAdminActive(componentName)
        if (settings.isPreventUninstallEnabled != isAdminActive) {
            settingsViewModel.updateSettings(context, settings.copy(isPreventUninstallEnabled = isAdminActive))
        }
    }

    LaunchedEffect(highlightPreventUninstall) {
        if (highlightPreventUninstall && !isAdminActive) {
            isHighlighted = true
            delay(2800.milliseconds)
            isHighlighted = false
        }
    }

    if (showVibrationDialog) {
        VibrationSelectionDialog(
            currentPattern = settings.selectedVibrationPattern,
            isPremium = settings.isPremium,
            onDismiss = { 
                VibrationUtils.stop(context)
                showVibrationDialog = false 
            },
            onSelect = { pattern ->
                settingsViewModel.updateSettings(context, settings.copy(selectedVibrationPattern = pattern))
            }
        )
    }

    if (showDisableFadeInDialog) {
        DisableFadeInDialog(
            onDismiss = { showDisableFadeInDialog = false },
            onDisable = {
                showDisableFadeInDialog = false
                settingsViewModel.updateSettings(context, settings.copy(isFadeInEnabled = false))
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HaObsidian)
    ) {
        data class Star(val x: Float, val y: Float, val size: Float, val alpha: Float)
        val stars = remember {
            val rng = Random(42)
            List(100) {
                Star(
                    x = rng.nextFloat(),
                    y = rng.nextFloat(),
                    size = rng.nextFloat() * 2.2f + 1.2f,
                    alpha = rng.nextFloat() * 0.5f + 0.15f
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HaInk,
                        HaObsidian,
                        HaObsidian
                    )
                )
            )

            stars.forEach { star ->
                val px = star.x * w
                val py = star.y * h
                val radius = star.size.dp.toPx()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HaIvory.copy(alpha = star.alpha * 0.7f),
                            HaGoldPale.copy(alpha = star.alpha * 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(px, py),
                        radius = radius * 1.8f
                    ),
                    radius = radius * 1.8f,
                    center = Offset(px, py)
                )
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Settings", 
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ) 
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(HaIvory.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = HaIvory)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                SettingsCategory(title = "ALARM SETTINGS")
                SettingsCard {
                    SettingsToggleItem(
                        title = "Vibration",
                        description = "Vibrate when alarm rings",
                        icon = Icons.Default.Vibration,
                        checked = settings.isVibrationEnabled,
                        onCheckedChange = { settingsViewModel.updateSettings(context, settings.copy(isVibrationEnabled = it)) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Custom Vibration", 
                        description = settings.selectedVibrationPattern, 
                        icon = Icons.Default.SettingsVoice,
                        onClick = { showVibrationDialog = true }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Enable Snooze",
                        description = "Allow snoozing alarms",
                        icon = Icons.Default.Bedtime,
                        checked = settings.isSnoozeEnabled,
                        onCheckedChange = { settingsViewModel.updateSettings(context, settings.copy(isSnoozeEnabled = it)) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Auto Silence", 
                        description = "Stop ringing after selected minutes (${settings.autoSilenceMinutes} minutes)", 
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        showChevron = false
                    )
                    Slider(
                        value = settings.autoSilenceMinutes.toFloat(),
                        onValueChange = { 
                            val snappedValue = ((it + 2.5f) / 5).toInt() * 5
                            val finalValue = snappedValue.coerceIn(5, 60)
                            if (finalValue != settings.autoSilenceMinutes) {
                                settingsViewModel.updateSettings(context, settings.copy(autoSilenceMinutes = finalValue))
                            }
                        },
                        valueRange = 5f..60f,
                        steps = 10,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        colors = SliderDefaults.colors(thumbColor = SoftAmber, activeTrackColor = SoftAmber)
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Upcoming Alarm Alert",
                        description = "Show alerts before an alarm goes off",
                        icon = Icons.Default.NotificationsActive,
                        checked = settings.isUpcomingAlarmAlertEnabled,
                        onCheckedChange = { settingsViewModel.updateSettings(context, settings.copy(isUpcomingAlarmAlertEnabled = it)) }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Mute During Challenge",
                        description = "Lower volume for 2 mins while solving challenges",
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        checked = settings.isMuteDuringChallengeEnabled,
                        onCheckedChange = { settingsViewModel.updateSettings(context, settings.copy(isMuteDuringChallengeEnabled = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
                SettingsCategory(title = "CHEATING PREVENTION")
                
                val isPreventEdits = settings.isPreventLastMinuteEditsEnabled
                val alarms by viewModel.alarms.collectAsState()
                val protectedAlarm = remember(alarms, isPreventEdits) {
                    if (isPreventEdits) {
                        alarms.find { viewModel.isAlarmLastMinuteLocked(context, it) }
                    } else {
                        null
                    }
                }

                SettingsCard {
                    Surface(
                        modifier = cardModifier,
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        SettingsToggleItem(
                            title = "Prevent App Uninstall",
                            description = "Extra security for deep sleepers",
                            icon = Icons.Default.VerifiedUser,
                            checked = isAdminActive,
                            onCheckedChange = { enable ->
                                isHighlighted = false
                                if (enable) {
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protects the app from being uninstalled during an alarm.")
                                    }
                                    adminLauncher.launch(intent)
                                } else {
                                    devicePolicyManager.removeActiveAdmin(componentName)
                                    isAdminActive = false
                                    settingsViewModel.updateSettings(context, settings.copy(isPreventUninstallEnabled = false))
                                }
                            }
                        )
                    }
                    SettingsDivider()

                    SettingsToggleItem(
                        title = "Block Last Minute Edits",
                        description = "Force commitment to your schedule",
                        icon = Icons.Default.LockClock,
                        checked = isPreventEdits,
                        onCheckedChange = { enable ->
                            if (!enable) {
                                if (protectedAlarm != null) {
                                    settingsViewModel.updateSettings(context, settings.copy(isPendingDisablePreventEdits = true))
                                    (context as? MainActivity)?.triggerProtectedAlarm(context, protectedAlarm.id)
                                } else {
                                    settingsViewModel.updateSettings(context, settings.copy(isPreventLastMinuteEditsEnabled = false, isPendingDisablePreventEdits = false))
                                }
                            } else {
                                settingsViewModel.updateSettings(context, settings.copy(isPreventLastMinuteEditsEnabled = true, isPendingDisablePreventEdits = false))
                            }
                        },
                        onClick = if (isPreventEdits && protectedAlarm != null) {
                            {
                                settingsViewModel.updateSettings(context, settings.copy(isPendingDisablePreventEdits = true))
                                (context as? MainActivity)?.triggerProtectedAlarm(context, protectedAlarm.id)
                            }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
                PremiumUpgradeCard(
                    isPremium = settings.isPremium,
                    activePurchaseType = settings.activePurchaseType,
                    onClick = {
                        if (settings.isPremium) {
                            openManageSubscriptions(context)
                        } else {
                            onNavigateToPremium(true)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))
                SettingsCategory(title = "SOUND SETTINGS")
                SettingsCard {
                    SettingsToggleItem(
                        title = "Fade In Duration",
                        description = "Gentle wake up experience",
                        icon = Icons.Default.Waves,
                        checked = settings.isFadeInEnabled,
                        onCheckedChange = { enable ->
                            if (!enable) {
                                showDisableFadeInDialog = true
                            } else {
                                settingsViewModel.updateSettings(context, settings.copy(isFadeInEnabled = true))
                            }
                        }
                    )
                    if (settings.isFadeInEnabled) {
                        SettingsDivider()
                        SettingsItem(
                            title = "Fade Duration", 
                            description = "${settings.fadeInSeconds} seconds", 
                            icon = Icons.Default.Timer,
                            showChevron = false
                        )
                        Slider(
                            value = settings.fadeInSeconds.toFloat().coerceIn(5f, 60f),
                            onValueChange = { newValue ->
                                val snapped = ((newValue + 2.5f) / 5).toInt() * 5
                                val finalVal = snapped.coerceIn(5, 60)
                                if (finalVal != settings.fadeInSeconds) {
                                    settingsViewModel.updateSettings(context, settings.copy(fadeInSeconds = finalVal))
                                }
                            },
                            valueRange = 5f..60f,
                            steps = 10,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = SoftAmber,
                                activeTrackColor = SoftAmber,
                                inactiveTrackColor = HaTrackOff
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                SettingsCategory(title = "TROUBLESHOOTING")
                AlarmReliabilityCard(
                    onClick = onNavigateToTroubleshooting
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                SettingsCategory(title = "SUPPORT")
                SettingsCard {
                    SettingsItem(
                        title = "Rate our app",
                        description = "Love the app? Leave a review!",
                        icon = Icons.Default.StarRate,
                        onClick = { openPlayStoreListing(context) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Send Feedback",
                        description = "Report issues or suggest features",
                        icon = Icons.Default.Email,
                        onClick = { openEmailFeedback(context) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Share with Friends",
                        description = "Recommend this app to others",
                        icon = Icons.Default.Share,
                        onClick = { openShareSheet(context) }
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Privacy Policy",
                        description = "Read our privacy policy & terms",
                        icon = Icons.Default.PrivacyTip,
                        onClick = { showPrivacyPolicyDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

fun openPlayStoreListing(context: Context) {
    val packageName = context.packageName
    val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(marketIntent)
    } catch (_: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open Google Play Store.", Toast.LENGTH_SHORT).show()
        }
    }
}

fun openManageSubscriptions(context: Context) {
    val packageName = context.packageName
    val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/account/subscriptions?package=$packageName".toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open subscription management.", Toast.LENGTH_SHORT).show()
        }
    }
}

fun openEmailFeedback(context: Context) {
    val recipient = "phprogramr@gmail.com"
    val subject = "High Alarm Feedback"

    val appVersion = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0"
    } catch (_: Exception) {
        "1.0"
    }
    val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ${Build.MODEL}"

    val body = """
        Hello High Alarm Team,

        I would like to share some feedback about the High Alarm app.

        App version: $appVersion
        Android version: $androidVersion
        Device: $deviceModel

        Feedback:
        [Write your feedback here]

        Thank you.
    """.trimIndent()

    val mailUri = "mailto:$recipient?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}".toUri()
    val emailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(emailIntent)
    } catch (_: Exception) {
        Toast.makeText(context, "No email app is available on this device.", Toast.LENGTH_SHORT).show()
    }
}

fun openShareSheet(context: Context) {
    val packageName = context.packageName
    val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
    val shareText = "Try High Alarm — a smart alarm app designed to help you wake up on time.\n\nDownload High Alarm:\n$playStoreUrl"

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    val chooserIntent = Intent.createChooser(sendIntent, "Share High Alarm").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(chooserIntent)
    } catch (_: Exception) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("High Alarm Link", playStoreUrl)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Could not share at this time.", Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyDialog(
    url: String = "https://sites.google.com/view/high-alarm-privacy-policy/home",
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    val webViewAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "webview_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = HaObsidian
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(HaIvory.copy(alpha = 0.1f))
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = HaIvory,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Privacy Policy",
                            color = HaIvory,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                HorizontalDivider(color = HaIvory.copy(alpha = 0.1f))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        return false
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = webViewAlpha }
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = SoftAmber,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmReliabilityCard(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            ,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    SoftAmber.copy(alpha = 0.8f),
                    HaGoldMid.copy(alpha = 0.4f),
                    SoftAmber.copy(alpha = 0.8f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HaUmber,
                            HaSurface,
                            HaUmber.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SoftAmber.copy(alpha = 0.25f),
                                    SoftAmber.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, SoftAmber.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SoftAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    AutoResizeText(
                        text = "Alarm reliability",
                        color = HaIvory,
                        fontWeight = FontWeight.Black,
                        initialFontSize = 17.sp,
                        minFontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AutoResizeText(
                        text = "Fix missed alarms & battery optimization",
                        color = HaIvory,
                        fontWeight = FontWeight.Medium,
                        initialFontSize = 13.sp,
                        minFontSize = 8.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SoftAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = SoftAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DisableFadeInDialog(
    onDismiss: () -> Unit,
    onDisable: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = HaSurface,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SoftAmber.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Waves,
                        contentDescription = null,
                        tint = SoftAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Disable Fade-in?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HaIvory,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fade-in is currently enabled. Disabling it will turn off this setting. You can enable it again anytime from Settings.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = HaMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, HaMuted.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HaIvory
                        )
                    ) {
                        Text(
                            text = "Keep It On",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = onDisable,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftAmber,
                            contentColor = MidnightBlue
                        )
                    ) {
                        Text(
                            text = "Disable",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun VibrationSelectionDialog(
    currentPattern: String,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = HaSurface,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 400.dp)
                .heightIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(SoftAmber.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Custom Vibration", fontSize = 20.sp, fontWeight = FontWeight.Black, color = HaIvory)
                Text("Choose how your alarm vibrates", fontSize = 13.sp, color = HaMuted, modifier = Modifier.padding(top = 2.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                VibrationPattern.all.forEach { pattern ->
                    val isSelected = pattern.name == currentPattern
                    val isLocked = pattern.isPremium && !isPremium
                    
                    Surface(
                        onClick = { 
                            if (!isLocked) {
                                onSelect(pattern.name)
                                VibrationUtils.vibrate(context, pattern.pattern, amplitudes = pattern.amplitudes)
                            }
                        },
                        color = if (isSelected) SoftAmber.copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        border = if (isSelected) BorderStroke(1.dp, SoftAmber) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                pattern.icon, 
                                contentDescription = null, 
                                tint = if (isSelected) SoftAmber else if (isLocked) HaIvory.copy(alpha = 0.6f) else HaIvory,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                pattern.name, 
                                color = if (isSelected) SoftAmber else if (isLocked) HaIvory.copy(alpha = 0.6f) else HaIvory,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(18.dp))
                            } else if (isLocked) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(SoftAmber.copy(alpha = 0.12f), CircleShape)
                                        .border(1.dp, SoftAmber.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = SoftAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (!isPremium) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = SoftAmber.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SoftAmber.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(SoftAmber.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upgrade to Premium to unlock all vibration patterns", fontSize = 11.sp, color = SoftAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("DONE", color = SoftAmber, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        color = SoftAmber,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = HaIvory.copy(alpha = 0.05f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(title: String, description: String, icon: ImageVector, showChevron: Boolean = true, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(HaIvory.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = HaIvory, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(18.dp))
        BoxWithConstraints(
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AutoResizeText(
                    text = title,
                    color = HaIvory,
                    fontWeight = FontWeight.ExtraBold,
                    initialFontSize = 16.sp,
                    minFontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                AutoResizeText(
                    text = description,
                    color = HaMuted,
                    fontWeight = FontWeight.Medium,
                    initialFontSize = 12.sp,
                    minFontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = HaMuted.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String, 
    description: String, 
    icon: ImageVector, 
    checked: Boolean, 
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (checked) SoftAmber.copy(alpha = 0.15f) else HaIvory.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (checked) SoftAmber else HaIvory, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(18.dp))
        BoxWithConstraints(
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AutoResizeText(
                    text = title,
                    color = HaIvory,
                    fontWeight = FontWeight.ExtraBold,
                    initialFontSize = 16.sp,
                    minFontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                AutoResizeText(
                    text = description,
                    color = HaMuted,
                    fontWeight = FontWeight.Medium,
                    initialFontSize = 12.sp,
                    minFontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box {
            Box(modifier = Modifier.graphicsLayer(scaleX = 0.85f, scaleY = 0.85f)) {
                Switch(
                    checked = checked,
                    onCheckedChange = if (onClick != null) null else onCheckedChange,
                    enabled = onClick == null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MidnightBlue,
                        checkedTrackColor = SoftAmber,
                        uncheckedThumbColor = HaMuted,
                        uncheckedTrackColor = HaTrackOff,
                        disabledCheckedTrackColor = SoftAmber,
                        disabledCheckedThumbColor = MidnightBlue
                    )
                )
            }
            if (onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onClick)
                )
            }
        }
    }
}

@Composable
fun PremiumUpgradeCard(
    isPremium: Boolean,
    activePurchaseType: String = "",
    onClick: () -> Unit
) {
    val cardTitle = when {
        !isPremium -> "High Alarm PRO"
        activePurchaseType == "lifetime" -> "Lifetime Pro Active"
        activePurchaseType == "yearly" -> "Yearly PRO Active"
        activePurchaseType == "monthly" -> "Monthly PRO Active"
        else -> "PRO Access Active"
    }

    val cardSubtitle = if (isPremium) "All features & challenges unlocked" else "Unlock all challenges & features"

    val buttonText = if (isPremium) "MANAGE SUBSCRIPTION" else "UPGRADE TO PRO"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            ,
        shape = RoundedCornerShape(28.dp),
        color = if (isPremium) SoftAmber.copy(alpha = 0.12f) else HaSurfaceLow,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                colors = listOf(
                    SoftAmber.copy(alpha = if (isPremium) 0.8f else 0.6f),
                    HaGoldLight,
                    HaGoldLight.copy(alpha = 0.35f),
                    SoftAmber.copy(alpha = if (isPremium) 0.8f else 0.6f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SoftAmber.copy(alpha = 0.4f),
                                    SoftAmber.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .border(1.2.dp, SoftAmber.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPremium) Icons.Default.CheckCircle else Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = SoftAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    AutoResizeText(
                        text = cardTitle,
                        color = HaIvory,
                        fontWeight = FontWeight.Black,
                        initialFontSize = 20.sp,
                        minFontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AutoResizeText(
                        text = cardSubtitle,
                        color = HaMuted,
                        fontWeight = FontWeight.Medium,
                        initialFontSize = 13.sp,
                        minFontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = SoftAmber.copy(alpha = 0.5f), spotColor = SoftAmber.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MidnightBlue
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    HaGoldPale,
                                    SoftAmber,
                                    HaGold,
                                    SoftAmber,
                                    HaGoldMid
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.1.sp,
                            color = MidnightBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MidnightBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDivider() {
    Spacer(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        HaWine.copy(alpha = 0.15f),
                        SoftAmber.copy(alpha = 0.20f),
                        HaWine.copy(alpha = 0.15f)
                    )
                )
            )
    )
}

@Composable
fun AutoResizeText(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    initialFontSize: TextUnit,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 9.sp,
    maxLines: Int = 1
) {
    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth
        val effectiveMinFontSize = if (initialFontSize <= 13.sp && text.length > 25) 6.sp else minFontSize
        val approxCharWidthDp = when {
            initialFontSize >= 18.sp -> 11f
            initialFontSize >= 16.sp -> 9.5f
            initialFontSize <= 13.sp -> 6.5f
            else -> 8f
        }
        val estimatedTextWidthDp = text.length * approxCharWidthDp
        val calculatedFontSize = if (estimatedTextWidthDp > widthDp.value && widthDp.value > 0f) {
            val scale = (widthDp.value / estimatedTextWidthDp).coerceIn(effectiveMinFontSize.value / initialFontSize.value, 1f)
            (initialFontSize.value * scale).coerceAtLeast(effectiveMinFontSize.value).sp
        } else {
            initialFontSize
        }

        Text(
            text = text,
            color = color,
            fontWeight = fontWeight,
            fontSize = calculatedFontSize,
            maxLines = maxLines,
            softWrap = false
        )
    }
}
