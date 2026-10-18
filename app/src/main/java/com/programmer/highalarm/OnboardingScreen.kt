package com.programmer.highalarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlin.time.Duration.Companion.milliseconds
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Calendar
import java.util.Locale
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    viewModel: AlarmViewModel,
    onCompleteOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentStep by remember { mutableIntStateOf(1) }
    var hasPlayedStep1Anim by remember { mutableStateOf(false) }

    // Step 2 State (Time & Days) - Initialized from device current local time
    val currentCal = remember { Calendar.getInstance() }
    val currentHour24 = currentCal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCal.get(Calendar.MINUTE)

    val initialAmPm = if (currentHour24 >= 12) "PM" else "AM"
    val initialHour12 = when {
        currentHour24 == 0 -> 12
        currentHour24 > 12 -> currentHour24 - 12
        else -> currentHour24
    }

    var selectedHour by remember { mutableIntStateOf(initialHour12) }
    var selectedMinute by remember { mutableIntStateOf(currentMinute) }
    var selectedAmPm by remember { mutableStateOf(initialAmPm) }
    var selectedDays by remember { mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6)) } // Default: Every Day
    var showDaysDialog by remember { mutableStateOf(false) }

    // Step 3 State (Challenge)
    var selectedChallengeId by remember { mutableStateOf("maths") } // Default: Maths

    // Step 4 Permission States
    var isBatteryOptIgnored by remember { mutableStateOf(PermissionUtils.isBatteryOptimizationIgnored(context)) }
    var areNotificationsAllowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var canDrawOverlays by remember { mutableStateOf(PermissionUtils.canDrawOverlays(context)) }

    // Notification Permission Launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        areNotificationsAllowed = isGranted || NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // Camera Permission Launcher for QR Code Challenge (Contextual)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            selectedChallengeId = "qr_code"
        } else {
            Toast.makeText(context, "Camera access is required for QR Code challenges.", Toast.LENGTH_SHORT).show()
        }
    }

    // Refresh permissions on activity resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptIgnored = PermissionUtils.isBatteryOptimizationIgnored(context)
                areNotificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
                canDrawOverlays = PermissionUtils.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Prepare ViewModel edit session on initial launch
    LaunchedEffect(Unit) {
        viewModel.prepareEditSession(context, null)
    }

    // Step 4 Sequential Permission Flow
    LaunchedEffect(currentStep, isBatteryOptIgnored, areNotificationsAllowed, canDrawOverlays) {
        if (currentStep == 4) {
            delay(600.milliseconds)
            if (!isBatteryOptIgnored) {
                PermissionUtils.requestIgnoreBatteryOptimization(context)
            } else if (!areNotificationsAllowed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PermissionUtils.openNotificationSettings(context)
                }
            } else if (!canDrawOverlays) {
                PermissionUtils.requestOverlayPermission(context)
            }
        }
    }

    val formattedTimeStr = remember(selectedHour, selectedMinute, selectedAmPm) {
        String.format(Locale.getDefault(), "%02d:%02d %s", selectedHour, selectedMinute, selectedAmPm)
    }

    val repeatText = remember(selectedDays) {
        when {
            selectedDays.size == 7 -> "Every Day"
            selectedDays.isEmpty() -> "Once"
            selectedDays.size == 5 && selectedDays.containsAll(listOf(1, 2, 3, 4, 5)) -> "Weekdays"
            selectedDays.size == 2 && selectedDays.containsAll(listOf(0, 6)) -> "Weekends"
            else -> {
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                selectedDays.sorted().joinToString(", ") { dayNames[it] }
            }
        }
    }

    val isAllPermissionsGranted = isBatteryOptIgnored && areNotificationsAllowed && canDrawOverlays

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HaObsidian,
                        HaSurfaceLow,
                        HaInk
                    )
                )
            )
    ) {
        // Subtle radial atmospheric lighting behind content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HaSurfaceHigh.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        radius = 1100f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header Progress Area
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = 5
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Step Content Area with Smooth Animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "OnboardingStepTransition"
                ) { step ->
                    when (step) {
                        1 -> Step1IntroContent(hasPlayedAnim = hasPlayedStep1Anim, onPlayAnim = { hasPlayedStep1Anim = true })
                        2 -> Step2TimePickerContent(
                            hour = selectedHour,
                            minute = selectedMinute,
                            amPm = selectedAmPm,
                            repeatText = repeatText,
                            onHourChange = { selectedHour = it },
                            onMinuteChange = { selectedMinute = it },
                            onAmPmChange = { selectedAmPm = it },
                            onEditRepeat = { showDaysDialog = true }
                        )
                        3 -> Step3ChallengeContent(
                            selectedChallengeId = selectedChallengeId,
                            onSelectChallenge = { id ->
                                if (id == "qr_code") {
                                    val hasCamera = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCamera) {
                                        selectedChallengeId = "qr_code"
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                } else {
                                    selectedChallengeId = id
                                }
                            }
                        )
                        4 -> Step4PermissionsContent(
                            isBatteryOptIgnored = isBatteryOptIgnored,
                            areNotificationsAllowed = areNotificationsAllowed,
                            canDrawOverlays = canDrawOverlays,
                            onRequestBattery = { PermissionUtils.requestIgnoreBatteryOptimization(context) },
                            onRequestNotifications = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    PermissionUtils.openNotificationSettings(context)
                                }
                            },
                            onRequestOverlay = { PermissionUtils.requestOverlayPermission(context) }
                        )
                        5 -> Step5SummaryContent(
                            timeText = formattedTimeStr,
                            repeatText = repeatText
                        )
                    }
                }
            }

            // Fixed Bottom Navigation Area
            Spacer(modifier = Modifier.height(16.dp))

            OnboardingBottomNav(
                currentStep = currentStep,
                isContinueEnabled = isAllPermissionsGranted,
                onBack = {
                    if (currentStep > 1) {
                        currentStep--
                    }
                },
                onContinue = {
                    if (currentStep == 4 && !isAllPermissionsGranted) {
                        return@OnboardingBottomNav
                    }
                    if (currentStep < 5) {
                        currentStep++
                    } else {
                        // Complete Onboarding:
                        // 1. Sync time, repeat days, and challenge to ViewModel
                        viewModel.selectTime(formattedTimeStr)
                        viewModel.setRepeatDays(selectedDays)
                        val config = viewModel.getChallengeConfig(selectedChallengeId)
                        viewModel.setSelectedChallenges(listOf(config))
                        viewModel.saveAlarm(context)

                        // 2. Persist completion flag
                        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                            .edit {
                                putBoolean("is_onboarding_completed", true)
                            }

                        onCompleteOnboarding()
                    }
                }
            )
        }
    }

    if (showDaysDialog) {
        RepeatSelectionModal(
            selectedDays = selectedDays,
            onDismiss = { showDaysDialog = false },
            onConfirm = { newDays ->
                selectedDays = newDays
                showDaysDialog = false
            }
        )
    }
}

@Composable
fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Step $currentStep/$totalSteps",
            color = HaGoldPale,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSteps) {
                val isCurrent = i == currentStep
                val isPast = i < currentStep
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (isCurrent) 28.dp else if (isPast) 16.dp else 12.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent || isPast) HaGoldPale else HaIvory.copy(alpha = 0.18f))
                )
            }
        }
    }
}

@Composable
fun Step1IntroContent(
    hasPlayedAnim: Boolean,
    onPlayAnim: () -> Unit
) {
    val animProgress = remember { Animatable(if (hasPlayedAnim) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!hasPlayedAnim) {
            delay(150.milliseconds) // Subtle initial pause
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
            )
            onPlayAnim()
        }
    }

    val progress = animProgress.value
    val cardAlpha = progress.coerceIn(0f, 1f)
    val cardScale = 0.95f + (progress * 0.05f)
    val cardOffsetY = (1f - progress) * 35f

    val leftAlpha = ((progress - 0.35f) / 0.35f).coerceIn(0f, 1f)
    val leftOffsetY = (1f - leftAlpha) * 16f

    val rightAlpha = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val rightOffsetY = (1f - rightAlpha) * 16f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Hero Headline (Static)
        Text(
            text = "One alarm. Zero excuses.",
            color = HaIvory,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 36.sp,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Hero Subtitle (Static)
        Text(
            text = "Lock in your morning before your brain starts bargaining.",
            color = HaIvory,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Unified Main Frosted Glass Card with opening liquid reveal animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = cardAlpha
                    scaleX = cardScale
                    scaleY = cardScale
                    translationY = cardOffsetY
                }
                .clip(RoundedCornerShape(28.dp))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = HaInk.copy(alpha = 0.78f),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            HaGold.copy(alpha = 0.35f),
                            HaHairline.copy(alpha = 0.4f)
                        )
                    )
                ),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Card Intro Text
                    Text(
                        text = "Easy alarms are easy to cheat. This alarm locks in your morning.",
                        color = HaIvory,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Unified Comparison Flow Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        // LEFT COLUMN: OLD PATTERN
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    alpha = leftAlpha
                                    translationY = leftOffsetY
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = HaGoldDeep.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, HaGoldDeep.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "OLD PATTERN",
                                    color = HaGoldMid,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            FlowCircularStep(
                                label = "Alarm",
                                icon = Icons.Default.AccessTime,
                                iconTint = HaIvory,
                                labelColor = HaIvory
                            )
                            FlowConnectorLine(
                                startColor = HaIvory.copy(alpha = 0.20f),
                                endColor = HaGoldDeep.copy(alpha = 0.35f)
                            )
                            FlowCircularStep(
                                label = "Snooze",
                                icon = Icons.Default.Bedtime,
                                iconTint = HaGoldMid,
                                labelColor = HaGoldMid
                            )
                            FlowConnectorLine(
                                startColor = HaGoldDeep.copy(alpha = 0.35f),
                                endColor = HaCoral.copy(alpha = 0.35f)
                            )
                            FlowCircularStep(
                                label = "Late",
                                icon = Icons.Default.Warning,
                                iconTint = HaCoral,
                                labelColor = HaCoral
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Snooze spiral",
                                color = HaMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = HaGoldDeep.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, HaGoldDeep.copy(alpha = 0.22f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = HaGoldMid,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Avg +24 min lost",
                                        color = HaGoldMid,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Delicate Vertical Divider Line
                        Box(
                            modifier = Modifier
                                .height(240.dp)
                                .width(1.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            HaIvory.copy(alpha = 0.0f),
                                            HaIvory.copy(alpha = 0.08f),
                                            HaIvory.copy(alpha = 0.0f)
                                        )
                                    )
                                )
                        )

                        // RIGHT COLUMN: SOLVE TO WAKE
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    alpha = rightAlpha
                                    translationY = rightOffsetY
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = HaJade.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, HaJade.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "SOLVE TO WAKE",
                                    color = HaJade,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            FlowCircularStep(
                                label = "Alarm",
                                icon = Icons.Default.AccessTime,
                                iconTint = HaIvory,
                                labelColor = HaIvory
                            )
                            FlowConnectorLine(
                                startColor = HaIvory.copy(alpha = 0.20f),
                                endColor = HaJade.copy(alpha = 0.35f)
                            )
                            FlowCircularStep(
                                label = "Solve",
                                icon = Icons.Default.Calculate,
                                iconTint = HaGoldPale,
                                labelColor = HaGoldPale
                            )
                            FlowConnectorLine(
                                startColor = HaJade.copy(alpha = 0.35f),
                                endColor = HaJade.copy(alpha = 0.35f)
                            )
                            FlowCircularStep(
                                label = "Up",
                                icon = Icons.Default.Check,
                                iconTint = HaJade,
                                labelColor = HaJade
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Wake flow",
                                color = HaJade,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = HaJade.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, HaJade.copy(alpha = 0.22f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = HaJade,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Start with a win",
                                        color = HaJade,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Liquid curved reveal mask overlay while progress < 1f
            if (progress < 1f) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = (1f - progress) * 1.5f }
                ) {
                    val w = size.width
                    val h = size.height
                    val revealY = h * progress

                    val revealPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, revealY)
                        val steps = 8
                        val stepW = w / steps
                        for (i in 0..steps) {
                            val x = i * stepW
                            val curve = (sin((x / w) * Math.PI + (progress * Math.PI * 3))).toFloat() * 16f
                            lineTo(x, revealY + curve)
                        }
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = revealPath,
                        color = HaInk
                    )

                    drawLine(
                        color = HaGold.copy(alpha = 0.85f * (1f - progress)),
                        start = Offset(0f, revealY),
                        end = Offset(w, revealY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun FlowCircularStep(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    labelColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(HaIvory.copy(alpha = 0.08f))
                .border(1.2.dp, HaIvory.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FlowConnectorLine(
    startColor: Color,
    endColor: Color
) {
    Box(
        modifier = Modifier
            .height(14.dp)
            .width(1.5.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(startColor, endColor)
                )
            )
    )
}

@Composable
fun Step2TimePickerContent(
    hour: Int,
    minute: Int,
    amPm: String,
    repeatText: String,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onAmPmChange: (String) -> Unit,
    onEditRepeat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What time do you want to wake up?",
            color = HaIvory,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Large Frosted Dark Glass Time Picker Card (Strictly Clipped & Taller)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = HaInk.copy(alpha = 0.78f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        HaGoldPale.copy(alpha = 0.18f),
                        HaIvory.copy(alpha = 0.04f)
                    )
                )
            ),
            shadowElevation = 16.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Working Wheel Selector Row (Reusing WheelPicker from EditAlarmScreen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        items = (1..12).map { String.format(Locale.getDefault(), "%02d", it) },
                        initialIndex = (hour - 1).coerceIn(0, 11),
                        isInfinite = true,
                        width = 76.dp,
                        playScrollSound = true
                    ) { idx ->
                        onHourChange(idx + 1)
                    }

                    Text(
                        text = ":",
                        color = SoftAmber,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    WheelPicker(
                        items = (0..59).map { String.format(Locale.getDefault(), "%02d", it) },
                        initialIndex = minute.coerceIn(0, 59),
                        isInfinite = true,
                        width = 76.dp,
                        playScrollSound = true
                    ) { idx ->
                        onMinuteChange(idx)
                    }

                    WheelPicker(
                        items = listOf("AM", "PM"),
                        initialIndex = if (amPm == "AM") 0 else 1,
                        isInfinite = false,
                        width = 82.dp,
                        playScrollSound = true
                    ) { idx ->
                        val newAmPm = if (idx == 0) "AM" else "PM"
                        onAmPmChange(newAmPm)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Repeat Config Glass Card
        Surface(
            onClick = onEditRepeat,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = HaInk.copy(alpha = 0.78f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        HaGoldPale.copy(alpha = 0.18f),
                        HaIvory.copy(alpha = 0.04f)
                    )
                )
            ),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SoftAmber.copy(alpha = 0.15f))
                        .border(1.dp, SoftAmber.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = SoftAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Repeat",
                        color = HaIvory,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = repeatText,
                        color = HaIvory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HaIvory.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "Edit",
                        color = SoftAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Step3ChallengeContent(
    selectedChallengeId: String,
    onSelectChallenge: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Pick your challenge",
            color = HaIvory,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "CHALLENGES",
            color = SoftAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Challenge Cards
        val challengeList = listOf(
            Triple("maths", Challenge.Maths.name, Challenge.Maths.description to Icons.Default.Calculate),
            Triple("qr_code", Challenge.QRCode.name, Challenge.QRCode.description to Icons.Default.QrCode),
            Triple("rewrite", Challenge.Rewrite.name, Challenge.Rewrite.description to Icons.Default.Edit),
            Triple("none", Challenge.None.name, Challenge.None.description to Icons.Default.NightsStay)
        )

        challengeList.forEach { (id, title, details) ->
            val (desc, icon) = details
            val isSelected = selectedChallengeId == id

            Surface(
                onClick = { onSelectChallenge(id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(
                        1.dp,
                        if (isSelected) Brush.linearGradient(listOf(SoftAmber, SoftAmber)) else Brush.verticalGradient(
                            listOf(
                                HaGoldPale.copy(alpha = 0.18f),
                                HaIvory.copy(alpha = 0.04f)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    ),
                shape = RoundedCornerShape(22.dp),
                color = if (isSelected) HaInk.copy(alpha = 0.90f) else HaInk.copy(alpha = 0.70f),
                shadowElevation = if (isSelected) 12.dp else 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) SoftAmber.copy(alpha = 0.2f) else HaIvory.copy(alpha = 0.06f))
                            .border(1.dp, if (isSelected) SoftAmber else HaIvory.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) SoftAmber else HaIvory,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = HaIvory,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            color = HaIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) SoftAmber else HaIvory.copy(alpha = 0.08f))
                            .border(1.dp, if (isSelected) SoftAmber else HaIvory.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MidnightBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You can unlock and select more challenges inside the app anytime.",
            color = HaMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun Step4PermissionsContent(
    isBatteryOptIgnored: Boolean,
    areNotificationsAllowed: Boolean,
    canDrawOverlays: Boolean,
    onRequestBattery: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    val isAllGranted = isBatteryOptIgnored && areNotificationsAllowed && canDrawOverlays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Enable required permissions",
            color = HaIvory,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Three quick setup items help alarms stay reliable.",
            color = HaIvory,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(26.dp))

        // PERMISSION 1: Battery
        PermissionRowCard(
            icon = Icons.Default.BatteryChargingFull,
            title = "Battery access",
            description = "Stops the system from putting the app to sleep before your alarm fires.",
            isGranted = isBatteryOptIgnored,
            onClick = onRequestBattery
        )

        Spacer(modifier = Modifier.height(14.dp))

        // PERMISSION 2: Notifications
        PermissionRowCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            description = "Allows alarm and wake-up check alerts to appear on time.",
            isGranted = areNotificationsAllowed,
            onClick = onRequestNotifications
        )

        Spacer(modifier = Modifier.height(14.dp))

        // PERMISSION 3: Display Over Other Apps
        PermissionRowCard(
            icon = Icons.Default.OpenInFull,
            title = "Display over other apps",
            description = "Allows the alarm screen to open in full screen immediately when ringing.",
            isGranted = canDrawOverlays,
            onClick = onRequestOverlay
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Turn on permissions again / Status
        if (!isAllGranted) {
            Surface(
                onClick = {
                    if (!isBatteryOptIgnored) onRequestBattery()
                    else if (!areNotificationsAllowed) onRequestNotifications()
                    else if (!canDrawOverlays) onRequestOverlay()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = HaInk.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, SoftAmber.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = SoftAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Turn on permissions again",
                        color = SoftAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // All required permissions granted status pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = HaJade.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, HaJade.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = HaJade,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All required permissions are on",
                        color = HaJade,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRowCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = HaInk.copy(alpha = 0.78f),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                colors = listOf(
                    HaGoldPale.copy(alpha = 0.18f),
                    HaIvory.copy(alpha = 0.04f)
                )
            )
        ),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) HaJade.copy(alpha = 0.2f) else SoftAmber.copy(alpha = 0.15f))
                    .border(1.dp, if (isGranted) HaJade.copy(alpha = 0.4f) else SoftAmber.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) HaJade else SoftAmber,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = HaIvory,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = HaIvory,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isGranted) HaJade.copy(alpha = 0.15f) else SoftAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (isGranted) HaJade.copy(alpha = 0.4f) else SoftAmber.copy(alpha = 0.35f))
            ) {
                Text(
                    text = if (isGranted) "On" else "Off",
                    color = if (isGranted) HaJade else SoftAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun Step5SummaryContent(
    timeText: String,
    repeatText: String
) {
    val entranceAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entranceAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
    }

    val p = entranceAnim.value

    val iconProgress = (p / 0.4f).coerceIn(0f, 1f)
    val iconScale = 0.7f + (iconProgress * 0.3f)

    val titleProgress = ((p - 0.2f) / 0.4f).coerceIn(0f, 1f)
    val titleOffsetY = (1f - titleProgress) * 16f

    val bodyProgress = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)
    val bodyOffsetY = (1f - bodyProgress) * 20f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Premium Success Icon
        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = iconProgress
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF173A2C),
                            Color(0xFF0F2119)
                        )
                    )
                )
                .border(2.dp, HaJade.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = HaJade,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "You're All Set!",
            color = HaIvory,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = titleProgress
                translationY = titleOffsetY
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your first alarm has been configured for success.",
            color = HaIvory,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = titleProgress
                translationY = titleOffsetY
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Large Glass Summary Card with uniform onboarding surface styling
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = bodyProgress
                    translationY = bodyOffsetY
                },
            shape = RoundedCornerShape(28.dp),
            color = HaInk.copy(alpha = 0.78f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        HaGold.copy(alpha = 0.35f),
                        HaHairline.copy(alpha = 0.4f)
                    )
                )
            ),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WAKE UP AT",
                    color = HaJade,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = timeText,
                    color = HaIvory,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HaIvory.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = SoftAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = repeatText,
                            color = HaIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OnboardingBottomNav(
    currentStep: Int,
    isContinueEnabled: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStep > 1) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(0.35f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HaIvory
                )
            ) {
                Text(
                    text = "Back",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val buttonEnabled = if (currentStep == 4) isContinueEnabled else true

        Button(
            onClick = onContinue,
            enabled = buttonEnabled,
            modifier = Modifier
                .weight(if (currentStep > 1) 0.65f else 1f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftAmber,
                contentColor = MidnightBlue,
                disabledContainerColor = SoftAmber.copy(alpha = 0.35f),
                disabledContentColor = MidnightBlue.copy(alpha = 0.4f)
            )
        ) {
            Text(
                text = if (currentStep == 5) "Let's Go" else "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun RepeatSelectionModal(
    selectedDays: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    var tempDays by remember { mutableStateOf(selectedDays) }
    var selectedPreset by remember {
        mutableStateOf(
            when {
                tempDays.isEmpty() -> "once"
                tempDays.size == 7 -> "every_day"
                tempDays.size == 5 && tempDays.containsAll(listOf(1, 2, 3, 4, 5)) -> "weekdays"
                tempDays.size == 2 && tempDays.containsAll(listOf(0, 6)) -> "weekends"
                else -> "custom"
            }
        )
    }

    val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = HaInk,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        HaGoldPale.copy(alpha = 0.25f),
                        HaIvory.copy(alpha = 0.05f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SoftAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = SoftAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Repeat",
                        color = HaIvory,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Options
                val options = listOf(
                    Triple("once", "Once", "Ring only one time"),
                    Triple("every_day", "Every Day", "Mon – Sun"),
                    Triple("weekdays", "Weekdays", "Mon – Fri"),
                    Triple("weekends", "Weekends", "Sat & Sun"),
                    Triple("custom", "Custom", "Pick specific days")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { (id, title, subtitle) ->
                        val isSelected = selectedPreset == id
                        Surface(
                            onClick = {
                                selectedPreset = id
                                when (id) {
                                    "once" -> tempDays = emptyList()
                                    "every_day" -> tempDays = listOf(0, 1, 2, 3, 4, 5, 6)
                                    "weekdays" -> tempDays = listOf(1, 2, 3, 4, 5)
                                    "weekends" -> tempDays = listOf(0, 6)
                                    "custom" -> if (tempDays.isEmpty()) tempDays = listOf(1, 2, 3, 4, 5)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) SoftAmber.copy(alpha = 0.12f) else HaIvory.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, if (isSelected) SoftAmber.copy(alpha = 0.5f) else HaIvory.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = HaIvory,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        color = HaIvory,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPreset = id
                                        when (id) {
                                            "once" -> tempDays = emptyList()
                                            "every_day" -> tempDays = listOf(0, 1, 2, 3, 4, 5, 6)
                                            "weekdays" -> tempDays = listOf(1, 2, 3, 4, 5)
                                            "weekends" -> tempDays = listOf(0, 6)
                                            "custom" -> if (tempDays.isEmpty()) tempDays = listOf(1, 2, 3, 4, 5)
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = SoftAmber,
                                        unselectedColor = HaMuted
                                    )
                                )
                            }
                        }
                    }
                }

                // Custom Day Selector Toggles
                if (selectedPreset == "custom") {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val isDaySelected = tempDays.contains(index)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDaySelected) SoftAmber else HaIvory.copy(alpha = 0.08f))
                                    .border(1.dp, if (isDaySelected) SoftAmber else HaIvory.copy(alpha = 0.15f), CircleShape)
                                    .clickable {
                                        tempDays = if (isDaySelected) tempDays - index else tempDays + index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label.take(1),
                                    color = if (isDaySelected) MidnightBlue else HaIvory,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

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
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HaIvory
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = { onConfirm(tempDays) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftAmber,
                            contentColor = MidnightBlue
                        )
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
