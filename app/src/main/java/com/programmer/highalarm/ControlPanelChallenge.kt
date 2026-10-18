package com.programmer.highalarm

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

sealed class ControlType {
    object Toggle : ControlType()
    object Slider : ControlType()
    object Selector : ControlType()
    object Stepper : ControlType()
}

data class ControlItem(
    val key: String,
    val label: String,
    val type: ControlType,
    val targetVal: Any,
    val currentVal: Any,
    val options: List<String> = emptyList()
)

data class ControlPanelPuzzle(
    val controls: List<ControlItem>,
    val initialValues: Map<String, Any>
)

private val recentPuzzleSignatures = mutableSetOf<String>()

fun generateControlPanelPuzzle(difficulty: Int): ControlPanelPuzzle {
    val allKeys = listOf(
        Triple("power", "POWER", ControlType.Toggle),
        Triple("shield", "SHIELD", ControlType.Toggle),
        Triple("light", "LIGHT", ControlType.Toggle),
        Triple("cooling", "COOLING", ControlType.Slider),
        Triple("core", "CORE", ControlType.Selector),
        Triple("voltage", "VOLTAGE", ControlType.Stepper),
        Triple("fan", "FAN", ControlType.Toggle),
        Triple("stability", "STABILITY", ControlType.Slider),
        Triple("frequency", "FREQUENCY", ControlType.Stepper),
        Triple("temperature", "TEMP", ControlType.Slider),
        Triple("pressure", "PRESSURE", ControlType.Slider),
        Triple("security", "SECURITY", ControlType.Selector),
        Triple("network", "NETWORK", ControlType.Toggle),
        Triple("reactor", "REACTOR", ControlType.Selector),
        Triple("signal", "SIGNAL", ControlType.Slider),
        Triple("backup", "BACKUP", ControlType.Toggle)
    )

    val count = when (difficulty) {
        0 -> 5
        1 -> 8
        2 -> 12
        3 -> 16
        else -> 5
    }

    val minDiffs = when (difficulty) {
        0 -> 2
        1 -> 4
        2 -> 6
        3 -> 8
        else -> 2
    }

    val controls = mutableListOf<ControlItem>()
    val initialVals = mutableMapOf<String, Any>()

    var attempts = 0
    do {
        controls.clear()
        initialVals.clear()
        var diffCount = 0

        val selectedKeys = allKeys.shuffled(Random(Random.nextLong())).take(count)

        for ((key, label, type) in selectedKeys) {
            val target: Any = when (type) {
                is ControlType.Toggle -> Random.nextBoolean()
                is ControlType.Slider -> Random.nextInt(1, 10) * 10
                is ControlType.Selector -> {
                    if (label == "SECURITY") listOf("SECURE", "LOCK", "OPEN").random()
                    else if (label == "REACTOR") listOf("ALPHA", "BETA", "GAMMA").random()
                    else listOf("RED", "BLUE", "GREEN").random()
                }
                is ControlType.Stepper -> Random.nextInt(1, 10) * 10
            }

            val current: Any = when (type) {
                is ControlType.Toggle -> if (Random.nextBoolean()) !(target as Boolean) else target
                is ControlType.Slider -> Random.nextInt(0, 11) * 10
                is ControlType.Selector -> {
                    val opts = if (label == "SECURITY") listOf("SECURE", "LOCK", "OPEN")
                    else if (label == "REACTOR") listOf("ALPHA", "BETA", "GAMMA")
                    else listOf("RED", "BLUE", "GREEN")
                    opts.filter { it != target }.random()
                }
                is ControlType.Stepper -> Random.nextInt(0, 11) * 10
            }

            if (current != target) {
                diffCount++
            }

            initialVals[key] = current
            controls.add(
                ControlItem(
                    key = key,
                    label = label,
                    type = type,
                    targetVal = target,
                    currentVal = current,
                    options = when (type) {
                        is ControlType.Selector -> {
                            if (label == "SECURITY") listOf("SECURE", "LOCK", "OPEN")
                            else if (label == "REACTOR") listOf("ALPHA", "BETA", "GAMMA")
                            else listOf("RED", "BLUE", "GREEN")
                        }
                        else -> emptyList()
                    }
                )
            )
        }

        val signature = controls.joinToString("|") { "${it.key}:${it.targetVal}:${it.currentVal}" }
        if (diffCount >= minDiffs && !recentPuzzleSignatures.contains(signature)) {
            recentPuzzleSignatures.add(signature)
            if (recentPuzzleSignatures.size > 20) {
                val firstKey = recentPuzzleSignatures.first()
                recentPuzzleSignatures.remove(firstKey)
            }
            break
        }
        attempts++
    } while (attempts < 100)

    return ControlPanelPuzzle(controls, initialVals)
}

@Composable
fun ControlPanelChallengeView(
    rounds: Int,
    difficulty: Int = 0,
    onComplete: () -> Unit
) {
    val totalRounds = if (rounds <= 0) 1 else rounds
    var currentRound by remember { mutableIntStateOf(1) }
    var puzzle by remember(currentRound, difficulty) { mutableStateOf(generateControlPanelPuzzle(difficulty)) }
    var activeTaskIndex by remember(currentRound, difficulty) {
        mutableIntStateOf(
            puzzle.controls.indexOfFirst { it.currentVal != it.targetVal }.let { if (it == -1) puzzle.controls.size else it }
        )
    }
    var hasCompleted by remember { mutableStateOf(value = false) }

    // Hint state (strictly works ONCE per mission/round, does not auto-solve or complete mission)
    var hintUsed by remember(currentRound, difficulty) { mutableStateOf(value = false) }
    var hintMessage by remember(currentRound, difficulty) { mutableStateOf<String?>(null) }

    val isSolved = activeTaskIndex >= puzzle.controls.size

    LaunchedEffect(isSolved) {
        if (isSolved && !hasCompleted) {
            hasCompleted = true
            delay(800L)
            if (currentRound < totalRounds) {
                currentRound++
                puzzle = generateControlPanelPuzzle(difficulty)
                activeTaskIndex = puzzle.controls.indexOfFirst { it.currentVal != it.targetVal }.let { if (it == -1) puzzle.controls.size else it }
                hintUsed = false
                hintMessage = null
                hasCompleted = false
            } else {
                onComplete()
            }
        }
    }

    val diffName = when (difficulty) {
        0 -> "Easy"
        1 -> "Medium"
        2 -> "Hard"
        3 -> "Extreme"
        else -> "Easy"
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HaObsidian)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CONTROL PANEL ($diffName)",
                color = SoftAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Round $currentRound / $totalRounds",
            color = HaIvory,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "COMPLETE TASKS SEQUENTIALLY",
            color = HaMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target Configuration Preview Card
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = HaSurfaceLow,
            border = BorderStroke(1.dp, SoftAmber.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "TARGET CONFIGURATION:",
                    color = SoftAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                puzzle.controls.forEachIndexed { idx, item ->
                    val isDone = idx < activeTaskIndex
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${if (isDone) "✓ " else "• "}${item.label}",
                            color = if (isDone) HaJade else HaMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = item.targetVal.toString(),
                            color = if (isDone) HaJade else HaIvory,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current System Console (Showing completed tasks as rows + active task below)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = HaSurfaceLow,
            border = BorderStroke(1.5.dp, SoftAmber.copy(alpha = 0.4f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CURRENT SYSTEM CONSOLE",
                        color = HaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isSolved) "ALL TASKS COMPLETE" else "Task ${activeTaskIndex + 1} of ${puzzle.controls.size}",
                        color = SoftAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Render all completed tasks as permanent visible rows
                for (i in 0 until activeTaskIndex.coerceAtMost(puzzle.controls.size)) {
                    val completedItem = puzzle.controls[i]
                    CompletedControlRow(item = completedItem)
                }

                // Render active task if not solved
                val activeItem = puzzle.controls.getOrNull(activeTaskIndex)
                if (activeItem != null) {
                    key(activeTaskIndex) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                                    slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { 30 } +
                                    scaleIn(initialScale = 0.98f, animationSpec = tween(400, easing = FastOutSlowInEasing)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            ControlRow(
                                item = activeItem,
                                onChanged = { newVal ->
                                    if (!isSolved && !hasCompleted) {
                                        val updatedControls = puzzle.controls.map {
                                            if (it.key == activeItem.key) it.copy(currentVal = newVal) else it
                                        }
                                        puzzle = puzzle.copy(controls = updatedControls)

                                        val updatedActive = updatedControls.first { it.key == activeItem.key }
                                        if (updatedActive.currentVal == updatedActive.targetVal) {
                                            activeTaskIndex++
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "✓ ALL TASKS COMPLETED",
                        color = HaJade,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Hint Message Banner if active
        if (hintMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(12.dp),
                color = SoftAmber.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SoftAmber.copy(alpha = 0.4f))
            ) {
                Text(
                    text = hintMessage!!,
                    color = SoftAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSolved) "✓ SYSTEM STABLE" else "Configuring...",
                color = if (isSolved) HaJade else HaMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reset Button
                OutlinedButton(
                    onClick = {
                        if (!isSolved && !hasCompleted) {
                            val resetControls = puzzle.controls.map { c ->
                                c.copy(currentVal = puzzle.initialValues[c.key] ?: c.currentVal)
                            }
                            puzzle = puzzle.copy(controls = resetControls)
                            activeTaskIndex = puzzle.controls.indexOfFirst { it.currentVal != it.targetVal }.let { if (it == -1) puzzle.controls.size else it }
                            hintUsed = false
                            hintMessage = null
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HaIvory.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HaIvory)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Hint Button (Works ONLY ONCE per mission/round, displays guidance text without auto-solving)
                Button(
                    onClick = {
                        if (!hintUsed && !isSolved && !hasCompleted) {
                            val activeItem = puzzle.controls.getOrNull(activeTaskIndex) ?: puzzle.controls.firstOrNull { it.currentVal != it.targetVal }
                            if (activeItem != null) {
                                hintMessage = "Hint: Check '${activeItem.label}' — adjust it to match target value (${activeItem.targetVal})."
                            } else {
                                hintMessage = "Hint: All parameters are currently correct!"
                            }
                            hintUsed = true
                        }
                    },
                    enabled = !hintUsed && !isSolved && !hasCompleted,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hintUsed) HaMuted.copy(alpha = 0.3f) else SoftAmber,
                        contentColor = if (hintUsed) HaMuted else Color.Black
                    )
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (hintUsed) "Hint Used" else "Hint", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CompletedControlRow(
    item: ControlItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HaInk.copy(alpha = 0.7f))
            .border(1.dp, HaJade.copy(alpha = 0.35f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✓",
                color = HaJade,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.label,
                color = HaIvory.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "${item.targetVal} — Completed",
            color = HaJade.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ControlRow(
    item: ControlItem,
    onChanged: (Any) -> Unit
) {
    val isMatch = item.currentVal == item.targetVal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HaInk)
            .border(1.dp, if (isMatch) HaJade.copy(alpha = 0.5f) else HaIvory.copy(alpha = 0.08f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(0.35f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isMatch) "✓" else "•",
                color = if (isMatch) HaJade else SoftAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.label,
                color = HaIvory,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier.weight(0.65f),
            contentAlignment = Alignment.CenterEnd
        ) {
            when (item.type) {
                is ControlType.Toggle -> {
                    val isOn = item.currentVal as Boolean
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { onChanged(true) },
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOn) SoftAmber else HaIvory.copy(alpha = 0.08f),
                                contentColor = if (isOn) Color.Black else HaIvory
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text("ON", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { onChanged(false) },
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isOn) SoftAmber else HaIvory.copy(alpha = 0.08f),
                                contentColor = if (!isOn) Color.Black else HaIvory
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text("OFF", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                is ControlType.Slider -> {
                    val current = item.currentVal as Int
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(current.toString(), color = SoftAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = current.toFloat(),
                            onValueChange = { snapped ->
                                val snappedInt = ((snapped + 5f) / 10f).roundToInt() * 10
                                onChanged(snappedInt.coerceIn(0, 100))
                            },
                            valueRange = 0f..100f,
                            steps = 9,
                            modifier = Modifier.width(110.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = SoftAmber,
                                activeTrackColor = SoftAmber,
                                inactiveTrackColor = HaIvory.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
                is ControlType.Selector -> {
                    val current = item.currentVal as String
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        item.options.forEach { opt ->
                            val selected = current == opt
                            Button(
                                onClick = { onChanged(opt) },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) SoftAmber else HaIvory.copy(alpha = 0.08f),
                                    contentColor = if (selected) Color.Black else HaIvory
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(opt, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                is ControlType.Stepper -> {
                    val current = item.currentVal as Int
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onChanged((current - 5).coerceAtLeast(0)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = SoftAmber, modifier = Modifier.size(16.dp))
                        }
                        Text(current.toString(), color = HaIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { onChanged((current + 5).coerceAtMost(100)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = SoftAmber, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
