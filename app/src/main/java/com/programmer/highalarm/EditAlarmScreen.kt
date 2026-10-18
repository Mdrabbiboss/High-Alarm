package com.programmer.highalarm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun EditAlarmScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: AlarmViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToPremium: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsState()
    
    val selectedTime by viewModel.selectedTime.collectAsState()
    val selectedChallenges by viewModel.selectedChallenges.collectAsState()
    val selectedRingtone by viewModel.selectedRingtone.collectAsState()
    val selectedDays by viewModel.selectedDays.collectAsState()
    val label by viewModel.label.collectAsState()
    val isBoostEnabled by viewModel.isBoostVolumeEnabled.collectAsState()
    val wakeUpCheckMinutes by viewModel.wakeUpCheckMinutes.collectAsState()

    var isSaveInProgress by remember { mutableStateOf(false) }

    var previewPlayer: Ringtone? by remember { mutableStateOf(null) }
    var showRepeatSheet by remember { mutableStateOf(false) }
    var showChallengeSettingsDialog by remember { mutableStateOf<Challenge?>(null) }
    var previewChallenge by remember { mutableStateOf<Pair<Challenge, Int>?>(null) }
    var previewChallengeExtra by remember { mutableStateOf("") }

    // Premium Transition States
    var tappedPremiumChallengeId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Info Sheet States
    var showInfoSheet by remember { mutableStateOf(false) }
    var showWakeUpTutorial by remember { mutableStateOf(false) }
    var infoSheetContent by remember { mutableStateOf<InfoSheetContent?>(null) }

    // Preview flow states
    var previewStep by remember { mutableIntStateOf(0) } // 0: Ringing, 1: Challenge, 2: Success
    var previewRingtonePlayer: Ringtone? by remember { mutableStateOf(null) }

    data class PendingAction(
        val run: () -> Unit,
        val needsCamera: Boolean,
        val needsActivity: Boolean
    )
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    val startPreviewInternal: (Challenge, Int, Int, String?) -> Unit = { chal, rounds, diff, extra ->
        previewChallenge = chal to rounds
        previewChallengeExtra = extra ?: ""
        viewModel.updateChallengeSetting(chal.id, rounds)
        viewModel.updateChallengeDifficulty(chal.id, diff)
        viewModel.updateChallengeExtra(chal.id, extra ?: "")
        previewStep = 0 // Start with Ringing
        showChallengeSettingsDialog = null
        pendingAction = null
    }

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAction?.run?.invoke()
        } else {
            Toast.makeText(context, "Physical Activity permission is required for this challenge.", Toast.LENGTH_LONG).show()
        }
        pendingAction = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val currentAction = pendingAction
            if (currentAction != null) {
                val hasActivity = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

                if (currentAction.needsActivity && !hasActivity) {
                    activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    currentAction.run.invoke()
                    pendingAction = null
                }
            }
        } else {
            Toast.makeText(context, "Camera access is required for camera-based challenges.", Toast.LENGTH_SHORT).show()
            pendingAction = null
        }
    }

    val resolveChallengePermission: (Challenge, String?, () -> Unit) -> Unit = { chal, extraVal, action ->
        val extra = extraVal ?: ""
        val needsCamera = chal is Challenge.QRCode || chal is Challenge.ScanSink || chal is Challenge.ScanObject ||
            ((chal is Challenge.Squat || chal is Challenge.PushUp) && (extra.isEmpty() || extra == "camera"))
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        val needsActivity = chal is Challenge.Steps || chal is Challenge.Squat || chal is Challenge.PushUp
        val hasActivity = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

        pendingAction = PendingAction(action, needsCamera, needsActivity)

        if (needsCamera && !hasCamera) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else if (needsActivity && !hasActivity) {
            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            action()
            pendingAction = null
        }
    }

    val handlePreviewRequest: (Challenge, Int, Int, String?) -> Unit = { chal, rounds, diff, extra ->
        resolveChallengePermission(chal, extra) {
            startPreviewInternal(chal, rounds, diff, extra)
        }
    }

    LaunchedEffect(previewChallenge) {
        AlarmService.isPreviewActive = previewChallenge != null
    }

    // Disable back button/gestures while preview is active
    BackHandler(enabled = previewChallenge != null) {
        // Block
    }

    // Lifecycle-aware Preview Cleanup
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                previewRingtonePlayer?.stop()
                previewRingtonePlayer = null
                VibrationUtils.stop(context)
                previewChallenge = null
                previewStep = 0 // Reset step
                context.stopService(Intent(context, AlarmService::class.java))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            previewRingtonePlayer?.stop()
            previewRingtonePlayer = null
            VibrationUtils.stop(context)
            previewChallenge = null
            previewStep = 0 // Reset step
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }

    // Full Interactive Alarm Preview Flow
    val isRingingInPreview = previewChallenge != null && previewStep < 2

    LaunchedEffect(isRingingInPreview) {
        if (isRingingInPreview) {
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra("ALARM_ID", 0)
                putExtra("RINGTONE_URI", selectedRingtone.second)
                putExtra("BOOST_VOLUME", isBoostEnabled)
                putExtra("IS_PREVIEW", true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }

    val nativeRingtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }

            if (pickedUri != null) {
                viewModel.handleRingtonePicked(context, pickedUri)
            }
        }
    }

    fun launchNativeRingtonePicker() {
        val currentUriString = selectedRingtone.second
        val currentName = selectedRingtone.first

        val defaultSymbolicUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val defaultActualUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)

        val isDefaultSelected = currentName == "Default" || 
                currentUriString.isEmpty() || 
                currentUriString == defaultSymbolicUri?.toString() || 
                (defaultActualUri != null && currentUriString == defaultActualUri.toString()) ||
                currentUriString == "content://settings/system/alarm_alert"

        val existingUri: Uri? = if (isDefaultSelected) {
            defaultSymbolicUri ?: "content://settings/system/alarm_alert".toUri()
        } else {
            try { currentUriString.toUri() } catch (_: Exception) { defaultSymbolicUri }
        }

        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra("android.intent.extra.ringtone.DEFAULT_STRING", "Default alarm sound")
            if (existingUri != null) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
            }
        }
        nativeRingtonePickerLauncher.launch(intent)
    }

    LaunchedEffect(Unit) {
        viewModel.loadRingtones(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.stop()
        }
    }

    val timeParts = selectedTime.split(":", " ")
    val hour = timeParts[0].toInt()
    val minute = timeParts[1].toInt()
    val amPm = timeParts[2]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(MidnightBlue, DeepIndigo)),
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (viewModel.currentEditingAlarmId == null) "New Alarm" else "Edit Alarm",
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (!isSaveInProgress) {
                                    isSaveInProgress = true
                                    onBack()
                                }
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .ha3DOrb()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = HaGoldLight)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (!isSaveInProgress) {
                                    isSaveInProgress = true
                                    onSave()
                                }
                            },
                            enabled = !isSaveInProgress,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Save", color = SoftAmber, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WheelPicker(
                                items = (1..12).map { it.toString() },
                                initialIndex = hour - 1,
                                isInfinite = true,
                                width = 75.dp
                            ) { idx ->
                                val newTime = String.format(Locale.getDefault(), "%02d:%02d %s", idx + 1, minute, amPm)
                                viewModel.selectTime(newTime)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            WheelPicker(
                                items = (0..59).map { String.format(Locale.getDefault(), "%02d", it) },
                                initialIndex = minute,
                                isInfinite = true,
                                width = 75.dp
                            ) { idx ->
                                val newTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour, idx, amPm)
                                viewModel.selectTime(newTime)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            WheelPicker(
                                items = listOf("AM", "PM"),
                                initialIndex = if (amPm == "AM") 0 else 1,
                                isInfinite = false,
                                width = 85.dp
                            ) { idx ->
                                val newAmPm = if (idx == 0) "AM" else "PM"
                                val newTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour, minute, newAmPm)
                                viewModel.selectTime(newTime)
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(3) }) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        EditOptionItem(
                            title = "Repeat",
                            value = getRepeatDisplayString(selectedDays),
                            icon = Icons.Default.Repeat,
                            onClick = { showRepeatSheet = true }
                        )

                        EditTextFieldItem(
                            label = "Label",
                            value = label,
                            onValueChange = { viewModel.updateLabel(it) }
                        )

                        EditOptionItem(
                            title = "Ringtone",
                            value = viewModel.sanitizeDisplayName(selectedRingtone.first),
                            icon = Icons.Default.Notifications,
                            onClick = { launchNativeRingtonePicker() }
                        )

                        EditToggleItem(
                            title = "Boost Volume",
                            subtitle = if (isBoostEnabled) "150% Extra High" else "Normal volume",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            checked = isBoostEnabled,
                            onCheckedChange = { viewModel.toggleBoostVolume(it) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "Wake Up Challenge",
                            color = HaIvory,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Tap to select • Long press for settings",
                            color = HaMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }
                }

                items(getAllChallenges()) { challenge ->
                    val config = selectedChallenges.find { it.id == challenge.id }
                    val isSelected = config != null
                    val subtitle = if (config != null) {
                        when(challenge) {
                            is Challenge.Maths -> "${getDifficultyName(config.difficulty)} - ${config.setting}Q"
                            is Challenge.AdvancedMaths -> "${getDifficultyName(config.difficulty)} - ${config.setting}Q"
                            is Challenge.Steps -> "${config.setting} Steps"
                            is Challenge.PushUp -> "${config.setting} Reps"
                            is Challenge.Squat -> {
                                val mode = if (config.extra == "motion") "Motion" else "Camera"
                                "${config.setting} Reps - $mode"
                            }
                            is Challenge.AudioMemory -> "${getDifficultyName(config.difficulty)} - ${config.setting}R"
                            is Challenge.Shake -> "${config.setting} Shakes"
                            is Challenge.Tap -> if (config.setting == 0) "Random" else "${config.setting} Taps"
                            is Challenge.ClockReading -> "${getDifficultyName(config.difficulty)} - ${config.setting}R"
                            is Challenge.Maze -> "${getDifficultyName(config.difficulty)} - ${config.setting}R"
                            is Challenge.Memory -> "${getDifficultyName(config.difficulty)} - ${config.setting}R"
                            is Challenge.SpellBee -> "${getDifficultyName(config.difficulty)} - ${config.setting}R"
                            is Challenge.Puzzle -> "${getDifficultyName(config.difficulty)} - ${config.setting}R"
                            is Challenge.ReverseTyping -> "${config.setting} Rounds"
                            is Challenge.Charger -> if (config.setting == 0) "Random" else "${config.setting} Time"
                            is Challenge.LightsOn -> "${config.setting} Lux"
                            is Challenge.Rewrite -> if (config.setting == 1) "Custom" else "Random"
                            is Challenge.QRCode -> if (config.setting == 1) "Specific" else "Any QR"
                            is Challenge.ScanObject -> if (config.setting == 1) "Random" else if (config.extra.isNotBlank() && !config.extra.contains(",")) config.extra else "Pick One"
                            is Challenge.ScanSink -> if (config.setting == 1) "Specific Sink" else "Any Sink"
                            is Challenge.BalanceTest -> "${config.setting}s Balance"
                            is Challenge.SafeCode -> "${config.setting} Round${if (config.setting > 1) "s" else ""}"
                            is Challenge.ControlPanel -> "${config.setting} Round${if (config.setting > 1) "s" else ""}"
                            else -> challenge.description
                        }
                    } else {
                        challenge.description
                    }

                    ChallengeItem(
                        challenge = challenge,
                        isSelected = isSelected,
                        isPremium = challenge.isPremium && !settings.isPremium,
                        isTapped = tappedPremiumChallengeId == challenge.id,
                        subtitle = subtitle,
                        onLongClick = {
                            if (challenge != Challenge.None) {
                                showChallengeSettingsDialog = challenge
                            }
                        }
                    ) {
                        if (challenge.isPremium && !settings.isPremium) {
                            scope.launch {
                                tappedPremiumChallengeId = challenge.id
                                delay(200.milliseconds) // Fast 150-250ms feedback
                                tappedPremiumChallengeId = null
                                onNavigateToPremium(true)
                            }
                        } else {
                            val config = viewModel.getChallengeConfig(challenge.id)
                            resolveChallengePermission(challenge, config.extra) {
                                viewModel.selectChallenge(context, challenge, settings.isPremium)
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(3) }) {
                    Column {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .ha3DPanel(RoundedCornerShape(16.dp), gold = true, elevation = 8.dp),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(30.dp).ha3DCoin(HaTone.Gold, elevation = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = haToneInk(HaTone.Gold), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Free users can long-press premium challenges to preview them.",
                                    color = HaIvory.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                "Wake Up Check",
                                color = HaIvory,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            IconButton(
                                onClick = {
                                    infoSheetContent = InfoSheetContent(
                                        title = "Follow-up Check",
                                        description = "A secondary check to ensure you're still awake after dismissing the alarm. If you don't respond, the alarm will ring again.",
                                        note = "Highly recommended for deep sleepers."
                                    )
                                    showInfoSheet = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = HaMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 40.dp)
                                .ha3DPanel(RoundedCornerShape(24.dp), elevation = 10.dp),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Follow-up Notification", fontWeight = FontWeight.Bold, color = HaIvory, fontSize = 16.sp)
                                        AutoResizeText(
                                            text = "Tap notification to confirm or alarm rings again",
                                            color = HaMuted,
                                            fontWeight = FontWeight.Normal,
                                            initialFontSize = 11.sp,
                                            minFontSize = 8.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            showWakeUpTutorial = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Info",
                                            tint = HaMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val options = listOf(
                                        "Off" to 0, "1m" to 1, "2m" to 2, "5m" to 5, 
                                        "10m" to 10, "15m" to 15, "30m" to 30, "40m" to 40
                                    )
                                    options.forEach { (label, mins) ->
                                        val isSelected = wakeUpCheckMinutes == mins
                                        val isOptionPremium = (mins > 0) && !settings.isPremium
                                        
                                        Surface(
                                            onClick = { 
                                                if (isOptionPremium) {
                                                    onNavigateToPremium(true)
                                                } else {
                                                    viewModel.updateWakeUpCheck(mins) 
                                                }
                                            },
                                            modifier = Modifier
                                                .padding(vertical = 6.dp)
                                                .width(68.dp)
                                                .height(48.dp)
                                                .then(
                                                    if (isSelected) Modifier.ha3DGoldFace(RoundedCornerShape(14.dp), elevation = 8.dp)
                                                    else Modifier.ha3DPanel(RoundedCornerShape(14.dp), elevation = 5.dp)
                                                ),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    label,
                                                    color = if (isSelected) Color(0xFF1A140A) else HaIvory,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp
                                                )
                                                if (isOptionPremium) {
                                                    Icon(
                                                        Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color(0xFF1A140A) else HaGold.copy(alpha = 0.7f),
                                                        modifier = Modifier.padding(start = 4.dp).size(12.dp)
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

            if (showRepeatSheet) {
                RepeatBottomSheet(
                    selectedDays = selectedDays,
                    onDismiss = { showRepeatSheet = false },
                    onSelection = { viewModel.setRepeatDays(it) },
                    onToggleDay = { viewModel.toggleDay(it) }
                )
            }

            if (showChallengeSettingsDialog != null) {
                showChallengeSettingsDialog?.let { challenge ->
                    val configMatch = selectedChallenges.find { it.id == challenge.id }
                    val isChallengeSelected = configMatch != null
                    val config = viewModel.getChallengeConfig(challenge.id)
                    ChallengeSettingsDialog(
                        challenge = challenge,
                        currentValue = config.setting,
                        currentDifficulty = config.difficulty,
                        currentExtra = config.extra,
                        isSelected = isChallengeSelected,
                        onDismiss = { showChallengeSettingsDialog = null },
                        onConfirm = { rounds, diff, extra -> 
                            viewModel.updateChallengeSetting(challenge.id, rounds)
                            viewModel.updateChallengeDifficulty(challenge.id, diff)
                            viewModel.updateChallengeExtra(challenge.id, extra ?: "")
                            showChallengeSettingsDialog = null
                        },
                        onPreview = { chal, rounds, diff, extra ->
                            handlePreviewRequest(chal, rounds, diff, extra)
                        }
                    )
                }
            }
        }
    }

    if (previewChallenge != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightBlue)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume background touches */ }
                )
        ) {
            when (previewStep) {
                0 -> {
                    InitialDismissState(
                        time = selectedTime,
                        label = label,
                        isSnoozeEnabled = settings.isSnoozeEnabled,
                        onDismissClick = { previewStep = 1 },
                        onSnoozeClick = {
                            previewChallenge = null 
                            context.stopService(Intent(context, AlarmService::class.java))
                        }
                    )
                }
                1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = SoftAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 64.dp)
                        ) {
                            previewChallenge?.first?.let { challenge ->
                                Text(
                                    text = challenge.name.uppercase(),
                                    fontSize = 14.sp,
                                    color = SoftAmber,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            previewChallenge?.let { (challenge, rounds) ->
                                val config = selectedChallenges.find { it.id == challenge.id } ?: ChallengeConfig(challenge.id, setting = rounds, extra = previewChallengeExtra)
                                when (challenge) {
                                    is Challenge.Maths -> {
                                        MathChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.AdvancedMaths -> {
                                        AdvancedMathChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.Charger -> {
                                        ChargerChallengeView(
                                            targetCount = config.setting,
                                            onComplete = { 
                                                previewStep = 2 
                                            }
                                        )
                                    }
                                    is Challenge.Tap -> {
                                        TapChallengeView(
                                            targetCount = config.setting,
                                            onComplete = { 
                                                previewStep = 2 
                                            }
                                        )
                                    }
                                    is Challenge.ClockReading -> {
                                        ClockReadingChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.LightsOn -> {
                                        LightsOnChallengeView(
                                            targetLux = config.setting,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.Steps -> {
                                        StepsChallengeView(
                                            targetSteps = config.setting,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.Maze -> {
                                        MazeChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.Memory -> {
                                        MemoryChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.Shake -> {
                                        ShakeChallengeView(
                                            targetShakes = config.setting,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.SpellBee -> {
                                        SpellBeeChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.Puzzle -> {
                                        PuzzleChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.ScanSink -> {
                                        ScanSinkChallengeView(
                                            mode = config.setting,
                                            extra = config.extra,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.PushUp -> {
                                        PushUpChallengeView(
                                            targetReps = config.setting,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.Squat -> {
                                        val mode = if (config.extra == "motion") 1 else 0
                                        SquatChallengeView(
                                            targetReps = config.setting,
                                            detectionMode = mode,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.ScanObject -> {
                                        ObjectChallengeView(
                                            mode = config.setting,
                                            extra = config.extra,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.Rewrite -> {
                                        RewriteChallengeView(
                                            mode = config.setting,
                                            customText = config.extra,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.QRCode -> {
                                        QRCodeChallengeView(
                                            mode = config.setting,
                                            targetQR = config.extra,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.ReverseTyping -> {
                                        ReverseTypingChallengeView(
                                            rounds = config.setting,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.AudioMemory -> {
                                        AudioMemoryChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.BalanceTest -> {
                                        BalanceTestChallengeView(
                                            targetSeconds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.SafeCode -> {
                                        SafeCodeChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    is Challenge.ControlPanel -> {
                                        ControlPanelChallengeView(
                                            rounds = config.setting,
                                            difficulty = config.difficulty,
                                            onComplete = {
                                                previewStep = 2
                                            }
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
                2 -> {
                    SuccessAnimation(onFinished = { 
                        previewChallenge = null 
                        context.stopService(Intent(context, AlarmService::class.java))
                    })
                }
                else -> {}
            }

            if (previewStep > 0) {
                IconButton(
                    onClick = { 
                        previewChallenge = null 
                        context.stopService(Intent(context, AlarmService::class.java))
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(HaIvory.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Preview", tint = HaIvory)
                }
            }
            
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp),
                color = SoftAmber,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "PREVIEW MODE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MidnightBlue
                )
            }
        }
    }

    if (showInfoSheet && infoSheetContent != null) {
        infoSheetContent?.let { content ->
            InfoBottomSheet(
                content = content,
                onDismiss = { showInfoSheet = false }
            )
        }
    }

    if (showWakeUpTutorial) {
        WakeUpTutorialBottomSheet(
            onDismiss = { showWakeUpTutorial = false }
        )
    }
}





fun getRepeatDisplayString(days: List<Int>): String {
    if (days.isEmpty()) return "Once"
    if (days.size == 7) return "Every Day"
    val isWeekdays = days.size == 5 && days.containsAll(listOf(1, 2, 3, 4, 5))
    if (isWeekdays) return "Weekdays"
    val isWeekends = days.size == 2 && days.containsAll(listOf(0, 6))
    if (isWeekends) return "Weekends"
    
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    return days.sorted().joinToString(", ") { dayNames[it] }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RepeatBottomSheet(
    selectedDays: List<Int>,
    onDismiss: () -> Unit,
    onSelection: (List<Int>) -> Unit,
    onToggleDay: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var isCustomExpanded by remember { 
        mutableStateOf(
            selectedDays.isNotEmpty() && 
            selectedDays.size != 7 && 
            !(selectedDays.size == 5 && selectedDays.containsAll(listOf(1,2,3,4,5))) &&
            !(selectedDays.size == 2 && selectedDays.containsAll(listOf(0,6)))
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HaPanelDeep,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .ha3DInset(CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Repeat Schedule",
                color = HaIvory,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            val modes = listOf(
                Triple("Once", "Ring only one time", emptyList()),
                Triple("Every Day", "Mon - Sun", listOf(0, 1, 2, 3, 4, 5, 6)),
                Triple("Weekdays", "Mon - Fri", listOf(1, 2, 3, 4, 5)),
                Triple("Weekends", "Sat & Sun", listOf(0, 6))
            )

            modes.forEach { (label, subtitle, days) ->
                val isSelected = !isCustomExpanded && (
                    (label == "Once" && selectedDays.isEmpty()) || 
                    (label != "Once" && selectedDays.size == days.size && selectedDays.containsAll(days))
                )
                
                RepeatModeItem(
                    label = label,
                    subtitle = subtitle,
                    isSelected = isSelected,
                    onClick = {
                        isCustomExpanded = false
                        onSelection(days)
                    }
                )
            }

            RepeatModeItem(
                label = "Custom",
                subtitle = "Select specific days",
                isSelected = isCustomExpanded,
                onClick = { isCustomExpanded = true }
            )

            AnimatedVisibility(visible = isCustomExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    val weekDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                    weekDays.forEachIndexed { index, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable { onToggleDay(index) }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = name,
                                color = if (selectedDays.contains(index)) SoftAmber else HaIvory,
                                fontSize = 16.sp,
                                fontWeight = if (selectedDays.contains(index)) FontWeight.Bold else FontWeight.Normal
                            )
                            HaSwitch(
                                checked = selectedDays.contains(index),
                                onCheckedChange = { onToggleDay(index) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HaGoldButton(text = "Done", onClick = onDismiss, height = 54.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepeatModeItem(label: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 4.dp)
            .then(
                if (isSelected) Modifier.ha3DPanelSelected(RoundedCornerShape(16.dp), elevation = 8.dp)
                else Modifier.ha3DPanel(RoundedCornerShape(16.dp), elevation = 4.dp)
            )
            .combinedClickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = if (isSelected) SoftAmber else HaIvory,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                subtitle,
                color = if (isSelected) SoftAmber.copy(alpha = 0.6f) else HaMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = SoftAmber,
                unselectedColor = HaMuted
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    isInfinite: Boolean = false,
    width: Dp = 75.dp,
    playScrollSound: Boolean = false,
    onIndexSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    var previousCenteredIndex by remember { mutableStateOf<Int?>(null) }
    val itemHeight = 48.dp
    val visibleItemsCount = 5
    val viewportHeight = itemHeight * visibleItemsCount
    
    val totalItems = if (isInfinite) 10000 * items.size else items.size
    val startIndex = if (isInfinite) {
        val mid = totalItems / 2
        mid - (mid % items.size) + initialIndex
    } else {
        initialIndex
    }

    val state = rememberLazyListState(startIndex)
    
    // --- SYNCHRONOUS STATE TRACKING ---
    // We use a derivedStateOf to track the centered index in real-time.
    // This ensures that even if 'isScrollInProgress' is true, we always know the "current" selection.
    val centeredIndex by remember(items.size) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) startIndex % items.size
            else {
                val center = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2f
                val closest = visibleItems.minByOrNull {
                    abs((it.offset + it.size / 2).toFloat() - center)
                }
                (closest?.index ?: 0) % items.size
            }
        }
    }

    // Update the ViewModel whenever the centered index changes (Real-time sync)
    LaunchedEffect(centeredIndex) {
        onIndexSelected(centeredIndex)
        if (playScrollSound) {
            if (previousCenteredIndex != null && previousCenteredIndex != centeredIndex) {
                ScrollSoundEffect.play(context)
            }
            previousCenteredIndex = centeredIndex
        }
    }

    val snapLayoutInfoProvider = remember(state) {
        SnapLayoutInfoProvider(state)
    }
    val snappingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider)

    Box(
        modifier = Modifier
            .width(width)
            .height(viewportHeight),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .ha3DPanel(RoundedCornerShape(14.dp), gold = true, elevation = 10.dp),
            color = Color.Transparent,
            shape = RoundedCornerShape(14.dp)
        ) {}

        LazyColumn(
            state = state,
            flingBehavior = snappingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * 2), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalItems) { index ->
                val item = items[index % items.size]
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    val transform by remember(index) {
                        derivedStateOf {
                            val layoutInfo = state.layoutInfo
                            val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2f
                            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                            
                            if (itemInfo != null) {
                                val itemCenter = (itemInfo.offset + itemInfo.size / 2).toFloat()
                                val distance = itemCenter - viewportCenter
                                
                                val rotX = (distance / 120f).coerceIn(-1.2f, 1.2f) * -45f
                                val alpha = (1f - (abs(distance) / 125f).coerceIn(0f, 0.8f)).coerceIn(0.2f, 1f)
                                val scale = (1.1f - (abs(distance) / 350f)).coerceIn(0.85f, 1.1f)
                                val transY = (distance * 0.08f)
                                
                                TransformData(rotX, alpha, scale, transY)
                            } else {
                                TransformData(0f, 0.2f, 0.85f, 0f)
                            }
                        }
                    }

                    Text(
                        text = item,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = HaIvory,
                        modifier = Modifier
                            .graphicsLayer {
                                rotationX = transform.rotX
                                alpha = transform.alpha
                                scaleX = transform.scale
                                scaleY = transform.scale
                                translationY = transform.transY
                                cameraDistance = 14f * density 
                            },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

data class TransformData(
    val rotX: Float,
    val alpha: Float,
    val scale: Float,
    val transY: Float
)

data class InfoSheetContent(
    val title: String,
    val description: String,
    val note: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChallengeSettingsDialog(
    challenge: Challenge,
    currentValue: Int,
    currentDifficulty: Int,
    currentExtra: String,
    isSelected: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, String?) -> Unit,
    onPreview: (Challenge, Int, Int, String?) -> Unit
) {
    val resolvedInitialValue = remember(challenge.id) {
        when (challenge) {
            is Challenge.Shake -> if (currentValue !in listOf(15, 30, 50, 75, 100, 120) || currentValue == 0) 30 else currentValue
            is Challenge.Steps -> if (currentValue !in listOf(10, 20, 30, 50, 100)) 20 else currentValue
            is Challenge.LightsOn -> if (currentValue < 10) 100 else currentValue
            is Challenge.Tap -> if (currentValue == 0) 0 else if (currentValue !in listOf(12, 15, 20, 25, 30, 35, 40, 45, 50, 55)) 30 else currentValue
            is Challenge.Maze, is Challenge.Memory -> if (currentValue !in listOf(1, 2, 3, 4, 5)) 1 else currentValue
            is Challenge.ClockReading -> if (currentValue !in listOf(1, 2, 3, 4, 5, 6)) 1 else currentValue
            is Challenge.SpellBee -> if (currentValue !in listOf(1, 2, 3, 5, 7)) 1 else currentValue
            is Challenge.Puzzle -> if (currentValue !in listOf(1, 2, 3)) 1 else currentValue
            is Challenge.ScanSink -> 0 
            is Challenge.PushUp -> if (currentValue !in listOf(5, 10, 15, 20, 30, 50, 100)) 15 else currentValue
            is Challenge.Squat -> if (currentValue !in listOf(5, 10, 15, 20, 30, 50, 100)) 15 else currentValue
            is Challenge.ScanObject -> if (currentValue == 0 && currentExtra.isEmpty()) 1 else if (currentValue !in listOf(0, 1)) 1 else currentValue
            is Challenge.QRCode -> if (currentExtra.isEmpty() || currentValue !in listOf(0, 1)) 0 else currentValue
            is Challenge.Rewrite -> if (currentValue == 1 && !isSelected) 0 else if (currentValue !in listOf(0, 1)) 0 else currentValue
            is Challenge.BalanceTest -> if (currentValue !in listOf(15, 20, 30, 45, 60)) 15 else currentValue
            is Challenge.SafeCode -> if (currentValue !in 1..6) 1 else currentValue
            is Challenge.ControlPanel -> if (currentValue !in 1..6) 1 else currentValue
            else -> if (currentValue < 1) 1 else currentValue
        }
    }

    var selectedValue by remember(challenge.id) { mutableIntStateOf(resolvedInitialValue) }
    var selectedDifficulty by remember(challenge.id) { mutableIntStateOf(currentDifficulty) }
    var selectedExtra by remember(challenge.id) { 
        val initialExtra = if (challenge is Challenge.ScanObject && currentExtra.isEmpty()) {
            ObjectCategories.list.joinToString(",")
        } else if (challenge is Challenge.QRCode && resolvedInitialValue == 0) {
            ""
        } else currentExtra
        mutableStateOf(initialExtra)
    }

    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            Toast.makeText(context, "Sink registered successfully!", Toast.LENGTH_SHORT).show()
            selectedExtra = "registered_sink_${System.currentTimeMillis()}" 
        }
    }

    Dialog(
        onDismissRequest = { /* Do nothing to prevent accidental outside clicks from closing the dialog */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = (if (challenge !is Challenge.ScanObject) {
                    Modifier.fillMaxWidth(0.95f).wrapContentHeight()
                } else {
                    Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f)
                }).ha3DPanelDeep(RoundedCornerShape(32.dp), elevation = 28.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = if (challenge !is Challenge.ScanObject) {
                        Modifier.padding(24.dp)
                    } else {
                        Modifier.fillMaxSize().padding(24.dp)
                    }
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(40.dp).ha3DCoin(HaTone.Gold, elevation = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = challenge.icon, contentDescription = null, tint = haToneInk(HaTone.Gold), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "${challenge.name} Settings", fontWeight = FontWeight.Black, color = HaIvory, fontSize = 18.sp, maxLines = 1) 
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp).ha3DOrb()) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = HaIvory.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    val columnModifier = if (challenge !is Challenge.ScanObject) {
                        Modifier
                    } else {
                        Modifier.weight(1f)
                    }
                    Column(modifier = columnModifier) {
                        if (challenge is Challenge.ClockReading || challenge is Challenge.Maze || challenge is Challenge.Memory || challenge is Challenge.SpellBee || challenge is Challenge.Puzzle || challenge is Challenge.Maths || challenge is Challenge.AdvancedMaths || challenge is Challenge.AudioMemory || challenge is Challenge.BalanceTest || challenge is Challenge.SafeCode || challenge is Challenge.ControlPanel) {
                            Text("Difficulty", color = HaMuted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            val difficulties = when(challenge) {
                                is Challenge.Maze -> listOf("EASY" to "Simple, short paths", "MEDIUM" to "Longer routes", "HARD" to "Complex maze structures", "EXTREME" to "Massive, highly intricate grid")
                                is Challenge.Memory -> listOf("EASY" to "3x3 grid, short sequence", "MEDIUM" to "3x3 grid, 5-tile sequence", "HARD" to "4x4 grid, long sequence", "EXTREME" to "5x5 grid, complex sequence")
                                is Challenge.SpellBee -> listOf("EASY" to "5-6 letter words", "MEDIUM" to "6-8 letter words", "HARD" to "8-10 letter words", "EXTREME" to "10+ letter words")
                                is Challenge.Puzzle -> listOf("EASY" to "2x2 grid, quick solve", "MEDIUM" to "3x3 classic puzzle", "HARD" to "4x4 grid challenge", "EXTREME" to "5x5 brain melter")
                                is Challenge.Maths -> listOf("EASY" to "Basic sums", "MEDIUM" to "Mixed operations", "HARD" to "Solve for X", "EXTREME" to "Advanced arithmetic")
                                is Challenge.AdvancedMaths -> listOf("EASY" to "Linear functions", "MEDIUM" to "Quadratic Eq", "HARD" to "Algebraic mapping", "EXTREME" to "Higher degrees")
                                is Challenge.AudioMemory -> listOf("EASY" to "Memorize 3 words", "MEDIUM" to "Memorize 4 words", "HARD" to "Memorize 5 words", "EXTREME" to "Memorize 6 words")
                                is Challenge.BalanceTest -> listOf("EASY" to "Large target, high tolerance", "MEDIUM" to "Moderate target area", "HARD" to "Small target, precise tilt", "EXTREME" to "Tiny target, zero margin for error")
                                is Challenge.SafeCode -> listOf("EASY" to "Simple direct digits", "MEDIUM" to "Direct & comparison clues", "HARD" to "Calculated & combined clues", "EXTREME" to "Complex logical reasoning")
                                is Challenge.ControlPanel -> listOf("EASY" to "Basic toggle controls", "MEDIUM" to "Sliders & selectors", "HARD" to "Complex parameters", "EXTREME" to "Full system console")
                                else -> listOf("EASY" to "5-minute intervals", "MEDIUM" to "Any minute value", "HARD" to "Tricky hand positions", "EXTREME" to "All minute positions")
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                difficulties.forEachIndexed { index, (title, desc) ->
                                    val isSel = selectedDifficulty == index
                                    Column {
                                        Surface(
                                            onClick = { selectedDifficulty = index }, 
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isSel) Modifier.ha3DPanelSelected(RoundedCornerShape(16.dp), elevation = 8.dp)
                                                    else Modifier.ha3DBlack(RoundedCornerShape(16.dp))
                                                ), 
                                            shape = RoundedCornerShape(16.dp), 
                                            color = Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(title, fontWeight = FontWeight.Black, color = if (isSel) SoftAmber else HaIvory, fontSize = 14.sp)
                                                    Text(desc, color = HaMuted, fontSize = 11.sp)
                                                }
                                                if (isSel) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = SoftAmber, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                        
                                        if (isSel) {
                                            val example = when (challenge) {
                                                is Challenge.Maths -> {
                                                    when(index) {
                                                        0 -> "e.g. 24 + 18 = ?"
                                                        1 -> "e.g. (14 + 8) × 3 = ?"
                                                        2 -> "e.g. 4x + 12 = 36"
                                                        else -> "e.g. (18 × 12) + 145 - 7 = ?"
                                                    }
                                                }
                                                is Challenge.AdvancedMaths -> {
                                                    when(index) {
                                                        0 -> "e.g. f(x) = 5x - 13, x=4"
                                                        1 -> "e.g. f(x) = 2x² + 5, x=3"
                                                        2 -> "e.g. f(x,y) = 3x + 4y, x=2, y=5"
                                                        else -> "e.g. f(x) = x³ + 2x - 10, x=3"
                                                    }
                                                }
                                                is Challenge.SpellBee -> {
                                                    when(index) {
                                                        0 -> "e.g. APPLE, DREAM"
                                                        1 -> "e.g. JOURNEY, MORNING"
                                                        2 -> "e.g. BEAUTIFUL, CHALLENGE"
                                                        else -> "e.g. CONGRATULATIONS, IMAGINATION"
                                                    }
                                                }
                                                is Challenge.AudioMemory -> {
                                                    // Audio Memory Examples
                                                    when(index) {
                                                        0 -> "Recall 3 spoken words"
                                                        1 -> "Recall 4 spoken words"
                                                        2 -> "Recall 5 spoken words"
                                                        else -> "Recall 6 spoken words"
                                                    }
                                                }
                                                else -> ""
                                            }
                                            Surface(
                                                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().ha3DInset(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                            ) {
                                                Text(
                                                    text = example,
                                                    color = SoftAmber.copy(alpha = 0.7f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        val labelText = when (challenge) {
                            is Challenge.Maths, is Challenge.AdvancedMaths -> "Number of Questions"
                            is Challenge.Tap -> "Target Taps"
                            is Challenge.Charger -> "Plug-in Count"
                            is Challenge.Shake -> "Target Shakes"
                            is Challenge.ClockReading -> "Number of Rounds"
                            is Challenge.LightsOn -> "Target Brightness"
                            is Challenge.Steps -> "Target Steps"
                            is Challenge.Memory -> "Number of Rounds"
                            is Challenge.Maze -> "Number of Rounds"
                            is Challenge.SpellBee -> "Number of Rounds"
                            is Challenge.Puzzle -> "Number of Rounds"
                            is Challenge.ScanSink -> "Scanning Mode"
                            is Challenge.PushUp -> "Target Reps"
                            is Challenge.Squat -> "Target Squats"
                            is Challenge.ScanObject -> "Detection Mode"
                            is Challenge.ReverseTyping, is Challenge.AudioMemory -> "Number of Rounds"
                            is Challenge.BalanceTest -> "Hold Duration (Seconds)"
                            is Challenge.SafeCode, is Challenge.ControlPanel -> "Number of Rounds"
                            else -> "Setting"
                        }
                        Text(labelText, color = HaMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (challenge is Challenge.Tap) {
                            val options = listOf(12, 15, 20, 25, 30, 35, 40, 45, 50, 55)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = selectedValue == 0, 
                                    onClick = { selectedValue = 0 }, 
                                    label = { Text("Random", fontSize = 14.sp, fontWeight = FontWeight.Bold) }, 
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftAmber, selectedLabelColor = MidnightBlue, containerColor = HaBlackRaised, labelColor = HaIvory)
                                )
                                options.forEach { opt -> 
                                    val isSel = selectedValue == opt
                                    FilterChip(
                                        selected = isSel, 
                                        onClick = { selectedValue = opt }, 
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSel) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text("$opt Taps", fontSize = 13.sp, fontWeight = FontWeight.Bold) 
                                            }
                                        }, 
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftAmber, selectedLabelColor = MidnightBlue, containerColor = HaBlackRaised, labelColor = HaIvory)
                                    ) 
                                }
                            }
                        } else if (challenge is Challenge.Charger) {
                            val options = listOf(1, 2)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val isRandom = selectedValue == 0
                                FilterChip(
                                    selected = isRandom, 
                                    onClick = { selectedValue = 0 }, 
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isRandom) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text("Random", fontSize = 14.sp, fontWeight = FontWeight.Bold) 
                                        }
                                    }, 
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftAmber, selectedLabelColor = MidnightBlue, containerColor = HaBlackRaised, labelColor = HaIvory)
                                )
                                options.forEach { opt -> 
                                    val isSel = selectedValue == opt
                                    FilterChip(
                                        selected = isSel, 
                                        onClick = { selectedValue = opt }, 
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSel) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text("$opt Time", fontSize = 14.sp, fontWeight = FontWeight.Bold) 
                                            }
                                        }, 
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftAmber, selectedLabelColor = MidnightBlue, containerColor = HaBlackRaised, labelColor = HaIvory)
                                    ) 
                                }
                            }
                        } else if (challenge is Challenge.ScanObject) {
                            var currentTab by remember { mutableIntStateOf(selectedValue) }
                            
                            val excludedObjects = remember {
                                val list = mutableStateListOf<String>()
                                if (selectedValue == 1 && selectedExtra.isNotEmpty()) {
                                    val included = selectedExtra.split(",")
                                    ObjectCategories.list.forEach { if (it !in included) list.add(it) }
                                }
                                list
                            }
                            
                            var tempSingle by remember { 
                                mutableStateOf(if (selectedValue == 0) selectedExtra else "Coffee Cup") 
                            }

                            val gridState = rememberLazyGridState()
                            val isAtBottom by remember {
                                derivedStateOf {
                                    val layoutInfo = gridState.layoutInfo
                                    val totalItems = layoutInfo.totalItemsCount
                                    if (totalItems == 0) false
                                    else {
                                        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                                        lastVisibleItem != null && lastVisibleItem.index >= totalItems - 1
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                // --- Modern Segmented Button (Pill Style) ---
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(27.dp))
                                        .background(HaBlackRaised)
                                        .border(1.dp, HaIvory.copy(alpha = 0.1f), RoundedCornerShape(27.dp))
                                        .padding(6.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        listOf("Pick One" to 0, "Random" to 1).forEach { (label, index) ->
                                            val isSel = currentTab == index
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(if (isSel) SoftAmber else Color.Transparent)
                                                    .combinedClickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
                                                        onClick = { 
                                                            currentTab = index; selectedValue = index
                                                            if (index == 0) {
                                                                selectedExtra = tempSingle
                                                            } else {
                                                                // Zero Default Exclusions when switching to Random
                                                                excludedObjects.clear()
                                                                selectedExtra = ObjectCategories.list.joinToString(",")
                                                            }
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label, 
                                                    color = if (isSel) MidnightBlue else HaIvory.copy(0.6f), 
                                                    fontWeight = FontWeight.ExtraBold, 
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))

                                // --- Interactive 2-Column Grid ---
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    state = gridState,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    items(ObjectCategories.list) { item ->
                                        val isExcluded = currentTab == 1 && excludedObjects.contains(item)
                                        val isPicked = currentTab == 0 && tempSingle == item
                                        
                                        val itemIcon = when(item) {
                                            "Backpack" -> Icons.Default.Backpack
                                            "Badminton" -> Icons.Default.SportsTennis
                                            "Bed" -> Icons.Default.Bed
                                            "Bicycle" -> Icons.AutoMirrored.Filled.DirectionsBike
                                            "Bottle" -> Icons.Default.LocalDrink
                                            "Cap" -> Icons.Default.Face
                                            "Cat" -> Icons.Default.Pets
                                            "Chair" -> Icons.Default.Chair
                                            "Clock" -> Icons.Default.Schedule
                                            "Coffee Cup" -> Icons.Default.LocalCafe
                                            "Couch" -> Icons.Default.Chair
                                            "Dog" -> Icons.Default.Pets
                                            "Fire" -> Icons.Default.Whatshot
                                            "Flowerpot" -> Icons.Default.LocalFlorist
                                            "Fruit" -> Icons.Default.ShoppingBasket
                                            "Laptop" -> Icons.Default.Laptop
                                            "Lipstick" -> Icons.Default.Brush
                                            "Newspaper" -> Icons.AutoMirrored.Filled.Article
                                            "Phone" -> Icons.Default.Smartphone
                                            "Plant" -> Icons.Default.Eco
                                            "Shoe" -> Icons.AutoMirrored.Filled.DirectionsWalk
                                            "Sink" -> Icons.Default.Kitchen
                                            "Sky" -> Icons.Default.Cloud
                                            "Sunglasses" -> Icons.Default.WbSunny
                                            "Television" -> Icons.Default.Tv
                                            "Tie" -> Icons.Default.Person
                                            "Toy" -> Icons.Default.SmartToy
                                            "Watch" -> Icons.Default.Watch
                                            else -> Icons.Default.ImageSearch
                                        }

                                        Surface(
                                            onClick = {
                                                if (currentTab == 0) {
                                                    tempSingle = item
                                                    selectedExtra = item
                                                } else {
                                                    if (excludedObjects.contains(item)) {
                                                        excludedObjects.remove(item)
                                                    } else {
                                                        if (excludedObjects.size < ObjectCategories.list.size - 1) {
                                                            excludedObjects.add(item)
                                                        }
                                                    }
                                                    selectedExtra = ObjectCategories.list.filter { it !in excludedObjects }.joinToString(",")
                                                }
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isPicked) SoftAmber.copy(alpha = 0.15f) else if (isExcluded) HaCoral.copy(alpha = 0.1f) else HaBlackRaised,
                                            border = BorderStroke(
                                                width = if (isPicked || isExcluded) 2.dp else 1.dp,
                                                color = if (isPicked) SoftAmber else if (isExcluded) HaCoral.copy(0.6f) else HaIvory.copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier.height(64.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = itemIcon,
                                                    contentDescription = null,
                                                    tint = if (isPicked) SoftAmber else if (isExcluded) HaCoral.copy(0.6f) else HaIvory.copy(0.4f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item,
                                                        color = if (isPicked) SoftAmber else HaIvory,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isPicked) FontWeight.ExtraBold else FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                    if (isExcluded) {
                                                        Text(
                                                            text = "Excluded",
                                                            color = HaCoral,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                if (isPicked) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = SoftAmber,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else if (isExcluded) {
                                                    Icon(
                                                        imageVector = Icons.Default.Block,
                                                        contentDescription = null,
                                                        tint = HaCoral,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            val infiniteTransition = rememberInfiniteTransition(label = "scroll")
                            val arrowOffset by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 12f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "y"
                            )
                            
                            Box(
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAtBottom) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = SoftAmber.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .graphicsLayer { translationY = arrowOffset }
                                )
                            }
                        } else if (challenge is Challenge.Maths || challenge is Challenge.AdvancedMaths) {
                            val options = listOf(1, 2, 3, 5, 12, 15)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                options.forEach { opt -> 
                                    val isSel = selectedValue == opt
                                    FilterChip(
                                        selected = isSel, 
                                        onClick = { selectedValue = opt }, 
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSel) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text("$opt ${if (opt == 1) "Question" else "Questions"}", fontSize = 13.sp, fontWeight = FontWeight.Bold) 
                                            }
                                        }, 
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SoftAmber, 
                                            selectedLabelColor = MidnightBlue, 
                                            containerColor = HaBlackRaised, 
                                            labelColor = HaIvory
                                        )
                                    ) 
                                }
                            }
                        } else if (challenge is Challenge.QRCode) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("Any QR" to 0, "Specific QR" to 1).forEach { (label, mode) -> 
                                        val isSel = selectedValue == mode
                                        FilterChip(
                                            selected = isSel, 
                                            onClick = { 
                                                selectedValue = mode
                                                if (mode == 0) {
                                                    selectedExtra = ""
                                                }
                                            }, 
                                            label = { 
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSel) {
                                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }, 
                                            modifier = Modifier.weight(1f).height(48.dp), 
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftAmber, selectedLabelColor = MidnightBlue, containerColor = HaBlackRaised, labelColor = HaIvory)
                                        ) 
                                    }
                                }
                                if (selectedValue == 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    var showQRScanner by remember { mutableStateOf(false) }
                                    val qrList = remember(selectedExtra) { 
                                        if (selectedExtra.isEmpty()) mutableStateListOf() 
                                        else selectedExtra.split("|").toMutableStateList() 
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        qrList.forEach { qrData ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                color = HaJade.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, HaJade.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Default.Link, null, tint = HaJade, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = qrData, 
                                                            color = HaIvory.copy(alpha = 0.7f), 
                                                            fontSize = 12.sp, 
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(onClick = { 
                                                        val newList = qrList.filter { it != qrData }
                                                        selectedExtra = newList.joinToString("|")
                                                    }) {
                                                        Icon(Icons.Default.Close, null, tint = HaCoral.copy(0.6f), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }

                                        if (qrList.size < 5) {
                                            Button(
                                                onClick = { showQRScanner = true }, 
                                                modifier = Modifier.fillMaxWidth().height(52.dp), 
                                                colors = ButtonDefaults.buttonColors(containerColor = HaBlackRaised), 
                                                shape = RoundedCornerShape(16.dp), 
                                                border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
                                            ) {
                                                Icon(Icons.Default.QrCodeScanner, null, tint = SoftAmber)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Add QR (${qrList.size}/5)", color = HaIvory, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    
                                    if (showQRScanner) {
                                        Dialog(onDismissRequest = { showQRScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                                            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                                CameraChallenge(onSuccess = { res ->
                                                    @Suppress("UNCHECKED_CAST")
                                                    val barcodes = res as? List<Barcode> ?: return@CameraChallenge
                                                    if (barcodes.isNotEmpty()) {
                                                        val value = barcodes.first().rawValue
                                                        if (value != null && !qrList.contains(value)) {
                                                            val newList = qrList + value
                                                            selectedExtra = newList.joinToString("|")
                                                            showQRScanner = false
                                                        }
                                                    }
                                                }, mode = "barcode")
                                                
                                                IconButton(onClick = { showQRScanner = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = HaIvory)
                                                }
                                                Text("SCAN QR TO ADD", color = HaIvory, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center).padding(top = 300.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (challenge is Challenge.Rewrite) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("Random Text" to 0, "Custom Text" to 1).forEach { (label, mode) -> 
                                        val isSel = selectedValue == mode
                                        FilterChip(
                                            selected = isSel, 
                                            onClick = { selectedValue = mode }, 
                                            label = { 
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSel) {
                                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }, 
                                            modifier = Modifier.weight(1f).height(48.dp), 
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftAmber, selectedLabelColor = MidnightBlue, containerColor = HaBlackRaised, labelColor = HaIvory)
                                        ) 
                                    }
                                }
                                
                                if (selectedValue == 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = selectedExtra,
                                        onValueChange = { selectedExtra = it },
                                        label = { Text("Your Custom Phrase", color = HaMuted) },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Type something inspiring...", color = HaMuted.copy(0.4f)) },
                                        textStyle = LocalTextStyle.current.copy(color = HaIvory),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SoftAmber,
                                            unfocusedBorderColor = HaIvory.copy(alpha = 0.1f)
                                        )
                                    )
                                    Text(
                                        "Max 100 characters. Keep it readable.", 
                                        color = HaMuted, 
                                        fontSize = 11.sp, 
                                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                                    )
                                }
                            }
                        } else if (challenge is Challenge.Shake) {
                            val options = listOf(15, 30, 50, 75, 100, 120)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                options.forEach { opt -> 
                                    val isSel = selectedValue == opt
                                    FilterChip(
                                        selected = isSel, 
                                        onClick = { selectedValue = opt }, 
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSel) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text("$opt Shakes", fontSize = 13.sp, fontWeight = FontWeight.Bold) 
                                            }
                                        }, 
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SoftAmber, 
                                            selectedLabelColor = MidnightBlue, 
                                            containerColor = HaBlackRaised, 
                                            labelColor = HaIvory
                                        )
                                    ) 
                                }
                            }
                        } else if (challenge is Challenge.ClockReading || challenge is Challenge.Maze || challenge is Challenge.Memory || challenge is Challenge.SpellBee || challenge is Challenge.Puzzle || challenge is Challenge.ReverseTyping || challenge is Challenge.AudioMemory || challenge is Challenge.BalanceTest || challenge is Challenge.SafeCode || challenge is Challenge.ControlPanel) {
                            val rounds = when (challenge) {
                                is Challenge.ClockReading -> listOf(1, 2, 3, 4, 5, 6)
                                is Challenge.Maze, is Challenge.SpellBee -> listOf(1, 2, 3, 4)
                                is Challenge.ReverseTyping -> listOf(1, 2, 3, 5, 7, 10)
                                is Challenge.AudioMemory -> listOf(1, 2, 3, 5)
                                is Challenge.BalanceTest -> listOf(15, 20, 30, 45, 60)
                                is Challenge.SafeCode, is Challenge.ControlPanel -> listOf(1, 2, 3, 4, 5, 6)
                                else -> listOf(1, 2, 3, 4, 5)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rounds.forEach { r -> 
                                    val isSel = selectedValue == r
                                    FilterChip(
                                        selected = isSel, 
                                        onClick = { selectedValue = r }, 
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSel) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                val displayText = if (challenge is Challenge.BalanceTest) "$r Seconds" else if (challenge is Challenge.SafeCode || challenge is Challenge.ControlPanel) "$r Round${if (r > 1) "s" else ""}" else "$r Rounds"
                                                Text(displayText, fontSize = 13.sp, fontWeight = FontWeight.Bold) 
                                            }
                                        }, 
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SoftAmber, 
                                            selectedLabelColor = MidnightBlue, 
                                            containerColor = HaBlackRaised, 
                                            labelColor = HaIvory
                                        )
                                    ) 
                                }
                            }
                        } else if (challenge is Challenge.LightsOn) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(HaBlackRaised, RoundedCornerShape(16.dp))
                                        .border(1.dp, HaIvory.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$selectedValue",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SoftAmber
                                        )
                                        Text(
                                            text = "LUX (BRIGHTNESS)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HaMuted,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Slider(
                                    value = selectedValue.toFloat(),
                                    onValueChange = { selectedValue = it.toInt() },
                                    valueRange = 10f..1000f,
                                    steps = 0,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SoftAmber,
                                        activeTrackColor = SoftAmber,
                                        inactiveTrackColor = HaIvory.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Dim (10)", color = HaMuted, fontSize = 11.sp)
                                    Text("Bright (1000)", color = HaMuted, fontSize = 11.sp)
                                }
                            }
                        } else if (challenge is Challenge.Steps) {
                            val options = listOf(10, 20, 30, 50, 100)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                options.forEach { opt -> 
                                    val isSel = selectedValue == opt
                                    FilterChip(
                                        selected = isSel, 
                                        onClick = { selectedValue = opt }, 
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSel) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text("$opt Steps", fontSize = 13.sp, fontWeight = FontWeight.Bold) 
                                            }
                                        }, 
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SoftAmber, 
                                            selectedLabelColor = MidnightBlue, 
                                            containerColor = HaBlackRaised, 
                                            labelColor = HaIvory
                                        )
                                    ) 
                                }
                            }
                        } else if (challenge is Challenge.PushUp || challenge is Challenge.Squat) {
                            val options = listOf(5, 10, 15, 20, 30, 50, 100)
                            Text("Target Reps", color = HaMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(110.dp) // Fixed height for 2 rows
                            ) {
                                items(options) { opt ->
                                    val isSel = selectedValue == opt
                                    Surface(
                                        onClick = { selectedValue = opt },
                                        modifier = Modifier.height(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSel) HaGoldSelectedFill else HaBlackRaised,
                                        border = BorderStroke(1.dp, if (isSel) SoftAmber else HaIvory.copy(alpha = 0.1f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isSel) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SoftAmber,
                                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(12.dp)
                                                )
                                            }
                                            Text(
                                                text = opt.toString(),
                                                color = if (isSel) SoftAmber else HaIvory,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (challenge is Challenge.Squat) {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text("Detection Mode", color = HaMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val currentMode = if (selectedExtra == "motion") 1 else 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    listOf(
                                        Triple("Camera", "AI Pose Tracking", 0),
                                        Triple("Motion", "Hold device in hands", 1)
                                    ).forEach { (label, desc, mode) ->
                                        val isSel = currentMode == mode
                                        Surface(
                                            onClick = { selectedExtra = if (mode == 1) "motion" else "camera" },
                                            modifier = Modifier.weight(1f).height(74.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (isSel) HaGoldSelectedFill else HaBlackRaised,
                                            border = BorderStroke(1.5.dp, if (isSel) SoftAmber else HaIvory.copy(alpha = 0.1f))
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                if (isSel) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = SoftAmber,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .size(16.dp)
                                                    )
                                                }
                                                Column(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = label, 
                                                        color = if (isSel) SoftAmber else HaIvory, 
                                                        fontWeight = FontWeight.Black, 
                                                        fontSize = 15.sp
                                                    )
                                                    Text(
                                                        text = desc, 
                                                        color = if (isSel) SoftAmber.copy(alpha = 0.6f) else HaMuted, 
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (challenge is Challenge.ScanSink) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("Any Sink" to 0, "Specific Sink" to 1).forEach { (label, mode) -> 
                                        val isSel = selectedValue == mode
                                        FilterChip(
                                            selected = isSel, 
                                            onClick = { selectedValue = mode }, 
                                            label = { 
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSel) {
                                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }, 
                                            modifier = Modifier.weight(1f).height(48.dp), 
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftAmber, selectedLabelColor = MidnightBlue, containerColor = HaBlackRaised, labelColor = HaIvory)
                                        ) 
                                    }
                                }
                                if (selectedValue == 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (selectedExtra.isEmpty()) {
                                        Button(
                                            onClick = { cameraLauncher.launch(null) }, 
                                            modifier = Modifier.fillMaxWidth().height(52.dp), 
                                            colors = ButtonDefaults.buttonColors(containerColor = HaBlackRaised), 
                                            shape = RoundedCornerShape(16.dp), 
                                            border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
                                        ) {
                                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = SoftAmber)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Register Specific Sink", color = HaIvory, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().height(52.dp),
                                            color = HaJade.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, HaJade.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = HaJade, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text("Sink Registered", color = HaIvory, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                IconButton(onClick = { 
                                                    selectedExtra = ""
                                                    selectedValue = 0 // Auto-revert to "Any Sink" mode on deletion
                                                    onConfirm(0, selectedDifficulty, "") // Forced immediate persistence
                                                }) {
                                                    Icon(Icons.Default.Delete, "Remove", tint = HaCoral.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Slider(value = selectedValue.toFloat(), onValueChange = { selectedValue = it.toInt() }, valueRange = 1f..50f, steps = 49, colors = SliderDefaults.colors(thumbColor = SoftAmber, activeTrackColor = SoftAmber))
                            Text(selectedValue.toString(), color = SoftAmber, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }

                    if (challenge !is Challenge.ScanObject) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Button(
                        onClick = { onPreview(challenge, selectedValue, selectedDifficulty, selectedExtra) },
                        modifier = Modifier.fillMaxWidth().height(52.dp).ha3DBlack(RoundedCornerShape(16.dp), elevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview Alarm", color = HaIvory, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HaGoldButton(text = "Done", onClick = { onConfirm(selectedValue, selectedDifficulty, selectedExtra) })
                }
            }
        }
    }
}

@Composable
fun EditOptionItem(title: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).ha3DPanel(RoundedCornerShape(20.dp), elevation = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = HaGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = HaIvory, fontSize = 16.sp)
                Text(value, fontSize = 12.sp, color = SoftAmber, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = HaMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun EditToggleItem(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).ha3DPanel(RoundedCornerShape(20.dp), elevation = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = HaGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = HaIvory, fontSize = 16.sp)
                Text(subtitle, fontSize = 11.sp, color = HaMuted)
            }
            HaSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun EditTextFieldItem(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = HaMuted) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .ha3DInset(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftAmber,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = HaIvory,
            unfocusedTextColor = HaIvory
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChallengeItem(
    challenge: Challenge,
    isSelected: Boolean,
    isPremium: Boolean,
    isTapped: Boolean = false,
    subtitle: String,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isTapped) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Animation for Locked Badge
    val infiniteTransition = rememberInfiniteTransition(label = "lock_badge")
    val lockPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val lockPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Premium Color Logic (Purple/Navy tones)
    val cardBackground = if (isPremium) {
        Brush.verticalGradient(
            listOf(HaSurfaceLow.copy(alpha = 0.6f), HaObsidian.copy(alpha = 0.9f))
        )
    } else {
        val baseColor = if (isSelected) HaGoldMid.copy(alpha = 0.12f) else HaIvory.copy(alpha = 0.05f)
        Brush.linearGradient(listOf(baseColor, baseColor))
    }

    val borderColor = when {
        isSelected -> SoftAmber
        isTapped -> SoftAmber
        isPremium -> HaGoldMid.copy(alpha = 0.4f) // Purple tint for locked
        else -> HaIvory.copy(alpha = 0.1f)
    }

    val accentColor = if (isSelected || isTapped) SoftAmber else HaIvory
    val contentAlpha = if (isPremium) 0.85f else 1f

    Box(
        modifier = Modifier
            .aspectRatio(0.95f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isSelected || isTapped) Modifier.ha3DPanelSelected(RoundedCornerShape(20.dp))
                else Modifier.ha3DPanel(RoundedCornerShape(20.dp), gold = isPremium, elevation = 8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // --- Challenge Content (Always Sharp) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Curated jewel-tone set: every tone shares the same depth so
                // the grid reads as one family next to the champagne gold.
                val goldTone = listOf(HaGoldPale, HaGold, HaGoldDeep)
                val roseTone = listOf(Color(0xFFF7DCD6), HaRose, Color(0xFF9A5A52))
                val jadeTone = listOf(HaJadeLight, HaJade, HaJadeDeep)
                val platinumTone = listOf(Color(0xFFF6F7FA), HaPlatinum, Color(0xFF7C808C))
                val steelTone = listOf(Color(0xFFD3EAF6), HaSky, Color(0xFF3C6E8A))
                val iconColorPalette = when (challenge.id) {
                    "maths", "adv_maths" -> goldTone
                    "qr_code", "rev_typing" -> steelTone
                    "rewrite", "tap" -> roseTone
                    "charger", "lights" -> goldTone
                    "clock", "sink" -> steelTone
                    "steps", "pushup", "squat" -> jadeTone
                    "maze", "puzzle", "audio" -> platinumTone
                    "memory", "shake", "spell" -> roseTone
                    "object" -> platinumTone
                    "balance_test" -> goldTone
                    "safe_code" -> jadeTone
                    "control_panel" -> steelTone
                    else -> goldTone
                }

                // raised 3D orb that holds the icon
                if (challenge.id != "pushup" && challenge.id != "squat") {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .ha3DOrb()
                    )
                }
                when (challenge.id) {
                    "pushup", "squat" -> Box(
                        // a small recessed 3D stage instead of the round orb
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(50.dp)
                            .ha3DInset(RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedExerciseIcon(
                            kind = if (challenge.id == "pushup") ExerciseKind.PushUp else ExerciseKind.Squat,
                            colors = AthleteGold,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                            showFloor = true
                        )
                    }
                    else -> Icon(
                    imageVector = challenge.icon,
                    contentDescription = challenge.name,
                    tint = HaIvory,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(alpha = 0.99f)
                        .drawWithCache {
                            val radialBrush = Brush.radialGradient(
                                colors = iconColorPalette,
                                center = Offset(size.width * 0.3f, size.height * 0.3f),
                                radius = size.width * 0.85f
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = radialBrush, blendMode = BlendMode.SrcIn)
                            }
                        }
                )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = challenge.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isPremium) HaIvory.copy(alpha = 0.9f) else accentColor, // High readability
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            AutoResizeText(
                text = subtitle,
                color = if (isSelected) SoftAmber.copy(alpha = 0.7f) else HaMuted.copy(alpha = contentAlpha),
                fontWeight = FontWeight.Normal,
                initialFontSize = 9.sp,
                minFontSize = 6.sp,
                textAlign = TextAlign.Center
            )
        }

        // --- Premium Visual Enhancements ---
        if (isPremium) {
            // Subtle gold highlight line at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(32.dp)
                    .height(2.dp)
                    .padding(bottom = 1.dp)
                    .clip(CircleShape)
                    .background(SoftAmber.copy(alpha = 0.3f))
            )

            // High-End Lock Icon Badge (Top Right) responsive to card width
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                val cardWidthDp = maxWidth
                val badgeSize = (cardWidthDp * 0.22f).coerceIn(22.dp, 30.dp)
                val iconSize = (badgeSize * 0.5f).coerceIn(11.dp, 15.dp)
                val cornerRadius = badgeSize * 0.35f

                Surface(
                    modifier = Modifier
                        .size(badgeSize)
                        .graphicsLayer {
                            scaleX = lockPulseScale
                            scaleY = lockPulseScale
                        }
                        .ha3DCoin(HaTone.Gold, elevation = 4.dp),
                    color = Color.Transparent,
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = haToneInk(HaTone.Gold),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }
}

private fun getDifficultyName(difficulty: Int): String = when(difficulty) {
    0 -> "Easy"
    1 -> "Medium"
    2 -> "Hard"
    else -> "Extreme"
}

object ScrollSoundEffect {
    private const val SAMPLE_RATE = 16000
    private const val DURATION_MS = 12
    private const val NUM_SAMPLES = (SAMPLE_RATE * DURATION_MS) / 1000
    private val generatedSnd = ByteArray(NUM_SAMPLES * 2)

    init {
        val freq = 1800.0
        for (i in 0 until NUM_SAMPLES) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 300.0)
            val valSample = sin(2.0 * Math.PI * freq * t) * envelope
            val sampleVal = (valSample * 32767.0).toInt().coerceIn(-32768, 32767)

            generatedSnd[2 * i] = (sampleVal and 0xff).toByte()
            generatedSnd[2 * i + 1] = ((sampleVal shr 8) and 0xff).toByte()
        }
    }

    fun play(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).toFloat()
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM).toFloat()
            val ratio = if (maxVol > 0f) curVol / maxVol else 0.7f
            val finalVolume = (ratio * 0.7f).coerceIn(0.1f, 1.0f)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(generatedSnd, 0, generatedSnd.size)
            track.setVolume(finalVolume)
            track.play()
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    content: InfoSheetContent,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HaPanelDeep,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .ha3DInset(CircleShape)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .ha3DCoin(HaTone.Gold, elevation = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = haToneInk(HaTone.Gold),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = content.title,
                color = HaIvory,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content.description,
                color = HaIvory.copy(alpha = 0.6f),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            if (content.note != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.ha3DPanel(RoundedCornerShape(12.dp), gold = true, elevation = 6.dp),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = content.note,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = SoftAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            HaGoldButton(text = "Got it", onClick = onDismiss)
        }
    }
}

private data class WakeStep(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val tone: HaTone,
    val motion: Int // 0 = ring/wobble, 1 = pop, 2 = countdown, 3 = drop-in
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeUpTutorialBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    // Colour follows meaning: gold = alarm/notification, jade = success, steel = waiting, coral = missed
    val steps = remember {
        listOf(
            WakeStep(Icons.Default.NotificationsActive, "Alarm Rings", "Your set alarm goes off.", HaTone.Gold, 0),
            WakeStep(Icons.Default.CheckCircle, "You dismiss it", "You complete your wake challenge.", HaTone.Jade, 1),
            WakeStep(Icons.Default.Timer, "Delay minutes pass...", "The verification timer starts counting down.", HaTone.Steel, 2),
            WakeStep(Icons.Default.Notifications, "Notification: Tap to confirm!", "A follow-up check appears at the top.", HaTone.Gold, 3),
            WakeStep(Icons.Default.VerifiedUser, "Tap within 5 min -- Confirmed!", "You're verified awake. Well done!", HaTone.Jade, 1),
            WakeStep(Icons.Default.PriorityHigh, "Didn't tap? Alarm rings again!", "Missed the check? Time to wake up (again)!", HaTone.Coral, 0)
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2800.milliseconds)
            currentStep = (currentStep + 1) % steps.size
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HaPanelDeep,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .ha3DInset(CircleShape)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Wake Up Check",
                color = HaIvory,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "After dismissing the alarm, tap the follow-up notification. If you don't respond within 5 minutes, the alarm rings again!",
                color = HaIvory.copy(alpha = 0.6f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Recessed 3D stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .ha3DInset(RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(450, easing = LinearOutSlowInEasing)) +
                         slideInVertically { height -> height / 3 })
                        .togetherWith(fadeOut(animationSpec = tween(300)) +
                                      slideOutVertically { height -> -height / 3 })
                    },
                    label = "tutorial_step"
                ) { stepIdx ->
                    val step = steps[stepIdx]
                    val motion = rememberInfiniteTransition(label = "step_motion")
                    val wobble by motion.animateFloat(
                        initialValue = -14f, targetValue = 14f,
                        animationSpec = infiniteRepeatable(tween(110), RepeatMode.Reverse),
                        label = "wobble"
                    )
                    val pulse by motion.animateFloat(
                        initialValue = 1f, targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "pulse"
                    )
                    val countdown by motion.animateFloat(
                        initialValue = 1f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart),
                        label = "countdown"
                    )
                    val drop by motion.animateFloat(
                        initialValue = 0f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "drop"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (step.motion == 2) {
                                // countdown ring cut into the stage around the coin
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(96.dp)) {
                                    val sw = 5.dp.toPx()
                                    drawArc(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                        topLeft = Offset(sw / 2, sw / 2),
                                        size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(sw)
                                    )
                                    drawArc(
                                        brush = Brush.verticalGradient(listOf(HaGoldPale, HaGold, HaGoldDeep)),
                                        startAngle = -90f, sweepAngle = 360f * countdown, useCenter = false,
                                        topLeft = Offset(sw / 2, sw / 2),
                                        size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .graphicsLayer {
                                        when (step.motion) {
                                            0 -> rotationZ = wobble
                                            1 -> { scaleX = pulse; scaleY = pulse }
                                            3 -> translationY = -10.dp.toPx() * (1f - drop)
                                        }
                                    }
                                    .ha3DCoin(step.tone, elevation = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(step.icon, null, tint = haToneInk(step.tone), modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(step.title, color = HaIvory, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Text(step.desc, color = HaMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 3D step track: finished steps are lit in their own colour, the
            // current step is a raised coin, upcoming steps sit recessed.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(6.dp)
                        .ha3DInset(CircleShape)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, step ->
                        val isCurrent = index == currentStep
                        val isDone = index < currentStep
                        val nodeSize by animateDpAsState(if (isCurrent) 26.dp else 16.dp, label = "node_size")
                        Box(
                            modifier = Modifier
                                .size(nodeSize)
                                .then(
                                    when {
                                        isCurrent || isDone -> Modifier.ha3DCoin(step.tone, elevation = if (isCurrent) 8.dp else 3.dp)
                                        else -> Modifier.ha3DInset(CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                Icon(step.icon, null, tint = haToneInk(step.tone), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            HaGoldButton(text = "Got it", onClick = onDismiss)
        }
    }
}

@Composable
fun AutoResizeText(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    initialFontSize: TextUnit,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 6.sp,
    maxLines: Int = 1,
    textAlign: TextAlign? = null
) {
    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth
        val approxCharWidthDp = 5.5f
        val estimatedTextWidthDp = text.length * approxCharWidthDp
        val calculatedFontSize = if (estimatedTextWidthDp > widthDp.value && widthDp.value > 0f) {
            val scale = (widthDp.value / estimatedTextWidthDp).coerceIn(minFontSize.value / initialFontSize.value, 1f)
            (initialFontSize.value * scale).coerceAtLeast(minFontSize.value).sp
        } else {
            initialFontSize
        }

        Text(
            text = text,
            color = color,
            fontWeight = fontWeight,
            fontSize = calculatedFontSize,
            maxLines = maxLines,
            softWrap = false,
            textAlign = textAlign
        )
    }
}
