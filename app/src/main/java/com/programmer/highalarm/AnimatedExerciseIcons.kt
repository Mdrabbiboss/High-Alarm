package com.programmer.highalarm

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

enum class ExerciseKind { PushUp, Squat }

/**
 * A little athlete that does one smooth rep, rests, and repeats.
 * Drawn on a 24x24 grid with real joint kinematics (the elbow / knee is
 * solved from fixed limb lengths), so the motion looks natural.
 *
 * @param colors light → mid → deep tone used for the body (vertical gradient)
 */
@Composable
fun AnimatedExerciseIcon(
    kind: ExerciseKind,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    showFloor: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "exercise_icon")
    // 0 = top (arms / legs straight), 1 = bottom of the rep
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2800
                0f at 0 using LinearEasing
                0f at 1000 using FastOutSlowInEasing   // rest at the top
                1f at 1650 using LinearEasing           // smooth descent
                1f at 1850 using FastOutSlowInEasing    // brief pause at the bottom
                0f at 2500 using LinearEasing           // drive back up
                0f at 2800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rep_phase"
    )

    Canvas(modifier = modifier) {
        val u = size.minDimension / 24f
        val ox = (size.width - 24f * u) / 2f
        val oy = (size.height - 24f * u) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
        val body = Brush.verticalGradient(colors, startY = oy + 2f * u, endY = oy + 21f * u)

        when (kind) {
            ExerciseKind.PushUp -> drawPushUp(t, u, ::p, body, colors, showFloor)
            ExerciseKind.Squat -> drawSquat(t, u, ::p, body, colors, showFloor)
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Elbow / knee position from two fixed limb lengths (2-bone IK). */
private fun solveJoint(
    ax: Float, ay: Float, bx: Float, by: Float,
    l1: Float, l2: Float, bendSign: Float
): Pair<Float, Float> {
    val dx = bx - ax; val dy = by - ay
    var d = hypot(dx, dy)
    d = d.coerceIn(0.01f, l1 + l2 - 0.001f)
    val a = (l1 * l1 - l2 * l2 + d * d) / (2 * d)
    val h = sqrt((l1 * l1 - a * a).coerceAtLeast(0f))
    val mx = ax + a * dx / d; val my = ay + a * dy / d
    return Pair(mx + bendSign * h * (-dy / d), my + bendSign * h * (dx / d))
}

private fun DrawScope.limb(points: List<Offset>, brush: Brush, width: Float) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path, brush, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.floorAndShadow(
    u: Float, p: (Float, Float) -> Offset, colors: List<Color>,
    x0: Float, x1: Float, y: Float, shadowCx: Float, shadowW: Float, show: Boolean
) {
    if (!show) return
    // soft contact shadow that breathes with the movement
    val c = p(shadowCx, y)
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
            center = c, radius = shadowW * u
        ),
        topLeft = Offset(c.x - shadowW * u, c.y - 1.1f * u),
        size = Size(shadowW * 2 * u, 2.2f * u)
    )
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, colors.last().copy(alpha = 0.9f), Color.Transparent),
            startX = p(x0, y).x, endX = p(x1, y).x
        ),
        start = p(x0, y), end = p(x1, y),
        strokeWidth = 1.1f * u, cap = StrokeCap.Round
    )
}

private fun DrawScope.drawPushUp(
    t: Float, u: Float, p: (Float, Float) -> Offset,
    body: Brush, colors: List<Color>, showFloor: Boolean
) {
    val floorY = 19.6f
    val feetX = 3.4f; val feetY = 17.6f
    val handX = 16.0f; val handY = 18.5f
    // shoulder travels down toward the hand while the body stays a straight plank
    val sx = lerp(16.2f, 16.9f, t)
    val sy = lerp(11.0f, 15.3f, t)
    // elbow folds back toward the feet, like a real push-up
    val (ex, ey) = solveJoint(sx, sy, handX, handY, 3.9f, 3.7f, bendSign = 1f)

    floorAndShadow(u, p, colors, 1.6f, 22.4f, floorY, 10.5f, lerp(8.5f, 9.3f, t), showFloor)

    // body: feet -> hips -> shoulders (straight plank)
    val hipX = lerp(feetX, sx, 0.52f); val hipY = lerp(feetY, sy, 0.52f)
    limb(listOf(p(feetX, feetY), p(hipX, hipY), p(sx, sy)), body, 2.7f * u)
    // supporting arm with a real elbow bend
    limb(listOf(p(sx, sy), p(ex, ey), p(handX, handY)), body, 2.0f * u)
    // head, following the body line
    val dx = sx - feetX; val dy = sy - feetY
    val len = hypot(dx, dy)
    val nx = dx / len; val ny = dy / len
    val hx = sx + nx * 2.5f + ny * 0.9f
    val hy = sy + ny * 2.5f - nx * 0.9f
    drawCircle(body, radius = 2.15f * u, center = p(hx, hy))
    // tiny specular highlight for a 3D feel
    drawCircle(Color.White.copy(alpha = 0.35f), radius = 0.7f * u, center = p(hx - 0.6f, hy - 0.7f))
}

private fun DrawScope.drawSquat(
    t: Float, u: Float, p: (Float, Float) -> Offset,
    body: Brush, colors: List<Color>, showFloor: Boolean
) {
    val floorY = 21.5f
    val ax = 13.4f; val ay = 20.2f              // ankle (fixed)
    val shin = 4.9f; val thigh = 5.1f; val torso = 5.3f

    // joint angles (degrees from vertical, + = forward / toward +x)
    val shinLean = Math.toRadians(lerp(3f, 28f, t).toDouble())
    val thighAng = Math.toRadians(lerp(-4f, -98f, t).toDouble())
    val torsoLean = Math.toRadians(lerp(2f, 36f, t).toDouble())
    val armAng = Math.toRadians(lerp(80f, 6f, t).toDouble())   // from +x, y down: arms rise forward

    val kx = ax + shin * sin(shinLean).toFloat(); val ky = ay - shin * cos(shinLean).toFloat()
    val hx = kx + thigh * sin(thighAng).toFloat(); val hy = ky - thigh * cos(thighAng).toFloat()
    val sx = hx + torso * sin(torsoLean).toFloat(); val sy = hy - torso * cos(torsoLean).toFloat()
    val headX = sx + 2.4f * sin(torsoLean).toFloat(); val headY = sy - 2.4f * cos(torsoLean).toFloat()
    val wx = sx + 5.4f * cos(armAng).toFloat(); val wy = sy + 0.4f + 5.4f * sin(armAng).toFloat()

    floorAndShadow(u, p, colors, 4f, 22f, floorY, lerp(13.6f, 12.4f, t), lerp(4.6f, 6.4f, t), showFloor)

    // foot
    limb(listOf(p(ax, ay), p(ax + 2.3f, ay + 0.3f)), body, 2.0f * u)
    // leg: ankle -> knee -> hip
    limb(listOf(p(ax, ay), p(kx, ky), p(hx, hy)), body, 2.5f * u)
    // torso
    limb(listOf(p(hx, hy), p(sx, sy)), body, 2.7f * u)
    // arms
    limb(listOf(p(sx, sy + 0.4f), p(wx, wy)), body, 1.9f * u)
    // head
    drawCircle(body, radius = 2.05f * u, center = p(headX, headY))
    drawCircle(Color.White.copy(alpha = 0.35f), radius = 0.7f * u, center = p(headX - 0.6f, headY - 0.7f))
}

@Suppress("unused")
private fun angleOf(dx: Float, dy: Float) = atan2(dy, dx)
