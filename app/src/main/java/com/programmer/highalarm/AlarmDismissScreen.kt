package com.programmer.highalarm

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.toColorInt
import android.media.AudioAttributes
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

// --- Models ---
data class PoseFrameData(val pose: Pose, val rotation: Int, val isFront: Boolean, val width: Int, val height: Int)

// --- Utility Functions ---
fun calculatePoseAngle(first: PoseLandmark, mid: PoseLandmark, last: PoseLandmark): Float {
    var angle = Math.toDegrees((atan2(last.position.y - mid.position.y, last.position.x - mid.position.x) - atan2(first.position.y - mid.position.y, first.position.x - mid.position.x)).toDouble())
    angle = abs(angle); if (angle > 180) angle = 360.0 - angle; return angle.toFloat()
}

fun getObjectKeywords(target: String): List<String> = when (target.lowercase()) {
    "bottle" -> listOf("bottle", "water bottle", "wine bottle", "plastic bottle", "glass bottle", "flask", "canteen", "thermos", "soda bottle", "beverage", "soft drink", "cup", "mug", "container", "jug", "vessel", "cylinder", "liquid", "drinkware", "tableware", "kitchenware")
    "backpack" -> listOf("backpack", "bag", "rucksack", "luggage", "knapsack", "satchel")
    "badminton" -> listOf("badminton", "racket", "shuttlecock", "racquet", "tennis racket", "sports equipment", "net", "mesh", "bat", "sports", "tennis", "stringing")
    "bed" -> listOf("bed", "bedroom", "mattress", "pillow", "quilt", "duvet", "bedstead", "cot", "bunk bed", "furniture", "couch", "sofa", "sleeping", "studio couch")
    "sink" -> listOf("sink", "washbasin", "basin", "faucet", "tap")
    "coffee cup" -> listOf("coffee cup", "mug", "cup", "teacup")
    "shoe" -> listOf("shoe", "sneaker", "footwear", "boot")
    "plant" -> listOf("plant", "houseplant", "flower")
    "laptop" -> listOf("laptop", "notebook computer", "notebook", "pc", "computer", "keyboard", "personal computer", "netbook", "ultrabook", "chromebook", "macbook")
    "fruit" -> listOf("fruit", "apple", "banana", "orange")
    "watch" -> listOf("watch", "wristwatch", "clock")
    "chair" -> listOf("chair", "seat", "furniture")
    "television" -> listOf("television", "tv", "monitor", "television set", "plasma television", "liquid crystal display", "led television")
    "phone" -> listOf("mobile phone", "telephone", "smartphone", "cell phone", "phone", "handset", "communication device", "mobile device")
    "bicycle" -> listOf("bicycle", "bike", "cycle")
    "clock" -> listOf("clock", "wall clock", "watch")
    "sky" -> listOf("sky", "cloud", "atmosphere", "daylight", "horizon", "natural landscape", "azure", "cumulus", "meteorological phenomenon", "blue sky")
    "dog" -> listOf("dog", "canine", "puppy", "hound", "retriever", "terrier", "bulldog", "shepherd", "corgi", "poodle", "pug")
    else -> listOf(target.lowercase())
}

@Composable
fun getObjectIcon(name: String): ImageVector = when(name) {
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

@SuppressLint("NewApi")
@Composable
fun RandomObjectPicker(pool: List<String>, onPicked: (String) -> Unit) {
    val safePool = remember(pool) { pool.filter { it.isNotBlank() } }
    
    if (safePool.isEmpty()) {
        LaunchedEffect(Unit) { onPicked("Phone") }
        Box(modifier = Modifier.fillMaxSize().background(MidnightBlue))
        return
    }

    val vibrantColors = remember {
        listOf(
            HaSky, // Electric Blue
            HaJade, // Emerald Green
            HaRose, // Neon Orange
            HaGoldLight, // Deep Purple
            HaPlatinum, // Cyan
            HaGold  // Amber
        )
    }

    var currentItem by remember { mutableStateOf(safePool.random()) }
    var currentColor by remember { mutableStateOf(vibrantColors.random()) }
    var isLanding by remember { mutableStateOf(false) }
    val finalPick = remember { safePool.random() }
    val finalColor = remember { vibrantColors.random() }
    
    val animatedAccentColor by animateColorAsState(
        targetValue = if (isLanding) finalColor else currentColor,
        animationSpec = tween(durationMillis = if (isLanding) 1000 else 150),
        label = "color"
    )

    LaunchedEffect(Unit) {
        // Shuffle phase
        repeat(15) {
            currentItem = safePool.random()
            currentColor = vibrantColors.random()
            delay(100.milliseconds)
        }
        repeat(8) {
            currentItem = safePool.random()
            currentColor = vibrantColors.random()
            delay(200.milliseconds)
        }
        currentItem = finalPick
        isLanding = true
        delay(2500.milliseconds)
        onPicked(finalPick)
    }

    Box(modifier = Modifier.fillMaxSize().background(MidnightBlue), contentAlignment = Alignment.Center) {
        // Dynamic Glow Background
        val glowAlpha by animateFloatAsState(
            targetValue = if (isLanding) 0.15f else 0.05f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "glow"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(animatedAccentColor.copy(alpha = glowAlpha), Color.Transparent)))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isLanding) "YOUR OBJECT IS..." else "PICKING OBJECT...",
                color = animatedAccentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = if (isLanding) 1.1f else pulseScale
                        scaleY = if (isLanding) 1.1f else pulseScale
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(HaIvory.copy(alpha = 0.05f))
                    .border(
                        width = if (isLanding) 3.dp else 1.dp,
                        color = animatedAccentColor.copy(alpha = if (isLanding) 1f else 0.2f),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getObjectIcon(currentItem),
                    contentDescription = null,
                    tint = animatedAccentColor,
                    modifier = Modifier.size(72.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = currentItem.uppercase(),
                color = HaIvory,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            
            if (!isLanding) {
                Text(
                    text = "Stand by...",
                    color = HaIvory.copy(alpha = 0.4f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private fun generateMaze(size: Int): Array<IntArray> {
    val maze = Array(size) { IntArray(size) { 1 } }
    val stack = mutableListOf(0 to 0)
    maze[0][0] = 0
    val directions = listOf(0 to 2, 0 to -2, 2 to 0, -2 to 0)
    
    while (stack.isNotEmpty()) {
        val current = stack.last()
        val neighbors = directions
            .map { (current.first + it.first) to (current.second + it.second) }
            .filter { it.first in 0 until size && it.second in 0 until size && maze[it.second][it.first] == 1 }
            
        if (neighbors.isNotEmpty()) {
            val next = neighbors.random()
            maze[next.second][next.first] = 0
            // Clear the wall between
            maze[current.second + (next.second - current.second) / 2][current.first + (next.first - current.first) / 2] = 0
            stack.add(next)
        } else {
            stack.removeAt(stack.size - 1)
        }
    }
    
    // Ensure exit is reachable
    maze[size - 1][size - 1] = 0
    if (maze[size - 2][size - 1] == 1 && maze[size - 1][size - 2] == 1) {
        maze[size - 2][size - 1] = 0
    }
    
    return maze
}

private fun generateDifficultyWord(difficulty: Int): String = when (difficulty) {
    0 -> listOf("APPLE", "PIZZA", "BEACH", "CHAIR", "DREAM", "FLOWER", "GARDEN", "HAPPY").random() // 5-6 letters
    1 -> listOf("JOURNEY", "KITCHEN", "MORNING", "PICTURE", "RELAXED", "SUNRISE", "WEATHER").random() // 7-8 letters
    2 -> listOf("BEAUTIFUL", "CHALLENGE", "DIFFERENT", "EDUCATION", "IMPORTANT", "KNOWLEDGE").random() // 9-10 letters
    else -> listOf("CONGRATULATIONS", "IMAGINATION", "UNDERSTANDING", "OPPORTUNITY", "SUCCESSFUL").random() // 10+ letters
}
private fun jumbleWord(word: String): String = word.toList().shuffled().joinToString("")
private fun generateShuffledPuzzle(size: Int): List<Int> {
    val tiles = (1 until size * size).toMutableList()
    tiles.add(0) // 0 represents the empty slot
    var currentTiles = tiles.toList()
    var emptyIndex = size * size - 1
    val random = java.util.Random()
    
    // Perform a series of valid moves to ensure solvability
    repeat(400 * size) {
        val row = emptyIndex / size
        val col = emptyIndex % size
        val neighbors = mutableListOf<Int>()
        if (row > 0) neighbors.add(emptyIndex - size)
        if (row < size - 1) neighbors.add(emptyIndex + size)
        if (col > 0) neighbors.add(emptyIndex - 1)
        if (col < size - 1) neighbors.add(emptyIndex + 1)
        
        val nextIndex = neighbors[random.nextInt(neighbors.size)]
        val newList = currentTiles.toMutableList()
        newList[emptyIndex] = currentTiles[nextIndex]
        newList[nextIndex] = 0
        currentTiles = newList.toList()
        emptyIndex = nextIndex
    }
    return currentTiles
}
private fun isPuzzleSolved(tiles: List<Int>): Boolean { for (i in 0 until tiles.size - 1) { if (tiles[i] != i + 1) return false }; return tiles.last() == 0 }
private fun generateUniqueRandomTime(difficulty: Int, used: List<Pair<Int, Int>>): Pair<Int, Int> {
    var time: Pair<Int, Int>
    var attempts = 0
    do {
        time = when (difficulty) {
            0 -> { // Easy: 5-minute intervals
                val h = Random.nextInt(1, 13)
                val m = (0..11).random() * 5
                h to m
            }
            1 -> { // Medium: Any minute
                val h = Random.nextInt(1, 13)
                val m = Random.nextInt(0, 60)
                h to m
            }
            2 -> { // Hard: Tricky positions (e.g. 1:59, 6:01, 11:32)
                val h = Random.nextInt(1, 13)
                val m = if (Random.nextBoolean()) {
                    listOf(1, 2, 29, 31, 58, 59).random() // Near markers
                } else {
                    Random.nextInt(0, 60)
                }
                h to m
            }
            else -> { // Extreme: Maximum confusion
                // Very close hands or overlapping positions (e.g. 12:05, 1:06, 6:32)
                val h = Random.nextInt(1, 13)
                val baseMinute = (h % 12) * 5
                val offset = listOf(-2, -1, 1, 2, 3, 4).random()
                val m = (baseMinute + offset + 60) % 60
                h to m
            }
        }
        attempts++
    } while (used.contains(time) && attempts < 100)
    return time
}
private fun formatTimeInput(input: String): String = when (input.length) { 1 -> input; 2 -> "${input[0]}:0${input[1]}"; 3 -> "${input[0]}:${input[1]}${input[2]}"; 4 -> "${input[0]}${input[1]}:${input[2]}${input[3]}"; else -> input }

// --- Main Screens ---

@SuppressLint("NewApi")
@Composable
fun AlarmDismissScreen(
    alarm: Alarm, 
    settingsViewModel: SettingsViewModel,
    onChallengeSolved: () -> Unit, 
    onDismiss: () -> Unit
) {
    val context = LocalContext.current; val settings by settingsViewModel.settings.collectAsState()
    var isDismissedSuccessfully by remember { mutableStateOf(false) }; var hasClickedInitialDismiss by remember { mutableStateOf(false) }
    
    BackHandler(enabled = !isDismissedSuccessfully) { }
    fun stopAlarmSound() { context.getSharedPreferences("alarm_state", Context.MODE_PRIVATE).edit { putBoolean("is_active", false) }; context.stopService(Intent(context, AlarmService::class.java)) }

    // loadSettings is already called in MainActivity
    LaunchedEffect(hasClickedInitialDismiss) { 
        if (hasClickedInitialDismiss && alarm.challenges.all { it.id == "none" }) { 
            onChallengeSolved()
            stopAlarmSound()
            isDismissedSuccessfully = true 
        } 
    }
    // Mute During Challenge Implementation
    LaunchedEffect(hasClickedInitialDismiss) {
        if (hasClickedInitialDismiss && settings.isMuteDuringChallengeEnabled) {
            // Mute completely
            context.startService(Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_LOWER_VOLUME
            })
            // Unmute automatically after 2 minutes if the user hasn't solved it yet
            delay(2.minutes)
            if (!isDismissedSuccessfully) {
                context.startService(Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_RESTORE_VOLUME
                })
            }
        }
    }

    fun onSnooze(mins: Int) { stopAlarmSound(); AlarmScheduler.snooze(context, alarm, mins); (context as? Activity)?.finish() }

    Surface(modifier = Modifier.fillMaxSize(), color = MidnightBlue) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isDismissedSuccessfully) SuccessAnimation(onFinished = onDismiss)
            else if (!hasClickedInitialDismiss) InitialDismissState(alarm.time, alarm.label, settings.isSnoozeEnabled, { hasClickedInitialDismiss = true }, ::onSnooze)
            else ChallengeState(alarm) { 
                onChallengeSolved()
                stopAlarmSound()
                isDismissedSuccessfully = true 
            }
        }
    }
}

@Composable
fun PremiumBellIcon(isSmallScreen: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "bell_anim")
    
    // Smooth swaying motion
    val rotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    // Subtle pulsing glow
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val bellSize = if (isSmallScreen) 54.dp else 72.dp
    
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(bellSize * 2f)) {
        // --- High-End Radial Glow (Fixed Artifacts) ---
        Box(
            modifier = Modifier
                .size(bellSize * 1.6f)
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                    alpha = glowAlpha
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(SoftAmber.copy(alpha = 0.6f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // --- Polished Gold Bell ---
        Box(
            modifier = Modifier
                .size(bellSize)
                .shadow(12.dp, CircleShape, ambientColor = SoftAmber, spotColor = SoftAmber)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(HaGoldPale, HaGold, HaGoldDeep)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                modifier = Modifier
                    .size(bellSize * 0.6f)
                    .graphicsLayer { rotationZ = rotation },
                tint = MidnightBlue
            )
        }
    }
}

@Composable
fun InitialDismissState(time: String, label: String, isSnoozeEnabled: Boolean, onDismissClick: () -> Unit, onSnoozeClick: (Int) -> Unit) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 600

    Box(modifier = Modifier.fillMaxSize()) {
        
        // --- Fluid Floating Animation for Dismiss Button ---
        val infiniteTransition = rememberInfiniteTransition(label = "floating")
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -12f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "y"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Flexible spacers to ensure perfect vertical balance and prevent overlaps
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PremiumBellIcon(isSmallScreen)
                
                Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 36.dp))
                
                // --- Alarm Label ---
                if (label.isNotEmpty()) {
                    Surface(
                        color = SoftAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = label.uppercase(),
                            color = SoftAmber,
                            fontSize = if (isSmallScreen) 12.sp else 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            letterSpacing = 2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val timeParts = time.split(" ")
                    val clockText = timeParts.getOrNull(0) ?: ""
                    val amPmText = timeParts.getOrNull(1) ?: ""

                    Text(
                        text = clockText,
                        fontSize = if (isSmallScreen) 62.sp else 84.sp,
                        fontWeight = FontWeight.Black,
                        color = HaIvory,
                        letterSpacing = (-1).sp
                    )
                    
                    if (amPmText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = amPmText,
                            fontSize = if (isSmallScreen) 18.sp else 24.sp,
                            fontWeight = FontWeight.Black,
                            color = SoftAmber,
                            modifier = Modifier.padding(bottom = if (isSmallScreen) 10.dp else 14.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "WAKE UP",
                    color = HaIvory.copy(0.4f),
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontSize = if (isSmallScreen) 12.sp else 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1.4f)) // Balanced assembly

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(bottom = if (isSmallScreen) 24.dp else 48.dp)
                    .graphicsLayer { translationY = floatOffset } // Apply floating effect
            ) {
                Button(
                    onClick = onDismissClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSmallScreen) 76.dp else 88.dp)
                        .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = HaIvory.copy(0.2f), spotColor = HaIvory.copy(0.2f)),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HaIvory,
                        contentColor = MidnightBlue
                    )
                ) {
                    Text(
                        text = "DISMISS",
                        fontSize = if (isSmallScreen) 20.sp else 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }

                if (isSnoozeEnabled) {
                    Spacer(modifier = Modifier.height(if (isSmallScreen) 24.dp else 36.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        listOf(5, 10, 15).forEach { m ->
                            Surface(
                                onClick = { onSnoozeClick(m) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isSmallScreen) 54.dp else 62.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = HaIvory.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${m}m",
                                        color = HaIvory.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isSmallScreen) 14.sp else 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(0.7f)) // Bottom buffer spacer
        }
    }
}

@Composable
fun ChallengeState(alarm: Alarm, onComplete: () -> Unit) {
    var currentChallengeIndex by remember { mutableIntStateOf(0) }
    val activeConfigs = remember(alarm.challenges) { alarm.challenges.filter { it.id != "none" } }
    
    if (activeConfigs.isEmpty()) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    val currentConfig = activeConfigs[currentChallengeIndex]
    val challengeType = remember(currentConfig.id) { getAllChallenges().find { it.id == currentConfig.id } ?: Challenge.None }

    fun next() {
        if (currentChallengeIndex < activeConfigs.size - 1) {
            currentChallengeIndex++
        } else {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Balanced spacing for the title badge - Lowered to sit comfortably away from status bar
        Spacer(modifier = Modifier.height(64.dp))
        
        Surface(
            color = SoftAmber.copy(0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "${challengeType.name.uppercase()} (${currentChallengeIndex + 1}/${activeConfigs.size})",
                color = SoftAmber,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                letterSpacing = 2.sp,
                fontSize = 12.sp
            )
        }
        
        // Weight-based centering to ensure the challenge is perfectly vertically aligned
        Box(
            modifier = Modifier.weight(1f), 
            contentAlignment = Alignment.Center
        ) {
            when (challengeType) {
                is Challenge.Maths -> MathChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.AdvancedMaths -> AdvancedMathChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.Rewrite -> RewriteChallengeView(mode = currentConfig.setting, customText = currentConfig.extra, onComplete = ::next)
                is Challenge.Tap -> TapChallengeView(targetCount = currentConfig.setting, onComplete = ::next)
                is Challenge.Charger -> ChargerChallengeView(targetCount = currentConfig.setting, onComplete = ::next)
                is Challenge.LightsOn -> LightsOnChallengeView(targetLux = currentConfig.setting, onComplete = ::next)
                is Challenge.Steps -> StepsChallengeView(targetSteps = currentConfig.setting, onComplete = ::next)
                is Challenge.Maze -> MazeChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.Memory -> MemoryChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.ClockReading -> ClockReadingChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.Shake -> ShakeChallengeView(targetShakes = currentConfig.setting, onComplete = ::next)
                is Challenge.SpellBee -> SpellBeeChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.Puzzle -> PuzzleChallengeView(currentConfig.setting, currentConfig.difficulty, ::next)
                is Challenge.ScanSink -> ScanSinkChallengeView(mode = currentConfig.setting, extra = currentConfig.extra, onComplete = ::next)
                is Challenge.PushUp -> PushUpChallengeView(targetReps = currentConfig.setting, onComplete = ::next)
                is Challenge.Squat -> {
                    val mode = if (currentConfig.extra == "motion") 1 else 0
                    SquatChallengeView(targetReps = currentConfig.setting, detectionMode = mode, onComplete = ::next)
                }
                is Challenge.ScanObject -> ObjectChallengeView(mode = currentConfig.setting, extra = currentConfig.extra, onComplete = ::next)
                is Challenge.QRCode -> QRCodeChallengeView(mode = currentConfig.setting, targetQR = currentConfig.extra, onComplete = ::next)
                is Challenge.ReverseTyping -> ReverseTypingChallengeView(rounds = currentConfig.setting, onComplete = ::next)
                is Challenge.AudioMemory -> AudioMemoryChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.BalanceTest -> BalanceTestChallengeView(targetSeconds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.SafeCode -> SafeCodeChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                is Challenge.ControlPanel -> ControlPanelChallengeView(rounds = currentConfig.setting, difficulty = currentConfig.difficulty, onComplete = ::next)
                else -> next()
            }
        }
    }
}

// --- Challenge Views ---

@SuppressLint("NewApi")
@Composable
fun MathChallengeView(rounds: Int, difficulty: Int = 1, onComplete: () -> Unit) {
    var cur by remember { mutableIntStateOf(1) }
    val mathDiff = when(difficulty) {
        0 -> MathChallengeGenerator.Difficulty.Easy
        1 -> MathChallengeGenerator.Difficulty.Medium
        2 -> MathChallengeGenerator.Difficulty.Hard
        else -> MathChallengeGenerator.Difficulty.Extreme
    }
    var p by remember(cur) { mutableStateOf(MathChallengeGenerator.generate(mathDiff)) }
    var input by remember { mutableStateOf("") }
    var f by remember { mutableStateOf(false) }
    
    LaunchedEffect(f) { if (f) { delay(300.milliseconds); f = false; input = "" } }
    
    Box(modifier = Modifier.fillMaxSize().background(if (f) HaCoral.copy(0.4f) else Color.Transparent), Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = "QUESTION $cur OF $rounds", 
                color = HaJade, 
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(color = HaIvory.copy(0.05f), shape = RoundedCornerShape(32.dp)) { 
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { 
                    Text(p.expression, fontSize = 44.sp, fontWeight = FontWeight.Black, color = HaIvory)
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(16.dp)), Alignment.Center) { 
                        Text(input.ifEmpty { "ANSWER" }, fontSize = 26.sp, color = if (input.isEmpty()) HaMuted else HaIvory) 
                    } 
                } 
            }
            Spacer(modifier = Modifier.height(32.dp))
            CustomKeypad { k -> 
                when (k) { 
                    "DEL" -> if (input.isNotEmpty()) input = input.dropLast(1)
                    "OK" -> {
                        if (input.toIntOrNull() == p.result) {
                            if (cur < rounds) {
                                cur++
                                input = ""
                            } else {
                                onComplete()
                            }
                        } else f = true
                    }
                    else -> if (input.length < 8) input += k 
                } 
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun AdvancedMathChallengeView(rounds: Int, difficulty: Int = 1, onComplete: () -> Unit) {
    var cur by remember { mutableIntStateOf(1) }
    val mathDiff = when(difficulty) {
        0 -> AdvancedMathChallengeGenerator.Difficulty.Easy
        1 -> AdvancedMathChallengeGenerator.Difficulty.Medium
        2 -> AdvancedMathChallengeGenerator.Difficulty.Hard
        else -> AdvancedMathChallengeGenerator.Difficulty.Extreme
    }
    var p by remember(cur) { mutableStateOf(AdvancedMathChallengeGenerator.generate(mathDiff)) }
    var input by remember { mutableStateOf("") }
    var f by remember { mutableStateOf(false) }
    var errorCount by remember(cur) { mutableIntStateOf(0) }
    
    LaunchedEffect(f) { if (f) { delay(300.milliseconds); f = false; input = "" } }
    
    Box(modifier = Modifier.fillMaxSize().background(if (f) HaCoral.copy(0.4f) else Color.Transparent), Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = "QUESTION $cur OF $rounds", 
                color = HaJade, 
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(color = HaIvory.copy(0.05f), shape = RoundedCornerShape(32.dp)) { 
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { 
                    Text(p.expression, fontSize = 32.sp, fontWeight = FontWeight.Black, color = HaIvory)
                    
                    if (p.hint != null && errorCount >= 2) {
                        Text(p.hint!!, fontSize = 14.sp, color = SoftAmber, modifier = Modifier.padding(top = 8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(16.dp)), Alignment.Center) { 
                        Text(input.ifEmpty { "ANSWER" }, fontSize = 24.sp, color = if (input.isEmpty()) HaMuted else HaIvory) 
                    } 
                } 
            }
            Spacer(modifier = Modifier.height(32.dp))
            CustomKeypad { k -> 
                when (k) { 
                    "DEL" -> if (input.isNotEmpty()) input = input.dropLast(1)
                    "OK" -> {
                        if (input.toIntOrNull() == p.result) {
                            if (cur < rounds) {
                                cur++
                                input = ""
                            } else {
                                onComplete()
                            }
                        } else {
                            errorCount++
                            f = true
                        }
                    }
                    else -> if (input.length < 8) input += k 
                } 
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun RewriteChallengeView(mode: Int, customText: String, onComplete: () -> Unit) {
    val target by remember { 
        mutableStateOf(if (mode == 1 && customText.isNotBlank()) customText else Affirmations.list.random()) 
    }
    var input by remember { mutableStateOf("") }
    var f by remember { mutableStateOf(false) }
    
    LaunchedEffect(f) { if (f) { delay(400.milliseconds); f = false; input = "" } }
    LaunchedEffect(input) { 
        if (input.length >= target.length && !f) { 
            if (input.trim() == target.trim()) onComplete() else f = true 
        } 
    }
    
    val isCorrectSoFar = target.startsWith(input)
    val boxColor = if (f) HaCoral else if (isCorrectSoFar && input.isNotEmpty()) HaJade else SoftAmber
    
    Box(modifier = Modifier.fillMaxSize().background(if (f) HaCoral.copy(0.3f) else Color.Transparent), Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp).imePadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(
                color = HaIvory.copy(alpha = 0.04f), 
                shape = RoundedCornerShape(32.dp), 
                border = BorderStroke(1.dp, boxColor.copy(alpha = 0.5f))
            ) { 
                Text(target, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HaJade, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp)) 
            }
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = input, 
                onValueChange = { if (!f) input = it }, 
                modifier = Modifier.fillMaxWidth(), 
                placeholder = { Text("Type above text here...", color = HaMuted.copy(0.5f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = HaIvory, fontSize = 18.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = boxColor,
                    unfocusedBorderColor = boxColor.copy(alpha = 0.3f),
                    cursorColor = boxColor
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("NewApi")
@Composable
fun TapChallengeView(targetCount: Int, onComplete: () -> Unit) {
    val t = remember(targetCount) { 
        if (targetCount == 0) {
            listOf(12, 15, 20, 25, 30, 35, 40, 45, 50, 55).random()
        } else targetCount 
    }
    var left by remember(t) { mutableIntStateOf(t) }
    val aX = remember { Animatable(0.5f) }
    val aY = remember { Animatable(0.5f) }
    val scope = rememberCoroutineScope()
    
    // Scale animation for the hand icon on tap
    val scale = remember { Animatable(1f) }

    Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
        // --- Sleek Line-Based Count Display ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 40.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$left TAPS LEFT", 
                color = SoftAmber, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Black, 
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(t) { index ->
                    val isActive = index < (t - left)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) SoftAmber else HaIvory.copy(alpha = 0.1f)
                            )
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val constraintsScope = this
            val d = LocalDensity.current
            val mW = with(d) { constraintsScope.maxWidth.toPx() }
            val mH = with(d) { constraintsScope.maxHeight.toPx() }
            
            // --- Premium Hand Icon ---
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer { 
                        translationX = (aX.value * mW) - with(d) { 38.dp.toPx() }
                        translationY = (aY.value * mH) - with(d) { 38.dp.toPx() } 
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SoftAmber.copy(alpha = 0.4f), SoftAmber.copy(alpha = 0.1f))
                        )
                    )
                    .border(2.dp, SoftAmber, CircleShape)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() }, 
                        indication = null
                    ) { 
                        if (left > 1) { 
                            left--
                            scope.launch { 
                                val fastSpec = tween<Float>(120, easing = LinearOutSlowInEasing)
                                launch { aX.animateTo(Random.nextFloat().coerceIn(0.12f, 0.88f), fastSpec) }
                                launch { aY.animateTo(Random.nextFloat().coerceIn(0.12f, 0.85f), fastSpec) }
                            } 
                        } else {
                            onComplete()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = SoftAmber,
                    modifier = Modifier.size(40.dp)
                )
                // Outer subtle ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .border(1.dp, SoftAmber.copy(alpha = 0.3f), CircleShape)
                )
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun StepsChallengeView(targetSteps: Int, onComplete: () -> Unit) {
    val ctx = LocalContext.current
    var cur by remember { mutableIntStateOf(0) }
    
    val progress = remember(cur, targetSteps) { 
        (cur.toFloat() / targetSteps).coerceIn(0f, 1f) 
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    LaunchedEffect(Unit) { 
        StepSensorHelper.getStepDetectorFlow(ctx).collect { 
            cur++
            if (cur >= targetSteps) {
                delay(200.milliseconds) // Brief pause to see 100%
                onComplete()
            }
        } 
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Background Glow
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(SoftAmber.copy(alpha = 0.1f), Color.Transparent)
                        )
                    )
            )
            
            // Progress Ring
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(220.dp),
                color = if (cur >= targetSteps) HaJade else SoftAmber,
                strokeWidth = 12.dp,
                trackColor = HaIvory.copy(alpha = 0.05f),
                strokeCap = StrokeCap.Round
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = SoftAmber,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cur.toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = HaIvory
                )
                Text(
                    text = "OF $targetSteps",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HaMuted,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "WALK TO DISMISS",
            color = SoftAmber,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Keep moving until target is reached",
            color = HaIvory.copy(alpha = 0.4f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@SuppressLint("NewApi")
@Composable
fun ClockReadingChallengeView(rounds: Int, difficulty: Int, onComplete: () -> Unit) {
    var cur by remember { mutableIntStateOf(1) }; var input by remember { mutableStateOf("") }; val u = remember { mutableStateListOf<Pair<Int, Int>>() }; var t by remember { mutableStateOf(generateUniqueRandomTime(difficulty, u)) }; var f by remember { mutableStateOf(false) }
    LaunchedEffect(f) { if (f) { delay(400.milliseconds); f = false; input = "" } }
    Box(modifier = Modifier.fillMaxSize().background(if (f) HaCoral.copy(0.3f) else Color.Transparent), Alignment.Center) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("ROUND $cur OF $rounds", color = HaJade, fontWeight = FontWeight.Black); Spacer(modifier = Modifier.height(24.dp))
            AnalogClock(t.first, t.second, difficulty, Modifier.size(240.dp)); Spacer(modifier = Modifier.height(24.dp))
            Text(formatTimeInput(input).ifEmpty { "H:MM" }, fontSize = 34.sp, fontWeight = FontWeight.Black, color = HaIvory)
            Spacer(modifier = Modifier.height(24.dp)); CustomKeypad { k -> if (k == "OK") { if (formatTimeInput(input) == String.format(Locale.getDefault(), "%d:%02d", if (t.first == 0) 12 else t.first, t.second)) { u.add(t); if (cur < rounds) { cur++; t = generateUniqueRandomTime(difficulty, u); input = "" } else onComplete() } else f = true } else if (k == "DEL") { if (input.isNotEmpty()) input = input.dropLast(1) } else if (input.length < 4) input += k }
        }
    }
}

@Composable
fun AnalogClock(h: Int, m: Int, difficulty: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.width / 2f
        
        // Face background
        drawCircle(color = HaIvory.copy(alpha = 0.05f), radius = r, center = c)
        drawCircle(color = HaIvory.copy(alpha = 0.15f), radius = r, center = c, style = Stroke(2.dp.toPx()))
        
        // Ticks/Dots
        for (i in 0 until 60) {
            val angle = (i * 6) * (PI / 180f)
            val isHour = i % 5 == 0
            
            // Extreme hides minute ticks for maximum challenge
            if (difficulty < 3 || isHour) {
                val startRadius = if (isHour) r * 0.86f else r * 0.92f
                val endRadius = r * 0.96f
                
                val start = Offset(c.x + startRadius * cos(angle).toFloat(), c.y + startRadius * sin(angle).toFloat())
                val end = Offset(c.x + endRadius * cos(angle).toFloat(), c.y + endRadius * sin(angle).toFloat())
                
                drawLine(
                    color = if (isHour) HaIvory.copy(0.4f) else HaIvory.copy(0.15f),
                    start = start,
                    end = end,
                    strokeWidth = if (isHour) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        val mA = (m * 6 - 90) * (PI / 180f)
        val hA = ((h % 12 + m / 60f) * 30 - 90) * (PI / 180f)
        
        // Hour hand shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = Offset(c.x + 2.dp.toPx(), c.y + 2.dp.toPx()),
            end = Offset(c.x + (r * 0.5f) * cos(hA).toFloat() + 2.dp.toPx(), c.y + (r * 0.5f) * sin(hA).toFloat() + 2.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Hour hand
        drawLine(color = HaIvory, start = c, end = Offset(c.x + (r * 0.5f) * cos(hA).toFloat(), c.y + (r * 0.5f) * sin(hA).toFloat()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        
        // Minute hand
        drawLine(color = SoftAmber, start = c, end = Offset(c.x + (r * 0.75f) * cos(mA).toFloat(), c.y + (r * 0.75f) * sin(mA).toFloat()), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        
        // Center cap
        drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = c)
        drawCircle(color = SoftAmber, radius = 3.dp.toPx(), center = c)
    }
}

@Composable
fun MemoryChallengeView(rounds: Int, difficulty: Int, onComplete: () -> Unit) {
    var cur by remember { mutableIntStateOf(1) }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("ROUND $cur OF $rounds", color = HaJade, modifier = Modifier.padding(top = 20.dp))
        Box(modifier = Modifier.weight(1f), Alignment.Center) { key(cur) { MemoryTileGame(difficulty) { if (cur < rounds) cur++ else onComplete() } } }
    }
}

@SuppressLint("NewApi")
@Composable
fun MemoryTileGame(difficulty: Int, onComplete: () -> Unit) {
    val gridSize = when (difficulty) {
        0 -> 3
        1 -> 3
        2 -> 4
        else -> 5
    }
    val sequenceLength = when (difficulty) {
        0 -> 4
        1 -> 6
        2 -> 8
        else -> 12
    }
    val totalTiles = gridSize * gridSize

    var gameIteration by remember { mutableIntStateOf(0) }
    var sequence by remember(gameIteration) { 
        mutableStateOf(List(sequenceLength) { Random.nextInt(totalTiles) }) 
    }
    var highlightedTile by remember { mutableStateOf<Int?>(null) }
    var userSequence by remember { mutableStateOf(listOf<Int>()) }
    var isShowingSequence by remember { mutableStateOf(true) }
    var isResetting by remember { mutableStateOf(false) }

    LaunchedEffect(gameIteration) {
        delay(1000.milliseconds)
        isShowingSequence = true
        isResetting = false
        userSequence = emptyList()
        
        for (index in sequence) {
            highlightedTile = index
            delay(450.milliseconds)
            highlightedTile = null
            delay(150.milliseconds)
        }
        isShowingSequence = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        // --- High-End Typography & State Info ---
        Text(
            text = if (isResetting) "TRY AGAIN!" else if (isShowingSequence) "WATCH CLOSELY" else "YOUR TURN", 
            color = if (isResetting) HaCoral else if (isShowingSequence) SoftAmber else HaJade, 
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isShowingSequence) "Memorize the pattern" else "Tap the tiles in order (${userSequence.size}/${sequence.size})",
            color = HaIvory.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- Premium Memory Grid ---
        Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (y in 0 until gridSize) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (x in 0 until gridSize) {
                            val tileIndex = y * gridSize + x
                            val isHighlighted = highlightedTile == tileIndex
                            
                            // Better highlight: check if this index is the next one we are looking for in sequence
                            val isCurrentlyFlashing = isHighlighted
                            
                            Surface(
                                onClick = {
                                    if (!isShowingSequence && !isResetting) {
                                        if (sequence[userSequence.size] == tileIndex) {
                                            userSequence += tileIndex
                                            if (userSequence.size == sequence.size) {
                                                onComplete()
                                            }
                                        } else {
                                            // Wrong Tap - Instant Reset
                                            isResetting = true
                                            gameIteration++
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        val s = if (isCurrentlyFlashing) 1.05f else 1.0f
                                        scaleX = s
                                        scaleY = s
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isCurrentlyFlashing -> SoftAmber
                                    isResetting -> HaCoral.copy(alpha = 0.1f)
                                    else -> HaIvory.copy(alpha = 0.05f)
                                },
                                border = BorderStroke(
                                    width = if (isCurrentlyFlashing) 2.dp else 1.dp,
                                    color = if (isCurrentlyFlashing) HaIvory.copy(alpha = 0.5f) else HaIvory.copy(alpha = 0.1f)
                                )
                            ) {
                                // Subtle glow effect for highlighted tiles
                                if (isCurrentlyFlashing) {
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(HaIvory.copy(alpha = 0.3f), Color.Transparent))))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun ShakeChallengeView(targetShakes: Int, onComplete: () -> Unit) {
    val ctx = LocalContext.current
    var count by remember { mutableIntStateOf(0) }
    
    val progress = remember(count, targetShakes) {
        (count.toFloat() / targetShakes).coerceIn(0f, 1f)
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    DisposableEffect(Unit) { 
        val d = ShakeDetector(ctx) { 
            count = it
        }
        d.start()
        onDispose { d.stop() }
    }
    
    LaunchedEffect(count) {
        if (count >= targetShakes) {
            delay(300.milliseconds)
            onComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Background Glow
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(SoftAmber.copy(alpha = 0.1f), Color.Transparent)
                        )
                    )
            )
            
            // Progress Ring
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(220.dp),
                color = if (count >= targetShakes) HaJade else SoftAmber,
                strokeWidth = 12.dp,
                trackColor = HaIvory.copy(alpha = 0.05f),
                strokeCap = StrokeCap.Round
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = SoftAmber,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$count",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = HaIvory
                )
                Text(
                    text = "OF $targetShakes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HaMuted,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "SHAKE TO DISMISS",
            color = SoftAmber,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Keep shaking your phone vigorously",
            color = HaIvory.copy(alpha = 0.4f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@SuppressLint("NewApi")
@Composable
fun LightsOnChallengeView(targetLux: Int, onComplete: () -> Unit) {
    val context = LocalContext.current
    var cur by remember { mutableFloatStateOf(0f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val sunScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    LaunchedEffect(Unit) {
        LightSensorHelper.getLuxFlow(context).collect { 
            cur = it
            if (it >= targetLux) onComplete() 
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            // Background glow
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(SoftAmber.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )
            
            // Animated Sun Icon
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = SoftAmber,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = sunScale
                        scaleY = sunScale
                        rotationZ = rotation
                    }
                    .shadow(if (cur > 10) 20.dp else 0.dp, CircleShape, ambientColor = SoftAmber, spotColor = SoftAmber)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "TURN ON THE LIGHTS",
            color = SoftAmber,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${cur.toInt()}",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = if (cur >= targetLux) HaJade else HaIvory
            )
            Text(
                text = " / $targetLux LX",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HaIvory.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 8.dp, top = 12.dp)
            )
        }
        
        if (cur < targetLux) {
            Text(
                text = "Brightness required to dismiss",
                color = HaIvory.copy(alpha = 0.3f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun ChargerChallengeView(targetCount: Int, onComplete: () -> Unit) {
    val context = LocalContext.current
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    var done by remember { mutableIntStateOf(0) }
    var unplugged by remember { mutableStateOf(!bm.isCharging) }
    val req = if (targetCount == 0) 1 else targetCount
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "alpha"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "float"
    )

    DisposableEffect(Unit) {
        val rec = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.action == Intent.ACTION_POWER_CONNECTED && unplugged) {
                    done++
                    unplugged = false
                    if (done >= req) onComplete()
                } else if (i.action == Intent.ACTION_POWER_DISCONNECTED) {
                    unplugged = true
                }
            }
        }
        context.registerReceiver(rec, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        })
        onDispose { context.unregisterReceiver(rec) }
    }

    Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // --- Premium Animated Charger Icon ---
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer { translationY = floatOffset }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SoftAmber.copy(alpha = 0.25f * pulseAlpha), 
                                SoftAmber.copy(alpha = 0.05f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // --- Clean Vector Port Representation ---
                if (unplugged) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 35.dp)
                            .size(60.dp, 10.dp)
                            .background(HaIvory.copy(alpha = 0.05f), RoundedCornerShape(5.dp))
                            .border(1.5.dp, SoftAmber.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                    )
                }

                // --- Professional Icon (Power/Bolt) ---
                Icon(
                    imageVector = if (unplugged) Icons.Default.Power else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = SoftAmber,
                    modifier = Modifier
                        .size(if (unplugged) 72.dp else 92.dp)
                        .graphicsLayer { 
                            alpha = pulseAlpha
                            translationY = if (unplugged) 12f else 0f 
                        }
                )
                
                // Pulsing rings
                repeat(2) { i ->
                    val ringPulse by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 2f,
                        animationSpec = infiniteRepeatable(tween(2000, delayMillis = i * 1000), RepeatMode.Restart),
                        label = "ring"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = ringPulse; scaleY = ringPulse; alpha = (2f - ringPulse).coerceIn(0f, 1f) * 0.3f }
                            .border(2.dp, SoftAmber, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- Instructional Text Guide ---
            Text(
                text = if (!unplugged) "UNPLUG CHARGER FIRST" else "PLUG IN CHARGER",
                color = if (!unplugged) HaCoral.copy(0.8f) else SoftAmber,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Progress: $done / $req",
                color = HaIvory.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (unplugged) {
                Text(
                    text = "Insert cable to proceed",
                    color = HaIvory.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MazeChallengeView(rounds: Int, difficulty: Int, onComplete: () -> Unit) {
    var cur by remember { mutableIntStateOf(1) }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("ROUND $cur OF $rounds", color = HaJade, modifier = Modifier.padding(top = 20.dp))
        Box(modifier = Modifier.weight(1f), Alignment.Center) { key(cur) { MazeGameView(difficulty) { if (cur < rounds) cur++ else onComplete() } } }
    }
}

@Composable
fun MazeGameView(difficulty: Int, onComplete: () -> Unit) {
    val size = when (difficulty) {
        0 -> 9   // Easy: Slightly larger
        1 -> 15  // Medium: Complex
        2 -> 21  // Hard: Very Complex
        else -> 31 // Extreme: Massive Intricacy
    }
    
    val mazeData = remember { generateMaze(size) }
    var playerPos by remember { mutableStateOf(IntOffset(0, 0)) }
    val pathHistory = remember { mutableStateListOf(IntOffset(0, 0)) }
    
    // Smooth movement animation
    val animatedX by animateFloatAsState(targetValue = playerPos.x.toFloat(), animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    val animatedY by animateFloatAsState(targetValue = playerPos.y.toFloat(), animationSpec = spring(stiffness = Spring.StiffnessMediumLow))

    fun resetToStart() {
        playerPos = IntOffset(0, 0)
        pathHistory.clear()
        pathHistory.add(IntOffset(0, 0))
    }

    fun applyPenalty() {
        repeat(2) {
            if (pathHistory.size > 1) {
                pathHistory.removeAt(pathHistory.size - 1)
            }
        }
        playerPos = pathHistory.last()
    }

    fun move(nx: Int, ny: Int, isJump: Boolean = false) {
        if (nx !in 0 until size || ny !in 0 until size) return 

        if (isJump) {
            applyPenalty()
            return
        }

        if (mazeData[ny][nx] == 1) {
            resetToStart() // Wall collision reset
        } else {
            val newPos = IntOffset(nx, ny)
            if (newPos != playerPos) {
                playerPos = newPos
                pathHistory.add(newPos)
                if (nx == size - 1 && ny == size - 1) onComplete()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .background(HaIvory.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                .border(1.dp, HaIvory.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(4.dp)
                .pointerInput(size) {
                    detectTapGestures { offset ->
                        // Correct size retrieval in PointerInputScope
                        val cellSizePx = this.size.width.toFloat() / size
                        val tx = (offset.x / cellSizePx).toInt().coerceIn(0, size - 1)
                        val ty = (offset.y / cellSizePx).toInt().coerceIn(0, size - 1)
                        
                        val dist = abs(tx - playerPos.x) + abs(ty - playerPos.y)
                        if (dist == 1) {
                            move(tx, ty)
                        } else if (dist > 1) {
                            move(tx, ty, isJump = true)
                        }
                    }
                }
        ) {
            val constraintsScope = this
            val cellSize = constraintsScope.maxWidth / size
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        if (mazeData[y][x] == 1) {
                            drawRect(
                                color = HaIvory.copy(alpha = 0.2f),
                                topLeft = Offset(x * cellSize.toPx(), y * cellSize.toPx()),
                                size = Size(cellSize.toPx() + 0.5f, cellSize.toPx() + 0.5f)
                            )
                        }
                    }
                }
                
                val paint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = Paint.Align.CENTER
                    textSize = cellSize.toPx() * 0.6f
                    typeface = Typeface.DEFAULT_BOLD
                }
                
                drawContext.canvas.nativeCanvas.drawText("S", 0.5f * cellSize.toPx(), 0.75f * cellSize.toPx(), paint)
                
                paint.color = "#4ADE80".toColorInt()
                drawContext.canvas.nativeCanvas.drawText("E", (size - 0.5f) * cellSize.toPx(), (size - 0.25f) * cellSize.toPx(), paint)
            }
            
            Box(
                modifier = Modifier
                    .size(cellSize * 0.85f)
                    .graphicsLayer {
                        translationX = animatedX * cellSize.toPx() + (cellSize.toPx() * 0.075f)
                        translationY = animatedY * cellSize.toPx() + (cellSize.toPx() * 0.075f)
                    }
                    .background(SoftAmber, RoundedCornerShape(4.dp))
                    .border(1.dp, HaIvory.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControlArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft) { move(playerPos.x - 1, playerPos.y) }
            Column {
                ControlArrow(Icons.Default.KeyboardArrowUp) { move(playerPos.x, playerPos.y - 1) }
                Spacer(modifier = Modifier.height(8.dp))
                ControlArrow(Icons.Default.KeyboardArrowDown) { move(playerPos.x, playerPos.y + 1) }
            }
            ControlArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight) { move(playerPos.x + 1, playerPos.y) }
        }
        
        Text(
            text = "Tap to move • Hit walls = Reset",
            color = HaMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ControlArrow(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(6.dp).size(60.dp),
        shape = RoundedCornerShape(16.dp),
        color = HaIvory.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = SoftAmber, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun SpellBeeChallengeView(rounds: Int, difficulty: Int, onComplete: () -> Unit) {
    var cur by remember { mutableIntStateOf(1) }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("ROUND $cur OF $rounds", color = HaJade, modifier = Modifier.padding(top = 20.dp))
        Box(modifier = Modifier.weight(1f), Alignment.Center) { key(cur) { SpellBeeGame(difficulty) { if (cur < rounds) cur++ else onComplete() } } }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@SuppressLint("NewApi")
@Composable
fun SpellBeeGame(difficulty: Int, onComplete: () -> Unit) {
    val word = remember { generateDifficultyWord(difficulty) }
    val jumbledWord = remember { jumbleWord(word) }
    var input by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(isError) {
        if (isError) {
            delay(500.milliseconds)
            isError = false
            input = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "UNSCRAMBLE THE WORD", 
            color = SoftAmber, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Jumbled hint
        Surface(
            color = HaIvory.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
        ) {
            Text(
                text = jumbledWord,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HaIvory.copy(alpha = 0.6f),
                letterSpacing = 4.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Input slots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            word.forEachIndexed { index, _ ->
                val char = input.getOrNull(index)?.toString() ?: ""
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = 32.dp, height = 44.dp)
                        .background(
                            if (isError) HaCoral.copy(alpha = 0.1f) else HaIvory.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp, 
                            if (isError) HaCoral else if (char.isNotEmpty()) SoftAmber else HaIvory.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        color = HaIvory,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (char.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .size(12.dp, 2.dp)
                                .background(HaIvory.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        // Premium Custom Keyboard
        val availableLetters = remember { jumbledWord.toList().sorted().distinct() }
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            availableLetters.forEach { char ->
                Surface(
                    onClick = { 
                        if (input.length < word.length && !isError) {
                            input += char
                        }
                    },
                    modifier = Modifier.padding(4.dp).size(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = HaIvory.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(char.toString(), color = HaIvory, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Delete key
            Surface(
                onClick = { if (input.isNotEmpty() && !isError) input = input.dropLast(1) },
                modifier = Modifier.padding(4.dp).size(width = 70.dp, height = 50.dp),
                shape = RoundedCornerShape(12.dp),
                color = HaIvory.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = HaIvory.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { 
                if (input.equals(word, true)) {
                    onComplete()
                } else {
                    isError = true
                }
            },
            enabled = input.length == word.length && !isError,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SoftAmber, disabledContainerColor = SoftAmber.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("CHECK ANSWER", color = MidnightBlue, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PuzzleChallengeView(rounds: Int, difficulty: Int, onComplete: () -> Unit) {
    var cur by remember { mutableIntStateOf(1) }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = "ROUND $cur OF $rounds", 
            color = HaJade, 
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 20.dp)
        )
        Box(modifier = Modifier.weight(1f), Alignment.Center) { key(cur) { SlidingPuzzleGame(difficulty) { if (cur < rounds) cur++ else onComplete() } } }
    }
}

@Composable
fun SlidingPuzzleGame(difficulty: Int, onComplete: () -> Unit) {
    val gridSize = when (difficulty) {
        0 -> 2 // Easy: 2x2
        1 -> 3 // Medium: 3x3
        2 -> 4 // Hard: 4x4
        else -> 5 // Extreme: 5x5
    }
    
    var tiles by remember { mutableStateOf(generateShuffledPuzzle(gridSize)) }
    var moves by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        // --- Bold & Modern Typography ---
        Text(
            text = "SLIDE TO SOLVE", 
            color = SoftAmber, 
            fontSize = 16.sp, 
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Moves: $moves",
            color = HaIvory,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- Optimized Physics Grid ---
        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .background(HaIvory.copy(alpha = 0.02f), RoundedCornerShape(20.dp))
                .border(1.5.dp, HaIvory.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(10.dp)
        ) {
            val constraintsScope = this
            val cellSize = constraintsScope.maxWidth / gridSize
            
            tiles.forEachIndexed { index, value ->
                if (value != 0) {
                    val row = index / gridSize
                    val col = index % gridSize
                    
                    val targetX = cellSize * col
                    val targetY = cellSize * row
                    
                    // High-performance smooth animation physics
                    val animX by animateDpAsState(
                        targetValue = targetX, 
                        animationSpec = spring(
                            dampingRatio = 0.85f, // Smooth, slightly bouncy feel
                            stiffness = 400f      // Responsive but fluid
                        ),
                        label = "tileX"
                    )
                    val animY by animateDpAsState(
                        targetValue = targetY, 
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = 400f
                        ),
                        label = "tileY"
                    )

                    Surface(
                        onClick = { 
                            val emptyIndex = tiles.indexOf(0)
                            val emptyRow = emptyIndex / gridSize
                            val emptyCol = emptyIndex % gridSize
                            
                            if (abs(row - emptyRow) + abs(col - emptyCol) == 1) {
                                val newList = tiles.toMutableList()
                                newList[emptyIndex] = value
                                newList[index] = 0
                                tiles = newList.toList()
                                moves++
                                if (isPuzzleSolved(newList)) onComplete()
                            }
                        },
                        modifier = Modifier
                            .size(cellSize)
                            .offset(x = animX, y = animY)
                            .padding(4.dp)
                            ,
                        shape = RoundedCornerShape(16.dp),
                        color = HaIvory.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, SoftAmber.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = value.toString(),
                                color = SoftAmber,
                                fontSize = if (gridSize > 4) 24.sp else 34.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        TextButton(
            onClick = { 
                tiles = generateShuffledPuzzle(gridSize)
                moves = 0
            },
            colors = ButtonDefaults.textButtonColors(contentColor = HaIvory.copy(alpha = 0.4f))
        ) {
            Text("RESET GRID", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun ScanSinkChallengeView(mode: Int, @Suppress("UNUSED_PARAMETER") extra: String = "", onComplete: () -> Unit) {
    var hold by remember { mutableStateOf(false) }
    var prog by remember { mutableFloatStateOf(0f) }
    var det by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }

    LaunchedEffect(hold, det) {
        if (hold && det) {
            while (prog < 1f) {
                delay(40.milliseconds)
                prog = (prog + 0.06f).coerceAtMost(1f)
            }
        } else {
            prog = 0f
        }
    }

    LaunchedEffect(prog) {
        if (prog >= 1f && !completed) {
            completed = true
            onComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraChallenge(
            onSuccess = { res -> 
                @Suppress("UNCHECKED_CAST") val lbs = res as? List<ImageLabel> ?: return@CameraChallenge
                
                // --- Robust Recognition Pipeline ---
                val sinkRelated = listOf("sink", "basin", "washbasin", "tap", "faucet", "plumbing fixture", "bathroom", "kitchen sink", "plumbing")
                
                val bestMatch = lbs.filter { label -> 
                    sinkRelated.any { label.text.lowercase().contains(it) } 
                }.maxByOrNull { it.confidence }

                val isSpecific = mode == 1
                // Relaxed but still distinct thresholds for reliability
                val threshold = if (isSpecific) 0.50f else 0.28f 
                
                val isSinkDetected = bestMatch != null && bestMatch.confidence >= threshold
                
                if (isSinkDetected != det) {
                    det = isSinkDetected
                }
            }, 
            mode = "labeling"
        )
        
        // Haptic feedback on detection
        val context = LocalContext.current
        LaunchedEffect(det, hold) {
            if (det && hold) {
                VibrationUtils.vibrate(context, longArrayOf(0, 40))
            }
        }
        
        // --- Premium Scanning Overlay (Centered) ---
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                TargetingFrame(isDet = det && hold)
                
                if (hold) {
                    // Scanning Laser Effect
                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                    val laserY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
                        label = "y"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer { translationY = 280.dp.toPx() * laserY }
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, SoftAmber, Color.Transparent)
                                )
                            )
                            .shadow(8.dp, ambientColor = SoftAmber, spotColor = SoftAmber)
                    )
                    
                    Text(
                        text = if (det) "VERIFYING..." else "SCANNING...",
                        color = if (det) HaJade else SoftAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-30).dp)
                            .graphicsLayer { alpha = if (det) 1f else 0.6f }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Directional Guidance Header
            Surface(
                color = MidnightBlue.copy(alpha = 0.85f),
                shape = CircleShape,
                border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))
            ) {
                Text(
                    text = if (mode == 1) "SCAN REGISTERED SINK" else "FIND ANY SINK",
                    color = SoftAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    letterSpacing = 2.sp
                )
            }

            // Hidden middle spacer to maintain SpaceBetween layout
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (hold) (if (det) HaJade.copy(0.2f) else MidnightBlue.copy(0.8f)) else SoftAmber)
                    .border(2.dp, if (hold) (if (det) HaJade else HaIvory.copy(0.1f)) else Color.Transparent, RoundedCornerShape(24.dp))
                    .pointerInput(Unit) { detectTapGestures(onPress = { hold = true; try { awaitRelease() } finally { hold = false } }) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hold) (if (det) "VERIFIED MATCH!" else "CENTER THE SINK") else "HOLD TO SCAN",
                    color = if (hold) HaIvory else MidnightBlue,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("NewApi")
@Composable
fun ObjectChallengeView(mode: Int, extra: String, onComplete: () -> Unit) {
    var isScanStarted by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var isObjectInFrame by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    
    val pool = remember(extra) { extra.split(",").filter { it.isNotBlank() } }
    var targetObject by remember { mutableStateOf(if (mode == 0 && extra.isNotBlank()) extra else null) }

    if (mode == 1 && targetObject == null) {
        RandomObjectPicker(pool = pool) { targetObject = it }
    } else if (targetObject != null) {
        val finalTarget = targetObject!!
        
        LaunchedEffect(isScanStarted, isObjectInFrame) {
            if (isScanStarted && isObjectInFrame) {
                while (scanProgress < 1f) {
                    delay(30.milliseconds)
                    scanProgress = (scanProgress + 0.08f).coerceAtMost(1f)
                }
            }
        }

        LaunchedEffect(scanProgress) {
            if (scanProgress >= 1f && !completed) {
                completed = true
                onComplete()
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            CameraChallenge(
                onSuccess = { res ->
                    @Suppress("UNCHECKED_CAST") val lbs = res as? List<ImageLabel> ?: return@CameraChallenge
                    val kws = getObjectKeywords(finalTarget)
                    isObjectInFrame = lbs.any { label ->
                        val text = label.text.lowercase()
                        val target = finalTarget.lowercase()
                        
                        val threshold = when(target) {
                            "bottle" -> 0.28f 
                            "badminton" -> 0.22f 
                            "bed" -> 0.22f 
                            "laptop" -> 0.40f 
                            "phone" -> 0.42f 
                            "television" -> 0.50f 
                            "sky" -> 0.45f 
                            "dog" -> 0.65f 
                            else -> 0.35f
                        }
                        
                        val isDirectMatch = kws.any { kw -> text == kw || text.contains(kw) }
                        
                        val isFallbackMatch = when(target) {
                            "bed" -> text.contains("furniture") || text.contains("couch") || 
                                    text.contains("sofa") || text.contains("studio couch") || 
                                    text.contains("sleeping") || text.contains("cot") ||
                                    text.contains("bedroom") || text.contains("mattress")
                            "bottle" -> text.contains("container") || text.contains("vessel") || 
                                       text.contains("plastic") || text.contains("liquid") ||
                                       text.contains("drink") || text.contains("glass") ||
                                       text.contains("cylinder")
                            "laptop" -> (text.contains("notebook") || text.contains("computer") || 
                                       text.contains("keyboard") || text.contains("pc")) && 
                                       !text.contains("phone")
                            "phone" -> text.contains("handset") || text.contains("mobile device") || 
                                      text.contains("communication device") || 
                                      (text.contains("electronics") && label.confidence > 0.6f)
                            "television" -> (text.contains("monitor") || text.contains("television set") || 
                                            text.contains("flat screen") || text.contains("led television")) && 
                                            !text.contains("laptop") && !text.contains("phone")
                            "sky" -> (text.contains("cloud") || text.contains("atmosphere") || 
                                     text.contains("daylight") || text.contains("horizon") || 
                                     text.contains("outdoor")) && 
                                     !text.contains("wall") && !text.contains("indoor")
                            "dog" -> (text.contains("canine") || text.contains("pet")) && 
                                    !text.contains("cat") && !text.contains("human")
                            else -> false
                        }
                        
                        (isDirectMatch || isFallbackMatch) && label.confidence >= threshold
                    }
                },
                mode = "labeling"
            )

            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Surface(color = MidnightBlue.copy(alpha = 0.85f), shape = CircleShape, border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.1f))) { Text(text = "FIND: ${finalTarget.uppercase()}", color = SoftAmber, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), letterSpacing = 2.sp) }
                Box(contentAlignment = Alignment.Center) {
                    TargetingFrame(isDet = isObjectInFrame && isScanStarted)
                    if (isScanStarted) {
                        CircularProgressIndicator(progress = { scanProgress }, modifier = Modifier.size(180.dp), color = if (scanProgress >= 1f) HaJade else SoftAmber, strokeWidth = 10.dp, trackColor = HaIvory.copy(alpha = 0.1f), strokeCap = StrokeCap.Round)
                        Text(text = "${(scanProgress * 100).toInt()}%", color = HaIvory, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(24.dp)).combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isScanStarted) isScanStarted = true
                    },
                    color = if (isScanStarted) (if (isObjectInFrame) HaJade.copy(alpha = 0.2f) else MidnightBlue.copy(alpha = 0.85f)) else SoftAmber,
                    border = BorderStroke(2.dp, if (isScanStarted) (if (isObjectInFrame) HaJade else HaIvory.copy(alpha = 0.1f)) else Color.Transparent)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                !isScanStarted -> "TAP TO START SCAN"
                                scanProgress >= 1f -> "VERIFIED!"
                                isObjectInFrame -> "MATCH FOUND! HOLD..."
                                else -> "SEARCHING FOR ${finalTarget.uppercase()}..."
                            },
                            color = if (isScanStarted) (if (isObjectInFrame) HaJade else HaIvory) else MidnightBlue,
                            fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Rep challenges: Push-up & Squat (ML Kit Pose Detection + tested counters)
// ---------------------------------------------------------------------------

private val REP_LANDMARKS = intArrayOf(
    PoseLandmark.NOSE,
    PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
    PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
    PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
)

private val ARM_BONES = listOf(
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
    PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
    PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST
)

private val LEG_BONES = listOf(
    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
    PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
    PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
)

private val TORSO_BONES = listOf(
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP
)

/** Landmarks of one frame in upright-image pixels, for drawing the skeleton. */
private data class PoseOverlayFrame(
    val points: Map<Int, PushUpCounter.Point>,
    val imageWidth: Int,
    val imageHeight: Int,
    val mirrored: Boolean
)

private fun Pose.landmarkPoint(type: Int): PushUpCounter.Point? =
    getPoseLandmark(type)?.let { PushUpCounter.Point(it.position.x, it.position.y, it.inFrameLikelihood) }

private fun Pose.toPushUpBody() = PushUpCounter.Body(
    nose = landmarkPoint(PoseLandmark.NOSE),
    leftShoulder = landmarkPoint(PoseLandmark.LEFT_SHOULDER), rightShoulder = landmarkPoint(PoseLandmark.RIGHT_SHOULDER),
    leftElbow = landmarkPoint(PoseLandmark.LEFT_ELBOW), rightElbow = landmarkPoint(PoseLandmark.RIGHT_ELBOW),
    leftWrist = landmarkPoint(PoseLandmark.LEFT_WRIST), rightWrist = landmarkPoint(PoseLandmark.RIGHT_WRIST),
    leftHip = landmarkPoint(PoseLandmark.LEFT_HIP), rightHip = landmarkPoint(PoseLandmark.RIGHT_HIP),
    leftKnee = landmarkPoint(PoseLandmark.LEFT_KNEE), rightKnee = landmarkPoint(PoseLandmark.RIGHT_KNEE)
)

private fun Pose.toSquatBody() = SquatCounter.Body(
    leftShoulder = landmarkPoint(PoseLandmark.LEFT_SHOULDER), rightShoulder = landmarkPoint(PoseLandmark.RIGHT_SHOULDER),
    leftHip = landmarkPoint(PoseLandmark.LEFT_HIP), rightHip = landmarkPoint(PoseLandmark.RIGHT_HIP),
    leftKnee = landmarkPoint(PoseLandmark.LEFT_KNEE), rightKnee = landmarkPoint(PoseLandmark.RIGHT_KNEE),
    leftAnkle = landmarkPoint(PoseLandmark.LEFT_ANKLE), rightAnkle = landmarkPoint(PoseLandmark.RIGHT_ANKLE)
)

private fun PoseFrameData.toOverlay(): PoseOverlayFrame {
    val rotated = rotation == 90 || rotation == 270
    val points = HashMap<Int, PushUpCounter.Point>()
    for (type in REP_LANDMARKS) pose.landmarkPoint(type)?.let { points[type] = it }
    return PoseOverlayFrame(
        points = points,
        imageWidth = if (rotated) height else width,
        imageHeight = if (rotated) width else height,
        mirrored = isFront
    )
}

private fun PushUpCounter.Stage.toRepStage(): RepStage = when (this) {
    PushUpCounter.Stage.NO_BODY -> RepStage.NO_BODY
    PushUpCounter.Stage.NOT_IN_POSITION -> RepStage.NOT_IN_POSITION
    PushUpCounter.Stage.TOP -> RepStage.TOP
    PushUpCounter.Stage.LOWERING -> RepStage.LOWERING
    PushUpCounter.Stage.BOTTOM -> RepStage.BOTTOM
}

/** What the dashboard says for each stage (title, hint). */
private class RepCopy(
    val noBody: Pair<String, String>,
    val notInPosition: Pair<String, String>,
    val top: Pair<String, String>,
    val lowering: Pair<String, String>,
    val bottom: Pair<String, String>,
    val done: Pair<String, String> = "Challenge complete!" to "Great work — you're awake."
)

private val PUSHUP_COPY = RepCopy(
    noBody = "Show your arms" to "Shoulders, elbows and wrists must be visible.",
    notInPosition = "Get into position" to "Hands on the floor, body in a straight plank.",
    top = "Go down" to "Lower your chest toward the floor.",
    lowering = "Lower… keep going" to "Bend your elbows until the bar turns green.",
    bottom = "Push up!" to "Straighten your arms to count the rep."
)

private val SQUAT_CAMERA_COPY = RepCopy(
    noBody = "Show your whole body" to "Hips, knees and ankles must be visible.",
    notInPosition = "Stand up straight" to "Feet on the floor, facing or side-on to the camera.",
    top = "Squat down" to "Push your hips back and bend your knees.",
    lowering = "Lower… keep going" to "Go down until the bar turns green.",
    bottom = "Stand up!" to "Drive up to full standing to count the rep."
)

private val SQUAT_MOTION_COPY = RepCopy(
    noBody = "Lock your grip" to "Hold the phone at your chest, both thumbs on the pads.",
    notInPosition = "Lock your grip" to "Hold the phone at your chest, both thumbs on the pads.",
    top = "Squat down" to "Keep the phone at your chest and lower your hips.",
    lowering = "Lower… keep going" to "Go down until the bar turns green.",
    bottom = "Stand up!" to "Rise to full standing to count the rep."
)

@SuppressLint("NewApi")
@Composable
fun PushUpChallengeView(targetReps: Int, onComplete: () -> Unit) {
    val target = targetReps.coerceAtLeast(1)
    val counter = remember { PushUpCounter() }
    var reps by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(RepStage.NO_BODY) }
    var depth by remember { mutableFloatStateOf(0f) }
    var angle by remember { mutableStateOf<Float?>(null) }
    var overlay by remember { mutableStateOf<PoseOverlayFrame?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    var finished by remember { mutableStateOf(false) }
    val latestOnComplete by rememberUpdatedState(onComplete)
    val repPop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraChallenge(
            onSuccess = { res ->
                val data = res as? PoseFrameData ?: return@CameraChallenge
                if (finished) return@CameraChallenge
                val s = counter.process(data.pose.toPushUpBody(), android.os.SystemClock.elapsedRealtime())
                reps = s.reps; stage = s.stage.toRepStage(); depth = s.depth; angle = s.elbowAngle
                overlay = data.toOverlay()
                if (s.repJustCounted) {
                    VibrationUtils.vibrate(context, longArrayOf(0, 60))
                    scope.launch {
                        repPop.animateTo(1.18f, tween(120))
                        repPop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    if (s.reps >= target && !finished) {
                        finished = true
                        scope.launch { delay(450.milliseconds); latestOnComplete() }
                    }
                }
            },
            mode = "pose",
            cameraSelector = cameraSelector
        )

        PoseSkeletonOverlay(
            frame = overlay,
            highlightBones = ARM_BONES,
            highlightJoints = setOf(PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW),
            active = stage == RepStage.BOTTOM,
            modifier = Modifier.fillMaxSize()
        )

        RepCameraChrome(
            title = "PUSH-UP CHALLENGE",
            icon = PushUpIcon,
            kind = ExerciseKind.PushUp,
            onFlip = {
                counter.reset(keepReps = true)
                overlay = null
                stage = RepStage.NO_BODY
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            },
            showTips = stage == RepStage.NO_BODY && !finished,
            tips = listOf(
                Icons.Default.PhoneAndroid to "Stand the phone on the floor, 1–2 m to your side — best accuracy",
                Icons.Default.Face to "Or lay it in front of your face with the front camera",
                Icons.Default.LightMode to "Good light helps the camera see your arms"
            )
        ) {
            RepDashboard(
                reps = reps, target = target, stage = stage, depth = depth,
                metric = angle?.let { "Elbow ${it.toInt()}°" },
                copy = PUSHUP_COPY, finished = finished, popScale = repPop.value
            )
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun SquatChallengeView(targetReps: Int, detectionMode: Int = 0, onComplete: () -> Unit) {
    if (detectionMode == 0) {
        SquatCameraChallengeView(targetReps, onComplete)
    } else {
        SquatMotionChallengeView(targetReps, onComplete)
    }
}

@SuppressLint("NewApi")
@Composable
fun SquatCameraChallengeView(targetReps: Int, onComplete: () -> Unit) {
    val target = targetReps.coerceAtLeast(1)
    val counter = remember { SquatCounter() }
    var reps by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(RepStage.NO_BODY) }
    var depth by remember { mutableFloatStateOf(0f) }
    var angle by remember { mutableStateOf<Float?>(null) }
    var overlay by remember { mutableStateOf<PoseOverlayFrame?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    var finished by remember { mutableStateOf(false) }
    val latestOnComplete by rememberUpdatedState(onComplete)
    val repPop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraChallenge(
            onSuccess = { res ->
                val data = res as? PoseFrameData ?: return@CameraChallenge
                if (finished) return@CameraChallenge
                val s = counter.process(data.pose.toSquatBody(), android.os.SystemClock.elapsedRealtime())
                reps = s.reps; stage = s.stage; depth = s.depth; angle = s.kneeAngle
                overlay = data.toOverlay()
                if (s.repJustCounted) {
                    VibrationUtils.vibrate(context, longArrayOf(0, 60))
                    scope.launch {
                        repPop.animateTo(1.18f, tween(120))
                        repPop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    if (s.reps >= target && !finished) {
                        finished = true
                        scope.launch { delay(450.milliseconds); latestOnComplete() }
                    }
                }
            },
            mode = "pose",
            cameraSelector = cameraSelector
        )

        PoseSkeletonOverlay(
            frame = overlay,
            highlightBones = LEG_BONES,
            highlightJoints = setOf(PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE),
            active = stage == RepStage.BOTTOM,
            modifier = Modifier.fillMaxSize()
        )

        RepCameraChrome(
            title = "SQUAT CHALLENGE",
            icon = SquatIcon,
            kind = ExerciseKind.Squat,
            onFlip = {
                counter.reset(keepReps = true)
                overlay = null
                stage = RepStage.NO_BODY
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            },
            showTips = stage == RepStage.NO_BODY && !finished,
            tips = listOf(
                Icons.Default.PhoneAndroid to "Stand the phone up 2–3 m away so your whole body fits",
                Icons.Default.Accessibility to "Side-on is most accurate; facing the camera works too",
                Icons.Default.LightMode to "Good light helps the camera see your legs"
            )
        ) {
            RepDashboard(
                reps = reps, target = target, stage = stage, depth = depth,
                metric = angle?.let { "Knee ${it.toInt()}°" },
                copy = SQUAT_CAMERA_COPY, finished = finished, popScale = repPop.value
            )
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun SquatMotionChallengeView(targetReps: Int, onComplete: () -> Unit) {
    val target = targetReps.coerceAtLeast(1)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnComplete by rememberUpdatedState(onComplete)

    val counter = remember { SquatMotionCounter() }
    var reps by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(RepStage.TOP) }
    var depth by remember { mutableFloatStateOf(0f) }
    var finished by remember { mutableStateOf(false) }
    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }
    val isGripLocked = isLeftPressed && isRightPressed
    val repPop = remember { Animatable(1f) }

    // Voice guidance
    val tts = remember {
        var t: TextToSpeech? = null
        t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) t?.language = Locale.US
        }
        t
    }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }
    var lastGuidanceTime by remember { mutableLongStateOf(0L) }
    fun speak(text: String) {
        val now = System.currentTimeMillis()
        if (now - lastGuidanceTime > 3000) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastGuidanceTime = now
        }
    }

    LaunchedEffect(isLeftPressed, isRightPressed) {
        if (!isGripLocked) {
            when {
                !isLeftPressed && !isRightPressed -> speak("Place both thumbs on the side pads to lock your grip")
                !isLeftPressed -> speak("Now press your left thumb to lock.")
                !isRightPressed -> speak("Now press your right thumb to lock.")
            }
        } else {
            speak("Grip locked. Start your squats.")
            VibrationUtils.vibrate(context, longArrayOf(0, 50, 50, 50))
        }
    }

    // Motion sensors: world-vertical acceleration, only while the grip is locked
    DisposableEffect(isGripLocked) {
        if (!isGripLocked || finished) {
            onDispose { }
        } else {
            counter.reset(keepReps = true)
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
            val linSensor = sm.getDefaultSensor(android.hardware.Sensor.TYPE_LINEAR_ACCELERATION)
            val gravSensor = sm.getDefaultSensor(android.hardware.Sensor.TYPE_GRAVITY)
            val accSensor = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            val useFused = linSensor != null && gravSensor != null
            val gravity = floatArrayOf(0f, 0f, 9.81f)
            var haveGravity = false
            val linear = FloatArray(3)

            val listener = object : android.hardware.SensorEventListener {
                override fun onSensorChanged(e: android.hardware.SensorEvent) {
                    if (finished) return
                    when (e.sensor.type) {
                        android.hardware.Sensor.TYPE_GRAVITY -> {
                            gravity[0] = e.values[0]; gravity[1] = e.values[1]; gravity[2] = e.values[2]
                            haveGravity = true
                            return
                        }
                        android.hardware.Sensor.TYPE_LINEAR_ACCELERATION -> {
                            if (!haveGravity) return
                            linear[0] = e.values[0]; linear[1] = e.values[1]; linear[2] = e.values[2]
                        }
                        android.hardware.Sensor.TYPE_ACCELEROMETER -> {
                            // Fallback: estimate gravity with a low-pass filter
                            val k = 0.92f
                            for (i in 0..2) {
                                gravity[i] = if (haveGravity) k * gravity[i] + (1 - k) * e.values[i] else e.values[i]
                                linear[i] = e.values[i] - gravity[i]
                            }
                            haveGravity = true
                        }
                        else -> return
                    }
                    val s = counter.process(linear, gravity, e.timestamp)
                    reps = s.reps; stage = s.stage; depth = s.depth
                    if (s.repJustCounted) {
                        VibrationUtils.vibrate(context, longArrayOf(0, 60))
                        scope.launch {
                            repPop.animateTo(1.18f, tween(120))
                            repPop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        if (s.reps >= target && !finished) {
                            finished = true
                            scope.launch { delay(450.milliseconds); latestOnComplete() }
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
            }
            val rate = android.hardware.SensorManager.SENSOR_DELAY_GAME
            if (useFused) {
                sm.registerListener(listener, gravSensor, rate)
                sm.registerListener(listener, linSensor, rate)
            } else if (accSensor != null) {
                sm.registerListener(listener, accSensor, rate)
            }
            onDispose { sm.unregisterListener(listener) }
        }
    }

    val shownStage = if (isGripLocked || finished) stage else RepStage.NOT_IN_POSITION

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0B0F), HaObsidian, Color(0xFF050507))))
    ) {
        // --- 3D thumb pads (whole left / right halves) ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GripPad(
                label = "LEFT THUMB",
                pressed = isLeftPressed,
                locked = isGripLocked,
                rotation = -90f,
                onPressChange = { isLeftPressed = it },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GripPad(
                label = "RIGHT THUMB",
                pressed = isRightPressed,
                locked = isGripLocked,
                rotation = 90f,
                onPressChange = { isRightPressed = it },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // --- Header + dashboard (no touch handling, so the pads stay usable) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            RepTitlePill("SQUAT CHALLENGE", SquatIcon, ExerciseKind.Squat)
            Spacer(modifier = Modifier.weight(1f))
            RepDashboard(
                reps = reps, target = target, stage = shownStage, depth = if (isGripLocked) depth else 0f,
                metric = if (isGripLocked) "Grip locked" else "Hold both pads",
                copy = SQUAT_MOTION_COPY, finished = finished, popScale = repPop.value
            )
        }
    }
}

@Composable
private fun GripPad(
    label: String,
    pressed: Boolean,
    locked: Boolean,
    rotation: Float,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    val pulse = rememberInfiniteTransition(label = "pad_pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pad_pulse_scale"
    )
    val tone = when {
        locked -> HaTone.Jade
        pressed -> HaTone.Gold
        else -> HaTone.Steel
    }
    Box(
        modifier = modifier
            .then(
                if (pressed) Modifier.ha3DPanelSelected(shape, elevation = 4.dp)
                else Modifier.ha3DBlack(shape, elevation = 14.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChange(true)
                        try { awaitRelease() } finally { onPressChange(false) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                rotationZ = rotation
                if (!pressed) { scaleX = pulseScale; scaleY = pulseScale }
            }
        ) {
            Box(
                modifier = Modifier.size(if (pressed) 64.dp else 56.dp).ha3DCoin(tone, elevation = if (pressed) 4.dp else 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TouchApp, null,
                    tint = haToneInk(tone),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (locked) "GRIP LOCKED" else if (pressed) "HOLDING…" else label,
                color = when {
                    locked -> HaJade
                    pressed -> HaGoldLight
                    else -> HaIvory.copy(alpha = 0.45f)
                },
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun RepTitlePill(title: String, icon: ImageVector, kind: ExerciseKind? = null) {
    Row(
        modifier = Modifier
            .ha3DBlack(CircleShape, elevation = 10.dp)
            .padding(start = 6.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).ha3DCoin(HaTone.Gold, elevation = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (kind != null) {
                AnimatedExerciseIcon(
                    kind = kind,
                    colors = listOf(Color(0xFF4A3614), Color(0xFF2A1F0E), Color(0xFF140E05)),
                    modifier = Modifier.size(26.dp),
                    showFloor = false
                )
            } else {
                Icon(icon, null, tint = haToneInk(HaTone.Gold), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = HaGoldLight, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
    }
}

/** Header, vignette, set-up tips and a slot for the dashboard over a camera feed. */
@Composable
private fun RepCameraChrome(
    title: String,
    icon: ImageVector,
    kind: ExerciseKind? = null,
    onFlip: () -> Unit,
    showTips: Boolean,
    tips: List<Pair<ImageVector, String>>,
    dashboard: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RepTitlePill(title, icon, kind)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onFlip, modifier = Modifier.size(46.dp).ha3DOrb()) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = HaGoldLight)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            AnimatedVisibility(
                visible = showTips,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .ha3DPanelDeep(RoundedCornerShape(24.dp), elevation = 16.dp)
                        .padding(18.dp)
                ) {
                    Text("Set up your phone", color = HaIvory, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    tips.forEach { (tipIcon, text) ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(28.dp).ha3DOrb(), contentAlignment = Alignment.Center) {
                                Icon(tipIcon, null, tint = HaGoldLight, modifier = Modifier.size(15.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text, color = HaIvory.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
            dashboard()
        }
    }
}

@Composable
private fun RepDashboard(
    reps: Int,
    target: Int,
    stage: RepStage,
    depth: Float,
    metric: String?,
    copy: RepCopy,
    finished: Boolean,
    popScale: Float
) {
    val shown = reps.coerceAtMost(target)
    val progress by animateFloatAsState(shown / target.toFloat(), tween(400), label = "rep_progress")
    val animDepth by animateFloatAsState(depth, tween(90), label = "rep_depth")

    val (tone, text) = when {
        finished -> HaTone.Jade to copy.done
        stage == RepStage.NO_BODY -> HaTone.Coral to copy.noBody
        stage == RepStage.NOT_IN_POSITION -> HaTone.Coral to copy.notInPosition
        stage == RepStage.BOTTOM -> HaTone.Jade to copy.bottom
        stage == RepStage.LOWERING -> HaTone.Gold to copy.lowering
        else -> HaTone.Gold to copy.top
    }
    val statusIcon = when (tone) {
        HaTone.Jade -> Icons.Default.Check
        HaTone.Coral -> Icons.Default.PriorityHigh
        else -> Icons.Default.ArrowDownward
    }
    val reachedDepth = stage == RepStage.BOTTOM

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ha3DPanelDeep(RoundedCornerShape(30.dp), elevation = 20.dp)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer { scaleX = popScale; scaleY = popScale },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = 11.dp.toPx()
                val inset = sw / 2 + 2.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val tl = Offset(inset, inset)
                drawArc(
                    brush = Brush.verticalGradient(listOf(Color(0xFF050507), Color(0xFF1A1A20))),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = tl, size = arcSize, style = Stroke(sw)
                )
                if (progress > 0f) {
                    drawArc(
                        brush = Brush.verticalGradient(
                            if (finished) listOf(HaJadeLight, HaJade, HaJadeDeep)
                            else listOf(HaGoldPale, HaGold, HaGoldDeep)
                        ),
                        startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = tl, size = arcSize, style = Stroke(sw, cap = StrokeCap.Round)
                    )
                }
            }
            Box(
                modifier = Modifier.size(88.dp).ha3DBlack(CircleShape, elevation = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$reps", color = HaIvory, fontSize = 38.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp)
                    Text("of $target", color = HaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(26.dp).ha3DCoin(tone, elevation = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(statusIcon, null, tint = haToneInk(tone), modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text.first,
                    color = HaIvory, fontWeight = FontWeight.Black, fontSize = 16.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text.second,
                color = HaMuted, fontSize = 11.sp, lineHeight = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DEPTH", color = HaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.weight(1f))
                if (metric != null) {
                    Text(metric, color = HaGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .ha3DInset(CircleShape)
            ) {
                val w = maxWidth
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(w * animDepth.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (reachedDepth) listOf(HaJadeLight, HaJade, HaJadeDeep)
                                else listOf(HaGoldPale, HaGold, HaGoldDeep)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .offset(x = w * 0.60f)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(HaIvory.copy(alpha = 0.55f))
                )
            }
        }
    }
}

@Composable
private fun PoseSkeletonOverlay(
    frame: PoseOverlayFrame?,
    highlightBones: List<Pair<Int, Int>>,
    highlightJoints: Set<Int>,
    active: Boolean,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val f = frame ?: return@Canvas
        if (f.imageWidth <= 0 || f.imageHeight <= 0) return@Canvas
        // PreviewView uses FILL_CENTER: scale to cover, then centre-crop.
        val scale = max(size.width / f.imageWidth, size.height / f.imageHeight)
        val dx = (size.width - f.imageWidth * scale) / 2f
        val dy = (size.height - f.imageHeight * scale) / 2f
        fun map(p: PushUpCounter.Point): Offset {
            var x = p.x * scale + dx
            val y = p.y * scale + dy
            if (f.mirrored) x = size.width - x
            return Offset(x, y)
        }
        fun ok(type: Int) = f.points[type]?.let { it.likelihood >= PushUpCounter.MIN_LIKELIHOOD } == true

        val dim = (TORSO_BONES + ARM_BONES + LEG_BONES).filter { it !in highlightBones }
        for ((a, b) in dim) {
            if (ok(a) && ok(b)) {
                drawLine(
                    color = HaIvory.copy(alpha = 0.45f),
                    start = map(f.points[a]!!), end = map(f.points[b]!!),
                    strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
                )
            }
        }
        val hot = if (active) HaJade else HaGold
        for ((a, b) in highlightBones) {
            if (ok(a) && ok(b)) {
                val s = map(f.points[a]!!); val e = map(f.points[b]!!)
                drawLine(color = Color.Black.copy(alpha = 0.5f), start = s, end = e, strokeWidth = 11.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = hot, start = s, end = e, strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        for (type in REP_LANDMARKS) {
            if (!ok(type) || type == PoseLandmark.NOSE) continue
            val c = map(f.points[type]!!)
            val key = type in highlightJoints
            val r = if (key) 9.dp.toPx() else 6.dp.toPx()
            drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = r + 2.dp.toPx(), center = c)
            drawCircle(
                brush = Brush.verticalGradient(
                    if (key) listOf(HaGoldPale, hot, HaGoldDeep) else listOf(Color.White, HaIvory, HaMuted),
                    startY = c.y - r, endY = c.y + r
                ),
                radius = r, center = c
            )
        }
    }
}

@Composable
fun QRCodeChallengeView(mode: Int, targetQR: String, onComplete: () -> Unit) {
    var showError by remember { mutableStateOf(false) }
    
    LaunchedEffect(showError) {
        if (showError) {
            delay(2000.milliseconds)
            showError = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CameraChallenge(onSuccess = { res -> 
            @Suppress("UNCHECKED_CAST")
            val barcodes = res as? List<Barcode> ?: return@CameraChallenge
            if (barcodes.isNotEmpty()) {
                if (mode == 0) {
                    onComplete()
                } else {
                    // Specific QR mode - Support up to 5 saved QRs
                    val detectedValue = barcodes.firstOrNull()?.rawValue
                    val savedQRs = targetQR.split("|").filter { it.isNotBlank() }
                    
                    if (detectedValue != null && savedQRs.contains(detectedValue)) {
                        onComplete()
                    } else if (detectedValue != null) {
                        showError = true
                    }
                }
            }
        }, mode = "barcode")
        
        Box(
            modifier = Modifier
                .size(260.dp)
                .border(
                    width = 2.dp, 
                    color = if (showError) HaCoral else SoftAmber.copy(alpha = 0.6f), 
                    shape = RoundedCornerShape(12.dp)
                )
        )
        
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (mode == 1) "SCAN SPECIFIC QR" else "SCAN ANY QR",
                color = HaIvory,
                fontWeight = FontWeight.Black
            )
            
            AnimatedVisibility(visible = showError) {
                Text(
                    text = "WRONG QR CODE",
                    color = HaCoral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun ReverseTypingChallengeView(rounds: Int, onComplete: () -> Unit) {
    var cur by remember { mutableIntStateOf(1) }
    val sentences = remember {
        listOf(
            "The quick brown fox jumps",
            "Life is what happens to you",
            "Believe you can and you are halfway",
            "Keep your face always toward sunshine",
            "Dream big and dare to fail",
            "Be the change you wish to see",
            "Stay hungry stay foolish",
            "Every moment is a fresh beginning",
            "Focus on the journey not destination",
            "Your limitation is only your imagination"
        ).shuffled()
    }
    
    var target by remember(cur) { mutableStateOf(sentences[cur % sentences.size]) }
    val reversedTarget = remember(target) { target.reversed() }
    var input by remember { mutableStateOf("") }
    var isWrong by remember { mutableStateOf(false) }
    
    LaunchedEffect(isWrong) {
        if (isWrong) {
            delay(1500.milliseconds)
            isWrong = false
            input = ""
        }
    }
    
    LaunchedEffect(input) {
        if (input.length >= reversedTarget.length && !isWrong) {
            // Case-insensitive validation
            if (input.trim().equals(reversedTarget.trim(), ignoreCase = true)) {
                if (cur < rounds) {
                    cur++
                    input = ""
                } else {
                    onComplete()
                }
            } else {
                isWrong = true
            }
        }
    }

    // High-end visuals: Gold accent by default, Green when correct so far
    val isCorrectSoFar = reversedTarget.startsWith(input, ignoreCase = true)
    val accentColor = if (isCorrectSoFar && input.isNotEmpty()) HaJade else SoftAmber

    Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp).imePadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = "QUESTION $cur OF $rounds", 
                color = HaJade, 
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "TYPE IN REVERSE",
                color = SoftAmber,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = HaIvory.copy(alpha = 0.04f), 
                shape = RoundedCornerShape(32.dp), 
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
            ) { 
                Text(
                    text = target, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = HaIvory, 
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.padding(32.dp)
                ) 
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Clean Error Feedback Notice (Polite & Minimal)
            AnimatedVisibility(
                visible = isWrong,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "Wrong, try again",
                    color = SoftAmber.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            OutlinedTextField(
                value = input, 
                onValueChange = { if (!isWrong) input = it }, 
                modifier = Modifier.fillMaxWidth(), 
                placeholder = { Text("esrever ni epyT...", color = HaIvory.copy(0.2f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = HaIvory, fontSize = 18.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = accentColor.copy(alpha = 0.3f),
                    cursorColor = accentColor
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun AudioMemoryChallengeView(rounds: Int, difficulty: Int, onComplete: () -> Unit) {
    val context = LocalContext.current
    var curRound by remember { mutableIntStateOf(1) }
    var isWrong by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var hasFinishedInitialSpeech by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    
    // Large, diverse word pool for variability
    val wordPool = remember {
        listOf(
            "Apple", "Banana", "Cloud", "River", "Mountain", "Table", "Chair", "House", "Garden", "Bird",
            "Ocean", "Forest", "Pillow", "Window", "Bridge", "Planet", "Stone", "Summer", "Winter", "Candle",
            "Bottle", "Camera", "Guitar", "Rocket", "Mirror", "Silver", "Desert", "Castle", "Orange",
            "Jungle", "Button", "Dragon", "Energy", "Flight", "Hammer", "Island", "Kettle", "Legend", "Museum",
            "Anchor", "Beacon", "Canyon", "Dolphin", "Eagle", "Falcon", "Geyser", "Harbor", "Icicle", "Jacket",
            "Knight", "Lantern", "Marble", "Nebula", "Orchid", "Parrot", "Quartz", "Rabbit", "Safari", "Temple",
            "Utopia", "Valley", "Wizard", "Xenon", "Yacht", "Zebra", "Breeze", "Cactus", "Desert", "Echo",
            "Frost", "Glacier", "Haven", "Igloo", "Joker", "Koi", "Lunar", "Meteor", "Nomad", "Oasis",
            "Pulse", "Quiver", "Radar", "Solar", "Tidal", "Umbra", "Vortex", "Willow", "Zenith", "Aura",
            "Bliss", "Cosmos", "Dawn", "Ember", "Flora", "Glint", "Haze", "Iris", "Jade", "Karma",
            "Lyric", "Mist", "Nova", "Opal", "Pearl", "Quill", "Roar", "Soul", "Titan", "Unity",
            "Vail", "Wave", "Yarn", "Zeal"
        ).distinct()
    }

    val numWords = when (difficulty) {
        0 -> 3
        1 -> 4
        2 -> 5
        else -> 6
    }

    // Trigger for word re-generation on mistakes
    var generationTrigger by remember { mutableIntStateOf(0) }

    var targetSequence by remember(curRound, generationTrigger) { 
        mutableStateOf(wordPool.shuffled().take(numWords)) 
    }
    
    val optionsGrid = remember(targetSequence) {
        (targetSequence + wordPool.filter { it !in targetSequence }.shuffled().take(12 - numWords)).shuffled()
    }

    var userSelection by remember { mutableStateOf(listOf<String>()) }
    
    // --- Optimized TTS Initialization ---
    var isTtsReady by remember { mutableStateOf(false) }
    var wordsSpokenCount by remember { mutableIntStateOf(0) }
    
    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
    }

    fun lowerAlarmVolume() {
        context.startService(Intent(context, AlarmService::class.java).apply { action = AlarmService.ACTION_LOWER_VOLUME })
    }

    fun restoreAlarmVolume() {
        context.startService(Intent(context, AlarmService::class.java).apply { action = AlarmService.ACTION_RESTORE_VOLUME })
    }

    DisposableEffect(tts) {
        onDispose { 
            tts.shutdown()
            restoreAlarmVolume()
        }
    }

    LaunchedEffect(isTtsReady) {
        if (isTtsReady) {
            tts.language = Locale.US
            tts.setPitch(1.05f)
            tts.setSpeechRate(0.80f)
            
            // Link TTS to Alarm Stream for consistency and loudness
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            tts.setAudioAttributes(attr)
            
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId?.startsWith("word_") == true || utteranceId == "last_word") {
                        wordsSpokenCount++
                    }
                }
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "last_word") {
                        isSpeaking = false
                        hasFinishedInitialSpeech = true
                        restoreAlarmVolume()
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    hasFinishedInitialSpeech = true
                    restoreAlarmVolume()
                }
            })
        }
    }

    fun startSpeaking() {
        if (!isTtsReady) return
        
        isSpeaking = true
        wordsSpokenCount = 0
        userSelection = emptyList()
        lowerAlarmVolume()
        
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AudioMemory")

        targetSequence.forEachIndexed { index, word ->
            val trackId = if (index == targetSequence.size - 1) "last_word" else "word_$index"
            tts.speak(word, TextToSpeech.QUEUE_ADD, params, trackId)
            tts.playSilentUtterance(900, TextToSpeech.QUEUE_ADD, null)
        }
    }

    LaunchedEffect(isWrong) {
        if (isWrong) {
            delay(1500.milliseconds)
            isWrong = false
            userSelection = emptyList()
            hasFinishedInitialSpeech = false
            generationTrigger++ // Force new words and re-speech on mistake
        }
    }

    LaunchedEffect(targetSequence, isWrong, isTtsReady) {
        if (!isWrong && isTtsReady) {
            // Reduced delay for near-instant speech trigger (500ms instead of 1200ms)
            delay(500.milliseconds)
            startSpeaking()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp).statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "QUESTION $curRound OF $rounds", 
                color = HaJade, 
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "AUDIO MEMORY",
                color = SoftAmber,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Premium Speaker Wave Animation/UI ---
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(HaIvory.copy(alpha = 0.03f))
                    .border(1.dp, if (isSpeaking) SoftAmber.copy(0.3f) else HaIvory.copy(alpha = 0.05f), CircleShape)
                    .clickable(onClick = { if (!isSpeaking) startSpeaking() }),
                contentAlignment = Alignment.Center
            ) {
                if (isSpeaking) {
                    val infiniteTransition = rememberInfiniteTransition(label = "audio_waves")
                    
                    // Triple ripple effect for high-end look
                    repeat(3) { i ->
                        val waveScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = i * 500, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "wave_scale"
                        )
                        val waveAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, delayMillis = i * 500, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "wave_alpha"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = waveScale
                                    scaleY = waveScale
                                    alpha = waveAlpha
                                }
                                .border(2.dp, SoftAmber, CircleShape)
                        )
                    }
                }

                val pulseTransition = rememberInfiniteTransition(label = "audio_pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "pulse"
                )

                Surface(
                    shape = CircleShape,
                    color = if (isSpeaking) SoftAmber.copy(0.15f) else HaIvory.copy(0.05f),
                    modifier = Modifier.size(90.dp).graphicsLayer {
                        if (isSpeaking) {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    },
                    border = BorderStroke(2.dp, if (isSpeaking) SoftAmber else HaIvory.copy(0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isSpeaking) SoftAmber else HaIvory,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = if (isSpeaking) "WORDS SPOKEN: $wordsSpokenCount / $numWords" else "TAP WORDS IN ORDER",
                color = if (isSpeaking) SoftAmber else HaIvory.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Grid Layout & Submission (Hidden during speech)
            AnimatedVisibility(
                visible = hasFinishedInitialSpeech && !isSpeaking,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.height(200.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(optionsGrid) { word ->
                                val isSelected = userSelection.contains(word)
                                val order = if (isSelected) userSelection.indexOf(word) + 1 else null
                                
                                Surface(
                                    onClick = {
                                        if (!isSpeaking && !isWrong && !completed) {
                                            if (isSelected) userSelection = userSelection - word
                                            else if (userSelection.size < numWords) userSelection = userSelection + word
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) SoftAmber.copy(alpha = 0.15f) else HaIvory.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, if (isSelected) SoftAmber else HaIvory.copy(alpha = 0.1f)),
                                    modifier = Modifier.height(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = word, 
                                            color = if (isSelected) SoftAmber else HaIvory, 
                                            fontSize = 13.sp, 
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (order != null) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(16.dp)
                                                    .background(SoftAmber, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = order.toString(),
                                                    fontSize = 10.sp,
                                                    color = MidnightBlue,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Clean Error Notice
                    AnimatedVisibility(
                        visible = isWrong,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = "Wrong, try again", 
                            color = HaCoral, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (userSelection == targetSequence) {
                                if (curRound < rounds) {
                                    curRound++
                                    userSelection = emptyList()
                                } else if (!completed) {
                                    completed = true
                                    onComplete()
                                }
                            } else {
                                isWrong = true
                            }
                        },
                        enabled = userSelection.size == numWords && !isSpeaking && !isWrong && !completed,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftAmber, disabledContainerColor = SoftAmber.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("SUBMIT ANSWER", color = MidnightBlue, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
            
            // Spoken Words Indicator (Dots) during speech
            if (isSpeaking) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(numWords) { index ->
                        val isFinished = index < wordsSpokenCount
                        val isCurrent = index == wordsSpokenCount
                        
                        val dotAlpha by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else if (isFinished) 0.6f else 0.2f,
                            animationSpec = tween(500),
                            label = "dot"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(SoftAmber.copy(alpha = dotAlpha))
                                .border(1.dp, SoftAmber.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomKeypad(onPress: (String) -> Unit) {
    val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("-", "0", "DEL"), listOf(".", "OK"))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) { keys.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { k -> Surface(onClick = { onPress(k) }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(16.dp), color = if (k == "OK") SoftAmber else HaIvory.copy(alpha = 0.05f)) { Box(contentAlignment = Alignment.Center) { if (k == "DEL") Icon(Icons.AutoMirrored.Filled.Backspace, null) else Text(k, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (k == "OK") MidnightBlue else HaIvory) } } } } } }
}

@SuppressLint("NewApi")
@Composable
fun SuccessAnimation(onFinished: () -> Unit) {
    val transitionProgress = remember { Animatable(0f) }
    val exitAnim = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_dismiss")
    val rippleCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val sunBreath by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    LaunchedEffect(Unit) {
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing)
        )
        delay(2700.milliseconds) // Hold state
        exitAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    val progress = transitionProgress.value
    val exit = exitAnim.value
    val sunRotation = progress * 35f
    val sunScale = (((0.75f + (progress * 0.25f)).coerceIn(0.75f, 1.0f)) * sunBreath) * (1f - exit)
    val sunAlpha = (1f - exit).coerceIn(0f, 1f)
    val textAlpha = ((progress - 0.2f) / 0.3f).coerceIn(0f, 1f) * (1f - exit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer {
                            scaleX = sunScale * 1.3f
                            scaleY = sunScale * 1.3f
                            alpha = ((0.2f + (0.08f * sin(rippleCycle * Math.PI * 2).toFloat())).coerceIn(0.15f, 0.3f)) * sunAlpha
                        }
                        .background(SoftAmber, CircleShape)
                )

                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            rotationZ = sunRotation
                            alpha = sunAlpha
                            scaleX = 1f - (exit * 0.5f)
                            scaleY = 1f - (exit * 0.5f)
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val radius = cx * 0.32f

                    val rayCount = 12
                    for (i in 0 until rayCount) {
                        val angle = (i * (360f / rayCount)) * (Math.PI / 180f)
                        val innerR = radius * 1.25f
                        val outerR = radius * 1.75f
                        val startX = cx + (innerR * cos(angle)).toFloat()
                        val startY = cy + (innerR * sin(angle)).toFloat()
                        val endX = cx + (outerR * cos(angle)).toFloat()
                        val endY = cy + (outerR * sin(angle)).toFloat()

                        drawLine(
                            color = SoftAmber.copy(alpha = 0.8f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    drawCircle(
                        color = SoftAmber.copy(alpha = 0.3f),
                        radius = radius * 1.2f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = SoftAmber,
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = HaIvory.copy(alpha = 0.4f),
                        radius = radius * 0.4f,
                        center = Offset(cx - radius * 0.25f, cy - radius * 0.25f)
                    )

                    val dropFallProgress = (rippleCycle * 1.5f).coerceIn(0f, 1f)
                    val dropY = cy + radius + (dropFallProgress * 45f)
                    val dropAlpha = if (dropFallProgress < 0.7f) 0.8f else (1f - dropFallProgress) * 2.6f

                    if (dropFallProgress < 0.75f && exit == 0f) {
                        drawCircle(
                            color = HaSky.copy(alpha = dropAlpha.coerceIn(0f, 1f)),
                            radius = 3.dp.toPx(),
                            center = Offset(cx, dropY)
                        )
                    }

                    val surfaceY = cy + radius + 45f
                    val ripplePhase = ((rippleCycle - 0.4f) / 0.6f).coerceIn(0f, 1f)
                    if (ripplePhase > 0f && exit == 0f) {
                        val rippleRadius = ripplePhase * 55f
                        val rippleAlpha = (1f - ripplePhase) * 0.7f
                        drawCircle(
                            color = HaSky.copy(alpha = rippleAlpha.coerceIn(0f, 1f)),
                            radius = rippleRadius,
                            center = Offset(cx, surfaceY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "ALARM DISMISSED",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = HaIvory,
                letterSpacing = 1.5.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha
                    translationY = (1f - textAlpha) * 16f
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Good morning! You’re up and ready.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = HaIvory.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha
                    translationY = (1f - textAlpha) * 16f
                }
            )
        }
    }
}

@Composable
fun TargetingFrame(isDet: Boolean) {
    val c = if (isDet) HaJade else HaIvory.copy(0.4f)
    val strokeWidth = if (isDet) 5.dp else 3.dp
    
    Box(modifier = Modifier.size(280.dp)) {
        // Corners with slightly more gap and thickness
        Box(modifier = Modifier.size(45.dp, strokeWidth).background(c).align(Alignment.TopStart)); Box(modifier = Modifier.size(strokeWidth, 45.dp).background(c).align(Alignment.TopStart))
        Box(modifier = Modifier.size(45.dp, strokeWidth).background(c).align(Alignment.TopEnd)); Box(modifier = Modifier.size(strokeWidth, 45.dp).background(c).align(Alignment.TopEnd))
        Box(modifier = Modifier.size(45.dp, strokeWidth).background(c).align(Alignment.BottomStart)); Box(modifier = Modifier.size(strokeWidth, 45.dp).background(c).align(Alignment.BottomStart))
        Box(modifier = Modifier.size(45.dp, strokeWidth).background(c).align(Alignment.BottomEnd)); Box(modifier = Modifier.size(strokeWidth, 45.dp).background(c).align(Alignment.BottomEnd))
        
        // Inner subtle scan line shadow area
        if (isDet) {
            Box(modifier = Modifier.fillMaxSize().background(HaJade.copy(alpha = 0.05f)))
        }
    }
}

@SuppressLint("UnsafeOptInUsageError", "NewApi")
@Composable
fun CameraChallenge(onSuccess: (Any) -> Unit, mode: String, cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA) {
    val ctx = LocalContext.current; val lo = LocalLifecycleOwner.current; val cpf = remember { ProcessCameraProvider.getInstance(ctx) }
    val bar = remember { BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()) }
    val obj = remember { ObjectDetection.getClient(ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.STREAM_MODE).enableMultipleObjects().enableClassification().build()) }
    val lab = remember { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }
    val pos = remember { PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()) }
    val pv = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    @Suppress("BlockingMethodInNonBlockingContext")
    LaunchedEffect(cameraSelector, mode) {
        val cp = cpf.get(); val pre = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
        val ana = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { it.setAnalyzer(Executors.newSingleThreadExecutor()) { ip ->
            val mi = ip.image; if (mi != null) {
                val img = InputImage.fromMediaImage(mi, ip.imageInfo.rotationDegrees)
                when (mode) {
                    "barcode" -> bar.process(img).addOnSuccessListener { result -> onSuccess(result) }.addOnCompleteListener { ip.close() }
                    "labeling" -> lab.process(img).addOnSuccessListener { result -> onSuccess(result) }.addOnCompleteListener { ip.close() }
                    "pose" -> pos.process(img).addOnSuccessListener { result -> onSuccess(PoseFrameData(result, ip.imageInfo.rotationDegrees, cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA, ip.width, ip.height)) }.addOnCompleteListener { ip.close() }
                    else -> obj.process(img).addOnSuccessListener { result -> onSuccess(result) }.addOnCompleteListener { ip.close() }
                }
            } else ip.close()
        } }
        try { cp.unbindAll(); cp.bindToLifecycle(lo, cameraSelector, pre, ana) } catch (_: Exception) { }
    }
    AndroidView(factory = { pv }, modifier = Modifier.fillMaxSize())
}
