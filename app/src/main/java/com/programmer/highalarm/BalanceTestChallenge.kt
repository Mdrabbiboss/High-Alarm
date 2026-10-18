package com.programmer.highalarm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun BalanceTestChallengeView(
    targetSeconds: Int,
    difficulty: Int = 0,
    onComplete: () -> Unit,
) {
    var secondsHeld by remember { mutableFloatStateOf(0f) }
    var isInside by remember { mutableStateOf(value = false) }

    val effectiveTargetSeconds = if (targetSeconds <= 0) 15 else targetSeconds

    // Play area boundary is defined by the full outer ring (280.dp -> radius 140f)
    val playAreaRadius = 140f
    val ballRadius = 17f
    val maxDist = (playAreaRadius - ballRadius).coerceAtLeast(10f)

    // Difficulty parameters: targetRadius (inner ring), sensitivity, maxVel, damping (slightly faster ball speed)
    val (targetRadius, sensitivity, maxVel, damping) = when (difficulty) {
        0 -> arrayOf(65f, 1.4f, 1.2f, 0.940f) // Easy
        1 -> arrayOf(50f, 1.8f, 1.5f, 0.945f) // Medium
        2 -> arrayOf(38f, 2.2f, 1.8f, 0.950f) // Hard
        3 -> arrayOf(28f, 2.7f, 2.1f, 0.955f) // Extreme
        else -> arrayOf(65f, 1.4f, 1.2f, 0.940f)
    }

    // Initial starting position clearly OUTSIDE the target ring, but safely inside maxDist
    val initialOffset = remember(difficulty, targetRadius) {
        val startDist = (targetRadius + 22f).coerceAtMost(maxDist)
        val angle = 2.35619f // ~135 degrees (up-left quadrant)
        Pair(cos(angle) * startDist, sin(angle) * startDist)
    }

    var ballX by remember(difficulty) { mutableFloatStateOf(initialOffset.first) }
    var ballY by remember(difficulty) { mutableFloatStateOf(initialOffset.second) }

    // Manual D-Pad continuous hold states (-1f, 0f, 1f)
    var manualDirX by remember { mutableFloatStateOf(0f) }
    var manualDirY by remember { mutableFloatStateOf(0f) }

    val applyNudge: (Float, Float) -> Unit = { dx, dy ->
        val newX = ballX + dx
        val newY = ballY + dy
        val dist = sqrt(((newX * newX) + (newY * newY)).toDouble()).toFloat()
        if (dist <= maxDist) {
            ballX = newX
            ballY = newY
        } else {
            ballX = (newX / dist) * maxDist
            ballY = (newY / dist) * maxDist
        }
    }

    // Smooth physics loop with true real-time elapsed timing and slightly increased ball speed
    LaunchedEffect(difficulty) {
        var vX = 0f
        var vY = 0f
        val driftRate = 0.035f // Gentle outward drift force

        var lastTime = System.currentTimeMillis()

        while (true) {
            delay(16L)
            val currentTime = System.currentTimeMillis()
            val deltaSeconds = (currentTime - lastTime) / 1000f
            lastTime = currentTime

            val manualAccX = manualDirX * (sensitivity * 0.12f)
            val manualAccY = manualDirY * (sensitivity * 0.12f)

            // Outward drift away from center
            val distFromCenter = sqrt(((ballX * ballX) + (ballY * ballY)).toDouble()).toFloat()
            val driftX = if (distFromCenter > 0.5f) (ballX / distFromCenter) * driftRate else driftRate * 0.5f
            val driftY = if (distFromCenter > 0.5f) (ballY / distFromCenter) * driftRate else driftRate * 0.5f

            val accX = manualAccX + driftX
            val accY = manualAccY + driftY

            vX = (vX + accX) * damping
            vY = (vY + accY) * damping

            vX = vX.coerceIn(-maxVel, maxVel)
            vY = vY.coerceIn(-maxVel, maxVel)

            ballX += vX
            ballY += vY

            // Strict outer ring boundary clamping with wall slide projection
            val dist = sqrt(((ballX * ballX) + (ballY * ballY)).toDouble()).toFloat()
            if (dist > maxDist) {
                ballX = (ballX / dist) * maxDist
                ballY = (ballY / dist) * maxDist

                val nx = ballX / dist
                val ny = ballY / dist
                val dot = vX * nx + vY * ny
                if (dot > 0f) {
                    vX -= dot * nx
                    vY -= dot * ny
                }
            }

            // Real-time elapsed timing for countdown timer
            if (dist <= targetRadius) {
                isInside = true
                secondsHeld = (secondsHeld + deltaSeconds).coerceAtMost(effectiveTargetSeconds.toFloat())
                if (secondsHeld >= effectiveTargetSeconds) {
                    onComplete()
                    break
                }
            } else {
                isInside = false
                secondsHeld = (secondsHeld - (deltaSeconds * 0.6f)).coerceAtLeast(0f)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HaObsidian)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BALANCE TEST ($diffName)",
            color = SoftAmber,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Use D-Pad controls to guide ball",
            color = HaIvory,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Hold inside for ${effectiveTargetSeconds}s",
            color = HaMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(CircleShape)
                .background(HaSurfaceLow)
                .border(2.dp, SoftAmber.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Target / Success Ring (Inner ring = goal)
            Box(
                modifier = Modifier
                    .size((targetRadius * 2f).dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isInside) HaJade else SoftAmber, CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(HaIvory.copy(alpha = 0.3f), CircleShape)
            )

            // Ball moving smoothly across the full outer ring play area
            Box(
                modifier = Modifier
                    .offset(x = ballX.dp, y = ballY.dp)
                    .size(34.dp)
                    .shadow(8.dp, CircleShape, ambientColor = SoftAmber, spotColor = SoftAmber)
                    .background(if (isInside) HaJade else SoftAmber, CircleShape)
                    .border(2.dp, HaIvory, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val progress = (secondsHeld / effectiveTargetSeconds).coerceIn(0f, 1f)
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress",
                    color = HaMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(secondsHeld * 10).toInt() / 10f}s / ${effectiveTargetSeconds}s",
                    color = if (isInside) HaJade else SoftAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isInside) HaJade else SoftAmber,
                trackColor = HaIvory.copy(alpha = 0.1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Moderately Larger Professional D-Pad (56.dp buttons)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BalanceDirectionButton(
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                onNudge = { applyNudge(0f, -9f) },
                onPressState = { pressed -> manualDirY = if (pressed) -1f else 0f }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BalanceDirectionButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Left",
                    onNudge = { applyNudge(-9f, 0f) },
                    onPressState = { pressed -> manualDirX = if (pressed) -1f else 0f }
                )
                BalanceDirectionButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Right",
                    onNudge = { applyNudge(9f, 0f) },
                    onPressState = { pressed -> manualDirX = if (pressed) +1f else 0f }
                )
            }
            BalanceDirectionButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                onNudge = { applyNudge(0f, 9f) },
                onPressState = { pressed -> manualDirY = if (pressed) 1f else 0f }
            )
        }
    }
}

@Composable
fun BalanceDirectionButton(
    icon: ImageVector,
    contentDescription: String,
    onNudge: () -> Unit,
    onPressState: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .size(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onNudge() },
                    onPress = {
                        isPressed = true
                        onPressState(true)
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                            onPressState(false)
                        }
                    }
                )
            },
        shape = CircleShape,
        color = if (isPressed) SoftAmber.copy(alpha = 0.25f) else HaSurfaceHigh,
        border = BorderStroke(1.5.dp, SoftAmber.copy(alpha = if (isPressed) 0.9f else 0.5f)),
        shadowElevation = if (isPressed) 2.dp else 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = SoftAmber,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
