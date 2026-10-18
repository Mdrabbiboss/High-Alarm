package com.programmer.highalarm

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmReliabilityScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isIgnoringBattery by remember { mutableStateOf(checkBatteryStatus(context)) }
    var areNotificationsEnabled by remember { mutableStateOf(checkNotificationStatus(context)) }
    var canUseFullScreenIntent by remember { mutableStateOf(checkFullScreenStatus(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBattery = checkBatteryStatus(context)
                areNotificationsEnabled = checkNotificationStatus(context)
                canUseFullScreenIntent = checkFullScreenStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(8.dp).ha3DOrb()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HaGoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBlue
                )
            )
        },
        containerColor = MidnightBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 0.dp)
        ) {
            // Alarm Reliability Main Title
            Text(
                text = "Alarm reliability",
                color = HaIvory,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Quick settings check for phones with aggressive battery controls.",
                color = HaIvory,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Compact Reliability Icons Row with Dynamic Status Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderSummaryIcon(
                    icon = Icons.Default.BatteryChargingFull,
                    isAllowed = isIgnoringBattery
                )
                HeaderSummaryIcon(
                    icon = Icons.Default.RocketLaunch,
                    isAllowed = null
                )
                HeaderSummaryIcon(
                    icon = Icons.Default.NotificationsActive,
                    isAllowed = areNotificationsEnabled
                )
                HeaderSummaryIcon(
                    icon = Icons.Default.OpenInFull,
                    isAllowed = canUseFullScreenIntent
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Check 1: Battery
            ReliabilityCheckCard(
                number = "1",
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery",
                description = "Allow Unrestricted use so alarms can run reliably in the background.",
                statusText = if (isIgnoringBattery) "Unrestricted" else "Optimized",
                statusColor = if (isIgnoringBattery) HaJade else SoftAmber,
                buttonText = "Open battery settings",
                onButtonClick = { openBatterySettings(context) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Check 2: Auto start
            ReliabilityCheckCard(
                number = "2",
                icon = Icons.Default.RocketLaunch,
                title = "Auto start",
                description = "Allow High Alarm to start automatically and keep alarms running in the background.",
                statusText = null,
                statusColor = Color.Transparent,
                buttonText = "Open app settings",
                onButtonClick = { openAutoStartSettings(context) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Check 3: Notifications
            ReliabilityCheckCard(
                number = "3",
                icon = Icons.Default.NotificationsActive,
                title = "Notifications",
                description = "Allow notifications so alarm alerts and wake-up checks can appear normally.",
                statusText = if (areNotificationsEnabled) "Allowed" else "Not allowed",
                statusColor = if (areNotificationsEnabled) HaJade else HaCoral,
                buttonText = "Open notification settings",
                onButtonClick = { openNotificationSettings(context) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Check 4: Full-screen alarms
            ReliabilityCheckCard(
                number = "4",
                icon = Icons.Default.OpenInFull,
                title = "Full-screen alarms",
                description = "Allow full-screen alerts so alarms can appear over the lock screen.",
                statusText = if (canUseFullScreenIntent) "Allowed" else "Not allowed",
                statusColor = if (canUseFullScreenIntent) HaJade else HaCoral,
                buttonText = "Open full-screen settings",
                onButtonClick = { openFullScreenSettings(context) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Manufacturer Guidance Section
            Text(
                text = "Where users usually find these settings",
                color = HaIvory,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Menu names may vary by model and Android version.",
                color = HaMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ManufacturerGuidanceCard(
                    brand = "Xiaomi",
                    logoResId = R.drawable.ic_logo_xiaomi,
                    guidance = "Battery saver → No restrictions\nAuto-start → Allow High Alarm",
                    modifier = Modifier.fillMaxWidth()
                )
                ManufacturerGuidanceCard(
                    brand = "Huawei",
                    logoResId = R.drawable.ic_logo_huawei,
                    guidance = "App launch → Manage manually\nEnable Auto-launch & Background activity",
                    modifier = Modifier.fillMaxWidth()
                )
                ManufacturerGuidanceCard(
                    brand = "OPPO",
                    logoResId = R.drawable.ic_logo_oppo,
                    guidance = "Auto-launch → Allow\nBattery → Don't optimize",
                    modifier = Modifier.fillMaxWidth()
                )
                ManufacturerGuidanceCard(
                    brand = "vivo",
                    logoResId = R.drawable.ic_logo_vivo,
                    guidance = "Auto-start → Allow\nBattery → High background usage",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun HeaderSummaryIcon(
    icon: ImageVector,
    isAllowed: Boolean?
) {
    Box(
        modifier = Modifier.size(40.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            color = HaSurface,
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        SoftAmber.copy(alpha = 0.6f),
                        HaGoldMid.copy(alpha = 0.3f),
                        SoftAmber.copy(alpha = 0.6f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SoftAmber.copy(alpha = 0.18f),
                                HaSurface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SoftAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isAllowed != null) {
            val badgeBg = if (isAllowed) HaJadeDeep else HaCoralDeep
            val badgeIcon = if (isAllowed) Icons.Default.Check else Icons.Default.Close
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .clip(CircleShape)
                    .background(badgeBg)
                    .border(1.dp, HaObsidian, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = HaIvory,
                    modifier = Modifier.size(9.dp)
                )
            }
        }
    }
}

@Composable
fun ReliabilityCheckCard(
    number: String,
    icon: ImageVector,
    title: String,
    description: String,
    statusText: String?,
    statusColor: Color,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().ha3DPanel(RoundedCornerShape(20.dp), elevation = 10.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Step Badge (01, 02, 03, 04)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftAmber.copy(alpha = 0.12f))
                        .border(1.dp, SoftAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "0$number",
                        color = SoftAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Feature Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(HaIvory.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SoftAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title
                Text(
                    text = title,
                    color = HaIvory,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )

                // Status Badge
                if (statusText != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                color = HaIvory,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            PremiumActionButton(
                text = buttonText,
                onClick = onButtonClick
            )
        }
    }
}

@Composable
fun PremiumActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    HaGoldLight,
                    HaGoldMid,
                    HaGoldDeep
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            HaGold,
                            HaGoldMid,
                            HaGoldDeep
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = HaObsidian,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun ManufacturerGuidanceCard(
    brand: String,
    logoResId: Int,
    guidance: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.ha3DPanel(RoundedCornerShape(18.dp), elevation = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HaIvory.copy(alpha = 0.06f))
                        .border(1.dp, HaIvory.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(logoResId),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = brand,
                    color = HaIvory,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = guidance,
                color = HaIvory,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
        }
    }
}

private fun checkBatteryStatus(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return if (powerManager != null) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }
}

private fun checkNotificationStatus(context: Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun checkFullScreenStatus(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.canUseFullScreenIntent() ?: true
    } else {
        true
    }
}

private fun openBatterySettings(context: Context) {
    val packageName = context.packageName
    val intentList = mutableListOf<Intent>()

    intentList.add(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        intentList.add(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }
    intentList.add(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    )

    launchSafely(context, intentList)
}

private fun openAutoStartSettings(context: Context) {
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
    val intentList = mutableListOf<Intent>()

    when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
            intentList.add(Intent().apply {
                component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            })
        }
        manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
            intentList.add(Intent().apply {
                component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            })
            intentList.add(Intent().apply {
                component = ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            })
        }
        manufacturer.contains("vivo") -> {
            intentList.add(Intent().apply {
                component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            })
        }
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
            intentList.add(Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            })
            intentList.add(Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            })
        }
    }

    intentList.add(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    )

    launchSafely(context, intentList)
}

private fun openNotificationSettings(context: Context) {
    val packageName = context.packageName
    val intentList = mutableListOf<Intent>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intentList.add(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        )
    }
    intentList.add(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    )

    launchSafely(context, intentList)
}

private fun openFullScreenSettings(context: Context) {
    val packageName = context.packageName
    val intentList = mutableListOf<Intent>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        intentList.add(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intentList.add(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        )
    }
    intentList.add(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    )

    launchSafely(context, intentList)
}

private fun launchSafely(context: Context, intents: List<Intent>) {
    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Try next intent fallback
        }
    }
    Toast.makeText(context, "Could not open settings automatically. Please check system settings.", Toast.LENGTH_SHORT).show()
}
