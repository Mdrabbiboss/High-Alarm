package com.programmer.highalarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import kotlin.time.Duration.Companion.seconds
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

// Premium Black + Graphite + Subtle Gold Color Palette
val MidnightBlue = HaObsidian
val DeepIndigo = HaSurfaceLow
val SoftAmber = HaGold
val SurfaceColor = HaSurface

@Composable
fun PremiumGeometricBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Obsidian base with a soft vertical falloff
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0B0B10), HaObsidian, Color(0xFF050507))
            )
        )

        // Faceted graphite panels (very low contrast, reads as texture)
        val facetA = Color(0xFF0F0F15)
        val facetB = Color(0xFF121218)
        val facetC = Color(0xFF0C0C11)

        val path1 = Path().apply {
            moveTo(0f, 0f)
            lineTo(width * 0.7f, 0f)
            lineTo(width * 0.9f, height * 0.35f)
            lineTo(width * 0.4f, height * 0.45f)
            close()
        }
        drawPath(path1, color = facetA)

        val path2 = Path().apply {
            moveTo(width * 0.7f, 0f)
            lineTo(width, 0f)
            lineTo(width, height * 0.5f)
            lineTo(width * 0.9f, height * 0.35f)
            close()
        }
        drawPath(path2, color = facetB)

        val path3 = Path().apply {
            moveTo(0f, height * 0.4f)
            lineTo(width * 0.4f, height * 0.45f)
            lineTo(width * 0.3f, height * 0.8f)
            lineTo(0f, height * 0.85f)
            close()
        }
        drawPath(path3, color = facetC)

        val path4 = Path().apply {
            moveTo(width * 0.4f, height * 0.45f)
            lineTo(width * 0.9f, height * 0.35f)
            lineTo(width, height * 0.65f)
            lineTo(width * 0.6f, height * 0.9f)
            close()
        }
        drawPath(path4, color = Color(0xFF0A0A0E))

        val path5 = Path().apply {
            moveTo(0f, height * 0.85f)
            lineTo(width * 0.3f, height * 0.8f)
            lineTo(width * 0.6f, height * 0.9f)
            lineTo(width, height * 0.75f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path5, color = Color(0xFF08080B))

        // Hair-thin tessellation lines
        val strokeColor = HaIvory.copy(alpha = 0.035f)
        val strokeWidth = 1.2f
        drawPath(path1, color = strokeColor, style = Stroke(strokeWidth))
        drawPath(path2, color = strokeColor, style = Stroke(strokeWidth))
        drawPath(path3, color = strokeColor, style = Stroke(strokeWidth))
        drawPath(path4, color = strokeColor, style = Stroke(strokeWidth))
        drawPath(path5, color = strokeColor, style = Stroke(strokeWidth))

        // Signature gold edge light
        val edgeStart = Offset(width * 0.6f, height * 0.9f)
        val edgeEnd = Offset(width, height * 0.75f)
        drawLine(
            brush = Brush.linearGradient(
                listOf(Color.Transparent, HaGold.copy(alpha = 0.18f)),
                start = edgeStart, end = edgeEnd
            ),
            start = edgeStart, end = edgeEnd,
            strokeWidth = 14f
        )
        drawLine(
            brush = Brush.linearGradient(
                listOf(HaGoldDeep.copy(alpha = 0.0f), HaGoldLight.copy(alpha = 0.75f)),
                start = edgeStart, end = edgeEnd
            ),
            start = edgeStart, end = edgeEnd,
            strokeWidth = 2f
        )
    }
}

class MainActivity : ComponentActivity() {
    private val intentFlow = MutableStateFlow<Intent?>(null)
    private val _showGoodMorning = MutableStateFlow(false)
    private lateinit var viewModel: AlarmViewModel

    fun triggerProtectedAlarm(context: Context, alarmId: Int) {
        val alarm = viewModel.alarms.value.find { it.id == alarmId } ?: return
        
        AlarmService.isRinging = true
        AlarmService.currentAlarmId = alarmId
        AlarmService.isBypassActive = true

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("RINGTONE_URI", alarm.ringtoneUri)
            putExtra("BOOST_VOLUME", alarm.isBoostVolumeEnabled)
            putExtra("FROM_SETTINGS_BYPASS", true)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("ALARM_TRIGGERED", true)
            putExtra("ALARM_ID", alarmId)
            putExtra("FROM_SETTINGS_BYPASS", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        intentFlow.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity full screen and restrictive
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        intentFlow.value = intent

        viewModel = ViewModelProvider(this)[AlarmViewModel::class.java]

        // Initialize settings immediately to prevent blank UI states
        val settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        settingsViewModel.init(this)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MidnightBlue,
                ) {
                    val context = LocalContext.current
                    val navController = rememberNavController()
                    val showGoodMorning by _showGoodMorning.collectAsState()

                    // Ensure alarms are loaded
                    LaunchedEffect(Unit) {
                        viewModel.loadAlarms(context)
                    }

                    // Handle new intents when app is in foreground
                    LaunchedEffect(Unit) {
                        intentFlow.collectLatest { incomingIntent ->
                            if (incomingIntent == null) return@collectLatest
                            
                            val isTriggered = incomingIntent.getBooleanExtra("ALARM_TRIGGERED", false)
                            val showGoodMorningIntent = incomingIntent.getBooleanExtra("SHOW_GOOD_MORNING", false)

                            if (showGoodMorningIntent) {
                                _showGoodMorning.value = true
                                intentFlow.value = null
                                return@collectLatest
                            }

                            if ((AlarmService.isRinging && !AlarmService.isPreviewActive) || isTriggered) {
                                // Reset the intent flow to null after recognizing the trigger 
                                // to prevent accidental launches on activity restarts or navigation
                                intentFlow.value = null
                                
                                val currentRoute = navController.currentBackStackEntry?.destination?.route
                                if (currentRoute != "dismiss") {
                                    hideSystemUI()
                                    navController.navigate("dismiss") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (AlarmService.isRinging && !AlarmService.isPreviewActive) {
                            val currentRoute = navController.currentBackStackEntry?.destination?.route
                            if (currentRoute != "dismiss") {
                                hideSystemUI()
                                navController.navigate("dismiss") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    val prefs = context.getSharedPreferences("app_settings", MODE_PRIVATE)
                    val isOnboardingCompleted = prefs.getBoolean("is_onboarding_completed", false)

                    val isInitialTrigger = intent.getBooleanExtra("ALARM_TRIGGERED", false)
                    val isTriggered = (isInitialTrigger || (AlarmService.isRinging && !AlarmService.isPreviewActive))
                    val startDest = if (isTriggered) "dismiss" else if (!isOnboardingCompleted) "onboarding" else "main"

                    NavHost(navController = navController, startDestination = startDest) {
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onCompleteOnboarding = {
                                    navController.navigate("main") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("main") {
                            AlarmMainScreen(
                                viewModel = viewModel,
                                onEditAlarm = { alarmId ->
                                    if (navController.currentDestination?.route == "main") {
                                        val alarm = viewModel.alarms.value.find { it.id == alarmId }
                                        if ((alarm != null) && viewModel.isAlarmLastMinuteLocked(this@MainActivity, alarm)) {
                                            Toast.makeText(this@MainActivity, "This alarm is locked (Last Minute Edits blocked)", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.prepareEditSession(this@MainActivity, alarmId)
                                            navController.navigate("edit?alarmId=$alarmId")
                                        }
                                    }
                                },
                                onOpenSettings = {
                                    if (navController.currentDestination?.route == "main") {
                                        navController.navigate("settings")
                                    }
                                },
                                onAddAlarm = {
                                    if (navController.currentDestination?.route == "main") {
                                        viewModel.prepareEditSession(this@MainActivity, null)
                                        navController.navigate("edit")
                                    }
                                },
                                onNavigateToSettingsHighlight = {
                                    if (navController.currentDestination?.route == "main") {
                                        navController.navigate("settings?highlightPreventUninstall=true")
                                    }
                                }
                            )
                        }
                        composable(
                            route = "settings?highlightPreventUninstall={highlightPreventUninstall}",
                            arguments = listOf(
                                navArgument("highlightPreventUninstall") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            val highlightPreventUninstall = backStackEntry.arguments?.getBoolean("highlightPreventUninstall") ?: false
                            SettingsScreen(
                                onBack = { 
                                    if (navController.currentDestination?.route?.startsWith("settings") == true) {
                                        navController.popBackStack() 
                                    }
                                },
                                onNavigateToTroubleshooting = {
                                    if (navController.currentDestination?.route?.startsWith("settings") == true) {
                                        navController.navigate("alarm_reliability")
                                    }
                                },
                                onNavigateToPremium = { contextual ->
                                    if (navController.currentDestination?.route?.startsWith("settings") == true) {
                                        navController.navigate("premium?contextual=$contextual")
                                    }
                                },
                                viewModel = viewModel,
                                settingsViewModel = settingsViewModel,
                                highlightPreventUninstall = highlightPreventUninstall
                            )
                        }
                        composable("alarm_reliability") {
                            AlarmReliabilityScreen(
                                onBack = {
                                    if (navController.currentDestination?.route == "alarm_reliability") {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                        composable(
                            route = "edit?alarmId={alarmId}",
                            arguments = listOf(
                                navArgument("alarmId") {
                                    type = NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ) { backStackEntry ->
                            val alarmIdArg = backStackEntry.arguments?.getInt("alarmId") ?: -1
                            LaunchedEffect(alarmIdArg) {
                                if (alarmIdArg != -1) {
                                    viewModel.prepareEditSession(context, alarmIdArg)
                                }
                            }
                            EditAlarmScreen(
                                onBack = { 
                                    if (navController.currentDestination?.route?.startsWith("edit") == true) {
                                        viewModel.prepareEditSession(context, null)
                                        navController.popBackStack() 
                                    }
                                },
                                onSave = {
                                    if (navController.currentDestination?.route?.startsWith("edit") == true) {
                                        viewModel.saveAlarm(context = context)
                                        navController.popBackStack()
                                    }
                                },
                                viewModel = viewModel,
                                settingsViewModel = settingsViewModel,
                                onNavigateToPremium = { contextual -> 
                                    if (navController.currentDestination?.route?.startsWith("edit") == true) {
                                        navController.navigate("premium?contextual=$contextual") 
                                    }
                                }
                            )
                        }
                        composable(
                            route = "premium?contextual={contextual}",
                            arguments = listOf(
                                navArgument("contextual") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            ),
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                    animationSpec = tween(700, easing = EaseInOutQuart)
                                ) + fadeIn(animationSpec = tween(700)) + scaleIn(initialScale = 0.96f, animationSpec = tween(700))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = tween(600, easing = EaseInOutQuart)
                                ) + fadeOut(animationSpec = tween(600))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(600))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = tween(600, easing = EaseInOutQuart)
                                ) + fadeOut(animationSpec = tween(600))
                            }
                        ) { backStackEntry ->
                            val isContextual = backStackEntry.arguments?.getBoolean("contextual") ?: false
                            PremiumSubscriptionScreen(
                                onBack = { 
                                    if (navController.currentDestination?.route?.startsWith("premium") == true) {
                                        navController.popBackStack() 
                                    }
                                },
                                isFromLockedChallenge = isContextual
                            )
                        }
                        composable("dismiss") {
                            val alarms by viewModel.alarms.collectAsState()
                            val latestIntent by intentFlow.collectAsState()
                            val settingsState by settingsViewModel.settings.collectAsState()
                            
                            val rawAlarm = remember(latestIntent, alarms) {
                                val alarmId = latestIntent?.getIntExtra("ALARM_ID", -1) ?: AlarmService.currentAlarmId
                                
                                if (latestIntent?.getBooleanExtra("ALARM_TRIGGERED", false) == true || AlarmService.isRinging) {
                                    if (alarmId != -1) {
                                        alarms.find { it.id == alarmId } ?: alarms.firstOrNull() ?: Alarm(0, "00:00 AM", isEnabled = true, challenges = listOf(ChallengeConfig("none")))
                                    } else {
                                        alarms.firstOrNull() ?: Alarm(0, "00:00 AM", isEnabled = true, challenges = listOf(ChallengeConfig("none")))
                                    }
                                } else {
                                    null
                                }
                            }

                            val alarm = remember(rawAlarm, settingsState.isPremium) {
                                if (rawAlarm == null) null
                                else {
                                    val (sanitized, _) = viewModel.sanitizeAlarmsForProStatus(this@MainActivity, listOf(rawAlarm), settingsState.isPremium)
                                    sanitized.firstOrNull() ?: rawAlarm
                                }
                            }
                            
                            if (alarm != null) {
                                fun commitPendingSettingsOffIfEligible() {
                                    val currentSettings = settingsViewModel.settings.value
                                    val isPending = currentSettings.isPendingDisablePreventEdits || AlarmService.isBypassActive || latestIntent?.getBooleanExtra("FROM_SETTINGS_BYPASS", false) == true
                                    if (isPending) {
                                        val hasOtherProtectedAlarm = viewModel.alarms.value.any { otherAlarm ->
                                            otherAlarm.id != alarm.id && viewModel.isAlarmLastMinuteLocked(this@MainActivity, otherAlarm)
                                        }
                                        val updatedSettings = if (!hasOtherProtectedAlarm) {
                                            currentSettings.copy(
                                                isPreventLastMinuteEditsEnabled = false,
                                                isPendingDisablePreventEdits = false
                                            )
                                        } else {
                                            currentSettings.copy(
                                                isPendingDisablePreventEdits = false
                                            )
                                        }
                                        settingsViewModel.updateSettings(this@MainActivity, updatedSettings)
                                        AlarmService.isBypassActive = false
                                    }
                                }

                                AlarmDismissScreen(
                                    alarm = alarm,
                                    settingsViewModel = settingsViewModel,
                                    onChallengeSolved = {
                                        // Finalize pending settings update immediately upon challenge solution
                                        commitPendingSettingsOffIfEligible()

                                        // --- BACKGROUND SCHEDULING (Survivable) ---
                                        // This runs immediately when the challenge is solved, 
                                        // before the 3s success animation finishes.
                                        
                                        // 1. Schedule Wake Up Check if enabled
                                        if (alarm.wakeUpCheckMinutes > 0) {
                                            AlarmScheduler.scheduleWakeUpCheck(this@MainActivity, alarm)
                                        }

                                        // 2. Reschedule repeating alarm or toggle off
                                        if (alarm.id != 0) {
                                            if (alarm.repeatDays.isNotEmpty()) {
                                                AlarmScheduler.schedule(this@MainActivity, alarm)
                                            } else {
                                                viewModel.toggleAlarm(this@MainActivity, alarm.id)
                                            }
                                        }
                                    }
                                ) {
                                    // --- UI DISMISSAL & NAVIGATION ---
                                    // This runs after the success animation
                                    
                                    commitPendingSettingsOffIfEligible()

                                    val stopIntent = Intent(this@MainActivity, AlarmService::class.java)
                                    stopService(stopIntent)
                                    showSystemUI()
                                    latestIntent?.removeExtra("ALARM_TRIGGERED")

                                    if (navController.currentDestination?.route == "dismiss") {
                                        navController.navigate("main") {
                                            popUpTo("dismiss") { inclusive = true }
                                        }
                                    }
                                }
                            } else {
                                LaunchedEffect(Unit) {
                                    if (navController.currentDestination?.route == "dismiss") {
                                        navController.navigate("main") {
                                            popUpTo("dismiss") { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showGoodMorning) {
                        GoodMorningDialog(onDismiss = { _showGoodMorning.value = false })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentFlow.value = intent
    }

    @SuppressLint("MissingPermission")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && AlarmService.isRinging && !AlarmService.isPreviewActive) {
            // Close status bar / system dialogs if user tries to pull down
            try {
                @Suppress("DEPRECATION")
                val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                sendBroadcast(closeIntent)
            } catch (_: Exception) {}
            
            // Re-hide UI
            hideSystemUI()
            
            // Force focus back by relaunching
            relaunchApp()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AlarmService.isRinging && !AlarmService.isPreviewActive) {
            relaunchApp()
        }
    }

    private fun relaunchApp() {
        val relaunchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ALARM_TRIGGERED", true)
        }
        startActivity(relaunchIntent)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (AlarmService.isRinging) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
        try {
            BillingManager.getInstance(this).queryPurchases()
        } catch (_: Exception) {}
        // Back from the overlay settings without granting -> close the floating guide.
        OverlayGuideActivity.onHostResumed()
        // Prepare the guide's icons/names ahead of time so it opens fully drawn.
        if (!PermissionUtils.canDrawOverlays(this)) OverlayGuideActivity.prefetch(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((AlarmService.isRinging || AlarmService.isPreviewActive) && 
            ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) || (keyCode == KeyEvent.KEYCODE_VOLUME_UP))) {
            forceMaxVolume()
            return true // Consume the event
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if ((AlarmService.isRinging || AlarmService.isPreviewActive) && 
            ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) || (keyCode == KeyEvent.KEYCODE_VOLUME_UP))) {
            forceMaxVolume()
            return true // Consume the event
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun forceMaxVolume() {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (_: Exception) {}
    }
}

@Composable
fun GoodMorningDialog(onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var showContent by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            showContent = true
        }

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(600)) + scaleIn(initialScale = 0.94f, animationSpec = tween(600, easing = EaseOutBack)),
            exit = fadeOut(tween(500))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .ha3DPanelDeep(RoundedCornerShape(32.dp), elevation = 28.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(32.dp)
            ) {
                // Outer subtle glow background
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Inner Gradient Glow
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        HaGoldMid.copy(alpha = 0.08f * glowAlpha), // Indigo glow
                                        Color.Transparent
                                    ),
                                    center = Offset.Infinite
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 32.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- Premium Success Icon (Animated Sunrise) ---
                        Box(contentAlignment = Alignment.Center) {
                            // Pulsing gold circle behind icon
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .graphicsLayer {
                                        scaleX = 1f + (0.1f * glowAlpha)
                                        scaleY = 1f + (0.1f * glowAlpha)
                                        alpha = 0.2f * glowAlpha
                                    }
                                    .background(SoftAmber, CircleShape)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(SoftAmber.copy(alpha = 0.2f), SoftAmber.copy(alpha = 0.05f))
                                        ),
                                        CircleShape
                                    )
                                    .border(1.5.dp, SoftAmber.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = SoftAmber,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "Good Morning! ☀️",
                            color = HaIvory,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "You did it. You're officially awake.",
                            color = HaIvory.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        
                        Text(
                            text = "Your morning is off to a great start.",
                            color = HaIvory.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .ha3DGoldFace(RoundedCornerShape(18.dp), elevation = 12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                "LET'S START", 
                                color = MidnightBlue, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 15.sp, 
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // --- Verification Detail ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Wake-Up Check Completed",
                                color = HaJade.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = HaJade.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = HaGold,
            onPrimary = HaObsidian,
            primaryContainer = HaUmber,
            onPrimaryContainer = HaGoldPale,
            secondary = HaGoldLight,
            onSecondary = HaObsidian,
            secondaryContainer = HaSurfaceHigh,
            onSecondaryContainer = HaIvory,
            tertiary = HaJade,
            onTertiary = HaObsidian,
            background = HaObsidian,
            onBackground = HaIvory,
            surface = HaSurface,
            onSurface = HaIvory,
            surfaceVariant = HaSurfaceHigh,
            onSurfaceVariant = HaMuted,
            surfaceContainerLowest = HaObsidian,
            surfaceContainerLow = HaSurfaceLow,
            surfaceContainer = HaSurface,
            surfaceContainerHigh = HaSurfaceHigh,
            surfaceContainerHighest = HaHairline,
            outline = HaHairline,
            outlineVariant = HaSurfaceHigh,
            error = HaCoral,
            onError = HaObsidian,
            inverseSurface = HaIvory,
            inverseOnSurface = HaObsidian,
            inversePrimary = HaGoldDeep,
            scrim = Color.Black
        ),
        typography = Typography(), 
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmMainScreen(
    viewModel: AlarmViewModel,
    onEditAlarm: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onAddAlarm: () -> Unit,
    onNavigateToSettingsHighlight: () -> Unit = onOpenSettings
) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsState()
    val hasLoaded by viewModel.hasLoaded.collectAsState()
    var isSelectionMode by remember { mutableStateOf(value = false) }
    val selectedIds = remember { mutableStateListOf<Int>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var alarmToDelete by remember { mutableStateOf<Int?>(null) }
    var isNavigating by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val componentName = remember { ComponentName(context, AlarmDeviceAdminReceiver::class.java) }

    var showPreventUninstallDialog by remember {
        mutableStateOf(
            !prefs.getBoolean("prevent_uninstall_prompt_handled", false) && !devicePolicyManager.isAdminActive(componentName)
        )
    }

    if (showPreventUninstallDialog) {
        Dialog(
            onDismissRequest = { /* Don't dismiss on outside click */ },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
                    .ha3DPanelDeep(RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .ha3DCoin(HaTone.Gold, elevation = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = haToneInk(HaTone.Gold),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Prevent App Uninstall?",
                        color = HaIvory,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "You set the alarm for a reason. This helps keep the app from being easily removed, so your alarms can stay reliable. You can always turn it off later in Settings.",
                        color = HaIvory,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                prefs.edit { putBoolean("prevent_uninstall_prompt_handled", true) }
                                showPreventUninstallDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .ha3DPanel(RoundedCornerShape(16.dp), elevation = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = null,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HaIvory
                            )
                        ) {
                            Text(
                                text = "Not Now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = {
                                prefs.edit { putBoolean("prevent_uninstall_prompt_handled", true) }
                                showPreventUninstallDialog = false
                                onNavigateToSettingsHighlight()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .ha3DGoldFace(RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MidnightBlue
                            )
                        ) {
                            Text(
                                text = "Enable",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    var timeTick by remember { mutableLongStateOf(System.currentTimeMillis() / 60000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15.seconds)
            timeTick = System.currentTimeMillis() / 60000
        }
    }

    // Reset navigation lock when screen is resumed
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNavigating = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HaObsidian)
    ) {
        PremiumGeometricBackground(modifier = Modifier.fillMaxSize())
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    SelectionTopBar(
                        selectedCount = selectedIds.size,
                        onCancel = {
                            isSelectionMode = false
                            selectedIds.clear()
                        },
                        onSelectAll = {
                            selectedIds.clear()
                            selectedIds.addAll(alarms.map { it.id })
                        },
                        onDeleteSelected = {
                            showDeleteConfirmation = true
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (!isNavigating) {
                                    isNavigating = true
                                    onOpenSettings()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .ha3DOrb()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = HaGoldLight, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    FloatingActionButton(
                        onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                onAddAlarm()
                            }
                        },
                        containerColor = Color.Transparent,
                        contentColor = HaObsidian,
                        shape = RoundedCornerShape(22.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        modifier = Modifier
                            .padding(bottom = 16.dp, end = 8.dp)
                            .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = HaGold.copy(alpha = 0.5f), spotColor = HaGold.copy(alpha = 0.6f))
                            .border(1.dp, HaGoldPale.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(HaGoldPale, HaGold, HaGoldMid, HaGoldDeep),
                                        start = Offset(0f, 0f),
                                        end = Offset(180f, 180f)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Alarm", tint = HaObsidian, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                val currentContext = LocalContext.current
                var isBatteryIgnored by remember { mutableStateOf(PermissionUtils.isBatteryOptimizationIgnored(currentContext)) }
                var canDrawOverlays by remember { mutableStateOf(PermissionUtils.canDrawOverlays(currentContext)) }
                var canScheduleExactAlarms by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            (currentContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
                        } else true
                    )
                }

                // Refresh permissions logic
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isBatteryIgnored = PermissionUtils.isBatteryOptimizationIgnored(currentContext)
                            canDrawOverlays = PermissionUtils.canDrawOverlays(currentContext)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                canScheduleExactAlarms = (currentContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Permission Banners for Stability
                if (!isBatteryIgnored) {
                    PermissionBanner(
                        title = "Battery Optimization",
                        description = "Enable to ensure alarms ring reliably in the background.",
                        onFix = { PermissionUtils.requestIgnoreBatteryOptimization(currentContext) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!canDrawOverlays) {
                    PermissionBanner(
                        title = "Display Over Other Apps",
                        description = "Required to show the alarm screen when your phone is locked.",
                        onFix = { PermissionUtils.requestOverlayPermission(currentContext) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms) {
                    PermissionBanner(
                        title = "Exact Alarms",
                        description = "Required for precise alarm timing.",
                        onFix = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = "package:${currentContext.packageName}".toUri()
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            currentContext.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AnimatedContent(
                    targetState = hasLoaded,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "main_content_anim",
                    modifier = Modifier.fillMaxSize()
                ) { loaded ->
                    if (!loaded) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading...",
                                color = SoftAmber.copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 2.sp
                            )
                        }
                    } else if (alarms.isEmpty() && !isSelectionMode) {
                        EmptyAlarmsState(onAddAlarm)
                    } else {
                        val nextAlarm by viewModel.nextAlarmInfo.collectAsState()

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if ((!isSelectionMode) && (nextAlarm != null)) {
                                item(key = "next_alarm_summary") {
                                    NextAlarmSummaryCard(nextAlarm!!)
                                }
                            }

                            items(
                                items = alarms,
                                key = { it.id },
                                contentType = { "alarm_card" }
                            ) { alarm ->
                                val isLocked = remember(alarm, alarms, timeTick) {
                                    viewModel.isAlarmLastMinuteLocked(currentContext, alarm)
                                }
                                var showMenu by remember { mutableStateOf(value = false) }
                                val isSelected = selectedIds.contains(alarm.id)

                                val dismissState = rememberSwipeToDismissBoxState()
                                
                                // Reset dismiss state if needed
                                LaunchedEffect(dismissState.currentValue) {
                                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart && !isSelectionMode) {
                                            viewModel.deleteAlarm(currentContext, alarm.id)
                                        }
                                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                    }
                                }

                                Box {
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = false,
                                        enableDismissFromEndToStart = !isSelectionMode && !isLocked,
                                        backgroundContent = {
                                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                                HaCoral.copy(alpha = 0.6f)
                                            } else Color.Transparent
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(color),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = null,
                                                        tint = HaIvory,
                                                        modifier = Modifier.padding(end = 32.dp)
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        AlarmCard(
                                            alarm = alarm,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = isSelected,
                                            isLocked = isLocked,
                                            onToggle = {
                                                if (isSelectionMode) {
                                                    if (isSelected) selectedIds.remove(alarm.id) else selectedIds.add(alarm.id)
                                                } else {
                                                    viewModel.toggleAlarm(currentContext, alarm.id)
                                                }
                                            },
                                            onEditChallenge = {
                                                if (!isNavigating) {
                                                    if (isSelectionMode) {
                                                        if (isSelected) selectedIds.remove(alarm.id) else selectedIds.add(alarm.id)
                                                    } else {
                                                        if (isLocked) {
                                                            Toast.makeText(currentContext, "This alarm is locked (Last Minute Edits blocked)", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            isNavigating = true
                                                            onEditAlarm(alarm.id)
                                                        }
                                                    }
                                                }
                                            },
                                            onLongClick = { if (!isSelectionMode) showMenu = true }
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        modifier = Modifier
                                            .background(SurfaceColor)
                                            .border(1.dp, HaIvory.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .width(180.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Alarm", color = HaIvory) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                showMenu = false
                                                if (isLocked) {
                                                    Toast.makeText(currentContext, "This alarm is locked (Last Minute Edits blocked)", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    if (!isNavigating) {
                                                        isNavigating = true
                                                        onEditAlarm(alarm.id)
                                                    }
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duplicate", color = HaIvory) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.duplicateAlarm(currentContext, alarm.id)
                                            }
                                        )
                                        if (alarms.size > 1) {
                                            DropdownMenuItem(
                                                text = { Text("Select Alarms", color = HaIvory) },
                                                leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(20.dp)) },
                                                onClick = {
                                                    showMenu = false
                                                    isSelectionMode = true
                                                    selectedIds.add(alarm.id)
                                                }
                                            )
                                        }
                                        HorizontalDivider(color = HaIvory.copy(alpha = 0.1f))
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = HaCoral, fontWeight = FontWeight.Bold) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = HaCoral, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                showMenu = false
                                                if (isLocked) {
                                                    Toast.makeText(currentContext, "Cannot delete: Alarm is locked (Last Minute Edits blocked)", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    alarmToDelete = alarm.id
                                                    showDeleteConfirmation = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmation) {
            DeleteConfirmationDialog(
                count = if (alarmToDelete != null) 1 else selectedIds.size,
                onDismiss = { 
                    showDeleteConfirmation = false
                    alarmToDelete = null
                },
                onConfirm = {
                    if (alarmToDelete != null) {
                        viewModel.deleteAlarm(context, alarmToDelete!!)
                    } else {
                        selectedIds.forEach { viewModel.deleteAlarm(context, it) }
                        selectedIds.clear()
                        isSelectionMode = false
                    }
                    showDeleteConfirmation = false
                    alarmToDelete = null
                }
            )
        }
    }
}

@Composable
fun NextAlarmSummaryCard(info: NextAlarmInfo) {
    val cardShape = RoundedCornerShape(32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            // real drop shadow under a solid (opaque) panel -> lifted, 3D feel
            .ha3DPanel(cardShape, gold = true, elevation = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3D gold coin
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .ha3DGoldCoin(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFF2A1F0E),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Next alarm in ${info.remainingFormatted}",
                color = HaIvory.copy(alpha = 0.62f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = info.time.substringBefore(" "),
                    color = HaIvory,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 54.sp
                )
                if (info.time.contains(" ")) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = info.time.substringAfter(" "),
                        color = HaGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAlarmsState(onAddAlarm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(HaIvory.copy(alpha = 0.03f), CircleShape)
                .border(1.dp, HaIvory.copy(alpha = 0.07f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = SoftAmber.copy(alpha = 0.6f),
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "No Alarms Set",
            color = HaIvory,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Wake up on time and start\nyour morning with a challenge!",
            color = HaMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onAddAlarm,
            colors = ButtonDefaults.buttonColors(containerColor = SoftAmber.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SoftAmber.copy(alpha = 0.3f)),
            modifier = Modifier.height(50.dp).padding(horizontal = 32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("SET AN ALARM", color = SoftAmber, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) {
            Text("Cancel", color = HaIvory, fontWeight = FontWeight.Bold)
        }
        
        Text(
            text = "$selectedCount Selected",
            color = HaIvory,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )

        Row {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = HaIvory)
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = HaCoral)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: Alarm,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isLocked: Boolean = false,
    onToggle: () -> Unit,
    onEditChallenge: () -> Unit,
    onLongClick: () -> Unit
) {
    val cardAlpha = if (alarm.isEnabled || isSelectionMode) 1f else 0.5f
    
    val cardShape = remember { RoundedCornerShape(24.dp) }
    val gradientBrush = remember(alarm.isEnabled) {
        if (alarm.isEnabled) {
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.06f),
                0.5f to Color.White.copy(alpha = 0.01f),
                1f to Color.Black.copy(alpha = 0.25f)
            )
        } else null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = cardShape,
        color = if (isSelected) SurfaceColor.copy(alpha = 0.95f) else SurfaceColor,
        shadowElevation = if (isSelected || alarm.isEnabled) 12.dp else 4.dp,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        border = when {
            isSelected -> BorderStroke(2.dp, HaGold)
            alarm.isEnabled -> BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(HaGoldPale.copy(alpha = 0.45f), HaGold.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.6f))
                )
            )
            else -> BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(HaIvory.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.5f)))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onEditChallenge,
                    onLongClick = onLongClick
                )
        ) {
            if (gradientBrush != null && !isSelectionMode) {
                Box(modifier = Modifier.fillMaxSize().background(gradientBrush))
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(checkedColor = SoftAmber, uncheckedColor = HaMuted)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Label if exists
                    if (alarm.label.isNotEmpty()) {
                        Text(
                            text = alarm.label.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (alarm.isEnabled) SoftAmber else HaMuted,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = alarm.time.substringBefore(" "),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = HaIvory.copy(alpha = cardAlpha),
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = alarm.time.substringAfter(" "),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = (if (alarm.isEnabled) SoftAmber else HaMuted).copy(alpha = cardAlpha),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    // Days Display
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val days = listOf("S", "M", "T", "W", "T", "F", "S")
                        days.forEachIndexed { index, day ->
                            val isDaySelected = alarm.repeatDays.contains(index)
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = if (isDaySelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isDaySelected) HaGold.copy(alpha = cardAlpha) else HaMuted.copy(alpha = cardAlpha * 0.45f),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    // Challenge Badge
                    Surface(
                        onClick = onEditChallenge,
                        color = HaIvory.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(top = 10.dp),
                        border = BorderStroke(1.dp, (if (alarm.isEnabled) SoftAmber else HaMuted).copy(alpha = 0.2f))
                    ) {
                        val activeChallenges = alarm.challenges.filter { it.id != "none" }.mapNotNull { config -> 
                            getAllChallenges().find { it.id == config.id }?.name 
                        }
                        val labelText = if (activeChallenges.isEmpty()) "None" else activeChallenges.joinToString(", ")
                        val firstIcon = alarm.challenges.firstOrNull { it.id != "none" }?.let { config ->
                            getAllChallenges().find { it.id == config.id }?.icon
                        } ?: Icons.Default.Block
                        
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = firstIcon,
                                contentDescription = null,
                                tint = if (alarm.isEnabled) SoftAmber else HaMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = labelText,
                                fontSize = 11.sp,
                                color = if (alarm.isEnabled) SoftAmber else HaMuted,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isSelectionMode) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = SoftAmber,
                                modifier = Modifier.padding(bottom = 4.dp).size(18.dp)
                            )
                        }
                        HaSwitch(
                            checked = alarm.isEnabled,
                            onCheckedChange = { onToggle() },
                            enabled = !isLocked
                        )
                    }
                }
            }
            
            // Decorative side line for enabled alarms
            if (alarm.isEnabled && !isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                        .background(Brush.verticalGradient(listOf(HaGoldPale, HaGold, HaGoldDeep)))
                )
            }
        }
    }
}

@Composable
fun PermissionBanner(
    title: String,
    description: String,
    onFix: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .ha3DPanel(RoundedCornerShape(20.dp), gold = true, elevation = 10.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .ha3DGoldCoin(),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = Color(0xFF2A1F0E), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = HaIvory, fontSize = 16.sp)
                Text(description, fontSize = 12.sp, color = HaMuted)
            }
            TextButton(
                onClick = onFix,
                colors = ButtonDefaults.textButtonColors(contentColor = SoftAmber)
            ) {
                Text("FIX", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HaPanelDeep,
        title = {
            Text(
                text = "Delete Alarm",
                color = HaIvory,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Text(
                text = if (count > 1) "Are you sure you want to delete these $count alarms?" else "Are you sure you want to delete this alarm?",
                color = HaIvory.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = HaCoral.copy(alpha = 0.8f))
            ) {
                Text("Delete", color = HaIvory, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HaMuted, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.85f).border(1.dp, HaIvory.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    )
}
