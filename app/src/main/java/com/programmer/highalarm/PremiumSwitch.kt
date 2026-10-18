package com.programmer.highalarm

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * HighAlarm 3D toggle.
 * OFF: recessed dark groove with a bright silver knob (always clearly visible).
 * ON : polished gold groove with a dark obsidian knob.
 *
 * Motion: one spring drives everything, so the knob glides while the groove
 * melts from black to gold and the knob from silver to obsidian at the same
 * pace. The knob stretches a touch while pressed, a soft light sweeps across
 * the gold when it turns on, and a light haptic tick confirms the change.
 *
 * Pass onCheckedChange = null to make it display-only (parent handles clicks).
 */
@Composable
fun HaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackW = 52.dp
    val trackH = 30.dp
    val knob = 24.dp
    val pad = 3.dp

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // 0 = off, 1 = on (a gentle spring: settles quickly with a hint of give)
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "ha_switch_progress"
    )
    val press by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f),
        label = "ha_switch_press"
    )

    // light sweep across the gold when switched on + haptic on every change
    val shine = remember { Animatable(1f) }
    val last = remember { mutableStateOf(checked) }
    val view = LocalView.current
    LaunchedEffect(checked) {
        if (checked == last.value) return@LaunchedEffect
        last.value = checked
        try {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (checked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                } else HapticFeedbackConstants.CLOCK_TICK
            )
        } catch (_: Exception) { }
        if (checked) {
            shine.snapTo(0f)
            shine.animateTo(1f, tween(durationMillis = 700, delayMillis = 140, easing = FastOutSlowInEasing))
        }
    }

    val p = progress.coerceIn(0f, 1f)
    val glow = lerp(Color.Black, HaGold.copy(alpha = 0.35f), p)
    val trackShape = RoundedCornerShape(50)

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interaction,
            indication = null,
            onValueChange = onCheckedChange
        )
    } else Modifier

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .then(toggleModifier)
            .size(trackW, trackH)
            .shadow(2.dp + 6.dp * p, trackShape, ambientColor = glow, spotColor = glow)
            .clip(trackShape)
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = CornerRadius(h / 2f)

                // ---- groove: recessed black, melting into polished gold ----
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFF07070A), Color(0xFF15151A), Color(0xFF26262E))),
                    cornerRadius = r
                )
                if (p > 0f) {
                    drawRoundRect(
                        Brush.verticalGradient(listOf(HaGoldPale, HaGold, HaGoldMid, HaGoldDeep)),
                        cornerRadius = r,
                        alpha = p
                    )
                }

                // ---- one soft light sweep when it turns on ----
                val s = shine.value
                if (s < 1f && p > 0.01f) {
                    val cx = -0.35f * w + 1.7f * w * s
                    drawRoundRect(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.5f),
                            1f to Color.Transparent,
                            startX = cx - w * 0.22f,
                            endX = cx + w * 0.22f
                        ),
                        cornerRadius = r,
                        alpha = p * (1f - s * 0.4f)
                    )
                }

                // ---- bevelled rims (cross-fade with the state) ----
                val sw = 1.dp.toPx()
                val rimTopLeft = Offset(sw / 2f, sw / 2f)
                val rimSize = Size(w - sw, h - sw)
                val rimR = CornerRadius(h / 2f - sw / 2f)
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.White.copy(alpha = 0.14f))),
                    topLeft = rimTopLeft, size = rimSize, cornerRadius = rimR,
                    style = Stroke(sw), alpha = 1f - p
                )
                if (p > 0f) {
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), HaBronze)),
                        topLeft = rimTopLeft, size = rimSize, cornerRadius = rimR,
                        style = Stroke(sw), alpha = p
                    )
                }

                // ---- knob: glides, stretches a little while pressed ----
                val padPx = pad.toPx()
                val kh = knob.toPx()
                val kw = kh + 5.dp.toPx() * press
                val pos = progress.coerceIn(-0.03f, 1.03f)
                val x = padPx + pos * (w - 2f * padPx - kw)
                val y = (h - kh) / 2f
                val kr = CornerRadius(kh / 2f)
                // soft drop shadow
                drawRoundRect(
                    Color.Black.copy(alpha = 0.22f),
                    topLeft = Offset(x, y + 2.5.dp.toPx()), size = Size(kw, kh), cornerRadius = kr
                )
                drawRoundRect(
                    Color.Black.copy(alpha = 0.30f),
                    topLeft = Offset(x, y + 1.dp.toPx()), size = Size(kw, kh), cornerRadius = kr
                )
                // silver (off) → obsidian (on)
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFDCDBE0), Color(0xFFA9A8B0)),
                        startY = y, endY = y + kh
                    ),
                    topLeft = Offset(x, y), size = Size(kw, kh), cornerRadius = kr, alpha = 1f - p
                )
                if (p > 0f) {
                    drawRoundRect(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3A3842), Color(0xFF16151A), Color(0xFF0A0A0D)),
                            startY = y, endY = y + kh
                        ),
                        topLeft = Offset(x, y), size = Size(kw, kh), cornerRadius = kr, alpha = p
                    )
                }
                // knob rim
                val ksw = 0.8.dp.toPx()
                val kRimTopLeft = Offset(x + ksw / 2f, y + ksw / 2f)
                val kRimSize = Size(kw - ksw, kh - ksw)
                val kRimR = CornerRadius(kh / 2f - ksw / 2f)
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White, Color(0xFF7D7C85)), startY = y, endY = y + kh),
                    topLeft = kRimTopLeft, size = kRimSize, cornerRadius = kRimR,
                    style = Stroke(ksw), alpha = 1f - p
                )
                if (p > 0f) {
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Black), startY = y, endY = y + kh),
                        topLeft = kRimTopLeft, size = kRimSize, cornerRadius = kRimR,
                        style = Stroke(ksw), alpha = p
                    )
                }
            }
    )
}
