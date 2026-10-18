package com.programmer.highalarm

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/*
 * Premium, text-free "wrong answer" cue for the challenges.
 *
 * Instead of tinting the whole screen red (which washed over the question and
 * the keypad), a wrong answer now:
 *  - gives a short "reject" haptic,
 *  - shakes the question card with a damped wobble,
 *  - lights the answer field with a coral rim,
 *  - and breathes a soft coral glow in from the screen edges, BEHIND the
 *    card and keypad (which are opaque), so the UI itself stays clean.
 */

/** Opaque versions of the old translucent challenge surfaces (same look, no colour bleeding through). */
val HaChallengeSurface: Color = HaIvory.copy(alpha = 0.05f).compositeOver(HaObsidian)

/**
 * Returns the cue's progress: 0 → 1 while it plays, 1 when idle.
 * Fires each time [trigger] turns true; the animation is not cut short when the
 * caller resets its flag.
 */
@Composable
fun rememberWrongCue(trigger: Boolean): Float {
    val progress = remember { Animatable(1f) }
    var shots by remember { mutableIntStateOf(0) }
    val view = LocalView.current
    LaunchedEffect(trigger) { if (trigger) shots++ }
    LaunchedEffect(shots) {
        if (shots == 0) return@LaunchedEffect
        try {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
                else HapticFeedbackConstants.LONG_PRESS
            )
        } catch (_: Exception) { }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 720, easing = LinearEasing))
    }
    return progress.value
}

/** Glow envelope: quick rise, smooth fade. 0 when idle. */
fun wrongGlow(t: Float): Float = when {
    t >= 1f -> 0f
    t < 0.12f -> t / 0.12f
    else -> ((1f - t) / 0.88f).let { it * it }
}

/** Soft coral glow breathing in from the screen edges, drawn behind everything. */
fun Modifier.haWrongBackdrop(t: Float): Modifier = this.drawBehind {
    val g = wrongGlow(t)
    if (g <= 0.001f) return@drawBehind
    drawRect(
        Brush.radialGradient(
            0f to Color.Transparent,
            0.5f to Color.Transparent,
            0.8f to HaCoral.copy(alpha = 0.14f * g),
            1f to HaCoral.copy(alpha = 0.34f * g),
            center = center,
            radius = size.maxDimension * 0.72f
        )
    )
}

/** Damped horizontal wobble ("no"). */
fun Modifier.haWrongShake(t: Float): Modifier = this.graphicsLayer {
    if (t < 1f) {
        val decay = (1f - t) * (1f - t)
        translationX = sin(t * 2f * PI.toFloat() * 3.5f) * 10.dp.toPx() * decay
    }
}
